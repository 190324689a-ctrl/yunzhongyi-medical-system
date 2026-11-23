package com.yunzhongyi.medical;

import com.yunzhongyi.medical.notification.controller.NotificationOverviewController;
import com.yunzhongyi.medical.notification.dto.GenerateTodoRequest;
import com.yunzhongyi.medical.notification.dto.PushRecordQuery;
import com.yunzhongyi.medical.notification.dto.SiteMessageQuery;
import com.yunzhongyi.medical.notification.dto.SmsRecordQuery;
import com.yunzhongyi.medical.notification.dto.TodoReminderQuery;
import com.yunzhongyi.medical.notification.entity.PushRecord;
import com.yunzhongyi.medical.notification.entity.SiteMessage;
import com.yunzhongyi.medical.notification.entity.SmsRecord;
import com.yunzhongyi.medical.notification.entity.TodoReminder;
import com.yunzhongyi.medical.notification.excel.SiteMessageExcelUtil;
import com.yunzhongyi.medical.notification.excel.TodoReminderExcelUtil;
import com.yunzhongyi.medical.notification.mapper.PushRecordMapper;
import com.yunzhongyi.medical.notification.mapper.SmsRecordMapper;
import com.yunzhongyi.medical.notification.service.PushRecordService;
import com.yunzhongyi.medical.notification.service.SiteMessageService;
import com.yunzhongyi.medical.notification.service.SmsRecordService;
import com.yunzhongyi.medical.notification.service.TodoReminderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
 * 消息提醒模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 条站内消息（未读 4 / 已读 2；通知 1 / 公告 1 / 待办提醒 3 / 系统消息 1；
 * 全部人员 2 / 按角色 2 / 按人员 2）、6 条待办提醒（待办 4 / 已完成 1 / 已逾期 1；
 * 审批待办 2 / 到期提醒 1 / 整改待办 1 / 其他待办 2；手工创建 2 / 系统生成 2 /
 * 按提醒设置生成 2）、4 条消息推送记录（待发送 1 / 已发送 0 / 发送失败 3；
 * 微信 2 / 移动端 1 / 邮件 1）、4 条短信提醒记录（待发送 1 / 已发送 0 / 发送失败 3）。
 * 推送与短信通道本机无真实服务：发送与重推均为预留占位，明确返回失败、如实记录。
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class NotificationTests {

    @Autowired
    private SiteMessageService siteMessageService;

    @Autowired
    private TodoReminderService todoReminderService;

    @Autowired
    private PushRecordService pushRecordService;

    @Autowired
    private SmsRecordService smsRecordService;

    @Autowired
    private PushRecordMapper pushRecordMapper;

    @Autowired
    private SmsRecordMapper smsRecordMapper;

    @Autowired
    private NotificationOverviewController overviewController;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(siteMessageService);
        assertNotNull(todoReminderService);
        assertNotNull(pushRecordService);
        assertNotNull(smsRecordService);
        assertNotNull(overviewController);
    }

    @Test
    void seedDataLoaded() {
        assertEquals(6L, siteMessageService.count());
        assertEquals(6L, todoReminderService.count());
        assertEquals(4L, pushRecordService.count());
        assertEquals(4L, smsRecordService.count());
        assertEquals("关于开展三级医师查房专项检查的通知", siteMessageService.getById(1L).getTitle());
        assertEquals(Integer.valueOf(1), siteMessageService.getById(3L).getReadStatus());
        assertEquals(Integer.valueOf(0), siteMessageService.getById(2L).getReadStatus());
        assertEquals("审批孙小红休假申请", todoReminderService.getById(1L).getTitle());
        assertEquals(Integer.valueOf(2), todoReminderService.getById(5L).getStatus());
        assertEquals("AUTH_EXPIRE", todoReminderService.getById(3L).getSettingKey());
        assertEquals(Integer.valueOf(2), pushRecordService.getById(1L).getSendStatus());
        assertTrue(pushRecordService.getById(1L).getFailReason().contains("未对接"));
        assertEquals(Integer.valueOf(0), pushRecordService.getById(4L).getSendStatus());
        assertEquals("13812340001", smsRecordService.getById(1L).getPhone());
        assertEquals(Integer.valueOf(2), smsRecordService.getById(2L).getSendStatus());
    }

    @Test
    void messageStats() {
        Map<String, Object> stats = siteMessageService.stats();
        assertEquals(6L, stats.get("total"));
        assertEquals(4L, stats.get("unread"));
        assertEquals(2L, stats.get("read"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byCategory = (Map<String, Long>) stats.get("byCategory");
        assertEquals(1L, byCategory.get("NOTICE"));
        assertEquals(1L, byCategory.get("ANNOUNCE"));
        assertEquals(3L, byCategory.get("TODO"));
        assertEquals(1L, byCategory.get("SYSTEM"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byReceiver = (Map<String, Long>) stats.get("byReceiverScope");
        assertEquals(2L, byReceiver.get("ALL"));
        assertEquals(2L, byReceiver.get("ROLE"));
        assertEquals(2L, byReceiver.get("USER"));
    }

    @Test
    void messageValidation() {
        // 标题为空拒绝
        SiteMessage noTitle = newMessage(null, "NOTICE", "ALL", null);
        assertThrows(IllegalArgumentException.class, () -> siteMessageService.create(noTitle));
        // 分类非法拒绝
        SiteMessage badCat = newMessage("分类非法", "ALERT", "ALL", null);
        assertThrows(IllegalArgumentException.class, () -> siteMessageService.create(badCat));
        // 接收范围非法拒绝
        SiteMessage badScope = newMessage("范围非法", "NOTICE", "DEPT", null);
        assertThrows(IllegalArgumentException.class, () -> siteMessageService.create(badScope));
        // 按角色接收不填角色编码拒绝
        SiteMessage noRole = newMessage("角色缺失", "NOTICE", "ROLE", null);
        assertThrows(IllegalArgumentException.class, () -> siteMessageService.create(noRole));
        // 按人员接收不填用户名拒绝
        SiteMessage noUser = newMessage("人员缺失", "NOTICE", "USER", null);
        assertThrows(IllegalArgumentException.class, () -> siteMessageService.create(noUser));
        // 全部人员时接收对象强制清空
        SiteMessage all = newMessage("全部人员", "NOTICE", "ALL", "MEDICAL_ADMIN");
        try {
            SiteMessage created = siteMessageService.create(all);
            assertNull(created.getReceiverValue());
            assertEquals(Integer.valueOf(0), created.getReadStatus());
            assertNotNull(created.getSendTime());
        } finally {
            if (all.getId() != null) {
                siteMessageService.removeById(all.getId());
            }
        }
    }

    @Test
    void messageMarkReadFlow() {
        SiteMessage created = null;
        try {
            created = siteMessageService.create(newMessage("标已读测试", "NOTICE", "ALL", null));
            assertEquals(Integer.valueOf(0), created.getReadStatus());
            // 已读消息不可修改
            SiteMessage edit = siteMessageService.getById(created.getId());
            edit.setTitle("标已读测试（改）");
            SiteMessage updated = siteMessageService.update(edit);
            assertEquals("标已读测试（改）", updated.getTitle());
            // 标已读：已读状态 1、已读时间有值
            SiteMessage read = siteMessageService.markRead(created.getId());
            assertEquals(Integer.valueOf(1), read.getReadStatus());
            assertNotNull(read.getReadTime());
            // 重复标已读拒绝
            Long id = created.getId();
            assertThrows(IllegalArgumentException.class, () -> siteMessageService.markRead(id));
            // 已读消息不可再修改
            SiteMessage editAgain = siteMessageService.getById(id);
            editAgain.setTitle("已读后修改");
            assertThrows(IllegalArgumentException.class, () -> siteMessageService.update(editAgain));
        } finally {
            if (created != null && created.getId() != null) {
                siteMessageService.removeById(created.getId());
            }
        }
    }

    @Test
    void messageUnreadCount() {
        // admin（角色 MEDICAL_ADMIN）：全部人员未读 1 条 + 按角色未读 1 条 = 2
        assertEquals(2L, siteMessageService.unreadCount("admin", "MEDICAL_ADMIN"));
        // chen（角色 PHYSICIAN）：全部人员未读 1 条 + 按人员未读 1 条 = 2
        assertEquals(2L, siteMessageService.unreadCount("chen", "PHYSICIAN"));
        // 不传用户：全部未读 4 条
        assertEquals(4L, siteMessageService.unreadCount(null, null));
        // 全部标已读后恢复：admin 的 2 条标已读，未读变为 0；全部人员那条是共享行，
        // chen 的未读也随之少 1 条（只剩按人员那条）
        long read = siteMessageService.markAllRead("admin", "MEDICAL_ADMIN");
        assertEquals(2L, read);
        try {
            assertEquals(0L, siteMessageService.unreadCount("admin", "MEDICAL_ADMIN"));
            assertEquals(1L, siteMessageService.unreadCount("chen", "PHYSICIAN"));
        } finally {
            // 恢复种子状态：把标已读的两条改回未读
            SiteMessage m1 = siteMessageService.getById(1L);
            m1.setReadStatus(0);
            m1.setReadTime(null);
            siteMessageService.updateById(m1);
            SiteMessage m2 = siteMessageService.getById(2L);
            m2.setReadStatus(0);
            m2.setReadTime(null);
            siteMessageService.updateById(m2);
        }
        assertEquals(4L, siteMessageService.unreadCount(null, null));
    }

    @Test
    void messageImportExportRoundTrip() throws Exception {
        // 新建一条临时消息，导出后删除再导入补回 1 条，重复导入 0 条
        SiteMessage temp = siteMessageService.create(newMessage("往返测试消息", "NOTICE", "ALL", null));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        siteMessageService.exportExcel(new SiteMessageQuery(), out);
        List<SiteMessage> rows = SiteMessageExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(7, rows.size());
        assertTrue(siteMessageService.removeById(temp.getId()));
        Map<String, Object> first = siteMessageService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, first.get("created"));
        assertEquals(6, first.get("skipped"));
        Map<String, Object> second = siteMessageService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, second.get("created"));
        assertEquals(7, second.get("skipped"));
        SiteMessage restored = siteMessageService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SiteMessage>()
                .eq(SiteMessage::getTitle, "往返测试消息"));
        assertNotNull(restored);
        assertEquals(Integer.valueOf(0), restored.getReadStatus());
        siteMessageService.removeById(restored.getId());
    }

    @Test
    void todoStats() {
        Map<String, Object> stats = todoReminderService.stats();
        assertEquals(6L, stats.get("total"));
        assertEquals(4L, stats.get("pending"));
        assertEquals(1L, stats.get("completed"));
        assertEquals(1L, stats.get("overdue"));
        assertEquals(0L, stats.get("dueToday"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) stats.get("byTodoType");
        assertEquals(2L, byType.get("APPROVAL"));
        assertEquals(1L, byType.get("EXPIRE"));
        assertEquals(1L, byType.get("RECTIFY"));
        assertEquals(2L, byType.get("OTHER"));
        @SuppressWarnings("unchecked")
        Map<String, Long> bySource = (Map<String, Long>) stats.get("bySource");
        assertEquals(2L, bySource.get("MANUAL"));
        assertEquals(2L, bySource.get("AUTO"));
        assertEquals(2L, bySource.get("SETTING"));
    }

    @Test
    void todoValidation() {
        // 标题为空拒绝
        TodoReminder noTitle = newTodo(null, "APPROVAL", "MANUAL");
        assertThrows(IllegalArgumentException.class, () -> todoReminderService.create(noTitle));
        // 类型非法拒绝
        TodoReminder badType = newTodo("类型非法", "URGENT", "MANUAL");
        assertThrows(IllegalArgumentException.class, () -> todoReminderService.create(badType));
        // 来源非法拒绝
        TodoReminder badSource = newTodo("来源非法", "APPROVAL", "API");
        assertThrows(IllegalArgumentException.class, () -> todoReminderService.create(badSource));
        // 按提醒设置生成但不填设置编码拒绝
        TodoReminder noSetting = newTodo("设置缺失", "APPROVAL", "SETTING");
        assertThrows(IllegalArgumentException.class, () -> todoReminderService.create(noSetting));
        // 合法待办默认待办状态、默认其他待办
        TodoReminder ok = new TodoReminder();
        ok.setTitle("合法待办");
        try {
            TodoReminder created = todoReminderService.create(ok);
            assertEquals(Integer.valueOf(0), created.getStatus());
            assertEquals("OTHER", created.getTodoType());
            assertEquals("MANUAL", created.getSource());
        } finally {
            if (ok.getId() != null) {
                todoReminderService.removeById(ok.getId());
            }
        }
    }

    @Test
    void todoCompleteFlow() {
        TodoReminder created = null;
        TodoReminder overdue = null;
        try {
            created = todoReminderService.create(newTodo("完成流转测试", "APPROVAL", "MANUAL"));
            assertEquals(Integer.valueOf(0), created.getStatus());
            // 完成：状态 1、完成时间有值
            TodoReminder done = todoReminderService.complete(created.getId());
            assertEquals(Integer.valueOf(1), done.getStatus());
            assertNotNull(done.getCompleteTime());
            // 重复完成拒绝
            Long id = created.getId();
            assertThrows(IllegalArgumentException.class, () -> todoReminderService.complete(id));
            // 已完成的待办不可修改、不可直接删除
            TodoReminder edit = todoReminderService.getById(id);
            edit.setTitle("已完成后修改");
            assertThrows(IllegalArgumentException.class, () -> todoReminderService.update(edit));
            assertThrows(IllegalArgumentException.class, () -> todoReminderService.delete(id));
            // 已逾期的待办也可以完成（晚完成）
            overdue = todoReminderService.create(newTodo("逾期完成测试", "OTHER", "MANUAL"));
            overdue.setDueDate(LocalDate.now().minusDays(1));
            overdue.setStatus(2);
            todoReminderService.updateById(overdue);
            TodoReminder doneOverdue = todoReminderService.complete(overdue.getId());
            assertEquals(Integer.valueOf(1), doneOverdue.getStatus());
        } finally {
            if (overdue != null && overdue.getId() != null) {
                todoReminderService.removeById(overdue.getId());
            }
            if (created != null && created.getId() != null) {
                todoReminderService.removeById(created.getId());
            }
        }
    }

    @Test
    void todoOverdueScan() {
        TodoReminder past = null;
        TodoReminder future = null;
        try {
            past = todoReminderService.create(newTodo("逾期扫描测试（过期）", "OTHER", "MANUAL"));
            past.setDueDate(LocalDate.now().minusDays(2));
            todoReminderService.updateById(past);
            future = todoReminderService.create(newTodo("逾期扫描测试（未到期）", "OTHER", "MANUAL"));
            future.setDueDate(LocalDate.now().plusDays(2));
            todoReminderService.updateById(future);
            // 扫描：只有已过期那条改为已逾期（种子里已逾期的那条已是 2，不重复计）
            assertEquals(1L, todoReminderService.overdueScan());
            assertEquals(Integer.valueOf(2), todoReminderService.getById(past.getId()).getStatus());
            assertEquals(Integer.valueOf(0), todoReminderService.getById(future.getId()).getStatus());
            // 再次扫描：0 条
            assertEquals(0L, todoReminderService.overdueScan());
        } finally {
            if (past != null && past.getId() != null) {
                todoReminderService.removeById(past.getId());
            }
            if (future != null && future.getId() != null) {
                todoReminderService.removeById(future.getId());
            }
        }
    }

    @Test
    void todoGenerateFromSetting() {
        TodoReminder generated = null;
        try {
            // 按启用的设置生成：来源 SETTING、设置编码回填、默认待办
            GenerateTodoRequest req = new GenerateTodoRequest();
            req.setSettingKey("LEAVE_APPROVE");
            req.setAssignee("admin");
            req.setAssigneeName("管理员");
            req.setDueDate(LocalDate.of(2026, 11, 25));
            generated = todoReminderService.generateFromSetting(req);
            assertEquals("休假审批提醒", generated.getTitle());
            assertEquals("SETTING", generated.getSource());
            assertEquals("LEAVE_APPROVE", generated.getSettingKey());
            assertEquals(Integer.valueOf(0), generated.getStatus());
            // 不存在的设置拒绝
            GenerateTodoRequest noSetting = new GenerateTodoRequest();
            noSetting.setSettingKey("NO_SUCH_SETTING");
            noSetting.setAssignee("admin");
            assertThrows(IllegalArgumentException.class,
                    () -> todoReminderService.generateFromSetting(noSetting));
            // 停用的设置拒绝（BACKUP_FAIL 为停用）
            GenerateTodoRequest disabled = new GenerateTodoRequest();
            disabled.setSettingKey("BACKUP_FAIL");
            disabled.setAssignee("admin");
            assertThrows(IllegalArgumentException.class,
                    () -> todoReminderService.generateFromSetting(disabled));
            // 不填责任人拒绝
            GenerateTodoRequest noAssignee = new GenerateTodoRequest();
            noAssignee.setSettingKey("LEAVE_APPROVE");
            assertThrows(IllegalArgumentException.class,
                    () -> todoReminderService.generateFromSetting(noAssignee));
        } finally {
            if (generated != null && generated.getId() != null) {
                todoReminderService.removeById(generated.getId());
            }
        }
    }

    @Test
    void pushStats() {
        Map<String, Object> stats = pushRecordService.stats();
        assertEquals(4L, stats.get("total"));
        assertEquals(1L, stats.get("pending"));
        assertEquals(0L, stats.get("sent"));
        assertEquals(3L, stats.get("failed"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byChannel = (Map<String, Long>) stats.get("byChannel");
        assertEquals(2L, byChannel.get("WECHAT"));
        assertEquals(1L, byChannel.get("APP"));
        assertEquals(1L, byChannel.get("EMAIL"));
    }

    @Test
    void pushSendPlaceholder() {
        // 发送：预留占位明确返回失败、如实记录，不伪造发送成功
        PushRecord record = newPush("占位推送测试", "WECHAT", "ALL", null);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = pushRecordService.send(record);
        Long id = record.getId();
        try {
            assertEquals(Boolean.FALSE, result.get("success"));
            assertTrue(String.valueOf(result.get("message")).contains("未对接"));
            PushRecord saved = pushRecordService.getById(id);
            assertEquals(Integer.valueOf(2), saved.getSendStatus());
            assertNotNull(saved.getSendTime());
            assertTrue(saved.getFailReason().contains("未对接"));
            // 重推：再次失败如实记录
            @SuppressWarnings("unchecked")
            Map<String, Object> retried = pushRecordService.retry(id);
            assertEquals(Boolean.FALSE, retried.get("success"));
            assertEquals(Integer.valueOf(2), pushRecordService.getById(id).getSendStatus());
            // 已发送成功的记录不可重推、不可直接删除（用 mapper 直接改状态模拟）
            PushRecord sent = pushRecordService.getById(id);
            sent.setSendStatus(1);
            pushRecordMapper.updateById(sent);
            assertThrows(IllegalArgumentException.class, () -> pushRecordService.retry(id));
            assertThrows(IllegalArgumentException.class, () -> pushRecordService.delete(id));
        } finally {
            if (id != null) {
                pushRecordMapper.deleteById(id);
            }
        }
    }

    @Test
    void pushValidation() {
        // 标题为空拒绝
        PushRecord noTitle = newPush(null, "WECHAT", "ALL", null);
        assertThrows(IllegalArgumentException.class, () -> pushRecordService.send(noTitle));
        // 渠道非法拒绝
        PushRecord badChannel = newPush("渠道非法", "FAX", "ALL", null);
        assertThrows(IllegalArgumentException.class, () -> pushRecordService.send(badChannel));
        // 按角色推送不填角色编码拒绝
        PushRecord noRole = newPush("角色缺失", "WECHAT", "ROLE", null);
        assertThrows(IllegalArgumentException.class, () -> pushRecordService.send(noRole));
        // 按人员推送不填用户名拒绝
        PushRecord noUser = newPush("人员缺失", "WECHAT", "USER", null);
        assertThrows(IllegalArgumentException.class, () -> pushRecordService.send(noUser));
        // 全部人员时接收对象强制清空（发送后记录仍为发送失败占位）
        PushRecord all = newPush("全部人员推送", "APP", "ALL", "MEDICAL_ADMIN");
        try {
            pushRecordService.send(all);
            assertNull(all.getReceiverValue());
            assertEquals(Integer.valueOf(2), all.getSendStatus());
        } finally {
            if (all.getId() != null) {
                pushRecordMapper.deleteById(all.getId());
            }
        }
    }

    @Test
    void smsStats() {
        Map<String, Object> stats = smsRecordService.stats();
        assertEquals(4L, stats.get("total"));
        assertEquals(1L, stats.get("pending"));
        assertEquals(0L, stats.get("sent"));
        assertEquals(3L, stats.get("failed"));
    }

    @Test
    void smsSendPlaceholder() {
        // 发送：预留占位明确返回失败、如实记录，不伪造发送成功
        SmsRecord record = newSms("13812340009", "占位短信测试");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = smsRecordService.send(record);
        Long id = record.getId();
        try {
            assertEquals(Boolean.FALSE, result.get("success"));
            assertTrue(String.valueOf(result.get("message")).contains("未对接"));
            SmsRecord saved = smsRecordService.getById(id);
            assertEquals(Integer.valueOf(2), saved.getSendStatus());
            assertNotNull(saved.getSendTime());
            assertTrue(saved.getFailReason().contains("未对接"));
            // 重发：再次失败如实记录
            @SuppressWarnings("unchecked")
            Map<String, Object> retried = smsRecordService.retry(id);
            assertEquals(Boolean.FALSE, retried.get("success"));
            assertEquals(Integer.valueOf(2), smsRecordService.getById(id).getSendStatus());
            // 已发送成功的记录不可重发、不可直接删除（用 mapper 直接改状态模拟）
            SmsRecord sent = smsRecordService.getById(id);
            sent.setSendStatus(1);
            smsRecordMapper.updateById(sent);
            assertThrows(IllegalArgumentException.class, () -> smsRecordService.retry(id));
            assertThrows(IllegalArgumentException.class, () -> smsRecordService.delete(id));
        } finally {
            if (id != null) {
                smsRecordMapper.deleteById(id);
            }
        }
    }

    @Test
    void smsValidation() {
        // 手机号为空拒绝
        SmsRecord noPhone = newSms(null, "内容");
        assertThrows(IllegalArgumentException.class, () -> smsRecordService.send(noPhone));
        // 手机号格式非法拒绝
        SmsRecord badPhone = newSms("12345", "内容");
        assertThrows(IllegalArgumentException.class, () -> smsRecordService.send(badPhone));
        SmsRecord badPhone2 = newSms("23812340009", "内容");
        assertThrows(IllegalArgumentException.class, () -> smsRecordService.send(badPhone2));
        // 内容为空拒绝
        SmsRecord noContent = newSms("13812340009", null);
        assertThrows(IllegalArgumentException.class, () -> smsRecordService.send(noContent));
    }

    @Test
    void overviewStats() {
        Map<String, Object> data = overviewController.overview().getData();
        assertEquals(6L, data.get("messages"));
        assertEquals(4L, data.get("unreadMessages"));
        assertEquals(6L, data.get("todos"));
        assertEquals(4L, data.get("pendingTodos"));
        assertEquals(1L, data.get("overdueTodos"));
        assertEquals(4L, data.get("pushRecords"));
        assertEquals(3L, data.get("failedPush"));
        assertEquals(4L, data.get("smsRecords"));
        assertEquals(3L, data.get("failedSms"));
        assertEquals(6L, data.get("noticeSettings"));
        assertEquals(5L, data.get("enabledNoticeSettings"));
    }

    @Test
    void notificationEndpoints() throws Exception {
        // 站内消息分页与统计
        mockMvc.perform(get("/api/site-messages").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(6));
        mockMvc.perform(get("/api/site-messages/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unread").value(4));
        mockMvc.perform(get("/api/site-messages/unread-count").param("username", "admin").param("roleCode", "MEDICAL_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unread").value(2));
        // 标已读接口：种子消息 6 未读，标已读后恢复
        mockMvc.perform(post("/api/site-messages/6/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.readStatus").value(1));
        SiteMessage m6 = siteMessageService.getById(6L);
        m6.setReadStatus(0);
        m6.setReadTime(null);
        siteMessageService.updateById(m6);
        // 待办提醒分页与统计
        mockMvc.perform(get("/api/todo-reminders").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(4));
        mockMvc.perform(get("/api/todo-reminders/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overdue").value(1));
        // 推送与短信统计
        mockMvc.perform(get("/api/push-records/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failed").value(3));
        mockMvc.perform(get("/api/sms-records/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failed").value(3));
        // 总览
        mockMvc.perform(get("/api/notifications/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages").value(6))
                .andExpect(jsonPath("$.data.pendingTodos").value(4));
        // 推送发送接口：占位失败，HTTP 200、success=false
        mockMvc.perform(post("/api/push-records").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"接口推送测试\",\"channel\":\"WECHAT\",\"receiverScope\":\"ALL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(false));
        // 短信发送接口：手机号非法返回 400
        mockMvc.perform(post("/api/sms-records").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123\",\"content\":\"测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        // 清理接口测试产生的推送记录
        PushRecord created = pushRecordService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PushRecord>()
                .eq(PushRecord::getTitle, "接口推送测试"));
        assertNotNull(created);
        pushRecordMapper.deleteById(created.getId());
    }

    // ---------- 构造 helper ----------

    private SiteMessage newMessage(String title, String category, String scope, String value) {
        SiteMessage m = new SiteMessage();
        m.setTitle(title);
        m.setContent("测试内容");
        m.setCategory(category);
        m.setSenderName("医务科");
        m.setReceiverScope(scope);
        m.setReceiverValue(value);
        return m;
    }

    private TodoReminder newTodo(String title, String type, String source) {
        TodoReminder t = new TodoReminder();
        t.setTitle(title);
        t.setContent("测试内容");
        t.setTodoType(type);
        t.setSource(source);
        t.setAssignee("admin");
        t.setAssigneeName("管理员");
        return t;
    }

    private PushRecord newPush(String title, String channel, String scope, String value) {
        PushRecord p = new PushRecord();
        p.setTitle(title);
        p.setContent("测试内容");
        p.setChannel(channel);
        p.setReceiverScope(scope);
        p.setReceiverValue(value);
        return p;
    }

    private SmsRecord newSms(String phone, String content) {
        SmsRecord s = new SmsRecord();
        s.setPhone(phone);
        s.setContent(content);
        s.setReceiverName("测试接收人");
        return s;
    }
}
