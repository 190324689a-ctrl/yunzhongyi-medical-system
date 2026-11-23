package com.yunzhongyi.medical.authorization.controller;

import com.yunzhongyi.medical.authorization.dto.AuthorizationQuery;
import com.yunzhongyi.medical.authorization.entity.Authorization;
import com.yunzhongyi.medical.authorization.service.AuthorizationService;
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
 * 重点授权管理接口：在线申请、多级审批、查询、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/authorizations")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(AuthorizationQuery query) {
        return Result.ok(authorizationService.pageQuery(query));
    }

    /** 统计：总数 / 待审批 / 已通过 / 已驳回 / 按类型分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(authorizationService.stats());
    }

    /** 详情（打印预览用） */
    @GetMapping("/{id}")
    public Result<Authorization> get(@PathVariable Long id) {
        return Result.ok(authorizationService.getById(id));
    }

    /** 在线申请 */
    @PostMapping
    public Result<Authorization> create(@Valid @RequestBody Authorization authorization) {
        authorizationService.save(authorization);
        return Result.ok(authorization);
    }

    /** 修改（仅待审批的申请可改，由前端控制；后端不拦截已通过的编辑以便补录） */
    @PutMapping("/{id}")
    public Result<Authorization> update(@PathVariable Long id, @Valid @RequestBody Authorization authorization) {
        authorization.setId(id);
        authorizationService.updateById(authorization);
        return Result.ok(authorization);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        authorizationService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        authorizationService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 多级审批：通过当前一级（科主任 → 医务科） */
    @PostMapping("/{id}/approve")
    public Result<Authorization> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String approver = body == null ? null : body.get("approver");
        String opinion = body == null ? null : body.get("opinion");
        try {
            return Result.ok(authorizationService.approve(id, approver, opinion));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 多级审批：驳回 */
    @PostMapping("/{id}/reject")
    public Result<Authorization> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String approver = body == null ? null : body.get("approver");
        String opinion = body == null ? null : body.get("opinion");
        try {
            return Result.ok(authorizationService.reject(id, approver, opinion));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(AuthorizationQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("重点授权.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        authorizationService.exportExcel(query, response.getOutputStream());
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
            int count = authorizationService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("重点授权导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        authorizationService.exportExcel(new AuthorizationQuery(), response.getOutputStream());
    }
}
