package com.yunzhongyi.medical.briefing.controller;

import com.yunzhongyi.medical.briefing.dto.BriefingQuery;
import com.yunzhongyi.medical.briefing.dto.GenerateBriefingRequest;
import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.briefing.service.BriefingService;
import com.yunzhongyi.medical.common.Result;
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
 * 医疗工作简报管理接口：简报查询/增删改、提交审批/审批通过/驳回、
 * 按模板自动生成、统计、导出 Word/PDF/纯文本/Excel、批量导入导出。
 * <p>
 * PDF 导出为真实 PDF 文件（OpenPDF + 嵌入简体中文字体）；若运行环境没有
 * 可用中文字体，接口明确返回失败，不生成假文件。
 */
@RestController
@RequestMapping("/api/briefings")
@RequiredArgsConstructor
public class BriefingController {

    private final BriefingService briefingService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(BriefingQuery query) {
        return Result.ok(briefingService.pageQuery(query));
    }

    /** 简报统计：总数 / 草稿 / 待审批 / 已发布 / 已驳回 / 按分类分组 / 模板数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(briefingService.stats());
    }

    /** 按分类汇总统计 */
    @GetMapping("/stats/by-category")
    public Result<Map<String, Object>> statsByCategory() {
        return Result.ok(briefingService.statsByCategory());
    }

    /** 按统计周期汇总统计（按周期升序） */
    @GetMapping("/stats/by-month")
    public Result<Map<String, Object>> statsByMonth() {
        return Result.ok(briefingService.statsByMonth());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Briefing> get(@PathVariable Long id) {
        try {
            return Result.ok(briefingService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增简报（默认草稿） */
    @PostMapping
    public Result<Briefing> create(@Valid @RequestBody Briefing briefing) {
        try {
            return Result.ok(briefingService.create(briefing));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改简报（只有草稿与已驳回的简报能修改） */
    @PutMapping("/{id}")
    public Result<Briefing> update(@PathVariable Long id,
                                   @Valid @RequestBody Briefing briefing) {
        try {
            briefing.setId(id);
            return Result.ok(briefingService.update(briefing));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除简报（已发布的简报不能直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            briefingService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除（已发布的简报不能直接删除） */
    @DeleteMapping("/batch")
    public Result<?> deleteBatch(@RequestBody List<Long> ids) {
        try {
            briefingService.deleteBatch(ids);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 提交审批：草稿 / 已驳回 → 待审批 */
    @PostMapping("/{id}/submit")
    public Result<Briefing> submit(@PathVariable Long id) {
        try {
            return Result.ok(briefingService.submit(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 审批通过：待审批 → 已发布，发布日期默认为当天，审批记录留痕 */
    @PostMapping("/{id}/approve")
    public Result<Briefing> approve(@PathVariable Long id,
                                    @RequestParam String approver,
                                    @RequestParam(required = false) String opinion) {
        try {
            return Result.ok(briefingService.approve(id, approver, opinion));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 审批驳回：待审批 → 已驳回，驳回原因必填，审批记录留痕 */
    @PostMapping("/{id}/reject")
    public Result<Briefing> reject(@PathVariable Long id,
                                   @RequestParam String approver,
                                   @RequestParam String reason) {
        try {
            return Result.ok(briefingService.reject(id, approver, reason));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 按模板自动生成简报：从各业务模块聚合本期数据写正文，生成结果为草稿 */
    @PostMapping("/generate")
    public Result<Briefing> generate(@Valid @RequestBody GenerateBriefingRequest request) {
        try {
            return Result.ok(briefingService.generate(request));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Word（.docx，Apache POI 真实文件） */
    @GetMapping("/{id}/export-word")
    public void exportWord(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Briefing b = briefingService.detail(id);
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医疗工作简报-" + b.getIssueNo() + ".docx", StandardCharsets.UTF_8).replace("+", "%20"));
        briefingService.exportWord(id, response.getOutputStream());
    }

    /** 导出 PDF（OpenPDF + 嵌入简体中文字体，真实 PDF 文件） */
    @GetMapping("/{id}/export-pdf")
    public void exportPdf(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Briefing b = briefingService.detail(id);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医疗工作简报-" + b.getIssueNo() + ".pdf", StandardCharsets.UTF_8).replace("+", "%20"));
        try {
            briefingService.exportPdf(id, response.getOutputStream());
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /** 下载正文纯文本（.txt） */
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Briefing b = briefingService.detail(id);
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医疗工作简报-" + b.getIssueNo() + ".txt", StandardCharsets.UTF_8).replace("+", "%20"));
        briefingService.downloadText(id, response.getOutputStream());
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(BriefingQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医疗工作简报.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        briefingService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的均为草稿，需走提交审批流程；期号重复跳过） */
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
            return Result.ok(briefingService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医疗工作简报导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        briefingService.writeImportTemplate(response.getOutputStream());
    }
}
