package com.yunzhongyi.medical;

import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.service.QualityTemplateService;
import com.yunzhongyi.medical.templatelib.entity.TemplateVersion;
import com.yunzhongyi.medical.templatelib.excel.TemplateLibraryExcelUtil;
import com.yunzhongyi.medical.templatelib.service.TemplateLibraryService;
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
 * 检查模板管理（模板库）模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：3 个检查模板（科室自查 / 院级质量检查 / 病历专项检查，全部启用）、
 * 各 1 个 V1.0 当前版本快照；检查计划 4 条（模板 1：待执行 1、进行中 1；
 * 模板 2：已完成 1；模板 3：待执行 1）、检查记录 3 条（模板 1 两条、模板 2 一条）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class TemplateLibraryTests {

    @Autowired
    private TemplateLibraryService templateLibraryService;

    @Autowired
    private QualityTemplateService templateService;

    private static final String ITEMS_2_ORIGINAL = "[{\"name\":\"医疗质量\",\"weight\":35,\"standard\":\"诊疗规范、病历质量、三级查房\"},{\"name\":\"医疗安全\",\"weight\":30,\"standard\":\"不良事件上报、患者身份识别、危急值管理\"},{\"name\":\"服务质量\",\"weight\":20,\"standard\":\"服务态度、患者满意度、投诉处理\"},{\"name\":\"科室管理\",\"weight\":15,\"standard\":\"科室制度、人员培训、设备管理\"}]";

    private static final String ITEMS_2_CHANGED = "[{\"name\":\"医疗质量\",\"weight\":40,\"standard\":\"诊疗规范、病历质量、三级查房\"},{\"name\":\"医疗安全\",\"weight\":30,\"standard\":\"不良事件上报、患者身份识别、危急值管理\"},{\"name\":\"服务质量\",\"weight\":20,\"standard\":\"服务态度、患者满意度、投诉处理\"},{\"name\":\"科室管理\",\"weight\":10,\"standard\":\"科室制度、人员培训、设备管理\"}]";

    @Test
    void contextLoads() {
        assertNotNull(templateLibraryService);
        assertNotNull(templateService);
    }

    @Test
    void seedDataLoaded() {
        assertEquals(3L, templateService.count());
        assertEquals(3L, templateLibraryService.count());
        // 3 个种子模板各 1 个 V1.0 当前版本
        for (long templateId = 1; templateId <= 3; templateId++) {
            List<TemplateVersion> versions = templateLibraryService.listVersions(templateId);
            assertEquals(1, versions.size());
            assertEquals("V1.0", versions.get(0).getVersionNo());
            assertEquals(1, versions.get(0).getStatus());
        }
        TemplateVersion current = templateLibraryService.currentVersion(1L);
        assertNotNull(current);
        assertEquals("V1.0", current.getVersionNo());
        assertTrue(current.getCheckItems().contains("病历书写规范"));
    }

    @Test
    void createVersionValidation() {
        // 权重之和不是 100 拒绝
        assertThrows(IllegalArgumentException.class, () ->
                templateLibraryService.createVersion(2L,
                        "[{\"name\":\"医疗质量\",\"weight\":50,\"standard\":\"x\"}]", "权重测试"));
        // 空检查项拒绝
        assertThrows(IllegalArgumentException.class, () ->
                templateLibraryService.createVersion(2L, "[]", "空项测试"));
        // 模板不存在拒绝
        assertThrows(IllegalArgumentException.class, () ->
                templateLibraryService.createVersion(999L, ITEMS_2_CHANGED, "不存在模板"));
        // 有进行中计划的模板（模板 1：待执行 + 进行中）不允许改版
        assertThrows(IllegalArgumentException.class, () ->
                templateLibraryService.createVersion(1L, ITEMS_2_CHANGED, "有进行中计划"));
        // 有待执行计划的模板 3 同样不允许改版
        assertThrows(IllegalArgumentException.class, () ->
                templateLibraryService.createVersion(3L, ITEMS_2_CHANGED, "有待执行计划"));
        // 种子模板 2 的检查项未被改动
        assertEquals(ITEMS_2_ORIGINAL, templateService.getById(2L).getCheckItems());
    }

    @Test
    void createVersionSuccess() {
        TemplateVersion created = null;
        try {
            created = templateLibraryService.createVersion(2L, ITEMS_2_CHANGED, "医疗质量权重上调");
            assertEquals("V2.0", created.getVersionNo());
            assertEquals(1, created.getStatus());
            assertEquals(ITEMS_2_CHANGED, created.getCheckItems());
            // 模板检查项已回写为新内容
            assertEquals(ITEMS_2_CHANGED, templateService.getById(2L).getCheckItems());
            // 旧 V1.0 改为历史版本
            List<TemplateVersion> versions = templateLibraryService.listVersions(2L);
            assertEquals(2, versions.size());
            TemplateVersion v1 = versions.stream().filter(v -> "V1.0".equals(v.getVersionNo())).findFirst().orElseThrow();
            assertEquals(0, v1.getStatus());
            assertEquals("V2.0", templateLibraryService.currentVersion(2L).getVersionNo());
        } finally {
            // 清理：删除新建版本，恢复 V1.0 为当前版本、模板检查项恢复原值
            if (created != null) {
                templateLibraryService.removeById(created.getId());
            }
            TemplateVersion v1 = templateLibraryService.listVersions(2L).stream()
                    .filter(v -> "V1.0".equals(v.getVersionNo())).findFirst().orElse(null);
            if (v1 != null && !Integer.valueOf(1).equals(v1.getStatus())) {
                v1.setStatus(1);
                templateLibraryService.updateById(v1);
            }
            QualityTemplate t2 = templateService.getById(2L);
            if (!ITEMS_2_ORIGINAL.equals(t2.getCheckItems())) {
                t2.setCheckItems(ITEMS_2_ORIGINAL);
                templateService.updateById(t2);
            }
        }
    }

    @Test
    void restoreVersion() {
        TemplateVersion v2 = null;
        TemplateVersion v3 = null;
        try {
            v2 = templateLibraryService.createVersion(2L, ITEMS_2_CHANGED, "改版测试");
            assertEquals("V2.0", v2.getVersionNo());
            TemplateVersion v1 = templateLibraryService.listVersions(2L).stream()
                    .filter(v -> "V1.0".equals(v.getVersionNo())).findFirst().orElseThrow();
            // 从 V1.0 恢复：追加 V3.0，内容为 V1.0 快照
            v3 = templateLibraryService.restoreVersion(v1.getId(), null);
            assertEquals("V3.0", v3.getVersionNo());
            assertEquals(1, v3.getStatus());
            assertEquals(ITEMS_2_ORIGINAL, v3.getCheckItems());
            assertTrue(v3.getChangeRemark().contains("V1.0"));
            assertEquals(ITEMS_2_ORIGINAL, templateService.getById(2L).getCheckItems());
            assertEquals(3, templateLibraryService.listVersions(2L).size());
            // 恢复不存在的版本拒绝
            assertThrows(IllegalArgumentException.class, () ->
                    templateLibraryService.restoreVersion(999L, null));
        } finally {
            if (v3 != null) {
                templateLibraryService.removeById(v3.getId());
            }
            if (v2 != null) {
                templateLibraryService.removeById(v2.getId());
            }
            TemplateVersion v1 = templateLibraryService.listVersions(2L).stream()
                    .filter(v -> "V1.0".equals(v.getVersionNo())).findFirst().orElse(null);
            if (v1 != null && !Integer.valueOf(1).equals(v1.getStatus())) {
                v1.setStatus(1);
                templateLibraryService.updateById(v1);
            }
            QualityTemplate t2 = templateService.getById(2L);
            if (!ITEMS_2_ORIGINAL.equals(t2.getCheckItems())) {
                t2.setCheckItems(ITEMS_2_ORIGINAL);
                templateService.updateById(t2);
            }
        }
    }

    @Test
    void copyTemplate() {
        QualityTemplate copy = null;
        try {
            copy = templateLibraryService.copyTemplate(2L, "测试复制模板");
            assertNotNull(copy.getId());
            assertEquals("院级质量检查模板", templateService.getById(2L).getTemplateName());
            assertEquals("HOSPITAL", copy.getInspectType());
            assertEquals(ITEMS_2_ORIGINAL, copy.getCheckItems());
            // 复制后默认停用，不进启用下拉
            assertEquals(0, copy.getStatus());
            assertTrue(copy.getRemark().contains("院级质量检查模板"));
            // 同名同类型再次复制拒绝
            assertThrows(IllegalArgumentException.class, () ->
                    templateLibraryService.copyTemplate(2L, "测试复制模板"));
            // 空名称拒绝
            assertThrows(IllegalArgumentException.class, () ->
                    templateLibraryService.copyTemplate(2L, "  "));
            // 模板不存在拒绝
            assertThrows(IllegalArgumentException.class, () ->
                    templateLibraryService.copyTemplate(999L, "不存在的模板"));
        } finally {
            if (copy != null) {
                templateService.removeById(copy.getId());
            }
        }
    }

    @Test
    void restoreVersionBlockedByActivePlan() {
        // 模板 1 有待执行 / 进行中计划：恢复其 V1.0 历史版本也被拒绝，版本历史不变
        TemplateVersion v1 = templateLibraryService.listVersions(1L).stream()
                .filter(v -> "V1.0".equals(v.getVersionNo())).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () ->
                templateLibraryService.restoreVersion(v1.getId(), "有进行中计划时恢复"));
        assertEquals(1, templateLibraryService.listVersions(1L).size());
        assertEquals(1, templateLibraryService.currentVersion(1L).getStatus());
    }

    @Test
    void freshTemplateUsageAndCurrentVersion() {
        // 无版本记录的新模板：当前版本为空、版本历史为空、使用情况全 0
        QualityTemplate fresh = new QualityTemplate();
        fresh.setTemplateName("临时测试模板");
        fresh.setInspectType("SELF");
        fresh.setCheckItems("[{\"name\":\"测试项\",\"weight\":100,\"standard\":\"测试标准\"}]");
        assertTrue(templateService.save(fresh));
        QualityTemplate crossTypeCopy = null;
        try {
            assertTrue(templateLibraryService.listVersions(fresh.getId()).isEmpty());
            assertNull(templateLibraryService.currentVersion(fresh.getId()));
            Map<String, Object> usage = templateLibraryService.usage(fresh.getId());
            assertEquals(0, ((Number) usage.get("planTotal")).intValue());
            assertEquals(0, ((Number) usage.get("recordTotal")).intValue());
            assertEquals(false, usage.get("hasActivePlan"));
            assertNull(usage.get("lastPlanDate"));
            // 同名模板在不同检查类型下允许复制（重名校验按“名称 + 检查类型”）
            crossTypeCopy = templateLibraryService.copyTemplate(2L, "科室自查模板");
            assertEquals("HOSPITAL", crossTypeCopy.getInspectType());
            assertEquals(0, crossTypeCopy.getStatus());
        } finally {
            if (crossTypeCopy != null) {
                templateService.removeById(crossTypeCopy.getId());
            }
            templateService.removeById(fresh.getId());
        }
    }

    @Test
    void usageStats() {
        Map<String, Object> u1 = templateLibraryService.usage(1L);
        assertEquals(2, ((Number) u1.get("planTotal")).intValue());
        assertEquals(1, ((Number) u1.get("planPending")).intValue());
        assertEquals(1, ((Number) u1.get("planInProgress")).intValue());
        assertEquals(0, ((Number) u1.get("planCompleted")).intValue());
        assertEquals(2, ((Number) u1.get("recordTotal")).intValue());
        assertEquals(true, u1.get("hasActivePlan"));
        assertEquals(LocalDate.of(2026, 11, 28), u1.get("lastPlanDate"));

        Map<String, Object> u2 = templateLibraryService.usage(2L);
        assertEquals(1, ((Number) u2.get("planTotal")).intValue());
        assertEquals(1, ((Number) u2.get("planCompleted")).intValue());
        assertEquals(1, ((Number) u2.get("recordTotal")).intValue());
        assertEquals(false, u2.get("hasActivePlan"));
        assertEquals(LocalDate.of(2026, 11, 25), u2.get("lastPlanDate"));

        Map<String, Object> u3 = templateLibraryService.usage(3L);
        assertEquals(1, ((Number) u3.get("planTotal")).intValue());
        assertEquals(1, ((Number) u3.get("planPending")).intValue());
        assertEquals(0, ((Number) u3.get("recordTotal")).intValue());
        assertEquals(true, u3.get("hasActivePlan"));

        assertThrows(IllegalArgumentException.class, () -> templateLibraryService.usage(999L));
    }

    @Test
    void libraryStats() {
        Map<String, Object> stats = templateLibraryService.stats();
        assertEquals(3, ((Number) stats.get("templateTotal")).intValue());
        assertEquals(3, ((Number) stats.get("enabled")).intValue());
        assertEquals(0, ((Number) stats.get("disabled")).intValue());
        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) stats.get("byType");
        assertEquals(1L, byType.get("SELF"));
        assertEquals(1L, byType.get("HOSPITAL"));
        assertEquals(1L, byType.get("SPECIAL"));
        assertEquals(3, ((Number) stats.get("versionTotal")).intValue());
        assertEquals(3, ((Number) stats.get("templatesWithVersions")).intValue());
        assertEquals(0, ((Number) stats.get("templatesWithoutVersions")).intValue());
        assertEquals(new BigDecimal("3.67"), stats.get("avgItems"));
        assertEquals("科室自查模板", stats.get("mostUsedTemplate"));
        assertEquals(2L, stats.get("mostUsedPlanCount"));
        assertEquals(0, ((Number) stats.get("neverUsed")).intValue());
    }

    @Test
    void templateDisableAndDeleteGuards() {
        // 模板 1 有进行中计划：停用拒绝
        assertThrows(IllegalArgumentException.class, () -> templateService.disable(1L));
        // 模板 1 已被检查计划引用：删除拒绝
        assertThrows(IllegalArgumentException.class, () -> templateService.removeById(1L));
        // 模板 2 只有已完成计划：停用允许，删除仍拒绝（有计划引用）
        assertThrows(IllegalArgumentException.class, () -> templateService.removeById(2L));
        QualityTemplate disabled = templateService.disable(2L);
        assertEquals(0, disabled.getStatus());
        try {
            QualityTemplate enabled = templateService.enable(2L);
            assertEquals(1, enabled.getStatus());
        } finally {
            QualityTemplate t2 = templateService.getById(2L);
            if (!Integer.valueOf(1).equals(t2.getStatus())) {
                templateService.enable(2L);
            }
        }
        // 无计划引用的新模板可正常删除
        QualityTemplate fresh = new QualityTemplate();
        fresh.setTemplateName("守卫测试模板");
        fresh.setInspectType("SELF");
        fresh.setCheckItems("[{\"name\":\"测试项\",\"weight\":100,\"standard\":\"测试标准\"}]");
        assertTrue(templateService.save(fresh));
        assertTrue(templateService.removeById(fresh.getId()));
    }

    @Test
    void excelExportAndImportRoundTrip() throws Exception {
        // 导出：有效 xlsx，读回 3 个种子模板
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        templateLibraryService.exportTemplates(out);
        assertTrue(out.size() > 0);
        List<QualityTemplate> exported = TemplateLibraryExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(3, exported.size());
        assertTrue(exported.stream().anyMatch(t -> "科室自查模板".equals(t.getTemplateName())));

        // 再次导入同一份导出：同名同类型全部跳过
        Map<String, Object> dup = templateLibraryService.importTemplates(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(0, ((Number) dup.get("created")).intValue());
        assertEquals(3, ((Number) dup.get("skipped")).intValue());

        // 导入 1 个新模板 + 1 个非法行（权重之和 90）+ 1 个重复行
        ByteArrayOutputStream importBytes = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("检查模板");
            Row header = sheet.createRow(0);
            String[] headers = {"模板名称", "检查类型", "检查项 JSON", "状态", "备注"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("导入测试模板");
            r1.createCell(1).setCellValue("专项检查");
            r1.createCell(2).setCellValue("[{\"name\":\"导入项 A\",\"weight\":60,\"standard\":\"标准 A\"},{\"name\":\"导入项 B\",\"weight\":40,\"standard\":\"标准 B\"}]");
            r1.createCell(3).setCellValue("启用");
            r1.createCell(4).setCellValue("导入测试");
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("非法权重模板");
            r2.createCell(1).setCellValue("科室自查");
            r2.createCell(2).setCellValue("[{\"name\":\"非法项\",\"weight\":90,\"standard\":\"标准\"}]");
            r2.createCell(3).setCellValue("启用");
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("科室自查模板");
            r3.createCell(1).setCellValue("科室自查");
            r3.createCell(2).setCellValue("[{\"name\":\"重复项\",\"weight\":100,\"standard\":\"标准\"}]");
            r3.createCell(3).setCellValue("启用");
            wb.write(importBytes);
        }
        QualityTemplate imported = null;
        try {
            Map<String, Object> result = templateLibraryService.importTemplates(
                    new ByteArrayInputStream(importBytes.toByteArray()));
            assertEquals(1, ((Number) result.get("created")).intValue());
            assertEquals(2, ((Number) result.get("skipped")).intValue());
            assertEquals(3, ((Number) result.get("total")).intValue());
            imported = templateService.list().stream()
                    .filter(t -> "导入测试模板".equals(t.getTemplateName())).findFirst().orElse(null);
            assertNotNull(imported);
            assertEquals("SPECIAL", imported.getInspectType());
            assertEquals(1, imported.getStatus());
            // 重复导入同一份：新模板已存在，全部跳过
            Map<String, Object> again = templateLibraryService.importTemplates(
                    new ByteArrayInputStream(importBytes.toByteArray()));
            assertEquals(0, ((Number) again.get("created")).intValue());
            assertEquals(3, ((Number) again.get("skipped")).intValue());
        } finally {
            if (imported != null) {
                templateService.removeById(imported.getId());
            }
        }
    }

    @Test
    void importTemplateDownload() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        templateLibraryService.writeImportTemplate(out);
        assertTrue(out.size() > 0);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Row header = wb.getSheetAt(0).getRow(0);
            assertEquals("模板名称", header.getCell(0).getStringCellValue());
            assertEquals("检查项 JSON", header.getCell(2).getStringCellValue());
        }
    }
}
