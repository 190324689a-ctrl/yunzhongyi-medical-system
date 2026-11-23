package com.yunzhongyi.medical;

import com.yunzhongyi.medical.statistics.dto.QualityIndicatorQuery;
import com.yunzhongyi.medical.statistics.dto.WorkloadQuery;
import com.yunzhongyi.medical.statistics.entity.QualityIndicator;
import com.yunzhongyi.medical.statistics.entity.WorkloadRecord;
import com.yunzhongyi.medical.statistics.excel.QualityIndicatorExcelUtil;
import com.yunzhongyi.medical.statistics.service.QualityIndicatorService;
import com.yunzhongyi.medical.statistics.service.StatisticsService;
import com.yunzhongyi.medical.statistics.service.StatisticsTypeUtil;
import com.yunzhongyi.medical.statistics.service.WorkloadService;
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
 * 统计模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：工作量登记 8 条（2026-11，门诊 3420 / 住院 1066 / 手术 241，
 * 合计 4727）；三级医院医疗质量指标 10 条（达标 7 条、未达标 3 条，
 * 达标率 70.00%；国家 4 / 省 3 / 市 3）。
 * 指标实际值均按库内已有业务数据聚合填写（见 data.sql 注释），测试里对
 * 跨模块聚合只断言“与同一库内实时数据一致”，不硬编码其他模块的种子数，
 * 避免与其他测试类的增删互相干扰。
 */
@SpringBootTest
@ActiveProfiles("h2")
class StatisticsTests {

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private QualityIndicatorService qualityIndicatorService;

    @Autowired
    private StatisticsService statisticsService;

    @Test
    void contextLoads() {
        assertNotNull(workloadService);
        assertNotNull(qualityIndicatorService);
        assertNotNull(statisticsService);
    }

    @Test
    void workloadSeedDataLoaded() {
        assertTrue(workloadService.count() >= 8);

        // 种子记录 1：陈国强，中医内科，2026-11，门诊 680 / 住院 210 / 手术 32
        WorkloadRecord w1 = workloadService.getById(1L);
        assertEquals("陈国强", w1.getPhysicianName());
        assertEquals("中医内科", w1.getDeptName());
        assertEquals("2026-11", w1.getStatMonth());
        assertEquals(680, w1.getOutpatientCount());
        assertEquals(210, w1.getInpatientCount());
        assertEquals(32, w1.getSurgeryCount());

        // 种子记录 8：吴文斌，医务科，行政科室，无临床工作量
        WorkloadRecord w8 = workloadService.getById(8L);
        assertEquals("吴文斌", w8.getPhysicianName());
        assertEquals("医务科", w8.getDeptName());
        assertEquals(0, w8.getOutpatientCount());
        assertEquals(0, w8.getInpatientCount());
        assertEquals(0, w8.getSurgeryCount());
    }

    @Test
    void workloadStatsTotals() {
        Map<String, Object> stats = workloadService.stats();
        // 种子合计：门诊 3420、住院 1066、手术 241、合计 4727（只增不减的断言，
        // 其他测试类不碰工作量表；本类新增的测试记录会在用例内自行删除）
        assertEquals(3420L, stats.get("totalOutpatient"));
        assertEquals(1066L, stats.get("totalInpatient"));
        assertEquals(241L, stats.get("totalSurgery"));
        assertEquals(4727L, stats.get("totalWorkload"));
        assertEquals(8L, stats.get("recordCount"));
        assertEquals(8L, stats.get("physicianCount"));
        assertEquals(8L, stats.get("deptCount"));
    }

    @Test
    void workloadStatsByDeptAndMonth() {
        List<Map<String, Object>> byDept = workloadService.statsByDept();
        assertEquals(8, byDept.size());
        // 按合计降序：中医内科 680+210+32=922 排第一
        assertEquals("中医内科", byDept.get(0).get("deptName"));
        assertEquals(922L, byDept.get(0).get("totalWorkload"));
        assertEquals(680L, byDept.get(0).get("outpatientCount"));
        // 医务科合计 0，排最后
        assertEquals("医务科", byDept.get(byDept.size() - 1).get("deptName"));
        assertEquals(0L, byDept.get(byDept.size() - 1).get("totalWorkload"));

        List<Map<String, Object>> byMonth = workloadService.statsByMonth();
        assertEquals(1, byMonth.size());
        assertEquals("2026-11", byMonth.get(0).get("month"));
        assertEquals(4727L, byMonth.get(0).get("totalWorkload"));

        List<Map<String, Object>> byPhysician = workloadService.statsByPhysician();
        assertEquals(8, byPhysician.size());
        assertEquals("陈国强", byPhysician.get(0).get("physicianName"));
        assertEquals(922L, byPhysician.get(0).get("totalWorkload"));
    }

