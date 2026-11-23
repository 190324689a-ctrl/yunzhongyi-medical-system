package com.yunzhongyi.medical.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.system.entity.SysDept;
import com.yunzhongyi.medical.system.entity.SysPermission;
import com.yunzhongyi.medical.system.entity.SysRole;
import com.yunzhongyi.medical.system.entity.SysRoleDept;
import com.yunzhongyi.medical.system.entity.SysRolePermission;
import com.yunzhongyi.medical.system.entity.SysUserRole;
import com.yunzhongyi.medical.system.mapper.SysDeptMapper;
import com.yunzhongyi.medical.system.mapper.SysPermissionMapper;
import com.yunzhongyi.medical.system.mapper.SysRoleDeptMapper;
import com.yunzhongyi.medical.system.mapper.SysRoleMapper;
import com.yunzhongyi.medical.system.mapper.SysRolePermissionMapper;
import com.yunzhongyi.medical.system.mapper.SysUserRoleMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理接口（底座）。
 * <p>
 * 第 28 项补强：角色编码全院唯一、启用/停用、分配权限、角色统计；
 * 删除角色时同时删除其权限分配与用户分配。
 * 登录鉴权增强：角色可绑定科室（查 / 全量替换绑定科室），删除角色时同时删除其科室绑定。
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    @GetMapping
    public Result<PageResult<SysRole>> page(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        Page<SysRole> p = roleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysRole>()
                        .and(StringUtils.hasText(keyword), x -> x.like(SysRole::getName, keyword)
                                .or().like(SysRole::getCode, keyword))
                        .eq(status != null, SysRole::getStatus, status)
                        .orderByDesc(SysRole::getId));
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 角色统计：总数 / 启用 / 停用 / 各角色已分配用户数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<SysRole> all = roleMapper.selectList(null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream().filter(x -> Integer.valueOf(1).equals(x.getStatus())).count());
        r.put("disabled", all.stream().filter(x -> Integer.valueOf(0).equals(x.getStatus())).count());
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (SysRole role : all) {
            usersByRole.put(role.getName(), userRoleMapper.selectCount(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, role.getId())));
        }
        r.put("usersByRole", usersByRole);
        return Result.ok(r);
    }

    /** 全部启用的角色（给用户分配角色时下拉用） */
    @GetMapping("/enabled")
    public Result<List<SysRole>> enabled() {
        return Result.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1).orderByAsc(SysRole::getId)));
    }

    @PostMapping
    public Result<SysRole> create(@Valid @RequestBody SysRole role) {
        if (!StringUtils.hasText(role.getCode())) {
            return Result.fail(400, "角色编码不能为空");
        }
        if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, role.getCode())) > 0) {
            return Result.fail(400, "角色编码已存在：" + role.getCode());
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        roleMapper.insert(role);
        return Result.ok(role);
    }

    @PutMapping("/{id}")
    public Result<SysRole> update(@PathVariable Long id, @Valid @RequestBody SysRole role) {
        SysRole old = roleMapper.selectById(id);
        if (old == null) {
            return Result.fail(400, "角色不存在：" + id);
        }
        if (StringUtils.hasText(role.getCode())
                && !role.getCode().equals(old.getCode())
                && roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, role.getCode())) > 0) {
            return Result.fail(400, "角色编码已存在：" + role.getCode());
        }
        role.setId(id);
        roleMapper.updateById(role);
        return Result.ok(role);
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(@PathVariable Long id) {
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, id));
        return Result.ok();
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<SysRole> enable(@PathVariable Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            return Result.fail(400, "角色不存在：" + id);
        }
        role.setStatus(1);
        roleMapper.updateById(role);
        return Result.ok(role);
    }

    /** 停用 */
    @PostMapping("/{id}/disable")
    public Result<SysRole> disable(@PathVariable Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            return Result.fail(400, "角色不存在：" + id);
        }
        role.setStatus(0);
        roleMapper.updateById(role);
        return Result.ok(role);
    }

    /** 查某角色的全部权限 */
    @GetMapping("/{id}/permissions")
    public Result<List<SysPermission>> getPermissions(@PathVariable Long id) {
        List<Long> permIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id))
                .stream().map(SysRolePermission::getPermissionId).toList();
        if (permIds.isEmpty()) {
            return Result.ok(List.of());
        }
        return Result.ok(permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().in(SysPermission::getId, permIds)));
    }

    /** 给角色分配权限（全量替换；权限 id 不存在时拒绝） */
    @PostMapping("/{id}/permissions")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignPermissions(@PathVariable Long id,
                                       @RequestBody Map<String, List<Long>> body) {
        if (roleMapper.selectById(id) == null) {
            return Result.fail(400, "角色不存在：" + id);
        }
        List<Long> permIds = body.getOrDefault("permissionIds", List.of());
        for (Long permId : permIds) {
            if (permissionMapper.selectById(permId) == null) {
                return Result.fail(400, "权限不存在：" + permId);
            }
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
        for (Long permId : permIds) {
            SysRolePermission link = new SysRolePermission();
            link.setRoleId(id);
            link.setPermissionId(permId);
            rolePermissionMapper.insert(link);
        }
        return Result.ok(Map.of("assigned", permIds.size()));
    }

    /** 查某角色绑定的全部科室（按科室排序返回） */
    @GetMapping("/{id}/depts")
    public Result<List<SysDept>> getDepts(@PathVariable Long id) {
        if (roleMapper.selectById(id) == null) {
            return Result.fail(400, "角色不存在：" + id);
        }
        List<Long> deptIds = roleDeptMapper.selectList(
                        new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, id))
                .stream().map(SysRoleDept::getDeptId).toList();
        if (deptIds.isEmpty()) {
            return Result.ok(List.of());
        }
        return Result.ok(deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .in(SysDept::getId, deptIds)
                .orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getId)));
    }

    /** 给角色绑定科室（全量替换；科室 id 不存在时拒绝） */
    @PostMapping("/{id}/depts")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignDepts(@PathVariable Long id,
                                 @RequestBody Map<String, List<Long>> body) {
        if (roleMapper.selectById(id) == null) {
            return Result.fail(400, "角色不存在：" + id);
        }
        List<Long> deptIds = body.getOrDefault("deptIds", List.of());
        for (Long deptId : deptIds) {
            if (deptMapper.selectById(deptId) == null) {
                return Result.fail(400, "科室不存在：" + deptId);
            }
        }
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, id));
        for (Long deptId : deptIds) {
            SysRoleDept link = new SysRoleDept();
            link.setRoleId(id);
            link.setDeptId(deptId);
            roleDeptMapper.insert(link);
        }
        return Result.ok(Map.of("assigned", deptIds.size()));
    }
}
