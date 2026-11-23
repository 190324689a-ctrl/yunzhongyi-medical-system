package com.yunzhongyi.medical.newtech.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.newtech.dto.NewTechApprovalRequest;
import com.yunzhongyi.medical.newtech.dto.NewTechEthicsRequest;
import com.yunzhongyi.medical.newtech.dto.NewTechQuery;
import com.yunzhongyi.medical.newtech.entity.NewTechProject;
import com.yunzhongyi.medical.newtech.entity.NewTechRecord;
import com.yunzhongyi.medical.newtech.service.NewTechService;
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
 * 新技术、新项目管理接口：申报查询/增删改、一级审批（医务科）、伦理审查、
 * 进展/验收/推广记录（驱动状态流转）、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/new-techs")
@RequiredArgsConstructor
public class NewTechController {

    private final NewTechService newTechService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(NewTechQuery query) {
        return Result.ok(newTechService.pageQuery(query));
    }

    /** 新技术、新项目统计：总数 / 待审批数 / 已立项数 / 已验收数 / 已推广数 / 已驳回数 / 待伦理审查数 / 按项目类型分组 / 按科室汇总 / 按月趋势 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(newTechService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(newTechService.statsByDept());
    }

    /** 按月趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(newTechService.statsByMonth());
    }

    /** 详情（含全部进展记录） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        try {
            return Result.ok(newTechService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增申报（默认待审批；需伦理审查的项目默认伦理待审查） */
    @PostMapping
    public Result<NewTechProject> create(@Valid @RequestBody NewTechProject project) {
        try {
            newTechService.save(project);
            return Result.ok(project);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待审批的项目能修改申报内容） */
    @PutMapping("/{id}")
    public Result<NewTechProject> update(@PathVariable Long id, @Valid @RequestBody NewTechProject project) {
        try {
            project.setId(id);
            newTechService.updateById(project);
            return Result.ok(project);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        newTechService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        newTechService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 一级审批通过（医务科）：只有待审批的项目能通过，需伦理审查的须伦理通过后才能立项，审批记录留痕 */
    @PostMapping("/{id}/approve")
    public Result<NewTechProject> approve(@PathVariable Long id,
                                          @RequestBody(required = false) NewTechApprovalRequest request) {
        String approver = request == null ? null : request.getApprover();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(newTechService.approve(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批驳回（医务科）：只有待审批的项目能驳回，审批记录留痕 */
    @PostMapping("/{id}/reject")
    public Result<NewTechProject> reject(@PathVariable Long id,
                                         @RequestBody(required = false) NewTechApprovalRequest request) {
        String approver = request == null ? null : request.getApprover();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(newTechService.reject(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 伦理审查：只有需伦理审查且待审查的项目能审查 */
    @PostMapping("/{id}/ethics")
    public Result<NewTechProject> ethicsReview(@PathVariable Long id,
                                               @RequestBody(required = false) NewTechEthicsRequest request) {
        Boolean passed = request == null ? null : request.getPassed();
        String reviewer = request == null ? null : request.getReviewer();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(newTechService.ethicsReview(id, passed, reviewer, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 查某项目的全部进展记录 */
    @GetMapping("/{id}/records")
    public Result<List<NewTechRecord>> listRecords(@PathVariable Long id) {
        try {
            return Result.ok(newTechService.listRecords(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 添加进展记录：验收记录使项目变为已验收，推广记录使项目变为已推广 */
    @PostMapping("/{id}/records")
    public Result<NewTechRecord> addRecord(@PathVariable Long id, @Valid @RequestBody NewTechRecord record) {
        try {
            return Result.ok(newTechService.addRecord(id, record));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除一条进展记录 */
    @DeleteMapping("/{id}/records/{recordId}")
    public Result<?> deleteRecord(@PathVariable Long id, @PathVariable Long recordId) {
        try {
            newTechService.removeRecord(id, recordId);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(NewTechQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("新技术新项目.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        newTechService.exportExcel(query, response.getOutputStream());
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
            int count = newTechService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("新技术新项目导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        newTechService.exportExcel(new NewTechQuery(), response.getOutputStream());
    }
}
