package com.yunzhongyi.medical;

import com.yunzhongyi.medical.document.dto.AbolishRequest;
import com.yunzhongyi.medical.document.dto.DocumentQuery;
import com.yunzhongyi.medical.document.dto.PublishRequest;
import com.yunzhongyi.medical.document.entity.Document;
import com.yunzhongyi.medical.document.excel.DocumentExcelUtil;
import com.yunzhongyi.medical.document.service.DocumentService;
import com.yunzhongyi.medical.document.service.DocumentTypeUtil;
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
 * 文件制度管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 个文件（制度 3 / 规范 1 / 流程 1 / 文件 1，已发布 4 /
 * 草稿 1 / 已作废 1，30 天内到期 1 个、已过期 1 个；发布月份
 * 2023-01 / 2024-01 / 2025-03 / 2025-06 / 2026-01 各 1 次）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class DocumentTests {

    @Autowired
    private DocumentService documentService;

    @Test
    void contextLoads() {
        assertNotNull(documentService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(documentService.count() >= 6);

        // 种子文件 1：三级医师查房制度，V3.0，已发布，2024-01-01 发布
        Document d1 = documentService.getById(1L);
        assertEquals("三级医师查房制度", d1.getTitle());
        assertEquals("YW-ZD-2024-001", d1.getDocNo());
        assertEquals("SYSTEM", d1.getCategory());
        assertEquals("V3.0", d1.getVersion());
        assertEquals("医务科", d1.getDepartment());
        assertEquals("吴文斌", d1.getDrafter());
        assertEquals("陈国强", d1.getApprover());
        assertEquals(LocalDate.of(2024, 1, 1), d1.getPublishDate());
        assertEquals(LocalDate.of(2027, 12, 31), d1.getExpiryDate());
        assertEquals(1, d1.getStatus());
        assertTrue(d1.getContent().contains("三级医师查房"));
        assertTrue(d1.getApprovalHistory().contains("发布"));

        // 种子文件 2：抗菌药物临床应用管理办法，2026-12-20 到期（30 天内到期）
        Document d2 = documentService.getById(2L);
        assertEquals("抗菌药物临床应用管理办法", d2.getTitle());
        assertEquals("V2.0", d2.getVersion());
        assertEquals(LocalDate.of(2026, 12, 20), d2.getExpiryDate());
        assertTrue(DocumentTypeUtil.isExpiringSoon(d2.getStatus(), d2.getExpiryDate()));
        assertFalse(DocumentTypeUtil.isExpired(d2.getStatus(), d2.getExpiryDate()));

        // 种子文件 3：病历书写规范，失效日期 2026-11-15 已过期
        Document d3 = documentService.getById(3L);
        assertEquals("病历书写规范", d3.getTitle());
        assertEquals("STANDARD", d3.getCategory());
        assertTrue(DocumentTypeUtil.isExpired(d3.getStatus(), d3.getExpiryDate()));

        // 种子文件 4：门诊接诊流程，草稿，未发布
        Document d4 = documentService.getById(4L);
        assertEquals("门诊接诊流程", d4.getTitle());
        assertEquals("PROCESS", d4.getCategory());
        assertEquals(0, d4.getStatus());
        assertNull(d4.getPublishDate());

        // 种子文件 5：会诊制度 V1.0，已作废
        Document d5 = documentService.getById(5L);
        assertEquals("会诊制度", d5.getTitle());
        assertEquals(2, d5.getStatus());
        assertTrue(d5.getAbolishReason().contains("V2.0"));

        // 种子文件 6：医疗质量持续改进工作方案，文件类，已发布
        Document d6 = documentService.getById(6L);
        assertEquals("医疗质量持续改进工作方案", d6.getTitle());
        assertEquals("FILE", d6.getCategory());
        assertEquals(1, d6.getStatus());
    }

    @Test
    void pageQueryWithFilters() {
        DocumentQuery q = new DocumentQuery();
        q.setCategory("SYSTEM");
        assertEquals(3, documentService.pageQuery(q).getTotal()); // 三级医师查房、抗菌药物、会诊制度

        DocumentQuery q2 = new DocumentQuery();
        q2.setStatus(1);
        assertEquals(4, documentService.pageQuery(q2).getTotal()); // 已发布 4 个

        DocumentQuery q3 = new DocumentQuery();
        q3.setKeyword("YW-ZD-2024-001");
        assertEquals(1, documentService.pageQuery(q3).getTotal()); // 三级医师查房制度

        DocumentQuery q4 = new DocumentQuery();
        q4.setKeyword("查房");
        assertEquals(1, documentService.pageQuery(q4).getTotal());

        DocumentQuery q5 = new DocumentQuery();
        q5.setExpiringSoon(true);
        assertEquals(1, documentService.pageQuery(q5).getTotal()); // 抗菌药物临床应用管理办法

        DocumentQuery q6 = new DocumentQuery();
        q6.setExpired(true);
        assertEquals(1, documentService.pageQuery(q6).getTotal()); // 病历书写规范
    }

    @Test
    void saveValidationAndDefaults() {
        // 文件分类非法，拒绝保存
        Document badCategory = newDocument("测试文件", "YW-CS-2026-001");
        badCategory.setCategory("XXX");
        assertThrows(IllegalArgumentException.class, () -> documentService.save(badCategory));

        // 失效日期早于生效日期，拒绝保存
        Document badDate = newDocument("测试文件", "YW-CS-2026-001");
        badDate.setEffectiveDate(LocalDate.of(2026, 1, 1));
        badDate.setExpiryDate(LocalDate.of(2025, 12, 31));
        assertThrows(IllegalArgumentException.class, () -> documentService.save(badDate));

        // 合法建档：默认草稿、V1.0
        Document d = newDocument("测试文件", "YW-CS-2026-001");
        assertTrue(documentService.save(d));
        try {
            assertNotNull(d.getId());
            assertEquals(DocumentTypeUtil.STATUS_DRAFT, d.getStatus());
            assertEquals("V1.0", d.getVersion());

            // 同一文件编号同一版本已有记录，拒绝重复建档
            Document dup = newDocument("测试文件", "YW-CS-2026-001");
            assertThrows(IllegalArgumentException.class, () -> documentService.save(dup));

            // 同一文件编号不同版本可以建档
            Document v2 = newDocument("测试文件", "YW-CS-2026-001");
            v2.setVersion("V2.0");
            assertTrue(documentService.save(v2));
            try {
                assertEquals("V2.0", v2.getVersion());
            } finally {
                documentService.removeById(v2.getId());
            }
        } finally {
            documentService.removeById(d.getId());
        }
    }

    @Test
    void updateGuard() {
        // 已发布的文件不能直接修改（改版走新建版本）
        Document published = documentService.getById(1L);
        published.setTitle("改名后的制度");
        assertThrows(IllegalStateException.class, () -> documentService.updateById(published));

        // 已作废的文件不能修改
        Document abolished = documentService.getById(5L);
        abolished.setTitle("改名后的制度");
        assertThrows(IllegalStateException.class, () -> documentService.updateById(abolished));

        // 草稿可以修改
        Document draft = documentService.getById(4L);
        String oldTitle = draft.getTitle();
        draft.setTitle("门诊接诊流程（修订稿）");
        assertTrue(documentService.updateById(draft));
        try {
            assertEquals("门诊接诊流程（修订稿）", documentService.getById(4L).getTitle());
        } finally {
            Document restore = new Document();
            restore.setId(4L);
            restore.setTitle(oldTitle);
            documentService.getBaseMapper().updateById(restore);
        }
    }

    @Test
    void publishFlow() {
        // 发布草稿（种子 id=4 门诊接诊流程）需要审批人
        PublishRequest noApprover = new PublishRequest();
        assertThrows(IllegalArgumentException.class, () -> documentService.publish(4L, noApprover));

        PublishRequest req = new PublishRequest();
        req.setApprover("吴文斌");
        req.setOpinion("同意");
        Document published = documentService.publish(4L, req);
        try {
            assertEquals(1, published.getStatus());
            assertEquals("吴文斌", published.getApprover());
            assertEquals(LocalDate.now(), published.getPublishDate());
            assertEquals(LocalDate.now(), published.getEffectiveDate()); // 生效日期为空时默认当天
            assertTrue(published.getApprovalHistory().contains("发布"));

            // 已发布的文件不能再次发布
            assertThrows(IllegalStateException.class, () -> documentService.publish(4L, req));
        } finally {
            // 恢复种子：状态改回草稿，并把发布日期/生效日期/审批信息显式清掉
            // （updateById 不更新 null 字段，所以这里用 updateWrapper 直接置空）
            documentService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Document>()
                            .eq(Document::getId, 4L)
                            .set(Document::getStatus, 0)
                            .set(Document::getPublishDate, null)
                            .set(Document::getEffectiveDate, null)
                            .set(Document::getApprover, null)
                            .set(Document::getApprovalHistory, null));
            assertEquals(0, documentService.getById(4L).getStatus());
            assertNull(documentService.getById(4L).getPublishDate());
        }

        // 已作废的文件不能发布
        PublishRequest req2 = new PublishRequest();
        req2.setApprover("吴文斌");
        assertThrows(IllegalStateException.class, () -> documentService.publish(5L, req2));
    }

    @Test
    void abolishFlow() {
        // 作废（种子 id=6 医疗质量持续改进工作方案）需要作废原因
        AbolishRequest noReason = new AbolishRequest();
        assertThrows(IllegalArgumentException.class, () -> documentService.abolish(6L, noReason));

        AbolishRequest req = new AbolishRequest();
        req.setReason("年度方案到期，新方案发布后作废");
        Document abolished = documentService.abolish(6L, req);
        try {
            assertEquals(2, abolished.getStatus());
            assertEquals("年度方案到期，新方案发布后作废", abolished.getAbolishReason());
            assertTrue(abolished.getApprovalHistory().contains("作废"));

            // 已作废的文件不能再次作废，也不能发布
            assertThrows(IllegalStateException.class, () -> documentService.abolish(6L, req));
            PublishRequest pub = new PublishRequest();
            pub.setApprover("吴文斌");
            assertThrows(IllegalStateException.class, () -> documentService.publish(6L, pub));
        } finally {
            // 恢复种子：状态改回已发布，并把作废原因显式清掉
            documentService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Document>()
                            .eq(Document::getId, 6L)
                            .set(Document::getStatus, 1)
                            .set(Document::getAbolishReason, null));
            assertEquals(1, documentService.getById(6L).getStatus());
            assertNull(documentService.getById(6L).getAbolishReason());
        }

        // 草稿不能作废
        AbolishRequest req2 = new AbolishRequest();
        req2.setReason("测试");
        assertThrows(IllegalStateException.class, () -> documentService.abolish(4L, req2));
    }

    @Test
    void newVersionFlow() {
        // 草稿不能新建版本
        assertThrows(IllegalStateException.class, () -> documentService.newVersion(4L));

        // 种子文件 1（三级医师查房制度 V3.0，已发布）新建版本：V4.0 草稿，正文沿用
        Document next = documentService.newVersion(1L);
        try {
            assertNotNull(next.getId());
            assertNotEquals(1L, next.getId());
            assertEquals("三级医师查房制度", next.getTitle());
            assertEquals("YW-ZD-2024-001", next.getDocNo());
            assertEquals("SYSTEM", next.getCategory());
            assertEquals("V4.0", next.getVersion());
            assertEquals(0, next.getStatus());
            assertTrue(next.getContent().contains("三级医师查房"));
            assertNull(next.getPublishDate());
            assertNull(next.getApprover());

            // 同一编号已有 V4.0 版本草稿，再次新建版本拒绝
            assertThrows(IllegalArgumentException.class, () -> documentService.newVersion(1L));

            // 发布 V4.0：旧版本 V3.0 自动作废
            PublishRequest req = new PublishRequest();
            req.setApprover("陈国强");
            Document published = documentService.publish(next.getId(), req);
            assertEquals(1, published.getStatus());
            Document old = documentService.getById(1L);
            assertEquals(2, old.getStatus());
            assertTrue(old.getAbolishReason().contains("V4.0"));
            assertTrue(old.getApprovalHistory().contains("自动作废"));
        } finally {
            // 恢复种子：删除 V4.0 记录，V3.0 恢复已发布并清空自动作废信息
            documentService.getBaseMapper().deleteById(next.getId());
            documentService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Document>()
                            .eq(Document::getId, 1L)
                            .set(Document::getStatus, 1)
                            .set(Document::getAbolishReason, null)
                            .set(Document::getApprovalHistory, "医务科 陈国强：发布（同意）"));
            assertEquals(1, documentService.getById(1L).getStatus());
            assertNull(documentService.getById(1L).getAbolishReason());
        }
    }

    @Test
    void deleteGuard() {
        // 已发布的文件不能直接删除
        assertThrows(IllegalStateException.class, () -> documentService.removeById(1L));

        // 草稿可以删除
        Document d = newDocument("待删文件", "YW-CS-2026-002");
        assertTrue(documentService.save(d));
        assertTrue(documentService.removeById(d.getId()));
        assertNull(documentService.getById(d.getId()));
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = documentService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(1L, s.get("draft")); // 门诊接诊流程
        assertEquals(4L, s.get("published")); // 三级医师查房、抗菌药物、病历书写规范、质量改进方案
        assertEquals(1L, s.get("abolished")); // 会诊制度 V1.0
        Map<?, ?> byCategory = (Map<?, ?>) s.get("byCategory");
        assertEquals(3L, byCategory.get("SYSTEM"));
        assertEquals(1L, byCategory.get("STANDARD"));
        assertEquals(1L, byCategory.get("PROCESS"));
        assertEquals(1L, byCategory.get("FILE"));
        assertEquals(0L, byCategory.get("OTHER"));

        // 30 天内到期 1 个（抗菌药物办法，2026-12-20）；已过期 1 个（病历书写规范，2026-11-15）
        assertEquals(1L, s.get("expiringSoon"));
        assertEquals(1L, s.get("expired"));

        // 按分类汇总：制度 3 个、已发布 2 个；规范 1 个、已发布 1 个
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byCategoryList = (List<Map<String, Object>>) s.get("byCategoryList");
        Map<String, Object> system = byCategoryList.stream()
                .filter(d -> "SYSTEM".equals(d.get("category"))).findFirst().orElseThrow();
        assertEquals(3L, system.get("count"));
        assertEquals(2L, system.get("publishedCount"));

        // 按月发布趋势：2023-01 / 2024-01 / 2025-03 / 2025-06 / 2026-01 各 1 次
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(5, byMonth.size());
        assertEquals("2023-01", byMonth.get(0).get("month"));
        assertEquals(1L, byMonth.get(0).get("count"));
        assertEquals("2026-01", byMonth.get(4).get("month"));
        assertEquals(1L, byMonth.get(4).get("count"));
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> documentService.detail(999999L));
    }

    @Test
    void downloadText() {
        String text = documentService.downloadText(1L);
        assertTrue(text.contains("三级医师查房制度"));
        assertTrue(text.contains("YW-ZD-2024-001"));
        assertTrue(text.contains("V3.0"));
        assertTrue(text.contains("三级医师查房"));
        assertThrows(IllegalArgumentException.class, () -> documentService.downloadText(999999L));
    }

    @Test
    void excelRoundTrip() {
        DocumentQuery q = new DocumentQuery();
        q.setPage(1);
        q.setSize(100);
        List<Document> all = documentService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DocumentExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<Document> back = DocumentExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（文件编号、标题、分类、版本、归口科室、起草人、发布/生效/失效日期、状态）
        Document first = all.stream()
                .filter(d -> "YW-ZD-2024-001".equals(d.getDocNo()))
                .findFirst().orElseThrow();
        Document firstBack = back.stream()
                .filter(d -> "YW-ZD-2024-001".equals(d.getDocNo()))
                .findFirst().orElseThrow();
        assertEquals(first.getTitle(), firstBack.getTitle());
        assertEquals(first.getCategory(), firstBack.getCategory());
        assertEquals(first.getVersion(), firstBack.getVersion());
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
        // 新建一条草稿文件 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （导入的均为草稿，种子里的已发布/已作废行按“同一编号同一版本重复”规则跳过）
        Document fresh = newDocument("导入测试文件", "YW-CS-2026-003");
        assertTrue(documentService.save(fresh));

        DocumentQuery q = new DocumentQuery();
        q.setPage(1);
        q.setSize(100);
        List<Document> all = documentService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DocumentExcelUtil.write(all, out);

        assertTrue(documentService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, documentService.listByQuery(q).size());

        int imported = documentService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因重复而跳过，只补回导入测试文件这一条
        assertEquals(all.size(), documentService.listByQuery(q).size());
        Document restored = documentService.listByQuery(q).stream()
                .filter(d -> "YW-CS-2026-003".equals(d.getDocNo()))
                .findFirst().orElseThrow();
        assertEquals("SYSTEM", restored.getCategory());
        assertEquals(0, restored.getStatus()); // 导入的文件均为草稿
        assertTrue(documentService.removeById(restored.getId()));
        assertEquals(all.size() - 1, documentService.listByQuery(q).size());
    }

    private Document newDocument(String title, String docNo) {
        Document d = new Document();
        d.setTitle(title);
        d.setDocNo(docNo);
        d.setCategory("SYSTEM");
        d.setVersion("V1.0");
        d.setContent("测试文件正文内容。");
        d.setDepartment("医务科");
        d.setDrafter("吴文斌");
        d.setEffectiveDate(LocalDate.of(2026, 1, 1));
        d.setExpiryDate(LocalDate.of(2027, 12, 31));
        return d;
    }
}
