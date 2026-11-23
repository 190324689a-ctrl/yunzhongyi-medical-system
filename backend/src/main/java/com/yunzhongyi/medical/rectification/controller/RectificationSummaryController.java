package com.yunzhongyi.medical.rectification.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.rectification.dto.GenerateRequest;
import com.yunzhongyi.medical.rectification.dto.IssueSummaryQuery;
import com.yunzhongyi.medical.rectification.dto.OverdueQuery;
import com.yunzhongyi.medical.rectification.dto.RectificationSummaryQuery;
import com.yunzhongyi.medical.rectification.entity.RectificationSummary;
import com.yunzhongyi.medical.rectification.service.RectificationSummaryService;
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
 * 检查整改汇总接口：汇总报告查询/增删改、按统计周期生成汇总、
 * 检查问题/整改任务实时汇总统计（按科室/按类型/按时间、整改完成率、逾期统计）、
 * 逾期整改与问题汇总查询、批量导入导出
 */
@RestController
@RequestMapping("/api/rectification-summaries")
@RequiredArgsConstructor
public class RectificationSummaryController {

    private final RectificationSummaryService rectificationSummaryService;

    /** 汇总报告多条件组合分页查询 */
    @GetMapping
    public Result<?> page(RectificationSummaryQuery query) {
        return Result.ok(rectificationSummaryService.pageQuery(query));
    }

    /** 实时汇总统计：问题总数 / 按严重程度分组 / 整改任务数 / 整改完成率 / 逾期数 / 按科室/按类型/按月汇总 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(rectificationSummaryService.stats());
    }

    /** 按责任科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(rectificationSummaryService.statsByDept());
    }

    /** 按检查类型汇总统计 */
    @GetMapping("/stats/by-type")
    public Result<List<Map<String, Object>>> statsByType() {
        return Result.ok(rectificationSummaryService.statsByType());
    }

    /** 按月汇总统计（按问题发现月份，月份升序） */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(rectificationSummaryService.statsByMonth());
    }

    /** 逾期整改任务分页查询（带问题描述、严重程度、责任科室） */
    @GetMapping("/overdue")
    public Result<?> overdue(OverdueQuery query) {
        return Result.ok(rectificationSummaryService.pageOverdue(query));
    }

    /** 检查问题汇总分页查询（带整改任务状态、整改期限与逾期标记） */
    @GetMapping("/issues")
    public Result<?> issues(IssueSummaryQuery query) {
        return Result.ok(rectificationSummaryService.pageIssueSummary(query));
    }

    /** 汇总报告详情 */
    @GetMapping("/{id}")
    public Result<RectificationSummary> get(@PathVariable Long id) {
        try {
            return Result.ok(rectificationSummaryService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增汇总报告（同一周期同一范围同一科室/类型已有记录时拒绝，请重新生成） */
    @PostMapping
    public Result<RectificationSummary> create(@Valid @RequestBody RectificationSummary summary) {
        try {
            rectificationSummaryService.save(summary);
            return Result.ok(summary);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改汇总报告（汇总口径不可改，改口径请重新生成；整改完成率按已复查通过数现算） */
    @PutMapping("/{id}")
    public Result<RectificationSummary> update(@PathVariable Long id,
                                               @Valid @RequestBody RectificationSummary summary) {
        try {
            summary.setId(id);
            rectificationSummaryService.updateById(summary);
            return Result.ok(summary);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除汇总报告 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            rectificationSummaryService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除汇总报告 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            rectificationSummaryService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 生成汇总报告：按统计周期 + 汇总范围实时聚合生成；已存在时重新生成（原地更新） */
    @PostMapping("/generate")
    public Result<RectificationSummary> generate(@RequestBody GenerateRequest request) {
        try {
            return Result.ok(rectificationSummaryService.generate(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出汇总报告 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(RectificationSummaryQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("检查整改汇总.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        rectificationSummaryService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入汇总报告 Excel（同一周期同一范围同一科室/类型重复跳过） */
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
            int count = rectificationSummaryService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("检查整改汇总导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        rectificationSummaryService.exportExcel(new RectificationSummaryQuery(), response.getOutputStream());
    }
}
