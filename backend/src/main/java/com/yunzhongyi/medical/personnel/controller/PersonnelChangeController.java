package com.yunzhongyi.medical.personnel.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.personnel.dto.PersonnelChangeApprovalRequest;
import com.yunzhongyi.medical.personnel.dto.PersonnelChangeQuery;
import com.yunzhongyi.medical.personnel.entity.PersonnelChange;
import com.yunzhongyi.medical.personnel.service.PersonnelChangeService;
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
 * 医疗人员动态管理接口：人员变动登记查询/增删改、一级审批（医务科，
 * 通过时同步医师档案）、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/personnel-changes")
@RequiredArgsConstructor
public class PersonnelChangeController {

    private final PersonnelChangeService personnelChangeService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(PersonnelChangeQuery query) {
        return Result.ok(personnelChangeService.pageQuery(query));
    }

    /** 医疗人员动态统计：总数 / 待审批数 / 已通过数 / 已驳回数 / 按变动类型分组 / 按相关科室汇总 / 按月趋势 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(personnelChangeService.stats());
    }

    /** 按相关科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(personnelChangeService.statsByDept());
    }

    /** 按月趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(personnelChangeService.statsByMonth());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<PersonnelChange> get(@PathVariable Long id) {
        try {
            return Result.ok(personnelChangeService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增人员变动登记（默认待审批；申请日期默认为当天） */
    @PostMapping
    public Result<PersonnelChange> create(@Valid @RequestBody PersonnelChange change) {
        try {
            personnelChangeService.save(change);
            return Result.ok(change);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待审批的变动能修改） */
    @PutMapping("/{id}")
    public Result<PersonnelChange> update(@PathVariable Long id, @Valid @RequestBody PersonnelChange change) {
        try {
            change.setId(id);
            personnelChangeService.updateById(change);
            return Result.ok(change);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        personnelChangeService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        personnelChangeService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 一级审批通过（医务科）：只有待审批的变动能通过，审批记录留痕；通过时同步医师档案 */
    @PostMapping("/{id}/approve")
    public Result<PersonnelChange> approve(@PathVariable Long id,
                                           @RequestBody(required = false) PersonnelChangeApprovalRequest request) {
        String approver = request == null ? null : request.getApprover();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(personnelChangeService.approve(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批驳回（医务科）：只有待审批的变动能驳回，审批记录留痕 */
    @PostMapping("/{id}/reject")
    public Result<PersonnelChange> reject(@PathVariable Long id,
                                          @RequestBody(required = false) PersonnelChangeApprovalRequest request) {
        String approver = request == null ? null : request.getApprover();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(personnelChangeService.reject(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(PersonnelChangeQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医疗人员动态.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        personnelChangeService.exportExcel(query, response.getOutputStream());
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
            int count = personnelChangeService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医疗人员动态导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        personnelChangeService.exportExcel(new PersonnelChangeQuery(), response.getOutputStream());
    }
}
