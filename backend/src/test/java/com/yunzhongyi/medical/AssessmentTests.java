package com.yunzhongyi.medical;

import com.yunzhongyi.medical.assessment.dto.AssessmentQuery;
import com.yunzhongyi.medical.assessment.dto.AssessmentTemplateQuery;
import com.yunzhongyi.medical.assessment.entity.Assessment;
import com.yunzhongyi.medical.assessment.entity.AssessmentTemplate;
import com.yunzhongyi.medical.assessment.excel.AssessmentExcelUtil;
import com.yunzhongyi.medical.assessment.service.AssessmentGradeUtil;
import com.yunzhongyi.medical.assessment.service.AssessmentService;
import com.yunzhongyi.medical.assessment.service.AssessmentTemplateService;
import com.yunzhongyi.medical.common.PageResult;
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
 * 医师考核模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：3 个模板（权重之和均为 100）、6 条考核记录，覆盖 3 种考核类型、4 个等级。
 */
@SpringBootTest
@ActiveProfiles("h2")
class AssessmentTests {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AssessmentTemplateService templateService;

    private static final String TEMPLATE_1_ITEMS =
            "[{\"name\":\"医德医风\",\"weight\":30},{\"name\":\"业务能力\",\"weight\":30},"
                    + "{\"name\":\"工作实绩\",\"weight\":25},{\"name\":\"服务质量\",\"weight\":15}]";

    @Test
    void contextLoads() {
        assertNotNull(assessmentService);
        assertNotNull(templateService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(templateService.count() >= 3);
        assertTrue(assessmentService.count() >= 6);

        AssessmentTemplate t = templateService.getById(1L);
        assertEquals("医师定期考核模板", t.getTemplateName());
        assertEquals("PERIODIC", t.getAssessType());
        assertEquals(1, t.getStatus());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(AssessmentGradeUtil.sumWeights(t.getScoreItems())));

        // 种子里总分最高的定期考核：陈国强 92 分优秀
        AssessmentQuery q = new AssessmentQuery();
        q.setKeyword("陈国强");
        PageResult<Assessment> r = assessmentService.pageQuery(q);
        assertEquals(1, r.getTotal());
        Assessment chen = r.getRecords().get(0);
        assertEquals(new BigDecimal("92.00"), chen.getTotalScore());
        assertEquals("EXCELLENT", chen.getGrade());
        assertEquals("优秀", chen.getResult());
        assertEquals(2026, chen.getAssessYear());
        assertEquals(LocalDate.of(2026, 11, 20), chen.getAssessDate());
    }

    @Test
    void pageQueryWithFilters() {
        AssessmentQuery q = new AssessmentQuery();
        q.setAssessType("ETHICS");
        assertEquals(1, assessmentService.pageQuery(q).getTotal()); // 林晓梅医德考评

        AssessmentQuery q2 = new AssessmentQuery();
        q2.setGrade("EXCELLENT");
        assertEquals(2, assessmentService.pageQuery(q2).getTotal()); // 陈国强、林晓梅

        AssessmentQuery q3 = new AssessmentQuery();
        q3.setAssessYear(2026);
        assertEquals(6, assessmentService.pageQuery(q3).getTotal());

        AssessmentQuery q4 = new AssessmentQuery();
        q4.setDeptId(1L);
        assertEquals(2, assessmentService.pageQuery(q4).getTotal()); // 中医内科：陈国强、李雅琴

        AssessmentQuery q5 = new AssessmentQuery();
        q5.setKeyword("王建军");
        assertEquals(1, assessmentService.pageQuery(q5).getTotal());

        AssessmentQuery q6 = new AssessmentQuery();
        q6.setTemplateId(3L);
        assertEquals(2, assessmentService.pageQuery(q6).getTotal()); // 医疗质量考核模板：孙小红、黄志明
    }

    @Test
    void gradeAutoCalculatedAtBoundaries() {
        // 90 分 → 优秀；80 分 → 合格；60 分 → 基本合格；55 分 → 不合格
        assertGrade(30, 30, 20, 10, "EXCELLENT", "优秀");       // 90
        assertGrade(25, 25, 20, 10, "QUALIFIED", "合格");       // 80
        assertGrade(20, 15, 15, 10, "BASIC", "基本合格");       // 60
        assertGrade(18, 16, 12, 9, "UNQUALIFIED", "不合格，需整改"); // 55
    }

