package com.yunzhongyi.medical;

import com.yunzhongyi.medical.inspection.dto.InspectionItemQuery;
import com.yunzhongyi.medical.inspection.dto.InspectionStandardQuery;
import com.yunzhongyi.medical.inspection.dto.TemplateItemConfigRequest;
import com.yunzhongyi.medical.inspection.entity.InspectionItem;
import com.yunzhongyi.medical.inspection.entity.InspectionStandard;
import com.yunzhongyi.medical.inspection.entity.InspectionTemplateItem;
import com.yunzhongyi.medical.inspection.excel.InspectionItemExcelUtil;
import com.yunzhongyi.medical.inspection.excel.InspectionStandardExcelUtil;
import com.yunzhongyi.medical.inspection.excel.InspectionTemplateItemExcelUtil;
import com.yunzhongyi.medical.inspection.service.InspectionItemService;
import com.yunzhongyi.medical.inspection.service.InspectionStandardService;
import com.yunzhongyi.medical.inspection.service.InspectionTemplateItemService;
import com.yunzhongyi.medical.inspection.service.InspectionTypeUtil;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.mapper.QualityTemplateMapper;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
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
 * 检查维护模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：12 个检查项目（病历质量 4 / 核心制度 1 / 医院感染 1 / 药事管理 1 /
 * 医疗安全 2 / 服务质量 1 / 科室管理 1 / 中医特色 1，启用 11 / 停用 1，
 * 已组装进模板 11 项、未组装 1 项）、
 * 6 条检查标准（国家 4 / 省 1 / 院内 1，现行 5 / 已废止 1，
 * 30 天内到期 1 部、已过期 1 部、被检查项目引用 4 部）、
 * 11 条模板检查项配置（科室自查模板 4 项、院级质量检查模板 4 项、
 * 病历专项检查模板 3 项，权重之和均为 100）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class InspectionTests {

    @Autowired
    private InspectionItemService inspectionItemService;

    @Autowired
    private InspectionStandardService inspectionStandardService;

    @Autowired
    private InspectionTemplateItemService inspectionTemplateItemService;

    @Autowired
    private QualityTemplateMapper qualityTemplateMapper;

    @Test
    void contextLoads() {
        assertNotNull(inspectionItemService);
        assertNotNull(inspectionStandardService);
        assertNotNull(inspectionTemplateItemService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(inspectionItemService.count() >= 12);
        assertTrue(inspectionStandardService.count() >= 6);
        assertTrue(inspectionTemplateItemService.count() >= 11);

        // 种子项目 1：病历书写规范，病历质量，抽查病历，依据病历书写基本规范，权重 30，科室自查，启用
        InspectionItem i1 = inspectionItemService.getById(1L);
        assertEquals("病历书写规范", i1.getItemName());
        assertEquals("JCXM-001", i1.getItemNo());
        assertEquals("MEDICAL_RECORD", i1.getCategory());
        assertEquals("RECORD_REVIEW", i1.getCheckMethod());
        assertEquals(2L, i1.getStandardId());
        assertEquals("JCBZ-002", i1.getStandardNo());
        assertEquals("运行病历书写及时、完整、规范", i1.getStandardText());
        assertEquals(100, i1.getMaxScore());
        assertEquals(0, BigDecimal.valueOf(30).compareTo(i1.getWeight()));
        assertEquals("SELF", i1.getApplicableType());
        assertEquals(1, i1.getStatus());

        // 种子项目 12：中医辨证论治规范，中医特色，已停用，未组装进模板
        InspectionItem i12 = inspectionItemService.getById(12L);
        assertEquals("中医辨证论治规范", i12.getItemName());
        assertEquals("TCM", i12.getCategory());
        assertEquals(0, i12.getStatus());

        // 种子标准 2：病历书写基本规范，国家，2026-12-20 到期（30 天内到期），现行
        InspectionStandard s2 = inspectionStandardService.getById(2L);
        assertEquals("病历书写基本规范", s2.getStandardName());
        assertEquals("JCBZ-002", s2.getStandardNo());
        assertEquals("NATIONAL", s2.getLevel());
        assertEquals(LocalDate.of(2026, 12, 20), s2.getExpiryDate());
        assertEquals(1, s2.getStatus());

        // 种子标准 5：院内医疗质量检查实施细则，已废止
        InspectionStandard s5 = inspectionStandardService.getById(5L);
        assertEquals(0, s5.getStatus());
        assertTrue(s5.getAbolishReason().contains("新版"));

        // 种子配置：科室自查模板 4 项，权重之和 100
        List<InspectionTemplateItem> links1 = inspectionTemplateItemService.listByTemplate(1L);
        assertEquals(4, links1.size());
        BigDecimal sum = links1.stream().map(InspectionTemplateItem::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, BigDecimal.valueOf(100).compareTo(sum));
        assertEquals("病历书写规范", links1.get(0).getItemName());
    }

    @Test
    void itemStats() {
        Map<String, Object> stats = inspectionItemService.stats();
        assertEquals(12L, stats.get("total"));
        assertEquals(11L, stats.get("enabled"));
        assertEquals(1L, stats.get("disabled"));
        assertEquals(11L, stats.get("linked"));
        assertEquals(1L, stats.get("unlinked"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byCategory = (Map<String, Long>) stats.get("byCategory");
        assertEquals(4L, byCategory.get("MEDICAL_RECORD"));
        assertEquals(1L, byCategory.get("CORE_SYSTEM"));
        assertEquals(1L, byCategory.get("INFECTION"));
        assertEquals(1L, byCategory.get("PHARMACY"));
        assertEquals(2L, byCategory.get("SAFETY"));
        assertEquals(1L, byCategory.get("SERVICE"));
        assertEquals(1L, byCategory.get("MGMT"));
        assertEquals(1L, byCategory.get("TCM"));
        assertEquals(0L, byCategory.get("NURSING"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byMethod = (Map<String, Long>) stats.get("byMethod");
        assertEquals(6L, byMethod.get("RECORD_REVIEW"));
        assertEquals(3L, byMethod.get("ON_SITE"));
        assertEquals(2L, byMethod.get("DOC_REVIEW"));
        assertEquals(1L, byMethod.get("INTERVIEW"));
        List<Map<String, Object>> byCategoryList = inspectionItemService.statsByCategory();
        assertEquals(10, byCategoryList.size());
        assertEquals(4L, byCategoryList.get(0).get("count"));
        assertEquals(4L, byCategoryList.get(0).get("enabledCount"));
    }

    @Test
    void standardStats() {
        Map<String, Object> stats = inspectionStandardService.stats();
        assertEquals(6L, stats.get("total"));
        assertEquals(5L, stats.get("current"));
        assertEquals(1L, stats.get("abolished"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byLevel = (Map<String, Long>) stats.get("byLevel");
        assertEquals(4L, byLevel.get("NATIONAL"));
        assertEquals(1L, byLevel.get("PROVINCE"));
        assertEquals(0L, byLevel.get("CITY"));
        assertEquals(1L, byLevel.get("HOSPITAL"));
        assertEquals(1L, stats.get("expiringSoon"));
        assertEquals(1L, stats.get("expired"));
        assertEquals(4L, stats.get("referenced"));
        List<Map<String, Object>> byLevelList = inspectionStandardService.statsByLevel();
        assertEquals(4, byLevelList.size());
        assertEquals(4L, byLevelList.get(0).get("count"));
        assertEquals(4L, byLevelList.get(0).get("currentCount"));
    }

    @Test
    void templateItemStats() {
        Map<String, Object> stats = inspectionTemplateItemService.stats();
        assertEquals(3L, stats.get("templatesConfigured"));
        assertEquals(11L, stats.get("totalLinks"));
        assertEquals(0, new BigDecimal("3.67").compareTo((BigDecimal) stats.get("avgItemsPerTemplate")));
        assertEquals(3L, stats.get("templateTotal"));
        assertEquals(0L, stats.get("uncoveredTemplates"));
        assertEquals(12L, stats.get("itemTotal"));
        assertEquals(11L, stats.get("linkedItems"));
        assertEquals(1L, stats.get("unlinkedItems"));
    }

    @Test
    void itemQueryFilters() {
        InspectionItemQuery q = new InspectionItemQuery();
        q.setKeyword("病历");
        assertEquals(4, inspectionItemService.listByQuery(q).size());

        q = new InspectionItemQuery();
        q.setCategory("MEDICAL_RECORD");
        assertEquals(4, inspectionItemService.listByQuery(q).size());

        q = new InspectionItemQuery();
        q.setCheckMethod("INTERVIEW");
        assertEquals(1, inspectionItemService.listByQuery(q).size());

        q = new InspectionItemQuery();
        q.setStandardId(2L);
        assertEquals(4, inspectionItemService.listByQuery(q).size());

        q = new InspectionItemQuery();
        q.setApplicableType("SPECIAL");
        assertEquals(3, inspectionItemService.listByQuery(q).size());

        q = new InspectionItemQuery();
        q.setStatus(0);
        assertEquals(1, inspectionItemService.listByQuery(q).size());

        q = new InspectionItemQuery();
        q.setLinked(true);
        assertEquals(11, inspectionItemService.listByQuery(q).size());

        q = new InspectionItemQuery();
        q.setUnlinked(true);
        assertEquals(1, inspectionItemService.listByQuery(q).size());
        assertEquals("中医辨证论治规范", inspectionItemService.listByQuery(q).get(0).getItemName());
    }

    @Test
    void standardQueryFilters() {
        InspectionStandardQuery q = new InspectionStandardQuery();
        q.setKeyword("病历");
        assertEquals(1, inspectionStandardService.listByQuery(q).size());

        q = new InspectionStandardQuery();
        q.setLevel("NATIONAL");
        assertEquals(4, inspectionStandardService.listByQuery(q).size());

        q = new InspectionStandardQuery();
        q.setStatus(0);
        assertEquals(1, inspectionStandardService.listByQuery(q).size());

        q = new InspectionStandardQuery();
        q.setExpiringSoon(true);
        assertEquals(1, inspectionStandardService.listByQuery(q).size());
        assertEquals("病历书写基本规范",
                inspectionStandardService.listByQuery(q).get(0).getStandardName());

        q = new InspectionStandardQuery();
        q.setExpired(true);
        assertEquals(1, inspectionStandardService.listByQuery(q).size());
        assertEquals("抗菌药物临床应用管理办法",
                inspectionStandardService.listByQuery(q).get(0).getStandardName());
    }

    @Test
    void itemValidationAndDefaults() {
        // 分类非法拒绝
        InspectionItem badCategory = newItem("JCXM-T1", "测试项目-分类非法");
        badCategory.setCategory("WRONG");
        assertThrows(IllegalArgumentException.class, () -> inspectionItemService.save(badCategory));

        // 满分 0 拒绝
        InspectionItem badScore = newItem("JCXM-T1", "测试项目-满分非法");
        badScore.setMaxScore(0);
        assertThrows(IllegalArgumentException.class, () -> inspectionItemService.save(badScore));

        // 默认权重 0 拒绝
        InspectionItem badWeight = newItem("JCXM-T1", "测试项目-权重非法");
        badWeight.setWeight(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> inspectionItemService.save(badWeight));

        // 依据标准不存在拒绝
        InspectionItem badStandard = newItem("JCXM-T1", "测试项目-标准不存在");
        badStandard.setStandardId(9999L);
        assertThrows(IllegalArgumentException.class, () -> inspectionItemService.save(badStandard));

        // 重复项目编号拒绝
        InspectionItem badDup = newItem("JCXM-001", "测试项目-编号重复");
        assertThrows(IllegalArgumentException.class, () -> inspectionItemService.save(badDup));

        // 默认值：满分 100、默认权重 10、适用类型通用、启用；依据标准自动补全编号
        InspectionItem p = newItem("JCXM-T1", "测试项目-默认值");
        p.setMaxScore(null);
        p.setWeight(null);
        p.setApplicableType(null);
        p.setStatus(null);
        p.setStandardId(1L);
        p.setStandardNo(null);
        inspectionItemService.save(p);
        try {
            InspectionItem saved = inspectionItemService.getById(p.getId());
            assertEquals(100, saved.getMaxScore());
            assertEquals(0, BigDecimal.valueOf(10).compareTo(saved.getWeight()));
            assertEquals("ALL", saved.getApplicableType());
            assertEquals(1, saved.getStatus());
            assertEquals("JCBZ-001", saved.getStandardNo());
        } finally {
            inspectionItemService.removeById(p.getId());
        }
    }

    @Test
    void itemEnableDisableAndDeleteGuards() {
        // 已被模板引用的项目不可停用、不可删除
        assertThrows(IllegalStateException.class, () -> inspectionItemService.disable(1L));
        assertThrows(IllegalStateException.class, () -> inspectionItemService.removeById(1L));

        // 新建临时项目：停用→启用→删除全流程
        InspectionItem p = newItem("JCXM-T2", "测试项目-启停");
        inspectionItemService.save(p);
        try {
            InspectionItem disabled = inspectionItemService.disable(p.getId());
            assertEquals(0, disabled.getStatus());
            assertThrows(IllegalStateException.class, () -> inspectionItemService.disable(p.getId()));
            InspectionItem enabled = inspectionItemService.enable(p.getId());
            assertEquals(1, enabled.getStatus());
            assertThrows(IllegalStateException.class, () -> inspectionItemService.enable(p.getId()));
        } finally {
            inspectionItemService.removeById(p.getId());
        }
        assertNull(inspectionItemService.getById(p.getId()));

        // 未被模板引用的临时项目可直接删除（种子项目 12 不删除，只验证其未被引用）
        InspectionItem unlinked = newItem("JCXM-T4", "测试项目-未引用可删");
        inspectionItemService.save(unlinked);
        assertTrue(inspectionItemService.removeById(unlinked.getId()));
        assertNull(inspectionItemService.getById(unlinked.getId()));
        assertEquals(0, inspectionItemService.getById(12L).getStatus());
    }

    @Test
    void standardValidationAndDefaults() {
        // 级别非法拒绝
        InspectionStandard badLevel = newStandard("JCBZ-T1", "测试标准-级别非法");
        badLevel.setLevel("WRONG");
        assertThrows(IllegalArgumentException.class, () -> inspectionStandardService.save(badLevel));

        // 有效期早于生效日期拒绝
        InspectionStandard badDate = newStandard("JCBZ-T1", "测试标准-日期非法");
        badDate.setEffectiveDate(LocalDate.of(2026, 1, 1));
        badDate.setExpiryDate(LocalDate.of(2025, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> inspectionStandardService.save(badDate));

        // 重复标准编号拒绝
        InspectionStandard badDup = newStandard("JCBZ-001", "测试标准-编号重复");
        assertThrows(IllegalArgumentException.class, () -> inspectionStandardService.save(badDup));

        // 默认值：默认现行
        InspectionStandard p = newStandard("JCBZ-T1", "测试标准-默认值");
        p.setStatus(null);
        inspectionStandardService.save(p);
        try {
            assertEquals(1, inspectionStandardService.getById(p.getId()).getStatus());
        } finally {
            inspectionStandardService.removeById(p.getId());
        }
    }

    @Test
    void standardAbolishRestoreAndDeleteGuards() {
        // 废止无原因拒绝
        assertThrows(IllegalArgumentException.class,
                () -> inspectionStandardService.abolish(4L, null));
        assertThrows(IllegalArgumentException.class,
                () -> inspectionStandardService.abolish(4L, "  "));

        // 废止→再次废止拒绝→恢复→再次恢复拒绝（用未被引用的省级标准 4，避免影响种子统计）
        InspectionStandard abolished = inspectionStandardService.abolish(4L, "测试废止");
        assertEquals(0, abolished.getStatus());
        assertEquals("测试废止", abolished.getAbolishReason());
        assertThrows(IllegalStateException.class,
                () -> inspectionStandardService.abolish(4L, "再次废止"));
        InspectionStandard restored = inspectionStandardService.restore(4L);
        assertEquals(1, restored.getStatus());
        assertNull(restored.getAbolishReason());
        assertThrows(IllegalStateException.class,
                () -> inspectionStandardService.restore(4L));

        // 已废止的标准不能再废止
        assertThrows(IllegalStateException.class,
                () -> inspectionStandardService.abolish(5L, "再次废止"));

        // 已被检查项目引用的标准不可删除
        assertThrows(IllegalStateException.class, () -> inspectionStandardService.removeById(1L));

        // 新建临时标准可删除
        InspectionStandard p = newStandard("JCBZ-T2", "测试标准-删除");
        inspectionStandardService.save(p);
        assertTrue(inspectionStandardService.removeById(p.getId()));
        assertNull(inspectionStandardService.getById(p.getId()));
    }

    @Test
    void templateConfigureAndJsonSync() {
        // 把科室自查模板改配为病历三项（40/30/30），模板 JSON 同步更新
        TemplateItemConfigRequest req = configRequest(1L,
                new long[]{9L, 10L, 11L}, new int[]{40, 30, 30});
        List<InspectionTemplateItem> saved = inspectionTemplateItemService.configure(req);
        assertEquals(3, saved.size());
        QualityTemplate t1 = qualityTemplateMapper.selectById(1L);
        List<Map<String, Object>> items = QualityTypeUtil.parseCheckItems(t1.getCheckItems());
        assertEquals(3, items.size());
        assertEquals("病历完整性", items.get(0).get("name"));
        assertEquals(40, ((Number) items.get(0).get("weight")).intValue());
        assertEquals("首页、病程记录、检查检验单齐全", items.get(0).get("standard"));
        assertEquals(0, QualityTypeUtil.sumWeights(t1.getCheckItems())
                .compareTo(BigDecimal.valueOf(100)));

        // 恢复种子配置（30/30/20/20）
        inspectionTemplateItemService.configure(configRequest(1L,
                new long[]{1L, 2L, 3L, 4L}, new int[]{30, 30, 20, 20}));
        assertEquals(4, inspectionTemplateItemService.listByTemplate(1L).size());
        QualityTemplate restored = qualityTemplateMapper.selectById(1L);
        assertEquals(4, QualityTypeUtil.parseCheckItems(restored.getCheckItems()).size());
        assertEquals("病历书写规范",
                QualityTypeUtil.parseCheckItems(restored.getCheckItems()).get(0).get("name"));
    }

    @Test
    void templateConfigureValidation() {
        // 权重之和不为 100 拒绝
        assertThrows(IllegalArgumentException.class, () -> inspectionTemplateItemService.configure(
                configRequest(1L, new long[]{1L, 2L}, new int[]{30, 30})));
        // 同一模板内项目重复拒绝
        assertThrows(IllegalArgumentException.class, () -> inspectionTemplateItemService.configure(
                configRequest(1L, new long[]{1L, 1L}, new int[]{50, 50})));
        // 停用项目拒绝（种子项目 12 已停用）
        assertThrows(IllegalArgumentException.class, () -> inspectionTemplateItemService.configure(
                configRequest(1L, new long[]{12L}, new int[]{100})));
        // 项目不存在拒绝
        assertThrows(IllegalArgumentException.class, () -> inspectionTemplateItemService.configure(
                configRequest(1L, new long[]{9999L}, new int[]{100})));
        // 模板不存在拒绝
        assertThrows(IllegalArgumentException.class, () -> inspectionTemplateItemService.configure(
                configRequest(9999L, new long[]{1L}, new int[]{100})));
        // 检查项为空拒绝
        TemplateItemConfigRequest empty = new TemplateItemConfigRequest();
        empty.setTemplateId(1L);
        empty.setItems(List.of());
        assertThrows(IllegalArgumentException.class,
                () -> inspectionTemplateItemService.configure(empty));
        // 权重为 0 拒绝
        assertThrows(IllegalArgumentException.class, () -> inspectionTemplateItemService.configure(
                configRequest(1L, new long[]{1L}, new int[]{0})));

        // 校验失败不改动原配置
        assertEquals(4, inspectionTemplateItemService.listByTemplate(1L).size());
    }

    @Test
    void templateRemoveSingleAndLastGuard() {
        // 病历专项模板先改配为单项（权重 100），删最后一项被拒绝
        inspectionTemplateItemService.configure(
                configRequest(3L, new long[]{9L}, new int[]{100}));
        List<InspectionTemplateItem> single = inspectionTemplateItemService.listByTemplate(3L);
        assertEquals(1, single.size());
        assertThrows(IllegalStateException.class,
                () -> inspectionTemplateItemService.removeById(single.get(0).getId()));
        assertEquals(1, inspectionTemplateItemService.listByTemplate(3L).size());

        // 恢复为三项，再删一项：剩余两项，模板 JSON 同步为两项
        inspectionTemplateItemService.configure(configRequest(3L,
                new long[]{9L, 10L, 11L}, new int[]{40, 30, 30}));
        List<InspectionTemplateItem> three = inspectionTemplateItemService.listByTemplate(3L);
        assertEquals(3, three.size());
        assertTrue(inspectionTemplateItemService.removeById(three.get(2).getId()));
        assertEquals(2, inspectionTemplateItemService.listByTemplate(3L).size());
        QualityTemplate t3 = qualityTemplateMapper.selectById(3L);
        assertEquals(2, QualityTypeUtil.parseCheckItems(t3.getCheckItems()).size());

        // 恢复种子配置
        inspectionTemplateItemService.configure(configRequest(3L,
                new long[]{9L, 10L, 11L}, new int[]{40, 30, 30}));
        assertEquals(3, inspectionTemplateItemService.listByTemplate(3L).size());
    }

    @Test
    void templateItemImportExportRoundTrip() throws Exception {
        // 导出病历专项模板配置 → 改配为单项 → 导入恢复三项
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        inspectionTemplateItemService.exportExcel(3L, out);
        assertTrue(out.size() > 0);
        List<InspectionTemplateItem> exported =
                InspectionTemplateItemExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(3, exported.size());
        assertEquals("JCXM-009", exported.get(0).getItemNo());

        inspectionTemplateItemService.configure(
                configRequest(3L, new long[]{9L}, new int[]{100}));
        assertEquals(1, inspectionTemplateItemService.listByTemplate(3L).size());

        int imported = inspectionTemplateItemService.importExcel(3L,
                new ByteArrayInputStream(out.toByteArray()));
        assertEquals(3, imported);
        assertEquals(3, inspectionTemplateItemService.listByTemplate(3L).size());
        QualityTemplate t3 = qualityTemplateMapper.selectById(3L);
        assertEquals(0, QualityTypeUtil.sumWeights(t3.getCheckItems())
                .compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    void itemImportExportRoundTrip() throws Exception {
        // 新建临时项目 → 导出全部 → 删除临时项目 → 导入补回 1 条 → 重复导入 0 条
        InspectionItem p = newItem("JCXM-T3", "测试项目-导入导出");
        inspectionItemService.save(p);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        inspectionItemService.exportExcel(new InspectionItemQuery(), out);
        assertTrue(out.size() > 0);
        List<InspectionItem> exported =
                InspectionItemExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(exported.stream().anyMatch(i -> "JCXM-T3".equals(i.getItemNo())));

        assertTrue(inspectionItemService.removeById(p.getId()));
        int imported = inspectionItemService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported);
        InspectionItem restored = inspectionItemService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InspectionItem>()
                        .eq(InspectionItem::getItemNo, "JCXM-T3"));
        assertNotNull(restored);
        assertEquals("测试项目-导入导出", restored.getItemName());
        int dup = inspectionItemService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, dup);
        inspectionItemService.removeById(restored.getId());
    }

    @Test
    void standardImportExportRoundTrip() throws Exception {
        // 新建临时标准 → 导出全部 → 删除临时标准 → 导入补回 1 条 → 重复导入 0 条
        InspectionStandard p = newStandard("JCBZ-T3", "测试标准-导入导出");
        inspectionStandardService.save(p);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        inspectionStandardService.exportExcel(new InspectionStandardQuery(), out);
        assertTrue(out.size() > 0);
        List<InspectionStandard> exported =
                InspectionStandardExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(exported.stream().anyMatch(s -> "JCBZ-T3".equals(s.getStandardNo())));

        assertTrue(inspectionStandardService.removeById(p.getId()));
        int imported = inspectionStandardService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported);
        InspectionStandard restored = inspectionStandardService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InspectionStandard>()
                        .eq(InspectionStandard::getStandardNo, "JCBZ-T3"));
        assertNotNull(restored);
        assertEquals("测试标准-导入导出", restored.getStandardName());
        int dup = inspectionStandardService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, dup);
        inspectionStandardService.removeById(restored.getId());
    }

    // ---------- helpers ----------

    private InspectionItem newItem(String itemNo, String itemName) {
        InspectionItem p = new InspectionItem();
        p.setItemNo(itemNo);
        p.setItemName(itemName);
        p.setCategory("MEDICAL_RECORD");
        p.setCheckMethod("RECORD_REVIEW");
        p.setStandardText("测试评分标准");
        p.setMaxScore(100);
        p.setWeight(BigDecimal.valueOf(10));
        p.setApplicableType("ALL");
        p.setStatus(1);
        p.setSortOrder(99);
        return p;
    }

    private InspectionStandard newStandard(String standardNo, String standardName) {
        InspectionStandard p = new InspectionStandard();
        p.setStandardNo(standardNo);
        p.setStandardName(standardName);
        p.setLevel("HOSPITAL");
        p.setSourceDoc("测试依据文件");
        p.setContent("测试标准内容");
        p.setApplicableScope("全院");
        p.setEffectiveDate(LocalDate.of(2026, 1, 1));
        p.setExpiryDate(LocalDate.of(2027, 12, 31));
        p.setStatus(1);
        return p;
    }

    private TemplateItemConfigRequest configRequest(Long templateId, long[] itemIds, int[] weights) {
        TemplateItemConfigRequest req = new TemplateItemConfigRequest();
        req.setTemplateId(templateId);
        List<TemplateItemConfigRequest.TemplateItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < itemIds.length; i++) {
            TemplateItemConfigRequest.TemplateItem cfg = new TemplateItemConfigRequest.TemplateItem();
            cfg.setItemId(itemIds[i]);
            cfg.setWeight(BigDecimal.valueOf(weights[i]));
            cfg.setSortOrder(i + 1);
            items.add(cfg);
        }
        req.setItems(items);
        return req;
    }
}
