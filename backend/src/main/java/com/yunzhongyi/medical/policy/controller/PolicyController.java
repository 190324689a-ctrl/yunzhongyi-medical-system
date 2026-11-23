package com.yunzhongyi.medical.policy.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.policy.dto.AbolishRequest;
import com.yunzhongyi.medical.policy.dto.PolicyQuery;
import com.yunzhongyi.medical.policy.dto.PublishRequest;
import com.yunzhongyi.medical.policy.entity.Policy;
import com.yunzhongyi.medical.policy.service.PolicyService;
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
 * 政策法规管理接口：法规查询/增删改、发布/废止/新建版本、在线阅读（详情含正文）、
 * 文本下载、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(PolicyQuery query) {
        return Result.ok(policyService.pageQuery(query));
    }

    /** 法规统计：总数 / 草稿 / 已发布 / 已废止 / 按分类分组 / 按层级分组 / 30 天内到期 / 已过期 / 查阅学习统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(policyService.stats());
    }

    /** 按法规分类汇总统计 */
    @GetMapping("/stats/by-category")
    public Result<List<Map<String, Object>>> statsByCategory() {
        return Result.ok(policyService.statsByCategory());
    }

    /** 按发布层级汇总统计 */
    @GetMapping("/stats/by-level")
    public Result<List<Map<String, Object>>> statsByLevel() {
        return Result.ok(policyService.statsByLevel());
    }

    /** 按月发布趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(policyService.statsByMonth());
    }

    /** 详情（含正文，用于在线阅读） */
    @GetMapping("/{id}")
    public Result<Policy> get(@PathVariable Long id) {
        try {
            return Result.ok(policyService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增法规（默认草稿、V1.0、适用科室默认“全院”） */
    @PostMapping
    public Result<Policy> create(@Valid @RequestBody Policy policy) {
        try {
            policyService.save(policy);
            return Result.ok(policy);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有草稿能修改；已发布的法规更新走新建版本） */
    @PutMapping("/{id}")
    public Result<Policy> update(@PathVariable Long id, @Valid @RequestBody Policy policy) {
        try {
            policy.setId(id);
            policyService.updateById(policy);
            return Result.ok(policy);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已发布的法规不能直接删除，请先废止；同时删除其查阅学习记录） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            policyService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            policyService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 发布（一级审批）：只有草稿能发布，审批人必填 */
    @PostMapping("/{id}/publish")
    public Result<Policy> publish(@PathVariable Long id,
                                  @RequestBody(required = false) PublishRequest request) {
        try {
            return Result.ok(policyService.publish(id, request == null ? new PublishRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 废止：只有已发布的法规能废止，废止原因必填 */
    @PostMapping("/{id}/abolish")
    public Result<Policy> abolish(@PathVariable Long id,
                                  @RequestBody(required = false) AbolishRequest request) {
        try {
            return Result.ok(policyService.abolish(id, request == null ? new AbolishRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新建版本：只有已发布的法规能新建版本，生成草稿记录（版本号 +1） */
    @PostMapping("/{id}/new-version")
    public Result<Policy> newVersion(@PathVariable Long id) {
        try {
            return Result.ok(policyService.newVersion(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 下载：按正文生成 .txt 文件（不引入真实文件存储） */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Policy p;
        try {
            p = policyService.detail(id);
        } catch (IllegalArgumentException e) {
            response.sendError(400, e.getMessage());
            return;
        }
        String filename = p.getPolicyNo() + "_" + (p.getVersion() == null ? "" : p.getVersion()) + ".txt";
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        response.getOutputStream().write(policyService.downloadText(id).getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(PolicyQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("政策法规.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        policyService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的均为草稿，需走发布审批流程） */
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
            int count = policyService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("政策法规导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        policyService.exportExcel(new PolicyQuery(), response.getOutputStream());
    }
}
