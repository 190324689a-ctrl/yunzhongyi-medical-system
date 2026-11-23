package com.yunzhongyi.medical.techgrading.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.techgrading.dto.ApproveRequest;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.dto.TechAuthorizationQuery;
import com.yunzhongyi.medical.techgrading.entity.TechAuthorization;
import com.yunzhongyi.medical.techgrading.service.TechAuthorizationService;
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
 * 医师医疗技术授权管理接口：授权查询/增删改、一级审批、注销、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/tech-authorizations")
@RequiredArgsConstructor
public class TechAuthorizationController {

    private final TechAuthorizationService techAuthorizationService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(TechAuthorizationQuery query) {
        return Result.ok(techAuthorizationService.pageQuery(query));
    }

    /** 授权统计：总数 / 待审批 / 已通过 / 已驳回 / 已注销 / 按授权级别分组 / 90 天内到期 / 已过期 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(techAuthorizationService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(techAuthorizationService.statsByDept());
    }

    /** 查某项技术的全部授权（按授权开始日期降序） */
    @GetMapping("/by-tech/{techId}")
    public Result<List<TechAuthorization>> listByTech(@PathVariable Long techId) {
        return Result.ok(techAuthorizationService.listByTech(techId));
    }

    /** 查某位医师的全部授权（按授权开始日期降序） */
    @GetMapping("/by-physician/{physicianId}")
    public Result<List<TechAuthorization>> listByPhysician(@PathVariable Long physicianId) {
        return Result.ok(techAuthorizationService.listByPhysician(physicianId));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<TechAuthorization> get(@PathVariable Long id) {
        try {
            return Result.ok(techAuthorizationService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增授权申请（默认待审批，授权开始日期默认为当天） */
    @PostMapping
    public Result<TechAuthorization> create(@Valid @RequestBody TechAuthorization techAuthorization) {
        try {
            techAuthorizationService.save(techAuthorization);
            return Result.ok(techAuthorization);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待审批的授权能修改） */
    @PutMapping("/{id}")
    public Result<TechAuthorization> update(@PathVariable Long id, @Valid @RequestBody TechAuthorization techAuthorization) {
        try {
            techAuthorization.setId(id);
            techAuthorizationService.updateById(techAuthorization);
            return Result.ok(techAuthorization);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已通过的授权不能直接删除，请先注销） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            techAuthorizationService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            techAuthorizationService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 审批通过（一级审批，医务科）：只有待审批的授权能通过 */
    @PostMapping("/{id}/approve")
    public Result<TechAuthorization> approve(@PathVariable Long id,
                                             @RequestBody(required = false) ApproveRequest request) {
        try {
            return Result.ok(techAuthorizationService.approve(id, request == null ? new ApproveRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 审批驳回（一级审批，医务科）：只有待审批的授权能驳回，驳回原因必填 */
    @PostMapping("/{id}/reject")
    public Result<TechAuthorization> reject(@PathVariable Long id,
                                            @RequestBody(required = false) RejectRequest request) {
        try {
            return Result.ok(techAuthorizationService.reject(id, request == null ? new RejectRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 注销：只有已通过的授权能注销，注销原因必填 */
    @PostMapping("/{id}/revoke")
    public Result<TechAuthorization> revoke(@PathVariable Long id,
                                            @RequestBody(required = false) RejectRequest request) {
        try {
            return Result.ok(techAuthorizationService.revoke(id, request == null ? new RejectRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(TechAuthorizationQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医师技术授权.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        techAuthorizationService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的均为待审批） */
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
            int count = techAuthorizationService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医师技术授权导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        techAuthorizationService.exportExcel(new TechAuthorizationQuery(), response.getOutputStream());
    }
}
