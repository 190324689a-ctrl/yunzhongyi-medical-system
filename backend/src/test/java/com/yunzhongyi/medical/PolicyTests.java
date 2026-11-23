package com.yunzhongyi.medical;

import com.yunzhongyi.medical.policy.dto.AbolishRequest;
import com.yunzhongyi.medical.policy.dto.PolicyQuery;
import com.yunzhongyi.medical.policy.dto.PublishRequest;
import com.yunzhongyi.medical.policy.entity.Policy;
import com.yunzhongyi.medical.policy.entity.PolicyReading;
import com.yunzhongyi.medical.policy.excel.PolicyExcelUtil;
import com.yunzhongyi.medical.policy.service.PolicyReadingService;
import com.yunzhongyi.medical.policy.service.PolicyService;
import com.yunzhongyi.medical.policy.service.PolicyTypeUtil;
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
 * 政策法规管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 部法规（法律 1 / 行政法规 2 / 部门规章 2 / 地方性法规 1，
 * 国家 5 / 省 1，已发布 4 / 草稿 1 / 已废止 1，30 天内到期 1 部、已过期 1 部；
 * 发布月份 1994-09 / 2011-03 / 2016-11 / 2022-03 / 2024-01 各 1 次）、
 * 6 条查阅学习记录（医师法 3 条 / 医疗质量管理办法 2 条 / 医疗机构管理条例 1 条，
 * 已学习 4 人次、已查阅 2 人次，覆盖法规 3 部）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class PolicyTests {

    @Autowired
    private PolicyService policyService;

    @Autowired
    private PolicyReadingService policyReadingService;

    @Test
    void contextLoads() {
        assertNotNull(policyService);
        assertNotNull(policyReadingService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(policyService.count() >= 6);

        // 种子法规 1：中华人民共和国医师法，V1.0，已发布，2022-03-01 发布，无失效日期
        Policy p1 = policyService.getById(1L);
        assertEquals("中华人民共和国医师法", p1.getTitle());
        assertEquals("主席令第95号", p1.getPolicyNo());
        assertEquals("LAW", p1.getCategory());
        assertEquals("NATIONAL", p1.getLevel());
        assertEquals("全国人民代表大会常务委员会", p1.getIssuingAuthority());
        assertEquals("V1.0", p1.getVersion());
        assertEquals("全院", p1.getApplicableDept());
        assertEquals("全体医师", p1.getApplicablePersonnel());
        assertEquals("医务科", p1.getDepartment());
        assertEquals("吴文斌", p1.getDrafter());
        assertEquals("陈国强", p1.getApprover());
        assertEquals(LocalDate.of(2022, 3, 1), p1.getPublishDate());
        assertNull(p1.getExpiryDate());
        assertEquals(1, p1.getStatus());
        assertTrue(p1.getContent().contains("医师合法权益"));
        assertTrue(p1.getApprovalHistory().contains("发布"));

        // 种子法规 2：医疗质量管理办法，2026-12-20 到期（30 天内到期）
        Policy p2 = policyService.getById(2L);
        assertEquals("医疗质量管理办法", p2.getTitle());
        assertEquals("V2.0", p2.getVersion());
        assertEquals(LocalDate.of(2026, 12, 20), p2.getExpiryDate());
        assertTrue(PolicyTypeUtil.isExpiringSoon(p2.getStatus(), p2.getExpiryDate()));
        assertFalse(PolicyTypeUtil.isExpired(p2.getStatus(), p2.getExpiryDate()));

        // 种子法规 3：医疗机构管理条例，失效日期 2026-11-15 已过期
        Policy p3 = policyService.getById(3L);
        assertEquals("医疗机构管理条例", p3.getTitle());
        assertEquals("REGULATION", p3.getCategory());
        assertTrue(PolicyTypeUtil.isExpired(p3.getStatus(), p3.getExpiryDate()));

        // 种子法规 4：中医诊所备案管理暂行办法，草稿，未发布
        Policy p4 = policyService.getById(4L);
        assertEquals("中医诊所备案管理暂行办法", p4.getTitle());
        assertEquals("RULE", p4.getCategory());
        assertEquals(0, p4.getStatus());
        assertNull(p4.getPublishDate());
        assertTrue(p4.getApplicableDept().contains("中医内科"));

        // 种子法规 5：医疗机构药事管理规定，V1.0，已废止
        Policy p5 = policyService.getById(5L);
        assertEquals("医疗机构药事管理规定", p5.getTitle());
        assertEquals(2, p5.getStatus());
        assertTrue(p5.getAbolishReason().contains("新版"));

        // 种子法规 6：广东省医疗纠纷预防与处理办法，地方性法规，省级，已发布
        Policy p6 = policyService.getById(6L);
        assertEquals("广东省医疗纠纷预防与处理办法", p6.getTitle());
        assertEquals("LOCAL", p6.getCategory());
        assertEquals("PROVINCIAL", p6.getLevel());
        assertEquals(1, p6.getStatus());

        // 查阅学习记录种子：6 条，已学习 4 人次，覆盖法规 3 部
        assertTrue(policyReadingService.count() >= 6);
        List<PolicyReading> p1Readings = policyReadingService.listByPolicy(1L);
        assertEquals(3, p1Readings.size());
        assertEquals(2, p1Readings.stream().filter(r -> Integer.valueOf(1).equals(r.getLearned())).count());
        assertEquals("中华人民共和国医师法", p1Readings.get(0).getPolicyTitle());
        assertEquals(2, policyReadingService.listByPolicy(2L).size());
        assertEquals(1, policyReadingService.listByPolicy(3L).size());
        assertEquals(0, policyReadingService.listByPolicy(4L).size());
    }

    @Test
    void pageQueryWithFilters() {
        PolicyQuery q = new PolicyQuery();
        q.setCategory("REGULATION");
        assertEquals(2, policyService.pageQuery(q).getTotal()); // 医疗质量管理办法、医疗机构管理条例

        PolicyQuery q2 = new PolicyQuery();
        q2.setLevel("PROVINCIAL");
        assertEquals(1, policyService.pageQuery(q2).getTotal()); // 广东省医疗纠纷预防与处理办法

        PolicyQuery q3 = new PolicyQuery();
        q3.setStatus(1);
        assertEquals(4, policyService.pageQuery(q3).getTotal()); // 已发布 4 部

        PolicyQuery q4 = new PolicyQuery();
        q4.setKeyword("卫医发");
        assertEquals(1, policyService.pageQuery(q4).getTotal()); // 医疗质量管理办法

        PolicyQuery q5 = new PolicyQuery();
        q5.setKeyword("医师法");
        assertEquals(1, policyService.pageQuery(q5).getTotal());

        PolicyQuery q6 = new PolicyQuery();
        q6.setApplicableDept("中医内科");
        assertEquals(1, policyService.pageQuery(q6).getTotal()); // 中医诊所备案管理暂行办法

        PolicyQuery q7 = new PolicyQuery();
        q7.setExpiringSoon(true);
        assertEquals(1, policyService.pageQuery(q7).getTotal()); // 医疗质量管理办法

        PolicyQuery q8 = new PolicyQuery();
        q8.setExpired(true);
        assertEquals(1, policyService.pageQuery(q8).getTotal()); // 医疗机构管理条例
    }

    @Test
    void saveValidationAndDefaults() {
        // 法规分类非法，拒绝保存
        Policy badCategory = newPolicy("测试法规", "ZF-CS-2026-001");
        badCategory.setCategory("XXX");
        assertThrows(IllegalArgumentException.class, () -> policyService.save(badCategory));

        // 发布层级非法，拒绝保存
        Policy badLevel = newPolicy("测试法规", "ZF-CS-2026-001");
        badLevel.setLevel("XXX");
        assertThrows(IllegalArgumentException.class, () -> policyService.save(badLevel));

        // 失效日期早于生效日期，拒绝保存
        Policy badDate = newPolicy("测试法规", "ZF-CS-2026-001");
        badDate.setEffectiveDate(LocalDate.of(2026, 1, 1));
        badDate.setExpiryDate(LocalDate.of(2025, 12, 31));
        assertThrows(IllegalArgumentException.class, () -> policyService.save(badDate));

        // 合法建档：默认草稿、V1.0、适用科室默认“全院”
        Policy p = newPolicy("测试法规", "ZF-CS-2026-001");
        p.setApplicableDept(null);
        assertTrue(policyService.save(p));
        try {
            assertNotNull(p.getId());
            assertEquals(PolicyTypeUtil.STATUS_DRAFT, p.getStatus());
            assertEquals("V1.0", p.getVersion());
            assertEquals("全院", p.getApplicableDept());

            // 同一文号同一版本已有记录，拒绝重复建档
            Policy dup = newPolicy("测试法规", "ZF-CS-2026-001");
            assertThrows(IllegalArgumentException.class, () -> policyService.save(dup));

            // 同一文号不同版本可以建档
            Policy v2 = newPolicy("测试法规", "ZF-CS-2026-001");
            v2.setVersion("V2.0");
            assertTrue(policyService.save(v2));
            try {
                assertEquals("V2.0", v2.getVersion());
            } finally {
                policyService.removeById(v2.getId());
            }
        } finally {
            policyService.removeById(p.getId());
        }
    }

    @Test
    void updateGuard() {
        // 已发布的法规不能直接修改（更新走新建版本）
        Policy published = policyService.getById(1L);
        published.setTitle("改名后的法规");
        assertThrows(IllegalStateException.class, () -> policyService.updateById(published));

        // 已废止的法规不能修改
        Policy abolished = policyService.getById(5L);
        abolished.setTitle("改名后的法规");
        assertThrows(IllegalStateException.class, () -> policyService.updateById(abolished));

        // 草稿可以修改
        Policy draft = policyService.getById(4L);
        String oldTitle = draft.getTitle();
        draft.setTitle("中医诊所备案管理暂行办法（修订稿）");
        assertTrue(policyService.updateById(draft));
        try {
            assertEquals("中医诊所备案管理暂行办法（修订稿）", policyService.getById(4L).getTitle());
        } finally {
            Policy restore = new Policy();
            restore.setId(4L);
            restore.setTitle(oldTitle);
            policyService.getBaseMapper().updateById(restore);
        }
    }

    @Test
    void publishFlow() {
        // 发布草稿（种子 id=4 中医诊所备案管理暂行办法）需要审批人
        PublishRequest noApprover = new PublishRequest();
        assertThrows(IllegalArgumentException.class, () -> policyService.publish(4L, noApprover));

        PublishRequest req = new PublishRequest();
        req.setApprover("吴文斌");
        req.setOpinion("同意");
        Policy published = policyService.publish(4L, req);
        try {
            assertEquals(1, published.getStatus());
            assertEquals("吴文斌", published.getApprover());
            assertEquals(LocalDate.now(), published.getPublishDate());
            assertEquals(LocalDate.now(), published.getEffectiveDate()); // 生效日期为空时默认当天
            assertTrue(published.getApprovalHistory().contains("发布"));

            // 已发布的法规不能再次发布
            assertThrows(IllegalStateException.class, () -> policyService.publish(4L, req));
        } finally {
            // 恢复种子：状态改回草稿，并把发布日期/生效日期/审批信息显式清掉
            // （updateById 不更新 null 字段，所以这里用 updateWrapper 直接置空）
            policyService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Policy>()
                            .eq(Policy::getId, 4L)
                            .set(Policy::getStatus, 0)
                            .set(Policy::getPublishDate, null)
                            .set(Policy::getEffectiveDate, null)
                            .set(Policy::getApprover, null)
                            .set(Policy::getApprovalHistory, null));
            assertEquals(0, policyService.getById(4L).getStatus());
            assertNull(policyService.getById(4L).getPublishDate());
        }

        // 已废止的法规不能发布
        PublishRequest req2 = new PublishRequest();
        req2.setApprover("吴文斌");
        assertThrows(IllegalStateException.class, () -> policyService.publish(5L, req2));
    }

    @Test
    void abolishFlow() {
        // 废止（种子 id=6 广东省医疗纠纷预防与处理办法）需要废止原因
        AbolishRequest noReason = new AbolishRequest();
        assertThrows(IllegalArgumentException.class, () -> policyService.abolish(6L, noReason));

        AbolishRequest req = new AbolishRequest();
        req.setReason("省级办法修订，新办法发布后废止");
        Policy abolished = policyService.abolish(6L, req);
        try {
            assertEquals(2, abolished.getStatus());
            assertEquals("省级办法修订，新办法发布后废止", abolished.getAbolishReason());
            assertTrue(abolished.getApprovalHistory().contains("废止"));

            // 已废止的法规不能再次废止，也不能发布
            assertThrows(IllegalStateException.class, () -> policyService.abolish(6L, req));
            PublishRequest pub = new PublishRequest();
            pub.setApprover("吴文斌");
            assertThrows(IllegalStateException.class, () -> policyService.publish(6L, pub));
        } finally {
            // 恢复种子：状态改回已发布，并把废止原因显式清掉
            policyService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Policy>()
                            .eq(Policy::getId, 6L)
                            .set(Policy::getStatus, 1)
                            .set(Policy::getAbolishReason, null));
            assertEquals(1, policyService.getById(6L).getStatus());
            assertNull(policyService.getById(6L).getAbolishReason());
        }

        // 草稿不能废止
        AbolishRequest req2 = new AbolishRequest();
        req2.setReason("测试");
        assertThrows(IllegalStateException.class, () -> policyService.abolish(4L, req2));
    }

    @Test
    void newVersionFlow() {
        // 草稿不能新建版本
        assertThrows(IllegalStateException.class, () -> policyService.newVersion(4L));

        // 种子法规 1（中华人民共和国医师法 V1.0，已发布）新建版本：V2.0 草稿，正文与适用范围沿用
        Policy next = policyService.newVersion(1L);
        try {
            assertNotNull(next.getId());
            assertNotEquals(1L, next.getId());
            assertEquals("中华人民共和国医师法", next.getTitle());
            assertEquals("主席令第95号", next.getPolicyNo());
            assertEquals("LAW", next.getCategory());
            assertEquals("NATIONAL", next.getLevel());
            assertEquals("V2.0", next.getVersion());
            assertEquals(0, next.getStatus());
            assertTrue(next.getContent().contains("医师合法权益"));
            assertEquals("全院", next.getApplicableDept());
            assertEquals("全体医师", next.getApplicablePersonnel());
            assertNull(next.getPublishDate());
            assertNull(next.getApprover());

            // 同一文号已有 V2.0 版本草稿，再次新建版本拒绝
            assertThrows(IllegalArgumentException.class, () -> policyService.newVersion(1L));

            // 发布 V2.0：旧版本 V1.0 自动废止
            PublishRequest req = new PublishRequest();
            req.setApprover("陈国强");
            Policy published = policyService.publish(next.getId(), req);
            assertEquals(1, published.getStatus());
            Policy old = policyService.getById(1L);
            assertEquals(2, old.getStatus());
            assertTrue(old.getAbolishReason().contains("V2.0"));
            assertTrue(old.getApprovalHistory().contains("自动废止"));
        } finally {
            // 恢复种子：删除 V2.0 记录，V1.0 恢复已发布并清空自动废止信息
            policyService.getBaseMapper().deleteById(next.getId());
            policyService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Policy>()
                            .eq(Policy::getId, 1L)
                            .set(Policy::getStatus, 1)
                            .set(Policy::getAbolishReason, null)
                            .set(Policy::getApprovalHistory, "医务科 陈国强：发布（同意）"));
            assertEquals(1, policyService.getById(1L).getStatus());
            assertNull(policyService.getById(1L).getAbolishReason());
        }
    }

    @Test
    void deleteGuard() {
        // 已发布的法规不能直接删除
        assertThrows(IllegalStateException.class, () -> policyService.removeById(1L));

        // 草稿可以删除
        Policy p = newPolicy("待删法规", "ZF-CS-2026-002");
        assertTrue(policyService.save(p));
        assertTrue(policyService.removeById(p.getId()));
        assertNull(policyService.getById(p.getId()));
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = policyService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(1L, s.get("draft")); // 中医诊所备案管理暂行办法
        assertEquals(4L, s.get("published")); // 医师法、医疗质量管理办法、医疗机构管理条例、广东省医疗纠纷办法
        assertEquals(1L, s.get("abolished")); // 医疗机构药事管理规定
        Map<?, ?> byCategory = (Map<?, ?>) s.get("byCategory");
        assertEquals(1L, byCategory.get("LAW"));
        assertEquals(2L, byCategory.get("REGULATION"));
        assertEquals(2L, byCategory.get("RULE"));
        assertEquals(1L, byCategory.get("LOCAL"));
        assertEquals(0L, byCategory.get("NORMATIVE"));
        assertEquals(0L, byCategory.get("STANDARD"));
        assertEquals(0L, byCategory.get("OTHER"));
        Map<?, ?> byLevel = (Map<?, ?>) s.get("byLevel");
        assertEquals(5L, byLevel.get("NATIONAL"));
        assertEquals(1L, byLevel.get("PROVINCIAL"));
        assertEquals(0L, byLevel.get("MUNICIPAL"));
        assertEquals(0L, byLevel.get("HOSPITAL"));

        // 30 天内到期 1 部（医疗质量管理办法，2026-12-20）；已过期 1 部（医疗机构管理条例，2026-11-15）
        assertEquals(1L, s.get("expiringSoon"));
        assertEquals(1L, s.get("expired"));

        // 查阅学习统计：总人次 6、已学习 4 人次、覆盖法规 3 部
        assertEquals(6L, s.get("totalReadings"));
        assertEquals(4L, s.get("learnedCount"));
        assertEquals(3L, s.get("readPolicyCount"));

        // 按分类汇总：行政法规 2 部、已发布 2 部；法律 1 部、已发布 1 部
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byCategoryList = (List<Map<String, Object>>) s.get("byCategoryList");
        Map<String, Object> regulation = byCategoryList.stream()
                .filter(p -> "REGULATION".equals(p.get("category"))).findFirst().orElseThrow();
        assertEquals(2L, regulation.get("count"));
        assertEquals(2L, regulation.get("publishedCount"));

        // 按层级汇总：国家 5 部、已发布 3 部；省 1 部、已发布 1 部
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byLevelList = (List<Map<String, Object>>) s.get("byLevelList");
        Map<String, Object> national = byLevelList.stream()
                .filter(p -> "NATIONAL".equals(p.get("level"))).findFirst().orElseThrow();
        assertEquals(5L, national.get("count"));
        assertEquals(3L, national.get("publishedCount"));

        // 按月发布趋势：1994-09 / 2011-03 / 2016-11 / 2022-03 / 2024-01 各 1 次
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(5, byMonth.size());
        assertEquals("1994-09", byMonth.get(0).get("month"));
        assertEquals(1L, byMonth.get(0).get("count"));
        assertEquals("2024-01", byMonth.get(4).get("month"));
        assertEquals(1L, byMonth.get(4).get("count"));
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> policyService.detail(999999L));
    }

    @Test
    void downloadText() {
        String text = policyService.downloadText(1L);
        assertTrue(text.contains("中华人民共和国医师法"));
        assertTrue(text.contains("主席令第95号"));
        assertTrue(text.contains("V1.0"));
        assertTrue(text.contains("医师合法权益"));
        assertTrue(text.contains("全院"));
        assertThrows(IllegalArgumentException.class, () -> policyService.downloadText(999999L));
    }

    @Test
    void excelRoundTrip() {
        PolicyQuery q = new PolicyQuery();
        q.setPage(1);
        q.setSize(100);
        List<Policy> all = policyService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PolicyExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<Policy> back = PolicyExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（文号、法规名称、分类、层级、发布机关、版本、适用科室/人员、归口科室、起草人、发布/生效/失效日期、状态）
        Policy first = all.stream()
                .filter(p -> "主席令第95号".equals(p.getPolicyNo()))
                .findFirst().orElseThrow();
        Policy firstBack = back.stream()
                .filter(p -> "主席令第95号".equals(p.getPolicyNo()))
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
        // 新建一条草稿法规 → 导出全部 → 删掉这条 → 再导回同一法规，应只补回这一条
        // （导入的均为草稿，种子里的已发布/已废止行按“同一文号同一版本重复”规则跳过）
        Policy fresh = newPolicy("导入测试法规", "ZF-CS-2026-003");
        assertTrue(policyService.save(fresh));

        PolicyQuery q = new PolicyQuery();
        q.setPage(1);
        q.setSize(100);
        List<Policy> all = policyService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PolicyExcelUtil.write(all, out);

        assertTrue(policyService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, policyService.listByQuery(q).size());

        int imported = policyService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因重复而跳过，只补回导入测试法规这一条
        assertEquals(all.size(), policyService.listByQuery(q).size());
        Policy restored = policyService.listByQuery(q).stream()
                .filter(p -> "ZF-CS-2026-003".equals(p.getPolicyNo()))
                .findFirst().orElseThrow();
        assertEquals("RULE", restored.getCategory());
        assertEquals("NATIONAL", restored.getLevel());
        assertEquals(0, restored.getStatus()); // 导入的法规均为草稿
        assertTrue(policyService.removeById(restored.getId()));
        assertEquals(all.size() - 1, policyService.listByQuery(q).size());
    }

    @Test
    void readingCreateAndLearned() {
        // 草稿法规不能登记查阅学习
        PolicyReading draftReading = newReading(4L, "测试人员", "医务科");
        assertThrows(IllegalStateException.class, () -> policyReadingService.create(draftReading));

        // 已废止法规不能登记查阅学习
        PolicyReading abolishedReading = newReading(5L, "测试人员", "医务科");
        assertThrows(IllegalStateException.class, () -> policyReadingService.create(abolishedReading));

        // 不存在的法规不能登记
        PolicyReading missingReading = newReading(999999L, "测试人员", "医务科");
        assertThrows(IllegalArgumentException.class, () -> policyReadingService.create(missingReading));

        // 合法登记：查阅日期默认当天、默认已查阅、法规名称自动补全
        PolicyReading r = newReading(1L, "测试人员", "中医内科");
        assertTrue(policyReadingService.create(r) != null);
        try {
            assertNotNull(r.getId());
            assertEquals("中华人民共和国医师法", r.getPolicyTitle());
            assertEquals(LocalDate.now(), r.getReadDate());
            assertEquals(0, r.getLearned());

            // 同一人同一法规同一天重复登记拒绝
            PolicyReading dup = newReading(1L, "测试人员", "中医内科");
            assertThrows(IllegalArgumentException.class, () -> policyReadingService.create(dup));

            // 标记已学习
            PolicyReading learned = policyReadingService.markLearned(r.getId());
            assertEquals(1, learned.getLearned());

            // 已学习不能再次标记
            assertThrows(IllegalStateException.class, () -> policyReadingService.markLearned(r.getId()));
        } finally {
            policyReadingService.deleteReading(r.getId());
            assertNull(policyReadingService.getById(r.getId()));
        }

        // 不存在的记录不能标记/删除
        assertThrows(IllegalArgumentException.class, () -> policyReadingService.markLearned(999999L));
        assertThrows(IllegalArgumentException.class, () -> policyReadingService.deleteReading(999999L));
    }

    @Test
    void readingPageQuery() {
        // 按法规查：医师法 3 条
        com.yunzhongyi.medical.policy.dto.PolicyReadingQuery q = new com.yunzhongyi.medical.policy.dto.PolicyReadingQuery();
        q.setPolicyId(1L);
        assertEquals(3, policyReadingService.pageQuery(q).getTotal());

        // 按是否已学习查：医师法已学习 2 条
        com.yunzhongyi.medical.policy.dto.PolicyReadingQuery q2 = new com.yunzhongyi.medical.policy.dto.PolicyReadingQuery();
        q2.setPolicyId(1L);
        q2.setLearned(1);
        assertEquals(2, policyReadingService.pageQuery(q2).getTotal());

        // 按查阅人关键字查
        com.yunzhongyi.medical.policy.dto.PolicyReadingQuery q3 = new com.yunzhongyi.medical.policy.dto.PolicyReadingQuery();
        q3.setKeyword("吴文斌");
        assertEquals(1, policyReadingService.pageQuery(q3).getTotal());
    }

    private Policy newPolicy(String title, String policyNo) {
        Policy p = new Policy();
        p.setTitle(title);
        p.setPolicyNo(policyNo);
        p.setCategory("RULE");
        p.setLevel("NATIONAL");
        p.setIssuingAuthority("国家卫生健康委员会");
        p.setVersion("V1.0");
        p.setContent("测试法规正文内容。");
        p.setApplicableDept("全院");
        p.setApplicablePersonnel("全体医务人员");
        p.setDepartment("医务科");
        p.setDrafter("吴文斌");
        p.setEffectiveDate(LocalDate.of(2026, 1, 1));
        p.setExpiryDate(LocalDate.of(2027, 12, 31));
        return p;
    }

    private PolicyReading newReading(Long policyId, String readerName, String deptName) {
        PolicyReading r = new PolicyReading();
        r.setPolicyId(policyId);
        r.setReaderName(readerName);
        r.setDeptName(deptName);
        r.setReadDate(LocalDate.now());
        r.setLearned(0);
        return r;
    }
}
