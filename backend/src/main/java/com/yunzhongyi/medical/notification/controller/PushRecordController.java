package com.yunzhongyi.medical.notification.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.notification.dto.PushRecordQuery;
import com.yunzhongyi.medical.notification.entity.PushRecord;
import com.yunzhongyi.medical.notification.service.PushRecordService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 消息推送记录接口：推送查询、发送（预留占位）、重推、统计、导出。
 * <p>
 * 发送与重推本机无真实通道：调用返回 {record, success=false, message}，
 * 记录记为发送失败并写明通道未对接，绝不伪造发送成功。
 */
@RestController
@RequestMapping("/api/push-records")
@RequiredArgsConstructor
public class PushRecordController {

    private final PushRecordService pushRecordService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(PushRecordQuery query) {
        return Result.ok(pushRecordService.pageQuery(query));
    }

    /** 推送记录统计：总数 / 待发送 / 已发送 / 发送失败 / 按渠道分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(pushRecordService.stats());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<PushRecord> get(@PathVariable Long id) {
        try {
            return Result.ok(pushRecordService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 发送推送（预留占位：无真实通道，返回失败并如实记录） */
    @PostMapping
    public Result<Map<String, Object>> send(@Valid @RequestBody PushRecord record) {
        try {
            return Result.ok(pushRecordService.send(record));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 重推（已发送成功不可重推） */
    @PostMapping("/{id}/retry")
    public Result<Map<String, Object>> retry(@PathVariable Long id) {
        try {
            return Result.ok(pushRecordService.retry(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已发送成功的记录不可直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            pushRecordService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(PushRecordQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("消息推送记录.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        pushRecordService.exportExcel(query, response.getOutputStream());
    }
}
