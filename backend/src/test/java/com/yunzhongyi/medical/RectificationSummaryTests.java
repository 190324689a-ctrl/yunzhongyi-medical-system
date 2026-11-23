package com.yunzhongyi.medical;

import com.yunzhongyi.medical.rectification.dto.GenerateRequest;
import com.yunzhongyi.medical.rectification.dto.IssueSummaryQuery;
import com.yunzhongyi.medical.rectification.dto.OverdueQuery;
import com.yunzhongyi.medical.rectification.dto.RectificationSummaryQuery;
import com.yunzhongyi.medical.rectification.entity.RectificationSummary;
import com.yunzhongyi.medical.rectification.excel.RectificationSummaryExcelUtil;
import com.yunzhongyi.medical.rectification.service.RectificationSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 检查整改汇总模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 条汇总报告（2026-11 全院 1 条、按科室 2 条、按检查类型 3 条），
 * 按 2026-11 质量检查种子数据聚合：问题 4 个（严重 2 / 一般 2）、
 * 整改 4 条（待整改 2 / 已整改 1 / 已复查通过 1，整改完成率 25.00%）、逾期 1 条。
 * <p>
 * 注意：逾期判断依赖当天日期（测试当天为 2026-11-22）：整改任务 4
 *（王建军，期限 2026-11-20）已逾期；任务 2（期限 2026-12-05）、
 * 任务 3（期限 2026-12-03）未逾期——与质量检查模块的断言口径一致。
 */
@SpringBootTest
@ActiveProfiles("h2")
class RectificationSummaryTests {

    @Autowired
    private RectificationSummaryService rectificationSummaryService;

