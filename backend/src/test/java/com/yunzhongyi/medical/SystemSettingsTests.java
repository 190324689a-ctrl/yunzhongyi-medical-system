package com.yunzhongyi.medical;

import com.yunzhongyi.medical.sysconfig.controller.SystemSettingsOverviewController;
import com.yunzhongyi.medical.sysconfig.dto.KnowledgeItemQuery;
import com.yunzhongyi.medical.sysconfig.dto.SysParamQuery;
import com.yunzhongyi.medical.sysconfig.entity.KnowledgeItem;
import com.yunzhongyi.medical.sysconfig.entity.SysBackup;
import com.yunzhongyi.medical.sysconfig.entity.SysInterface;
import com.yunzhongyi.medical.sysconfig.entity.SysNoticeSetting;
import com.yunzhongyi.medical.sysconfig.entity.SysParam;
import com.yunzhongyi.medical.sysconfig.excel.KnowledgeItemExcelUtil;
import com.yunzhongyi.medical.sysconfig.excel.SysParamExcelUtil;
import com.yunzhongyi.medical.sysconfig.mapper.SysBackupMapper;
import com.yunzhongyi.medical.sysconfig.service.KnowledgeItemService;
import com.yunzhongyi.medical.sysconfig.service.SysBackupService;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
import com.yunzhongyi.medical.sysconfig.service.SysInterfaceService;
import com.yunzhongyi.medical.sysconfig.service.SysNoticeSettingService;
import com.yunzhongyi.medical.sysconfig.service.SysParamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 医务系统设置模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：8 个系统参数（启用 7 / 停用 1；内置 4 / 自定义 4）、3 条数据备份（全部成功，
 * 2026-11-15 全量备份已恢复 1 次，记录条数合计 1526）、5 个系统接口（启用 4 / 停用 1，
 * 均未测试）、6 条消息提醒设置（启用 5 / 停用 1）、8 条知识库（启用 7 / 停用 1）。
 * 底座种子：2 用户、4 角色、33 权限、8 科室、5 字典类型、21 字典数据、0 操作日志。
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class SystemSettingsTests {

    @Autowired
    private SysParamService paramService;

    @Autowired
    private SysBackupService backupService;

    @Autowired
    private SysBackupMapper backupMapper;

    @Autowired
    private SysInterfaceService interfaceService;

    @Autowired
    private SysNoticeSettingService noticeSettingService;

    @Autowired
    private KnowledgeItemService knowledgeItemService;

    @Autowired
    private SystemSettingsOverviewController overviewController;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(paramService);
        assertNotNull(backupService);
        assertNotNull(interfaceService);
        assertNotNull(noticeSettingService);
        assertNotNull(knowledgeItemService);
        assertNotNull(overviewController);
    }

    @Test
    void seedDataLoaded() {
        assertEquals(8L, paramService.count());
        assertEquals(3L, backupService.count());
        assertEquals(5L, interfaceService.count());
        assertEquals(6L, noticeSettingService.count());
        assertEquals(8L, knowledgeItemService.count());
        assertEquals("云中医医务系统", paramService.getById(1L).getParamValue());
        assertEquals("false", paramService.getById(3L).getParamValue());
        assertEquals(Integer.valueOf(0), paramService.getById(3L).getStatus());
        assertEquals("HIS 挂号接口", interfaceService.getById(1L).getInterfaceName());
        assertEquals(Integer.valueOf(0), interfaceService.getById(3L).getStatus());
        assertEquals("KB-001", knowledgeItemService.getById(1L).getKbCode());
        assertEquals(Integer.valueOf(0), knowledgeItemService.getById(8L).getStatus());
    }

    @Test
    void paramStats() {
        Map<String, Object> stats = paramService.stats();
        assertEquals(8L, stats.get("total"));
        assertEquals(7L, stats.get("enabled"));
        assertEquals(1L, stats.get("disabled"));
        assertEquals(4L, stats.get("system"));
        assertEquals(4L, stats.get("custom"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byGroup = (Map<String, Long>) stats.get("byGroup");
        assertEquals(4L, byGroup.get("SYSTEM"));
        assertEquals(2L, byGroup.get("BUSINESS"));
        assertEquals(2L, byGroup.get("NOTIFY"));
    }

    @Test
    void paramValidation() {
        // 参数键重复拒绝
        SysParam dup = newParam("sys.name", "重复参数", "x", "SYSTEM", "STRING");
        assertThrows(IllegalArgumentException.class, () -> paramService.create(dup));
        // 分组非法拒绝
        SysParam badGroup = newParam("test.group", "分组非法", "x", "UNKNOWN", "STRING");
        assertThrows(IllegalArgumentException.class, () -> paramService.create(badGroup));
        // 值类型非法拒绝
        SysParam badType = newParam("test.type", "类型非法", "x", "BUSINESS", "DATE");
        assertThrows(IllegalArgumentException.class, () -> paramService.create(badType));
        // NUMBER 类型的值不是数字拒绝
        SysParam badNumber = newParam("test.number", "数字非法", "abc", "BUSINESS", "NUMBER");
        assertThrows(IllegalArgumentException.class, () -> paramService.create(badNumber));
        // BOOLEAN 类型的值不是 true/false 拒绝
        SysParam badBool = newParam("test.bool", "开关非法", "yes", "BUSINESS", "BOOLEAN");
        assertThrows(IllegalArgumentException.class, () -> paramService.create(badBool));
        // NUMBER 与 BOOLEAN 合法值通过（用后删除）
        SysParam ok = newParam("test.ok", "合法参数", "42", "BUSINESS", "NUMBER");
        try {
            SysParam created = paramService.create(ok);
            assertEquals("42", created.getParamValue());
            assertEquals(Integer.valueOf(1), created.getStatus());
        } finally {
            if (ok.getId() != null) {
                paramService.removeById(ok.getId());
            }
        }
    }

    @Test
    void paramSystemGuard() {
        // 内置参数改键拒绝
        SysParam rename = paramService.getById(1L);
        rename.setParamKey("sys.renamed");
        assertThrows(IllegalArgumentException.class, () -> paramService.update(rename));
        // 内置参数删除拒绝
        assertThrows(IllegalArgumentException.class, () -> paramService.removeById(1L));
        // 自定义参数可正常删除
        SysParam custom = newParam("test.custom", "自定义参数", "v", "BUSINESS", "STRING");
        SysParam created = paramService.create(custom);
        assertTrue(paramService.removeById(created.getId()));
        assertNull(paramService.getById(created.getId()));
    }

    @Test
    void paramEnableDisableAndByKey() {
        // sys.version 停用后按参数键读取拒绝，重新启用后可读
        paramService.disable(2L);
        try {
            assertThrows(IllegalArgumentException.class, () -> paramService.getByKey("sys.version"));
        } finally {
            paramService.enable(2L);
        }
        assertEquals("V1.0", paramService.getByKey("sys.version").getParamValue());
        // 不存在的参数键拒绝
        assertThrows(IllegalArgumentException.class, () -> paramService.getByKey("no.such.key"));
    }

    @Test
    void backupStats() {
        Map<String, Object> stats = backupService.stats();
        assertEquals(3L, stats.get("total"));
        assertEquals(3L, stats.get("success"));
        assertEquals(0L, stats.get("failed"));
        assertEquals(0L, stats.get("running"));
        assertEquals(1L, stats.get("restored"));
        assertEquals(1L, stats.get("totalRestoreCount"));
        assertEquals(1526L, stats.get("totalRecords"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) stats.get("byType");
        assertEquals(1L, byType.get("FULL"));
        assertEquals(1L, byType.get("INCREMENTAL"));
        assertEquals(1L, byType.get("CONFIG"));
    }

    @Test
    void backupCreateAndRestore() {
        SysBackup backup = new SysBackup();
        backup.setBackupName("测试全量备份");
        backup.setBackupType("FULL");
        backup.setBackupScope("ALL");
        backup.setCreatedBy("admin");
        SysBackup created = null;
        SysBackup fresh = null;
        SysBackup failed = null;
        try {
            created = backupService.create(backup);
            // 执行备份后状态成功、记录条数为全库行数之和、文件名按规则生成、文件大小按记录数估算
            assertEquals(Integer.valueOf(1), created.getStatus());
            assertTrue(created.getRecordCount() > 0);
            assertTrue(created.getFileName().startsWith("medical-backup-"));
            assertTrue(created.getFileName().endsWith(".zip"));
            assertEquals(created.getRecordCount() * 1024L, created.getFileSize());
            // 恢复：恢复次数 +1、恢复时间有值
            SysBackup restored = backupService.restore(created.getId());
            assertEquals(Integer.valueOf(1), restored.getRestoreCount());
            assertNotNull(restored.getLastRestoreTime());
            // 已恢复过的备份不可直接删除
            Long restoredId = created.getId();
            assertThrows(IllegalArgumentException.class, () -> backupService.removeById(restoredId));
            // 未恢复过的备份可直接删除
            fresh = backupService.create(newBackup("测试增量备份", "INCREMENTAL"));
            assertTrue(backupService.removeById(fresh.getId()));
            fresh = null;
            // 失败的备份不可恢复
            failed = backupService.create(newBackup("测试失败备份", "CONFIG"));
            failed.setStatus(SysConfigTypeUtil.BACKUP_FAILED);
            backupMapper.updateById(failed);
            Long failedId = failed.getId();
            assertThrows(IllegalArgumentException.class, () -> backupService.restore(failedId));
        } finally {
            if (failed != null && failed.getId() != null) {
                backupService.removeById(failed.getId());
            }
            if (created != null && created.getId() != null) {
                // 已恢复过的备份删不掉是守卫本身；用 mapper 物理删除清理测试数据
                backupMapper.deleteById(created.getId());
            }
        }
    }

    @Test
    void backupValidation() {
        SysBackup noName = new SysBackup();
        noName.setBackupType("FULL");
        assertThrows(IllegalArgumentException.class, () -> backupService.create(noName));
        SysBackup badType = newBackup("类型非法备份", "DIFF");
        assertThrows(IllegalArgumentException.class, () -> backupService.create(badType));
    }

    @Test
    void interfaceValidation() {
        // 编码重复拒绝
        SysInterface dup = newInterface("HIS-001", "重复接口", "HIS", "http://a.local/api");
        assertThrows(IllegalArgumentException.class, () -> interfaceService.create(dup));
        // 地址不是 http/https 开头拒绝
        SysInterface badUrl = newInterface("TEST-URL", "地址非法", "HIS", "ftp://a.local/api");
        assertThrows(IllegalArgumentException.class, () -> interfaceService.create(badUrl));
        // 接口类型非法拒绝
        SysInterface badType = newInterface("TEST-TYPE", "类型非法", "RIS", "http://a.local/api");
        assertThrows(IllegalArgumentException.class, () -> interfaceService.create(badType));
        // 超时时间超范围拒绝
        SysInterface badTimeout = newInterface("TEST-TIME", "超时非法", "HIS", "http://a.local/api");
        badTimeout.setTimeoutSeconds(0);
        assertThrows(IllegalArgumentException.class, () -> interfaceService.create(badTimeout));
        badTimeout.setTimeoutSeconds(61);
        assertThrows(IllegalArgumentException.class, () -> interfaceService.create(badTimeout));
    }

    @Test
    void interfaceTestConnection() {
        // 指向本机未监听端口的接口：测试真实发起请求，失败结果如实记录，不伪造成功
        SysInterface iface = newInterface("TEST-UNREACH", "不可达接口", "OTHER", "http://127.0.0.1:9/health");
        iface.setTimeoutSeconds(2);
        SysInterface created = interfaceService.create(iface);
        try {
            Map<String, Object> result = interfaceService.testConnection(created.getId());
            assertEquals(Boolean.FALSE, result.get("success"));
            // 连接被拒时 getResponseCode() 可能抛异常（message 含“连接失败”）也可能返回 -1，
            // 两种情况都表示测试失败、未伪造成功
            Object httpStatus = result.get("httpStatus");
            assertTrue(httpStatus == null || Integer.valueOf(-1).equals(httpStatus),
                    "httpStatus 应为空或 -1，实际为 " + httpStatus);
            SysInterface after = interfaceService.getById(created.getId());
            assertEquals("FAIL", after.getLastTestResult());
            assertNotNull(after.getLastTestTime());
            // 停用的接口不能测试
            interfaceService.disable(created.getId());
            Long id = created.getId();
            assertThrows(IllegalArgumentException.class, () -> interfaceService.testConnection(id));
        } finally {
            interfaceService.removeById(created.getId());
        }
    }

    @Test
    void interfaceStats() {
        Map<String, Object> stats = interfaceService.stats();
        assertEquals(5L, stats.get("total"));
        assertEquals(4L, stats.get("enabled"));
        assertEquals(1L, stats.get("disabled"));
        assertEquals(0L, stats.get("testSuccess"));
        assertEquals(0L, stats.get("testFail"));
        assertEquals(5L, stats.get("untested"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) stats.get("byType");
        assertEquals(1L, byType.get("HIS"));
        assertEquals(1L, byType.get("LIS"));
        assertEquals(1L, byType.get("PACS"));
        assertEquals(1L, byType.get("INSURANCE"));
        assertEquals(1L, byType.get("OTHER"));
    }

    @Test
    void noticeValidation() {
        // 编码重复拒绝
        SysNoticeSetting dup = newNotice("LEAVE_APPROVE", "重复设置", "SITE", "ALL", null);
        assertThrows(IllegalArgumentException.class, () -> noticeSettingService.create(dup));
        // 渠道非法拒绝
        SysNoticeSetting badChannel = newNotice("TEST-CH", "渠道非法", "FAX", "ALL", null);
        assertThrows(IllegalArgumentException.class, () -> noticeSettingService.create(badChannel));
        // 按角色接收不填角色编码拒绝
        SysNoticeSetting noRole = newNotice("TEST-ROLE", "角色缺失", "SITE", "ROLE", null);
        assertThrows(IllegalArgumentException.class, () -> noticeSettingService.create(noRole));
        // 按人员接收不填用户名拒绝
        SysNoticeSetting noUser = newNotice("TEST-USER", "人员缺失", "SITE", "USER", null);
        assertThrows(IllegalArgumentException.class, () -> noticeSettingService.create(noUser));
        // 全部人员时接收对象强制清空
        SysNoticeSetting all = newNotice("TEST-ALL", "全部人员", "SITE", "ALL", "MEDICAL_ADMIN");
        try {
            SysNoticeSetting created = noticeSettingService.create(all);
            assertNull(created.getReceiverValue());
        } finally {
            if (all.getId() != null) {
                noticeSettingService.removeById(all.getId());
            }
        }
    }

    @Test
    void noticeStats() {
        Map<String, Object> stats = noticeSettingService.stats();
        assertEquals(6L, stats.get("total"));
        assertEquals(5L, stats.get("enabled"));
        assertEquals(1L, stats.get("disabled"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byChannel = (Map<String, Long>) stats.get("byChannel");
        assertEquals(3L, byChannel.get("SITE"));
        assertEquals(1L, byChannel.get("SMS"));
        assertEquals(1L, byChannel.get("WECHAT"));
        assertEquals(1L, byChannel.get("EMAIL"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byReceiver = (Map<String, Long>) stats.get("byReceiverScope");
        assertEquals(2L, byReceiver.get("ALL"));
        assertEquals(3L, byReceiver.get("ROLE"));
        assertEquals(1L, byReceiver.get("USER"));
    }

    @Test
    void knowledgeSearchAndViews() {
        // 关键字检索：标题命中
        KnowledgeItemQuery q = new KnowledgeItemQuery();
        q.setKeyword("三级医师查房");
        assertEquals(1L, knowledgeItemService.pageQuery(q).getTotal());
        assertEquals("KB-001", knowledgeItemService.pageQuery(q).getRecords().get(0).getKbCode());
        // 按分类筛选：制度规范 2 条
        KnowledgeItemQuery q2 = new KnowledgeItemQuery();
        q2.setCategory("制度规范");
        assertEquals(2L, knowledgeItemService.pageQuery(q2).getTotal());
        // 停用条目仍在列表里，但 enabled() 不含它
        assertEquals(7, knowledgeItemService.enabled().size());
        // 查阅量：新建条目打开两次 +2，编辑后查阅量不变
        KnowledgeItem item = newKnowledge("KB-TEST", "测试知识", "常见问题", "测试正文", "测试");
        KnowledgeItem created = knowledgeItemService.create(item);
        try {
            assertEquals(Long.valueOf(1), knowledgeItemService.detail(created.getId()).getViewCount());
            assertEquals(Long.valueOf(2), knowledgeItemService.detail(created.getId()).getViewCount());
            created.setTitle("测试知识（已改）");
            KnowledgeItem updated = knowledgeItemService.update(created);
            assertEquals(Long.valueOf(2), updated.getViewCount());
        } finally {
            knowledgeItemService.removeById(created.getId());
        }
    }

    @Test
    void knowledgeValidation() {
        KnowledgeItem dup = newKnowledge("KB-001", "重复知识", "常见问题", "正文", "关键词");
        assertThrows(IllegalArgumentException.class, () -> knowledgeItemService.create(dup));
        KnowledgeItem badCat = newKnowledge("KB-BAD", "分类非法", "未知分类", "正文", "关键词");
        assertThrows(IllegalArgumentException.class, () -> knowledgeItemService.create(badCat));
    }

    @Test
    void knowledgeStats() {
        Map<String, Object> stats = knowledgeItemService.stats();
        assertEquals(8L, stats.get("total"));
        assertEquals(7L, stats.get("enabled"));
        assertEquals(1L, stats.get("disabled"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byCategory = (Map<String, Long>) stats.get("byCategory");
        assertEquals(2L, byCategory.get("制度规范"));
        assertEquals(2L, byCategory.get("操作指南"));
        assertEquals(1L, byCategory.get("常见问题"));
        assertEquals(1L, byCategory.get("法律法规"));
        assertEquals(1L, byCategory.get("培训资料"));
        assertEquals(1L, byCategory.get("其他"));
    }

    @Test
    void paramImportExportRoundTrip() throws Exception {
        // 新建一条临时参数，导出后删除再导入补回 1 条，重复导入 0 条
        SysParam temp = paramService.create(newParam("test.roundtrip", "往返测试参数", "1", "BUSINESS", "NUMBER"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        paramService.exportExcel(new SysParamQuery(), out);
        List<SysParam> rows = SysParamExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(9, rows.size());
        assertTrue(paramService.removeById(temp.getId()));
        Map<String, Object> first = paramService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, first.get("created"));
        assertEquals(8, first.get("skipped"));
        Map<String, Object> second = paramService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, second.get("created"));
        assertEquals(9, second.get("skipped"));
        SysParam restored = paramService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysParam>()
                .eq(SysParam::getParamKey, "test.roundtrip"));
        assertNotNull(restored);
        paramService.removeById(restored.getId());
    }

    @Test
    void knowledgeImportExportRoundTrip() throws Exception {
        // 新建一条临时知识，导出后删除再导入补回 1 条，重复导入 0 条；导入的知识查阅量从 0 计
        KnowledgeItem temp = knowledgeItemService.create(
                newKnowledge("KB-ROUNDTRIP", "往返测试知识", "常见问题", "往返测试正文", "往返"));
        knowledgeItemService.detail(temp.getId());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        knowledgeItemService.exportExcel(new KnowledgeItemQuery(), out);
        List<KnowledgeItem> rows = KnowledgeItemExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(9, rows.size());
        assertTrue(knowledgeItemService.removeById(temp.getId()));
        Map<String, Object> first = knowledgeItemService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, first.get("created"));
        assertEquals(8, first.get("skipped"));
        Map<String, Object> second = knowledgeItemService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, second.get("created"));
        assertEquals(9, second.get("skipped"));
        KnowledgeItem restored = knowledgeItemService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeItem>()
                .eq(KnowledgeItem::getKbCode, "KB-ROUNDTRIP"));
        assertNotNull(restored);
        assertEquals(Long.valueOf(0), restored.getViewCount());
        knowledgeItemService.removeById(restored.getId());
    }

    @Test
    void baseUserTests() throws Exception {
        // 登录账号重复拒绝
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"realName\":\"重复用户\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        // 重置密码成功
        mockMvc.perform(post("/api/users/2/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        // 用户统计：总数 4（admin、chen、wuwenbin、sunxiaohong），全部启用
        mockMvc.perform(get("/api/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.enabled").value(4));
    }

    @Test
    void baseRoleAssignTests() throws Exception {
        // 角色编码重复拒绝
        mockMvc.perform(post("/api/roles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ADMIN\",\"name\":\"重复角色\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        // 给医师角色分配 2 个权限，再查回 2 个；不存在的权限拒绝
        mockMvc.perform(post("/api/roles/3/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.assigned").value(2));
        mockMvc.perform(get("/api/roles/3/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(post("/api/roles/3/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[9999]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        // 清理分配：恢复种子状态（医师角色的 6 个页面权限），避免影响共用同一 H2 的其他测试类
        mockMvc.perform(post("/api/roles/3/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[1,2,5,12,31,32]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/roles/3/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));
    }

    @Test
    void baseDeptPermissionDictTests() throws Exception {
        // 科室编码重复拒绝
        mockMvc.perform(post("/api/depts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"重复科室\",\"code\":\"ZY-NK\",\"type\":\"clinical\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        // 权限标识重复拒绝
        mockMvc.perform(post("/api/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"重复权限\",\"code\":\"physician:list\",\"type\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        // 同一字典下数据值重复拒绝
        mockMvc.perform(post("/api/dicts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":\"title\",\"dictValue\":\"主任医师\",\"label\":\"主任医师\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        // 操作日志统计：种子里没有日志，总数 0
        mockMvc.perform(get("/api/oper-logs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
        // 科室统计：总数 11（含 3 个下级科室），全部启用
        mockMvc.perform(get("/api/depts/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.enabled").value(11));
    }

    @Test
    void overviewStats() {
        Map<String, Object> data = overviewController.overview().getData();
        assertEquals(4L, data.get("users"));
        assertEquals(4L, data.get("roles"));
        assertEquals(33L, data.get("permissions"));
        assertEquals(11L, data.get("depts"));
        assertEquals(5L, data.get("dictTypes"));
        assertEquals(21L, data.get("dictData"));
        assertEquals(0L, data.get("operLogs"));
        assertEquals(8L, data.get("params"));
        assertEquals(3L, data.get("backups"));
        assertEquals(5L, data.get("interfaces"));
        assertEquals(6L, data.get("noticeSettings"));
        assertEquals(8L, data.get("knowledge"));
    }

    // ---------- 构造 helper ----------

    private SysParam newParam(String key, String name, String value, String group, String valueType) {
        SysParam p = new SysParam();
        p.setParamKey(key);
        p.setParamName(name);
        p.setParamValue(value);
        p.setParamGroup(group);
        p.setValueType(valueType);
        return p;
    }

    private SysBackup newBackup(String name, String type) {
        SysBackup b = new SysBackup();
        b.setBackupName(name);
        b.setBackupType(type);
        b.setBackupScope("ALL");
        b.setCreatedBy("admin");
        return b;
    }

    private SysInterface newInterface(String code, String name, String type, String url) {
        SysInterface s = new SysInterface();
        s.setInterfaceCode(code);
        s.setInterfaceName(name);
        s.setType(type);
        s.setUrl(url);
        s.setAuthType("NONE");
        return s;
    }

    private SysNoticeSetting newNotice(String key, String name, String channel, String scope, String value) {
        SysNoticeSetting s = new SysNoticeSetting();
        s.setSettingKey(key);
        s.setSettingName(name);
        s.setChannel(channel);
        s.setTriggerEvent("测试事件");
        s.setTemplateContent("测试模板");
        s.setReceiverScope(scope);
        s.setReceiverValue(value);
        return s;
    }

    private KnowledgeItem newKnowledge(String code, String title, String category, String content, String keywords) {
        KnowledgeItem k = new KnowledgeItem();
        k.setKbCode(code);
        k.setTitle(title);
        k.setCategory(category);
        k.setContent(content);
        k.setKeywords(keywords);
        return k;
    }
}
