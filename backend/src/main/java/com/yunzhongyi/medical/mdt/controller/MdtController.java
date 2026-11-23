package com.yunzhongyi.medical.mdt.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.mdt.dto.MdtApprovalRequest;
import com.yunzhongyi.medical.mdt.dto.MdtQuery;
import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import com.yunzhongyi.medical.mdt.entity.MdtRecord;
import com.yunzhongyi.medical.mdt.service.MdtService;
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
 * MDT 多学科会诊管理接口：申请查询/增删改、一级审批（医务科/科主任）、
 * 会诊记录（可多次补充）、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/mdts")
@RequiredArgsConstructor
public class MdtController {

    private final MdtService mdtService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(MdtQuery query) {
        return Result.ok(mdtService.pageQuery(query));
    }

    /** MDT 统计：总数 / 待审批数 / 已通过数 / 已驳回数 / 紧急会诊数 / 按会诊类型分组 / 按科室汇总 / 按月趋势 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(mdtService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(mdtService.statsByDept());
    }

    /** 按月趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(mdtService.statsByMonth());
    }

    /** 详情（含全部会诊记录） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        try {
            return Result.ok(mdtService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增 MDT 申请（默认待审批；会诊类型/紧急程度/期望会诊时间必填） */
    @PostMapping
    public Result<MdtApplication> create(@Valid @RequestBody MdtApplication application) {
        try {
            mdtService.save(application);
            return Result.ok(application);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待审批的申请能修改） */
    @PutMapping("/{id}")
    public Result<MdtApplication> update(@PathVariable Long id, @Valid @RequestBody MdtApplication application) {
        try {
            application.setId(id);
            mdtService.updateById(application);
            return Result.ok(application);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        mdtService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        mdtService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 一级审批通过（医务科/科主任）：只有待审批的申请能通过，审批记录留痕 */
    @PostMapping("/{id}/approve")
    public Result<MdtApplication> approve(@PathVariable Long id,
                                          @RequestBody(required = false) MdtApprovalRequest request) {
        String approver = request == null ? null : request.getApprover();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(mdtService.approve(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批驳回（医务科/科主任）：只有待审批的申请能驳回，审批记录留痕 */
    @PostMapping("/{id}/reject")
    public Result<MdtApplication> reject(@PathVariable Long id,
                                         @RequestBody(required = false) MdtApprovalRequest request) {
        String approver = request == null ? null : request.getApprover();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(mdtService.reject(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 查某申请的全部会诊记录 */
    @GetMapping("/{id}/records")
    public Result<List<MdtRecord>> listRecords(@PathVariable Long id) {
        try {
            return Result.ok(mdtService.listRecords(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 添加会诊记录：只有审批通过的申请才能记录，可多次补充 */
    @PostMapping("/{id}/records")
    public Result<MdtRecord> addRecord(@PathVariable Long id, @Valid @RequestBody MdtRecord record) {
        try {
            return Result.ok(mdtService.addRecord(id, record));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除一条会诊记录 */
    @DeleteMapping("/{id}/records/{recordId}")
    public Result<?> deleteRecord(@PathVariable Long id, @PathVariable Long recordId) {
        try {
            mdtService.removeRecord(id, recordId);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(MdtQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("MDT会诊申请.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        mdtService.exportExcel(query, response.getOutputStream());
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
            int count = mdtService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("MDT导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        mdtService.exportExcel(new MdtQuery(), response.getOutputStream());
    }
}
