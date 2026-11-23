package com.yunzhongyi.medical.statistics.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.statistics.dto.QualityIndicatorQuery;
import com.yunzhongyi.medical.statistics.entity.QualityIndicator;
import com.yunzhongyi.medical.statistics.service.QualityIndicatorService;
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
 * 三级医院医疗质量指标统计接口：第 25 项，指标查询 / 增删改、按国家 / 省 / 市
 * 标准统计（达标率）、批量导入导出
 */
@RestController
@RequestMapping("/api/quality-indicators")
@RequiredArgsConstructor
public class QualityIndicatorController {

    private final QualityIndicatorService qualityIndicatorService;

    /** 多条件组合分页查询（每条指标附带达标情况） */
    @GetMapping
    public Result<?> page(QualityIndicatorQuery query) {
        return Result.ok(qualityIndicatorService.pageQuery(query));
    }

    /** 指标统计：总数 / 已填报数 / 达标数 / 达标率 / 按标准级别 / 按分类汇总 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(qualityIndicatorService.stats());
    }

    /** 按标准级别汇总统计 */
    @GetMapping("/stats/by-level")
    public Result<List<Map<String, Object>>> statsByLevel() {
        return Result.ok(qualityIndicatorService.statsByLevel());
    }

    /** 按指标分类汇总统计 */
    @GetMapping("/stats/by-category")
    public Result<List<Map<String, Object>>> statsByCategory() {
        return Result.ok(qualityIndicatorService.statsByCategory());
    }

    /** 详情（附带达标情况） */
    @GetMapping("/{id}")
    public Result<QualityIndicator> get(@PathVariable Long id) {
        QualityIndicator indicator = qualityIndicatorService.getById(id);
        if (indicator == null) {
            return Result.fail(400, "医疗质量指标不存在：" + id);
        }
        return Result.ok(indicator);
    }

    /** 新增指标（指标编码全院唯一） */
    @PostMapping
    public Result<QualityIndicator> create(@Valid @RequestBody QualityIndicator indicator) {
        try {
            qualityIndicatorService.save(indicator);
            return Result.ok(indicator);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<QualityIndicator> update(@PathVariable Long id, @Valid @RequestBody QualityIndicator indicator) {
        try {
            indicator.setId(id);
            qualityIndicatorService.updateById(indicator);
            return Result.ok(indicator);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        qualityIndicatorService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        qualityIndicatorService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(QualityIndicatorQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("三级医院医疗质量指标.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        qualityIndicatorService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel */
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
            int count = qualityIndicatorService.importExcel(file.getInputStream());
            return Result.ok(Map.of("imported", count));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("三级医院医疗质量指标导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        qualityIndicatorService.exportExcel(new QualityIndicatorQuery(), response.getOutputStream());
    }
}
