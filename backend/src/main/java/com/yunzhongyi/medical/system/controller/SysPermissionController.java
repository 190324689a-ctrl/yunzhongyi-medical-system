package com.yunzhongyi.medical.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.system.entity.SysPermission;
import com.yunzhongyi.medical.system.mapper.SysPermissionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限（菜单/按钮）管理接口（底座）。
 * <p>
 * 第 28 项补强：权限标识全院唯一、启用/停用、权限统计；
 * 有子权限的不可直接删除。
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionMapper permissionMapper;

    /** 全部权限（按排序返回，前端自行组树） */
    @GetMapping
    public Result<List<SysPermission>> list() {
        return Result.ok(permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSortOrder)
                        .orderByAsc(SysPermission::getId)));
    }

    /** 权限统计：总数 / 启用 / 停用 / 按类型分组（目录/菜单/按钮） */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<SysPermission> all = permissionMapper.selectList(null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream().filter(p -> Integer.valueOf(1).equals(p.getStatus())).count());
        r.put("disabled", all.stream().filter(p -> Integer.valueOf(0).equals(p.getStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("目录", all.stream().filter(p -> Integer.valueOf(1).equals(p.getType())).count());
        byType.put("菜单", all.stream().filter(p -> Integer.valueOf(2).equals(p.getType())).count());
        byType.put("按钮", all.stream().filter(p -> Integer.valueOf(3).equals(p.getType())).count());
        r.put("byType", byType);
        return Result.ok(r);
    }

    @PostMapping
    public Result<SysPermission> create(@Valid @RequestBody SysPermission perm) {
        if (StringUtils.hasText(perm.getCode())
                && permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, perm.getCode())) > 0) {
            return Result.fail(400, "权限标识已存在：" + perm.getCode());
        }
        if (perm.getParentId() == null) {
            perm.setParentId(0L);
        }
        if (perm.getStatus() == null) {
            perm.setStatus(1);
        }
        permissionMapper.insert(perm);
        return Result.ok(perm);
    }

    @PutMapping("/{id}")
    public Result<SysPermission> update(@PathVariable Long id, @Valid @RequestBody SysPermission perm) {
        SysPermission old = permissionMapper.selectById(id);
        if (old == null) {
            return Result.fail(400, "权限不存在：" + id);
        }
        if (StringUtils.hasText(perm.getCode())
                && !perm.getCode().equals(old.getCode())
                && permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, perm.getCode())) > 0) {
            return Result.fail(400, "权限标识已存在：" + perm.getCode());
        }
        perm.setId(id);
        permissionMapper.updateById(perm);
        return Result.ok(perm);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        long children = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getParentId, id));
        if (children > 0) {
            return Result.fail(400, "该权限有 " + children + " 个子权限，不能直接删除");
        }
        permissionMapper.deleteById(id);
        return Result.ok();
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<SysPermission> enable(@PathVariable Long id) {
        SysPermission perm = permissionMapper.selectById(id);
        if (perm == null) {
            return Result.fail(400, "权限不存在：" + id);
        }
        perm.setStatus(1);
        permissionMapper.updateById(perm);
        return Result.ok(perm);
    }

    /** 停用 */
    @PostMapping("/{id}/disable")
    public Result<SysPermission> disable(@PathVariable Long id) {
        SysPermission perm = permissionMapper.selectById(id);
        if (perm == null) {
            return Result.fail(400, "权限不存在：" + id);
        }
        perm.setStatus(0);
        permissionMapper.updateById(perm);
        return Result.ok(perm);
    }
}
