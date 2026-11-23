package com.yunzhongyi.medical.sysconfig.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.sysconfig.dto.SysInterfaceQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysInterface;
import com.yunzhongyi.medical.sysconfig.service.SysInterfaceService;
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
 * 系统接口管理接口：外部系统对接配置查询/增删改、启用/停用、连通性测试、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/sys-interfaces")
@RequiredArgsConstructor
public class SysInterfaceController {

    private final SysInterfaceService sysInterfaceService;

    /** 多条件组合分页查询（鉴权凭证不回显） */
    @GetMapping
    public Result<?> page(SysInterfaceQuery query) {
        return Result.ok(sysInterfaceService.pageQuery(query));
    }

    /** 接口统计：总数 / 启用 / 停用 / 测试成功 / 失败 / 未测试 / 按类型分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(sysInterfaceService.stats());
    }

    /** 全部启用的接口 */
    @GetMapping("/enabled")
    public Result<List<SysInterface>> enabled() {
        return Result.ok(sysInterfaceService.enabled());
    }

    /** 详情（鉴权凭证不回显） */
    @GetMapping("/{id}")
    public Result<SysInterface> get(@PathVariable Long id) {
        try {
            return Result.ok(sysInterfaceService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增接口（默认其他类型、无鉴权、启用、超时 5 秒、未测试） */
    @PostMapping
    public Result<SysInterface> create(@Valid @RequestBody SysInterface iface) {
        try {
            return Result.ok(sysInterfaceService.create(iface));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改接口（编码不可改；不传凭证时沿用原凭证） */
    @PutMapping("/{id}")
    public Result<SysInterface> update(@PathVariable Long id,
                                       @Valid @RequestBody SysInterface iface) {
        try {
            iface.setId(id);
            return Result.ok(sysInterfaceService.update(iface));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            sysInterfaceService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用 */
    @PostMapping("/{id}/enable")
    public Result<SysInterface> enable(@PathVariable Long id) {
        try {
            return Result.ok(sysInterfaceService.enable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用 */
    @PostMapping("/{id}/disable")
    public Result<SysInterface> disable(@PathVariable Long id) {
        try {
            return Result.ok(sysInterfaceService.disable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 连通性测试：只有启用的接口能测试；真实发起 HTTP 请求，不伪造成功 */
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> test(@PathVariable Long id) {
        try {
            return Result.ok(sysInterfaceService.testConnection(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件，鉴权凭证不导出） */
    @GetMapping("/export")
    public void export(SysInterfaceQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("系统接口.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        sysInterfaceService.exportExcel(query, response.getOutputStream());
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
            return Result.ok(sysInterfaceService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("系统接口导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        sysInterfaceService.writeImportTemplate(response.getOutputStream());
    }
}
