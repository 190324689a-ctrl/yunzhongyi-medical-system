package com.yunzhongyi.medical.sysconfig.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.sysconfig.dto.KnowledgeItemQuery;
import com.yunzhongyi.medical.sysconfig.entity.KnowledgeItem;
import com.yunzhongyi.medical.sysconfig.service.KnowledgeItemService;
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
 * 知识库管理接口：知识条目查询/增删改、启用/停用、查阅计数、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/sys-knowledge")
@RequiredArgsConstructor
public class KnowledgeItemController {

    private final KnowledgeItemService knowledgeItemService;

    /** 多条件组合分页查询（标题/正文/关键词模糊检索） */
    @GetMapping
    public Result<?> page(KnowledgeItemQuery query) {
        return Result.ok(knowledgeItemService.pageQuery(query));
    }

    /** 知识库统计：总数 / 启用 / 停用 / 总查阅量 / 按分类分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(knowledgeItemService.stats());
    }

    /** 全部启用的知识（检索用） */
    @GetMapping("/enabled")
    public Result<List<KnowledgeItem>> enabled() {
        return Result.ok(knowledgeItemService.enabled());
    }

    /** 详情（打开一次查阅量 +1） */
    @GetMapping("/{id}")
    public Result<KnowledgeItem> get(@PathVariable Long id) {
        try {
            return Result.ok(knowledgeItemService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增知识（默认其他分类、启用、查阅量 0） */
    @PostMapping
    public Result<KnowledgeItem> create(@Valid @RequestBody KnowledgeItem item) {
        try {
            return Result.ok(knowledgeItemService.create(item));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改知识（查阅量不随编辑改写） */
    @PutMapping("/{id}")
    public Result<KnowledgeItem> update(@PathVariable Long id,
                                        @Valid @RequestBody KnowledgeItem item) {
        try {
            item.setId(id);
            return Result.ok(knowledgeItemService.update(item));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            knowledgeItemService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<KnowledgeItem> enable(@PathVariable Long id) {
        try {
            return Result.ok(knowledgeItemService.enable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用 */
    @PostMapping("/{id}/disable")
    public Result<KnowledgeItem> disable(@PathVariable Long id) {
        try {
            return Result.ok(knowledgeItemService.disable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(KnowledgeItemQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("知识库.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        knowledgeItemService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（编码重复或非法行跳过；查阅量从 0 开始计） */
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
            return Result.ok(knowledgeItemService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("知识库导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        knowledgeItemService.writeImportTemplate(response.getOutputStream());
    }
}
