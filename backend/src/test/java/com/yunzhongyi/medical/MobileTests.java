package com.yunzhongyi.medical;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunzhongyi.medical.assessment.service.AssessmentService;
import com.yunzhongyi.medical.leave.entity.LeaveApplication;
import com.yunzhongyi.medical.leave.service.LeaveApplicationService;
import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import com.yunzhongyi.medical.mdt.mapper.MdtApplicationMapper;
import com.yunzhongyi.medical.mdt.service.MdtService;
import com.yunzhongyi.medical.mobile.controller.MobileApprovalController;
import com.yunzhongyi.medical.mobile.controller.MobileDeviceController;
import com.yunzhongyi.medical.mobile.controller.MobileOverviewController;
import com.yunzhongyi.medical.mobile.dto.MobileApprovalLogQuery;
import com.yunzhongyi.medical.mobile.dto.MobileApproveRequest;
import com.yunzhongyi.medical.mobile.dto.MobileDeviceQuery;
import com.yunzhongyi.medical.mobile.dto.MobilePendingItem;
import com.yunzhongyi.medical.mobile.entity.MobileApprovalLog;
import com.yunzhongyi.medical.mobile.entity.MobileDevice;
import com.yunzhongyi.medical.mobile.excel.MobileDeviceExcelUtil;
import com.yunzhongyi.medical.mobile.service.MobileApprovalService;
import com.yunzhongyi.medical.mobile.service.MobileDeviceService;
import com.yunzhongyi.medical.notification.entity.PushRecord;
import com.yunzhongyi.medical.notification.mapper.PushRecordMapper;
import com.yunzhongyi.medical.notification.service.PushRecordService;
import com.yunzhongyi.medical.schedule.dto.ScheduleQuery;
import com.yunzhongyi.medical.schedule.entity.Schedule;
import com.yunzhongyi.medical.schedule.service.ScheduleService;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 医务管理移动端模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 台移动端设备（已绑定 4 / 已解绑 2；iOS 3 / Android 3）、
 * 6 条移动端审批操作记录（通过 5 / 驳回 1，全部成功）。
 * 待审批聚合：8 个业务模块共 19 条（休假执业 4 / 重点授权 4 / 排班调班 1 /
 * MDT 2 / 新技术新项目 1 / 医疗人员动态 4 / 医疗分级 2 / 医疗工作简报 1）。
 * 审批复用各业务模块已有逻辑；移动端推送复用 29 号推送记录走 APP 渠道，
 * 通道未对接时如实返回失败，不伪造发送成功。
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class MobileTests {

    @Autowired
    private MobileDeviceService mobileDeviceService;

    @Autowired
    private MobileApprovalService mobileApprovalService;

    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private MdtService mdtService;

    @Autowired
    private MdtApplicationMapper mdtApplicationMapper;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private PushRecordService pushRecordService;

    @Autowired
    private PushRecordMapper pushRecordMapper;

    @Autowired
    private MobileDeviceController mobileDeviceController;

    @Autowired
    private MobileApprovalController mobileApprovalController;

    @Autowired
    private MobileOverviewController mobileOverviewController;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(mobileDeviceService);
        assertNotNull(mobileApprovalService);
        assertNotNull(mobileDeviceController);
        assertNotNull(mobileApprovalController);
        assertNotNull(mobileOverviewController);
    }

    @Test
    void seedDataLoaded() {
        assertEquals(6L, mobileDeviceService.count());
        assertEquals(6L, mobileApprovalService.count());
        assertEquals("DEV-001", mobileDeviceService.getById(1L).getDeviceCode());
        assertEquals("IOS", mobileDeviceService.getById(1L).getPlatform());
        assertEquals(Integer.valueOf(1), mobileDeviceService.getById(1L).getStatus());
        assertEquals(Integer.valueOf(0), mobileDeviceService.getById(5L).getStatus());
        assertEquals("ANDROID", mobileDeviceService.getById(6L).getPlatform());
        assertEquals("LEAVE", mobileApprovalService.getById(1L).getModuleType());
        assertEquals("APPROVE", mobileApprovalService.getById(1L).getAction());
        assertEquals("SUCCESS", mobileApprovalService.getById(1L).getResult());
        assertEquals("陈国强休假申请", mobileApprovalService.getById(1L).getRelatedTitle());
        assertEquals("REJECT", mobileApprovalService.getById(6L).getAction());
        assertEquals("黄志明进修申请", mobileApprovalService.getById(6L).getRelatedTitle());
    }

    @Test
    void devicePageQueryFilters() {
        // 按平台过滤：iOS 3 台
        MobileDeviceQuery ios = new MobileDeviceQuery();
        ios.setPlatform("IOS");
        assertEquals(3L, mobileDeviceService.pageQuery(ios).getTotal());
        // 按状态过滤：已绑定 4 台
        MobileDeviceQuery bound = new MobileDeviceQuery();
        bound.setStatus(1);
        assertEquals(4L, mobileDeviceService.pageQuery(bound).getTotal());
        // 按关键字过滤：DEV-001 1 台；使用人“陈医生”1 台
        MobileDeviceQuery kw = new MobileDeviceQuery();
        kw.setKeyword("DEV-001");
        assertEquals(1L, mobileDeviceService.pageQuery(kw).getTotal());
        MobileDeviceQuery kw2 = new MobileDeviceQuery();
        kw2.setKeyword("陈医生");
        assertEquals(1L, mobileDeviceService.pageQuery(kw2).getTotal());
        // 按使用人过滤
        MobileDeviceQuery user = new MobileDeviceQuery();
        user.setUsername("admin");
        assertEquals(1L, mobileDeviceService.pageQuery(user).getTotal());
        // 审批日志按模块过滤：休假执业 2 条
        MobileApprovalLogQuery logs = new MobileApprovalLogQuery();
        logs.setModuleType("LEAVE");
        assertEquals(2L, mobileApprovalService.pageQuery(logs).getTotal());
    }

    @Test
    void deviceStats() {
        Map<String, Object> stats = mobileDeviceService.stats();
        assertEquals(6L, stats.get("total"));
        assertEquals(4L, stats.get("bound"));
        assertEquals(2L, stats.get("unbound"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byPlatform = (Map<String, Long>) stats.get("byPlatform");
        assertEquals(3L, byPlatform.get("IOS"));
        assertEquals(3L, byPlatform.get("ANDROID"));
    }

    @Test
    void deviceValidation() {
        // 设备编码为空拒绝
        MobileDevice noCode = newDevice(null, "admin");
        assertThrows(IllegalArgumentException.class, () -> mobileDeviceService.register(noCode));
        // 使用人为空拒绝
        MobileDevice noUser = newDevice("DEV-TEST-001", null);
        assertThrows(IllegalArgumentException.class, () -> mobileDeviceService.register(noUser));
        // 平台非法拒绝
        MobileDevice badPlatform = newDevice("DEV-TEST-001", "admin");
        badPlatform.setPlatform("WINDOWS");
        assertThrows(IllegalArgumentException.class, () -> mobileDeviceService.register(badPlatform));
        // 设备编码重复拒绝
        MobileDevice dup = newDevice("DEV-001", "admin");
        assertThrows(IllegalArgumentException.class, () -> mobileDeviceService.register(dup));
        // 合法设备默认已绑定、最近活跃时间有值
        MobileDevice ok = newDevice("DEV-TEST-001", "admin");
        try {
            MobileDevice created = mobileDeviceService.register(ok);
            assertEquals(Integer.valueOf(1), created.getStatus());
            assertNotNull(created.getLastActiveTime());
        } finally {
            if (ok.getId() != null) {
                mobileDeviceService.removeById(ok.getId());
            }
        }
    }

    @Test
    void deviceBindUnbindFlow() {
        MobileDevice created = null;
        try {
            created = mobileDeviceService.register(newDevice("DEV-TEST-002", "chen"));
            assertEquals(Integer.valueOf(1), created.getStatus());
            // 已绑定重复绑定拒绝
            Long id = created.getId();
            assertThrows(IllegalArgumentException.class, () -> mobileDeviceService.bind(id));
            // 已绑定的设备不可直接删除
            assertThrows(IllegalArgumentException.class, () -> mobileDeviceService.delete(id));
            // 解绑：状态 0；重复解绑拒绝
            MobileDevice unbound = mobileDeviceService.unbind(id);
            assertEquals(Integer.valueOf(0), unbound.getStatus());
            assertThrows(IllegalArgumentException.class, () -> mobileDeviceService.unbind(id));
            // 重新绑定：状态 1、最近活跃时间更新
            MobileDevice bound = mobileDeviceService.bind(id);
            assertEquals(Integer.valueOf(1), bound.getStatus());
            assertNotNull(bound.getLastActiveTime());
            // 解绑后可删除
            mobileDeviceService.unbind(id);
            mobileDeviceService.delete(id);
            created = null;
            assertNull(mobileDeviceService.getById(id));
        } finally {
            if (created != null && created.getId() != null) {
                mobileDeviceService.removeById(created.getId());
            }
        }
    }

    @Test
    void deviceImportExportRoundTrip() throws Exception {
        // 新建一条临时设备，导出后删除再导入补回 1 条，重复导入 0 条
        MobileDevice temp = mobileDeviceService.register(newDevice("DEV-TEST-003", "wang"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mobileDeviceService.exportExcel(new MobileDeviceQuery(), out);
        List<MobileDevice> rows = MobileDeviceExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(7, rows.size());
        assertTrue(mobileDeviceService.removeById(temp.getId()));
        Map<String, Object> first = mobileDeviceService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, first.get("created"));
        assertEquals(6, first.get("skipped"));
        Map<String, Object> second = mobileDeviceService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, second.get("created"));
        assertEquals(7, second.get("skipped"));
        MobileDevice restored = mobileDeviceService.getOne(new LambdaQueryWrapper<MobileDevice>()
                .eq(MobileDevice::getDeviceCode, "DEV-TEST-003"));
        assertNotNull(restored);
        assertEquals(Integer.valueOf(1), restored.getStatus());
        mobileDeviceService.removeById(restored.getId());
    }

    @Test
    void pendingAggregation() {
        List<MobilePendingItem> all = mobileApprovalService.pendingList(null, null);
        assertEquals(19, all.size());
        assertEquals(4L, all.stream().filter(i -> "LEAVE".equals(i.getModuleType())).count());
        assertEquals(4L, all.stream().filter(i -> "AUTH".equals(i.getModuleType())).count());
        assertEquals(1L, all.stream().filter(i -> "SCHEDULE".equals(i.getModuleType())).count());
        assertEquals(2L, all.stream().filter(i -> "MDT".equals(i.getModuleType())).count());
        assertEquals(1L, all.stream().filter(i -> "NEWTECH".equals(i.getModuleType())).count());
        assertEquals(4L, all.stream().filter(i -> "PERSONNEL".equals(i.getModuleType())).count());
        assertEquals(2L, all.stream().filter(i -> "MEDGRADING".equals(i.getModuleType())).count());
        assertEquals(1L, all.stream().filter(i -> "BRIEFING".equals(i.getModuleType())).count());
        // 按模块过滤：休假执业 4 条，含孙小红休假申请（待科主任审批）
        List<MobilePendingItem> leaves = mobileApprovalService.pendingList("LEAVE", null);
        assertEquals(4, leaves.size());
        assertTrue(leaves.stream().anyMatch(i -> "孙小红休假申请".equals(i.getTitle())
                && "待科主任审批".equals(i.getStatusText())));
        assertTrue(leaves.stream().anyMatch(i -> i.getRelatedId() == 2L
                && "待医务科审批".equals(i.getStatusText())));
        // 按关键字过滤：孙小红 2 条（休假 1 条 + 重点授权 1 条）
        assertEquals(2, mobileApprovalService.pendingList(null, "孙小红").size());
        // 非法模块拒绝
        assertThrows(IllegalArgumentException.class,
                () -> mobileApprovalService.pendingList("NO_SUCH_MODULE", null));
    }

    @Test
    void pendingScheduleTitle() {
        List<MobilePendingItem> schedules = mobileApprovalService.pendingList("SCHEDULE", null);
        assertEquals(1, schedules.size());
        MobilePendingItem item = schedules.get(0);
        assertEquals("李雅琴", item.getApplicant());
        assertTrue(item.getTitle().contains("调班"));
        assertTrue(item.getTitle().contains("2026-11-25"));
        assertEquals("待科主任审批", item.getStatusText());
    }

    @Test
    void pendingCount() {
        Map<String, Object> count = mobileApprovalService.pendingCount();
        assertEquals(19L, count.get("total"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byModule = (Map<String, Long>) count.get("byModule");
        assertEquals(4L, byModule.get("LEAVE"));
        assertEquals(4L, byModule.get("AUTH"));
        assertEquals(1L, byModule.get("SCHEDULE"));
        assertEquals(2L, byModule.get("MDT"));
        assertEquals(1L, byModule.get("NEWTECH"));
        assertEquals(4L, byModule.get("PERSONNEL"));
        assertEquals(2L, byModule.get("MEDGRADING"));
        assertEquals(1L, byModule.get("BRIEFING"));
    }

    @Test
    void mobileApproveLeaveFlow() {
        // 孙小红休假申请（id=4，待科主任审批）：移动端两级审批 0→1→2，每级都记 SUCCESS 日志
        MobileApproveRequest req = new MobileApproveRequest();
        req.setOperator("吴文斌");
        req.setDeviceCode("DEV-001");
        req.setOpinion("同意");
        MobileApprovalLog first = mobileApprovalService.approve("LEAVE", 4L, req);
        MobileApprovalLog second = null;
        try {
            assertEquals("SUCCESS", first.getResult());
            assertEquals("孙小红休假申请", first.getRelatedTitle());
            assertEquals(Integer.valueOf(1), leaveApplicationService.getById(4L).getStatus());
            second = mobileApprovalService.approve("LEAVE", 4L, req);
            assertEquals("SUCCESS", second.getResult());
            assertEquals(Integer.valueOf(2), leaveApplicationService.getById(4L).getStatus());
        } finally {
            // 恢复种子状态：待科主任审批、审批历史清空；删除本测试产生的两条日志
            LeaveApplication a = leaveApplicationService.getById(4L);
            a.setStatus(0);
            a.setCurrentApprover("科主任");
            a.setApprovalOpinion(null);
            a.setApprovalHistory(null);
            leaveApplicationService.updateById(a);
            if (first != null && first.getId() != null) {
                mobileApprovalService.removeById(first.getId());
            }
            if (second != null && second.getId() != null) {
                mobileApprovalService.removeById(second.getId());
            }
        }
        assertEquals(Integer.valueOf(0), leaveApplicationService.getById(4L).getStatus());
        assertEquals(4, mobileApprovalService.pendingList("LEAVE", null).size());
    }

    @Test
    void mobileApproveScheduleFlow() {
        // 李雅琴调班申请（id=7，待科主任审批）：移动端审批通过后调班状态 1→2
        MobileApproveRequest req = new MobileApproveRequest();
        req.setOperator("吴文斌");
        req.setDeviceCode("DEV-001");
        req.setOpinion("同意调班");
        MobileApprovalLog log = mobileApprovalService.approve("SCHEDULE", 7L, req);
        try {
            assertEquals("SUCCESS", log.getResult());
            assertTrue(log.getRelatedTitle().contains("李雅琴"));
            assertEquals(Integer.valueOf(2), scheduleService.getById(7L).getChangeStatus());
        } finally {
            Schedule s = scheduleService.getById(7L);
            s.setChangeStatus(1);
            s.setApprover(null);
            s.setApprovalOpinion(null);
            s.setApprovalHistory(null);
            scheduleService.updateById(s);
            if (log.getId() != null) {
                mobileApprovalService.removeById(log.getId());
            }
        }
        assertEquals(Integer.valueOf(1), scheduleService.getById(7L).getChangeStatus());
    }

    @Test
    void mobileRejectFlow() {
        // 王丽华 MDT 会诊申请（id=5，待审批）：移动端驳回后状态 0→2
        MobileApproveRequest req = new MobileApproveRequest();
        req.setOperator("吴文斌");
        req.setDeviceCode("DEV-002");
        req.setOpinion("资料不全，请补充后重报");
        MobileApprovalLog log = mobileApprovalService.reject("MDT", 5L, req);
        try {
            assertEquals("SUCCESS", log.getResult());
            assertEquals("REJECT", log.getAction());
            assertEquals(Integer.valueOf(2), mdtService.getById(5L).getStatus());
        } finally {
            // 恢复种子状态：MdtService.updateById 有“已结束不可修改”守卫，
            // 恢复须走 mapper 绕过该守卫
            MdtApplication m = mdtService.getById(5L);
            m.setStatus(0);
            m.setApprover(null);
            m.setApprovalOpinion(null);
            mdtApplicationMapper.updateById(m);
            if (log.getId() != null) {
                mobileApprovalService.removeById(log.getId());
            }
        }
        assertEquals(Integer.valueOf(0), mdtService.getById(5L).getStatus());
        // 驳回不填原因拒绝
        MobileApproveRequest noReason = new MobileApproveRequest();
        noReason.setOperator("吴文斌");
        assertThrows(IllegalArgumentException.class,
                () -> mobileApprovalService.reject("MDT", 5L, noReason));
        // 不存在的事项拒绝
        MobileApproveRequest req2 = new MobileApproveRequest();
        req2.setOperator("吴文斌");
        req2.setOpinion("驳回");
        assertThrows(IllegalArgumentException.class,
                () -> mobileApprovalService.reject("MDT", 999L, req2));
    }

    @Test
    void mobileApproveFailureLogsFail() {
        // 陈国强休假申请（id=1）已通过：移动端再审批委托失败，记 FAIL 日志并原样抛错
        MobileApproveRequest req = new MobileApproveRequest();
        req.setOperator("吴文斌");
        req.setDeviceCode("DEV-001");
        req.setOpinion("同意");
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> mobileApprovalService.approve("LEAVE", 1L, req));
        assertTrue(thrown.getMessage().contains("已结束"));
        MobileApprovalLog failLog = mobileApprovalService.getOne(new LambdaQueryWrapper<MobileApprovalLog>()
                .eq(MobileApprovalLog::getModuleType, "LEAVE")
                .eq(MobileApprovalLog::getRelatedId, 1L)
                .eq(MobileApprovalLog::getResult, "FAIL"));
        assertNotNull(failLog);
        assertEquals("APPROVE", failLog.getAction());
        assertTrue(failLog.getFailReason().contains("已结束"));
        mobileApprovalService.removeById(failLog.getId());
        // 种子 SUCCESS 日志仍在
        assertEquals("SUCCESS", mobileApprovalService.getById(1L).getResult());
    }

    @Test
    void mobileApproveValidation() {
        MobileApproveRequest req = new MobileApproveRequest();
        req.setOperator("吴文斌");
        req.setOpinion("同意");
        // 非法模块拒绝
        assertThrows(IllegalArgumentException.class,
                () -> mobileApprovalService.approve("NO_SUCH_MODULE", 1L, req));
        // 审批人为空拒绝
        MobileApproveRequest noOperator = new MobileApproveRequest();
        noOperator.setOpinion("同意");
        assertThrows(IllegalArgumentException.class,
                () -> mobileApprovalService.approve("LEAVE", 4L, noOperator));
        // 不存在的事项拒绝
        assertThrows(IllegalArgumentException.class,
                () -> mobileApprovalService.approve("LEAVE", 999L, req));
    }

    @Test
    void approvalLogStats() {
        Map<String, Object> stats = mobileApprovalService.stats();
        assertEquals(6L, stats.get("total"));
        assertEquals(5L, stats.get("approved"));
        assertEquals(1L, stats.get("rejected"));
        assertEquals(6L, stats.get("success"));
        assertEquals(0L, stats.get("failed"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byModule = (Map<String, Long>) stats.get("byModule");
        assertEquals(2L, byModule.get("LEAVE"));
        assertEquals(1L, byModule.get("AUTH"));
        assertEquals(0L, byModule.get("SCHEDULE"));
        assertEquals(1L, byModule.get("MDT"));
        assertEquals(1L, byModule.get("NEWTECH"));
        assertEquals(0L, byModule.get("PERSONNEL"));
        assertEquals(0L, byModule.get("MEDGRADING"));
        assertEquals(1L, byModule.get("BRIEFING"));
    }

    @Test
    void approvalLogExport() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mobileApprovalService.exportExcel(new MobileApprovalLogQuery(), out);
        assertTrue(out.size() > 0);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals(7, sheet.getLastRowNum() + 1);
            assertEquals("业务模块", sheet.getRow(0).getCell(0).getStringCellValue());
            // 按操作时间倒序：首行是 2026-11-22 的简报审批，末行是 2026-11-21 最早的休假驳回
            assertEquals("医疗工作简报", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("休假执业", sheet.getRow(6).getCell(0).getStringCellValue());
            assertEquals("驳回", sheet.getRow(6).getCell(3).getStringCellValue());
        }
    }

    @Test
    void overviewStats() {
        Map<String, Object> data = mobileOverviewController.overview(null, null).getData();
        assertEquals(12L, data.get("physicians"));
        assertEquals(19L, data.get("pendingApprovals"));
        assertEquals(4L, data.get("leavePending"));
        assertEquals(8L, data.get("schedules"));
        assertEquals(1L, data.get("pendingScheduleChanges"));
        ScheduleQuery today = new ScheduleQuery();
        today.setStartDate(LocalDate.now());
        today.setEndDate(LocalDate.now());
        assertEquals((long) scheduleService.listByQuery(today).size(), data.get("todaySchedules"));
        assertEquals(6L, data.get("assessments"));
        assertEquals(assessmentService.stats().get("avgScore"), data.get("assessmentAvgScore"));
        assertEquals(6L, data.get("devices"));
        assertEquals(4L, data.get("boundDevices"));
        assertEquals(2L, data.get("unboundDevices"));
        assertEquals(4L, data.get("pushRecords"));
        assertEquals(3L, data.get("failedPush"));
        assertEquals(4L, data.get("unreadMessages"));
        assertEquals(6L, data.get("approvalLogs"));
    }

    @Test
    void pushSendPlaceholder() {
        // 移动端推送走 APP 渠道：预留占位明确返回失败、如实记录，不伪造发送成功
        PushRecord record = new PushRecord();
        record.setTitle("占位推送测试");
        record.setContent("测试内容");
        record.setChannel("APP");
        record.setReceiverScope("ALL");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = pushRecordService.send(record);
        Long id = record.getId();
        try {
            assertEquals(Boolean.FALSE, result.get("success"));
            assertTrue(String.valueOf(result.get("message")).contains("未对接"));
            PushRecord saved = pushRecordService.getById(id);
            assertEquals("APP", saved.getChannel());
            assertEquals(Integer.valueOf(2), saved.getSendStatus());
            assertTrue(saved.getFailReason().contains("未对接"));
            // 重推：再次失败如实记录
            @SuppressWarnings("unchecked")
            Map<String, Object> retried = pushRecordService.retry(id);
            assertEquals(Boolean.FALSE, retried.get("success"));
            assertEquals(Integer.valueOf(2), pushRecordService.getById(id).getSendStatus());
        } finally {
            if (id != null) {
                pushRecordMapper.deleteById(id);
            }
        }
    }

    @Test
    void mobileEndpoints() throws Exception {
        // 设备分页与统计
        mockMvc.perform(get("/api/mobile-devices").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(6));
        mockMvc.perform(get("/api/mobile-devices/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bound").value(4));
        // 待审批计数与总览
        mockMvc.perform(get("/api/mobile-approvals/pending/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(19));
        mockMvc.perform(get("/api/mobile/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.physicians").value(12))
                .andExpect(jsonPath("$.data.pendingApprovals").value(19));
        // 移动端查看医师档案（只读）与今日排班
        mockMvc.perform(get("/api/mobile/physicians").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(12));
        MvcResult todayResult = mockMvc.perform(get("/api/mobile/schedules/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        ScheduleQuery today = new ScheduleQuery();
        today.setStartDate(LocalDate.now());
        today.setEndDate(LocalDate.now());
        JsonNode todayNode = new ObjectMapper().readTree(todayResult.getResponse().getContentAsString());
        assertEquals(scheduleService.listByQuery(today).size(), todayNode.get("data").size());
        // 待审批列表：休假执业 4 条
        MvcResult pendingResult = mockMvc.perform(get("/api/mobile-approvals/pending")
                        .param("moduleType", "LEAVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode pendingNode = new ObjectMapper().readTree(pendingResult.getResponse().getContentAsString());
        assertEquals(4, pendingNode.get("data").size());
        // 移动端审批接口：李秀英 MDT 会诊申请（id=2）审批通过，状态 0→1，日志留痕
        mockMvc.perform(post("/api/mobile-approvals/MDT/2/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operator\":\"吴文斌\",\"deviceCode\":\"DEV-001\",\"opinion\":\"同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.result").value("SUCCESS"));
        assertEquals(Integer.valueOf(1), mdtService.getById(2L).getStatus());
        MdtApplication m = mdtService.getById(2L);
        m.setStatus(0);
        m.setApprover(null);
        m.setApprovalOpinion(null);
        mdtApplicationMapper.updateById(m);
        MobileApprovalLog log = mobileApprovalService.getOne(new LambdaQueryWrapper<MobileApprovalLog>()
                .eq(MobileApprovalLog::getModuleType, "MDT")
                .eq(MobileApprovalLog::getRelatedId, 2L)
                .eq(MobileApprovalLog::getAction, "APPROVE"));
        assertNotNull(log);
        mobileApprovalService.removeById(log.getId());
        // 已结束的申请移动端再审批：400，且记 FAIL 日志，随后清理
        mockMvc.perform(post("/api/mobile-approvals/LEAVE/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operator\":\"吴文斌\",\"opinion\":\"同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        MobileApprovalLog failLog = mobileApprovalService.getOne(new LambdaQueryWrapper<MobileApprovalLog>()
                .eq(MobileApprovalLog::getModuleType, "LEAVE")
                .eq(MobileApprovalLog::getRelatedId, 1L)
                .eq(MobileApprovalLog::getResult, "FAIL"));
        assertNotNull(failLog);
        mobileApprovalService.removeById(failLog.getId());
        // 移动端推送接口：占位失败，HTTP 200、success=false
        mockMvc.perform(post("/api/mobile/push").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"接口推送测试\",\"receiverScope\":\"ALL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(false));
        PushRecord created = pushRecordService.getOne(new LambdaQueryWrapper<PushRecord>()
                .eq(PushRecord::getTitle, "接口推送测试"));
        assertNotNull(created);
        assertEquals("APP", created.getChannel());
        pushRecordMapper.deleteById(created.getId());
        // 不存在的设备绑定：400
        mockMvc.perform(post("/api/mobile-devices/999/bind"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ---------- 构造 helper ----------

    private MobileDevice newDevice(String deviceCode, String username) {
        MobileDevice d = new MobileDevice();
        d.setDeviceCode(deviceCode);
        d.setUsername(username);
        d.setDisplayName("测试用户");
        d.setPlatform("IOS");
        d.setDeviceModel("iPhone 15");
        d.setAppVersion("1.0.0");
        return d;
    }
}
