package com.yunzhongyi.medical;

import com.yunzhongyi.medical.inspection.dto.InspectionCategoryQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionCategory;
import com.yunzhongyi.medical.inspection.entity.InspectionItem;
import com.yunzhongyi.medical.inspection.excel.InspectionCategoryExcelUtil;
import com.yunzhongyi.medical.inspection.service.InspectionCategoryService;
import com.yunzhongyi.medical.inspection.service.InspectionItemService;
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
 * 检查分类模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：12 个检查分类（一级 10 个，编码与模块 19 既有项目分类口径一致；
 * 二级 2 个，运行病历 / 终末病历，均挂在病历质量下）、
 * 全部启用；归属检查项目 12 个（8 个分类有项目、4 个分类无项目，
 * 其中病历质量 4 个、医疗安全 2 个，其余 1 个；已组装进模板 11 项、涉及 3 个模板）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class InspectionCategoryTests {

    @Autowired
    private InspectionCategoryService inspectionCategoryService;

    @Autowired
    private InspectionItemService inspectionItemService;

    @Test
    void contextLoads() {
        assertNotNull(inspectionCategoryService);
        assertNotNull(inspectionItemService);
    }

    @Test
    void seedDataLoaded() {
        assertEquals(12L, inspectionCategoryService.count());

        // 种子分类 1：病历质量，一级，通用，启用
        InspectionCategory c1 = inspectionCategoryService.getById(1L);
        assertEquals("MEDICAL_RECORD", c1.getCategoryCode());
        assertEquals("病历质量", c1.getCategoryName());
        assertNull(c1.getParentId());
        assertEquals(1, c1.getLevel());
        assertEquals("ALL", c1.getApplicableType());
        assertEquals(1, c1.getStatus());

        // 种子分类 5：护理质量，一级，暂无项目归属
        InspectionCategory c5 = inspectionCategoryService.getById(5L);
        assertEquals("NURSING", c5.getCategoryCode());
        assertEquals("护理质量", c5.getCategoryName());
        assertEquals(1, c5.getLevel());

        // 种子分类 11：运行病历，二级，挂在病历质量下，适用科室自查
        InspectionCategory c11 = inspectionCategoryService.getById(11L);
        assertEquals("MEDICAL_RECORD_RUNNING", c11.getCategoryCode());
        assertEquals("运行病历", c11.getCategoryName());
        assertEquals(1L, c11.getParentId());
        assertEquals(2, c11.getLevel());
        assertEquals("SELF", c11.getApplicableType());

        // 种子分类 12：终末病历，二级，适用专项检查
        InspectionCategory c12 = inspectionCategoryService.getById(12L);
        assertEquals("MEDICAL_RECORD_FINAL", c12.getCategoryCode());
        assertEquals("终末病历", c12.getCategoryName());
        assertEquals(1L, c12.getParentId());
        assertEquals(2, c12.getLevel());
        assertEquals("SPECIAL", c12.getApplicableType());
    }

    @Test
    void categoryStats() {
        Map<String, Object> stats = inspectionCategoryService.stats();
        assertEquals(12L, stats.get("total"));
        assertEquals(12L, stats.get("enabled"));
        assertEquals(0L, stats.get("disabled"));
        assertEquals(10L, stats.get("level1"));
        assertEquals(2L, stats.get("level2"));
        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) stats.get("byApplicableType");
        assertEquals(10L, byType.get("ALL"));
        assertEquals(1L, byType.get("SELF"));
        assertEquals(0L, byType.get("HOSPITAL"));
        assertEquals(1L, byType.get("SPECIAL"));
        assertEquals(12L, stats.get("itemTotal"));
        assertEquals(8L, stats.get("categoriesWithItems"));
        assertEquals(4L, stats.get("categoriesWithoutItems"));
        assertEquals(11L, stats.get("linkedItems"));
        assertEquals(3L, stats.get("templateCount"));
    }

    @Test
    void statsByCategory() {
        List<Map<String, Object>> list = inspectionCategoryService.statsByCategory();
        assertEquals(12, list.size());
        Map<String, Object> medicalRecord = list.stream()
                .filter(r -> "MEDICAL_RECORD".equals(r.get("categoryCode"))).findFirst().orElseThrow();
        assertEquals(4L, medicalRecord.get("itemCount"));
        assertEquals(4L, medicalRecord.get("enabledItemCount"));
        assertEquals(2L, medicalRecord.get("templateCount"));
        assertEquals(2L, medicalRecord.get("childCount"));
        Map<String, Object> nursing = list.stream()
                .filter(r -> "NURSING".equals(r.get("categoryCode"))).findFirst().orElseThrow();
        assertEquals(0L, nursing.get("itemCount"));
        assertEquals(0L, nursing.get("templateCount"));
        assertEquals(0L, nursing.get("childCount"));
        // 中医特色：1 个项目（已停用），未组装进模板
        Map<String, Object> tcm = list.stream()
                .filter(r -> "TCM".equals(r.get("categoryCode"))).findFirst().orElseThrow();
        assertEquals(1L, tcm.get("itemCount"));
        assertEquals(0L, tcm.get("enabledItemCount"));
        assertEquals(0L, tcm.get("templateCount"));
    }

    @Test
    void queryFilters() {
        InspectionCategoryQuery q = new InspectionCategoryQuery();
        q.setKeyword("病历");
        assertEquals(3, inspectionCategoryService.listByQuery(q).size());

        q = new InspectionCategoryQuery();
        q.setLevel(2);
        assertEquals(2, inspectionCategoryService.listByQuery(q).size());

        q = new InspectionCategoryQuery();
        q.setParentId(1L);
        assertEquals(2, inspectionCategoryService.listByQuery(q).size());

        q = new InspectionCategoryQuery();
        q.setApplicableType("SPECIAL");
        assertEquals(1, inspectionCategoryService.listByQuery(q).size());

        q = new InspectionCategoryQuery();
        q.setStatus(0);
        assertEquals(0, inspectionCategoryService.listByQuery(q).size());

        q = new InspectionCategoryQuery();
        q.setWithItems(true);
        assertEquals(8, inspectionCategoryService.listByQuery(q).size());

        q = new InspectionCategoryQuery();
        q.setWithoutItems(true);
        assertEquals(4, inspectionCategoryService.listByQuery(q).size());
    }

    @Test
    void validationAndDefaults() {
        // 重复分类编码拒绝
        InspectionCategory badDup = newCategory("MEDICAL_RECORD", "测试分类-编码重复");
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.save(badDup));

        // 同一上级下同名拒绝（顶级分类之间同名）
        InspectionCategory badName = newCategory("JCFL-T1", "病历质量");
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.save(badName));

        // 适用检查类型非法拒绝
        InspectionCategory badType = newCategory("JCFL-T1", "测试分类-类型非法");
        badType.setApplicableType("WRONG");
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.save(badType));

        // 上级不存在拒绝
        InspectionCategory badParent = newCategory("JCFL-T1", "测试分类-上级不存在");
        badParent.setParentId(9999L);
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.save(badParent));

        // 上级是二级分类拒绝（不建三级）
        InspectionCategory badLevel = newCategory("JCFL-T1", "测试分类-三级");
        badLevel.setParentId(11L);
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.save(badLevel));

        // 不同上级下同名允许（挂在核心制度下的“病历质量”）
        InspectionCategory okSameName = newCategory("JCFL-T1", "病历质量");
        okSameName.setParentId(2L);
        inspectionCategoryService.save(okSameName);
        try {
            assertEquals(2, inspectionCategoryService.getById(okSameName.getId()).getLevel());
        } finally {
            inspectionCategoryService.removeById(okSameName.getId());
        }

        // 默认值：默认启用、适用类型默认通用、层级按上级自动算
        InspectionCategory p = newCategory("JCFL-T1", "测试分类-默认值");
        p.setApplicableType(null);
        p.setStatus(null);
        inspectionCategoryService.save(p);
        try {
            InspectionCategory saved = inspectionCategoryService.getById(p.getId());
            assertEquals(1, saved.getStatus());
            assertEquals("ALL", saved.getApplicableType());
            assertEquals(1, saved.getLevel());
        } finally {
            inspectionCategoryService.removeById(p.getId());
        }
    }

    @Test
    void enableDisableAndDeleteGuards() {
        // 病历质量下有项目且已被模板引用：不可停用、不可删除（报错点明模板引用）
        IllegalStateException disableEx = assertThrows(IllegalStateException.class,
                () -> inspectionCategoryService.disable(1L));
        assertTrue(disableEx.getMessage().contains("模板"));
        IllegalStateException deleteEx = assertThrows(IllegalStateException.class,
                () -> inspectionCategoryService.removeById(1L));
        assertTrue(deleteEx.getMessage().contains("下级分类"));

        // 中医特色：项目未组装进模板，停用/删除报错只说项目归属、不提模板
        IllegalStateException tcmEx = assertThrows(IllegalStateException.class,
                () -> inspectionCategoryService.disable(8L));
        assertTrue(tcmEx.getMessage().contains("检查项目归属"));
        assertFalse(tcmEx.getMessage().contains("模板"));

        // 新建临时分类：停用→启用→删除全流程
        InspectionCategory p = newCategory("JCFL-T2", "测试分类-启停");
        inspectionCategoryService.save(p);
        try {
            InspectionCategory disabled = inspectionCategoryService.disable(p.getId());
            assertEquals(0, disabled.getStatus());
            assertThrows(IllegalStateException.class, () -> inspectionCategoryService.disable(p.getId()));
            InspectionCategory enabled = inspectionCategoryService.enable(p.getId());
            assertEquals(1, enabled.getStatus());
            assertThrows(IllegalStateException.class, () -> inspectionCategoryService.enable(p.getId()));
        } finally {
            inspectionCategoryService.removeById(p.getId());
        }
        assertNull(inspectionCategoryService.getById(p.getId()));

        // 无项目归属的临时分类可直接删除（种子分类 5 不删除，只验证其无项目）
        InspectionCategory unlinked = newCategory("JCFL-T4", "测试分类-无项目可删");
        inspectionCategoryService.save(unlinked);
        assertTrue(inspectionCategoryService.removeById(unlinked.getId()));
        assertNull(inspectionCategoryService.getById(unlinked.getId()));
    }

    @Test
    void parentGuards() {
        // 有下级的分类不可改为二级
        InspectionCategory c1 = inspectionCategoryService.getById(1L);
        c1.setParentId(2L);
        assertThrows(IllegalStateException.class, () -> inspectionCategoryService.updateById(c1));

        // 上级不能是自己
        InspectionCategory c11 = inspectionCategoryService.getById(11L);
        c11.setParentId(11L);
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.updateById(c11));

        // 上级不能是自己的下级（病历质量挂到运行病历下会成环）
        InspectionCategory c1Again = inspectionCategoryService.getById(1L);
        c1Again.setParentId(11L);
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.updateById(c1Again));

        // 上级是二级分类拒绝（临时二级分类挂到运行病历下）
        InspectionCategory temp = newCategory("JCFL-T5", "测试分类-挂二级");
        temp.setParentId(11L);
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.save(temp));

        // 校验失败不改动原数据
        assertNull(inspectionCategoryService.getById(1L).getParentId());
        assertEquals(1L, inspectionCategoryService.getById(11L).getParentId());
    }

    @Test
    void updateCodeGuard() {
        // 已有项目归属的分类，改编码拒绝
        InspectionCategory c1 = inspectionCategoryService.getById(1L);
        c1.setCategoryCode("MEDICAL_RECORD_NEW");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> inspectionCategoryService.updateById(c1));
        assertTrue(ex.getMessage().contains("编码不可修改"));

        // 改编码为已被占用的编码拒绝
        InspectionCategory c5 = inspectionCategoryService.getById(5L);
        c5.setCategoryCode("MEDICAL_RECORD");
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.updateById(c5));

        // 改名为同级已有的名称拒绝
        InspectionCategory c5Again = inspectionCategoryService.getById(5L);
        c5Again.setCategoryName("病历质量");
        assertThrows(IllegalArgumentException.class, () -> inspectionCategoryService.updateById(c5Again));

        // 无项目归属的临时分类改编码、改名都允许
        InspectionCategory p = newCategory("JCFL-T6", "测试分类-改编码");
        inspectionCategoryService.save(p);
        try {
            p.setCategoryCode("JCFL-T6-NEW");
            p.setCategoryName("测试分类-改编码后");
            inspectionCategoryService.updateById(p);
            InspectionCategory saved = inspectionCategoryService.getById(p.getId());
            assertEquals("JCFL-T6-NEW", saved.getCategoryCode());
            assertEquals("测试分类-改编码后", saved.getCategoryName());
        } finally {
            inspectionCategoryService.removeById(p.getId());
        }

        // 校验失败不改动原数据
        assertEquals("MEDICAL_RECORD", inspectionCategoryService.getById(1L).getCategoryCode());
        assertEquals("护理质量", inspectionCategoryService.getById(5L).getCategoryName());
    }

    @Test
    void parentDisableEnableGuards() {
        // 临时父子分类：父有启用下级时不可停用；父停用时子不可启用
        InspectionCategory parent = newCategory("JCFL-T7", "测试分类-父");
        inspectionCategoryService.save(parent);
        InspectionCategory child = newCategory("JCFL-T7-C", "测试分类-子");
        child.setParentId(parent.getId());
        inspectionCategoryService.save(child);
        try {
            assertThrows(IllegalStateException.class,
                    () -> inspectionCategoryService.disable(parent.getId()));
            inspectionCategoryService.disable(child.getId());
            assertEquals(0, inspectionCategoryService.getById(child.getId()).getStatus());
            InspectionCategory disabledParent = inspectionCategoryService.disable(parent.getId());
            assertEquals(0, disabledParent.getStatus());
            assertThrows(IllegalStateException.class,
                    () -> inspectionCategoryService.enable(child.getId()));
            inspectionCategoryService.enable(parent.getId());
            InspectionCategory enabledChild = inspectionCategoryService.enable(child.getId());
            assertEquals(1, enabledChild.getStatus());
        } finally {
            inspectionCategoryService.removeById(child.getId());
            inspectionCategoryService.removeById(parent.getId());
        }
        assertNull(inspectionCategoryService.getById(child.getId()));
        assertNull(inspectionCategoryService.getById(parent.getId()));
    }

    @Test
    void treeAndEnabled() {
        List<Map<String, Object>> tree = inspectionCategoryService.tree();
        assertEquals(10, tree.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> medicalRecord = tree.stream()
                .filter(n -> "MEDICAL_RECORD".equals(n.get("categoryCode"))).findFirst().orElseThrow();
        List<Map<String, Object>> children =
                (List<Map<String, Object>>) medicalRecord.get("children");
        assertEquals(2, children.size());
        assertEquals("运行病历", children.get(0).get("categoryName"));
        assertEquals("终末病历", children.get(1).get("categoryName"));
        // 无下级的分类 children 为空列表
        Map<String, Object> nursing = tree.stream()
                .filter(n -> "NURSING".equals(n.get("categoryCode"))).findFirst().orElseThrow();
        assertTrue(((List<?>) nursing.get("children")).isEmpty());

        List<InspectionCategory> enabled = inspectionCategoryService.enabled();
        assertEquals(12, enabled.size());
    }

    @Test
    void itemCategoryIntegration() {
        // 新建的二级分类编码可直接归检查项目（分类库为准，不只认 10 个硬编码）
        InspectionItem p = newItem("JCXM-T5", "测试项目-二级分类");
        p.setCategory("MEDICAL_RECORD_RUNNING");
        inspectionItemService.save(p);
        try {
            assertEquals("MEDICAL_RECORD_RUNNING",
                    inspectionItemService.getById(p.getId()).getCategory());
        } finally {
            inspectionItemService.removeById(p.getId());
        }

        // 已停用的分类不可再归检查项目
        InspectionCategory temp = newCategory("JCFL-T8", "测试分类-停用后归项目");
        inspectionCategoryService.save(temp);
        try {
            inspectionCategoryService.disable(temp.getId());
            InspectionItem bad = newItem("JCXM-T5", "测试项目-停用分类");
            bad.setCategory("JCFL-T8");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> inspectionItemService.save(bad));
            assertTrue(ex.getMessage().contains("已停用"));
            // 启用后又可归项目
            inspectionCategoryService.enable(temp.getId());
            InspectionItem ok = newItem("JCXM-T5", "测试项目-启用后归项目");
            ok.setCategory("JCFL-T8");
            inspectionItemService.save(ok);
            try {
                assertEquals("JCFL-T8", inspectionItemService.getById(ok.getId()).getCategory());
            } finally {
                inspectionItemService.removeById(ok.getId());
            }
        } finally {
            inspectionCategoryService.removeById(temp.getId());
        }
    }

    @Test
    void importExportRoundTrip() throws Exception {
        // 新建临时分类 → 导出全部 → 删除临时分类 → 导入补回 1 条 → 重复导入 0 条
        InspectionCategory p = newCategory("JCFL-T3", "测试分类-导入导出");
        p.setDescription("导入导出测试");
        inspectionCategoryService.save(p);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        inspectionCategoryService.exportExcel(new InspectionCategoryQuery(), out);
        assertTrue(out.size() > 0);
        List<InspectionCategory> exported =
                InspectionCategoryExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(exported.stream().anyMatch(c -> "JCFL-T3".equals(c.getCategoryCode())));
        // 导出含上级编码：二级分类导出后上级编码为 MEDICAL_RECORD
        assertTrue(exported.stream()
                .filter(c -> "MEDICAL_RECORD_RUNNING".equals(c.getCategoryCode()))
                .anyMatch(c -> "MEDICAL_RECORD".equals(c.getParentCode())));

        assertTrue(inspectionCategoryService.removeById(p.getId()));
        int imported = inspectionCategoryService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported);
        InspectionCategory restored = inspectionCategoryService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InspectionCategory>()
                        .eq(InspectionCategory::getCategoryCode, "JCFL-T3"));
        assertNotNull(restored);
        assertEquals("测试分类-导入导出", restored.getCategoryName());
        int dup = inspectionCategoryService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, dup);
        inspectionCategoryService.removeById(restored.getId());
    }

    // ---------- helpers ----------

    private InspectionCategory newCategory(String code, String name) {
        InspectionCategory c = new InspectionCategory();
        c.setCategoryCode(code);
        c.setCategoryName(name);
        c.setParentId(null);
        c.setApplicableType("ALL");
        c.setDescription("测试分类");
        c.setStatus(1);
        c.setSortOrder(99);
        return c;
    }

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
}
