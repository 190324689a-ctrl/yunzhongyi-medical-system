package com.yunzhongyi.medical;

import com.yunzhongyi.medical.briefing.dto.GenerateBriefingRequest;
import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.briefing.entity.BriefingTemplate;
import com.yunzhongyi.medical.briefing.excel.BriefingExcelUtil;
import com.yunzhongyi.medical.briefing.export.BriefingPdfUtil;
import com.yunzhongyi.medical.briefing.export.BriefingWordUtil;
import com.yunzhongyi.medical.briefing.service.BriefingService;
import com.yunzhongyi.medical.briefing.service.BriefingTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 医疗工作简报模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：3 个简报模板（月报 1 / 季报 1 / 工作动态 1，启用 2 / 停用 1）、
 * 6 期简报（草稿 2 / 待审批 1 / 已发布 2 / 已驳回 1；月报 4 / 季报 1 / 工作动态 1；
 * 2026-11 有 3 期、2026-10 有 2 期、2026-09 有 1 期）。
 * PDF 导出为真实 PDF（OpenPDF + 嵌入简体中文字体），Word 导出为真实 .docx。
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class BriefingTests {

    @Autowired
    private BriefingTemplateService briefingTemplateService;

    @Autowired
    private BriefingService briefingService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(briefingTemplateService);
        assertNotNull(briefingService);
    }

    @Test
    void seedDataLoaded() {
        assertEquals(3L, briefingTemplateService.count());
        assertEquals(6L, briefingService.count());
        assertEquals("月度医疗工作简报模板", briefingTemplateService.getById(1L).getName());
        assertEquals(Integer.valueOf(0), briefingTemplateService.getById(3L).getStatus());
        assertEquals("医疗工作简报（2026年11月）", briefingService.getById(1L).getTitle());
        assertEquals(Integer.valueOf(2), briefingService.getById(1L).getStatus());
        assertEquals(Integer.valueOf(1), briefingService.getById(2L).getStatus());
        assertEquals(Integer.valueOf(0), briefingService.getById(4L).getStatus());
        assertEquals(Integer.valueOf(3), briefingService.getById(5L).getStatus());
        assertEquals("数据口径需核对，请重新核对检查问题数后重提。", briefingService.getById(5L).getRejectReason());
        assertEquals("2026-11-03", briefingService.getById(6L).getIssueNo());
    }

    @Test
    void briefingStats() {
        Map<String, Object> stats = briefingService.stats();
        assertEquals(6L, stats.get("total"));
        assertEquals(2L, stats.get("draft"));
        assertEquals(1L, stats.get("pending"));
        assertEquals(2L, stats.get("published"));
        assertEquals(1L, stats.get("rejected"));
        assertEquals(3L, stats.get("templateTotal"));
        assertEquals(2L, stats.get("templateEnabled"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byCategory = (Map<String, Long>) stats.get("byCategory");
        assertEquals(4L, byCategory.get("MONTHLY"));
        assertEquals(1L, byCategory.get("QUARTERLY"));
        assertEquals(0L, byCategory.get("ANNUAL"));
        assertEquals(0L, byCategory.get("SPECIAL"));
        assertEquals(1L, byCategory.get("DYNAMIC"));
    }

    @Test
    void briefingStatsByMonth() {
        Map<String, Object> byMonth = briefingService.statsByMonth();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) byMonth.get("byMonth");
        assertEquals(3, rows.size());
        assertEquals("2026-09", rows.get(0).get("period"));
        assertEquals(1L, rows.get(0).get("total"));
        assertEquals(0L, rows.get(0).get("published"));
        assertEquals("2026-10", rows.get(1).get("period"));
        assertEquals(2L, rows.get(1).get("total"));
        assertEquals(1L, rows.get(1).get("published"));
        assertEquals("2026-11", rows.get(2).get("period"));
        assertEquals(3L, rows.get(2).get("total"));
        assertEquals(1L, rows.get(2).get("published"));
    }

    @Test
    void templateValidation() {
        // 名称为空拒绝
        BriefingTemplate noName = newTemplate(null, "MONTHLY", "[\"工作概况\"]");
        assertThrows(IllegalArgumentException.class, () -> briefingTemplateService.create(noName));
        // 分类非法拒绝
        BriefingTemplate badCat = newTemplate("分类非法", "WEEKLY", "[\"工作概况\"]");
        assertThrows(IllegalArgumentException.class, () -> briefingTemplateService.create(badCat));
        // 栏目不是 JSON 数组拒绝
        BriefingTemplate badSections = newTemplate("栏目非法", "MONTHLY", "工作概况");
        assertThrows(IllegalArgumentException.class, () -> briefingTemplateService.create(badSections));
        // 栏目为空数组拒绝
        BriefingTemplate emptySections = newTemplate("栏目为空", "MONTHLY", "[]");
        assertThrows(IllegalArgumentException.class, () -> briefingTemplateService.create(emptySections));
        // 同名模板拒绝
        BriefingTemplate dup = newTemplate("月度医疗工作简报模板", "MONTHLY", "[\"工作概况\"]");
        assertThrows(IllegalArgumentException.class, () -> briefingTemplateService.create(dup));
    }

    @Test
    void templateEnableDisableFlow() {
        BriefingTemplate created = null;
        try {
            created = briefingTemplateService.create(newTemplate("冒烟模板", "SPECIAL", "[\"重点工作\"]"));
            assertEquals(Integer.valueOf(1), created.getStatus());
            BriefingTemplate disabled = briefingTemplateService.disable(created.getId());
            assertEquals(Integer.valueOf(0), disabled.getStatus());
            BriefingTemplate enabled = briefingTemplateService.enable(created.getId());
            assertEquals(Integer.valueOf(1), enabled.getStatus());
        } finally {
            if (created != null && created.getId() != null) {
                briefingTemplateService.removeById(created.getId());
            }
        }
    }

    @Test
    void templateDeleteGuard() {
        // 已被简报引用的模板不能删除
        assertThrows(IllegalArgumentException.class, () -> briefingTemplateService.delete(1L));
        // 未被引用的模板可正常删除
        BriefingTemplate created = briefingTemplateService.create(newTemplate("可删模板", "DYNAMIC", "[\"工作动态\"]"));
        briefingTemplateService.delete(created.getId());
        assertNull(briefingTemplateService.getById(created.getId()));
    }

    @Test
    void briefingValidation() {
        // 期号为空拒绝
        Briefing noIssue = newBriefing(null, "期号为空", "MONTHLY", "2026-11");
        assertThrows(IllegalArgumentException.class, () -> briefingService.create(noIssue));
        // 标题为空拒绝
        Briefing noTitle = newBriefing("TEST-000", null, "MONTHLY", "2026-11");
        assertThrows(IllegalArgumentException.class, () -> briefingService.create(noTitle));
        // 分类非法拒绝
        Briefing badCat = newBriefing("TEST-001", "分类非法", "WEEKLY", "2026-11");
        assertThrows(IllegalArgumentException.class, () -> briefingService.create(badCat));
        // 统计周期格式非法拒绝
        Briefing badPeriod = newBriefing("TEST-002", "周期非法", "MONTHLY", "2026-13");
        assertThrows(IllegalArgumentException.class, () -> briefingService.create(badPeriod));
        // 期号重复拒绝
        Briefing dup = newBriefing("2026-11-01", "期号重复", "MONTHLY", "2026-11");
        assertThrows(IllegalArgumentException.class, () -> briefingService.create(dup));
    }

    @Test
    void briefingUpdateGuard() {
        // 已发布的简报不能修改
        Briefing published = briefingService.getById(1L);
        published.setTitle("已发布后修改");
        assertThrows(IllegalArgumentException.class, () -> briefingService.update(published));
        // 草稿可以修改
        Briefing draft = briefingService.getById(4L);
        String oldTitle = draft.getTitle();
        try {
            draft.setTitle("草稿修改后标题");
            Briefing updated = briefingService.update(draft);
            assertEquals("草稿修改后标题", updated.getTitle());
            assertEquals(Integer.valueOf(0), updated.getStatus());
        } finally {
            Briefing restore = briefingService.getById(4L);
            restore.setTitle(oldTitle);
            briefingService.updateById(restore);
        }
    }

    @Test
    void briefingApproveFlow() {
        Briefing created = null;
        try {
            created = briefingService.create(newBriefing("TEST-APPR", "审批流转测试", "MONTHLY", "2026-12"));
            assertEquals(Integer.valueOf(0), created.getStatus());
            // 未提交不能审批
            Long id = created.getId();
            assertThrows(IllegalArgumentException.class, () -> briefingService.approve(id, "吴文斌", "同意"));
            // 提交：草稿 → 待审批
            Briefing submitted = briefingService.submit(id);
            assertEquals(Integer.valueOf(1), submitted.getStatus());
            // 无审批人拒绝
            assertThrows(IllegalArgumentException.class, () -> briefingService.approve(id, "", "同意"));
            // 审批通过：待审批 → 已发布，发布日期默认为当天，审批记录留痕
            Briefing approved = briefingService.approve(id, "吴文斌", "同意发布");
            assertEquals(Integer.valueOf(2), approved.getStatus());
            assertNotNull(approved.getPublishDate());
            assertTrue(approved.getApprovalHistory().contains("吴文斌"));
            // 已结束的简报不能再审批
            assertThrows(IllegalArgumentException.class, () -> briefingService.approve(id, "吴文斌", "同意"));
            assertThrows(IllegalArgumentException.class, () -> briefingService.reject(id, "吴文斌", "驳回"));
            // 已发布的简报不能删除
            assertThrows(IllegalArgumentException.class, () -> briefingService.delete(id));
        } finally {
            if (created != null && created.getId() != null) {
                briefingService.removeById(created.getId());
            }
        }
    }

    @Test
    void briefingRejectFlow() {
        Briefing created = null;
        try {
            created = briefingService.create(newBriefing("TEST-REJ", "驳回流转测试", "MONTHLY", "2026-12"));
            Long id = created.getId();
            briefingService.submit(id);
            // 无驳回原因拒绝
            assertThrows(IllegalArgumentException.class, () -> briefingService.reject(id, "吴文斌", ""));
            // 驳回：待审批 → 已驳回
            Briefing rejected = briefingService.reject(id, "吴文斌", "数据有误，请核对后重提");
            assertEquals(Integer.valueOf(3), rejected.getStatus());
            assertEquals("数据有误，请核对后重提", rejected.getRejectReason());
            // 已驳回可改回草稿重提
            Briefing edit = briefingService.getById(id);
            edit.setTitle("驳回后修改重提");
            Briefing updated = briefingService.update(edit);
            assertEquals(Integer.valueOf(0), updated.getStatus());
            assertNull(updated.getRejectReason());
            Briefing resubmitted = briefingService.submit(id);
            assertEquals(Integer.valueOf(1), resubmitted.getStatus());
        } finally {
            if (created != null && created.getId() != null) {
                briefingService.removeById(created.getId());
            }
        }
    }

    @Test
    void generateFromTemplate() {
        Briefing generated = null;
        try {
            GenerateBriefingRequest req = new GenerateBriefingRequest();
            req.setTemplateId(1L);
            req.setPeriod("2026-11");
            req.setAuthorName("医务科");
            generated = briefingService.generate(req);
            assertEquals(Integer.valueOf(0), generated.getStatus());
            // 2026-11 已有 01/02/03 三期，自动生成顺延为 04 期
            assertEquals("2026-11-04", generated.getIssueNo());
            assertEquals("月度医疗工作简报模板（2026-11）", generated.getTitle());
            String content = generated.getContent();
            assertNotNull(content);
            // 工作量：种子 8 条、门诊 3420、住院 1066、手术 241
            assertTrue(content.contains("全院在册医师 12 人"));
            assertTrue(content.contains("门诊 3420 人次、住院 1066 人次、手术 241 台"));
            // 质量检查：计划 4 个（已完成 1、进行中 1）、记录 3 条平均 80.33、问题 4 个严重 2 个
            assertTrue(content.contains("检查计划 4 个（已完成 1 个、进行中 1 个）"));
            assertTrue(content.contains("检查记录 3 条、平均分 80.33 分"));
            assertTrue(content.contains("发现问题 4 个，其中严重问题 2 个"));
            // MDT：申请 5 例（已通过 2 例，紧急 2 例）；新技术：11 月申报 3 项（已立项 1 项、已推广 0 项）
            assertTrue(content.contains("MDT 会诊申请 5 例（已通过 2 例，其中紧急会诊 2 例）"));
            assertTrue(content.contains("新技术、新项目申报 3 项（已立项 1 项、已推广 0 项）"));
            // 考核：2026 年度 6 人次、平均 81.17、优秀 2 人
            assertTrue(content.contains("本年度考核 6 人次，平均分 81.17 分，优秀 2 人"));
        } finally {
            if (generated != null && generated.getId() != null) {
                briefingService.removeById(generated.getId());
            }
        }
    }

    @Test
    void generateGuards() {
        // 停用的模板不能自动生成
        GenerateBriefingRequest disabled = new GenerateBriefingRequest();
        disabled.setTemplateId(3L);
        disabled.setPeriod("2026-11");
        assertThrows(IllegalArgumentException.class, () -> briefingService.generate(disabled));
        // 统计周期格式非法拒绝
        GenerateBriefingRequest badPeriod = new GenerateBriefingRequest();
        badPeriod.setTemplateId(1L);
        badPeriod.setPeriod("2026-13");
        assertThrows(IllegalArgumentException.class, () -> briefingService.generate(badPeriod));
        // 模板不存在拒绝
        GenerateBriefingRequest noTemplate = new GenerateBriefingRequest();
        noTemplate.setTemplateId(9999L);
        noTemplate.setPeriod("2026-11");
        assertThrows(IllegalArgumentException.class, () -> briefingService.generate(noTemplate));
        // 指定已存在的期号拒绝
        GenerateBriefingRequest dupIssue = new GenerateBriefingRequest();
        dupIssue.setTemplateId(1L);
        dupIssue.setPeriod("2026-11");
        dupIssue.setIssueNo("2026-11-01");
        assertThrows(IllegalArgumentException.class, () -> briefingService.generate(dupIssue));
    }

    @Test
    void exportWordIsRealDocx() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        briefingService.exportWord(1L, out);
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length > 1000, "Word 导出不应为空");
        // .docx 是 zip 包，以 PK 开头
        assertEquals((byte) 'P', bytes[0]);
        assertEquals((byte) 'K', bytes[1]);
    }

    @Test
    void exportPdfIsRealPdf() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        briefingService.exportPdf(1L, out);
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length > 1000, "PDF 导出不应为空");
        // 真实 PDF 以 %PDF- 开头
        assertEquals((byte) '%', bytes[0]);
        assertEquals((byte) 'P', bytes[1]);
        assertEquals((byte) 'D', bytes[2]);
        assertEquals((byte) 'F', bytes[3]);
    }

    @Test
    void downloadTextContainsTitleAndContent() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        briefingService.downloadText(1L, out);
        String text = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("医疗工作简报（2026年11月）"));
        assertTrue(text.contains("2026-11-01"));
        assertTrue(text.contains("门诊 3420 人次"));
    }

    @Test
    void excelRoundTrip() {
        Briefing temp = briefingService.create(newBriefing("TEST-RT", "导入导出往返测试", "MONTHLY", "2026-12"));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            briefingService.exportExcel(new com.yunzhongyi.medical.briefing.dto.BriefingQuery(), out);
            byte[] bytes = out.toByteArray();
            assertTrue(bytes.length > 1000);
            assertEquals((byte) 'P', bytes[0]);
            assertEquals((byte) 'K', bytes[1]);
            // 读回 7 行（6 期种子 + 临时行）
            List<Briefing> rows = BriefingExcelUtil.read(new ByteArrayInputStream(bytes));
            assertEquals(7, rows.size());
            // 删除临时行再导入补回 1 条；重复导入 0 条
            briefingService.removeById(temp.getId());
            Map<String, Object> first = briefingService.importExcel(new ByteArrayInputStream(bytes));
            assertEquals(1, first.get("created"));
            assertEquals(7L, briefingService.count());
            Map<String, Object> second = briefingService.importExcel(new ByteArrayInputStream(bytes));
            assertEquals(0, second.get("created"));
            assertEquals(7L, briefingService.count());
        } finally {
            // 按期号删掉往返导入产生的临时行，不动种子数据
            briefingService.remove(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Briefing>()
                    .eq(Briefing::getIssueNo, "TEST-RT"));
            assertEquals(6L, briefingService.count());
        }
    }

    @Test
    void importTemplateIsValidXlsx() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        briefingService.writeImportTemplate(out);
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length > 500);
        assertEquals((byte) 'P', bytes[0]);
        assertEquals((byte) 'K', bytes[1]);
        List<Briefing> rows = BriefingExcelUtil.read(new ByteArrayInputStream(bytes));
        assertEquals(0, rows.size());
    }

    @Test
    void briefingPageQueryFilters() {
        // 只看已发布：2 期
        com.yunzhongyi.medical.briefing.dto.BriefingQuery q =
                new com.yunzhongyi.medical.briefing.dto.BriefingQuery();
        q.setStatus(2);
        assertEquals(2L, briefingService.pageQuery(q).getTotal());
        // 按周期 2026-10：2 期
        q = new com.yunzhongyi.medical.briefing.dto.BriefingQuery();
        q.setPeriod("2026-10");
        assertEquals(2L, briefingService.pageQuery(q).getTotal());
        // 关键字“专项”：季报 1 期
        q = new com.yunzhongyi.medical.briefing.dto.BriefingQuery();
        q.setKeyword("专项");
        assertEquals(1L, briefingService.pageQuery(q).getTotal());
    }

    @Test
    void templatePageQueryFilters() {
        com.yunzhongyi.medical.briefing.dto.BriefingTemplateQuery q =
                new com.yunzhongyi.medical.briefing.dto.BriefingTemplateQuery();
        q.setStatus(0);
        assertEquals(1L, briefingTemplateService.pageQuery(q).getTotal());
        q = new com.yunzhongyi.medical.briefing.dto.BriefingTemplateQuery();
        q.setKeyword("月度");
        assertEquals(1L, briefingTemplateService.pageQuery(q).getTotal());
        assertEquals(2, briefingTemplateService.enabledList().size());
    }

    @Test
    void statsEndpointViaMockMvc() throws Exception {
        mockMvc.perform(get("/api/briefings/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.published").value(2));
        mockMvc.perform(get("/api/briefing-templates/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void deleteBatchSkipsPublished() {
        Briefing a = null;
        Briefing b = null;
        try {
            a = briefingService.create(newBriefing("TEST-B1", "批量删除甲", "MONTHLY", "2026-12"));
            b = briefingService.create(newBriefing("TEST-B2", "批量删除乙", "MONTHLY", "2026-12"));
            // 混入已发布简报时整批拒绝（已发布不可删），两条草稿应保留
            final Long aId = a.getId();
            final Long bId = b.getId();
            assertThrows(IllegalArgumentException.class,
                    () -> briefingService.deleteBatch(List.of(aId, 1L)));
            assertNotNull(briefingService.getById(aId));
            assertNotNull(briefingService.getById(bId));
            // 只删草稿成功
            briefingService.deleteBatch(List.of(aId, bId));
            assertNull(briefingService.getById(a.getId()));
            assertNull(briefingService.getById(b.getId()));
            a = null;
            b = null;
        } finally {
            if (a != null && a.getId() != null) {
                briefingService.removeById(a.getId());
            }
            if (b != null && b.getId() != null) {
                briefingService.removeById(b.getId());
            }
        }
    }

    // ---------- 辅助构造 ----------

    private BriefingTemplate newTemplate(String name, String category, String sections) {
        BriefingTemplate t = new BriefingTemplate();
        t.setName(name);
        t.setCategory(category);
        t.setSections(sections);
        t.setStatus(1);
        return t;
    }

    private Briefing newBriefing(String issueNo, String title, String category, String period) {
        Briefing b = new Briefing();
        b.setIssueNo(issueNo);
        b.setTitle(title);
        b.setCategory(category);
        b.setPeriod(period);
        b.setTemplateId(1L);
        b.setTemplateName("月度医疗工作简报模板");
        b.setContent("测试正文。");
        b.setAuthorName("医务科");
        return b;
    }
}
