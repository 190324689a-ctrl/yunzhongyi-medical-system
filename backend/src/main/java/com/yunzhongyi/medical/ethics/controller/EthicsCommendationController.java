package com.yunzhongyi.medical.ethics.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.ethics.dto.ApproveRequest;
import com.yunzhongyi.medical.ethics.dto.EthicsCommendationQuery;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsCommendation;
import com.yunzhongyi.medical.ethics.service.EthicsCommendationService;
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
 * 医德医风表彰接口：表彰登记、一级审批、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/ethics-commendations")
@RequiredArgsConstructor
public class EthicsCommendationController {

    private final EthicsCommendationService commendationService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(EthicsCommendationQuery query) {
        return Result.ok(commendationService.pageQuery(query));
    }

    /** 表彰统计：总数、待审批、已通过、已驳回、按表彰类型分组、按表彰级别分组、按科室汇总 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(commendationService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(commendationService.statsByDept());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<EthicsCommendation> get(@PathVariable Long id) {
        try {
            return Result.ok(commendationService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增表彰登记：默认待审批 */
    @PostMapping
    public Result<EthicsCommendation> create(@Valid @RequestBody EthicsCommendation commendation) {
        try {
            commendationService.save(commendation);
            return Result.ok(commendation);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待审批的表彰能修改） */
    @PutMapping("/{id}")
    public Result<EthicsCommendation> update(@PathVariable Long id, @Valid @RequestBody EthicsCommendation commendation) {
        try {
            commendation.setId(id);
            commendationService.updateById(commendation);
            return Result.ok(commendation);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已通过的表彰不能直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            commendationService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            commendationService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批通过：只有待审批的表彰能通过，审批记录留痕 */
    @PostMapping("/{id}/approve")
    public Result<EthicsCommendation> approve(@PathVariable Long id,
                                              @RequestBody(required = false) ApproveRequest request) {
        try {
            return Result.ok(commendationService.approve(id, request == null ? new ApproveRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批驳回：只有待审批的表彰能驳回，驳回原因必填 */
    @PostMapping("/{id}/reject")
    public Result<EthicsCommendation> reject(@PathVariable Long id,
                                             @RequestBody(required = false) RejectRequest request) {
        try {
            return Result.ok(commendationService.reject(id, request == null ? new RejectRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(EthicsCommendationQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医德医风表彰.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        commendationService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（同一医师同一表彰标题同一表彰日期重复跳过；按姓名查医师档案补全） */
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
            int count = commendationService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医德医风表彰导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        commendationService.exportExcel(new EthicsCommendationQuery(), response.getOutputStream());
    }
}
