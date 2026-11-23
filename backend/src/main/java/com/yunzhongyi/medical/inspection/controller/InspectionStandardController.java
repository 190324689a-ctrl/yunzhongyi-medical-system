package com.yunzhongyi.medical.inspection.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.inspection.dto.InspectionStandardQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionStandard;
import com.yunzhongyi.medical.inspection.service.InspectionStandardService;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
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
 * 检查标准管理接口：标准查询/增删改、废止/恢复现行、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/inspection-standards")
@RequiredArgsConstructor
public class InspectionStandardController {

    private final InspectionStandardService inspectionStandardService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(InspectionStandardQuery query) {
        return Result.ok(inspectionStandardService.pageQuery(query));
    }

    /** 检查标准统计：总数 / 现行 / 已废止 / 按标准级别分组 / 30 天内到期 / 已过期 / 被检查项目引用数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(inspectionStandardService.stats());
    }

    /** 按标准级别汇总统计 */
    @GetMapping("/stats/by-level")
    public Result<List<Map<String, Object>>> statsByLevel() {
        return Result.ok(inspectionStandardService.statsByLevel());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<InspectionStandard> get(@PathVariable Long id) {
        try {
            return Result.ok(inspectionStandardService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增检查标准（默认现行） */
    @PostMapping
    public Result<InspectionStandard> create(@Valid @RequestBody InspectionStandard inspectionStandard) {
        try {
            inspectionStandardService.save(inspectionStandard);
            return Result.ok(inspectionStandard);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改检查标准 */
    @PutMapping("/{id}")
    public Result<InspectionStandard> update(@PathVariable Long id,
                                             @Valid @RequestBody InspectionStandard inspectionStandard) {
        try {
            inspectionStandard.setId(id);
            inspectionStandardService.updateById(inspectionStandard);
            return Result.ok(inspectionStandard);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已被检查项目引用的标准不能直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            inspectionStandardService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            inspectionStandardService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 废止：只有现行的标准能废止，废止原因必填 */
    @PostMapping("/{id}/abolish")
    public Result<InspectionStandard> abolish(@PathVariable Long id,
                                              @RequestBody(required = false) RejectRequest request) {
        try {
            return Result.ok(inspectionStandardService.abolish(id,
                    request == null ? null : request.getReason()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 恢复现行：只有已废止的标准能恢复 */
    @PostMapping("/{id}/restore")
    public Result<InspectionStandard> restore(@PathVariable Long id) {
        try {
            return Result.ok(inspectionStandardService.restore(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(InspectionStandardQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("检查标准.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        inspectionStandardService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（导入的标准默认现行） */
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
            int count = inspectionStandardService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("检查标准导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        inspectionStandardService.exportExcel(new InspectionStandardQuery(), response.getOutputStream());
    }
}
