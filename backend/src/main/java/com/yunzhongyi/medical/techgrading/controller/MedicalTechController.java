package com.yunzhongyi.medical.techgrading.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.techgrading.dto.MedicalTechQuery;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.entity.MedicalTech;
import com.yunzhongyi.medical.techgrading.service.MedicalTechService;
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
 * 医疗技术档案管理接口：档案查询/增删改、停用/重新启用、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/medical-techs")
@RequiredArgsConstructor
public class MedicalTechController {

    private final MedicalTechService medicalTechService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(MedicalTechQuery query) {
        return Result.ok(medicalTechService.pageQuery(query));
    }

    /** 技术档案统计：总数 / 待准入 / 已准入 / 已停用 / 按分级分组 / 按准入类别分组 / 按分类分组 / 90 天内到期 / 已过期 / 准入申请统计 / 医师授权统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(medicalTechService.stats());
    }

    /** 按技术分级汇总统计 */
    @GetMapping("/stats/by-level")
    public Result<List<Map<String, Object>>> statsByLevel() {
        return Result.ok(medicalTechService.statsByLevel());
    }

    /** 按技术分类汇总统计 */
    @GetMapping("/stats/by-category")
    public Result<List<Map<String, Object>>> statsByCategory() {
        return Result.ok(medicalTechService.statsByCategory());
    }

    /** 按开展科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(medicalTechService.statsByDept());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<MedicalTech> get(@PathVariable Long id) {
        try {
            return Result.ok(medicalTechService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增技术档案（默认待准入，需走准入审批流程） */
    @PostMapping
    public Result<MedicalTech> create(@Valid @RequestBody MedicalTech medicalTech) {
        try {
            medicalTechService.save(medicalTech);
            return Result.ok(medicalTech);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（已准入的技术不能直接修改，分级/科室/有效期变更请走准入申请） */
    @PutMapping("/{id}")
    public Result<MedicalTech> update(@PathVariable Long id, @Valid @RequestBody MedicalTech medicalTech) {
        try {
            medicalTech.setId(id);
            medicalTechService.updateById(medicalTech);
            return Result.ok(medicalTech);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已准入的技术不能直接删除，请先停用；同时删除其准入申请与授权记录） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            medicalTechService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            medicalTechService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用：只有已准入的技术能停用，停用原因必填 */
    @PostMapping("/{id}/suspend")
    public Result<MedicalTech> suspend(@PathVariable Long id,
                                       @RequestBody(required = false) RejectRequest request) {
        try {
            return Result.ok(medicalTechService.suspend(id, request == null ? new RejectRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 重新启用：只有已停用的技术能重新启用 */
    @PostMapping("/{id}/resume")
    public Result<MedicalTech> resume(@PathVariable Long id) {
        try {
            return Result.ok(medicalTechService.resume(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(MedicalTechQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医疗技术档案.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        medicalTechService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的均为待准入，需走准入审批流程） */
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
            int count = medicalTechService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医疗技术档案导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        medicalTechService.exportExcel(new MedicalTechQuery(), response.getOutputStream());
    }
}
