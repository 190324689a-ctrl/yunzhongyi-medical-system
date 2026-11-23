package com.yunzhongyi.medical.quality.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.quality.dto.QualityPlanQuery;
import com.yunzhongyi.medical.quality.dto.QualityRectificationQuery;
import com.yunzhongyi.medical.quality.dto.QualityReviewRequest;
import com.yunzhongyi.medical.quality.entity.QualityIssue;
import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityRecord;
import com.yunzhongyi.medical.quality.entity.QualityRectification;
import com.yunzhongyi.medical.quality.excel.QualityExcelUtil;
import com.yunzhongyi.medical.quality.service.QualityService;
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
 * 质量检查管理接口：检查计划查询/增删改、开始/完成/取消、检查记录（逐项打分）、
 * 检查问题、整改跟踪（提交整改/复查通过/打回）、检查报告、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/quality-inspections")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;

    /** 多条件组合分页查询检查计划 */
    @GetMapping
    public Result<?> page(QualityPlanQuery query) {
        return Result.ok(qualityService.pageQuery(query));
    }

    /** 质量检查统计：计划总数 / 按状态分组 / 按检查类型分组 / 按科室汇总 / 按月趋势 / 记录数 / 平均分 / 问题数 / 整改完成率 / 逾期整改数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(qualityService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(qualityService.statsByDept());
    }

    /** 按月趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(qualityService.statsByMonth());
    }

    /** 整改任务分页查询（责任人/状态/只看逾期/计划） */
    @GetMapping("/rectifications")
    public Result<?> pageRectifications(QualityRectificationQuery query) {
        return Result.ok(qualityService.pageRectifications(query));
    }

    /** 详情（含全部检查记录、问题、整改任务） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        try {
            return Result.ok(qualityService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增检查计划（默认待执行；模板须启用，计划的检查类型与模板一致） */
    @PostMapping
    public Result<QualityPlan> create(@Valid @RequestBody QualityPlan plan) {
        try {
            qualityService.save(plan);
            return Result.ok(plan);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待执行的计划能修改） */
    @PutMapping("/{id}")
    public Result<QualityPlan> update(@PathVariable Long id, @Valid @RequestBody QualityPlan plan) {
        try {
            plan.setId(id);
            qualityService.updateById(plan);
            return Result.ok(plan);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        qualityService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        qualityService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 开始检查：待执行 → 进行中 */
    @PostMapping("/{id}/start")
    public Result<QualityPlan> start(@PathVariable Long id) {
        try {
            return Result.ok(qualityService.start(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 完成检查：进行中 → 已完成（需要至少一条检查记录） */
    @PostMapping("/{id}/complete")
    public Result<QualityPlan> complete(@PathVariable Long id) {
        try {
            return Result.ok(qualityService.complete(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 取消计划：待执行/进行中 → 已取消 */
    @PostMapping("/{id}/cancel")
    public Result<QualityPlan> cancel(@PathVariable Long id) {
        try {
            return Result.ok(qualityService.cancel(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 检查报告：按计划生成报告摘要（总分、等级、问题数、整改完成率） */
    @GetMapping("/{id}/report")
    public Result<Map<String, Object>> report(@PathVariable Long id) {
        try {
            return Result.ok(qualityService.report(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ---- 检查记录 ----

    /** 查某计划的全部检查记录 */
    @GetMapping("/{id}/records")
    public Result<List<QualityRecord>> listRecords(@PathVariable Long id) {
        try {
            return Result.ok(qualityService.listRecords(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 添加检查记录：只有进行中的计划才能记录；逐项打分，总分与等级自动算 */
    @PostMapping("/{id}/records")
    public Result<QualityRecord> addRecord(@PathVariable Long id, @Valid @RequestBody QualityRecord record) {
        try {
            return Result.ok(qualityService.addRecord(id, record));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除一条检查记录（同时删掉该记录下的问题与整改任务） */
    @DeleteMapping("/{id}/records/{recordId}")
    public Result<?> deleteRecord(@PathVariable Long id, @PathVariable Long recordId) {
        try {
            qualityService.removeRecord(id, recordId);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ---- 检查问题 ----

    /** 查某记录的全部问题 */
    @GetMapping("/records/{recordId}/issues")
    public Result<List<QualityIssue>> listIssues(@PathVariable Long recordId) {
        try {
            return Result.ok(qualityService.listIssues(recordId));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 添加问题：严重程度须合法 */
    @PostMapping("/records/{recordId}/issues")
    public Result<QualityIssue> addIssue(@PathVariable Long recordId, @Valid @RequestBody QualityIssue issue) {
        try {
            return Result.ok(qualityService.addIssue(recordId, issue));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除一条问题（同时删掉该问题下的整改任务） */
    @DeleteMapping("/records/{recordId}/issues/{issueId}")
    public Result<?> deleteIssue(@PathVariable Long recordId, @PathVariable Long issueId) {
        try {
            qualityService.removeIssue(recordId, issueId);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ---- 整改跟踪 ----

    /** 查某计划的全部整改任务（逾期标记动态算出） */
    @GetMapping("/{id}/rectifications")
    public Result<List<QualityRectification>> listRectifications(@PathVariable Long id) {
        try {
            return Result.ok(qualityService.listRectifications(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 针对一个问题发整改任务：一个问题只发一个 */
    @PostMapping("/issues/{issueId}/rectifications")
    public Result<QualityRectification> addRectification(@PathVariable Long issueId,
                                                         @Valid @RequestBody QualityRectification rectification) {
        try {
            return Result.ok(qualityService.addRectification(issueId, rectification));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 提交整改：待整改 → 已整改（待复查） */
    @PostMapping("/rectifications/{rectificationId}/submit")
    public Result<QualityRectification> submitRectification(@PathVariable Long rectificationId) {
        try {
            return Result.ok(qualityService.submitRectification(rectificationId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 复查通过：已整改 → 已复查通过 */
    @PostMapping("/rectifications/{rectificationId}/review-pass")
    public Result<QualityRectification> reviewPass(@PathVariable Long rectificationId,
                                                   @RequestBody(required = false) QualityReviewRequest request) {
        String reviewer = request == null ? null : request.getReviewer();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(qualityService.reviewPass(rectificationId, reviewer, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 复查打回：已整改 → 待整改，可重新提交 */
    @PostMapping("/rectifications/{rectificationId}/review-reject")
    public Result<QualityRectification> reviewReject(@PathVariable Long rectificationId,
                                                     @RequestBody(required = false) QualityReviewRequest request) {
        String reviewer = request == null ? null : request.getReviewer();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(qualityService.reviewReject(rectificationId, reviewer, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    // ---- Excel ----

    /** 导出 Excel：检查记录（按当前查询条件对应的计划） */
    @GetMapping("/export")
    public void export(QualityPlanQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("质量检查记录.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        qualityService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（检查计划）：同一模板同一科室同一计划日期重复跳过，非法值给默认值并在结果里说明 */
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
            return Result.ok(qualityService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（检查计划导入模板，空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("质量检查计划导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        QualityExcelUtil.writePlanTemplate(response.getOutputStream());
    }
}
