package com.yunzhongyi.medical.sysconfig.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.sysconfig.dto.SysNoticeSettingQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysNoticeSetting;
import com.yunzhongyi.medical.sysconfig.service.SysNoticeSettingService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 消息提醒设置管理接口：提醒规则配置查询/增删改、启用/停用、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/sys-notice-settings")
@RequiredArgsConstructor
public class SysNoticeSettingController {

    private final SysNoticeSettingService sysNoticeSettingService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(SysNoticeSettingQuery query) {
        return Result.ok(sysNoticeSettingService.pageQuery(query));
    }

    /** 提醒设置统计：总数 / 启用 / 停用 / 按渠道分组 / 按接收范围分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(sysNoticeSettingService.stats());
    }

    /** 全部启用的设置 */
    @GetMapping("/enabled")
    public Result<List<SysNoticeSetting>> enabled() {
        return Result.ok(sysNoticeSettingService.enabled());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<SysNoticeSetting> get(@PathVariable Long id) {
        try {
            return Result.ok(sysNoticeSettingService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增设置（默认站内消息、全部人员、启用） */
    @PostMapping
    public Result<SysNoticeSetting> create(@Valid @RequestBody SysNoticeSetting setting) {
        try {
            return Result.ok(sysNoticeSettingService.create(setting));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改设置 */
    @PutMapping("/{id}")
    public Result<SysNoticeSetting> update(@PathVariable Long id,
                                           @Valid @RequestBody SysNoticeSetting setting) {
        try {
            setting.setId(id);
            return Result.ok(sysNoticeSettingService.update(setting));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            sysNoticeSettingService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<SysNoticeSetting> enable(@PathVariable Long id) {
        try {
            return Result.ok(sysNoticeSettingService.enable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用 */
    @PostMapping("/{id}/disable")
    public Result<SysNoticeSetting> disable(@PathVariable Long id) {
        try {
            return Result.ok(sysNoticeSettingService.disable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(SysNoticeSettingQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("消息提醒设置.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        sysNoticeSettingService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（编码重复或非法行跳过） */
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
            return Result.ok(sysNoticeSettingService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("消息提醒设置导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        sysNoticeSettingService.writeImportTemplate(response.getOutputStream());
    }
}
