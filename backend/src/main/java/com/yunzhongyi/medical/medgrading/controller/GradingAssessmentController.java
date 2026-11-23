package com.yunzhongyi.medical.medgrading.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.medgrading.dto.ApproveRequest;
import com.yunzhongyi.medical.medgrading.dto.GradingAssessmentQuery;
import com.yunzhongyi.medical.medgrading.entity.GradingAssessment;
import com.yunzhongyi.medical.medgrading.service.GradingAssessmentService;
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
 * 医疗分级评定管理接口：评定查询/增删改、一级审批、统计、批量导入导出。
 * <p>
 * 等级由评分按所属标准的分数线自动算：甲级 / 乙级 / 丙级 / 丁级。
 */
@RestController
@RequestMapping("/api/grading-assessments")
@RequiredArgsConstructor
public class GradingAssessmentController {

    private final GradingAssessmentService gradingAssessmentService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(GradingAssessmentQuery query) {
        return Result.ok(gradingAssessmentService.pageQuery(query));
    }

    /** 分级评定统计：总数 / 待审批 / 已通过 / 已驳回 / 按等级分组 / 平均分 / 达标率 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(gradingAssessmentService.stats());
    }

    /** 按科室汇总：各科室最新已通过等级（当前分级）、平均分、评定次数 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(gradingAssessmentService.statsByDept());
    }

    /** 按月趋势：每月评定数、已通过数、平均分，按月份升序 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(gradingAssessmentService.statsByMonth());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<GradingAssessment> get(@PathVariable Long id) {
        try {
            return Result.ok(gradingAssessmentService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增评定（默认待审批；等级按标准分数线自动算） */
    @PostMapping
    public Result<GradingAssessment> create(@Valid @RequestBody GradingAssessment assessment) {
        try {
            return Result.ok(gradingAssessmentService.create(assessment));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改评定（只有待审批的能修改，改评分时重算等级） */
    @PutMapping("/{id}")
    public Result<GradingAssessment> update(@PathVariable Long id,
                                            @Valid @RequestBody GradingAssessment assessment) {
        try {
            assessment.setId(id);
            return Result.ok(gradingAssessmentService.update(assessment));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已通过的评定不可直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            gradingAssessmentService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批通过（审批人必填，审批记录留痕） */
    @PostMapping("/{id}/approve")
    public Result<GradingAssessment> approve(@PathVariable Long id,
                                             @RequestBody ApproveRequest request) {
        try {
            String approver = request == null ? null : request.getApprover();
            String opinion = request == null ? null : request.getOpinion();
            return Result.ok(gradingAssessmentService.approve(id, approver, opinion));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批驳回（审批人必填，审批记录留痕） */
    @PostMapping("/{id}/reject")
    public Result<GradingAssessment> reject(@PathVariable Long id,
                                            @RequestBody ApproveRequest request) {
        try {
            String approver = request == null ? null : request.getApprover();
            String opinion = request == null ? null : request.getOpinion();
            return Result.ok(gradingAssessmentService.reject(id, approver, opinion));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(GradingAssessmentQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("分级评定.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        gradingAssessmentService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（标准编码须存在且启用；重复或非法行跳过） */
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
            return Result.ok(gradingAssessmentService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("分级评定导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        gradingAssessmentService.writeImportTemplate(response.getOutputStream());
    }
}