    @Test
    void workloadCrudAndDuplicateRejected() {
        WorkloadRecord w = new WorkloadRecord();
        w.setPhysicianName("测试医师");
        w.setDeptName("中医内科");
        w.setStatMonth("2026-12");
        w.setOutpatientCount(100);
        w.setInpatientCount(50);
        w.setSurgeryCount(10);
        assertTrue(workloadService.save(w));
        assertNotNull(w.getId());

        // 同一医师同一统计月份重复登记应拒绝
        WorkloadRecord dup = new WorkloadRecord();
        dup.setPhysicianName("测试医师");
        dup.setDeptName("中医内科");
        dup.setStatMonth("2026-12");
        dup.setOutpatientCount(1);
        assertThrows(IllegalArgumentException.class, () -> workloadService.save(dup));

        // 负数应拒绝
        WorkloadRecord negative = new WorkloadRecord();
        negative.setPhysicianName("测试医师二");
        negative.setStatMonth("2026-12");
        negative.setOutpatientCount(-1);
        assertThrows(IllegalArgumentException.class, () -> workloadService.save(negative));

        // 修改后删除，恢复种子状态
        w.setOutpatientCount(120);
        assertTrue(workloadService.updateById(w));
        assertEquals(120, workloadService.getById(w.getId()).getOutpatientCount());
        assertTrue(workloadService.removeById(w.getId()));
        assertNull(workloadService.getById(w.getId()));
    }

    @Test
    void workloadExcelRoundTrip() throws Exception {
        // 新建一条测试记录，导出后删除，再从导出的文件里导入补回
        WorkloadRecord w = new WorkloadRecord();
        w.setPhysicianName("测试医师三");
        w.setDeptName("儿科");
        w.setStatMonth("2026-12");
        w.setOutpatientCount(33);
        w.setInpatientCount(22);
        w.setSurgeryCount(11);
        assertTrue(workloadService.save(w));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workloadService.exportExcel(new WorkloadQuery(), out);
        assertTrue(out.size() > 0);
        // xlsx 魔数 PK
        assertEquals(0x50, out.toByteArray()[0]);
        assertEquals(0x4B, out.toByteArray()[1]);

        assertTrue(workloadService.removeById(w.getId()));
        int imported = workloadService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported);
        WorkloadRecord restored = workloadService.getById(w.getId());
        // 导入按姓名 + 统计月份去重后新建，id 会变；按内容查回
        if (restored == null) {
            List<WorkloadRecord> found = workloadService.listByQuery(new WorkloadQuery() {
                {
                    setKeyword("测试医师三");
                    setStatMonth("2026-12");
                }
            });
            assertEquals(1, found.size());
            restored = found.get(0);
        }
        assertEquals(33, restored.getOutpatientCount());
        assertEquals(22, restored.getInpatientCount());
        assertEquals(11, restored.getSurgeryCount());

