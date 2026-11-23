package com.yunzhongyi.medical.sysconfig.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.sysconfig.dto.SysBackupQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysBackup;
import com.yunzhongyi.medical.sysconfig.service.SysBackupService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 数据备份管理接口：备份执行、恢复、查询、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/sys-backups")
@RequiredArgsConstructor
public class SysBackupController {

    private final SysBackupService sysBackupService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(SysBackupQuery query) {
        return Result.ok(sysBackupService.pageQuery(query));
    }

    /** 备份统计：总数 / 成功 / 失败 / 进行中 / 已恢复数 / 总记录数 / 按类型分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(sysBackupService.stats());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<SysBackup> get(@PathVariable Long id) {
        try {
            return Result.ok(sysBackupService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 执行备份（统计全库行数、生成备份文件名、登记为成功） */
    @PostMapping
    public Result<SysBackup> create(@Valid @RequestBody SysBackup backup) {
        try {
            return Result.ok(sysBackupService.create(backup));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 恢复备份：只有成功的备份能恢复，恢复次数 +1 并记录恢复时间 */
    @PostMapping("/{id}/restore")
    public Result<SysBackup> restore(@PathVariable Long id) {
        try {
            return Result.ok(sysBackupService.restore(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已恢复过的备份不可直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            sysBackupService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(SysBackupQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("数据备份.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        sysBackupService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的是历史备份记录） */
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
            return Result.ok(sysBackupService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("数据备份导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        sysBackupService.writeImportTemplate(response.getOutputStream());
    }
}