    private void assertGrade(int ethics, int ability, int performance, int service,
                             String expectedGrade, String expectedResult) {
        Assessment a = new Assessment();
        a.setPhysicianId(1L);
        a.setPhysicianName("等级边界测试");
        a.setDeptId(1L);
        a.setDeptName("中医内科");
        a.setTemplateId(1L);
        a.setTemplateName("医师定期考核模板");
        a.setAssessType("PERIODIC");
        a.setAssessYear(2026);
        a.setCycle("年度");
        a.setItemScores(String.format(
                "{\"医德医风\":%d,\"业务能力\":%d,\"工作实绩\":%d,\"服务质量\":%d}",
                ethics, ability, performance, service));
        a.setAssessor("吴文斌");
        a.setAssessDate(LocalDate.of(2026, 11, 22));
        try {
            assertTrue(assessmentService.save(a));
            assertEquals(expectedGrade, a.getGrade());
            assertEquals(expectedResult, a.getResult());
            // 改分后等级自动重算：改成满分 100 应变为优秀
            a.setItemScores("{\"医德医风\":30,\"业务能力\":30,\"工作实绩\":25,\"服务质量\":15}");
            assertTrue(assessmentService.updateById(a));
            Assessment updated = assessmentService.getById(a.getId());
            assertEquals(new BigDecimal("100.00"), updated.getTotalScore());
            assertEquals("EXCELLENT", updated.getGrade());
        } finally {
            assessmentService.removeById(a.getId());
        }
    }

    @Test
    void scoreExceedingTemplateWeightRejected() {
        Assessment a = new Assessment();
        a.setPhysicianId(1L);
        a.setPhysicianName("超分测试");
        a.setTemplateId(1L);
        a.setTemplateName("医师定期考核模板");
        a.setAssessType("PERIODIC");
        a.setAssessYear(2026);
        // 医德医风满分 30，得 31 分应被拒绝
        a.setItemScores("{\"医德医风\":31,\"业务能力\":20,\"工作实绩\":20,\"服务质量\":10}");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.save(a));

