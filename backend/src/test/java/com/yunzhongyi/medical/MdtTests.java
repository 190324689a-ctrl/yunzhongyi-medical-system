package com.yunzhongyi.medical;

import com.yunzhongyi.medical.mdt.dto.MdtQuery;
import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import com.yunzhongyi.medical.mdt.entity.MdtRecord;
import com.yunzhongyi.medical.mdt.excel.MdtExcelUtil;
import com.yunzhongyi.medical.mdt.service.MdtService;
import com.yunzhongyi.medical.mdt.service.MdtTypeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MDT 多学科会诊管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：5 条 MDT 申请，覆盖院内/院外/远程/急会诊 4 种会诊类型、
 * 待审批/已通过/已驳回 3 种审批状态，含 2 条紧急申请；3 条会诊记录，
 * 其中张建国的申请有 2 条补充记录。
 */
@SpringBootTest
@ActiveProfiles("h2")
class MdtTests {

    @Autowired
    private MdtService mdtService;

    @Test
    void contextLoads() {
        assertNotNull(mdtService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(mdtService.count() >= 5);

        // 种子申请 1：张建国，中医内科，院内会诊，已通过
        MdtApplication zhang = mdtService.getById(1L);
        assertEquals("张建国", zhang.getPatientName());
        assertEquals("ZY202611001", zhang.getInpatientNo());
        assertEquals("IN_HOSPITAL", zhang.getMdtType());
        assertEquals("中医内科", zhang.getDeptName());
        assertEquals("陈国强", zhang.getApplyPhysicianName());
        assertEquals("NORMAL", zhang.getUrgency());
        assertEquals(1, zhang.getStatus());
        assertEquals("吴文斌", zhang.getApprover());
        assertTrue(zhang.getApprovalHistory().contains("医务科"));
        assertTrue(zhang.getApprovalHistory().contains("通过"));
        assertEquals(LocalDateTime.of(2026, 11, 24, 15, 0), zhang.getExpectedTime());

        // 种子申请 3：刘大海急会诊，已通过，紧急
        MdtApplication liu = mdtService.getById(3L);
        assertEquals("EMERGENCY", liu.getMdtType());
        assertEquals("URGENT", liu.getUrgency());
        assertEquals(1, liu.getStatus());

        // 种子申请 4：陈小雨院外会诊，已驳回
        MdtApplication chen = mdtService.getById(4L);
        assertEquals("OUT_HOSPITAL", chen.getMdtType());
        assertEquals(2, chen.getStatus());
        assertTrue(chen.getApprovalHistory().contains("驳回"));

        // 种子会诊记录：张建国有 2 条补充记录，刘大海 1 条
        List<MdtRecord> zhangRecords = mdtService.listRecords(1L);
        assertEquals(2, zhangRecords.size());
        assertEquals("陈国强、王建军、周建国", zhangRecords.get(0).getAttendees());
        assertTrue(zhangRecords.get(0).getOpinion().contains("慢性阻塞性肺疾病"));
        assertEquals("医务科会议室", zhangRecords.get(0).getMeetingPlace());
        assertTrue(zhangRecords.get(1).getOpinion().contains("复查胸部 CT"));
        assertEquals(1, mdtService.listRecords(3L).size());
        assertTrue(mdtService.listRecords(3L).get(0).getOpinion().contains("肺栓塞"));
        // 待审批的申请暂无会诊记录
        assertEquals(0, mdtService.listRecords(2L).size());
    }

    @Test
    void pageQueryWithFilters() {
        MdtQuery q = new MdtQuery();
        q.setMdtType("IN_HOSPITAL");
        assertEquals(2, mdtService.pageQuery(q).getTotal()); // 张建国、王丽华

        MdtQuery q2 = new MdtQuery();
        q2.setMdtType("EMERGENCY");
        assertEquals(1, mdtService.pageQuery(q2).getTotal()); // 刘大海

        MdtQuery q3 = new MdtQuery();
        q3.setStatus(0);
        assertEquals(2, mdtService.pageQuery(q3).getTotal()); // 待审批：李秀英、王丽华

        MdtQuery q4 = new MdtQuery();
        q4.setStatus(1);
        assertEquals(2, mdtService.pageQuery(q4).getTotal()); // 已通过：张建国、刘大海

        MdtQuery q5 = new MdtQuery();
        q5.setUrgency("URGENT");
        assertEquals(2, mdtService.pageQuery(q5).getTotal()); // 紧急：李秀英、刘大海

        MdtQuery q6 = new MdtQuery();
        q6.setDeptId(1L);
        assertEquals(1, mdtService.pageQuery(q6).getTotal()); // 中医内科：张建国

        MdtQuery q7 = new MdtQuery();
        q7.setStartDate(LocalDate.of(2026, 11, 23));
        q7.setEndDate(LocalDate.of(2026, 11, 24));
        assertEquals(2, mdtService.pageQuery(q7).getTotal()); // 刘大海 11-23、张建国 11-24

        MdtQuery q8 = new MdtQuery();
        q8.setKeyword("张建国");
        assertEquals(1, mdtService.pageQuery(q8).getTotal());

        MdtQuery q9 = new MdtQuery();
        q9.setKeyword("ZY202611002");
        assertEquals(1, mdtService.pageQuery(q9).getTotal()); // 住院号也能查

        MdtQuery q10 = new MdtQuery();
        q10.setKeyword("林晓梅");
        assertEquals(1, mdtService.pageQuery(q10).getTotal()); // 申请医师姓名也能查
    }

    @Test
    void saveValidationAndDefaults() {
        // 会诊类型非法，拒绝保存
        MdtApplication badType = newApplication("测试患者", "IN_HOSPITAL");
        badType.setMdtType("XXX");
        assertThrows(IllegalArgumentException.class, () -> mdtService.save(badType));

        // 紧急程度非法，拒绝保存
        MdtApplication badUrgency = newApplication("测试患者", "IN_HOSPITAL");
        badUrgency.setUrgency("XXX");
        assertThrows(IllegalArgumentException.class, () -> mdtService.save(badUrgency));

        // 期望会诊时间为空，拒绝保存
        MdtApplication noTime = newApplication("测试患者", "IN_HOSPITAL");
        noTime.setExpectedTime(null);
        assertThrows(IllegalArgumentException.class, () -> mdtService.save(noTime));

        // 合法申请：默认待审批、申请日期为当天
        MdtApplication app = newApplication("测试患者", "REMOTE");
        assertTrue(mdtService.save(app));
        assertNotNull(app.getId());
        assertEquals(MdtTypeUtil.STATUS_PENDING, app.getStatus());
        assertEquals(LocalDate.now(), app.getApplyDate());
        mdtService.removeById(app.getId());
    }

    @Test
    void approvalFlow() {
        MdtApplication app = newApplication("审批测试患者", "IN_HOSPITAL");
        assertTrue(mdtService.save(app));
        try {
            // 医务科通过：状态变为已通过，审批记录留痕
            MdtApplication approved = mdtService.approve(app.getId(), "吴文斌", "同意");
            assertEquals(1, approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertEquals("同意", approved.getApprovalOpinion());
            assertTrue(approved.getApprovalHistory().contains("医务科"));
            assertTrue(approved.getApprovalHistory().contains("通过"));

            // 已结束的申请不能再通过，也不能再驳回
            assertThrows(IllegalStateException.class,
                    () -> mdtService.approve(app.getId(), "吴文斌", "再次通过"));
            assertThrows(IllegalStateException.class,
                    () -> mdtService.reject(app.getId(), "吴文斌", "改主意了"));

            // 已结束的申请不能再修改
            approved.setConditionSummary("改病情摘要");
            assertThrows(IllegalStateException.class, () -> mdtService.updateById(approved));
        } finally {
            mdtService.removeById(app.getId());
        }

        // 驳回流程：状态变为已驳回，审批记录留痕；驳回后不能再通过
        MdtApplication app2 = newApplication("驳回测试患者", "REMOTE");
        assertTrue(mdtService.save(app2));
        try {
            MdtApplication rejected = mdtService.reject(app2.getId(), "吴文斌", "资料不全，请补充后重报");
            assertEquals(2, rejected.getStatus());
            assertTrue(rejected.getApprovalHistory().contains("驳回"));
            assertThrows(IllegalStateException.class,
                    () -> mdtService.approve(app2.getId(), "吴文斌", "改主意了"));
        } finally {
            mdtService.removeById(app2.getId());
        }
    }

    @Test
    void updatePendingOnly() {
        // 待审批的申请可以修改
        MdtApplication app = newApplication("修改测试患者", "IN_HOSPITAL");
        assertTrue(mdtService.save(app));
        try {
            app.setConditionSummary("更新后的病情摘要");
            assertTrue(mdtService.updateById(app));
            assertEquals("更新后的病情摘要", mdtService.getById(app.getId()).getConditionSummary());
        } finally {
            mdtService.removeById(app.getId());
        }
    }

    @Test
    void recordFlow() {
        // 待审批的申请不能添加会诊记录
        MdtApplication pending = newApplication("记录测试患者", "IN_HOSPITAL");
        assertTrue(mdtService.save(pending));
        try {
            MdtRecord r = newRecord("陈国强、王建军", LocalDateTime.of(2026, 12, 1, 15, 0), "考虑普通感染");
            assertThrows(IllegalStateException.class, () -> mdtService.addRecord(pending.getId(), r));

            // 审批通过后才能记录，且可多次补充
            mdtService.approve(pending.getId(), "吴文斌", "同意");
            MdtRecord first = mdtService.addRecord(pending.getId(), r);
            assertNotNull(first.getId());
            assertEquals(LocalDate.now(), first.getRecordDate());
            MdtRecord second = newRecord("陈国强", LocalDateTime.of(2026, 12, 2, 10, 0), "复查好转");
            second.setSuggestion("继续观察");
            mdtService.addRecord(pending.getId(), second);
            assertEquals(2, mdtService.listRecords(pending.getId()).size());
            // 记录按会诊时间升序
            assertEquals(LocalDateTime.of(2026, 12, 1, 15, 0),
                    mdtService.listRecords(pending.getId()).get(0).getMeetingTime());

            // 删除一条记录
            assertTrue(mdtService.removeRecord(pending.getId(), second.getId()));
            assertEquals(1, mdtService.listRecords(pending.getId()).size());
            // 删不存在的记录，拒绝
            assertThrows(IllegalArgumentException.class,
                    () -> mdtService.removeRecord(pending.getId(), 999999L));

            // 会诊意见为空，拒绝记录
            MdtRecord bad = newRecord("陈国强", LocalDateTime.of(2026, 12, 3, 10, 0), "");
            assertThrows(IllegalArgumentException.class, () -> mdtService.addRecord(pending.getId(), bad));
        } finally {
            mdtService.removeById(pending.getId());
        }

        // 已驳回的申请也不能添加会诊记录
        MdtApplication rejected = newApplication("驳回记录测试患者", "REMOTE");
        assertTrue(mdtService.save(rejected));
        try {
            mdtService.reject(rejected.getId(), "吴文斌", "驳回");
            MdtRecord r = newRecord("陈国强", LocalDateTime.of(2026, 12, 1, 15, 0), "意见");
            assertThrows(IllegalStateException.class, () -> mdtService.addRecord(rejected.getId(), r));
        } finally {
            mdtService.removeById(rejected.getId());
        }
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = mdtService.stats();
        assertEquals(5L, s.get("total"));
        assertEquals(2L, s.get("pending")); // 待审批：李秀英、王丽华
        assertEquals(2L, s.get("approved")); // 已通过：张建国、刘大海
        assertEquals(1L, s.get("rejected")); // 已驳回：陈小雨
        assertEquals(2L, s.get("urgentCount")); // 紧急：李秀英、刘大海
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(2L, byType.get("IN_HOSPITAL"));
        assertEquals(1L, byType.get("OUT_HOSPITAL"));
        assertEquals(1L, byType.get("REMOTE"));
        assertEquals(1L, byType.get("EMERGENCY"));

        // 按科室汇总：5 个科室各 1 条
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        assertEquals(5, byDept.size());
        Map<String, Object> nk = byDept.stream()
                .filter(d -> "中医内科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(1L, nk.get("count"));
        assertEquals(1L, nk.get("approvedCount"));

        // 按月趋势：种子申请都在 2026-11，5 条、通过 2 条、待审批 2 条
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(1, byMonth.size());
        assertEquals("2026-11", byMonth.get(0).get("month"));
        assertEquals(5L, byMonth.get(0).get("count"));
        assertEquals(2L, byMonth.get(0).get("approvedCount"));
        assertEquals(2L, byMonth.get(0).get("pendingCount"));
    }

    @Test
    void detailContainsRecords() {
        Map<String, Object> detail = mdtService.detail(1L);
        assertNotNull(detail.get("application"));
        @SuppressWarnings("unchecked")
        List<MdtRecord> records = (List<MdtRecord>) detail.get("records");
        assertEquals(2, records.size());
        // 不存在的申请，详情抛错
        assertThrows(IllegalArgumentException.class, () -> mdtService.detail(999999L));
    }

    @Test
    void excelRoundTrip() {
        MdtQuery q = new MdtQuery();
        q.setPage(1);
        q.setSize(100);
        List<MdtApplication> all = mdtService.listByQuery(q);
        assertTrue(all.size() >= 5);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MdtExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<MdtApplication> back = MdtExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（患者、住院号、类型、科室、医师、期望时间、紧急程度、状态）
        MdtApplication first = all.get(0);
        MdtApplication firstBack = back.stream()
                .filter(a -> first.getPatientName().equals(a.getPatientName()))
                .findFirst().orElseThrow();
        assertEquals(first.getInpatientNo(), firstBack.getInpatientNo());
        assertEquals(first.getMdtType(), firstBack.getMdtType());
        assertEquals(first.getDeptName(), firstBack.getDeptName());
        assertEquals(first.getApplyPhysicianName(), firstBack.getApplyPhysicianName());
        assertEquals(first.getExpectedTime(), firstBack.getExpectedTime());
        assertEquals(first.getUrgency(), firstBack.getUrgency());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条申请 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （项目用逻辑删除，删后重导按“同一患者同一住院号同一期望时间重复”规则会跳过种子行，只补回被删的那条）
        MdtApplication fresh = newApplication("导入测试患者", "IN_HOSPITAL");
        fresh.setInpatientNo("ZY202612001");
        fresh.setExpectedTime(LocalDateTime.of(2026, 12, 8, 14, 0));
        assertTrue(mdtService.save(fresh));

        MdtQuery q = new MdtQuery();
        q.setPage(1);
        q.setSize(100);
        List<MdtApplication> all = mdtService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MdtExcelUtil.write(all, out);

        assertTrue(mdtService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, mdtService.listByQuery(q).size());

        int imported = mdtService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 5 条种子因重复而跳过，只补回导入测试患者这一条
        assertEquals(all.size(), mdtService.listByQuery(q).size());
        MdtApplication restored = mdtService.listByQuery(q).stream()
                .filter(a -> "导入测试患者".equals(a.getPatientName()))
                .findFirst().orElseThrow();
        assertEquals("ZY202612001", restored.getInpatientNo());
        assertEquals("IN_HOSPITAL", restored.getMdtType());
        assertEquals(LocalDateTime.of(2026, 12, 8, 14, 0), restored.getExpectedTime());
        assertEquals(0, restored.getStatus()); // 导入的申请默认待审批
        assertTrue(mdtService.removeById(restored.getId()));
        assertEquals(all.size() - 1, mdtService.listByQuery(q).size());
    }

    private MdtApplication newApplication(String patientName, String mdtType) {
        MdtApplication a = new MdtApplication();
        a.setPatientName(patientName);
        a.setInpatientNo("ZY202612999");
        a.setMdtType(mdtType);
        a.setDeptId(1L);
        a.setDeptName("中医内科");
        a.setApplyPhysicianId(1L);
        a.setApplyPhysicianName("陈国强");
        a.setConditionSummary("病情摘要（测试）");
        a.setInvitedDepts("骨伤科");
        a.setInvitedExperts("王建军");
        a.setExpectedTime(LocalDateTime.of(2026, 12, 10, 15, 0));
        a.setUrgency("NORMAL");
        return a;
    }

    private MdtRecord newRecord(String attendees, LocalDateTime meetingTime, String opinion) {
        MdtRecord r = new MdtRecord();
        r.setAttendees(attendees);
        r.setMeetingTime(meetingTime);
        r.setMeetingPlace("医务科会议室");
        r.setOpinion(opinion);
        r.setSuggestion("后续治疗建议（测试）");
        r.setRecorder("陈国强");
        return r;
    }
}