    @Test
    void contextLoads() {
        assertNotNull(rectificationSummaryService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(rectificationSummaryService.count() >= 6);

        // 种子 1：2026-11 全院汇总
        RectificationSummary all = rectificationSummaryService.getById(1L);
        assertEquals("2026-11", all.getStatPeriod());
        assertEquals("ALL", all.getScopeType());
        assertNull(all.getDeptName());
        assertNull(all.getInspectType());
        assertEquals(4, all.getPlanCount());
        assertEquals(3, all.getRecordCount());
        assertEquals(0, new BigDecimal("80.33").compareTo(all.getAvgScore()));
        assertEquals(4, all.getIssueCount());
        assertEquals(2, all.getMajorCount());
        assertEquals(2, all.getMinorCount());
        assertEquals(0, all.getMildCount());
        assertEquals(4, all.getRectificationTotal());
        assertEquals(2, all.getRectificationPending());
        assertEquals(1, all.getRectificationDone());
        assertEquals(1, all.getRectificationReviewed());
        assertEquals(0, new BigDecimal("25.00").compareTo(all.getRectificationRate()));
        assertEquals(1, all.getOverdueCount());
        assertEquals("吴文斌", all.getGeneratedBy());

        // 种子 2：2026-11 中医内科（院级检查，89 分，整改完成率 50%）
        RectificationSummary neike = rectificationSummaryService.getById(2L);
        assertEquals("DEPT", neike.getScopeType());
        assertEquals("中医内科", neike.getDeptName());
        assertEquals(1, neike.getPlanCount());
        assertEquals(1, neike.getRecordCount());
        assertEquals(0, new BigDecimal("89.00").compareTo(neike.getAvgScore()));
        assertEquals(2, neike.getIssueCount());
        assertEquals(1, neike.getMajorCount());
        assertEquals(1, neike.getMinorCount());
        assertEquals(2, neike.getRectificationTotal());
        assertEquals(0, neike.getRectificationPending());
        assertEquals(1, neike.getRectificationDone());
        assertEquals(1, neike.getRectificationReviewed());
        assertEquals(0, new BigDecimal("50.00").compareTo(neike.getRectificationRate()));
        assertEquals(0, neike.getOverdueCount());

        // 种子 3：2026-11 骨伤科（科室自查，平均 76 分，整改完成率 0%，逾期 1 条）
        RectificationSummary gushang = rectificationSummaryService.getById(3L);
        assertEquals("DEPT", gushang.getScopeType());
        assertEquals("骨伤科", gushang.getDeptName());
        assertEquals(1, gushang.getPlanCount());
        assertEquals(2, gushang.getRecordCount());
        assertEquals(0, new BigDecimal("76.00").compareTo(gushang.getAvgScore()));
        assertEquals(2, gushang.getIssueCount());
        assertEquals(2, gushang.getRectificationTotal());
        assertEquals(2, gushang.getRectificationPending());
        assertEquals(0, gushang.getRectificationReviewed());
        assertEquals(0, new BigDecimal("0.00").compareTo(gushang.getRectificationRate()));
        assertEquals(1, gushang.getOverdueCount());

        // 种子 4：2026-11 科室自查（2 个计划、2 条记录、平均 76 分、逾期 1 条）
        RectificationSummary self = rectificationSummaryService.getById(4L);
        assertEquals("TYPE", self.getScopeType());
        assertEquals("SELF", self.getInspectType());
        assertEquals(2, self.getPlanCount());
        assertEquals(2, self.getRecordCount());
        assertEquals(2, self.getIssueCount());
        assertEquals(0, new BigDecimal("0.00").compareTo(self.getRectificationRate()));
        assertEquals(1, self.getOverdueCount());

        // 种子 5：2026-11 院级检查（89 分，整改完成率 50%）
        RectificationSummary hospital = rectificationSummaryService.getById(5L);
        assertEquals("TYPE", hospital.getScopeType());
        assertEquals("HOSPITAL", hospital.getInspectType());
        assertEquals(1, hospital.getPlanCount());
        assertEquals(1, hospital.getRecordCount());
        assertEquals(2, hospital.getIssueCount());
        assertEquals(0, new BigDecimal("50.00").compareTo(hospital.getRectificationRate()));
        assertEquals(0, hospital.getOverdueCount());

        // 种子 6：2026-11 专项检查（妇科病历专项检查待执行，暂无问题与整改）
        RectificationSummary special = rectificationSummaryService.getById(6L);
        assertEquals("TYPE", special.getScopeType());
        assertEquals("SPECIAL", special.getInspectType());
        assertEquals(1, special.getPlanCount());
        assertEquals(0, special.getRecordCount());
        assertEquals(0, special.getIssueCount());
        assertEquals(0, special.getRectificationTotal());
        assertEquals(0, new BigDecimal("0.00").compareTo(special.getRectificationRate()));
        assertEquals(0, special.getOverdueCount());
    }

    @Test
    void pageQueryWithFilters() {
        RectificationSummaryQuery q = new RectificationSummaryQuery();
        q.setStatPeriod("2026-11");
        assertEquals(6, rectificationSummaryService.pageQuery(q).getTotal());

        RectificationSummaryQuery q2 = new RectificationSummaryQuery();
        q2.setScopeType("DEPT");
        assertEquals(2, rectificationSummaryService.pageQuery(q2).getTotal());

        RectificationSummaryQuery q3 = new RectificationSummaryQuery();
        q3.setScopeType("TYPE");
        assertEquals(3, rectificationSummaryService.pageQuery(q3).getTotal());

        RectificationSummaryQuery q4 = new RectificationSummaryQuery();
        q4.setDeptName("骨伤科");
        assertEquals(1, rectificationSummaryService.pageQuery(q4).getTotal());

        RectificationSummaryQuery q5 = new RectificationSummaryQuery();
        q5.setInspectType("SELF");
        assertEquals(1, rectificationSummaryService.pageQuery(q5).getTotal());

        RectificationSummaryQuery q6 = new RectificationSummaryQuery();
        q6.setStatPeriod("2026-10");
        assertEquals(0, rectificationSummaryService.pageQuery(q6).getTotal());
    }

    @Test
    void saveValidationAndDefaults() {
        // 统计周期格式非法，拒绝保存
        RectificationSummary badPeriod = newSummary("2026-13", "ALL");
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.save(badPeriod));

        // 汇总范围非法，拒绝保存
        RectificationSummary badScope = newSummary("2026-10", "XXX");
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.save(badScope));

