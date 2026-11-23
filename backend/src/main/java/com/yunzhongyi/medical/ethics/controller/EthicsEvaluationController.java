package com.yunzhongyi.medical.ethics.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.ethics.dto.ApproveRequest;
import com.yunzhongyi.medical.ethics.dto.EthicsEvaluationQuery;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsEvaluation;
import com.yunzhongyi.medical.ethics.service.EthicsEvaluationService;
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
 * 医德考评接口：评分录入（总分与等级自动计算）、一级审批、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/ethics-evaluations")
@RequiredArgsConstructor
public class EthicsEvaluationController {

    private final EthicsEvaluationService evaluationService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(EthicsEvaluationQuery query) {
        return Result.ok(evaluationService.pageQuery(query));
    }

    /** 评分统计：总数、平均分、优秀人数与优秀率、按等级分组、按科室汇总 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(evaluationService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(evaluationService.statsByDept());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<EthicsEvaluation> get(@PathVariable Long id) {
        try {
            return Result.ok(evaluationService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增考评记录：由各项得分求和自动算总分，再按总分自动算等级；默认待审批 */
    @PostMapping
    public Result<EthicsEvaluation> create(@Valid @RequestBody EthicsEvaluation evaluation) {
        try {
            evaluationService.save(evaluation);
            return Result.ok(evaluation);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待审批的考评能修改；重新录入得分后总分与等级自动重算） */
    @PutMapping("/{id}")
    public Result<EthicsEvaluation> update(@PathVariable Long id, @Valid @RequestBody EthicsEvaluation evaluation) {
        try {
            evaluation.setId(id);
            evaluationService.updateById(evaluation);
            return Result.ok(evaluation);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已通过的考评不能直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            evaluationService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            evaluationService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批通过：只有待审批的考评能通过；通过时同步医德档案的最近考评结果 */
    @PostMapping("/{id}/approve")
    public Result<EthicsEvaluation> approve(@PathVariable Long id,
                                            @RequestBody(required = false) ApproveRequest request) {
        try {
            return Result.ok(evaluationService.approve(id, request == null ? new ApproveRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批驳回：只有待审批的考评能驳回，驳回原因必填 */
    @PostMapping("/{id}/reject")
    public Result<EthicsEvaluation> reject(@PathVariable Long id,
                                           @RequestBody(required = false) RejectRequest request) {
        try {
            return Result.ok(evaluationService.reject(id, request == null ? new RejectRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(EthicsEvaluationQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医德考评.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        evaluationService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（同一医师同一模板同一年度重复跳过；按姓名查医师档案补全） */
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
            int count = evaluationService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医德考评导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        evaluationService.exportExcel(new EthicsEvaluationQuery(), response.getOutputStream());
    }
}
