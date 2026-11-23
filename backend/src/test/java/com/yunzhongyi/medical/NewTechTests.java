package com.yunzhongyi.medical;

import com.yunzhongyi.medical.newtech.dto.NewTechQuery;
import com.yunzhongyi.medical.newtech.entity.NewTechProject;
import com.yunzhongyi.medical.newtech.entity.NewTechRecord;
import com.yunzhongyi.medical.newtech.excel.NewTechExcelUtil;
import com.yunzhongyi.medical.newtech.service.NewTechService;
import com.yunzhongyi.medical.newtech.service.NewTechTypeUtil;
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
 * 新技术、新项目管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：5 个项目，覆盖新技术/新项目 2 种类型、待审批/已立项/已验收/
 * 已驳回/已推广 5 种状态；伦理审查：待审查/已通过/未通过/不需审查各 1 条；
 * 3 条进展记录（骨伤科项目验收 1 条，儿科项目验收 + 推广各 1 条）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class NewTechTests {

    @Autowired
    private NewTechService newTechService;

    @Test
    void contextLoads() {
        assertNotNull(newTechService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(newTechService.count() >= 5);

        // 种子项目 1：温针灸治疗膝骨关节炎，中医内科，新技术，已立项，伦理已通过
        NewTechProject p1 = newTechService.getById(1L);
        assertEquals("温针灸治疗膝骨关节炎临床研究", p1.getProjectName());
        assertEquals("NEW_TECH", p1.getProjectType());
        assertEquals("中医内科", p1.getDeptName());
        assertEquals("陈国强", p1.getApplicantName());
        assertEquals("陈国强", p1.getProjectLeader());
        assertEquals(0, new BigDecimal("12.50").compareTo(p1.getBudget()));
        assertEquals(LocalDate.of(2026, 1, 5), p1.getStartDate());
        assertEquals(LocalDate.of(2026, 12, 31), p1.getEndDate());
        assertTrue(p1.getEthicsRequired());
        assertEquals(2, p1.getEthicsStatus());
        assertEquals("伦理委员会", p1.getEthicsReviewer());
        assertEquals(1, p1.getStatus());
        assertEquals("吴文斌", p1.getApprover());
        assertTrue(p1.getApprovalHistory().contains("伦理审查"));
        assertTrue(p1.getApprovalHistory().contains("通过"));

        // 种子项目 2：穴位埋线，待审批，伦理待审查
        NewTechProject p2 = newTechService.getById(2L);
        assertEquals("NEW_TECH", p2.getProjectType());
        assertEquals(0, p2.getStatus());
        assertEquals(1, p2.getEthicsStatus());

        // 种子项目 3：中药熏洗，骨伤科，已验收，不需伦理审查
        NewTechProject p3 = newTechService.getById(3L);
        assertEquals("NEW_PROJECT", p3.getProjectType());
        assertEquals(2, p3.getStatus());
        assertFalse(p3.getEthicsRequired());
        assertEquals(0, p3.getEthicsStatus());

        // 种子项目 4：小儿推拿，儿科，已推广
        NewTechProject p4 = newTechService.getById(4L);
        assertEquals(4, p4.getStatus());

        // 种子项目 5：中药周期疗法，已驳回，伦理未通过
        NewTechProject p5 = newTechService.getById(5L);
        assertEquals(3, p5.getStatus());
        assertEquals(3, p5.getEthicsStatus());
        assertTrue(p5.getApprovalHistory().contains("未通过"));
        assertTrue(p5.getApprovalHistory().contains("驳回"));

        // 种子进展记录：骨伤科项目 1 条验收记录，儿科项目验收 + 推广各 1 条
        List<NewTechRecord> p3Records = newTechService.listRecords(3L);
        assertEquals(1, p3Records.size());
        assertEquals("ACCEPTANCE", p3Records.get(0).getRecordType());
        assertTrue(p3Records.get(0).getConclusion().contains("验收通过"));
        List<NewTechRecord> p4Records = newTechService.listRecords(4L);
        assertEquals(2, p4Records.size());
        assertEquals("ACCEPTANCE", p4Records.get(0).getRecordType());
        assertEquals("PROMOTION", p4Records.get(1).getRecordType());
        assertTrue(p4Records.get(1).getContent().contains("全面推广"));
        // 待审批的项目暂无进展记录
        assertEquals(0, newTechService.listRecords(2L).size());
    }

    @Test
    void pageQueryWithFilters() {
        NewTechQuery q = new NewTechQuery();
        q.setProjectType("NEW_TECH");
        assertEquals(3, newTechService.pageQuery(q).getTotal()); // 温针灸、穴位埋线、中药周期疗法

        NewTechQuery q2 = new NewTechQuery();
        q2.setProjectType("NEW_PROJECT");
        assertEquals(2, newTechService.pageQuery(q2).getTotal()); // 中药熏洗、小儿推拿

        NewTechQuery q3 = new NewTechQuery();
        q3.setStatus(0);
        assertEquals(1, newTechService.pageQuery(q3).getTotal()); // 待审批：穴位埋线

        NewTechQuery q4 = new NewTechQuery();
        q4.setStatus(1);
        assertEquals(1, newTechService.pageQuery(q4).getTotal()); // 已立项：温针灸

        NewTechQuery q5 = new NewTechQuery();
        q5.setStatus(2);
        assertEquals(1, newTechService.pageQuery(q5).getTotal()); // 已验收：中药熏洗

        NewTechQuery q6 = new NewTechQuery();
        q6.setStatus(4);
        assertEquals(1, newTechService.pageQuery(q6).getTotal()); // 已推广：小儿推拿

        NewTechQuery q7 = new NewTechQuery();
        q7.setEthicsStatus(1);
        assertEquals(1, newTechService.pageQuery(q7).getTotal()); // 伦理待审查：穴位埋线

        NewTechQuery q8 = new NewTechQuery();
        q8.setEthicsStatus(3);
        assertEquals(1, newTechService.pageQuery(q8).getTotal()); // 伦理未通过：中药周期疗法

        NewTechQuery q9 = new NewTechQuery();
        q9.setDeptId(1L);
        assertEquals(1, newTechService.pageQuery(q9).getTotal()); // 中医内科：温针灸

        NewTechQuery q10 = new NewTechQuery();
        q10.setStartDate(LocalDate.of(2026, 11, 1));
        q10.setEndDate(LocalDate.of(2026, 11, 30));
        assertEquals(3, newTechService.pageQuery(q10).getTotal()); // 11 月申报：温针灸、穴位埋线、中药周期疗法

        NewTechQuery q11 = new NewTechQuery();
        q11.setKeyword("温针灸");
        assertEquals(1, newTechService.pageQuery(q11).getTotal());

        NewTechQuery q12 = new NewTechQuery();
        q12.setKeyword("孙小红");
        assertEquals(1, newTechService.pageQuery(q12).getTotal()); // 申报人姓名也能查

        NewTechQuery q13 = new NewTechQuery();
        q13.setKeyword("王建军");
        assertEquals(1, newTechService.pageQuery(q13).getTotal()); // 项目负责人也能查
    }

    @Test
    void saveValidationAndDefaults() {
        // 项目类型非法，拒绝保存
        NewTechProject badType = newProject("测试项目", "NEW_TECH");
        badType.setProjectType("XXX");
        assertThrows(IllegalArgumentException.class, () -> newTechService.save(badType));

        // 计划结束日期早于开始日期，拒绝保存
        NewTechProject badDate = newProject("测试项目", "NEW_TECH");
        badDate.setStartDate(LocalDate.of(2026, 12, 1));
        badDate.setEndDate(LocalDate.of(2026, 11, 1));
        assertThrows(IllegalArgumentException.class, () -> newTechService.save(badDate));

        // 经费预算为负数，拒绝保存
        NewTechProject badBudget = newProject("测试项目", "NEW_TECH");
        badBudget.setBudget(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> newTechService.save(badBudget));

        // 合法申报：默认待审批、申报日期为当天；需伦理审查的默认伦理待审查
        NewTechProject p = newProject("测试项目", "NEW_PROJECT");
        p.setEthicsRequired(true);
        assertTrue(newTechService.save(p));
        assertNotNull(p.getId());
        assertEquals(NewTechTypeUtil.STATUS_PENDING, p.getStatus());
        assertEquals(NewTechTypeUtil.ETHICS_PENDING, p.getEthicsStatus());
        assertEquals(LocalDate.now(), p.getApplyDate());
        newTechService.removeById(p.getId());

        // 不需伦理审查的，伦理状态默认不需审查
        NewTechProject p2 = newProject("测试项目二", "NEW_TECH");
        p2.setEthicsRequired(false);
        assertTrue(newTechService.save(p2));
        assertEquals(NewTechTypeUtil.ETHICS_NOT_REQUIRED, p2.getEthicsStatus());
        newTechService.removeById(p2.getId());
    }

    @Test
    void approvalFlowWithEthicsGate() {
        // 需伦理审查的项目：伦理未审查前不能审批立项
        NewTechProject p = newProject("审批测试项目", "NEW_TECH");
        p.setEthicsRequired(true);
        assertTrue(newTechService.save(p));
        try {
            assertThrows(IllegalStateException.class,
                    () -> newTechService.approve(p.getId(), "吴文斌", "同意立项"));

            // 伦理审查通过后才能审批立项
            NewTechProject reviewed = newTechService.ethicsReview(p.getId(), true, "伦理委员会", "符合要求");
            assertEquals(2, reviewed.getEthicsStatus());
            assertEquals("伦理委员会", reviewed.getEthicsReviewer());

            NewTechProject approved = newTechService.approve(p.getId(), "吴文斌", "同意立项");
            assertEquals(1, approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertEquals("同意立项", approved.getApprovalOpinion());
            assertTrue(approved.getApprovalHistory().contains("伦理审查"));
            assertTrue(approved.getApprovalHistory().contains("医务科"));
            assertTrue(approved.getApprovalHistory().contains("通过"));

            // 已结束申报阶段的项目不能再通过，也不能再驳回、再修改
            assertThrows(IllegalStateException.class,
                    () -> newTechService.approve(p.getId(), "吴文斌", "再次通过"));
            assertThrows(IllegalStateException.class,
                    () -> newTechService.reject(p.getId(), "吴文斌", "改主意了"));
            approved.setObjective("改项目目标");
            assertThrows(IllegalStateException.class, () -> newTechService.updateById(approved));
        } finally {
            newTechService.removeById(p.getId());
        }

        // 驳回流程：状态变为已驳回，审批记录留痕；驳回后不能再通过
        NewTechProject p2 = newProject("驳回测试项目", "NEW_PROJECT");
        p2.setEthicsRequired(false);
        assertTrue(newTechService.save(p2));
        try {
            NewTechProject rejected = newTechService.reject(p2.getId(), "吴文斌", "方案不成熟，请完善后重报");
            assertEquals(3, rejected.getStatus());
            assertTrue(rejected.getApprovalHistory().contains("驳回"));
            assertThrows(IllegalStateException.class,
                    () -> newTechService.approve(p2.getId(), "吴文斌", "改主意了"));
        } finally {
            newTechService.removeById(p2.getId());
        }
    }

    @Test
    void ethicsReviewFlow() {
        // 不需伦理审查的项目不能做伦理审查
        NewTechProject p = newProject("伦理测试项目", "NEW_TECH");
        p.setEthicsRequired(false);
        assertTrue(newTechService.save(p));
        try {
            assertThrows(IllegalStateException.class,
                    () -> newTechService.ethicsReview(p.getId(), true, "伦理委员会", "通过"));
        } finally {
            newTechService.removeById(p.getId());
        }

        // 需伦理审查的项目：审查未通过后不能再审查，审批立项也被拦住
        NewTechProject p2 = newProject("伦理未通过测试项目", "NEW_PROJECT");
        p2.setEthicsRequired(true);
        assertTrue(newTechService.save(p2));
        try {
            // 审查结论为空，拒绝
            assertThrows(IllegalArgumentException.class,
                    () -> newTechService.ethicsReview(p2.getId(), null, "伦理委员会", "无结论"));
            NewTechProject failed = newTechService.ethicsReview(p2.getId(), false, "伦理委员会", "知情同意不完善");
            assertEquals(3, failed.getEthicsStatus());
            assertEquals(LocalDate.now(), failed.getEthicsReviewDate());
            assertTrue(failed.getApprovalHistory().contains("伦理审查"));
            assertTrue(failed.getApprovalHistory().contains("未通过"));
            // 审查已结束，不能再审查；伦理未通过，不能审批立项
            assertThrows(IllegalStateException.class,
                    () -> newTechService.ethicsReview(p2.getId(), true, "伦理委员会", "改主意了"));
            assertThrows(IllegalStateException.class,
                    () -> newTechService.approve(p2.getId(), "吴文斌", "同意立项"));
        } finally {
            newTechService.removeById(p2.getId());
        }
    }

    @Test
    void updatePendingOnly() {
        // 待审批的项目可以修改
        NewTechProject p = newProject("修改测试项目", "NEW_TECH");
        assertTrue(newTechService.save(p));
        try {
            p.setObjective("更新后的项目目标");
            assertTrue(newTechService.updateById(p));
            assertEquals("更新后的项目目标", newTechService.getById(p.getId()).getObjective());
        } finally {
            newTechService.removeById(p.getId());
        }
    }

    @Test
    void recordFlow() {
        // 待审批的项目不能添加任何进展记录
        NewTechProject pending = newProject("记录测试项目", "NEW_TECH");
        pending.setEthicsRequired(false);
        assertTrue(newTechService.save(pending));
        try {
            NewTechRecord progress = newRecord("PROGRESS", LocalDate.of(2026, 12, 1), "进展情况（测试）");
            assertThrows(IllegalStateException.class, () -> newTechService.addRecord(pending.getId(), progress));
            NewTechRecord acceptance = newRecord("ACCEPTANCE", LocalDate.of(2026, 12, 2), "验收情况（测试）");
            assertThrows(IllegalStateException.class, () -> newTechService.addRecord(pending.getId(), acceptance));

            // 审批立项后才能记录：先加进展记录，状态仍为已立项
            newTechService.approve(pending.getId(), "吴文斌", "同意立项");
            NewTechRecord first = newTechService.addRecord(pending.getId(), progress);
            assertNotNull(first.getId());
            assertEquals(LocalDate.of(2026, 12, 1), first.getRecordDate());
            assertEquals(1, newTechService.getById(pending.getId()).getStatus());
            assertEquals(1, newTechService.listRecords(pending.getId()).size());

            // 验收记录使项目变为已验收
            NewTechRecord acc = newTechService.addRecord(pending.getId(), acceptance);
            assertNotNull(acc.getId());
            assertEquals(2, newTechService.getById(pending.getId()).getStatus());
            // 已验收后还能补进展记录
            NewTechRecord progress2 = newRecord("PROGRESS", LocalDate.of(2026, 12, 3), "验收后进展（测试）");
            newTechService.addRecord(pending.getId(), progress2);
            assertEquals(2, newTechService.getById(pending.getId()).getStatus());

            // 推广记录使项目变为已推广；已推广后不能再验收
            NewTechRecord promotion = newRecord("PROMOTION", LocalDate.of(2026, 12, 5), "推广情况（测试）");
            newTechService.addRecord(pending.getId(), promotion);
            assertEquals(4, newTechService.getById(pending.getId()).getStatus());
            NewTechRecord acc2 = newRecord("ACCEPTANCE", LocalDate.of(2026, 12, 6), "再次验收（测试）");
            assertThrows(IllegalStateException.class, () -> newTechService.addRecord(pending.getId(), acc2));
            assertEquals(4, newTechService.listRecords(pending.getId()).size());
            // 记录按记录日期升序
            assertEquals(LocalDate.of(2026, 12, 1),
                    newTechService.listRecords(pending.getId()).get(0).getRecordDate());

            // 删除一条记录
            assertTrue(newTechService.removeRecord(pending.getId(), progress2.getId()));
            assertEquals(3, newTechService.listRecords(pending.getId()).size());
            // 删不存在的记录，拒绝
            assertThrows(IllegalArgumentException.class,
                    () -> newTechService.removeRecord(pending.getId(), 999999L));

            // 记录内容为空，拒绝；记录类型非法，拒绝
            NewTechRecord bad = newRecord("PROGRESS", LocalDate.of(2026, 12, 7), "");
            assertThrows(IllegalArgumentException.class, () -> newTechService.addRecord(pending.getId(), bad));
            NewTechRecord badType = newRecord("XXX", LocalDate.of(2026, 12, 7), "内容");
            assertThrows(IllegalArgumentException.class, () -> newTechService.addRecord(pending.getId(), badType));
        } finally {
            newTechService.removeById(pending.getId());
        }

        // 已驳回的项目也不能添加进展记录
        NewTechProject rejected = newProject("驳回记录测试项目", "NEW_PROJECT");
        rejected.setEthicsRequired(false);
        assertTrue(newTechService.save(rejected));
        try {
            newTechService.reject(rejected.getId(), "吴文斌", "驳回");
            NewTechRecord r = newRecord("PROGRESS", LocalDate.of(2026, 12, 1), "内容");
            assertThrows(IllegalStateException.class, () -> newTechService.addRecord(rejected.getId(), r));
        } finally {
            newTechService.removeById(rejected.getId());
        }
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = newTechService.stats();
        assertEquals(5L, s.get("total"));
        assertEquals(1L, s.get("pending")); // 待审批：穴位埋线
        assertEquals(1L, s.get("approved")); // 已立项：温针灸
        assertEquals(1L, s.get("accepted")); // 已验收：中药熏洗
        assertEquals(1L, s.get("promoted")); // 已推广：小儿推拿
        assertEquals(1L, s.get("rejected")); // 已驳回：中药周期疗法
        assertEquals(1L, s.get("ethicsPending")); // 伦理待审查：穴位埋线
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(3L, byType.get("NEW_TECH"));
        assertEquals(2L, byType.get("NEW_PROJECT"));

        // 按科室汇总：5 个科室各 1 条
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        assertEquals(5, byDept.size());
        Map<String, Object> nk = byDept.stream()
                .filter(d -> "中医内科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(1L, nk.get("count"));
        assertEquals(1L, nk.get("approvedCount"));

        // 按月趋势：2026-01 1 条（已推广）、2026-02 1 条（已验收）、2026-11 3 条（已立项 1、待审批 1、已驳回 1）
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(3, byMonth.size());
        assertEquals("2026-01", byMonth.get(0).get("month"));
        assertEquals(1L, byMonth.get(0).get("count"));
        assertEquals(1L, byMonth.get(0).get("approvedCount"));
        assertEquals("2026-02", byMonth.get(1).get("month"));
        assertEquals(1L, byMonth.get(1).get("count"));
        assertEquals("2026-11", byMonth.get(2).get("month"));
        assertEquals(3L, byMonth.get(2).get("count"));
        assertEquals(1L, byMonth.get(2).get("approvedCount"));
        assertEquals(1L, byMonth.get(2).get("pendingCount"));
    }

    @Test
    void detailContainsRecords() {
        Map<String, Object> detail = newTechService.detail(4L);
        assertNotNull(detail.get("project"));
        @SuppressWarnings("unchecked")
        List<NewTechRecord> records = (List<NewTechRecord>) detail.get("records");
        assertEquals(2, records.size());
        // 不存在的项目，详情抛错
        assertThrows(IllegalArgumentException.class, () -> newTechService.detail(999999L));
    }

    @Test
    void excelRoundTrip() {
        NewTechQuery q = new NewTechQuery();
        q.setPage(1);
        q.setSize(100);
        List<NewTechProject> all = newTechService.listByQuery(q);
        assertTrue(all.size() >= 5);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NewTechExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<NewTechProject> back = NewTechExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（项目名称、类型、科室、申报人、预算、计划日期、伦理状态、审批状态）
        NewTechProject first = all.get(0);
        NewTechProject firstBack = back.stream()
                .filter(p -> first.getProjectName().equals(p.getProjectName()))
                .findFirst().orElseThrow();
        assertEquals(first.getProjectType(), firstBack.getProjectType());
        assertEquals(first.getDeptName(), firstBack.getDeptName());
        assertEquals(first.getApplicantName(), firstBack.getApplicantName());
        assertEquals(0, first.getBudget().compareTo(firstBack.getBudget()));
        assertEquals(first.getStartDate(), firstBack.getStartDate());
        assertEquals(first.getEndDate(), firstBack.getEndDate());
        assertEquals(first.getEthicsRequired(), firstBack.getEthicsRequired());
        assertEquals(first.getEthicsStatus(), firstBack.getEthicsStatus());
        assertEquals(first.getStatus(), firstBack.getStatus());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一个项目 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （项目用逻辑删除，删后重导按“同一项目名称同一申报科室重复”规则会跳过种子行，只补回被删的那条）
        NewTechProject fresh = newProject("导入测试项目", "NEW_TECH");
        fresh.setEthicsRequired(false);
        fresh.setStartDate(LocalDate.of(2026, 12, 1));
        fresh.setEndDate(LocalDate.of(2027, 6, 30));
        assertTrue(newTechService.save(fresh));

        NewTechQuery q = new NewTechQuery();
        q.setPage(1);
        q.setSize(100);
        List<NewTechProject> all = newTechService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NewTechExcelUtil.write(all, out);

        assertTrue(newTechService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, newTechService.listByQuery(q).size());

        int imported = newTechService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 5 条种子因重复而跳过，只补回导入测试项目这一条
        assertEquals(all.size(), newTechService.listByQuery(q).size());
        NewTechProject restored = newTechService.listByQuery(q).stream()
                .filter(p -> "导入测试项目".equals(p.getProjectName()))
                .findFirst().orElseThrow();
        assertEquals("NEW_TECH", restored.getProjectType());
        assertEquals("中医内科", restored.getDeptName());
        assertEquals("陈国强", restored.getApplicantName());
        assertEquals(0, restored.getStatus()); // 导入的项目默认待审批
        assertTrue(newTechService.removeById(restored.getId()));
        assertEquals(all.size() - 1, newTechService.listByQuery(q).size());
    }

    private NewTechProject newProject(String projectName, String projectType) {
        NewTechProject p = new NewTechProject();
        p.setProjectName(projectName);
        p.setProjectType(projectType);
        p.setDeptId(1L);
        p.setDeptName("中医内科");
        p.setApplicantId(1L);
        p.setApplicantName("陈国强");
        p.setProjectLeader("陈国强");
        p.setTeamMembers("陈国强、李雅琴");
        p.setBackground("项目背景（测试）");
        p.setObjective("项目目标（测试）");
        p.setTechPlan("技术方案（测试）");
        p.setExpectedBenefit("预期效益（测试）");
        p.setBudget(new BigDecimal("10.00"));
        p.setStartDate(LocalDate.of(2026, 11, 20));
        p.setEndDate(LocalDate.of(2027, 5, 31));
        p.setEthicsRequired(false);
        return p;
    }

    private NewTechRecord newRecord(String recordType, LocalDate recordDate, String content) {
        NewTechRecord r = new NewTechRecord();
        r.setRecordType(recordType);
        r.setRecordDate(recordDate);
        r.setContent(content);
        r.setConclusion("结论（测试）");
        r.setRecorder("陈国强");
        return r;
    }
}
