package com.yunzhongyi.medical.techgrading.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.techgrading.dto.ApproveRequest;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.dto.TechAdmissionQuery;
import com.yunzhongyi.medical.techgrading.entity.TechAdmission;
import com.yunzhongyi.medical.techgrading.service.TechAdmissionService;
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
 * 医疗技术准入申请管理接口：申请查询/增删改、一级审批（通过时同步技术档案）、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/tech-admissions")
@RequiredArgsConstructor
public class TechAdmissionController {

    private final TechAdmissionService techAdmissionService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(TechAdmissionQuery query) {
        return Result.ok(techAdmissionService.pageQuery(query));
    }

    /** 准入申请统计：总数 / 待审批 / 已通过 / 已驳回 / 按申请类型分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(techAdmissionService.stats());
    }

    /** 查某项技术的全部准入申请（按申请日期降序） */
    @GetMapping("/by-tech/{techId}")
    public Result<List<TechAdmission>> listByTech(@PathVariable Long techId) {
        return Result.ok(techAdmissionService.listByTech(techId));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<TechAdmission> get(@PathVariable Long id) {
        try {
            return Result.ok(techAdmissionService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增准入申请（默认待审批，申请日期默认为当天） */
    @PostMapping
    public Result<TechAdmission> create(@Valid @RequestBody TechAdmission techAdmission) {
        try {
            techAdmissionService.save(techAdmission);
            return Result.ok(techAdmission);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待审批的申请能修改） */
    @PutMapping("/{id}")
    public Result<TechAdmission> update(@PathVariable Long id, @Valid @RequestBody TechAdmission techAdmission) {
        try {
            techAdmission.setId(id);
            techAdmissionService.updateById(techAdmission);
            return Result.ok(techAdmission);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已通过的准入申请不能直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            techAdmissionService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            techAdmissionService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 审批通过（一级审批，医务科）：只有待审批的申请能通过，通过时同步技术档案 */
    @PostMapping("/{id}/approve")
    public Result<TechAdmission> approve(@PathVariable Long id,
                                         @RequestBody(required = false) ApproveRequest request) {
        try {
            return Result.ok(techAdmissionService.approve(id, request == null ? new ApproveRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 审批驳回（一级审批，医务科）：只有待审批的申请能驳回，驳回原因必填 */
    @PostMapping("/{id}/reject")
    public Result<TechAdmission> reject(@PathVariable Long id,
                                        @RequestBody(required = false) RejectRequest request) {
        try {
            return Result.ok(techAdmissionService.reject(id, request == null ? new RejectRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(TechAdmissionQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("技术准入申请.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        techAdmissionService.exportExcel(query, response.getOutputStream());
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
            int count = techAdmissionService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("技术准入申请导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        techAdmissionService.exportExcel(new TechAdmissionQuery(), response.getOutputStream());
    }
}
