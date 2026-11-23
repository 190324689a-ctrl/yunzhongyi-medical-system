package com.yunzhongyi.medical;

import com.yunzhongyi.medical.medgrading.dto.GradingAssessmentQuery;
import com.yunzhongyi.medical.medgrading.dto.GradingStandardQuery;
import com.yunzhongyi.medical.medgrading.entity.GradingAssessment;
import com.yunzhongyi.medical.medgrading.entity.GradingStandard;
import com.yunzhongyi.medical.medgrading.excel.GradingAssessmentExcelUtil;
import com.yunzhongyi.medical.medgrading.excel.GradingStandardExcelUtil;
import com.yunzhongyi.medical.medgrading.mapper.GradingAssessmentMapper;
import com.yunzhongyi.medical.medgrading.service.GradingAssessmentService;
import com.yunzhongyi.medical.medgrading.service.GradingStandardService;
import com.yunzhongyi.medical.medgrading.service.MedGradingTypeUtil;
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
 * 医疗分级管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：3 个分级标准（医疗质量 / 医疗服务 / 综合分级）、6 条分级评定
 * （2026-11：甲级 1 / 乙级 3 / 丙级 1 / 丁级 1；已通过 3 / 待审批 2 / 已驳回 1）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class MedGradingTests {

    @Autowired
    private GradingStandardService standardService;

    @Autowired
    private GradingAssessmentService assessmentService;

    @Autowired
    private GradingAssessmentMapper assessmentMapper;

    @Test
    void contextLoads() {
        assertNotNull(standardService);
        assertNotNull(assessmentService);
    }

    @Test
    void seedDataLoaded() {
        assertEquals(3L, standardService.count());
        assertEquals(6L, assessmentService.count());
        // 种子评定的等级与状态与分数一致
        assertEquals("A", assessmentService.getById(1L).getGrade());
        assertEquals(Integer.valueOf(1), assessmentService.getById(1L).getStatus());
        assertEquals("B", assessmentService.getById(2L).getGrade());
        assertEquals("C", assessmentService.getById(3L).getGrade());
        assertEquals(Integer.valueOf(0), assessmentService.getById(3L).getStatus());
        assertEquals("B", assessmentService.getById(4L).getGrade());
        assertEquals("D", assessmentService.getById(5L).getGrade());
        assertEquals(Integer.valueOf(2), assessmentService.getById(5L).getStatus());
        // 综合标准分数线 85/75/60：82 分为乙级
        assertEquals("B", assessmentService.getById(6L).getGrade());
        assertEquals("STD-C001", assessmentService.getById(6L).getStandardCode());
    }

    @Test
    void gradeBoundaries() {
        // 纯函数边界：90 甲、80 乙、60 丙、59.99 丁
        assertEquals("A", MedGradingTypeUtil.calcGrade(new BigDecimal("90"),
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("60")));
        assertEquals("B", MedGradingTypeUtil.calcGrade(new BigDecimal("89.99"),
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("60")));
        assertEquals("B", MedGradingTypeUtil.calcGrade(new BigDecimal("80"),
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("60")));
        assertEquals("C", MedGradingTypeUtil.calcGrade(new BigDecimal("60"),
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("60")));
        assertEquals("D", MedGradingTypeUtil.calcGrade(new BigDecimal("59.99"),
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("60")));
        // 综合标准分数线 85/75/60：85 甲、75 乙
        assertEquals("A", MedGradingTypeUtil.calcGrade(new BigDecimal("85"),
                new BigDecimal("85"), new BigDecimal("75"), new BigDecimal("60")));
        assertEquals("B", MedGradingTypeUtil.calcGrade(new BigDecimal("82"),
                new BigDecimal("85"), new BigDecimal("75"), new BigDecimal("60")));
        // 端到端：新建 90 分评定自动为甲级
        GradingAssessment a = newAssessment(7L, "放射科", 1L, "2026-12",
                new BigDecimal("90"), "周建国");
        try {
            GradingAssessment created = assessmentService.create(a);
            assertEquals("A", created.getGrade());
            assertEquals(Integer.valueOf(0), created.getStatus());
        } finally {
            if (a.getId() != null) {
                assessmentService.removeById(a.getId());
            }
        }
    }

    @Test
    void standardValidation() {
        // 分数线顺序错误拒绝（丙级线不小于乙级线）
        GradingStandard bad = newStandard("STD-BAD1", "分数线错误标准", "QUALITY",
                new BigDecimal("90"), new BigDecimal("90"), new BigDecimal("60"));
        assertThrows(IllegalArgumentException.class, () -> standardService.create(bad));
        // 分数线超 100 拒绝
        GradingStandard over = newStandard("STD-BAD2", "分数线超限标准", "QUALITY",
                new BigDecimal("101"), new BigDecimal("80"), new BigDecimal("60"));
        assertThrows(IllegalArgumentException.class, () -> standardService.create(over));
        // 编码重复拒绝
        GradingStandard dup = newStandard("STD-Q001", "重复编码标准", "QUALITY",
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("60"));
        assertThrows(IllegalArgumentException.class, () -> standardService.create(dup));
        // 分级范围非法拒绝
        GradingStandard scope = newStandard("STD-BAD3", "范围非法标准", "UNKNOWN",
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("60"));
        assertThrows(IllegalArgumentException.class, () -> standardService.create(scope));
        assertEquals(3L, standardService.count());
    }

    @Test
    void standardGuards() {
        // 有评定的标准不能删除
        assertThrows(IllegalArgumentException.class, () -> standardService.removeById(1L));
        // 有待审批评定的标准不能停用（标准 1 有 1 条待审批）
        assertThrows(IllegalArgumentException.class, () -> standardService.disable(1L));
        // 无评定的新标准可停用、再启用、可删除
        GradingStandard fresh = newStandard("STD-TMP1", "临时测试标准", "SERVICE",
                new BigDecimal("90"), new BigDecimal("80"), new BigDecimal("60"));
        GradingStandard created = standardService.create(fresh);
        try {
            // 新建评定时用停用的标准拒绝
            GradingStandard disabled = standardService.disable(created.getId());
            assertEquals(Integer.valueOf(0), disabled.getStatus());
            GradingAssessment a = newAssessment(7L, "放射科", created.getId(), "2026-12",
                    new BigDecimal("88"), "周建国");
            assertThrows(IllegalArgumentException.class, () -> assessmentService.create(a));
            GradingStandard enabled = standardService.enable(created.getId());
            assertEquals(Integer.valueOf(1), enabled.getStatus());
        } finally {
            standardService.removeById(created.getId());
        }
        assertEquals(3L, standardService.count());
    }

    @Test
    void assessmentValidation() {
        // 评分超 100 / 小于 0 拒绝
        GradingAssessment over = newAssessment(7L, "放射科", 1L, "2026-12",
                new BigDecimal("101"), "周建国");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.create(over));
        GradingAssessment under = newAssessment(7L, "放射科", 1L, "2026-12",
                new BigDecimal("-1"), "周建国");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.create(under));
        // 周期格式错误拒绝
        GradingAssessment period = newAssessment(7L, "放射科", 1L, "2026-13",
                new BigDecimal("88"), "周建国");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.create(period));
        // 同一科室 + 同一标准 + 同一周期重复拒绝（种子：中医内科 + 标准 1 + 2026-11）
        GradingAssessment dup = newAssessment(1L, "中医内科", 1L, "2026-11",
                new BigDecimal("95"), "陈国强");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.create(dup));
        // 标准不存在拒绝
        GradingAssessment noStd = newAssessment(7L, "放射科", 999L, "2026-12",
                new BigDecimal("88"), "周建国");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.create(noStd));
        assertEquals(6L, assessmentService.count());
    }

    @Test
    void approveRejectFlow() {
        GradingAssessment a = newAssessment(7L, "放射科", 1L, "2026-12",
                new BigDecimal("95"), "周建国");
        GradingAssessment created = assessmentService.create(a);
        try {
            // 无审批人拒绝
            assertThrows(IllegalArgumentException.class, () ->
                    assessmentService.approve(created.getId(), "  ", "同意"));
            // 审批通过
            GradingAssessment approved = assessmentService.approve(created.getId(), "吴文斌", "同意，甲级");
            assertEquals(Integer.valueOf(1), approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertEquals(LocalDate.now(), approved.getApproveDate());
            // 已通过的不能再审批、不能修改、不能走 service 删除
            assertThrows(IllegalArgumentException.class, () ->
                    assessmentService.approve(created.getId(), "吴文斌", "再次审批"));
            GradingAssessment edit = new GradingAssessment();
            edit.setId(created.getId());
            edit.setStandardId(1L);
            edit.setDeptId(7L);
            edit.setDeptName("放射科");
            edit.setPeriod("2026-12");
            edit.setScore(new BigDecimal("96"));
            assertThrows(IllegalArgumentException.class, () -> assessmentService.update(edit));
            assertThrows(IllegalArgumentException.class, () ->
                    assessmentService.removeById(created.getId()));
        } finally {
            // 清理：已通过的评定走 mapper 直接删除（绕过业务守卫，仅测试清理用）
            assessmentMapper.deleteById(created.getId());
        }
        // 驳回流程：新建待审批 → 驳回 → 可删除
        GradingAssessment b = newAssessment(7L, "放射科", 1L, "2026-12",
                new BigDecimal("70"), "周建国");
        GradingAssessment createdB = assessmentService.create(b);
        GradingAssessment rejected = assessmentService.reject(createdB.getId(), "吴文斌", "评分依据不足");
        assertEquals(Integer.valueOf(2), rejected.getStatus());
        assertEquals("评分依据不足", rejected.getApproveOpinion());
        assertTrue(assessmentService.removeById(createdB.getId()));
        assertEquals(6L, assessmentService.count());
    }

    @Test
    void updateRecomputesGrade() {
        GradingAssessment a = newAssessment(7L, "放射科", 1L, "2026-12",
                new BigDecimal("88"), "周建国");
        GradingAssessment created = assessmentService.create(a);
        try {
            assertEquals("B", created.getGrade());
            GradingAssessment edit = new GradingAssessment();
            edit.setId(created.getId());
            edit.setStandardId(1L);
            edit.setDeptId(7L);
            edit.setDeptName("放射科");
            edit.setPeriod("2026-12");
            edit.setScore(new BigDecimal("92"));
            edit.setAssessor("周建国");
            GradingAssessment updated = assessmentService.update(edit);
            assertEquals("A", updated.getGrade());
            assertEquals(Integer.valueOf(0), updated.getStatus());
        } finally {
            assessmentService.removeById(created.getId());
        }
    }

    @Test
    void statsAndByDept() {
        Map<String, Object> stats = assessmentService.stats();
        assertEquals(6L, ((Number) stats.get("total")).longValue());
        assertEquals(2L, ((Number) stats.get("pending")).longValue());
        assertEquals(3L, ((Number) stats.get("approved")).longValue());
        assertEquals(1L, ((Number) stats.get("rejected")).longValue());
        @SuppressWarnings("unchecked")
        Map<String, Long> byGrade = (Map<String, Long>) stats.get("byGrade");
        assertEquals(1L, byGrade.get("A"));
        assertEquals(3L, byGrade.get("B"));
        assertEquals(1L, byGrade.get("C"));
        assertEquals(1L, byGrade.get("D"));
        assertEquals(new BigDecimal("80.67"), stats.get("avgScore"));
        assertEquals(new BigDecimal("83.33"), stats.get("passRate"));
        assertEquals(3L, ((Number) stats.get("standardsUsed")).longValue());
        assertEquals(6L, ((Number) stats.get("deptsAssessed")).longValue());

        // 按科室汇总：当前分级取最新已通过评定
        List<Map<String, Object>> byDept = assessmentService.statsByDept();
        assertEquals(6, byDept.size());
        Map<String, Object> zy = byDept.stream()
                .filter(r -> "中医内科".equals(r.get("deptName"))).findFirst().orElseThrow();
        assertEquals("A", zy.get("currentGrade"));
        assertEquals("甲级", zy.get("currentGradeText"));
        assertEquals("2026-11", zy.get("latestPeriod"));
        Map<String, Object> gs = byDept.stream()
                .filter(r -> "骨伤科".equals(r.get("deptName"))).findFirst().orElseThrow();
        assertNull(gs.get("currentGrade"));
        assertEquals("未评级", gs.get("currentGradeText"));
        Map<String, Object> ek = byDept.stream()
                .filter(r -> "儿科".equals(r.get("deptName"))).findFirst().orElseThrow();
        assertNull(ek.get("currentGrade"));

        // 按月趋势：只有 2026-11 一个月
        List<Map<String, Object>> byMonth = assessmentService.statsByMonth();
        assertEquals(1, byMonth.size());
        assertEquals("2026-11", byMonth.get(0).get("period"));
        assertEquals(6L, ((Number) byMonth.get(0).get("total")).longValue());
        assertEquals(3L, ((Number) byMonth.get(0).get("approved")).longValue());
        assertEquals(new BigDecimal("80.67"), byMonth.get(0).get("avgScore"));

        // 分页查询：按等级甲级只 1 条
        GradingAssessmentQuery q = new GradingAssessmentQuery();
        q.setGrade("A");
        assertEquals(1L, assessmentService.pageQuery(q).getTotal());
        q.setGrade(null);
        q.setStatus(0);
        assertEquals(2L, assessmentService.pageQuery(q).getTotal());
    }

    @Test
    void excelStandardRoundTrip() throws Exception {
        // 导出：有效 xlsx，读回 3 个种子标准
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        standardService.exportExcel(new GradingStandardQuery(), out);
        assertTrue(out.size() > 0);
        List<GradingStandard> exported = GradingStandardExcelUtil.read(
                new ByteArrayInputStream(out.toByteArray()));
        assertEquals(3, exported.size());
        assertTrue(exported.stream().anyMatch(s -> "STD-Q001".equals(s.getStandardCode())));

        // 再次导入同一份导出：编码重复全部跳过
        Map<String, Object> dup = standardService.importExcel(
                new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, ((Number) dup.get("created")).intValue());
        assertEquals(3, ((Number) dup.get("skipped")).intValue());

        // 导入 1 个新标准 + 1 个非法行（分数线顺序错）+ 1 个重复行
        ByteArrayOutputStream importBytes = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("分级标准");
            Row header = sheet.createRow(0);
            String[] headers = {"标准编码", "标准名称", "分级范围", "甲级线", "乙级线", "丙级线",
                    "有效期起", "有效期止", "状态", "备注"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("STD-TMP2");
            r1.createCell(1).setCellValue("导入测试标准");
            r1.createCell(2).setCellValue("医疗质量分级");
            r1.createCell(3).setCellValue(90);
            r1.createCell(4).setCellValue(80);
            r1.createCell(5).setCellValue(60);
            r1.createCell(8).setCellValue("启用");
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("STD-BAD9");
            r2.createCell(1).setCellValue("非法分数线标准");
            r2.createCell(2).setCellValue("医疗质量分级");
            r2.createCell(3).setCellValue(80);
            r2.createCell(4).setCellValue(90);
            r2.createCell(5).setCellValue(60);
            r2.createCell(8).setCellValue("启用");
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("STD-Q001");
            r3.createCell(1).setCellValue("医疗质量分级标准");
            r3.createCell(2).setCellValue("医疗质量分级");
            r3.createCell(3).setCellValue(90);
            r3.createCell(4).setCellValue(80);
            r3.createCell(5).setCellValue(60);
            r3.createCell(8).setCellValue("启用");
            wb.write(importBytes);
        }
        GradingStandard imported = null;
        try {
            Map<String, Object> result = standardService.importExcel(
                    new ByteArrayInputStream(importBytes.toByteArray()));
            assertEquals(1, ((Number) result.get("created")).intValue());
            assertEquals(2, ((Number) result.get("skipped")).intValue());
            assertEquals(3, ((Number) result.get("total")).intValue());
            imported = standardService.list().stream()
                    .filter(s -> "STD-TMP2".equals(s.getStandardCode())).findFirst().orElse(null);
            assertNotNull(imported);
            assertEquals("QUALITY", imported.getScope());
            // 重复导入同一份：新标准已存在，全部跳过
            Map<String, Object> again = standardService.importExcel(
                    new ByteArrayInputStream(importBytes.toByteArray()));
            assertEquals(0, ((Number) again.get("created")).intValue());
            assertEquals(3, ((Number) again.get("skipped")).intValue());
        } finally {
            if (imported != null) {
                standardService.removeById(imported.getId());
            }
        }
    }

    @Test
    void excelAssessmentRoundTrip() throws Exception {
        // 导出：有效 xlsx，读回 6 条种子评定
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assessmentService.exportExcel(new GradingAssessmentQuery(), out);
        assertTrue(out.size() > 0);
        List<GradingAssessment> exported = GradingAssessmentExcelUtil.read(
                new ByteArrayInputStream(out.toByteArray()));
        assertEquals(6, exported.size());
        assertTrue(exported.stream().anyMatch(a -> "中医内科".equals(a.getDeptName())
                && "STD-Q001".equals(a.getStandardCode())));

        // 再次导入同一份导出：同一科室 + 同一标准 + 同一周期全部跳过
        Map<String, Object> dup = assessmentService.importExcel(
                new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, ((Number) dup.get("created")).intValue());
        assertEquals(6, ((Number) dup.get("skipped")).intValue());

        // 导入 1 条新评定（放射科 + STD-Q001 + 2026-12 + 88 分 → 乙级待审批）
        ByteArrayOutputStream importBytes = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("分级评定");
            Row header = sheet.createRow(0);
            String[] headers = {"科室名称", "标准编码", "分级标准", "评定周期", "评分", "等级",
                    "状态", "评定人", "评定日期", "备注"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("放射科");
            r1.createCell(1).setCellValue("STD-Q001");
            r1.createCell(3).setCellValue("2026-12");
            r1.createCell(4).setCellValue(88);
            r1.createCell(7).setCellValue("周建国");
            r1.createCell(8).setCellValue("2026-11-25");
            // 1 条非法行：标准编码不存在
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("放射科");
            r2.createCell(1).setCellValue("STD-NOT-EXIST");
            r2.createCell(3).setCellValue("2026-12");
            r2.createCell(4).setCellValue(88);
            wb.write(importBytes);
        }
        GradingAssessment imported = null;
        try {
            Map<String, Object> result = assessmentService.importExcel(
                    new ByteArrayInputStream(importBytes.toByteArray()));
            assertEquals(1, ((Number) result.get("created")).intValue());
            assertEquals(1, ((Number) result.get("skipped")).intValue());
            assertEquals(2, ((Number) result.get("total")).intValue());
            imported = assessmentService.list().stream()
                    .filter(a -> "放射科".equals(a.getDeptName())
                            && "2026-12".equals(a.getPeriod()))
                    .findFirst().orElse(null);
            assertNotNull(imported);
            assertEquals("B", imported.getGrade());
            assertEquals(Integer.valueOf(0), imported.getStatus());
            assertEquals(Long.valueOf(7L), imported.getDeptId());
            // 重复导入同一份：新评定已存在，全部跳过
            Map<String, Object> again = assessmentService.importExcel(
                    new ByteArrayInputStream(importBytes.toByteArray()));
            assertEquals(0, ((Number) again.get("created")).intValue());
            assertEquals(2, ((Number) again.get("skipped")).intValue());
        } finally {
            if (imported != null) {
                assessmentService.removeById(imported.getId());
            }
        }
    }

    @Test
    void importTemplateDownload() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        standardService.writeImportTemplate(out);
        assertTrue(out.size() > 0);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Row header = wb.getSheetAt(0).getRow(0);
            assertEquals("标准编码", header.getCell(0).getStringCellValue());
            assertEquals("甲级线", header.getCell(3).getStringCellValue());
        }
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        assessmentService.writeImportTemplate(out2);
        assertTrue(out2.size() > 0);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out2.toByteArray()))) {
            Row header = wb.getSheetAt(0).getRow(0);
            assertEquals("科室名称", header.getCell(0).getStringCellValue());
            assertEquals("标准编码", header.getCell(1).getStringCellValue());
        }
    }

    // ---------- helpers ----------

    private GradingStandard newStandard(String code, String name, String scope,
                                        BigDecimal excellent, BigDecimal good, BigDecimal pass) {
        GradingStandard s = new GradingStandard();
        s.setStandardCode(code);
        s.setStandardName(name);
        s.setScope(scope);
        s.setExcellentMin(excellent);
        s.setGoodMin(good);
        s.setPassMin(pass);
        s.setStatus(1);
        return s;
    }

    private GradingAssessment newAssessment(Long deptId, String deptName, Long standardId,
                                            String period, BigDecimal score, String assessor) {
        GradingAssessment a = new GradingAssessment();
        a.setDeptId(deptId);
        a.setDeptName(deptName);
        a.setStandardId(standardId);
        a.setPeriod(period);
        a.setScore(score);
        a.setAssessor(assessor);
        a.setAssessDate(LocalDate.of(2026, 11, 25));
        return a;
    }
}
