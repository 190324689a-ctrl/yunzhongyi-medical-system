package com.yunzhongyi.medical.inspection.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.inspection.dto.InspectionItemQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionItem;
import com.yunzhongyi.medical.inspection.service.InspectionItemService;
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
 * 检查项目管理接口：项目查询/增删改、启用/停用、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/inspection-items")
@RequiredArgsConstructor
public class InspectionItemController {

    private final InspectionItemService inspectionItemService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(InspectionItemQuery query) {
        return Result.ok(inspectionItemService.pageQuery(query));
    }

    /** 检查项目统计：总数 / 已启用 / 已停用 / 按项目分类分组 / 按检查方式分组 / 按适用检查类型分组 / 已组装进模板数 / 未组装进模板数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(inspectionItemService.stats());
    }

    /** 按项目分类汇总统计 */
    @GetMapping("/stats/by-category")
    public Result<List<Map<String, Object>>> statsByCategory() {
        return Result.ok(inspectionItemService.statsByCategory());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<InspectionItem> get(@PathVariable Long id) {
        try {
            return Result.ok(inspectionItemService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增检查项目（默认启用） */
    @PostMapping
    public Result<InspectionItem> create(@Valid @RequestBody InspectionItem inspectionItem) {
        try {
            inspectionItemService.save(inspectionItem);
            return Result.ok(inspectionItem);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改检查项目 */
    @PutMapping("/{id}")
    public Result<InspectionItem> update(@PathVariable Long id, @Valid @RequestBody InspectionItem inspectionItem) {
        try {
            inspectionItem.setId(id);
            inspectionItemService.updateById(inspectionItem);
            return Result.ok(inspectionItem);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已被模板引用的项目不能直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            inspectionItemService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            inspectionItemService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用：只有停用的项目能启用 */
    @PostMapping("/{id}/enable")
    public Result<InspectionItem> enable(@PathVariable Long id) {
        try {
            return Result.ok(inspectionItemService.enable(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用：只有启用的项目能停用；已被模板引用的项目不可停用 */
    @PostMapping("/{id}/disable")
    public Result<InspectionItem> disable(@PathVariable Long id) {
        try {
            return Result.ok(inspectionItemService.disable(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(InspectionItemQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("检查项目.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        inspectionItemService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的项目默认启用） */
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
            int count = inspectionItemService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("检查项目导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        inspectionItemService.exportExcel(new InspectionItemQuery(), response.getOutputStream());
    }
}