        // 再次导入同一文件：全部重复，导入 0 条
        assertEquals(0, workloadService.importExcel(new ByteArrayInputStream(out.toByteArray())));
        assertTrue(workloadService.removeById(restored.getId()));
    }

    @Test
    void indicatorSeedDataLoaded() {
        assertTrue(qualityIndicatorService.count() >= 10);

        // QI-001 医师考核优秀率：33.33%，目标 30%，越高越好 → 达标
        QualityIndicator qi1 = qualityIndicatorService.getById(1L);
        assertEquals("QI-001", qi1.getIndicatorCode());
        assertEquals("医师考核优秀率", qi1.getIndicatorName());
        assertEquals("医疗质量", qi1.getCategory());
        assertEquals("NATIONAL", qi1.getStandardLevel());
        assertEquals(0, new BigDecimal("33.33").compareTo(qi1.getActualValue()));
        assertEquals(Boolean.TRUE, StatisticsTypeUtil.evaluate(qi1));
        assertTrue(qi1.getDataSource().contains("医师考核记录"));

        // QI-004 整改完成率：25%，目标 90% → 未达标（诚实呈现短板）
        QualityIndicator qi4 = qualityIndicatorService.getById(4L);
        assertEquals(0, new BigDecimal("25.00").compareTo(qi4.getActualValue()));
        assertEquals(Boolean.FALSE, StatisticsTypeUtil.evaluate(qi4));

        // QI-005 严重问题占比：50%，越低越好，目标 ≤20% → 未达标
        QualityIndicator qi5 = qualityIndicatorService.getById(5L);
        assertEquals("LOWER_BETTER", qi5.getDirection());
        assertEquals(Boolean.FALSE, StatisticsTypeUtil.evaluate(qi5));

        // QI-008 月门诊量：3420 人次，与工作量登记合计一致
        QualityIndicator qi8 = qualityIndicatorService.getById(8L);
        assertEquals(0, new BigDecimal("3420.00").compareTo(qi8.getActualValue()));
        assertEquals(Boolean.TRUE, StatisticsTypeUtil.evaluate(qi8));
    }

    @Test
    void indicatorStatsTotals() {
        Map<String, Object> stats = qualityIndicatorService.stats();
        assertEquals(10L, stats.get("total"));
        assertEquals(10L, stats.get("reportedCount"));
        assertEquals(7L, stats.get("compliantCount"));
        assertEquals(0, new BigDecimal("70.00").compareTo((BigDecimal) stats.get("complianceRate")));

        // 按标准级别：国家 4 条达标 3 条（75%）、省 3 条达标 2 条（66.67%）、市 3 条达标 2 条（66.67%）
        List<Map<String, Object>> byLevel = qualityIndicatorService.statsByLevel();
        assertEquals(3, byLevel.size());
        assertEquals("NATIONAL", byLevel.get(0).get("standardLevel"));
        assertEquals(4L, byLevel.get(0).get("total"));
        assertEquals(3L, byLevel.get(0).get("compliantCount"));
        assertEquals(0, new BigDecimal("75.00").compareTo((BigDecimal) byLevel.get(0).get("complianceRate")));
        assertEquals("PROVINCE", byLevel.get(1).get("standardLevel"));
        assertEquals(2L, byLevel.get(1).get("compliantCount"));
        assertEquals("CITY", byLevel.get(2).get("standardLevel"));
        assertEquals(2L, byLevel.get(2).get("compliantCount"));

        // 按分类：医疗质量 5 条、医疗安全 2 条、工作效率 3 条
        List<Map<String, Object>> byCategory = qualityIndicatorService.statsByCategory();
        assertEquals(3, byCategory.size());
        assertTrue(byCategory.stream().anyMatch(r -> "医疗质量".equals(r.get("category")) && (Long) r.get("total") == 5L));
        assertTrue(byCategory.stream().anyMatch(r -> "医疗安全".equals(r.get("category")) && (Long) r.get("total") == 2L));
        assertTrue(byCategory.stream().anyMatch(r -> "工作效率".equals(r.get("category")) && (Long) r.get("total") == 3L));
    }

    @Test
    void indicatorCrudAndDuplicateCodeRejected() {
        QualityIndicator q = new QualityIndicator();
        q.setIndicatorCode("QI-TEST");
        q.setIndicatorName("测试指标");
        q.setCategory("医疗质量");
        q.setStandardLevel("CITY");
        q.setTargetValue(new BigDecimal("50.00"));
        q.setActualValue(new BigDecimal("60.00"));
        q.setUnit("%");
        q.setStatPeriod("2026-11");
        q.setDataSource("测试");
        assertTrue(qualityIndicatorService.save(q));
        assertNotNull(q.getId());

        // 同一指标编码重复应拒绝
        QualityIndicator dup = new QualityIndicator();
        dup.setIndicatorCode("QI-TEST");
        dup.setIndicatorName("测试指标二");
        dup.setCategory("医疗质量");
        dup.setStandardLevel("CITY");
        assertThrows(IllegalArgumentException.class, () -> qualityIndicatorService.save(dup));

        // 非法标准级别应拒绝
        QualityIndicator badLevel = new QualityIndicator();
        badLevel.setIndicatorCode("QI-TEST2");
        badLevel.setIndicatorName("测试指标三");
        badLevel.setCategory("医疗质量");
        badLevel.setStandardLevel("区县");
        assertThrows(IllegalArgumentException.class, () -> qualityIndicatorService.save(badLevel));

        // 未填报实际值的指标：达标判定为 null（未填报），不计入达标率
        // （直接对内存对象判定：MyBatis-Plus updateById 默认不更新 null 字段，
        // 库内值不会被这次 update 清空，所以不在库上测这一支）
        QualityIndicator unreported = new QualityIndicator();
        unreported.setTargetValue(new BigDecimal("50.00"));
        unreported.setActualValue(null);
        unreported.setDirection(StatisticsTypeUtil.DIRECTION_HIGHER);
        assertNull(StatisticsTypeUtil.evaluate(unreported));
        assertEquals("未填报", StatisticsTypeUtil.complianceText(unreported));

        assertTrue(qualityIndicatorService.removeById(q.getId()));
        assertNull(qualityIndicatorService.getById(q.getId()));
    }

    @Test
    void indicatorExcelRoundTrip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QualityIndicatorQuery query = new QualityIndicatorQuery();
        query.setPage(1);
        query.setSize(10000);
        qualityIndicatorService.exportExcel(query, out);
        assertTrue(out.size() > 0);
        assertEquals(0x50, out.toByteArray()[0]);
        assertEquals(0x4B, out.toByteArray()[1]);

        // 导出文件再读回：10 条种子指标都在
        List<QualityIndicator> readBack = QualityIndicatorExcelUtil.read(
                new ByteArrayInputStream(out.toByteArray()));
        assertEquals(10, readBack.size());
        assertEquals("QI-001", readBack.get(0).getIndicatorCode());
        assertEquals("NATIONAL", readBack.get(0).getStandardLevel());

        // 再次导入同一文件：全部按编码去重，导入 0 条
        assertEquals(0, qualityIndicatorService.importExcel(new ByteArrayInputStream(out.toByteArray())));

        // 删除一条种子指标再导入：补回 1 条
        assertTrue(qualityIndicatorService.removeById(10L));
        assertEquals(1, qualityIndicatorService.importExcel(new ByteArrayInputStream(out.toByteArray())));
        QualityIndicator restored = qualityIndicatorService.getById(10L);
        if (restored == null) {
            List<QualityIndicator> found = qualityIndicatorService.listByQuery(new QualityIndicatorQuery() {
                {
                    setKeyword("QI-010");
                }
            });
            assertEquals(1, found.size());
            restored = found.get(0);
        }
        assertEquals("QI-010", restored.getIndicatorCode());
        assertEquals(0, new BigDecimal("1066.00").compareTo(restored.getActualValue()));
    }

    @Test
    void statisticsOverviewAggregatesLiveData() {
        Map<String, Object> overview = statisticsService.overview();
        assertNotNull(overview.get("work"));
        assertNotNull(overview.get("tertiary"));
        assertNotNull(overview.get("quality"));

        // 工作统计与工作量服务同一口径
        Map<String, Object> work = (Map<String, Object>) overview.get("work");
        Map<String, Object> workload = (Map<String, Object>) work.get("workload");
        assertEquals(workloadService.stats().get("totalWorkload"), workload.get("totalWorkload"));
        // 排班人次是另一口径：种子排班 8 条（只断言下限，避免与其他测试类互相干扰）
        Map<String, Object> schedule = (Map<String, Object>) work.get("schedule");
        assertTrue(((Number) schedule.get("total")).longValue() >= 8);

        // 三级医院指标统计与指标服务同一口径
        Map<String, Object> tertiary = (Map<String, Object>) overview.get("tertiary");
        assertEquals(10L, tertiary.get("total"));
        assertEquals(7L, tertiary.get("compliantCount"));

        // 医疗质量统计：与各模块实时数据一致（只断言下限，避免与其他测试类互相干扰）
        Map<String, Object> quality = (Map<String, Object>) overview.get("quality");
        Map<String, Object> qualitySection = (Map<String, Object>) quality.get("quality");
        assertTrue(((Number) qualitySection.get("assessmentTotal")).longValue() >= 6);
        assertTrue(((Number) qualitySection.get("checkRecordCount")).longValue() >= 3);
        Map<String, Object> safety = (Map<String, Object>) quality.get("safety");
        assertTrue(((Number) safety.get("issueCount")).longValue() >= 4);
        assertTrue(((Number) safety.get("overdueCount")).longValue() >= 1);
        Map<String, Object> patientSafety = (Map<String, Object>) quality.get("patientSafety");
        assertTrue(((Number) patientSafety.get("mdtTotal")).longValue() >= 5);
        assertTrue(((Number) patientSafety.get("urgentMdtCount")).longValue() >= 2);
        Map<String, Object> newTech = (Map<String, Object>) quality.get("newTech");
        assertTrue(((Number) newTech.get("total")).longValue() >= 5);
        // 种子立项（含验收 / 推广）3 个：已立项 1 + 已验收 1 + 已推广 1（只断言下限）
        assertTrue(((Number) newTech.get("establishedCount")).longValue() >= 3);
    }

    @Test
    void statisticsReportExport() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        statisticsService.exportReport(out);
        assertTrue(out.size() > 0);
        assertEquals(0x50, out.toByteArray()[0]);
        assertEquals(0x4B, out.toByteArray()[1]);
        // 三个 sheet 都在：用 POI 读回校验 sheet 名
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                             new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(3, wb.getNumberOfSheets());
            assertEquals("工作统计", wb.getSheetName(0));
            assertEquals("三级医院医疗质量指标", wb.getSheetName(1));
            assertEquals("医疗质量统计", wb.getSheetName(2));
            // 工作统计 sheet：8 个科室 + 表头 + 合计行 = 10 行
            assertEquals(10, wb.getSheetAt(0).getLastRowNum() + 1);
            // 指标 sheet：10 条指标 + 表头 = 11 行
            assertEquals(11, wb.getSheetAt(1).getLastRowNum() + 1);
        }
    }
}
