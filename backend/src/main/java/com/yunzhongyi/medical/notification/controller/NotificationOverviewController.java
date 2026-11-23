package com.yunzhongyi.medical.notification.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.notification.service.PushRecordService;
import com.yunzhongyi.medical.notification.service.SiteMessageService;
import com.yunzhongyi.medical.notification.service.SmsRecordService;
import com.yunzhongyi.medical.notification.service.TodoReminderService;
import com.yunzhongyi.medical.sysconfig.service.SysNoticeSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消息提醒总览接口：站内消息 / 待办提醒 / 消息推送 / 短信提醒 /
 * 28 号提醒设置的数量汇总。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationOverviewController {

    private final SiteMessageService siteMessageService;
    private final TodoReminderService todoReminderService;
    private final PushRecordService pushRecordService;
    private final SmsRecordService smsRecordService;
    private final SysNoticeSettingService sysNoticeSettingService;

    /** 消息提醒总览：四组记录与 28 号提醒设置的数量汇总 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> r = new LinkedHashMap<>();
        Map<String, Object> messageStats = siteMessageService.stats();
        Map<String, Object> todoStats = todoReminderService.stats();
        Map<String, Object> pushStats = pushRecordService.stats();
        Map<String, Object> smsStats = smsRecordService.stats();
        r.put("messages", messageStats.get("total"));
        r.put("unreadMessages", messageStats.get("unread"));
        r.put("todos", todoStats.get("total"));
        r.put("pendingTodos", todoStats.get("pending"));
        r.put("overdueTodos", todoStats.get("overdue"));
        r.put("pushRecords", pushStats.get("total"));
        r.put("failedPush", pushStats.get("failed"));
        r.put("smsRecords", smsStats.get("total"));
        r.put("failedSms", smsStats.get("failed"));
        r.put("noticeSettings", sysNoticeSettingService.count());
        r.put("enabledNoticeSettings", sysNoticeSettingService.stats().get("enabled"));
        return Result.ok(r);
    }
}
