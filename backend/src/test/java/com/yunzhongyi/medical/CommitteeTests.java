package com.yunzhongyi.medical;

import com.yunzhongyi.medical.committee.dto.CommitteeMeetingQuery;
import com.yunzhongyi.medical.committee.dto.CommitteeQuery;
import com.yunzhongyi.medical.committee.dto.CommitteeRenewRequest;
import com.yunzhongyi.medical.committee.dto.CommitteeResolutionQuery;
import com.yunzhongyi.medical.committee.entity.Committee;
import com.yunzhongyi.medical.committee.entity.CommitteeMeeting;
import com.yunzhongyi.medical.committee.entity.CommitteeResolution;
import com.yunzhongyi.medical.committee.excel.CommitteeExcelUtil;
import com.yunzhongyi.medical.committee.service.CommitteeMeetingService;
import com.yunzhongyi.medical.committee.service.CommitteeResolutionService;
import com.yunzhongyi.medical.committee.service.CommitteeService;
import com.yunzhongyi.medical.committee.service.CommitteeTypeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 委员会管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 个委员会（医疗质量/药事/院感/输血/病案 5 种类别，
 * 在任 4 / 已换届 1 / 已解散 1，任期 90 天内到期 1 个）、5 次会议
 * （2026-10 月 1 次、2026-11 月 4 次）、5 条决议（待执行 2 / 执行中 1 /
 * 已完成 1 / 已取消 1，逾期 1 条，决议完成率 25.00%）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class CommitteeTests {

    @Autowired
    private CommitteeService committeeService;

    @Autowired
    private CommitteeMeetingService meetingService;

    @Autowired
    private CommitteeResolutionService resolutionService;

    @Test
    void contextLoads() {
        assertNotNull(committeeService);
        assertNotNull(meetingService);
        assertNotNull(resolutionService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(committeeService.count() >= 6);
        assertTrue(meetingService.count() >= 5);
        assertTrue(resolutionService.count() >= 5);

        // 种子委员会 1：医疗质量管理委员会，第 3 届，2025-01-01~2027-12-31，在任
        Committee c1 = committeeService.getById(1L);
        assertEquals("医疗质量管理委员会", c1.getName());
        assertEquals("MEDICAL_QUALITY", c1.getType());
        assertEquals("吴文斌", c1.getChairperson());
        assertEquals("陈国强", c1.getViceChairperson());
        assertEquals(15, c1.getMemberCount());
        assertEquals(3, c1.getTermNo());
        assertEquals(LocalDate.of(2025, 1, 1), c1.getTermStartDate());
        assertEquals(LocalDate.of(2027, 12, 31), c1.getTermEndDate());
        assertEquals(1, c1.getStatus());

        // 种子委员会 3：医院感染管理委员会，第 2 届，任期 2026-12-31 截止（90 天内到期）
        Committee c3 = committeeService.getById(3L);
        assertEquals("医院感染管理委员会", c3.getName());
        assertEquals("INFECTION", c3.getType());
        assertEquals(LocalDate.of(2026, 12, 31), c3.getTermEndDate());
        assertEquals(1, c3.getStatus());

        // 种子委员会 4：输血管理委员会第 1 届，已换届；5：同名第 2 届，在任
        Committee c4 = committeeService.getById(4L);
        assertEquals("输血管理委员会", c4.getName());
        assertEquals(1, c4.getTermNo());
        assertEquals(2, c4.getStatus());
        Committee c5 = committeeService.getById(5L);
        assertEquals("输血管理委员会", c5.getName());
        assertEquals(2, c5.getTermNo());
        assertEquals(1, c5.getStatus());

        // 种子委员会 6：病案管理委员会，已解散
        Committee c6 = committeeService.getById(6L);
        assertEquals("病案管理委员会", c6.getName());
        assertEquals(3, c6.getStatus());

        // 种子会议 1：医疗质量管理委员会 2026 年第 11 次会议，2026-11-10，吴文斌主持
        CommitteeMeeting m1 = meetingService.getById(1L);
        assertEquals(1L, m1.getCommitteeId());
        assertEquals("医疗质量管理委员会2026年第11次会议", m1.getTitle());
        assertEquals(LocalDate.of(2026, 11, 10), m1.getMeetingDate());
        assertEquals("吴文斌", m1.getHost());
        assertEquals(18, m1.getAttendeeCount());

        // 种子决议 3：修订抗菌药物分级管理目录，已完成，完成日期 2026-11-20
        CommitteeResolution r3 = resolutionService.getById(3L);
        assertEquals(3L, r3.getMeetingId());
        assertEquals("孙小红", r3.getResponsiblePerson());
        assertEquals(2, r3.getStatus());
        assertEquals(LocalDate.of(2026, 11, 20), r3.getCompleteDate());

        // 种子决议 4：落实三级医师查房制度，待执行，期限 2026-11-15 已逾期
        CommitteeResolution r4 = resolutionService.getById(4L);
        assertEquals(0, r4.getStatus());
        assertEquals(LocalDate.of(2026, 11, 15), r4.getDeadline());
        assertTrue(CommitteeTypeUtil.isResolutionOverdue(r4.getStatus(), r4.getDeadline()));

        // 种子决议 5：规范临床用血申请流程，已取消
        CommitteeResolution r5 = resolutionService.getById(5L);
        assertEquals(3, r5.getStatus());
    }

    @Test
    void pageQueryWithFilters() {
        CommitteeQuery q = new CommitteeQuery();
        q.setType("TRANSFUSION");
        assertEquals(2, committeeService.pageQuery(q).getTotal()); // 输血第 1、2 届

        CommitteeQuery q2 = new CommitteeQuery();
        q2.setStatus(1);
        assertEquals(4, committeeService.pageQuery(q2).getTotal()); // 在任 4 个

        CommitteeQuery q3 = new CommitteeQuery();
        q3.setKeyword("吴文斌");
        assertEquals(1, committeeService.pageQuery(q3).getTotal()); // 医疗质量管理委员会

        CommitteeQuery q4 = new CommitteeQuery();
        q4.setExpiringSoon(true);
        assertEquals(1, committeeService.pageQuery(q4).getTotal()); // 医院感染管理委员会

        // 会议：医疗质量管理委员会 2 次；2026-11 月 4 次
        CommitteeMeetingQuery mq = new CommitteeMeetingQuery();
        mq.setCommitteeId(1L);
        assertEquals(2, meetingService.pageQuery(mq).getTotal());
        CommitteeMeetingQuery mq2 = new CommitteeMeetingQuery();
        mq2.setDateFrom("2026-11-01");
        mq2.setDateTo("2026-11-30");
        assertEquals(4, meetingService.pageQuery(mq2).getTotal());

        // 决议：待执行 2 条；逾期 1 条；某会议的决议 2 条
        CommitteeResolutionQuery rq = new CommitteeResolutionQuery();
        rq.setStatus(0);
        assertEquals(2, resolutionService.pageQuery(rq).getTotal());
        CommitteeResolutionQuery rq2 = new CommitteeResolutionQuery();
        rq2.setOverdue(true);
        assertEquals(1, resolutionService.pageQuery(rq2).getTotal()); // 落实三级医师查房制度
        CommitteeResolutionQuery rq3 = new CommitteeResolutionQuery();
        rq3.setMeetingId(1L);
        assertEquals(2, resolutionService.pageQuery(rq3).getTotal()); // 会议 1 的两条决议
    }

    @Test
    void saveValidationAndDefaults() {
        // 委员会类别非法，拒绝保存
        Committee badType = newCommittee("测试委员会");
        badType.setType("XXX");
        assertThrows(IllegalArgumentException.class, () -> committeeService.save(badType));

        // 任期结束日期早于开始日期，拒绝保存
        Committee badDate = newCommittee("测试委员会");
        badDate.setTermEndDate(LocalDate.of(2024, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> committeeService.save(badDate));

        // 委员人数为负，拒绝保存
        Committee badCount = newCommittee("测试委员会");
        badCount.setMemberCount(-1);
        assertThrows(IllegalArgumentException.class, () -> committeeService.save(badCount));

        // 合法建档：默认在任、第 1 届
        Committee c = newCommittee("测试委员会");
        assertTrue(committeeService.save(c));
        try {
            assertNotNull(c.getId());
            assertEquals(CommitteeTypeUtil.STATUS_ACTIVE, c.getStatus());
            assertEquals(1, c.getTermNo());

            // 同一委员会名称同一届次已有记录，拒绝重复建档
            Committee dup = newCommittee("测试委员会");
            assertThrows(IllegalArgumentException.class, () -> committeeService.save(dup));
        } finally {
            committeeService.removeById(c.getId());
        }
    }

    @Test
    void renewFlow() {
        // 医疗质量管理委员会（种子 id=1，第 3 届、在任）换届：第 4 届新记录，原记录改为已换届
        CommitteeRenewRequest req = new CommitteeRenewRequest();
        req.setChairperson("陈国强");
        req.setViceChairperson("王建军");
        req.setSecretary("李雅琴");
        req.setMemberCount(17);
        req.setTermStartDate(LocalDate.of(2028, 1, 1));
        req.setTermEndDate(LocalDate.of(2030, 12, 31));
        Committee next = committeeService.renew(1L, req);
        try {
            assertNotNull(next.getId());
            assertNotEquals(1L, next.getId());
            assertEquals("医疗质量管理委员会", next.getName());
            assertEquals("MEDICAL_QUALITY", next.getType());
            assertEquals(4, next.getTermNo());
            assertEquals("陈国强", next.getChairperson());
            assertEquals(17, next.getMemberCount());
            assertEquals(LocalDate.of(2028, 1, 1), next.getTermStartDate());
            assertEquals(LocalDate.of(2030, 12, 31), next.getTermEndDate());
            assertEquals(1, next.getStatus());

            Committee old = committeeService.getById(1L);
            assertEquals(2, old.getStatus()); // 原记录已换届

            // 已换届的原记录不能再换届
            assertThrows(IllegalStateException.class, () -> committeeService.renew(1L, req));
        } finally {
            // 恢复种子：删除新一届记录，原记录恢复在任
            committeeService.removeById(next.getId());
            Committee restore = new Committee();
            restore.setId(1L);
            restore.setStatus(1);
            committeeService.getBaseMapper().updateById(restore);
            assertEquals(1, committeeService.getById(1L).getStatus());
        }
    }

    @Test
    void dissolveFlow() {
        // 药事管理与药物治疗学委员会（种子 id=2，在任）解散后状态 2→3，不能再解散
        Committee dissolved = committeeService.dissolve(2L);
        try {
            assertEquals(3, dissolved.getStatus());
            assertThrows(IllegalStateException.class, () -> committeeService.dissolve(2L));
        } finally {
            Committee restore = new Committee();
            restore.setId(2L);
            restore.setStatus(1);
            committeeService.getBaseMapper().updateById(restore);
            assertEquals(1, committeeService.getById(2L).getStatus());
        }

        // 已换届的委员会（种子 id=4）不能解散
        assertThrows(IllegalStateException.class, () -> committeeService.dissolve(4L));
    }

    @Test
    void meetingValidation() {
        // 已解散的委员会（种子 id=6）不能新增会议
        CommitteeMeeting badCommittee = newMeeting(6L, "病案管理委员会");
        assertThrows(IllegalStateException.class, () -> meetingService.save(badCommittee));

        // 会议日期为空，拒绝保存
        CommitteeMeeting badDate = newMeeting(1L, "医疗质量管理委员会");
        badDate.setMeetingDate(null);
        assertThrows(IllegalArgumentException.class, () -> meetingService.save(badDate));

        // 参会人数为负，拒绝保存
        CommitteeMeeting badCount = newMeeting(1L, "医疗质量管理委员会");
        badCount.setAttendeeCount(-1);
        assertThrows(IllegalArgumentException.class, () -> meetingService.save(badCount));

        // 合法登记：委员会名称自动补全
        CommitteeMeeting m = newMeeting(1L, null);
        assertTrue(meetingService.save(m));
        try {
            assertNotNull(m.getId());
            assertEquals("医疗质量管理委员会", m.getCommitteeName());

            // 同一委员会同一天同一主题已有会议，拒绝重复登记
            CommitteeMeeting dup = newMeeting(1L, "医疗质量管理委员会");
            assertThrows(IllegalArgumentException.class, () -> meetingService.save(dup));
        } finally {
            meetingService.removeById(m.getId());
        }
    }

    @Test
    void resolutionFlow() {
        // 决议 2（种子，待执行）：开始执行 0→1，再完成 1→2，完成日期默认为当天
        CommitteeResolution started = resolutionService.start(2L);
        try {
            assertEquals(1, started.getStatus());
            // 待执行的决议不能直接完成：先测另一条待执行决议 4
            assertThrows(IllegalStateException.class, () -> resolutionService.complete(4L));

            CommitteeResolution done = resolutionService.complete(2L);
            assertEquals(2, done.getStatus());
            assertEquals(LocalDate.now(), done.getCompleteDate());

            // 已完成的决议不能再开始、再完成、再取消、再修改
            assertThrows(IllegalStateException.class, () -> resolutionService.start(2L));
            assertThrows(IllegalStateException.class, () -> resolutionService.complete(2L));
            assertThrows(IllegalStateException.class, () -> resolutionService.cancel(2L));
            done.setTitle("改名后的决议");
            assertThrows(IllegalStateException.class, () -> resolutionService.updateById(done));
        } finally {
            // 恢复种子：状态改回待执行，并把完成日期显式清掉
            // （updateById 不更新 null 字段，所以这里用 updateWrapper 直接置空）
            resolutionService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CommitteeResolution>()
                            .eq(CommitteeResolution::getId, 2L)
                            .set(CommitteeResolution::getStatus, 0)
                            .set(CommitteeResolution::getCompleteDate, null));
            assertEquals(0, resolutionService.getById(2L).getStatus());
            assertNull(resolutionService.getById(2L).getCompleteDate());
        }

        // 决议 1（种子，执行中）取消后状态变为已取消；已取消的决议不能再取消、再完成
        CommitteeResolution cancelled = resolutionService.cancel(1L);
        try {
            assertEquals(3, cancelled.getStatus());
            assertThrows(IllegalStateException.class, () -> resolutionService.cancel(1L));
            assertThrows(IllegalStateException.class, () -> resolutionService.complete(1L));
        } finally {
            CommitteeResolution restore = new CommitteeResolution();
            restore.setId(1L);
            restore.setStatus(1);
            resolutionService.getBaseMapper().updateById(restore);
            assertEquals(1, resolutionService.getById(1L).getStatus());
        }

        // 同一会议同一决议事项已有记录，拒绝重复登记
        CommitteeResolution dup = newResolution(1L, "完善中医优势病种诊疗方案");
        assertThrows(IllegalArgumentException.class, () -> resolutionService.save(dup));

        // 合法登记：状态默认待执行，委员会 id 自动补全
        CommitteeResolution r = newResolution(1L, "测试决议");
        r.setStatus(null);
        r.setCommitteeId(null);
        assertTrue(resolutionService.save(r));
        try {
            assertEquals(0, r.getStatus());
            assertEquals(1L, r.getCommitteeId());
        } finally {
            resolutionService.removeById(r.getId());
        }
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = committeeService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(4L, s.get("active")); // 在任：医疗质量、药事、院感、输血第 2 届
        assertEquals(1L, s.get("renewed")); // 已换届：输血第 1 届
        assertEquals(1L, s.get("dissolved")); // 已解散：病案
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(1L, byType.get("MEDICAL_QUALITY"));
        assertEquals(1L, byType.get("PHARMACY"));
        assertEquals(1L, byType.get("INFECTION"));
        assertEquals(2L, byType.get("TRANSFUSION"));
        assertEquals(1L, byType.get("MEDICAL_RECORD"));
        assertEquals(0L, byType.get("OTHER"));

        // 任期 90 天内到期 1 个（医院感染管理委员会，2026-12-31）
        assertEquals(1L, s.get("expiringSoon"));

        // 会议 5 次；决议 5 条（待执行 2 / 执行中 1 / 已完成 1 / 已取消 1，完成率 25.00%，逾期 1 条）
        assertEquals(5L, s.get("meetingTotal"));
        assertEquals(5L, s.get("resolutionTotal"));
        assertEquals(2L, s.get("resolutionPending"));
        assertEquals(1L, s.get("resolutionInProgress"));
        assertEquals(1L, s.get("resolutionDone"));
        assertEquals(1L, s.get("resolutionCancelled"));
        assertEquals(new BigDecimal("25.00"), s.get("completionRate"));
        assertEquals(1L, s.get("overdueResolutionCount")); // 落实三级医师查房制度

        // 按类别汇总：输血 2 个、在任 1 个；医疗质量 1 个、在任 1 个
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byTypeList = (List<Map<String, Object>>) s.get("byTypeList");
        Map<String, Object> transfusion = byTypeList.stream()
                .filter(d -> "TRANSFUSION".equals(d.get("type"))).findFirst().orElseThrow();
        assertEquals(2L, transfusion.get("count"));
        assertEquals(1L, transfusion.get("activeCount"));

        // 按月会议趋势：2026-10 月 1 次、2026-11 月 4 次
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(2, byMonth.size());
        assertEquals("2026-10", byMonth.get(0).get("month"));
        assertEquals(1L, byMonth.get(0).get("count"));
        assertEquals("2026-11", byMonth.get(1).get("month"));
        assertEquals(4L, byMonth.get(1).get("count"));
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> committeeService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> meetingService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> resolutionService.detail(999999L));
    }

    @Test
    void excelRoundTrip() {
        CommitteeQuery q = new CommitteeQuery();
        q.setPage(1);
        q.setSize(100);
        List<Committee> all = committeeService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CommitteeExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<Committee> back = CommitteeExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（名称、类别、主任委员、届次、任期、状态）
        Committee first = all.stream()
                .filter(c -> "医院感染管理委员会".equals(c.getName()))
                .findFirst().orElseThrow();
        Committee firstBack = back.stream()
                .filter(c -> "医院感染管理委员会".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals(first.getType(), firstBack.getType());
        assertEquals(first.getChairperson(), firstBack.getChairperson());
        assertEquals(first.getTermNo(), firstBack.getTermNo());
        assertEquals(first.getTermStartDate(), firstBack.getTermStartDate());
        assertEquals(first.getTermEndDate(), firstBack.getTermEndDate());
        assertEquals(first.getStatus(), firstBack.getStatus());
        assertEquals(first.getMemberCount(), firstBack.getMemberCount());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条委员会 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （委员会用逻辑删除，删后重导按“同一名称同一届次重复”规则会跳过种子行，只补回被删的那条）
        Committee fresh = newCommittee("导入测试委员会");
        assertTrue(committeeService.save(fresh));

        CommitteeQuery q = new CommitteeQuery();
        q.setPage(1);
        q.setSize(100);
        List<Committee> all = committeeService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CommitteeExcelUtil.write(all, out);

        assertTrue(committeeService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, committeeService.listByQuery(q).size());

        int imported = committeeService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因重复而跳过，只补回导入测试委员会这一条
        assertEquals(all.size(), committeeService.listByQuery(q).size());
        Committee restored = committeeService.listByQuery(q).stream()
                .filter(c -> "导入测试委员会".equals(c.getName()))
                .findFirst().orElseThrow();
        assertEquals("MEDICAL_QUALITY", restored.getType());
        assertEquals(1, restored.getStatus()); // 导入的委员会默认在任
        assertTrue(committeeService.removeById(restored.getId()));
        assertEquals(all.size() - 1, committeeService.listByQuery(q).size());
    }

    private Committee newCommittee(String name) {
        Committee c = new Committee();
        c.setName(name);
        c.setType("MEDICAL_QUALITY");
        c.setChairperson("吴文斌");
        c.setViceChairperson("陈国强");
        c.setSecretary("李雅琴");
        c.setMemberCount(15);
        c.setTermNo(1);
        c.setTermStartDate(LocalDate.of(2025, 1, 1));
        c.setTermEndDate(LocalDate.of(2027, 12, 31));
        return c;
    }

    private CommitteeMeeting newMeeting(Long committeeId, String committeeName) {
        CommitteeMeeting m = new CommitteeMeeting();
        m.setCommitteeId(committeeId);
        m.setCommitteeName(committeeName);
        m.setTitle("测试会议");
        m.setMeetingDate(LocalDate.of(2026, 12, 10));
        m.setLocation("会议室301");
        m.setHost("吴文斌");
        m.setRecorder("李雅琴");
        m.setAttendeeCount(15);
        m.setContent("测试会议内容");
        return m;
    }

    private CommitteeResolution newResolution(Long meetingId, String title) {
        CommitteeResolution r = new CommitteeResolution();
        r.setMeetingId(meetingId);
        r.setTitle(title);
        r.setContent("测试决议内容");
        r.setResponsiblePerson("陈国强");
        r.setDeadline(LocalDate.of(2026, 12, 31));
        r.setStatus(0);
        return r;
    }
}
