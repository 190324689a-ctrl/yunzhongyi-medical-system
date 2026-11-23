package com.yunzhongyi.medical.radiation.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.radiation.dto.RadiationPersonnelQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationPersonnel;
import com.yunzhongyi.medical.radiation.service.RadiationPersonnelService;
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
 * 放射人员管理接口：档案查询/增删改、统计、批量导入导出；
 * 资质、培训、健康记录见 RadiationQualificationController /
 * RadiationTrainingController / RadiationHealthController
 */
@RestController
@RequestMapping("/api/radiation-personnel")
@RequiredArgsConstructor
public class RadiationPersonnelController {

    private final RadiationPersonnelService radiationPersonnelService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(RadiationPersonnelQuery query) {
        return Result.ok(radiationPersonnelService.pageQuery(query));
    }

    /** 放射人员统计：档案总数 / 在岗 / 停岗 / 离岗 / 按放射类别分组 / 按科室汇总 / 资质 / 培训 / 健康聚合统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(radiationPersonnelService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(radiationPersonnelService.statsByDept());
    }

    /** 按放射类别汇总统计 */
    @GetMapping("/stats/by-work-type")
    public Result<List<Map<String, Object>>> statsByWorkType() {
        return Result.ok(radiationPersonnelService.statsByWorkType());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<RadiationPersonnel> get(@PathVariable Long id) {
        try {
            return Result.ok(radiationPersonnelService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增放射人员档案（默认在岗） */
    @PostMapping
    public Result<RadiationPersonnel> create(@Valid @RequestBody RadiationPersonnel personnel) {
        try {
            radiationPersonnelService.save(personnel);
            return Result.ok(personnel);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<RadiationPersonnel> update(@PathVariable Long id, @Valid @RequestBody RadiationPersonnel personnel) {
        try {
            personnel.setId(id);
            radiationPersonnelService.updateById(personnel);
            return Result.ok(personnel);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        radiationPersonnelService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        radiationPersonnelService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(RadiationPersonnelQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("放射人员档案.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        radiationPersonnelService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel */
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
            int count = radiationPersonnelService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("放射人员档案导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        radiationPersonnelService.exportExcel(new RadiationPersonnelQuery(), response.getOutputStream());
    }
}
