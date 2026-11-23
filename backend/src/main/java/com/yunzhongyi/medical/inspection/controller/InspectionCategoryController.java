package com.yunzhongyi.medical.inspection.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.inspection.dto.InspectionCategoryQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionCategory;
import com.yunzhongyi.medical.inspection.service.InspectionCategoryService;
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
 * 检查分类管理接口：分类查询/增删改、启用/停用、统计、分类树、批量导入导出
 */
@RestController
@RequestMapping("/api/inspection-categories")
@RequiredArgsConstructor
public class InspectionCategoryController {

    private final InspectionCategoryService inspectionCategoryService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(InspectionCategoryQuery query) {
        return Result.ok(inspectionCategoryService.pageQuery(query));
    }

    /** 检查分类统计：总数 / 已启用 / 已停用 / 一级 / 二级 / 按适用检查类型分组 / 归属项目数 / 有项目归属的分类数 / 已组装进模板的项目数 / 涉及模板数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(inspectionCategoryService.stats());
    }

    /** 按分类汇总统计（每个分类的项目数、已启用项目数、涉及模板数、下级数） */
    @GetMapping("/stats/by-category")
    public Result<List<Map<String, Object>>> statsByCategory() {
        return Result.ok(inspectionCategoryService.statsByCategory());
    }

    /** 分类树（一级分类带下级） */
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(inspectionCategoryService.tree());
    }

    /** 全部启用的分类（新建检查项目时下拉用） */
    @GetMapping("/enabled")
    public Result<List<InspectionCategory>> enabled() {
        return Result.ok(inspectionCategoryService.enabled());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<InspectionCategory> get(@PathVariable Long id) {
        try {
            return Result.ok(inspectionCategoryService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增检查分类（默认启用、适用类型默认通用） */
    @PostMapping
    public Result<InspectionCategory> create(@Valid @RequestBody InspectionCategory inspectionCategory) {
        try {
            inspectionCategoryService.save(inspectionCategory);
            return Result.ok(inspectionCategory);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改检查分类（已有项目归属时编码不可改） */
    @PutMapping("/{id}")
    public Result<InspectionCategory> update(@PathVariable Long id,
                                             @Valid @RequestBody InspectionCategory inspectionCategory) {
        try {
            inspectionCategory.setId(id);
            inspectionCategoryService.updateById(inspectionCategory);
            return Result.ok(inspectionCategory);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已有下级、或已有检查项目归属的分类不能直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            inspectionCategoryService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            inspectionCategoryService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用：只有停用的分类能启用；父分类停用时子分类不可启用 */
    @PostMapping("/{id}/enable")
    public Result<InspectionCategory> enable(@PathVariable Long id) {
        try {
            return Result.ok(inspectionCategoryService.enable(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用：只有启用的分类能停用；已有检查项目归属、或有启用下级的分类不可停用 */
    @PostMapping("/{id}/disable")
    public Result<InspectionCategory> disable(@PathVariable Long id) {
        try {
            return Result.ok(inspectionCategoryService.disable(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(InspectionCategoryQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("检查分类.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        inspectionCategoryService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的分类默认启用） */
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
            int count = inspectionCategoryService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("检查分类导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        inspectionCategoryService.exportExcel(new InspectionCategoryQuery(), response.getOutputStream());
    }
}
