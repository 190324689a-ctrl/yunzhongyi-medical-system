package com.yunzhongyi.medical.notification.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.notification.dto.SiteMessageQuery;
import com.yunzhongyi.medical.notification.entity.SiteMessage;
import com.yunzhongyi.medical.notification.service.SiteMessageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 站内消息管理接口：消息查询/收发、标已读、未读计数、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/site-messages")
@RequiredArgsConstructor
public class SiteMessageController {

    private final SiteMessageService siteMessageService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(SiteMessageQuery query) {
        return Result.ok(siteMessageService.pageQuery(query));
    }

    /** 站内消息统计：总数 / 未读 / 已读 / 按分类分组 / 按接收范围分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(siteMessageService.stats());
    }

    /** 未读数：按用户名与角色编码统计该用户可见的未读消息数 */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount(@RequestParam(required = false) String username,
                                                   @RequestParam(required = false) String roleCode) {
        return Result.ok(Map.of("unread", siteMessageService.unreadCount(username, roleCode)));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<SiteMessage> get(@PathVariable Long id) {
        try {
            return Result.ok(siteMessageService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 发送站内消息（默认通知分类、全部人员、未读） */
    @PostMapping
    public Result<SiteMessage> create(@Valid @RequestBody SiteMessage message) {
        try {
            return Result.ok(siteMessageService.create(message));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改消息（已读消息不可修改） */
    @PutMapping("/{id}")
    public Result<SiteMessage> update(@PathVariable Long id,
                                      @Valid @RequestBody SiteMessage message) {
        try {
            message.setId(id);
            return Result.ok(siteMessageService.update(message));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            siteMessageService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 标已读 */
    @PostMapping("/{id}/read")
    public Result<SiteMessage> markRead(@PathVariable Long id) {
        try {
            return Result.ok(siteMessageService.markRead(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 全部标已读：把某用户可见的未读消息全部标已读 */
    @PostMapping("/read-all")
    public Result<Map<String, Object>> markAllRead(@RequestParam(required = false) String username,
                                                   @RequestParam(required = false) String roleCode) {
        return Result.ok(Map.of("read", siteMessageService.markAllRead(username, roleCode)));
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(SiteMessageQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("站内消息.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        siteMessageService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（标题与内容均已存在的行跳过） */
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
            return Result.ok(siteMessageService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("站内消息导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        siteMessageService.writeImportTemplate(response.getOutputStream());
    }
}
