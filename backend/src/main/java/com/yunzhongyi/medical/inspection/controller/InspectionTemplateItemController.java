package com.yunzhongyi.medical.inspection.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.inspection.dto.InspectionTemplateItemQuery;
import com.yunzhongyi.medical.inspection.dto.TemplateItemConfigRequest;
import com.yunzhongyi.medical.inspection.entity.InspectionTemplateItem;
import com.yunzhongyi.medical.inspection.service.InspectionTemplateItemService;
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
 * 模板检查项配置接口：把检查项目库里的项目组装进质量检查模板，
 * 配置保存后同步回写模板的 checkItems JSON
 */
@RestController
@RequestMapping("/api/inspection-template-items")
@RequiredArgsConstructor
public class InspectionTemplateItemController {

    private final InspectionTemplateItemService inspectionTemplateItemService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(InspectionTemplateItemQuery query) {
        return Result.ok(inspectionTemplateItemService.pageQuery(query));
    }

    /** 模板配置统计：已配置模板数 / 配置记录总数 / 平均每模板检查项数 / 未配置检查项的模板数 / 未组装进任何模板的项目数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(inspectionTemplateItemService.stats());
    }

    /** 查某模板的全部检查项（按排序号升序） */
    @GetMapping("/by-template/{templateId}")
    public Result<List<InspectionTemplateItem>> listByTemplate(@PathVariable Long templateId) {
        return Result.ok(inspectionTemplateItemService.listByTemplate(templateId));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<InspectionTemplateItem> get(@PathVariable Long id) {
        try {
            return Result.ok(inspectionTemplateItemService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 配置模板检查项（整模板替换，权重之和须为 100，保存后同步回写模板 JSON） */
    @PostMapping("/configure")
    public Result<List<InspectionTemplateItem>> configure(
            @Valid @RequestBody TemplateItemConfigRequest request) {
        try {
            return Result.ok(inspectionTemplateItemService.configure(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除单项（一个模板至少保留一项，删除后同步回写模板 JSON） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            inspectionTemplateItemService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            inspectionTemplateItemService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出指定模板的检查项 Excel */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) Long templateId,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("模板检查项.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        inspectionTemplateItemService.exportExcel(templateId, response.getOutputStream());
    }

    /** 批量导入指定模板的检查项 Excel（整模板替换，权重之和须为 100） */
    @PostMapping("/import")
    public Result<?> importExcel(@RequestParam Long templateId,
                                 @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "请上传 Excel 文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.endsWith(".xlsx") || name.endsWith(".xls"))) {
            return Result.fail(400, "只支持 .xlsx / .xls 文件");
        }
        try {
            int count = inspectionTemplateItemService.importExcel(templateId, file.getInputStream());
            return Result.ok(Map.of("imported", count));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(@RequestParam(required = false) Long templateId,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("模板检查项导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        inspectionTemplateItemService.exportExcel(templateId, response.getOutputStream());
    }
}
