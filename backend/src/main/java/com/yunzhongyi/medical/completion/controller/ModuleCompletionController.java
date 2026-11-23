package com.yunzhongyi.medical.completion.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.completion.dto.ModuleCompletionQuery;
import com.yunzhongyi.medical.completion.entity.ModuleCompletion;
import com.yunzhongyi.medical.completion.service.ModuleCompletionService;
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
 * 医务系统完成统计接口：模块完成台账查询/增删改、标记完成/重新开始、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/module-completions")
@RequiredArgsConstructor
public class ModuleCompletionController {

    private final ModuleCompletionService moduleCompletionService;

    /** 多条件组合分页查询（关键字 / 分组 / 状态 / 模块编号） */
    @GetMapping
    public Result<?> page(ModuleCompletionQuery query) {
        return Result.ok(moduleCompletionService.pageQuery(query));
    }

    /** 完成统计总览：模块总数、已完成/进行中/待开发数、完成率、接口总数、测试总数、平均进度 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(moduleCompletionService.stats());
    }

    /** 按分组汇总统计 */
    @GetMapping("/stats/by-category")
    public Result<List<Map<String, Object>>> statsByCategory() {
        return Result.ok(moduleCompletionService.statsByCategory());
    }

    /** 按完成月份趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(moduleCompletionService.statsByMonth());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<ModuleCompletion> get(@PathVariable Long id) {
        try {
            return Result.ok(moduleCompletionService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增模块台账（模块编号全院唯一） */
    @PostMapping
    public Result<ModuleCompletion> create(@Valid @RequestBody ModuleCompletion record) {
        try {
            return Result.ok(moduleCompletionService.create(record));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改模块台账 */
    @PutMapping("/{id}")
    public Result<ModuleCompletion> update(@PathVariable Long id,
                                           @Valid @RequestBody ModuleCompletion record) {
        try {
            record.setId(id);
            return Result.ok(moduleCompletionService.update(record));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(moduleCompletionService.removeById(id));
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        return Result.ok(moduleCompletionService.removeByIds(ids));
    }

    /** 标记完成：进度置 100、完成日期默认当天 */
    @PostMapping("/{id}/complete")
    public Result<ModuleCompletion> complete(@PathVariable Long id) {
        try {
            return Result.ok(moduleCompletionService.complete(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 重新开始：回到进行中、进度置 50、完成日期清空 */
    @PostMapping("/{id}/reopen")
    public Result<ModuleCompletion> reopen(@PathVariable Long id) {
        try {
            return Result.ok(moduleCompletionService.reopen(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(ModuleCompletionQuery query, HttpServletResponse response)
            throws IOException {
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("模块完成情况.xlsx", StandardCharsets.UTF_8));
        moduleCompletionService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（模块编号重复或非法行跳过） */
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            return Result.ok(moduleCompletionService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail(400, "导入失败：" + e.getMessage());
        }
    }

    /** 导入模板下载 */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("模块完成情况导入模板.xlsx", StandardCharsets.UTF_8));
        moduleCompletionService.writeImportTemplate(response.getOutputStream());
    }
}
