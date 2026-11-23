package com.yunzhongyi.medical;

import com.yunzhongyi.medical.coresystem.dto.AbolishRequest;
import com.yunzhongyi.medical.coresystem.dto.CoreSystemQuery;
import com.yunzhongyi.medical.coresystem.dto.PublishRequest;
import com.yunzhongyi.medical.coresystem.entity.CoreSystem;
import com.yunzhongyi.medical.coresystem.entity.CoreSystemReading;
import com.yunzhongyi.medical.coresystem.excel.CoreSystemExcelUtil;
import com.yunzhongyi.medical.coresystem.service.CoreSystemReadingService;
import com.yunzhongyi.medical.coresystem.service.CoreSystemService;
import com.yunzhongyi.medical.coresystem.service.CoreSystemTypeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心制度管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 项制度（医疗 3 / 护理 1 / 药事 1 / 安全 1，
 * 院级 5 / 科室级 1，已发布 4 / 草稿 1 / 已废止 1，30 天内到期 1 项、已过期 1 项；
 * 发布月份 2023-01 1 次 / 2024-01 3 次 / 2024-06 1 次）、
 * 6 条查阅学习记录（首诊负责制度 3 条 / 三级医师查房制度 2 条 / 分级护理制度 1 条，
 * 已学习 4 人次、已查阅 2 人次，覆盖制度 3 项）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class CoreSystemTests {

    @Autowired
    private CoreSystemService coreSystemService;

    @Autowired
    private CoreSystemReadingService coreSystemReadingService;

    @Test
    void contextLoads() {
        assertNotNull(coreSystemService);
        assertNotNull(coreSystemReadingService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(coreSystemService.count() >= 6);

        // 种子制度 1：首诊负责制度，V2.0，已发布，2024-01-01 发布，2027-12-31 失效
        CoreSystem p1 = coreSystemService.getById(1L);
        assertEquals("首诊负责制度", p1.getTitle());
        assertEquals("HX-ZD-2024-001", p1.getSystemNo());
        assertEquals("MEDICAL", p1.getCategory());
        assertEquals("HOSPITAL", p1.getLevel());
        assertEquals("医务科", p1.getIssuingAuthority());
        assertEquals("V2.0", p1.getVersion());
        assertEquals("全院", p1.getApplicableDept());
        assertEquals("全体医师", p1.getApplicablePersonnel());
        assertEquals("医务科", p1.getDepartment());
        assertEquals("吴文斌", p1.getDrafter());
        assertEquals("陈国强", p1.getApprover());
        assertEquals(LocalDate.of(2024, 1, 1), p1.getPublishDate());
        assertEquals(LocalDate.of(2027, 12, 31), p1.getExpiryDate());
        assertEquals(1, p1.getStatus());
        assertTrue(p1.getContent().contains("首诊负责"));
        assertTrue(p1.getApprovalHistory().contains("发布"));

        // 种子制度 2：三级医师查房制度，2026-12-20 到期（30 天内到期）
        CoreSystem p2 = coreSystemService.getById(2L);
        assertEquals("三级医师查房制度", p2.getTitle());
        assertEquals("V3.0", p2.getVersion());
        assertEquals(LocalDate.of(2026, 12, 20), p2.getExpiryDate());
        assertTrue(CoreSystemTypeUtil.isExpiringSoon(p2.getStatus(), p2.getExpiryDate()));
        assertFalse(CoreSystemTypeUtil.isExpired(p2.getStatus(), p2.getExpiryDate()));

        // 种子制度 3：分级护理制度，失效日期 2026-11-15 已过期
        CoreSystem p3 = coreSystemService.getById(3L);
        assertEquals("分级护理制度", p3.getTitle());
        assertEquals("NURSING", p3.getCategory());
        assertTrue(CoreSystemTypeUtil.isExpired(p3.getStatus(), p3.getExpiryDate()));

        // 种子制度 4：危急值报告制度，草稿，未发布
        CoreSystem p4 = coreSystemService.getById(4L);
        assertEquals("危急值报告制度", p4.getTitle());
        assertEquals("SAFETY", p4.getCategory());
        assertEquals(0, p4.getStatus());
        assertNull(p4.getPublishDate());
        assertEquals("全院", p4.getApplicableDept());

        // 种子制度 5：抗菌药物分级管理制度，V1.0，已废止
        CoreSystem p5 = coreSystemService.getById(5L);
        assertEquals("抗菌药物分级管理制度", p5.getTitle());
        assertEquals(2, p5.getStatus());
        assertTrue(p5.getAbolishReason().contains("新版"));

        // 种子制度 6：手术安全核查制度，医疗，科室级，已发布
        CoreSystem p6 = coreSystemService.getById(6L);
        assertEquals("手术安全核查制度", p6.getTitle());
        assertEquals("MEDICAL", p6.getCategory());
        assertEquals("DEPT", p6.getLevel());
        assertEquals(1, p6.getStatus());

        // 查阅学习记录种子：6 条，已学习 4 人次，覆盖制度 3 项
        assertTrue(coreSystemReadingService.count() >= 6);
        List<CoreSystemReading> p1Readings = coreSystemReadingService.listByCoreSystem(1L);
        assertEquals(3, p1Readings.size());
        assertEquals(2, p1Readings.stream().filter(r -> Integer.valueOf(1).equals(r.getLearned())).count());
        assertEquals("首诊负责制度", p1Readings.get(0).getCoreSystemTitle());
        assertEquals(2, coreSystemReadingService.listByCoreSystem(2L).size());
        assertEquals(1, coreSystemReadingService.listByCoreSystem(3L).size());
        assertEquals(0, coreSystemReadingService.listByCoreSystem(4L).size());
    }

    @Test
    void pageQueryWithFilters() {
        CoreSystemQuery q = new CoreSystemQuery();
        q.setCategory("MEDICAL");
        assertEquals(3, coreSystemService.pageQuery(q).getTotal()); // 首诊负责、三级查房、手术安全核查

        CoreSystemQuery q2 = new CoreSystemQuery();
        q2.setLevel("DEPT");
        assertEquals(1, coreSystemService.pageQuery(q2).getTotal()); // 手术安全核查制度

        CoreSystemQuery q3 = new CoreSystemQuery();
        q3.setStatus(1);
        assertEquals(4, coreSystemService.pageQuery(q3).getTotal()); // 已发布 4 项

        CoreSystemQuery q4 = new CoreSystemQuery();
        q4.setKeyword("HX-ZD-2024");
        assertEquals(4, coreSystemService.pageQuery(q4).getTotal()); // 2024 年编号 4 项

        CoreSystemQuery q5 = new CoreSystemQuery();
        q5.setKeyword("首诊");
        assertEquals(1, coreSystemService.pageQuery(q5).getTotal());

        CoreSystemQuery q6 = new CoreSystemQuery();
        q6.setApplicableDept("手术科室");
        assertEquals(1, coreSystemService.pageQuery(q6).getTotal()); // 手术安全核查制度

        CoreSystemQuery q7 = new CoreSystemQuery();
        q7.setExpiringSoon(true);
        assertEquals(1, coreSystemService.pageQuery(q7).getTotal()); // 三级医师查房制度

        CoreSystemQuery q8 = new CoreSystemQuery();
        q8.setExpired(true);
        assertEquals(1, coreSystemService.pageQuery(q8).getTotal()); // 分级护理制度
    }

    @Test
    void saveValidationAndDefaults() {
        // 制度类别非法，拒绝保存
        CoreSystem badCategory = newCoreSystem("测试制度", "HX-CS-2026-001");
        badCategory.setCategory("XXX");
        assertThrows(IllegalArgumentException.class, () -> coreSystemService.save(badCategory));

        // 适用层级非法，拒绝保存
        CoreSystem badLevel = newCoreSystem("测试制度", "HX-CS-2026-001");
        badLevel.setLevel("XXX");
        assertThrows(IllegalArgumentException.class, () -> coreSystemService.save(badLevel));

        // 失效日期早于生效日期，拒绝保存
        CoreSystem badDate = newCoreSystem("测试制度", "HX-CS-2026-001");
        badDate.setEffectiveDate(LocalDate.of(2026, 1, 1));
        badDate.setExpiryDate(LocalDate.of(2025, 12, 31));
        assertThrows(IllegalArgumentException.class, () -> coreSystemService.save(badDate));

        // 合法建档：默认草稿、V1.0、适用科室默认“全院”
        CoreSystem p = newCoreSystem("测试制度", "HX-CS-2026-001");
        p.setApplicableDept(null);
        assertTrue(coreSystemService.save(p));
        try {
            assertNotNull(p.getId());
            assertEquals(CoreSystemTypeUtil.STATUS_DRAFT, p.getStatus());
            assertEquals("V1.0", p.getVersion());
            assertEquals("全院", p.getApplicableDept());

            // 同一编号同一版本已有记录，拒绝重复建档
            CoreSystem dup = newCoreSystem("测试制度", "HX-CS-2026-001");
            assertThrows(IllegalArgumentException.class, () -> coreSystemService.save(dup));

            // 同一编号不同版本可以建档
            CoreSystem v2 = newCoreSystem("测试制度", "HX-CS-2026-001");
            v2.setVersion("V2.0");
            assertTrue(coreSystemService.save(v2));
            try {
                assertEquals("V2.0", v2.getVersion());
            } finally {
                coreSystemService.removeById(v2.getId());
            }
        } finally {
            coreSystemService.removeById(p.getId());
        }
    }

    @Test
    void updateGuard() {
        // 已发布的制度不能直接修改（更新走新建版本）
        CoreSystem published = coreSystemService.getById(1L);
        published.setTitle("改名后的制度");
        assertThrows(IllegalStateException.class, () -> coreSystemService.updateById(published));

        // 已废止的制度不能修改
        CoreSystem abolished = coreSystemService.getById(5L);
        abolished.setTitle("改名后的制度");
        assertThrows(IllegalStateException.class, () -> coreSystemService.updateById(abolished));

        // 草稿可以修改
        CoreSystem draft = coreSystemService.getById(4L);
        String oldTitle = draft.getTitle();
        draft.setTitle("危急值报告制度（修订稿）");
        assertTrue(coreSystemService.updateById(draft));
        try {
            assertEquals("危急值报告制度（修订稿）", coreSystemService.getById(4L).getTitle());
        } finally {
            CoreSystem restore = new CoreSystem();
            restore.setId(4L);
            restore.setTitle(oldTitle);
            coreSystemService.getBaseMapper().updateById(restore);
        }
    }

    @Test
    void publishFlow() {
        // 发布草稿（种子 id=4 危急值报告制度）需要审批人
        PublishRequest noApprover = new PublishRequest();
        assertThrows(IllegalArgumentException.class, () -> coreSystemService.publish(4L, noApprover));

        PublishRequest req = new PublishRequest();
        req.setApprover("吴文斌");
        req.setOpinion("同意");
        CoreSystem published = coreSystemService.publish(4L, req);
        try {
            assertEquals(1, published.getStatus());
            assertEquals("吴文斌", published.getApprover());
            assertEquals(LocalDate.now(), published.getPublishDate());
            assertEquals(LocalDate.now(), published.getEffectiveDate()); // 生效日期为空时默认当天
            assertTrue(published.getApprovalHistory().contains("发布"));

            // 已发布的制度不能再次发布
            assertThrows(IllegalStateException.class, () -> coreSystemService.publish(4L, req));
        } finally {
            // 恢复种子：状态改回草稿，并把发布日期/生效日期/审批信息显式清掉
            // （updateById 不更新 null 字段，所以这里用 updateWrapper 直接置空）
            coreSystemService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CoreSystem>()
                            .eq(CoreSystem::getId, 4L)
                            .set(CoreSystem::getStatus, 0)
                            .set(CoreSystem::getPublishDate, null)
                            .set(CoreSystem::getEffectiveDate, null)
                            .set(CoreSystem::getApprover, null)
                            .set(CoreSystem::getApprovalHistory, null));
            assertEquals(0, coreSystemService.getById(4L).getStatus());
            assertNull(coreSystemService.getById(4L).getPublishDate());
        }

        // 已废止的制度不能发布
        PublishRequest req2 = new PublishRequest();
        req2.setApprover("吴文斌");
        assertThrows(IllegalStateException.class, () -> coreSystemService.publish(5L, req2));
    }

    @Test
    void abolishFlow() {
        // 废止（种子 id=6 手术安全核查制度）需要废止原因
        AbolishRequest noReason = new AbolishRequest();
        assertThrows(IllegalArgumentException.class, () -> coreSystemService.abolish(6L, noReason));

        AbolishRequest req = new AbolishRequest();
        req.setReason("科室级制度修订，新制度发布后废止");
        CoreSystem abolished = coreSystemService.abolish(6L, req);
        try {
            assertEquals(2, abolished.getStatus());
            assertEquals("科室级制度修订，新制度发布后废止", abolished.getAbolishReason());
            assertTrue(abolished.getApprovalHistory().contains("废止"));

            // 已废止的制度不能再次废止，也不能发布
            assertThrows(IllegalStateException.class, () -> coreSystemService.abolish(6L, req));
            PublishRequest pub = new PublishRequest();
            pub.setApprover("吴文斌");
            assertThrows(IllegalStateException.class, () -> coreSystemService.publish(6L, pub));
        } finally {
            // 恢复种子：状态改回已发布，并把废止原因显式清掉
            coreSystemService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CoreSystem>()
                            .eq(CoreSystem::getId, 6L)
                            .set(CoreSystem::getStatus, 1)
                            .set(CoreSystem::getAbolishReason, null));
            assertEquals(1, coreSystemService.getById(6L).getStatus());
            assertNull(coreSystemService.getById(6L).getAbolishReason());
        }

        // 草稿不能废止
        AbolishRequest req2 = new AbolishRequest();
        req2.setReason("测试");
        assertThrows(IllegalStateException.class, () -> coreSystemService.abolish(4L, req2));
    }

    @Test
    void newVersionFlow() {
        // 草稿不能新建版本
        assertThrows(IllegalStateException.class, () -> coreSystemService.newVersion(4L));

        // 种子制度 1（首诊负责制度 V2.0，已发布）新建版本：V3.0 草稿，正文与适用范围沿用
        CoreSystem next = coreSystemService.newVersion(1L);
        try {
            assertNotNull(next.getId());
            assertNotEquals(1L, next.getId());
            assertEquals("首诊负责制度", next.getTitle());
            assertEquals("HX-ZD-2024-001", next.getSystemNo());
            assertEquals("MEDICAL", next.getCategory());
            assertEquals("HOSPITAL", next.getLevel());
            assertEquals("V3.0", next.getVersion());
            assertEquals(0, next.getStatus());
            assertTrue(next.getContent().contains("首诊负责"));
            assertEquals("全院", next.getApplicableDept());
            assertEquals("全体医师", next.getApplicablePersonnel());
            assertNull(next.getPublishDate());
            assertNull(next.getApprover());

            // 同一编号已有 V3.0 版本草稿，再次新建版本拒绝
            assertThrows(IllegalArgumentException.class, () -> coreSystemService.newVersion(1L));

            // 发布 V3.0：旧版本 V2.0 自动废止
            PublishRequest req = new PublishRequest();
            req.setApprover("陈国强");
            CoreSystem published = coreSystemService.publish(next.getId(), req);
            assertEquals(1, published.getStatus());
            CoreSystem old = coreSystemService.getById(1L);
            assertEquals(2, old.getStatus());
            assertTrue(old.getAbolishReason().contains("V3.0"));
            assertTrue(old.getApprovalHistory().contains("自动废止"));
        } finally {
            // 恢复种子：删除 V3.0 记录，V2.0 恢复已发布并清空自动废止信息
            coreSystemService.getBaseMapper().deleteById(next.getId());
            coreSystemService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CoreSystem>()
                            .eq(CoreSystem::getId, 1L)
                            .set(CoreSystem::getStatus, 1)
                            .set(CoreSystem::getAbolishReason, null)
                            .set(CoreSystem::getApprovalHistory, "医务科 陈国强：发布（同意）"));
            assertEquals(1, coreSystemService.getById(1L).getStatus());
            assertNull(coreSystemService.getById(1L).getAbolishReason());
        }
    }

    @Test
    void deleteGuard() {
        // 已发布的制度不能直接删除
        assertThrows(IllegalStateException.class, () -> coreSystemService.removeById(1L));

        // 草稿可以删除
        CoreSystem p = newCoreSystem("待删制度", "HX-CS-2026-002");
        assertTrue(coreSystemService.save(p));
        assertTrue(coreSystemService.removeById(p.getId()));
        assertNull(coreSystemService.getById(p.getId()));
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = coreSystemService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(1L, s.get("draft")); // 危急值报告制度
        assertEquals(4L, s.get("published")); // 首诊负责、三级查房、分级护理、手术安全核查
        assertEquals(1L, s.get("abolished")); // 抗菌药物分级管理制度
        Map<?, ?> byCategory = (Map<?, ?>) s.get("byCategory");
        assertEquals(3L, byCategory.get("MEDICAL"));
        assertEquals(1L, byCategory.get("NURSING"));
        assertEquals(1L, byCategory.get("PHARMACY"));
        assertEquals(0L, byCategory.get("MANAGEMENT"));
        assertEquals(1L, byCategory.get("SAFETY"));
        assertEquals(0L, byCategory.get("OTHER"));
        Map<?, ?> byLevel = (Map<?, ?>) s.get("byLevel");
        assertEquals(5L, byLevel.get("HOSPITAL"));
        assertEquals(1L, byLevel.get("DEPT"));

        // 30 天内到期 1 项（三级医师查房制度，2026-12-20）；已过期 1 项（分级护理制度，2026-11-15）
        assertEquals(1L, s.get("expiringSoon"));
        assertEquals(1L, s.get("expired"));

        // 查阅学习统计：总人次 6、已学习 4 人次、覆盖制度 3 项
        assertEquals(6L, s.get("totalReadings"));
        assertEquals(4L, s.get("learnedCount"));
        assertEquals(3L, s.get("readCoreSystemCount"));

        // 按类别汇总：医疗 3 项、已发布 3 项；护理 1 项、已发布 1 项
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byCategoryList = (List<Map<String, Object>>) s.get("byCategoryList");
        Map<String, Object> medical = byCategoryList.stream()
                .filter(p -> "MEDICAL".equals(p.get("category"))).findFirst().orElseThrow();
        assertEquals(3L, medical.get("count"));
        assertEquals(3L, medical.get("publishedCount"));

        // 按层级汇总：院级 5 项、已发布 3 项；科室级 1 项、已发布 1 项
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byLevelList = (List<Map<String, Object>>) s.get("byLevelList");
        Map<String, Object> hospital = byLevelList.stream()
                .filter(p -> "HOSPITAL".equals(p.get("level"))).findFirst().orElseThrow();
        assertEquals(5L, hospital.get("count"));
        assertEquals(3L, hospital.get("publishedCount"));

        // 按月发布趋势：2023-01 1 次 / 2024-01 3 次 / 2024-06 1 次
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(3, byMonth.size());
        assertEquals("2023-01", byMonth.get(0).get("month"));
        assertEquals(1L, byMonth.get(0).get("count"));
        assertEquals("2024-01", byMonth.get(1).get("month"));
        assertEquals(3L, byMonth.get(1).get("count"));
        assertEquals("2024-06", byMonth.get(2).get("month"));
        assertEquals(1L, byMonth.get(2).get("count"));
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> coreSystemService.detail(999999L));
    }

    @Test
    void downloadText() {
        String text = coreSystemService.downloadText(1L);
        assertTrue(text.contains("首诊负责制度"));
        assertTrue(text.contains("HX-ZD-2024-001"));
        assertTrue(text.contains("V2.0"));
        assertTrue(text.contains("首诊负责"));
        assertTrue(text.contains("全院"));
        assertThrows(IllegalArgumentException.class, () -> coreSystemService.downloadText(999999L));
    }

    @Test
    void excelRoundTrip() {
        CoreSystemQuery q = new CoreSystemQuery();
        q.setPage(1);
        q.setSize(100);
        List<CoreSystem> all = coreSystemService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CoreSystemExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<CoreSystem> back = CoreSystemExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（制度编号、制度名称、类别、层级、制定部门、版本、适用科室/人员、归口科室、起草人、发布/生效/失效日期、状态）
        CoreSystem first = all.stream()
                .filter(p -> "HX-ZD-2024-001".equals(p.getSystemNo()))
                .findFirst().orElseThrow();
        CoreSystem firstBack = back.stream()
                .filter(p -> "HX-ZD-2024-001".equals(p.getSystemNo()))
                .findFirst().orElseThrow();
        assertEquals(first.getTitle(), firstBack.getTitle());
        assertEquals(first.getCategory(), firstBack.getCategory());
        assertEquals(first.getLevel(), firstBack.getLevel());
        assertEquals(first.getIssuingAuthority(), firstBack.getIssuingAuthority());
        assertEquals(first.getVersion(), firstBack.getVersion());
        assertEquals(first.getApplicableDept(), firstBack.getApplicableDept());
        assertEquals(first.getApplicablePersonnel(), firstBack.getApplicablePersonnel());
        assertEquals(first.getDepartment(), firstBack.getDepartment());
        assertEquals(first.getDrafter(), firstBack.getDrafter());
        assertEquals(first.getApprover(), firstBack.getApprover());
        assertEquals(first.getPublishDate(), firstBack.getPublishDate());
        assertEquals(first.getEffectiveDate(), firstBack.getEffectiveDate());
        assertEquals(first.getExpiryDate(), firstBack.getExpiryDate());
        assertEquals(first.getStatus(), firstBack.getStatus());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条草稿制度 → 导出全部 → 删掉这条 → 再导回同一制度，应只补回这一条
        // （导入的均为草稿，种子里的已发布/已废止行按“同一编号同一版本重复”规则跳过）
        CoreSystem fresh = newCoreSystem("导入测试制度", "HX-CS-2026-003");
        assertTrue(coreSystemService.save(fresh));

        CoreSystemQuery q = new CoreSystemQuery();
        q.setPage(1);
        q.setSize(100);
        List<CoreSystem> all = coreSystemService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CoreSystemExcelUtil.write(all, out);

        assertTrue(coreSystemService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, coreSystemService.listByQuery(q).size());

        int imported = coreSystemService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因重复而跳过，只补回导入测试制度这一条
        assertEquals(all.size(), coreSystemService.listByQuery(q).size());
        CoreSystem restored = coreSystemService.listByQuery(q).stream()
                .filter(p -> "HX-CS-2026-003".equals(p.getSystemNo()))
                .findFirst().orElseThrow();
        assertEquals("MEDICAL", restored.getCategory());
        assertEquals("HOSPITAL", restored.getLevel());
        assertEquals(0, restored.getStatus()); // 导入的制度均为草稿
        assertTrue(coreSystemService.removeById(restored.getId()));
        assertEquals(all.size() - 1, coreSystemService.listByQuery(q).size());
    }

    @Test
    void readingCreateAndLearned() {
        // 草稿制度不能登记查阅学习
        CoreSystemReading draftReading = newReading(4L, "测试人员", "医务科");
        assertThrows(IllegalStateException.class, () -> coreSystemReadingService.create(draftReading));

        // 已废止制度不能登记查阅学习
        CoreSystemReading abolishedReading = newReading(5L, "测试人员", "医务科");
        assertThrows(IllegalStateException.class, () -> coreSystemReadingService.create(abolishedReading));

        // 不存在的制度不能登记
        CoreSystemReading missingReading = newReading(999999L, "测试人员", "医务科");
        assertThrows(IllegalArgumentException.class, () -> coreSystemReadingService.create(missingReading));

        // 合法登记：查阅日期默认当天、默认已查阅、制度名称自动补全
        CoreSystemReading r = newReading(1L, "测试人员", "中医内科");
        assertTrue(coreSystemReadingService.create(r) != null);
        try {
            assertNotNull(r.getId());
            assertEquals("首诊负责制度", r.getCoreSystemTitle());
            assertEquals(LocalDate.now(), r.getReadDate());
            assertEquals(0, r.getLearned());

            // 同一人同一制度同一天重复登记拒绝
            CoreSystemReading dup = newReading(1L, "测试人员", "中医内科");
            assertThrows(IllegalArgumentException.class, () -> coreSystemReadingService.create(dup));

            // 标记已学习
            CoreSystemReading learned = coreSystemReadingService.markLearned(r.getId());
            assertEquals(1, learned.getLearned());

            // 已学习不能再次标记
            assertThrows(IllegalStateException.class, () -> coreSystemReadingService.markLearned(r.getId()));
        } finally {
            coreSystemReadingService.deleteReading(r.getId());
            assertNull(coreSystemReadingService.getById(r.getId()));
        }

        // 不存在的记录不能标记/删除
        assertThrows(IllegalArgumentException.class, () -> coreSystemReadingService.markLearned(999999L));
        assertThrows(IllegalArgumentException.class, () -> coreSystemReadingService.deleteReading(999999L));
    }

    @Test
    void readingPageQuery() {
        // 按制度查：首诊负责制度 3 条
        com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery q = new com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery();
        q.setCoreSystemId(1L);
        assertEquals(3, coreSystemReadingService.pageQuery(q).getTotal());

        // 按是否已学习查：首诊负责制度已学习 2 条
        com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery q2 = new com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery();
        q2.setCoreSystemId(1L);
        q2.setLearned(1);
        assertEquals(2, coreSystemReadingService.pageQuery(q2).getTotal());

        // 按查阅人关键字查
        com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery q3 = new com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery();
        q3.setKeyword("吴文斌");
        assertEquals(1, coreSystemReadingService.pageQuery(q3).getTotal());
    }

    private CoreSystem newCoreSystem(String title, String systemNo) {
        CoreSystem p = new CoreSystem();
        p.setTitle(title);
        p.setSystemNo(systemNo);
        p.setCategory("MEDICAL");
        p.setLevel("HOSPITAL");
        p.setIssuingAuthority("医务科");
        p.setVersion("V1.0");
        p.setContent("测试制度正文内容。");
        p.setApplicableDept("全院");
        p.setApplicablePersonnel("全体医务人员");
        p.setDepartment("医务科");
        p.setDrafter("吴文斌");
        p.setEffectiveDate(LocalDate.of(2026, 1, 1));
        p.setExpiryDate(LocalDate.of(2027, 12, 31));
        return p;
    }

    private CoreSystemReading newReading(Long coreSystemId, String readerName, String deptName) {
        CoreSystemReading r = new CoreSystemReading();
        r.setCoreSystemId(coreSystemId);
        r.setReaderName(readerName);
        r.setDeptName(deptName);
        r.setReadDate(LocalDate.now());
        r.setLearned(0);
        return r;
    }
}
