package com.yunzhongyi.medical.templatelib.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.templatelib.dto.TemplateCopyRequest;
import com.yunzhongyi.medical.templatelib.dto.TemplateVersionRequest;
import com.yunzhongyi.medical.templatelib.entity.TemplateVersion;
import com.yunzhongyi.medical.templatelib.service.TemplateLibraryService;
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
 * 检查模板管理（模板库）接口：版本管理、复制、使用情况、统计、导入导出。
 * <p>
 * 模板本身的增删改与启用/停用见 /api/quality-templates（质量检查管理模块）。
 */
@RestController
@RequestMapping("/api/template-library")
@RequiredArgsConstructor
public class TemplateLibraryController {

    private final TemplateLibraryService templateLibraryService;

    /** 模板库统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(templateLibraryService.stats());
    }

    /** 某模板的使用情况：计划数（按状态）、记录数、最近一次计划日期 */
    @GetMapping("/templates/{id}/usage")
    public Result<Map<String, Object>> usage(@PathVariable Long id) {
        try {
            return Result.ok(templateLibraryService.usage(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 复制模板：同类型同检查项的新模板，默认停用 */
    @PostMapping("/templates/{id}/copy")
    public Result<QualityTemplate> copy(@PathVariable Long id,
                                        @Valid @RequestBody TemplateCopyRequest request) {
        try {
            return Result.ok(templateLibraryService.copyTemplate(id, request.getTemplateName()));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 某模板的版本历史（按版本号倒序） */
    @GetMapping("/templates/{id}/versions")
    public Result<List<TemplateVersion>> listVersions(@PathVariable Long id) {
        try {
            return Result.ok(templateLibraryService.listVersions(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 某模板的当前版本 */
    @GetMapping("/templates/{id}/versions/current")
    public Result<TemplateVersion> currentVersion(@PathVariable Long id) {
        try {
            return Result.ok(templateLibraryService.currentVersion(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新建版本：检查项权重之和须为 100；有进行中计划时拒绝 */
    @PostMapping("/templates/{id}/versions")
    public Result<TemplateVersion> createVersion(@PathVariable Long id,
                                                 @Valid @RequestBody TemplateVersionRequest request) {
        try {
            return Result.ok(templateLibraryService.createVersion(id, request.getCheckItems(),
                    request.getChangeRemark()));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 版本详情 */
    @GetMapping("/versions/{id}")
    public Result<TemplateVersion> getVersion(@PathVariable Long id) {
        TemplateVersion v = templateLibraryService.getById(id);
        if (v == null) {
            return Result.fail(400, "模板版本不存在：" + id);
        }
        return Result.ok(v);
    }

    /** 从历史版本恢复：以该版本快照内容新建一版；有进行中计划时拒绝 */
    @PostMapping("/versions/{id}/restore")
    public Result<TemplateVersion> restoreVersion(@PathVariable Long id,
                                                  @RequestBody(required = false) TemplateVersionRequest request) {
        try {
            String remark = request == null ? null : request.getChangeRemark();
            return Result.ok(templateLibraryService.restoreVersion(id, remark));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出全部模板 Excel */
    @GetMapping("/templates/export")
    public void export(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("检查模板.xlsx", StandardCharsets.UTF_8));
        templateLibraryService.exportTemplates(response.getOutputStream());
    }

    /** 批量导入模板 Excel（同名同类型跳过，非法行跳过） */
    @PostMapping("/templates/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            return Result.ok(templateLibraryService.importTemplates(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail(400, "导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/templates/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("检查模板导入模板.xlsx", StandardCharsets.UTF_8));
        templateLibraryService.writeImportTemplate(response.getOutputStream());
    }
}
