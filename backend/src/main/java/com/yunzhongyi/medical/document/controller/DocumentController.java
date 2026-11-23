package com.yunzhongyi.medical.document.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.document.dto.AbolishRequest;
import com.yunzhongyi.medical.document.dto.DocumentQuery;
import com.yunzhongyi.medical.document.dto.PublishRequest;
import com.yunzhongyi.medical.document.entity.Document;
import com.yunzhongyi.medical.document.service.DocumentService;
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
 * 文件制度管理接口：文件查询/增删改、发布/作废/新建版本、在线阅读（详情含正文）、
 * 文本下载、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(DocumentQuery query) {
        return Result.ok(documentService.pageQuery(query));
    }

    /** 文件统计：总数 / 草稿 / 已发布 / 已作废 / 按分类分组 / 30 天内到期 / 已过期 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(documentService.stats());
    }

    /** 按文件分类汇总统计 */
    @GetMapping("/stats/by-category")
    public Result<List<Map<String, Object>>> statsByCategory() {
        return Result.ok(documentService.statsByCategory());
    }

    /** 按月发布趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(documentService.statsByMonth());
    }

    /** 详情（含正文，用于在线阅读） */
    @GetMapping("/{id}")
    public Result<Document> get(@PathVariable Long id) {
        try {
            return Result.ok(documentService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增文件（默认草稿、V1.0） */
    @PostMapping
    public Result<Document> create(@Valid @RequestBody Document document) {
        try {
            documentService.save(document);
            return Result.ok(document);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有草稿能修改；已发布的文件改版走新建版本） */
    @PutMapping("/{id}")
    public Result<Document> update(@PathVariable Long id, @Valid @RequestBody Document document) {
        try {
            document.setId(id);
            documentService.updateById(document);
            return Result.ok(document);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已发布的文件不能直接删除，请先作废） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            documentService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            documentService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 发布（一级审批）：只有草稿能发布，审批人必填 */
    @PostMapping("/{id}/publish")
    public Result<Document> publish(@PathVariable Long id,
                                    @RequestBody(required = false) PublishRequest request) {
        try {
            return Result.ok(documentService.publish(id, request == null ? new PublishRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 作废：只有已发布的文件能作废，作废原因必填 */
    @PostMapping("/{id}/abolish")
    public Result<Document> abolish(@PathVariable Long id,
                                    @RequestBody(required = false) AbolishRequest request) {
        try {
            return Result.ok(documentService.abolish(id, request == null ? new AbolishRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新建版本：只有已发布的文件能新建版本，生成草稿记录（版本号 +1） */
    @PostMapping("/{id}/new-version")
    public Result<Document> newVersion(@PathVariable Long id) {
        try {
            return Result.ok(documentService.newVersion(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 下载：按正文生成 .txt 文件（不引入真实文件存储） */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Document d;
        try {
            d = documentService.detail(id);
        } catch (IllegalArgumentException e) {
            response.sendError(400, e.getMessage());
            return;
        }
        String filename = d.getDocNo() + "_" + (d.getVersion() == null ? "" : d.getVersion()) + ".txt";
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        response.getOutputStream().write(documentService.downloadText(id).getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(DocumentQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("文件制度.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        documentService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的均为草稿，需走发布审批流程） */
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
            int count = documentService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("文件制度导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        documentService.exportExcel(new DocumentQuery(), response.getOutputStream());
    }
}