        // 模板外的评分项也应被拒绝
        a.setItemScores("{\"医德医风\":20,\"不存在的维度\":20}");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.save(a));
    }

    @Test
    void templateCrudAndWeightValidation() {
        // 权重之和不是 100，拒绝保存
        AssessmentTemplate bad = new AssessmentTemplate();
        bad.setTemplateName("权重错误模板");
        bad.setAssessType("PERIODIC");
        bad.setCycle("年度");
        bad.setScoreItems("[{\"name\":\"医德医风\",\"weight\":30},{\"name\":\"业务能力\",\"weight\":30}]");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(bad));

        // 权重之和为 100，正常保存；默认启用
        AssessmentTemplate t = new AssessmentTemplate();
        t.setTemplateName("测试模板");
        t.setAssessType("QUALITY");
        t.setCycle("季度");
        t.setScoreItems("[{\"name\":\"病历质量\",\"weight\":50},{\"name\":\"诊疗规范\",\"weight\":50}]");
        assertTrue(templateService.save(t));
        assertNotNull(t.getId());
        assertEquals(1, t.getStatus());

        AssessmentTemplate found = templateService.getById(t.getId());
        assertEquals("测试模板", found.getTemplateName());

        // 停用后不在启用列表里
        found.setStatus(0);
        assertTrue(templateService.updateById(found));
        assertTrue(templateService.listEnabled().stream().noneMatch(x -> x.getId().equals(t.getId())));

        // 启用列表仍包含 3 个种子模板
        assertTrue(templateService.listEnabled().size() >= 3);

        assertTrue(templateService.removeById(t.getId()));
        assertNull(templateService.getById(t.getId()));
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = assessmentService.stats();
        assertTrue((Long) s.get("total") >= 6);
        assertTrue((Long) s.get("excellentCount") >= 2);
        // 优秀率 = 优秀人数 / 总数（种子：2/6 = 33.33%）
        assertEquals(0, new BigDecimal("33.33").compareTo((BigDecimal) s.get("excellentRate")));
        // 平均分与按等级分组自洽：各等级人数之和 = 总数
        Map<?, ?> byGrade = (Map<?, ?>) s.get("byGrade");
        assertEquals(2L, byGrade.get("EXCELLENT"));
        assertEquals(2L, byGrade.get("QUALIFIED"));
        assertEquals(1L, byGrade.get("BASIC"));
        assertEquals(1L, byGrade.get("UNQUALIFIED"));
        long sum = byGrade.values().stream().mapToLong(v -> (Long) v).sum();
        assertEquals(s.get("total"), sum);

        // 平均分自洽：种子总分 92+95+85+72+55+88=487，平均 81.17
        assertEquals(0, new BigDecimal("81.17").compareTo((BigDecimal) s.get("avgScore")));

        // 按科室汇总：中医内科 2 人（陈国强 92、李雅琴 55），平均 73.50
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        Map<String, Object> nk = byDept.stream()
                .filter(d -> "中医内科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(2L, nk.get("count"));
        assertEquals(0, new BigDecimal("73.50").compareTo((BigDecimal) nk.get("avgScore")));
        assertEquals(1L, nk.get("excellentCount"));
    }

    @Test
    void crudRoundTrip() {
        Assessment a = new Assessment();
        a.setPhysicianId(5L);
        a.setPhysicianName("增删改测试");
        a.setDeptId(5L);
        a.setDeptName("儿科");
        a.setTemplateId(1L);
        a.setTemplateName("医师定期考核模板");
        a.setAssessType("PERIODIC");
        a.setAssessYear(2026);
        a.setCycle("年度");
        a.setItemScores("{\"医德医风\":28,\"业务能力\":27,\"工作实绩\":22,\"服务质量\":12}");
        a.setAssessor("吴文斌");
        assertTrue(assessmentService.save(a));
        assertNotNull(a.getId());
        assertEquals(new BigDecimal("89.00"), a.getTotalScore());
        assertEquals("QUALIFIED", a.getGrade());

        Assessment found = assessmentService.getById(a.getId());
        assertEquals("增删改测试", found.getPhysicianName());

        found.setAssessor("修改后的考核人");
        assertTrue(assessmentService.updateById(found));
        assertEquals("修改后的考核人", assessmentService.getById(a.getId()).getAssessor());

        assertTrue(assessmentService.removeById(a.getId()));
        assertNull(assessmentService.getById(a.getId()));
    }

    @Test
    void excelRoundTrip() throws Exception {
        AssessmentQuery q = new AssessmentQuery();
        q.setPage(1);
        q.setSize(100);
        List<Assessment> all = assessmentService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AssessmentExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<Assessment> back = AssessmentExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（医师、模板、类型、年度、总分、等级）
        Assessment first = all.get(0);
        Assessment firstBack = back.stream()
                .filter(a -> first.getPhysicianName().equals(a.getPhysicianName())
                        && first.getTemplateName().equals(a.getTemplateName()))
                .findFirst().orElseThrow();
        assertEquals(first.getAssessType(), firstBack.getAssessType());
        assertEquals(first.getAssessYear(), firstBack.getAssessYear());
        assertEquals(first.getGrade(), firstBack.getGrade());
        // 各项得分往返后求和应与原总分一致
        assertEquals(0, first.getTotalScore().compareTo(AssessmentGradeUtil.sumItemScores(firstBack.getItemScores())));
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条考核 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （项目用逻辑删除，删后重导按“同医师同模板同年度重复”规则会跳过种子行，只补回被删的那条）
        Assessment fresh = new Assessment();
        fresh.setPhysicianId(12L);
        fresh.setPhysicianName("郑秀兰");
        fresh.setDeptId(4L);
        fresh.setDeptName("妇科");
        fresh.setTemplateId(2L);
        fresh.setTemplateName("医德考评模板");
        fresh.setAssessType("ETHICS");
        fresh.setAssessYear(2026);
        fresh.setCycle("年度");
        fresh.setItemScores("{\"医德医风\":45,\"服务态度\":22,\"廉洁行医\":21}");
        fresh.setAssessor("吴文斌");
        assertTrue(assessmentService.save(fresh));
        assertEquals(new BigDecimal("88.00"), fresh.getTotalScore());
        assertEquals("QUALIFIED", fresh.getGrade());

        AssessmentQuery q = new AssessmentQuery();
        q.setPage(1);
        q.setSize(100);
        List<Assessment> all = assessmentService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AssessmentExcelUtil.write(all, out);

        assertTrue(assessmentService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, assessmentService.listByQuery(q).size());

        int imported = assessmentService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因同医师同模板同年度重复而跳过，只补回郑秀兰这一条
        assertEquals(all.size(), assessmentService.listByQuery(q).size());
        Assessment restored = assessmentService.listByQuery(q).stream()
                .filter(a -> "郑秀兰".equals(a.getPhysicianName())).findFirst().orElseThrow();
        assertEquals("ETHICS", restored.getAssessType());
        assertEquals(12L, restored.getPhysicianId()); // 导入时按姓名查医师档案补全医师 id
        assertEquals(2L, restored.getTemplateId()); // 导入时按模板名称补全模板 id
        assertEquals(new BigDecimal("88.00"), restored.getTotalScore());
        assertEquals("QUALIFIED", restored.getGrade());
        assertTrue(assessmentService.removeById(restored.getId()));
        assertEquals(all.size() - 1, assessmentService.listByQuery(q).size());
    }
}
