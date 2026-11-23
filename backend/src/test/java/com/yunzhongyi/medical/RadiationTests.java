package com.yunzhongyi.medical;

import com.yunzhongyi.medical.radiation.dto.RadiationHealthQuery;
import com.yunzhongyi.medical.radiation.dto.RadiationPersonnelQuery;
import com.yunzhongyi.medical.radiation.dto.RadiationQualificationQuery;
import com.yunzhongyi.medical.radiation.dto.RadiationTrainingQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationHealth;
import com.yunzhongyi.medical.radiation.entity.RadiationPersonnel;
import com.yunzhongyi.medical.radiation.entity.RadiationQualification;
import com.yunzhongyi.medical.radiation.entity.RadiationTraining;
import com.yunzhongyi.medical.radiation.excel.RadiationPersonnelExcelUtil;
import com.yunzhongyi.medical.radiation.service.RadiationHealthService;
import com.yunzhongyi.medical.radiation.service.RadiationPersonnelService;
import com.yunzhongyi.medical.radiation.service.RadiationQualificationService;
import com.yunzhongyi.medical.radiation.service.RadiationTrainingService;
import com.yunzhongyi.medical.radiation.service.RadiationTypeUtil;
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
 * 放射人员管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 名放射人员（放射诊断/介入放射/核医学/放射治疗 4 种放射类别，
 * 在岗 4 / 停岗 1 / 离岗 1）、6 条资质（待审核 2 / 已通过 3 / 已驳回 1，
 * 90 天内到期 2 条、已过期 1 条）、5 条培训（合格 4 / 不合格 1）、
 * 6 条体检（平均剂量 8.75 mSv，超标 1 条，复查 1 条，体检逾期 1 人）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class RadiationTests {

    @Autowired
    private RadiationPersonnelService personnelService;

    @Autowired
    private RadiationQualificationService qualificationService;

    @Autowired
    private RadiationTrainingService trainingService;

    @Autowired
    private RadiationHealthService healthService;

    @Test
    void contextLoads() {
        assertNotNull(personnelService);
        assertNotNull(qualificationService);
        assertNotNull(trainingService);
        assertNotNull(healthService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(personnelService.count() >= 6);
        assertTrue(qualificationService.count() >= 6);
        assertTrue(trainingService.count() >= 5);
        assertTrue(healthService.count() >= 6);

        // 种子档案 1：周建国，放射科主任医师，放射诊断，关联医师档案 id=7，在岗
        RadiationPersonnel p1 = personnelService.getById(1L);
        assertEquals("周建国", p1.getName());
        assertEquals(7L, p1.getPhysicianId());
        assertEquals("放射科", p1.getDeptName());
        assertEquals("DIAGNOSIS", p1.getWorkType());
        assertEquals("FS-2018-0001", p1.getLicenseNo());
        assertEquals(1, p1.getStatus());

        // 种子档案 5：赵雪，骨伤科介入放射，停岗（剂量超标复查中）
        RadiationPersonnel p5 = personnelService.getById(5L);
        assertEquals("赵雪", p5.getName());
        assertEquals("骨伤科", p5.getDeptName());
        assertEquals("INTERVENTION", p5.getWorkType());
        assertEquals(2, p5.getStatus());

        // 种子档案 6：李建华，放射治疗，离岗
        RadiationPersonnel p6 = personnelService.getById(6L);
        assertEquals("李建华", p6.getName());
        assertEquals("RADIOTHERAPY", p6.getWorkType());
        assertEquals(3, p6.getStatus());

        // 种子资质 2：陈志强大型设备上岗证（DSA），已通过，2027-01-31 到期（90 天内）
        RadiationQualification q2 = qualificationService.getById(2L);
        assertEquals("陈志强", q2.getPersonnelName());
        assertEquals("EQUIPMENT", q2.getCertType());
        assertEquals(LocalDate.of(2027, 1, 31), q2.getExpiryDate());
        assertEquals(1, q2.getStatus());

        // 种子资质 3：刘海燕防护合格证，已通过但 2026-10-31 已过期
        RadiationQualification q3 = qualificationService.getById(3L);
        assertEquals("PROTECTION", q3.getCertType());
        assertEquals(LocalDate.of(2026, 10, 31), q3.getExpiryDate());
        assertEquals(1, q3.getStatus());

        // 种子资质 4：王小明执业医师证，待审核
        RadiationQualification q4 = qualificationService.getById(4L);
        assertEquals("王小明", q4.getPersonnelName());
        assertEquals(0, q4.getStatus());

        // 种子资质 5：赵雪工作人员证，已驳回
        RadiationQualification q5 = qualificationService.getById(5L);
        assertEquals(2, q5.getStatus());
        assertTrue(q5.getApprovalHistory().contains("驳回"));

        // 种子培训 5：赵雪复训不合格
        RadiationTraining t5 = trainingService.getById(5L);
        assertEquals("赵雪", t5.getPersonnelName());
        assertEquals("RETRAINING", t5.getTrainingType());
        assertEquals("FAIL", t5.getResult());

        // 种子体检 5：赵雪在岗体检，剂量 21.40 mSv 超标，结论复查
        RadiationHealth h5 = healthService.getById(5L);
        assertEquals("赵雪", h5.getPersonnelName());
        assertEquals("ON_JOB", h5.getExamType());
        assertEquals(new BigDecimal("21.40"), h5.getDose());
        assertEquals("REVIEW", h5.getResult());
        assertTrue(RadiationTypeUtil.isDoseOverLimit(h5.getDose()));
    }

    @Test
    void pageQueryWithFilters() {
        RadiationPersonnelQuery q = new RadiationPersonnelQuery();
        q.setWorkType("DIAGNOSIS");
        assertEquals(2, personnelService.pageQuery(q).getTotal()); // 周建国、王小明

        RadiationPersonnelQuery q2 = new RadiationPersonnelQuery();
        q2.setWorkType("INTERVENTION");
        assertEquals(2, personnelService.pageQuery(q2).getTotal()); // 陈志强、赵雪

        RadiationPersonnelQuery q3 = new RadiationPersonnelQuery();
        q3.setStatus(1);
        assertEquals(4, personnelService.pageQuery(q3).getTotal()); // 在岗 4 人

        RadiationPersonnelQuery q4 = new RadiationPersonnelQuery();
        q4.setDeptId(7L);
        assertEquals(5, personnelService.pageQuery(q4).getTotal()); // 放射科 5 人

        RadiationPersonnelQuery q5 = new RadiationPersonnelQuery();
        q5.setKeyword("FS-2021-0005");
        assertEquals(1, personnelService.pageQuery(q5).getTotal()); // 赵雪证号

        RadiationPersonnelQuery q6 = new RadiationPersonnelQuery();
        q6.setPhysicianId(7L);
        assertEquals(1, personnelService.pageQuery(q6).getTotal()); // 周建国

        // 资质：待审核 2 条；90 天内到期（已通过）2 条
        RadiationQualificationQuery qq = new RadiationQualificationQuery();
        qq.setStatus(0);
        assertEquals(2, qualificationService.pageQuery(qq).getTotal());
        RadiationQualificationQuery qq2 = new RadiationQualificationQuery();
        qq2.setExpiringSoon(true);
        assertEquals(2, qualificationService.pageQuery(qq2).getTotal()); // 陈志强、刘海燕
        RadiationQualificationQuery qq3 = new RadiationQualificationQuery();
        qq3.setPersonnelId(1L);
        assertEquals(1, qualificationService.pageQuery(qq3).getTotal()); // 周建国 1 条资质

        // 培训：复训 2 条；不合格 1 条
        RadiationTrainingQuery tq = new RadiationTrainingQuery();
        tq.setTrainingType("RETRAINING");
        assertEquals(2, trainingService.pageQuery(tq).getTotal());
        RadiationTrainingQuery tq2 = new RadiationTrainingQuery();
        tq2.setResult("FAIL");
        assertEquals(1, trainingService.pageQuery(tq2).getTotal()); // 赵雪

        // 体检：剂量超标 1 条；复查结论 1 条
        RadiationHealthQuery hq = new RadiationHealthQuery();
        hq.setOverLimit(true);
        assertEquals(1, healthService.pageQuery(hq).getTotal()); // 赵雪
        RadiationHealthQuery hq2 = new RadiationHealthQuery();
        hq2.setResult("REVIEW");
        assertEquals(1, healthService.pageQuery(hq2).getTotal());
    }

    @Test
    void saveValidationAndDefaults() {
        // 放射类别非法，拒绝保存
        RadiationPersonnel badType = newPersonnel("测试人员");
        badType.setWorkType("XXX");
        assertThrows(IllegalArgumentException.class, () -> personnelService.save(badType));

        // 在岗状态非法，拒绝保存
        RadiationPersonnel badStatus = newPersonnel("测试人员");
        badStatus.setStatus(9);
        assertThrows(IllegalArgumentException.class, () -> personnelService.save(badStatus));

        // 许可证有效期早于从事放射工作日期，拒绝保存
        RadiationPersonnel badDate = newPersonnel("测试人员");
        badDate.setPermitExpiry(LocalDate.of(2010, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> personnelService.save(badDate));

        // 合法建档：默认在岗
        RadiationPersonnel p = newPersonnel("测试人员");
        assertTrue(personnelService.save(p));
        try {
            assertNotNull(p.getId());
            assertEquals(RadiationTypeUtil.STATUS_ACTIVE, p.getStatus());

            // 同一姓名同一科室已有档案，拒绝重复建档
            RadiationPersonnel dup = newPersonnel("测试人员");
            assertThrows(IllegalArgumentException.class, () -> personnelService.save(dup));
        } finally {
            personnelService.removeById(p.getId());
        }
    }

    @Test
    void qualificationApprovalFlow() {
        // 王小明执业医师证（种子 id=4，待审核）：通过后状态 0→1，审核记录留痕
        RadiationQualification approved = qualificationService.approve(4L, "吴文斌", "同意");
        try {
            assertEquals(1, approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertEquals("同意", approved.getApprovalOpinion());
            assertTrue(approved.getApprovalHistory().contains("医务科"));
            assertTrue(approved.getApprovalHistory().contains("通过"));

            // 已结束的资质不能再通过、再驳回、再修改
            assertThrows(IllegalStateException.class,
                    () -> qualificationService.approve(4L, "吴文斌", "再次通过"));
            assertThrows(IllegalStateException.class,
                    () -> qualificationService.reject(4L, "吴文斌", "改主意了"));
            approved.setCertName("改名后的证书");
            assertThrows(IllegalStateException.class, () -> qualificationService.updateById(approved));
        } finally {
            // 恢复为待审核，避免污染其他测试
            RadiationQualification restore = new RadiationQualification();
            restore.setId(4L);
            restore.setStatus(0);
            restore.setApprover(null);
            restore.setApprovalOpinion(null);
            restore.setApprovalHistory(null);
            qualificationService.getBaseMapper().updateById(restore);
        }

        // 驳回流程：李建华大型设备上岗证（种子 id=6，待审核）驳回后状态变为已驳回
        RadiationQualification rejected = qualificationService.reject(6L, "吴文斌", "材料不全");
        try {
            assertEquals(2, rejected.getStatus());
            assertTrue(rejected.getApprovalHistory().contains("驳回"));
            assertThrows(IllegalStateException.class,
                    () -> qualificationService.approve(6L, "吴文斌", "改主意了"));
        } finally {
            RadiationQualification restore = new RadiationQualification();
            restore.setId(6L);
            restore.setStatus(0);
            restore.setApprover(null);
            restore.setApprovalOpinion(null);
            restore.setApprovalHistory(null);
            qualificationService.getBaseMapper().updateById(restore);
        }
    }

    @Test
    void qualificationValidation() {
        // 证书类型非法，拒绝保存
        RadiationQualification badType = newQualification(1L, "周建国");
        badType.setCertType("XXX");
        assertThrows(IllegalArgumentException.class, () -> qualificationService.save(badType));

        // 有效期早于发证日期，拒绝保存
        RadiationQualification badDate = newQualification(1L, "周建国");
        badDate.setExpiryDate(LocalDate.of(2020, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> qualificationService.save(badDate));

        // 同一人员同一证书同一编号已有待审核记录，拒绝重复登记
        RadiationQualification dup = newQualification(4L, "王小明");
        dup.setCertType("PRACTICE");
        dup.setCertNo("110440301000013");
        assertThrows(IllegalArgumentException.class, () -> qualificationService.save(dup));

        // 待审核的资质可以修改
        RadiationQualification q = newQualification(1L, "周建国");
        q.setCertNo("FS-TEST-001");
        assertTrue(qualificationService.save(q));
        try {
            assertEquals(0, q.getStatus());
            q.setCertName("修改后的证书名称");
            assertTrue(qualificationService.updateById(q));
            assertEquals("修改后的证书名称", qualificationService.getById(q.getId()).getCertName());
        } finally {
            qualificationService.removeById(q.getId());
        }
    }

    @Test
    void trainingValidationAndDuplicate() {
        // 培训类型非法，拒绝保存
        RadiationTraining badType = newTraining(1L, "周建国");
        badType.setTrainingType("XXX");
        assertThrows(IllegalArgumentException.class, () -> trainingService.save(badType));

        // 培训日期为空，拒绝保存
        RadiationTraining badDate = newTraining(1L, "周建国");
        badDate.setTrainingDate(null);
        assertThrows(IllegalArgumentException.class, () -> trainingService.save(badDate));

        // 学时为负，拒绝保存
        RadiationTraining badHours = newTraining(1L, "周建国");
        badHours.setHours(-1);
        assertThrows(IllegalArgumentException.class, () -> trainingService.save(badHours));

        // 培训结果非法，拒绝保存
        RadiationTraining badResult = newTraining(1L, "周建国");
        badResult.setResult("XXX");
        assertThrows(IllegalArgumentException.class, () -> trainingService.save(badResult));

        // 合法登记：结果默认合格
        RadiationTraining t = newTraining(1L, "周建国");
        t.setResult(null);
        assertTrue(trainingService.save(t));
        try {
            assertEquals("PASS", t.getResult());

            // 同一人员同一培训同一日期已有记录，拒绝重复登记
            RadiationTraining dup = newTraining(1L, "周建国");
            assertThrows(IllegalArgumentException.class, () -> trainingService.save(dup));
        } finally {
            trainingService.removeById(t.getId());
        }
    }

    @Test
    void healthValidationAndDose() {
        // 体检类型非法，拒绝保存
        RadiationHealth badType = newHealth(1L, "周建国");
        badType.setExamType("XXX");
        assertThrows(IllegalArgumentException.class, () -> healthService.save(badType));

        // 体检日期为空，拒绝保存
        RadiationHealth badDate = newHealth(1L, "周建国");
        badDate.setExamDate(null);
        assertThrows(IllegalArgumentException.class, () -> healthService.save(badDate));

        // 剂量为负，拒绝保存
        RadiationHealth badDose = newHealth(1L, "周建国");
        badDose.setDose(new BigDecimal("-1.00"));
        assertThrows(IllegalArgumentException.class, () -> healthService.save(badDose));

        // 下次体检日期早于本次体检日期，拒绝保存
        RadiationHealth badNext = newHealth(1L, "周建国");
        badNext.setNextExamDate(LocalDate.of(2026, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> healthService.save(badNext));

        // 合法登记：结论默认合格
        RadiationHealth h = newHealth(1L, "周建国");
        h.setResult(null);
        assertTrue(healthService.save(h));
        try {
            assertEquals("FIT", h.getResult());

            // 同一人员同一体检类型同一日期已有记录，拒绝重复登记
            RadiationHealth dup = newHealth(1L, "周建国");
            assertThrows(IllegalArgumentException.class, () -> healthService.save(dup));
        } finally {
            healthService.removeById(h.getId());
        }

        // 剂量限值判定：20 mSv 不超标，20.01 mSv 超标
        assertFalse(RadiationTypeUtil.isDoseOverLimit(new BigDecimal("20.00")));
        assertTrue(RadiationTypeUtil.isDoseOverLimit(new BigDecimal("20.01")));
        assertFalse(RadiationTypeUtil.isDoseOverLimit(null));
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = personnelService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(4L, s.get("active")); // 在岗：周建国、陈志强、刘海燕、王小明
        assertEquals(1L, s.get("suspended")); // 停岗：赵雪
        assertEquals(1L, s.get("left")); // 离岗：李建华
        Map<?, ?> byWorkType = (Map<?, ?>) s.get("byWorkType");
        assertEquals(2L, byWorkType.get("DIAGNOSIS"));
        assertEquals(2L, byWorkType.get("INTERVENTION"));
        assertEquals(1L, byWorkType.get("NUCLEAR_MED"));
        assertEquals(1L, byWorkType.get("RADIOTHERAPY"));

        // 按科室汇总：放射科 5、骨伤科 1
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        assertEquals(2, byDept.size());
        Map<String, Object> fs = byDept.stream()
                .filter(d -> "放射科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(5L, fs.get("count"));
        assertEquals(4L, fs.get("activeCount"));
        Map<String, Object> gs = byDept.stream()
                .filter(d -> "骨伤科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(1L, gs.get("count"));
        assertEquals(1L, gs.get("suspendedCount"));

        // 资质：总数 6、待审核 2、已通过 3、已驳回 1、90 天内到期 2、已过期 1
        assertEquals(6L, s.get("qualTotal"));
        assertEquals(2L, s.get("qualPending"));
        assertEquals(3L, s.get("qualApproved"));
        assertEquals(1L, s.get("qualRejected"));
        assertEquals(2L, s.get("qualExpiringSoon")); // 陈志强、刘海燕
        assertEquals(1L, s.get("qualExpired")); // 刘海燕

        // 培训：总数 5、合格 4、不合格 1
        assertEquals(5L, s.get("trainingTotal"));
        assertEquals(4L, s.get("trainingPassed"));
        assertEquals(1L, s.get("trainingFailed"));

        // 健康：总数 6、平均剂量 8.75、超标 1、复查 1、不宜从事放射工作 0、体检逾期 1 人（王小明）
        assertEquals(6L, s.get("healthTotal"));
        assertEquals(new BigDecimal("8.75"), s.get("avgDose"));
        assertEquals(1L, s.get("overLimitCount")); // 赵雪 21.40 mSv
        assertEquals(1L, s.get("reviewCount"));
        assertEquals(0L, s.get("unfitCount"));
        assertEquals(1L, s.get("overdueExamCount")); // 王小明最近体检 2023-06-25，已超 365 天
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> personnelService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> qualificationService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> trainingService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> healthService.detail(999999L));
    }

    @Test
    void excelRoundTrip() {
        RadiationPersonnelQuery q = new RadiationPersonnelQuery();
        q.setPage(1);
        q.setSize(100);
        List<RadiationPersonnel> all = personnelService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RadiationPersonnelExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<RadiationPersonnel> back = RadiationPersonnelExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（姓名、科室、放射类别、证号、许可证有效期、从事放射工作日期、在岗状态）
        RadiationPersonnel first = all.stream()
                .filter(p -> "赵雪".equals(p.getName()))
                .findFirst().orElseThrow();
        RadiationPersonnel firstBack = back.stream()
                .filter(p -> "赵雪".equals(p.getName()))
                .findFirst().orElseThrow();
        assertEquals(first.getDeptName(), firstBack.getDeptName());
        assertEquals(first.getWorkType(), firstBack.getWorkType());
        assertEquals(first.getLicenseNo(), firstBack.getLicenseNo());
        assertEquals(first.getPermitExpiry(), firstBack.getPermitExpiry());
        assertEquals(first.getEntryDate(), firstBack.getEntryDate());
        assertEquals(first.getStatus(), firstBack.getStatus());
        assertEquals(first.getIdCard(), firstBack.getIdCard());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条档案 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （档案用逻辑删除，删后重导按“同一姓名同一科室重复”规则会跳过种子行，只补回被删的那条）
        RadiationPersonnel fresh = newPersonnel("导入测试人员");
        assertTrue(personnelService.save(fresh));

        RadiationPersonnelQuery q = new RadiationPersonnelQuery();
        q.setPage(1);
        q.setSize(100);
        List<RadiationPersonnel> all = personnelService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RadiationPersonnelExcelUtil.write(all, out);

        assertTrue(personnelService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, personnelService.listByQuery(q).size());

        int imported = personnelService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因重复而跳过，只补回导入测试人员这一条
        assertEquals(all.size(), personnelService.listByQuery(q).size());
        RadiationPersonnel restored = personnelService.listByQuery(q).stream()
                .filter(p -> "导入测试人员".equals(p.getName()))
                .findFirst().orElseThrow();
        assertEquals("DIAGNOSIS", restored.getWorkType());
        assertEquals("放射科", restored.getDeptName());
        assertEquals(1, restored.getStatus()); // 导入的档案默认在岗
        assertTrue(personnelService.removeById(restored.getId()));
        assertEquals(all.size() - 1, personnelService.listByQuery(q).size());
    }

    private RadiationPersonnel newPersonnel(String name) {
        RadiationPersonnel p = new RadiationPersonnel();
        p.setName(name);
        p.setGender("M");
        p.setDeptId(7L);
        p.setDeptName("放射科");
        p.setTitle("主治医师");
        p.setWorkType("DIAGNOSIS");
        p.setLicenseNo("FS-TEST-001");
        p.setPermitNo("粤放诊许字〔2026〕099号");
        p.setPermitExpiry(LocalDate.of(2030, 12, 31));
        p.setEntryDate(LocalDate.of(2020, 1, 1));
        return p;
    }

    private RadiationQualification newQualification(Long personnelId, String personnelName) {
        RadiationQualification q = new RadiationQualification();
        q.setPersonnelId(personnelId);
        q.setPersonnelName(personnelName);
        q.setCertType("WORKER_CERT");
        q.setCertName("放射工作人员证");
        q.setCertNo("FS-TEST-002");
        q.setIssueDate(LocalDate.of(2026, 1, 1));
        q.setExpiryDate(LocalDate.of(2031, 1, 1));
        return q;
    }

    private RadiationTraining newTraining(Long personnelId, String personnelName) {
        RadiationTraining t = new RadiationTraining();
        t.setPersonnelId(personnelId);
        t.setPersonnelName(personnelName);
        t.setTrainingName("测试培训");
        t.setTrainingType("RETRAINING");
        t.setOrg("市疾控中心");
        t.setTrainingDate(LocalDate.of(2026, 12, 10));
        t.setHours(16);
        t.setResult("PASS");
        return t;
    }

    private RadiationHealth newHealth(Long personnelId, String personnelName) {
        RadiationHealth h = new RadiationHealth();
        h.setPersonnelId(personnelId);
        h.setPersonnelName(personnelName);
        h.setExamType("ON_JOB");
        h.setExamDate(LocalDate.of(2026, 12, 10));
        h.setExamOrg("市职业病防治院");
        h.setDose(new BigDecimal("5.00"));
        h.setResult("FIT");
        h.setNextExamDate(LocalDate.of(2027, 12, 10));
        return h;
    }
}
