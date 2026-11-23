package com.yunzhongyi.medical.sysconfig.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.sysconfig.service.KnowledgeItemService;
import com.yunzhongyi.medical.sysconfig.service.SysBackupService;
import com.yunzhongyi.medical.sysconfig.service.SysInterfaceService;
import com.yunzhongyi.medical.sysconfig.service.SysNoticeSettingService;
import com.yunzhongyi.medical.sysconfig.service.SysParamService;
import com.yunzhongyi.medical.system.mapper.SysDeptMapper;
import com.yunzhongyi.medical.system.mapper.SysDictDataMapper;
import com.yunzhongyi.medical.system.mapper.SysDictTypeMapper;
import com.yunzhongyi.medical.system.mapper.SysOperLogMapper;
import com.yunzhongyi.medical.system.mapper.SysPermissionMapper;
import com.yunzhongyi.medical.system.mapper.SysRoleMapper;
import com.yunzhongyi.medical.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 医务系统设置总览：底座（用户/角色/权限/科室/字典/操作日志）与
 * 新增五组配置（参数/备份/接口/提醒设置/知识库）的数量汇总，供设置总览页展示。
 */
@RestController
@RequestMapping("/api/system-settings")
@RequiredArgsConstructor
public class SystemSettingsOverviewController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysDeptMapper deptMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysOperLogMapper operLogMapper;
    private final SysParamService paramService;
    private final SysBackupService backupService;
    private final SysInterfaceService interfaceService;
    private final SysNoticeSettingService noticeSettingService;
    private final KnowledgeItemService knowledgeItemService;

    /** 系统设置总览统计 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("users", userMapper.selectCount(null));
        r.put("roles", roleMapper.selectCount(null));
        r.put("permissions", permissionMapper.selectCount(null));
        r.put("depts", deptMapper.selectCount(null));
        r.put("dictTypes", dictTypeMapper.selectCount(null));
        r.put("dictData", dictDataMapper.selectCount(null));
        r.put("operLogs", operLogMapper.selectCount(null));
        r.put("params", paramService.count());
        r.put("backups", backupService.count());
        r.put("interfaces", interfaceService.count());
        r.put("noticeSettings", noticeSettingService.count());
        r.put("knowledge", knowledgeItemService.count());
        r.put("paramStats", paramService.stats());
        r.put("backupStats", backupService.stats());
        r.put("interfaceStats", interfaceService.stats());
        r.put("noticeStats", noticeSettingService.stats());
        r.put("knowledgeStats", knowledgeItemService.stats());
        return Result.ok(r);
    }
}
