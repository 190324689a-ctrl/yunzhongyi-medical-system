package com.yunzhongyi.medical;

import com.yunzhongyi.medical.quality.dto.QualityPlanQuery;
import com.yunzhongyi.medical.quality.dto.QualityRectificationQuery;
import com.yunzhongyi.medical.quality.entity.QualityIssue;
import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityRecord;
import com.yunzhongyi.medical.quality.entity.QualityRectification;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.excel.QualityExcelUtil;
import com.yunzhongyi.medical.quality.service.QualityService;
import com.yunzhongyi.medical.quality.service.QualityTemplateService;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 * 质量检查管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：3 个检查模板（科室自查 / 院级检查 / 病历专项检查，权重之和均为 100）、
 * 4 个检查计划（覆盖 3 种检查类型、待执行/进行中/已完成 3 种状态）、3 条检查记录
 * （89 分合格、84 分合格、68 分基本合格）、4 个检查问题（2 严重/2 一般）、
 * 4 个整改任务（1 已复查通过、1 已整改待复查、2 待整改，其中 1 条已逾期）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class QualityTests {

    @Autowired
    private QualityService qualityService;

    @Autowired
    private QualityTemplateService templateService;

    @Test
    void contextLoads() {
        assertNotNull(qualityService);
        assertNotNull(templateService);
    }

    @Test
    void seedDataLoaded() {
        // 3 个模板，权重之和均为 100
        assertEquals(3, templateService.count());
        QualityTemplate self = templateService.getById(1L);
        assertEquals("科室自查模板", self.getTemplateName());
        assertEquals("SELF", self.getInspectType());
        assertEquals(1, self.getStatus());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(QualityTypeUtil.sumWeights(self.getCheckItems())));
        assertEquals(4, QualityTypeUtil.parseCheckItems(self.getCheckItems()).size());
        QualityTemplate hospital = templateService.getById(2L);
        assertEquals("HOSPITAL", hospital.getInspectType());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(QualityTypeUtil.sumWeights(hospital.getCheckItems())));
        QualityTemplate special = templateService.getById(3L);
        assertEquals("病历专项检查模板", special.getTemplateName());
        assertEquals("SPECIAL", special.getInspectType());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(QualityTypeUtil.sumWeights(special.getCheckItems())));

        // 4 个计划
        assertEquals(4, qualityService.count());
        QualityPlan plan1 = qualityService.getById(1L);
        assertEquals("院级质量检查模板", plan1.getTemplateName());
        assertEquals("HOSPITAL", plan1.getInspectType());
        assertEquals("中医内科", plan1.getDeptName());
        assertEquals("吴文斌、陈国强", plan1.getInspector());
        assertEquals(LocalDate.of(2026, 11, 25), plan1.getPlanDate());
        assertEquals(2, plan1.getStatus()); // 已完成
        assertEquals(1, qualityService.getById(2L).getStatus()); // 进行中
        assertEquals(0, qualityService.getById(3L).getStatus()); // 待执行
        assertEquals(0, qualityService.getById(4L).getStatus()); // 待执行

        // 3 条检查记录
        List<QualityRecord> plan1Records = qualityService.listRecords(1L);
        assertEquals(1, plan1Records.size());
        assertEquals(0, new BigDecimal("89").compareTo(plan1Records.get(0).getTotalScore()));
        assertEquals("QUALIFIED", plan1Records.get(0).getGrade());
        List<QualityRecord> plan2Records = qualityService.listRecords(2L);
        assertEquals(2, plan2Records.size());
        assertEquals(0, new BigDecimal("84").compareTo(plan2Records.get(0).getTotalScore()));
        assertEquals(0, new BigDecimal("68").compareTo(plan2Records.get(1).getTotalScore()));
        assertEquals("BASIC", plan2Records.get(1).getGrade());
        assertEquals(0, qualityService.listRecords(3L).size());

        // 4 个问题、4 个整改任务
        Map<String, Object> detail1 = qualityService.detail(1L);
        @SuppressWarnings("unchecked")
        List<QualityIssue> issues1 = (List<QualityIssue>) detail1.get("issues");
        assertEquals(2, issues1.size());
        assertEquals("MAJOR", issues1.get(0).getSeverity());
        assertTrue(issues1.get(0).getDescription().contains("查房记录"));
        @SuppressWarnings("unchecked")
        List<QualityRectification> rects1 = (List<QualityRectification>) detail1.get("rectifications");
        assertEquals(2, rects1.size());
        assertEquals(2, rects1.get(0).getStatus()); // 已复查通过
        assertEquals("吴文斌", rects1.get(0).getReviewer());
        assertEquals(false, rects1.get(0).getOverdue());

        Map<String, Object> detail2 = qualityService.detail(2L);
        @SuppressWarnings("unchecked")
        List<QualityRectification> rects2 = (List<QualityRectification>) detail2.get("rectifications");
        assertEquals(2, rects2.size());
        // 整改期限 2026-11-20 已过、仍待整改 → 逾期
        QualityRectification overdue = rects2.stream()
                .filter(r -> r.getId() == 4L).findFirst().orElseThrow();
        assertEquals(true, overdue.getOverdue());
        assertEquals("立即更换效期临近急救药品，建立效期登记台账", overdue.getMeasure());
    }

    @Test
    void pageQueryWithFilters() {
        QualityPlanQuery q = new QualityPlanQuery();
        q.setInspectType("SELF");
        assertEquals(2, qualityService.pageQuery(q).getTotal()); // 骨伤科、儿科自查

        QualityPlanQuery q2 = new QualityPlanQuery();
        q2.setInspectType("HOSPITAL");
        assertEquals(1, qualityService.pageQuery(q2).getTotal()); // 院级检查

        QualityPlanQuery q3 = new QualityPlanQuery();
        q3.setStatus(0);
        assertEquals(2, qualityService.pageQuery(q3).getTotal()); // 待执行：妇科、儿科

        QualityPlanQuery q4 = new QualityPlanQuery();
        q4.setStatus(2);
        assertEquals(1, qualityService.pageQuery(q4).getTotal()); // 已完成：中医内科

        QualityPlanQuery q5 = new QualityPlanQuery();
        q5.setDeptId(3L);
        assertEquals(1, qualityService.pageQuery(q5).getTotal()); // 骨伤科

        QualityPlanQuery q6 = new QualityPlanQuery();
        q6.setTemplateId(1L);
        assertEquals(2, qualityService.pageQuery(q6).getTotal()); // 科室自查模板

        QualityPlanQuery q7 = new QualityPlanQuery();
        q7.setStartDate(LocalDate.of(2026, 11, 25));
        q7.setEndDate(LocalDate.of(2026, 11, 26));
        assertEquals(2, qualityService.pageQuery(q7).getTotal()); // 11-25、11-26

        QualityPlanQuery q8 = new QualityPlanQuery();
        q8.setKeyword("骨伤科");
        assertEquals(1, qualityService.pageQuery(q8).getTotal());

        QualityPlanQuery q9 = new QualityPlanQuery();
        q9.setKeyword("吴文斌");
        assertEquals(1, qualityService.pageQuery(q9).getTotal()); // 检查人也能查
    }

    @Test
    void templateValidation() {
        // 权重之和不是 100，拒绝保存
        QualityTemplate badSum = newTemplate("权重测试模板", "SELF",
                "[{\"name\":\"A\",\"weight\":30},{\"name\":\"B\",\"weight\":30}]");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(badSum));

        // 检查项为空，拒绝保存
        QualityTemplate noItems = newTemplate("空项测试模板", "SELF", "[]");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(noItems));

        // 检查类型非法，拒绝保存
        QualityTemplate badType = newTemplate("类型测试模板", "XXX",
                "[{\"name\":\"A\",\"weight\":100}]");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(badType));

        // 合法模板：默认启用
        QualityTemplate t = newTemplate("测试模板", "SPECIAL",
                "[{\"name\":\"A\",\"weight\":60},{\"name\":\"B\",\"weight\":40}]");
        assertTrue(templateService.save(t));
        assertNotNull(t.getId());
        assertEquals(1, t.getStatus());
        try {
            // 停用后不能用于新建计划
            templateService.disable(t.getId());
            assertEquals(0, templateService.getById(t.getId()).getStatus());
            QualityPlan p = newPlan(t.getId(), "测试模板", "SPECIAL", 5L, "儿科");
            assertThrows(IllegalArgumentException.class, () -> qualityService.save(p));
            // 重新启用后可以建计划
            templateService.enable(t.getId());
            assertEquals(1, templateService.getById(t.getId()).getStatus());
            assertTrue(qualityService.save(p));
            qualityService.removeById(p.getId());
            // 启用模板下拉包含该模板
            assertTrue(templateService.listEnabled().stream().anyMatch(x -> x.getId().equals(t.getId())));
        } finally {
            templateService.removeById(t.getId());
        }
    }

    @Test
    void planFlow() {
        QualityPlan plan = newPlan(1L, "科室自查模板", "SELF", 5L, "儿科");
        plan.setPlanDate(LocalDate.of(2026, 12, 10));
        assertTrue(qualityService.save(plan));
        try {
            assertNotNull(plan.getId());
            assertEquals(0, plan.getStatus()); // 默认待执行
            // 待执行的计划可以修改
            plan.setInspector("孙小红、张伟");
            assertTrue(qualityService.updateById(plan));
            assertEquals("孙小红、张伟", qualityService.getById(plan.getId()).getInspector());

            // 待执行的计划不能直接完成、不能添加记录
            assertThrows(IllegalStateException.class, () -> qualityService.complete(plan.getId()));
            QualityRecord r = newRecord("{\"病历书写规范\":25,\"核心制度执行\":25,\"合理用药\":15,\"院感防控\":15}");
            assertThrows(IllegalStateException.class, () -> qualityService.addRecord(plan.getId(), r));

            // 开始检查：待执行 → 进行中；再开始一次拒绝
            QualityPlan started = qualityService.start(plan.getId());
            assertEquals(1, started.getStatus());
            assertThrows(IllegalStateException.class, () -> qualityService.start(plan.getId()));
            // 进行中的计划不能再修改
            plan.setInspector("改检查人");
            assertThrows(IllegalStateException.class, () -> qualityService.updateById(plan));

            // 没有记录时不能完成
            assertThrows(IllegalStateException.class, () -> qualityService.complete(plan.getId()));

            // 添加记录后才能完成
            QualityRecord saved = qualityService.addRecord(plan.getId(), r);
            assertNotNull(saved.getId());
            QualityPlan completed = qualityService.complete(plan.getId());
            assertEquals(2, completed.getStatus());
            // 已完成的计划不能再完成、不能再取消、不能再添加记录
            assertThrows(IllegalStateException.class, () -> qualityService.complete(plan.getId()));
            assertThrows(IllegalStateException.class, () -> qualityService.cancel(plan.getId()));
            assertThrows(IllegalStateException.class,
                    () -> qualityService.addRecord(plan.getId(), newRecord(
                            "{\"病历书写规范\":20,\"核心制度执行\":20,\"合理用药\":10,\"院感防控\":10}")));
        } finally {
            // 先删记录再删计划
            qualityService.listRecords(plan.getId()).forEach(rec ->
                    qualityService.removeRecord(plan.getId(), rec.getId()));
            qualityService.removeById(plan.getId());
        }

        // 取消流程：待执行 → 已取消；已取消不能再开始
        QualityPlan plan2 = newPlan(1L, "科室自查模板", "SELF", 4L, "妇科");
        plan2.setPlanDate(LocalDate.of(2026, 12, 11));
        assertTrue(qualityService.save(plan2));
        try {
            assertEquals(3, qualityService.cancel(plan2.getId()).getStatus());
            assertThrows(IllegalStateException.class, () -> qualityService.start(plan2.getId()));
            assertThrows(IllegalStateException.class, () -> qualityService.cancel(plan2.getId()));
        } finally {
            qualityService.removeById(plan2.getId());
        }
    }

    @Test
    void recordScoring() {
        QualityPlan plan = newPlan(1L, "科室自查模板", "SELF", 5L, "儿科");
        plan.setPlanDate(LocalDate.of(2026, 12, 12));
        assertTrue(qualityService.save(plan));
        try {
            qualityService.start(plan.getId());

            // 单项得分超过权重，拒绝
            QualityRecord over = newRecord("{\"病历书写规范\":31,\"核心制度执行\":25,\"合理用药\":15,\"院感防控\":15}");
            assertThrows(IllegalArgumentException.class, () -> qualityService.addRecord(plan.getId(), over));

            // 负分，拒绝
            QualityRecord negative = newRecord("{\"病历书写规范\":-1,\"核心制度执行\":25,\"合理用药\":15,\"院感防控\":15}");
            assertThrows(IllegalArgumentException.class, () -> qualityService.addRecord(plan.getId(), negative));

            // 90 分边界 → 优秀
            QualityRecord excellent = newRecord(
                    "{\"病历书写规范\":30,\"核心制度执行\":30,\"合理用药\":20,\"院感防控\":10}");
            QualityRecord savedExcellent = qualityService.addRecord(plan.getId(), excellent);
            assertEquals(0, new BigDecimal("90").compareTo(savedExcellent.getTotalScore()));
            assertEquals("EXCELLENT", savedExcellent.getGrade());
            assertEquals("儿科", savedExcellent.getDeptName());
            assertEquals(LocalDate.of(2026, 12, 10), savedExcellent.getCheckDate());

            // 检查日期为空时默认为当天
            QualityRecord noDate = newRecord(
                    "{\"病历书写规范\":25,\"核心制度执行\":25,\"合理用药\":15,\"院感防控\":15}");
            noDate.setCheckDate(null);
            QualityRecord savedNoDate = qualityService.addRecord(plan.getId(), noDate);
            assertEquals(LocalDate.now(), savedNoDate.getCheckDate());
            assertTrue(qualityService.removeRecord(plan.getId(), savedNoDate.getId()));

            // 59 分 → 不合格
            QualityRecord bad = newRecord(
                    "{\"病历书写规范\":20,\"核心制度执行\":20,\"合理用药\":10,\"院感防控\":9}");
            QualityRecord savedBad = qualityService.addRecord(plan.getId(), bad);
            assertEquals(0, new BigDecimal("59").compareTo(savedBad.getTotalScore()));
            assertEquals("UNQUALIFIED", savedBad.getGrade());

            // 删除记录
            assertTrue(qualityService.removeRecord(plan.getId(), savedBad.getId()));
            assertEquals(1, qualityService.listRecords(plan.getId()).size());
            assertThrows(IllegalArgumentException.class,
                    () -> qualityService.removeRecord(plan.getId(), 999999L));
        } finally {
            qualityService.listRecords(plan.getId()).forEach(rec ->
                    qualityService.removeRecord(plan.getId(), rec.getId()));
            qualityService.removeById(plan.getId());
        }
    }

    @Test
    void issueFlow() {
        // 在种子计划 2（进行中）的记录 2 上加问题
        QualityIssue issue = new QualityIssue();
        issue.setDescription("测试问题：门诊病历缺鉴别诊断");
        issue.setSeverity("MINOR");
        issue.setRespDeptName("骨伤科");
        issue.setRespPerson("张伟");
        QualityIssue saved = qualityService.addIssue(2L, issue);
        assertNotNull(saved.getId());
        assertEquals(2L, saved.getPlanId());
        assertEquals(LocalDate.now(), saved.getFoundDate());
        try {
            assertEquals(2, qualityService.listIssues(2L).size()); // 种子 1 个 + 新增 1 个
            // 严重程度非法，拒绝
            QualityIssue bad = new QualityIssue();
            bad.setDescription("严重程度非法");
            bad.setSeverity("XXX");
            assertThrows(IllegalArgumentException.class, () -> qualityService.addIssue(2L, bad));
            // 问题描述为空，拒绝
            QualityIssue empty = new QualityIssue();
            empty.setSeverity("MILD");
            assertThrows(IllegalArgumentException.class, () -> qualityService.addIssue(2L, empty));
        } finally {
            assertTrue(qualityService.removeIssue(2L, saved.getId()));
            assertEquals(1, qualityService.listIssues(2L).size());
            assertThrows(IllegalArgumentException.class,
                    () -> qualityService.removeIssue(2L, 999999L));
        }
    }

    @Test
    void rectificationFlow() {
        // 种子问题 3（门诊病历主诉不规范）已有整改任务 3，不能重复发
        QualityRectification dup = newRectification("重复发整改", LocalDate.of(2026, 12, 10), "张伟");
        assertThrows(IllegalStateException.class, () -> qualityService.addRectification(3L, dup));

        // 在种子问题 2 的记录上新发现一个问题，再发整改，走完整流转
        QualityIssue issue = new QualityIssue();
        issue.setDescription("测试问题：抗菌药物使用不规范");
        issue.setSeverity("MAJOR");
        issue.setRespPerson("陈国强");
        QualityIssue savedIssue = qualityService.addIssue(1L, issue);
        QualityRectification rect = null;
        try {
            rect = qualityService.addRectification(savedIssue.getId(),
                    newRectification("组织抗菌药物合理使用培训", LocalDate.of(2026, 12, 8), "陈国强"));
            assertNotNull(rect.getId());
            assertEquals(0, rect.getStatus()); // 默认待整改
            assertEquals(false, rect.getOverdue());
            Long rectId = rect.getId();

            // 待整改不能直接复查
            assertThrows(IllegalStateException.class,
                    () -> qualityService.reviewPass(rectId, "吴文斌", "直接复查"));
            // 提交整改：待整改 → 已整改，整改完成日期默认为当天
            QualityRectification done = qualityService.submitRectification(rectId);
            assertEquals(1, done.getStatus());
            assertEquals(LocalDate.now(), done.getRectifyDate());
            // 已整改不能再提交
            assertThrows(IllegalStateException.class, () -> qualityService.submitRectification(rectId));

            // 复查打回：已整改 → 待整改，整改完成日期清空，可重新提交
            QualityRectification rejected = qualityService.reviewReject(rectId, "吴文斌", "培训记录不全，请补充");
            assertEquals(0, rejected.getStatus());
            assertNull(rejected.getRectifyDate());
            assertEquals("吴文斌", rejected.getReviewer());
            assertEquals("培训记录不全，请补充", rejected.getReviewOpinion());

            // 重新提交 → 复查通过
            qualityService.submitRectification(rectId);
            QualityRectification reviewed = qualityService.reviewPass(rectId, "吴文斌", "复查通过");
            assertEquals(2, reviewed.getStatus());
            assertEquals(LocalDate.now(), reviewed.getReviewDate());
            // 已复查通过不能再提交、不能再复查
            assertThrows(IllegalStateException.class, () -> qualityService.submitRectification(rectId));
            assertThrows(IllegalStateException.class,
                    () -> qualityService.reviewPass(rectId, "吴文斌", "再次复查"));
        } finally {
            if (rect != null) {
                qualityService.removeIssue(1L, savedIssue.getId()); // 级联删整改任务
            } else {
                qualityService.removeIssue(1L, savedIssue.getId());
            }
        }

        // 逾期判定：期限为昨天的待整改任务就是逾期
        QualityIssue issue2 = new QualityIssue();
        issue2.setDescription("测试逾期问题");
        issue2.setSeverity("MILD");
        issue2.setRespPerson("测试人");
        QualityIssue savedIssue2 = qualityService.addIssue(1L, issue2);
        try {
            QualityRectification overdueRect = qualityService.addRectification(savedIssue2.getId(),
                    newRectification("测试逾期整改", LocalDate.now().minusDays(1), "测试人"));
            assertEquals(true, overdueRect.getOverdue());
            // 整改任务分页：只看逾期能查到它
            QualityRectificationQuery q = new QualityRectificationQuery();
            q.setOverdueOnly(true);
            assertTrue(qualityService.pageRectifications(q).getRecords().stream()
                    .anyMatch(r -> r.getId().equals(overdueRect.getId())));
            // 按责任人查
            QualityRectificationQuery q2 = new QualityRectificationQuery();
            q2.setRespPerson("测试人");
            assertEquals(1, qualityService.pageRectifications(q2).getTotal());
        } finally {
            qualityService.removeIssue(1L, savedIssue2.getId());
        }
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = qualityService.stats();
        assertEquals(4L, s.get("total"));
        assertEquals(2L, s.get("pending")); // 待执行：妇科、儿科
        assertEquals(1L, s.get("inProgress")); // 进行中：骨伤科
        assertEquals(1L, s.get("completed")); // 已完成：中医内科
        assertEquals(0L, s.get("cancelled"));
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(2L, byType.get("SELF"));
        assertEquals(1L, byType.get("HOSPITAL"));
        assertEquals(1L, byType.get("SPECIAL"));
        assertEquals(3L, s.get("recordCount"));
        assertEquals(new BigDecimal("80.33"), s.get("avgScore")); // (89+84+68)/3
        Map<?, ?> byGrade = (Map<?, ?>) s.get("byGrade");
        assertEquals(2L, byGrade.get("QUALIFIED"));
        assertEquals(1L, byGrade.get("BASIC"));
        assertEquals(0L, byGrade.get("EXCELLENT"));
        assertEquals(4L, s.get("issueCount"));
        assertEquals(4L, s.get("rectificationTotal"));
        assertEquals(1L, s.get("rectificationReviewed")); // 已复查通过 1 条
        assertEquals(new BigDecimal("25.00"), s.get("rectificationRate"));
        assertTrue((Long) s.get("overdueCount") >= 1); // 至少急救药品那条已逾期

        // 按科室汇总：4 个科室各 1 个计划
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        assertEquals(4, byDept.size());
        Map<String, Object> gs = byDept.stream()
                .filter(d -> "骨伤科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(1L, gs.get("count"));
        assertEquals(1L, gs.get("inProgressCount"));
        assertEquals(2L, gs.get("recordCount"));
        assertEquals(new BigDecimal("76.00"), gs.get("avgScore")); // (84+68)/2

        // 按月趋势：种子计划都在 2026-11，4 个、已完成 1 个、进行中 1 个
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(1, byMonth.size());
        assertEquals("2026-11", byMonth.get(0).get("month"));
        assertEquals(4L, byMonth.get(0).get("count"));
        assertEquals(1L, byMonth.get(0).get("completedCount"));
        assertEquals(1L, byMonth.get(0).get("inProgressCount"));
    }

    @Test
    void reportContainsSummary() {
        Map<String, Object> report = qualityService.report(1L);
        assertEquals(1L, report.get("recordCount"));
        assertEquals(new BigDecimal("89.00"), report.get("avgScore"));
        assertEquals(2L, report.get("issueCount"));
        assertEquals(2L, report.get("rectificationTotal"));
        assertEquals(1L, report.get("rectificationReviewed"));
        assertEquals(new BigDecimal("50.00"), report.get("rectificationRate"));
        Map<?, ?> bySeverity = (Map<?, ?>) report.get("bySeverity");
        assertEquals(1L, bySeverity.get("MAJOR"));
        assertEquals(1L, bySeverity.get("MINOR"));
        // 逾期数与返回的整改任务列表自洽
        @SuppressWarnings("unchecked")
        List<QualityRectification> rects = (List<QualityRectification>) report.get("rectifications");
        long expectedOverdue = rects.stream().filter(r -> Boolean.TRUE.equals(r.getOverdue())).count();
        assertEquals(expectedOverdue, report.get("overdueCount"));
        // 不存在的计划，报告抛错
        assertThrows(IllegalArgumentException.class, () -> qualityService.report(999999L));
    }

    @Test
    void excelExportAndTemplate() throws Exception {
        // 导出检查记录：有内容、有效 xlsx
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QualityPlanQuery q = new QualityPlanQuery();
        q.setPage(1);
        q.setSize(100);
        qualityService.exportExcel(q, out);
        assertTrue(out.size() > 0);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("检查记录", sheet.getSheetName());
            // 表头 + 3 条种子记录
            assertEquals(4, sheet.getLastRowNum() + 1);
            Row header = sheet.getRow(0);
            assertEquals("检查计划", header.getCell(0).getStringCellValue());
            assertEquals("总分", header.getCell(6).getStringCellValue());
        }

        // 导入模板下载：表头与导入格式一致
        ByteArrayOutputStream tpl = new ByteArrayOutputStream();
        QualityExcelUtil.writePlanTemplate(tpl);
        assertTrue(tpl.size() > 0);
        List<QualityPlan> back = QualityExcelUtil.readPlans(new ByteArrayInputStream(tpl.toByteArray()));
        assertEquals(0, back.size());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 造一个检查计划导入文件：1 条新计划 + 1 条重复计划 + 1 条模板不存在 + 1 条类型非法（给默认值）
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("检查计划");
            Row header = sheet.createRow(0);
            String[] headers = {"模板名称", "检查类型", "被查科室", "检查人", "计划检查日期", "计划状态", "备注"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            // 新计划：儿科自查 2026-12-15
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("科室自查模板");
            r1.createCell(1).setCellValue("科室自查");
            r1.createCell(2).setCellValue("儿科");
            r1.createCell(3).setCellValue("孙小红");
            r1.createCell(4).setCellValue("2026-12-15");
            r1.createCell(5).setCellValue("待执行");
            // 重复计划：与种子计划 4（科室自查模板/儿科/2026-11-28）同一模板同一科室同一日期
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("科室自查模板");
            r2.createCell(1).setCellValue("科室自查");
            r2.createCell(2).setCellValue("儿科");
            r2.createCell(3).setCellValue("孙小红");
            r2.createCell(4).setCellValue("2026-11-28");
            // 模板不存在
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("不存在的模板");
            r3.createCell(1).setCellValue("科室自查");
            r3.createCell(2).setCellValue("儿科");
            r3.createCell(3).setCellValue("孙小红");
            r3.createCell(4).setCellValue("2026-12-16");
            // 检查类型非法 → 按模板给默认值（科室自查模板是 SELF）
            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("科室自查模板");
            r4.createCell(1).setCellValue("乱填的类型");
            r4.createCell(2).setCellValue("妇科");
            r4.createCell(3).setCellValue("赵丽华");
            r4.createCell(4).setCellValue("2026-12-17");
            wb.write(out);
        }

        Map<String, Object> result = qualityService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(2, result.get("imported")); // 新计划 + 类型非法但给默认值的那条
        assertEquals(2, result.get("skipped")); // 重复 + 模板不存在
        @SuppressWarnings("unchecked")
        List<String> details = (List<String>) result.get("details");
        assertTrue(details.stream().anyMatch(d -> d.contains("重复")));
        assertTrue(details.stream().anyMatch(d -> d.contains("不存在")));
        assertTrue(details.stream().anyMatch(d -> d.contains("默认值")));

        // 导入的计划确实入库，且类型非法那条已按模板补为 SELF
        QualityPlanQuery q = new QualityPlanQuery();
        q.setKeyword("孙小红");
        q.setStartDate(LocalDate.of(2026, 12, 15));
        q.setEndDate(LocalDate.of(2026, 12, 15));
        List<QualityPlan> imported = qualityService.listByQuery(q);
        assertEquals(1, imported.size());
        assertEquals(0, imported.get(0).getStatus());
        QualityPlanQuery q2 = new QualityPlanQuery();
        q2.setKeyword("赵丽华");
        q2.setStartDate(LocalDate.of(2026, 12, 17));
        q2.setEndDate(LocalDate.of(2026, 12, 17));
        List<QualityPlan> fixed = qualityService.listByQuery(q2);
        assertEquals(1, fixed.size());
        assertEquals("SELF", fixed.get(0).getInspectType());

        // 再导同一文件：两条都重复，只补 0 条
        Map<String, Object> again = qualityService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, again.get("imported"));
        assertEquals(4, again.get("skipped"));

        // 清理导入的计划
        imported.forEach(p -> qualityService.removeById(p.getId()));
        fixed.forEach(p -> qualityService.removeById(p.getId()));
    }

    private QualityTemplate newTemplate(String name, String type, String items) {
        QualityTemplate t = new QualityTemplate();
        t.setTemplateName(name);
        t.setInspectType(type);
        t.setCheckItems(items);
        t.setRemark("测试模板");
        return t;
    }

    private QualityPlan newPlan(Long templateId, String templateName, String type, Long deptId, String deptName) {
        QualityPlan p = new QualityPlan();
        p.setTemplateId(templateId);
        p.setTemplateName(templateName);
        p.setInspectType(type);
        p.setDeptId(deptId);
        p.setDeptName(deptName);
        p.setInspector("吴文斌");
        p.setPlanDate(LocalDate.of(2026, 12, 10));
        return p;
    }

    private QualityRecord newRecord(String itemScores) {
        QualityRecord r = new QualityRecord();
        r.setItemScores(itemScores);
        r.setInspector("吴文斌");
        r.setCheckDate(LocalDate.of(2026, 12, 10));
        r.setResult("测试记录");
        return r;
    }

    private QualityRectification newRectification(String measure, LocalDate deadline, String respPerson) {
        QualityRectification r = new QualityRectification();
        r.setMeasure(measure);
        r.setDeadline(deadline);
        r.setRespPerson(respPerson);
        return r;
    }
}
