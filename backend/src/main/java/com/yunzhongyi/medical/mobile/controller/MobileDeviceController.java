package com.yunzhongyi.medical.mobile.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.mobile.dto.MobileDeviceQuery;
import com.yunzhongyi.medical.mobile.entity.MobileDevice;
import com.yunzhongyi.medical.mobile.service.MobileDeviceService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动端设备管理接口：设备登记 / 查询 / 绑定 / 解绑 / 统计 / 批量导入导出。
 */
@RestController
@RequestMapping("/api/mobile-devices")
@RequiredArgsConstructor
public class MobileDeviceController {

    private final MobileDeviceService mobileDeviceService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(MobileDeviceQuery query) {
        return Result.ok(mobileDeviceService.pageQuery(query));
    }

    /** 设备统计：总数 / 已绑定 / 已解绑 / 按平台分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(mobileDeviceService.stats());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<MobileDevice> get(@PathVariable Long id) {
        try {
            return Result.ok(mobileDeviceService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 登记设备（默认已绑定） */
    @PostMapping
    public Result<MobileDevice> create(@Valid @RequestBody MobileDevice device) {
        try {
            return Result.ok(mobileDeviceService.register(device));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改设备（已绑定状态与最近活跃时间不在此改，走绑定 / 解绑接口） */
    @PutMapping("/{id}")
    public Result<MobileDevice> update(@PathVariable Long id,
                                       @Valid @RequestBody MobileDevice device) {
        try {
            device.setId(id);
            return Result.ok(mobileDeviceService.update(device));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已绑定的设备不可直接删除，请先解绑） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            mobileDeviceService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除：逐条走删除守卫，已绑定的跳过并计入 failed */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        int deleted = 0;
        List<String> failed = new ArrayList<>();
        for (Long id : ids) {
            try {
                mobileDeviceService.delete(id);
                deleted++;
            } catch (IllegalArgumentException e) {
                failed.add("id=" + id + "：" + e.getMessage());
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("deleted", deleted);
        r.put("failed", failed.size());
        r.put("errors", failed);
        return Result.ok(r);
    }

    /** 绑定设备 */
    @PostMapping("/{id}/bind")
    public Result<MobileDevice> bind(@PathVariable Long id) {
        try {
            return Result.ok(mobileDeviceService.bind(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 解绑设备 */
    @PostMapping("/{id}/unbind")
    public Result<MobileDevice> unbind(@PathVariable Long id) {
        try {
            return Result.ok(mobileDeviceService.unbind(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(MobileDeviceQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("移动端设备.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        mobileDeviceService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（设备编码已存在的行跳过） */
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
            return Result.ok(mobileDeviceService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("移动端设备导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        mobileDeviceService.writeImportTemplate(response.getOutputStream());
    }
}
