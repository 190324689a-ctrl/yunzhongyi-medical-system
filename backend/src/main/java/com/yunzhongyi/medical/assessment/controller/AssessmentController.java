package com.yunzhongyi.medical.assessment.controller;

import com.yunzhongyi.medical.assessment.dto.AssessmentQuery;
import com.yunzhongyi.medical.assessment.entity.Assessment;
import com.yunzhongyi.medical.assessment.service.AssessmentService;
import com.yunzhongyi.medical.common.Result;
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
 * 医师考核管理接口：评分录入（总分与等级自动计算）、查询、评分统计、批量导入导出
 */
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(AssessmentQuery query) {
        return Result.ok(assessmentService.pageQuery(query));
    }

    /** 评分统计：总数 / 平均分 / 各等级人数与优秀率 / 按等级分组 / 按科室汇总 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(assessmentService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(assessmentService.statsByDept());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Assessment> get(@PathVariable Long id) {
        return Result.ok(assessmentService.getById(id));
    }

    /** 新增考核记录（总分与等级由各项得分自动计算） */
    @PostMapping
    public Result<Assessment> create(@Valid @RequestBody Assessment assessment) {
        try {
            assessmentService.save(assessment);
            return Result.ok(assessment);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（重新录入得分后总分与等级自动重算） */
    @PutMapping("/{id}")
    public Result<Assessment> update(@PathVariable Long id, @Valid @RequestBody Assessment assessment) {
        try {
            assessment.setId(id);
            assessmentService.updateById(assessment);
            return Result.ok(assessment);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        assessmentService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        assessmentService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(AssessmentQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医师考核.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        assessmentService.exportExcel(query, response.getOutputStream());
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
            int count = assessmentService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医师考核导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        assessmentService.exportExcel(new AssessmentQuery(), response.getOutputStream());
    }
}
