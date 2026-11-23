package com.yunzhongyi.medical.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunzhongyi.medical.auth.PasswordUtil;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.system.entity.SysRole;
import com.yunzhongyi.medical.system.entity.SysUser;
import com.yunzhongyi.medical.system.entity.SysUserRole;
import com.yunzhongyi.medical.system.excel.SysUserExcelUtil;
import com.yunzhongyi.medical.system.mapper.SysRoleMapper;
import com.yunzhongyi.medical.system.mapper.SysUserMapper;
import com.yunzhongyi.medical.system.mapper.SysUserRoleMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理接口（底座；登录鉴权已接入：新建/重置/导入的密码均 PBKDF2 哈希存储，
 * 登录见 /api/auth，业务接口由鉴权拦截器校验 JWT）。
 * <p>
 * 第 28 项补强：登录账号全院唯一、启用/停用、重置密码（默认 123456）、
 * 分配角色、用户统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @GetMapping
    public Result<PageResult<SysUser>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>()
                        .and(StringUtils.hasText(keyword), x -> x.like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getRealName, keyword))
                        .eq(deptId != null, SysUser::getDeptId, deptId)
                        .eq(status != null, SysUser::getStatus, status)
                        .orderByDesc(SysUser::getId));
        // 不返回密码
        p.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 用户统计：总数 / 启用 / 停用 / 按科室分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<SysUser> all = userMapper.selectList(null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream().filter(u -> Integer.valueOf(1).equals(u.getStatus())).count());
        r.put("disabled", all.stream().filter(u -> Integer.valueOf(0).equals(u.getStatus())).count());
        Map<String, Long> byDept = new LinkedHashMap<>();
        for (SysUser u : all) {
            String key = u.getDeptId() == null ? "未分配科室" : "科室 " + u.getDeptId();
            byDept.merge(key, 1L, Long::sum);
        }
        r.put("byDept", byDept);
        return Result.ok(r);
    }

    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id) {
        SysUser u = userMapper.selectById(id);
        if (u != null) {
            u.setPassword(null);
        }
        return Result.ok(u);
    }

    @PostMapping
    public Result<SysUser> create(@Valid @RequestBody SysUser user) {
        if (!StringUtils.hasText(user.getUsername())) {
            return Result.fail(400, "登录账号不能为空");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername())) > 0) {
            return Result.fail(400, "登录账号已存在：" + user.getUsername());
        }
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(PasswordUtil.hash("123456"));
        } else {
            user.setPassword(PasswordUtil.hash(user.getPassword()));
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser old = userMapper.selectById(id);
        if (old == null) {
            return Result.fail(400, "用户不存在：" + id);
        }
        if (StringUtils.hasText(user.getUsername())
                && !user.getUsername().equals(old.getUsername())
                && userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername())) > 0) {
            return Result.fail(400, "登录账号已存在：" + user.getUsername());
        }
        user.setId(id);
        user.setPassword(null); // 改密码走单独接口，这里不覆盖
        userMapper.updateById(user);
        return Result.ok(user);
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        return Result.ok();
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<SysUser> enable(@PathVariable Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            return Result.fail(400, "用户不存在：" + id);
        }
        u.setStatus(1);
        userMapper.updateById(u);
        u.setPassword(null);
        return Result.ok(u);
    }

    /** 停用 */
    @PostMapping("/{id}/disable")
    public Result<SysUser> disable(@PathVariable Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            return Result.fail(400, "用户不存在：" + id);
        }
        u.setStatus(0);
        userMapper.updateById(u);
        u.setPassword(null);
        return Result.ok(u);
    }

    /** 重置密码（默认 123456，PBKDF2 哈希存储） */
    @PostMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            return Result.fail(400, "用户不存在：" + id);
        }
        u.setPassword(PasswordUtil.hash("123456"));
        userMapper.updateById(u);
        return Result.ok(Map.of("message", "密码已重置为默认密码 123456"));
    }

    /** 查某用户的全部角色 */
    @GetMapping("/{id}/roles")
    public Result<List<SysRole>> getRoles(@PathVariable Long id) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Result.ok(List.of());
        }
        return Result.ok(roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)));
    }

    /** 给用户分配角色（全量替换；角色 id 不存在时拒绝） */
    @PostMapping("/{id}/roles")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        if (userMapper.selectById(id) == null) {
            return Result.fail(400, "用户不存在：" + id);
        }
        List<Long> roleIds = body.getOrDefault("roleIds", List.of());
        for (Long roleId : roleIds) {
            if (roleMapper.selectById(roleId) == null) {
                return Result.fail(400, "角色不存在：" + roleId);
            }
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        for (Long roleId : roleIds) {
            SysUserRole link = new SysUserRole();
            link.setUserId(id);
            link.setRoleId(roleId);
            userRoleMapper.insert(link);
        }
        return Result.ok(Map.of("assigned", roleIds.size()));
    }

    /** 导出 Excel（按当前查询条件；密码不导出） */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long deptId,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("用户.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), x -> x.like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword))
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId));
        SysUserExcelUtil.write(list, response.getOutputStream());
    }

    /** 批量导入 Excel（账号重复或非法行跳过；密码统一为默认密码 123456） */
    @PostMapping("/import")
    public Result<?> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "请上传 Excel 文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.endsWith(".xlsx") || name.endsWith(".xls"))) {
            return Result.fail(400, "只支持 .xlsx / .xls 文件");
        }
        try {
            List<SysUser> rows = SysUserExcelUtil.read(file.getInputStream());
            int created = 0;
            int skipped = 0;
            for (SysUser row : rows) {
                if (!StringUtils.hasText(row.getUsername())
                        || userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, row.getUsername())) > 0) {
                    skipped++;
                    continue;
                }
                row.setPassword(PasswordUtil.hash("123456"));
                if (row.getStatus() == null) {
                    row.setStatus(1);
                }
                userMapper.insert(row);
                created++;
            }
            return Result.ok(Map.of("created", created, "skipped", skipped, "total", rows.size()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("用户导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        SysUserExcelUtil.writeTemplate(response.getOutputStream());
    }
}
