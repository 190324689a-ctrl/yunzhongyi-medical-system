package com.yunzhongyi.medical.ethics.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.ethics.dto.EthicsProfileQuery;
import com.yunzhongyi.medical.ethics.entity.EthicsProfile;
import com.yunzhongyi.medical.ethics.service.EthicsProfileService;
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
 * 医德医风档案接口：档案查询/增删改、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/ethics-profiles")
@RequiredArgsConstructor
public class EthicsProfileController {

    private final EthicsProfileService profileService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(EthicsProfileQuery query) {
        return Result.ok(profileService.pageQuery(query));
    }

    /** 档案统计：总数 / 在档 / 封存 / 按科室汇总 / 已通过表彰数 / 投诉总数 / 考评平均分与优秀率 / 逾期投诉数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(profileService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(profileService.statsByDept());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<EthicsProfile> get(@PathVariable Long id) {
        try {
            return Result.ok(profileService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增档案（默认在档；一名医师只能建一份档案，档案编号全院唯一） */
    @PostMapping
    public Result<EthicsProfile> create(@Valid @RequestBody EthicsProfile profile) {
        try {
            profileService.save(profile);
            return Result.ok(profile);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<EthicsProfile> update(@PathVariable Long id, @Valid @RequestBody EthicsProfile profile) {
        try {
            profile.setId(id);
            profileService.updateById(profile);
            return Result.ok(profile);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            profileService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            profileService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(EthicsProfileQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医德医风档案.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        profileService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（同一档案编号/同一医师重复跳过；按姓名查医师档案补全） */
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
            int count = profileService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医德医风档案导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        profileService.exportExcel(new EthicsProfileQuery(), response.getOutputStream());
    }
}
