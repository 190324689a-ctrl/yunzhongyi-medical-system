package com.yunzhongyi.medical.system.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.system.entity.SysDept;
import com.yunzhongyi.medical.system.service.SysDeptService;
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
 * 科室管理接口。
 * <p>
 * 第 28 项补强：科室编码唯一、启用/停用、科室统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    /** 分页查询 */
    @GetMapping
    public Result<?> page(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(deptService.page(keyword, type, status, page, size));
    }

    /** 科室统计：总数 / 启用 / 停用 / 按类型分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(deptService.stats());
    }

    /** 全部启用科室（下拉框） */
    @GetMapping("/enabled")
    public Result<List<SysDept>> enabled() {
        return Result.ok(deptService.listEnabled());
    }

    /** 科室树（层级展示与角色绑定科室时勾选用） */
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(deptService.tree());
    }

    @GetMapping("/{id}")
    public Result<SysDept> get(@PathVariable Long id) {
        return Result.ok(deptService.getById(id));
    }

    @PostMapping
    public Result<SysDept> create(@Valid @RequestBody SysDept dept) {
        try {
            return Result.ok(deptService.create(dept));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<SysDept> update(@PathVariable Long id, @Valid @RequestBody SysDept dept) {
        try {
            dept.setId(id);
            return Result.ok(deptService.update(dept));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            deptService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<SysDept> enable(@PathVariable Long id) {
        try {
            return Result.ok(deptService.enable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用（有下级科室的不可停用） */
    @PostMapping("/{id}/disable")
    public Result<SysDept> disable(@PathVariable Long id) {
        try {
            return Result.ok(deptService.disable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("科室.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        deptService.exportExcel(keyword, type, status, response.getOutputStream());
    }

    /** 批量导入 Excel（编码重复或非法行跳过） */
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
            return Result.ok(deptService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("科室导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        deptService.writeImportTemplate(response.getOutputStream());
    }
}
