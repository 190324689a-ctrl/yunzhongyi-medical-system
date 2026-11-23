package com.yunzhongyi.medical.physician.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.physician.dto.PhysicianQuery;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.service.PhysicianService;
import com.yunzhongyi.medical.system.service.SysOperLogService;
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
 * 医师档案管理接口
 */
@RestController
@RequestMapping("/api/physicians")
@RequiredArgsConstructor
public class PhysicianController {

    private final PhysicianService physicianService;
    private final SysOperLogService operLogService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(PhysicianQuery query) {
        return Result.ok(physicianService.pageQuery(query));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Physician> get(@PathVariable Long id) {
        return Result.ok(physicianService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Physician> create(@Valid @RequestBody Physician physician) {
        long t0 = System.currentTimeMillis();
        physicianService.save(physician);
        operLogService.record("admin", "physician", "create", "POST /api/physicians",
                "name=" + physician.getName(), true, System.currentTimeMillis() - t0, null);
        return Result.ok(physician);
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<Physician> update(@PathVariable Long id, @Valid @RequestBody Physician physician) {
        physician.setId(id);
        physicianService.updateById(physician);
        return Result.ok(physician);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        physicianService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        physicianService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(PhysicianQuery query, HttpServletResponse response) throws IOException {
        long t0 = System.currentTimeMillis();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医师档案.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        physicianService.exportExcel(query, response.getOutputStream());
        operLogService.record("admin", "physician", "export", "GET /api/physicians/export",
                "keyword=" + query.getKeyword(), true, System.currentTimeMillis() - t0, null);
    }

    /** 导入 Excel */
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
            int count = physicianService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医师档案导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        physicianService.exportExcel(new PhysicianQuery(), response.getOutputStream());
    }
}
