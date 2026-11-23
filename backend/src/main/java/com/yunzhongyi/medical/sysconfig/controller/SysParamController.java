package com.yunzhongyi.medical.sysconfig.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.sysconfig.dto.SysParamQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysParam;
import com.yunzhongyi.medical.sysconfig.service.SysParamService;
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
 * 系统参数管理接口：参数键值对查询/增删改、启用/停用、按参数键读取、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/sys-params")
@RequiredArgsConstructor
public class SysParamController {

    private final SysParamService sysParamService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(SysParamQuery query) {
        return Result.ok(sysParamService.pageQuery(query));
    }

    /** 参数统计：总数 / 启用 / 停用 / 内置 / 自定义 / 按分组统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(sysParamService.stats());
    }

    /** 全部启用的参数（配置读取用） */
    @GetMapping("/enabled")
    public Result<List<SysParam>> enabled() {
        return Result.ok(sysParamService.enabled());
    }

    /** 按参数键取启用参数（业务读取用） */
    @GetMapping("/by-key/{paramKey}")
    public Result<SysParam> getByKey(@PathVariable String paramKey) {
        try {
            return Result.ok(sysParamService.getByKey(paramKey));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<SysParam> get(@PathVariable Long id) {
        try {
            return Result.ok(sysParamService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增参数（默认业务分组、文本类型、启用、自定义） */
    @PostMapping
    public Result<SysParam> create(@Valid @RequestBody SysParam param) {
        try {
            return Result.ok(sysParamService.create(param));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改参数（内置参数的键不可改） */
    @PutMapping("/{id}")
    public Result<SysParam> update(@PathVariable Long id,
                                   @Valid @RequestBody SysParam param) {
        try {
            param.setId(id);
            return Result.ok(sysParamService.update(param));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（内置参数不可删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            sysParamService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<SysParam> enable(@PathVariable Long id) {
        try {
            return Result.ok(sysParamService.enable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用 */
    @PostMapping("/{id}/disable")
    public Result<SysParam> disable(@PathVariable Long id) {
        try {
            return Result.ok(sysParamService.disable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(SysParamQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("系统参数.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        sysParamService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（键重复或非法行跳过） */
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
            return Result.ok(sysParamService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("系统参数导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        sysParamService.writeImportTemplate(response.getOutputStream());
    }
}
