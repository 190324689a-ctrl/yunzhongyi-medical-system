package com.yunzhongyi.medical.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunzhongyi.medical.auth.dto.LoginResult;
import com.yunzhongyi.medical.system.entity.SysDept;
import com.yunzhongyi.medical.system.entity.SysPermission;
import com.yunzhongyi.medical.system.entity.SysRole;
import com.yunzhongyi.medical.system.entity.SysRoleDept;
import com.yunzhongyi.medical.system.entity.SysRolePermission;
import com.yunzhongyi.medical.system.entity.SysUser;
import com.yunzhongyi.medical.system.entity.SysUserRole;
import com.yunzhongyi.medical.system.mapper.SysDeptMapper;
import com.yunzhongyi.medical.system.mapper.SysPermissionMapper;
import com.yunzhongyi.medical.system.mapper.SysRoleDeptMapper;
import com.yunzhongyi.medical.system.mapper.SysRoleMapper;
import com.yunzhongyi.medical.system.mapper.SysRolePermissionMapper;
import com.yunzhongyi.medical.system.mapper.SysUserMapper;
import com.yunzhongyi.medical.system.mapper.SysUserRoleMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录鉴权服务：复用系统设置底座的 sys_user / sys_role / sys_permission 三张表。
 * <p>
 * 密码：PBKDF2 哈希存储；历史明文种子登录成功后自动迁移为哈希。
 * 失败处理：同一账号连续失败 {@code app.auth.max-failures} 次（默认 5 次）后锁定
 * {@code app.auth.lock-minutes} 分钟（默认 15 分钟）；账号不存在与密码错误返回同一提示，
 * 不泄露账号是否存在。
 * 登出：token 加入内存黑名单直至其过期；黑名单与失败计数均为单机内存实现，
 * 多实例部署需改用 Redis 等共享存储（见 README 限制说明）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final JwtUtil jwtUtil;

    /** 连续失败多少次后锁定，默认 5 */
    @Value("${app.auth.max-failures:5}")
    private int maxFailures;

    /** 锁定时长（分钟），默认 15 */
    @Value("${app.auth.lock-minutes:15}")
    private int lockMinutes;

    /** 账号 -> 失败记录 */
    private final ConcurrentHashMap<String, FailedLogin> failures = new ConcurrentHashMap<>();

    /** 已登出 token -> 过期时间毫秒（到期后清理） */
    private final ConcurrentHashMap<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    private record FailedLogin(int count, long lockedUntilMillis) {
    }

    /** 登录：校验账号密码，成功签发 JWT */
    public LoginResult login(String username, String password) {
        String name = username == null ? "" : username.trim();
        FailedLogin failed = failures.get(name);
        if (failed != null && failed.lockedUntilMillis() > System.currentTimeMillis()) {
            throw new AuthException(429, "登录失败次数过多，请 " + lockMinutes + " 分钟后再试");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, name));
        if (user == null || !StringUtils.hasText(user.getPassword())
                || !PasswordUtil.matches(password, user.getPassword())) {
            recordFailure(name);
            // 账号不存在与密码错误返回同一提示，不泄露账号是否存在
            throw new AuthException(401, "账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new AuthException(403, "账号已停用，请联系管理员");
        }
        // 历史明文密码登录成功后自动迁移为 PBKDF2 哈希
        if (!PasswordUtil.isHashed(user.getPassword())) {
            SysUser patch = new SysUser();
            patch.setId(user.getId());
            patch.setPassword(PasswordUtil.hash(password));
            userMapper.updateById(patch);
        }
        failures.remove(name);
        List<String> roles = roleCodesOf(user.getId());
        List<String> permissions = permissionCodesOf(user.getId());
        // 权限编码随 token 一起签发（快照）：接口权限校验只认 token 里的快照，
        // 角色改权限后需重新登录才生效；/api/auth/me 与 /api/auth/menus 仍实时查库。
        LoginResult result = new LoginResult();
        result.setToken(jwtUtil.createToken(user.getId(), user.getUsername(), user.getRealName(), roles, permissions));
        result.setExpiresIn(jwtUtil.getExpireSeconds());
        result.setUser(userInfo(user));
        result.setRoles(roles);
        result.setPermissions(permissions);
        return result;
    }

    /** 登出：把 token 加入黑名单直至其过期 */
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            blacklistedTokens.put(token, claims.getExpiration().getTime());
        } catch (Exception e) {
            // 已过期 / 非法 token 本来就不可用，无需再拉黑
            blacklistedTokens.put(token, System.currentTimeMillis());
        }
        purgeExpiredBlacklist();
    }

    /** token 是否在登出黑名单里 */
    public boolean isBlacklisted(String token) {
        Long expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry <= System.currentTimeMillis()) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    /** 当前登录用户信息（/api/auth/me）：数据库实时查角色与权限 */
    public Map<String, Object> currentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException(401, "登录已失效，请重新登录");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", userInfo(user));
        data.put("roles", roleCodesOf(userId));
        data.put("permissions", permissionCodesOf(userId));
        data.put("deptScope", deptScopeOf(userId));
        return data;
    }

    /** 当前登录用户可见的菜单树（/api/auth/menus）：按角色权限取启用的目录/菜单，按父子嵌套 */
    public List<Map<String, Object>> menusOf(Long userId) {
        List<SysPermission> allowed = visibleMenuPermissions(userId);
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (SysPermission p : allowed) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", p.getId());
            node.put("parentId", p.getParentId());
            node.put("name", p.getName());
            node.put("code", p.getCode());
            node.put("type", p.getType());
            node.put("path", p.getPath());
            node.put("sortOrder", p.getSortOrder());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(p.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SysPermission p : allowed) {
            Map<String, Object> node = nodes.get(p.getId());
            Long parentId = p.getParentId();
            if (parentId == null || parentId == 0 || !nodes.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodes.get(parentId).get("children");
                children.add(node);
            }
        }
        return roots;
    }

    /** 当前用户可见的目录/菜单权限（启用、type 1/2，按排序返回） */
    public List<SysPermission> visibleMenuPermissions(Long userId) {
        List<Long> roleIds = enabledRoleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<SysPermission> all = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, 1)
                .in(SysPermission::getType, List.of(1, 2))
                .orderByAsc(SysPermission::getSortOrder).orderByAsc(SysPermission::getId));
        // ADMIN 角色可见全部菜单；其他角色只看角色-权限关联到的菜单
        boolean isAdmin = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds).eq(SysRole::getCode, "ADMIN")).size() > 0;
        if (isAdmin) {
            return all;
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>()
                                .in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return all.stream().filter(p -> permissionIds.contains(p.getId())).toList();
    }

    /** 某用户启用角色绑定的科室名称（去重，按科室排序）：登录后数据范围用 */
    public List<String> deptScopeOf(Long userId) {
        List<Long> roleIds = enabledRoleIdsOf(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> deptIds = roleDeptMapper.selectList(
                        new LambdaQueryWrapper<SysRoleDept>().in(SysRoleDept::getRoleId, roleIds))
                .stream().map(SysRoleDept::getDeptId).distinct().toList();
        if (deptIds.isEmpty()) {
            return List.of();
        }
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                        .in(SysDept::getId, deptIds)
                        .orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getId))
                .stream().map(SysDept::getName).toList();
    }

    /** 某用户启用的角色 id */
    private List<Long> enabledRoleIdsOf(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1))
                .stream().map(SysRole::getId).toList();
    }

    /** 修改密码：校验原密码后写 PBKDF2 哈希；调用方应同时让当前 token 失效 */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException(401, "登录已失效，请重新登录");
        }
        if (!StringUtils.hasText(user.getPassword())
                || !PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new AuthException(401, "原密码不正确");
        }
        if (newPassword != null && newPassword.equals(oldPassword)) {
            throw new AuthException(400, "新密码不能与原密码相同");
        }
        SysUser patch = new SysUser();
        patch.setId(userId);
        patch.setPassword(PasswordUtil.hash(newPassword));
        userMapper.updateById(patch);
    }

    // ---------- 角色 / 权限（复用底座表） ----------

    /** 某用户的角色编码（只取启用角色） */
    public List<String> roleCodesOf(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1))
                .stream().map(SysRole::getCode).toList();
    }

    /** 某用户的权限标识（经角色关联，只取启用权限） */
    public List<String> permissionCodesOf(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permissionIds)
                        .eq(SysPermission::getStatus, 1))
                .stream().map(SysPermission::getCode).toList();
    }

    // ---------- 内部 ----------

    private void recordFailure(String username) {
        failures.compute(username, (key, old) -> {
            int count = old == null ? 1 : old.count() + 1;
            long lockedUntil = old == null ? 0L : old.lockedUntilMillis();
            if (count >= maxFailures) {
                lockedUntil = System.currentTimeMillis() + (long) lockMinutes * 60 * 1000;
                count = 0;
            }
            return new FailedLogin(count, lockedUntil);
        });
    }

    private void purgeExpiredBlacklist() {
        long now = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(e -> e.getValue() <= now);
    }

    /** 返回给前端的用户信息（不含密码） */
    private Map<String, Object> userInfo(SysUser user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("phone", user.getPhone());
        info.put("deptId", user.getDeptId());
        info.put("deptName", deptNameOf(user.getDeptId()));
        info.put("status", user.getStatus());
        return info;
    }

    /** 科室 id 对应名称（用户未归属科室时为空） */
    private String deptNameOf(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = deptMapper.selectById(deptId);
        return dept == null ? null : dept.getName();
    }

    /** 供测试清理：清除某账号的失败计数（测试之间互不干扰） */
    public void clearFailures(String username) {
        failures.remove(username);
    }

    /** 供测试清理：清除全部失败计数与黑名单 */
    public void clearAllAuthState() {
        failures.clear();
        blacklistedTokens.clear();
    }
}