        // 按科室汇总不填科室，拒绝保存
        RectificationSummary noDept = newSummary("2026-10", "DEPT");
        noDept.setDeptName(null);
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.save(noDept));

        // 按检查类型汇总不填类型，拒绝保存
        RectificationSummary noType = newSummary("2026-10", "TYPE");
        noType.setInspectType(null);
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.save(noType));

        // 计数为负，拒绝保存
        RectificationSummary negative = newSummary("2026-10", "ALL");
        negative.setIssueCount(-1);
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.save(negative));

        // 合法建档：空计数按 0 处理，整改完成率按已复查通过数现算
        RectificationSummary s = newSummary("2026-10", "ALL");
        s.setIssueCount(2);
        s.setMajorCount(1);
        s.setMinorCount(1);
        s.setRectificationTotal(2);
        s.setRectificationReviewed(1);
        s.setRectificationRate(new BigDecimal("99.99")); // 传入的完成率会被现算覆盖
        assertTrue(rectificationSummaryService.save(s));
        try {
            assertNotNull(s.getId());
            assertEquals(0, s.getPlanCount());
            assertEquals(0, new BigDecimal("0.00").compareTo(s.getAvgScore()));
            assertEquals(0, new BigDecimal("50.00").compareTo(s.getRectificationRate()));

            // 同一周期同一范围已有记录，拒绝重复建档（请重新生成）
            RectificationSummary dup = newSummary("2026-10", "ALL");
            assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.save(dup));
        } finally {
            rectificationSummaryService.removeById(s.getId());
        }
    }

    @Test
    void updateRecalculatesRateAndKeepsScope() {
        // 改种子 3（骨伤科）已复查通过数：完成率应重算为 50%，汇总口径不可改
        RectificationSummary update = new RectificationSummary();
        update.setId(3L);
        update.setStatPeriod("2026-12"); // 试图改口径，应被忽略
        update.setScopeType("ALL");
        update.setDeptName("中医内科");
        update.setRectificationReviewed(1);
        update.setRectificationPending(1);
        assertTrue(rectificationSummaryService.updateById(update));
        try {
            RectificationSummary after = rectificationSummaryService.getById(3L);
            assertEquals("2026-11", after.getStatPeriod());
            assertEquals("DEPT", after.getScopeType());
            assertEquals("骨伤科", after.getDeptName());
            assertEquals(1, after.getRectificationReviewed());
            assertEquals(0, new BigDecimal("50.00").compareTo(after.getRectificationRate()));
        } finally {
            // 恢复种子：已复查通过数改回 0、待整改改回 2
            rectificationSummaryService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RectificationSummary>()
                            .eq(RectificationSummary::getId, 3L)
                            .set(RectificationSummary::getRectificationReviewed, 0)
                            .set(RectificationSummary::getRectificationPending, 2)
                            .set(RectificationSummary::getRectificationRate, new BigDecimal("0.00")));
            RectificationSummary restored = rectificationSummaryService.getById(3L);
            assertEquals(0, restored.getRectificationReviewed());
            assertEquals(0, new BigDecimal("0.00").compareTo(restored.getRectificationRate()));
        }
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = rectificationSummaryService.stats();
        assertEquals(4L, s.get("issueCount"));
        Map<?, ?> bySeverity = (Map<?, ?>) s.get("bySeverity");
        assertEquals(2L, bySeverity.get("MAJOR"));
        assertEquals(2L, bySeverity.get("MINOR"));
        assertEquals(0L, bySeverity.get("MILD"));
        assertEquals(4L, s.get("rectificationTotal"));
        assertEquals(2L, s.get("rectificationPending"));
        assertEquals(1L, s.get("rectificationDone"));
        assertEquals(1L, s.get("rectificationReviewed"));
        assertEquals(0, new BigDecimal("25.00").compareTo((BigDecimal) s.get("rectificationRate")));
        assertEquals(1L, s.get("overdueCount"));
        assertEquals(4L, s.get("planCount"));
        assertEquals(3L, s.get("recordCount"));
        assertEquals(0, new BigDecimal("80.33").compareTo((BigDecimal) s.get("avgScore")));

        // 按科室汇总：中医内科问题 2、完成率 50%、逾期 0；骨伤科问题 2、完成率 0%、逾期 1
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        Map<String, Object> neike = byDept.stream()
                .filter(d -> "中医内科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(2L, neike.get("issueCount"));
        assertEquals(0, new BigDecimal("50.00").compareTo((BigDecimal) neike.get("rectificationRate")));
        assertEquals(0L, neike.get("overdueCount"));
        assertEquals(1L, neike.get("planCount"));
        assertEquals(0, new BigDecimal("89.00").compareTo((BigDecimal) neike.get("avgScore")));
        Map<String, Object> gushang = byDept.stream()
                .filter(d -> "骨伤科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(2L, gushang.get("issueCount"));
        assertEquals(0, new BigDecimal("0.00").compareTo((BigDecimal) gushang.get("rectificationRate")));
        assertEquals(1L, gushang.get("overdueCount"));
        // 妇科、儿科有计划无问题：问题 0、整改 0
        Map<String, Object> fuke = byDept.stream()
                .filter(d -> "妇科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(0L, fuke.get("issueCount"));
        assertEquals(1L, fuke.get("planCount"));
        assertEquals(0, new BigDecimal("0.00").compareTo((BigDecimal) fuke.get("rectificationRate")));

        // 按检查类型汇总：科室自查问题 2、完成率 0%、逾期 1；院级检查问题 2、完成率 50%；专项检查问题 0
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byType = (List<Map<String, Object>>) s.get("byType");
        assertEquals(3, byType.size());
        Map<String, Object> self = byType.stream()
                .filter(t -> "SELF".equals(t.get("inspectType"))).findFirst().orElseThrow();
        assertEquals(2L, self.get("issueCount"));
        assertEquals(0, new BigDecimal("0.00").compareTo((BigDecimal) self.get("rectificationRate")));
        assertEquals(1L, self.get("overdueCount"));
        Map<String, Object> hospital = byType.stream()
                .filter(t -> "HOSPITAL".equals(t.get("inspectType"))).findFirst().orElseThrow();
        assertEquals(2L, hospital.get("issueCount"));
        assertEquals(0, new BigDecimal("50.00").compareTo((BigDecimal) hospital.get("rectificationRate")));
        Map<String, Object> special = byType.stream()
                .filter(t -> "SPECIAL".equals(t.get("inspectType"))).findFirst().orElseThrow();
        assertEquals(0L, special.get("issueCount"));
        assertEquals(1L, special.get("planCount"));

        // 按月汇总：只有 2026-11 一个月，问题 4、完成率 25%、逾期 1
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(1, byMonth.size());
        assertEquals("2026-11", byMonth.get(0).get("month"));
        assertEquals(4L, byMonth.get(0).get("issueCount"));
        assertEquals(0, new BigDecimal("25.00").compareTo((BigDecimal) byMonth.get(0).get("rectificationRate")));
        assertEquals(1L, byMonth.get(0).get("overdueCount"));
        assertEquals(4L, byMonth.get(0).get("planCount"));
    }

    @Test
    void pageOverdue() {
        // 逾期整改只有 1 条：王建军，急救药品效期，期限 2026-11-20
        OverdueQuery q = new OverdueQuery();
        assertEquals(1, rectificationSummaryService.pageOverdue(q).getTotal());
        Map<String, Object> row = rectificationSummaryService.pageOverdue(q).getRecords().get(0);
        assertEquals("王建军", row.get("respPerson"));
        assertTrue(String.valueOf(row.get("issueDescription")).contains("急救药品"));
        assertEquals("MAJOR", row.get("severity"));
        assertEquals("骨伤科", row.get("deptName"));
        assertEquals("SELF", row.get("inspectType"));
        assertEquals(true, row.get("overdue"));
        assertEquals(0, row.get("status"));

        OverdueQuery q2 = new OverdueQuery();
        q2.setKeyword("王建军");
        assertEquals(1, rectificationSummaryService.pageOverdue(q2).getTotal());

        OverdueQuery q3 = new OverdueQuery();
        q3.setKeyword("李雅琴");
        assertEquals(0, rectificationSummaryService.pageOverdue(q3).getTotal());

        OverdueQuery q4 = new OverdueQuery();
        q4.setDeptName("中医内科");
        assertEquals(0, rectificationSummaryService.pageOverdue(q4).getTotal());
    }

    @Test
    void pageIssueSummary() {
        IssueSummaryQuery q = new IssueSummaryQuery();
        assertEquals(4, rectificationSummaryService.pageIssueSummary(q).getTotal());

        IssueSummaryQuery major = new IssueSummaryQuery();
        major.setSeverity("MAJOR");
        assertEquals(2, rectificationSummaryService.pageIssueSummary(major).getTotal());

        IssueSummaryQuery dept = new IssueSummaryQuery();
        dept.setDeptName("骨伤科");
        assertEquals(2, rectificationSummaryService.pageIssueSummary(dept).getTotal());

        IssueSummaryQuery type = new IssueSummaryQuery();
        type.setInspectType("HOSPITAL");
        assertEquals(2, rectificationSummaryService.pageIssueSummary(type).getTotal());

        // 只看逾期：只有问题 4（急救药品效期，整改期限 2026-11-20）
        IssueSummaryQuery overdue = new IssueSummaryQuery();
        overdue.setOverdueOnly(true);
        assertEquals(1, rectificationSummaryService.pageIssueSummary(overdue).getTotal());
        assertEquals(4L, rectificationSummaryService.pageIssueSummary(overdue).getRecords().get(0).get("id"));

        // 只看未完成整改：问题 2（已整改待复查）、3、4（待整改），问题 1 已复查通过不算
        IssueSummaryQuery unrectified = new IssueSummaryQuery();
        unrectified.setUnrectifiedOnly(true);
        assertEquals(3, rectificationSummaryService.pageIssueSummary(unrectified).getTotal());

        // 问题 1 的整改任务已复查通过，不逾期；问题 4 待整改且已逾期
        Map<String, Object> issue1 = rectificationSummaryService.pageIssueSummary(q).getRecords().stream()
                .filter(r -> Long.valueOf(1L).equals(r.get("id"))).findFirst().orElseThrow();
        assertEquals(2, issue1.get("rectificationStatus"));
        assertEquals(false, issue1.get("overdue"));
        Map<String, Object> issue4 = rectificationSummaryService.pageIssueSummary(q).getRecords().stream()
                .filter(r -> Long.valueOf(4L).equals(r.get("id"))).findFirst().orElseThrow();
        assertEquals(0, issue4.get("rectificationStatus"));
        assertEquals(true, issue4.get("overdue"));
        assertEquals("MAJOR", issue4.get("severity"));
    }

    @Test
    void generateFlow() {
        // 重新生成已有口径：全院 2026-11，原地更新 id=1，总数仍为 6
        GenerateRequest all = new GenerateRequest();
        all.setStatPeriod("2026-11");
        all.setScopeType("ALL");
        all.setGeneratedBy("吴文斌");
        RectificationSummary regenerated = rectificationSummaryService.generate(all);
        assertEquals(1L, regenerated.getId());
        assertEquals(4, regenerated.getIssueCount());
        assertEquals(2, regenerated.getMajorCount());
        assertEquals(0, new BigDecimal("25.00").compareTo(regenerated.getRectificationRate()));
        assertEquals(1, regenerated.getOverdueCount());
        assertEquals(6, rectificationSummaryService.count());

        // 重新生成按类型：科室自查 2026-11，原地更新 id=4
        GenerateRequest self = new GenerateRequest();
        self.setStatPeriod("2026-11");
        self.setScopeType("TYPE");
        self.setInspectType("SELF");
        RectificationSummary selfRegen = rectificationSummaryService.generate(self);
        assertEquals(4L, selfRegen.getId());
        assertEquals(2, selfRegen.getIssueCount());
        assertEquals(0, new BigDecimal("0.00").compareTo(selfRegen.getRectificationRate()));
        assertEquals(1, selfRegen.getOverdueCount());

        // 生成新口径：妇科 2026-11（有计划无问题），新增一条，用完删除
        GenerateRequest fuke = new GenerateRequest();
        fuke.setStatPeriod("2026-11");
        fuke.setScopeType("DEPT");
        fuke.setDeptName("妇科");
        RectificationSummary created = rectificationSummaryService.generate(fuke);
        try {
            assertNotEquals(1L, created.getId());
            assertEquals("妇科", created.getDeptName());
            assertEquals(1, created.getPlanCount());
            assertEquals(0, created.getRecordCount());
            assertEquals(0, created.getIssueCount());
            assertEquals(0, created.getRectificationTotal());
            assertEquals(0, new BigDecimal("0.00").compareTo(created.getRectificationRate()));
            assertEquals(0, created.getOverdueCount());
            assertEquals(7, rectificationSummaryService.count());

            // 再次生成同一口径：原地更新，不新增
            RectificationSummary again = rectificationSummaryService.generate(fuke);
            assertEquals(created.getId(), again.getId());
            assertEquals(7, rectificationSummaryService.count());
        } finally {
            rectificationSummaryService.removeById(created.getId());
            assertEquals(6, rectificationSummaryService.count());
        }

        // 生成请求校验：周期格式、范围、科室、类型缺失都拒绝
        GenerateRequest badPeriod = new GenerateRequest();
        badPeriod.setStatPeriod("2026/11");
        badPeriod.setScopeType("ALL");
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.generate(badPeriod));

        GenerateRequest noDept = new GenerateRequest();
        noDept.setStatPeriod("2026-11");
        noDept.setScopeType("DEPT");
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.generate(noDept));

        GenerateRequest badType = new GenerateRequest();
        badType.setStatPeriod("2026-11");
        badType.setScopeType("TYPE");
        badType.setInspectType("XXX");
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.generate(badType));
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> rectificationSummaryService.detail(999999L));
    }

    @Test
    void excelRoundTrip() {
        RectificationSummaryQuery q = new RectificationSummaryQuery();
        q.setPage(1);
        q.setSize(100);
        List<RectificationSummary> all = rectificationSummaryService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RectificationSummaryExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<RectificationSummary> back = RectificationSummaryExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（统计周期、汇总范围、科室、检查类型、各项计数、整改完成率）
        RectificationSummary first = all.stream()
                .filter(s -> Long.valueOf(1L).equals(s.getId())).findFirst().orElseThrow();
        RectificationSummary firstBack = back.stream()
                .filter(s -> "2026-11".equals(s.getStatPeriod()) && "ALL".equals(s.getScopeType()))
                .findFirst().orElseThrow();
        assertEquals(first.getPlanCount(), firstBack.getPlanCount());
        assertEquals(first.getRecordCount(), firstBack.getRecordCount());
        assertEquals(0, first.getAvgScore().compareTo(firstBack.getAvgScore()));
        assertEquals(first.getIssueCount(), firstBack.getIssueCount());
        assertEquals(first.getMajorCount(), firstBack.getMajorCount());
        assertEquals(first.getMinorCount(), firstBack.getMinorCount());
        assertEquals(first.getRectificationTotal(), firstBack.getRectificationTotal());
        assertEquals(first.getRectificationReviewed(), firstBack.getRectificationReviewed());
        assertEquals(0, first.getRectificationRate().compareTo(firstBack.getRectificationRate()));
        assertEquals(first.getOverdueCount(), firstBack.getOverdueCount());
    }

    @Test
    void importViaServiceRoundTrip() {
        // 新建一条 2026-10 全院汇总 → 导出全部 → 删掉这条 → 再导回，应只补回这一条
        // （6 条种子按“同一周期同一范围”规则跳过）
        RectificationSummary fresh = newSummary("2026-10", "ALL");
        fresh.setIssueCount(1);
        fresh.setRectificationTotal(1);
        fresh.setRectificationReviewed(1);
        assertTrue(rectificationSummaryService.save(fresh));

        RectificationSummaryQuery q = new RectificationSummaryQuery();
        q.setPage(1);
        q.setSize(100);
        List<RectificationSummary> all = rectificationSummaryService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RectificationSummaryExcelUtil.write(all, out);

        assertTrue(rectificationSummaryService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, rectificationSummaryService.listByQuery(q).size());

        int imported = rectificationSummaryService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因重复而跳过，只补回 2026-10 这一条
        assertEquals(all.size(), rectificationSummaryService.listByQuery(q).size());
        RectificationSummary restored = rectificationSummaryService.listByQuery(q).stream()
                .filter(s -> "2026-10".equals(s.getStatPeriod()))
                .findFirst().orElseThrow();
        assertEquals(1, restored.getIssueCount());
        assertEquals(0, new BigDecimal("100.00").compareTo(restored.getRectificationRate()));
        assertTrue(rectificationSummaryService.removeById(restored.getId()));
        assertEquals(all.size() - 1, rectificationSummaryService.listByQuery(q).size());
    }

    @Test
    void deleteFlow() {
        RectificationSummary s = newSummary("2026-09", "ALL");
        assertTrue(rectificationSummaryService.save(s));
        assertTrue(rectificationSummaryService.removeById(s.getId()));
        assertNull(rectificationSummaryService.getById(s.getId()));
    }

    private RectificationSummary newSummary(String statPeriod, String scopeType) {
        RectificationSummary s = new RectificationSummary();
        s.setStatPeriod(statPeriod);
        s.setScopeType(scopeType);
        if ("DEPT".equals(scopeType)) {
            s.setDeptName("中医内科");
        }
        if ("TYPE".equals(scopeType)) {
            s.setInspectType("SELF");
        }
        s.setGeneratedBy("吴文斌");
        return s;
    }
}
