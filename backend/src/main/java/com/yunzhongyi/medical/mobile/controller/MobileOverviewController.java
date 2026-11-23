package com.yunzhongyi.medical.mobile.controller;

import com.yunzhongyi.medical.assessment.service.AssessmentService;
import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.leave.service.LeaveApplicationService;
import com.yunzhongyi.medical.mobile.service.MobileApprovalService;
import com.yunzhongyi.medical.mobile.service.MobileDeviceService;
import com.yunzhongyi.medical.notification.entity.PushRecord;
import com.yunzhongyi.medical.notification.service.PushRecordService;
import com.yunzhongyi.medical.notification.service.SiteMessageService;
import com.yunzhongyi.medical.physician.dto.PhysicianQuery;
import com.yunzhongyi.medical.physician.service.PhysicianService;
import com.yunzhongyi.medical.schedule.dto.ScheduleQuery;
import com.yunzhongyi.medical.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 医务管理移动端总览接口：移动端首页聚合数据（只读）与移动端消息推送。
 * <p>
 * 查看类数据全部聚合自已有模块的服务层，不另造业务表：
 * 医师档案（只读分页）、今日排班（只读）、待审批汇总、考核统计、
 * 设备统计、消息与推送统计。
 * 消息推送复用 29 号消息提醒的推送记录：移动端推送固定走 APP 渠道，
 * 推送通道本机没有真实服务，发送明确返回失败、如实记录，不伪造发送成功。
 */
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileOverviewController {

    private final PhysicianService physicianService;
    private final LeaveApplicationService leaveApplicationService;
    private final ScheduleService scheduleService;
    private final AssessmentService assessmentService;
    private final MobileApprovalService mobileApprovalService;
    private final MobileDeviceService mobileDeviceService;
    private final PushRecordService pushRecordService;
    private final SiteMessageService siteMessageService;

    /** 移动端首页总览：医师 / 待审批 / 考核 / 设备 / 消息 / 推送的数量汇总 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String roleCode) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("physicians", physicianService.count());
        r.put("pendingApprovals", mobileApprovalService.pendingCount().get("total"));
        r.put("pendingByModule", mobileApprovalService.pendingCount().get("byModule"));
        Map<String, Object> leaveStats = leaveApplicationService.stats();
        r.put("leavePending", leaveStats.get("pending"));
        Map<String, Object> scheduleStats = scheduleService.stats();
        r.put("schedules", scheduleStats.get("total"));
        r.put("pendingScheduleChanges", scheduleStats.get("pendingChange"));
        ScheduleQuery today = new ScheduleQuery();
        today.setStartDate(LocalDate.now());
        today.setEndDate(LocalDate.now());
        r.put("todaySchedules", (long) scheduleService.listByQuery(today).size());
        Map<String, Object> assessmentStats = assessmentService.stats();
        r.put("assessments", assessmentStats.get("total"));
        r.put("assessmentAvgScore", assessmentStats.get("avgScore"));
        r.put("assessmentExcellentCount", assessmentStats.get("excellentCount"));
        Map<String, Object> deviceStats = mobileDeviceService.stats();
        r.put("devices", deviceStats.get("total"));
        r.put("boundDevices", deviceStats.get("bound"));
        r.put("unboundDevices", deviceStats.get("unbound"));
        r.put("devicesByPlatform", deviceStats.get("byPlatform"));
        Map<String, Object> pushStats = pushRecordService.stats();
        r.put("pushRecords", pushStats.get("total"));
        r.put("failedPush", pushStats.get("failed"));
        r.put("unreadMessages", siteMessageService.unreadCount(username, roleCode));
        r.put("approvalLogs", mobileApprovalService.count());
        return Result.ok(r);
    }

    /** 移动端查看医师档案（只读分页，与 PC 端同一数据源） */
    @GetMapping("/physicians")
    public Result<?> physicians(PhysicianQuery query) {
        return Result.ok(physicianService.pageQuery(query));
    }

    /** 移动端查看今日排班（只读，按当天日期过滤） */
    @GetMapping("/schedules/today")
    public Result<?> todaySchedules() {
        ScheduleQuery q = new ScheduleQuery();
        q.setStartDate(LocalDate.now());
        q.setEndDate(LocalDate.now());
        return Result.ok(scheduleService.listByQuery(q));
    }

    /** 移动端消息推送：固定走 APP 渠道，复用 29 号推送记录；通道未对接时如实返回失败 */
    @PostMapping("/push")
    public Result<Map<String, Object>> push(@RequestBody PushRecord record) {
        record.setChannel("APP");
        return Result.ok(pushRecordService.send(record));
    }

    /** 移动端推送统计：总数 / 待发送 / 已发送 / 发送失败 / 按渠道分组 */
    @GetMapping("/push/stats")
    public Result<Map<String, Object>> pushStats() {
        return Result.ok(pushRecordService.stats());
    }
}
