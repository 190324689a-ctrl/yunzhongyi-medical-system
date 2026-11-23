package com.yunzhongyi.medical.medgrading.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.medgrading.dto.GradingStandardQuery;
import com.yunzhongyi.medical.medgrading.entity.GradingStandard;
import com.yunzhongyi.medical.medgrading.service.GradingStandardService;
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
 * 医疗分级标准管理接口：分数线方案查询/增删改、启用/停用、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/grading-standards")
@RequiredArgsConstructor
public class GradingStandardController {

    private final GradingStandardService gradingStandardService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(GradingStandardQuery query) {
        return Result.ok(gradingStandardService.pageQuery(query));
    }

    /** 分级标准统计：总数 / 启用 / 停用 / 按分级范围分组 / 已用 / 未用 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(gradingStandardService.stats());
    }

    /** 全部启用的标准（新建评定时下拉用） */
    @GetMapping("/enabled")
    public Result<List<GradingStandard>> enabled() {
        return Result.ok(gradingStandardService.enabled());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<GradingStandard> get(@PathVariable Long id) {
        try {
            return Result.ok(gradingStandardService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增标准（默认启用；分数线默认甲级 90 / 乙级 80 / 丙级 60） */
    @PostMapping
    public Result<GradingStandard> create(@Valid @RequestBody GradingStandard standard) {
        try {
            return Result.ok(gradingStandardService.create(standard));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改标准（已有评定引用时编码不可改） */
    @PutMapping("/{id}")
    public Result<GradingStandard> update(@PathVariable Long id,
                                          @Valid @RequestBody GradingStandard standard) {
        try {
            standard.setId(id);
            return Result.ok(gradingStandardService.update(standard));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已有分级评定的标准不能直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            gradingStandardService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<GradingStandard> enable(@PathVariable Long id) {
        try {
            return Result.ok(gradingStandardService.enable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用（有待审批评定的标准不可停用） */
    @PostMapping("/{id}/disable")
    public Result<GradingStandard> disable(@PathVariable Long id) {
        try {
            return Result.ok(gradingStandardService.disable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(GradingStandardQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("分级标准.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        gradingStandardService.exportExcel(query, response.getOutputStream());
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
            return Result.ok(gradingStandardService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("分级标准导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        gradingStandardService.writeImportTemplate(response.getOutputStream());
    }
}
