package com.yunzhongyi.medical.notification.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.notification.dto.SmsRecordQuery;
import com.yunzhongyi.medical.notification.entity.SmsRecord;
import com.yunzhongyi.medical.notification.service.SmsRecordService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 短信提醒记录接口：短信查询、发送（预留占位）、重发、统计、导出。
 * <p>
 * 发送与重发本机无真实网关：调用返回 {record, success=false, message}，
 * 记录记为发送失败并写明网关未对接，绝不伪造发送成功。
 */
@RestController
@RequestMapping("/api/sms-records")
@RequiredArgsConstructor
public class SmsRecordController {

    private final SmsRecordService smsRecordService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(SmsRecordQuery query) {
        return Result.ok(smsRecordService.pageQuery(query));
    }

    /** 短信记录统计：总数 / 待发送 / 已发送 / 发送失败 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(smsRecordService.stats());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<SmsRecord> get(@PathVariable Long id) {
        try {
            return Result.ok(smsRecordService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 发送短信（预留占位：无真实网关，返回失败并如实记录） */
    @PostMapping
    public Result<Map<String, Object>> send(@Valid @RequestBody SmsRecord record) {
        try {
            return Result.ok(smsRecordService.send(record));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 重发（已发送成功不可重发） */
    @PostMapping("/{id}/retry")
    public Result<Map<String, Object>> retry(@PathVariable Long id) {
        try {
            return Result.ok(smsRecordService.retry(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（已发送成功的记录不可直接删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            smsRecordService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(SmsRecordQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("短信提醒记录.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        smsRecordService.exportExcel(query, response.getOutputStream());
    }
}
