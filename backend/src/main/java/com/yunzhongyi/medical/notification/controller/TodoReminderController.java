package com.yunzhongyi.medical.notification.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.notification.dto.GenerateTodoRequest;
import com.yunzhongyi.medical.notification.dto.TodoReminderQuery;
import com.yunzhongyi.medical.notification.entity.TodoReminder;
import com.yunzhongyi.medical.notification.service.TodoReminderService;
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
 * 待办提醒管理接口：待办查询/增删改、完成流转、逾期扫描、按 28 号提醒设置生成、统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/todo-reminders")
@RequiredArgsConstructor
public class TodoReminderController {

    private final TodoReminderService todoReminderService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(TodoReminderQuery query) {
        return Result.ok(todoReminderService.pageQuery(query));
    }

    /** 待办提醒统计：总数 / 待办 / 已完成 / 已逾期 / 今日到期 / 按类型分组 / 按来源分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(todoReminderService.stats());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<TodoReminder> get(@PathVariable Long id) {
        try {
            return Result.ok(todoReminderService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新建待办（默认其他待办、手工创建、待办状态） */
    @PostMapping
    public Result<TodoReminder> create(@Valid @RequestBody TodoReminder todo) {
        try {
            return Result.ok(todoReminderService.create(todo));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改待办（已完成的待办不可修改） */
    @PutMapping("/{id}")
    public Result<TodoReminder> update(@PathVariable Long id,
                                       @Valid @RequestBody TodoReminder todo) {
        try {
            todo.setId(id);
            return Result.ok(todoReminderService.update(todo));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已完成的待办不可直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            todoReminderService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 完成待办 */
    @PostMapping("/{id}/complete")
    public Result<TodoReminder> complete(@PathVariable Long id) {
        try {
            return Result.ok(todoReminderService.complete(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 逾期扫描：待办且截止日期早于当天的改为已逾期 */
    @PostMapping("/overdue-scan")
    public Result<Map<String, Object>> overdueScan() {
        return Result.ok(Map.of("overdue", todoReminderService.overdueScan()));
    }

    /** 按 28 号提醒设置生成待办：设置须存在且启用 */
    @PostMapping("/generate")
    public Result<TodoReminder> generateFromSetting(@RequestBody GenerateTodoRequest request) {
        try {
            return Result.ok(todoReminderService.generateFromSetting(request));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(TodoReminderQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("待办提醒.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        todoReminderService.exportExcel(query, response.getOutputStream());
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
            return Result.ok(todoReminderService.importExcel(file.getInputStream()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("待办提醒导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        todoReminderService.writeImportTemplate(response.getOutputStream());
    }
}
