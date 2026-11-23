package com.yunzhongyi.medical.statistics.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.statistics.dto.WorkloadQuery;
import com.yunzhongyi.medical.statistics.entity.WorkloadRecord;
import com.yunzhongyi.medical.statistics.service.WorkloadService;
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
 * 工作统计接口：第 24 项，工作量登记查询 / 增删改、按科室 / 按人员 / 按月统计、
 * 批量导入导出
 */
@RestController
@RequestMapping("/api/workloads")
@RequiredArgsConstructor
public class WorkloadController {

    private final WorkloadService workloadService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(WorkloadQuery query) {
        return Result.ok(workloadService.pageQuery(query));
    }

    /** 工作统计：总量 / 记录数 / 按科室 / 按人员 / 按月汇总 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(workloadService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(workloadService.statsByDept());
    }

    /** 按人员汇总统计 */
    @GetMapping("/stats/by-physician")
    public Result<List<Map<String, Object>>> statsByPhysician() {
        return Result.ok(workloadService.statsByPhysician());
    }

    /** 按月趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(workloadService.statsByMonth());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<WorkloadRecord> get(@PathVariable Long id) {
        WorkloadRecord record = workloadService.getById(id);
        if (record == null) {
            return Result.fail(400, "工作量登记不存在：" + id);
        }
        return Result.ok(record);
    }

    /** 新增登记（同一医师同一统计月份只允许一条） */
    @PostMapping
    public Result<WorkloadRecord> create(@Valid @RequestBody WorkloadRecord record) {
        try {
            workloadService.save(record);
            return Result.ok(record);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<WorkloadRecord> update(@PathVariable Long id, @Valid @RequestBody WorkloadRecord record) {
        try {
            record.setId(id);
            workloadService.updateById(record);
            return Result.ok(record);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        workloadService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        workloadService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(WorkloadQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("工作量登记.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        workloadService.exportExcel(query, response.getOutputStream());
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
            int count = workloadService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("工作量登记导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        workloadService.exportExcel(new WorkloadQuery(), response.getOutputStream());
    }
}
