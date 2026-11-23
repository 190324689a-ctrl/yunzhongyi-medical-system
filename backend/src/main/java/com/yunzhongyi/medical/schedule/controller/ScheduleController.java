package com.yunzhongyi.medical.schedule.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.schedule.dto.ScheduleChangeRequest;
import com.yunzhongyi.medical.schedule.dto.ScheduleGenerateRequest;
import com.yunzhongyi.medical.schedule.dto.ScheduleQuery;
import com.yunzhongyi.medical.schedule.entity.Schedule;
import com.yunzhongyi.medical.schedule.service.ScheduleService;
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
 * 排班管理接口：排班查询/增删改、按模板批量生成、调班/代班一级审批、
 * 统计、批量导入导出、HIS 同步预留
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(ScheduleQuery query) {
        return Result.ok(scheduleService.pageQuery(query));
    }

    /** 排班统计：总数 / 待审批调班代班数 / 夜班人次 / 按类型分组 / 按班次分组 / 按科室汇总 / 按人员汇总 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(scheduleService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(scheduleService.statsByDept());
    }

    /** 按人员汇总统计（排班天数、夜班天数） */
    @GetMapping("/stats/by-physician")
    public Result<List<Map<String, Object>>> statsByPhysician() {
        return Result.ok(scheduleService.statsByPhysician());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Schedule> get(@PathVariable Long id) {
        return Result.ok(scheduleService.getById(id));
    }

    /** 新增排班（同一医师同一天不能重复排班；班次时间段缺失时按班次默认时间补全） */
    @PostMapping
    public Result<Schedule> create(@Valid @RequestBody Schedule schedule) {
        try {
            scheduleService.save(schedule);
            return Result.ok(schedule);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<Schedule> update(@PathVariable Long id, @Valid @RequestBody Schedule schedule) {
        try {
            schedule.setId(id);
            scheduleService.updateById(schedule);
            return Result.ok(schedule);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        scheduleService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        scheduleService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 按模板批量生成排班（指定日期范围 + 科室/人员；冲突跳过并计数返回） */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@Valid @RequestBody ScheduleGenerateRequest request) {
        try {
            return Result.ok(scheduleService.generateByTemplate(request));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 申请调班（换班次/换日期）：进入待科主任审批 */
    @PostMapping("/{id}/swap")
    public Result<Schedule> applySwap(@PathVariable Long id, @RequestBody ScheduleChangeRequest request) {
        try {
            return Result.ok(scheduleService.applySwap(id, request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 申请代班（换人）：进入待科主任审批 */
    @PostMapping("/{id}/substitute")
    public Result<Schedule> applySubstitute(@PathVariable Long id, @RequestBody ScheduleChangeRequest request) {
        try {
            return Result.ok(scheduleService.applySubstitute(id, request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批通过（科主任）：调班改日期/班次，代班改人员；调班/代班后仍校验不冲突 */
    @PostMapping("/{id}/approve-change")
    public Result<Schedule> approveChange(@PathVariable Long id,
                                          @RequestBody(required = false) Map<String, String> body) {
        String approver = body == null ? null : body.get("approver");
        String opinion = body == null ? null : body.get("opinion");
        try {
            return Result.ok(scheduleService.approveChange(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审批驳回（科主任）：原排班不变 */
    @PostMapping("/{id}/reject-change")
    public Result<Schedule> rejectChange(@PathVariable Long id,
                                         @RequestBody(required = false) Map<String, String> body) {
        String approver = body == null ? null : body.get("approver");
        String opinion = body == null ? null : body.get("opinion");
        try {
            return Result.ok(scheduleService.rejectChange(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 同步单条排班到 HIS（预留占位）。
     * 目前没有真实 HIS 系统可连、HIS 对接默认未启用，调用会明确返回 501
     * “HIS 对接未启用”，绝不伪造同步成功。
     */
    @PostMapping("/{id}/his-sync")
    public Result<?> syncToHis(@PathVariable Long id) {
        try {
            return Result.ok(scheduleService.syncToHis(id));
        } catch (UnsupportedOperationException e) {
            return Result.fail(501, e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(ScheduleQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("排班记录.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        scheduleService.exportExcel(query, response.getOutputStream());
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
            int count = scheduleService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("排班导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        scheduleService.exportExcel(new ScheduleQuery(), response.getOutputStream());
    }
}
