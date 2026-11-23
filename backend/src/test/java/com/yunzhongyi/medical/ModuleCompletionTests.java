package com.yunzhongyi.medical;

import com.yunzhongyi.medical.completion.dto.ModuleCompletionQuery;
import com.yunzhongyi.medical.completion.entity.ModuleCompletion;
import com.yunzhongyi.medical.completion.excel.ModuleCompletionExcelUtil;
import com.yunzhongyi.medical.completion.service.CompletionTypeUtil;
import com.yunzhongyi.medical.completion.service.ModuleCompletionService;
import com.yunzhongyi.medical.common.PageResult;
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
 * 医务系统完成统计模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：31 个模块完成台账（已完成 31 / 进行中 0 / 待开发 0；
 * 接口总数 720、测试总数 419、完成率 100.00%、平均进度 100.00）。
 * 接口数 / 测试数为代码实测（各包 @Mapping 注解数与各测试类 @Test 数）；
 * 合入模块（14/15、25/26 测试）记 NULL，统计时按 0 计。
 */
@SpringBootTest
@ActiveProfiles("h2")
class ModuleCompletionTests {

    @Autowired
    private ModuleCompletionService completionService;

    @Test
    void contextLoads() {
        assertNotNull(completionService);
    }

    @Test
    void seedDataLoaded() {
        assertEquals(31L, completionService.count());
        // 模块 27 本模块：已完成、进度 100、14 个接口、13 个测试
        ModuleCompletion m27 = completionService.getById(27L);
        assertEquals(Integer.valueOf(27), m27.getModuleNo());
        assertEquals("医务系统完成统计", m27.getModuleName());
        assertEquals("科研与统计", m27.getCategory());
        assertEquals(Integer.valueOf(2), m27.getStatus());
        assertEquals(Integer.valueOf(100), m27.getProgress());
        assertEquals(Integer.valueOf(14), m27.getInterfaceCount());
        assertEquals(Integer.valueOf(13), m27.getTestCount());
        assertEquals(LocalDate.of(2026, 11, 22), m27.getCompletedDate());
        // 合入模块：接口与测试记 NULL，不虚增
        ModuleCompletion m14 = completionService.getById(14L);
        assertNull(m14.getInterfaceCount());
        assertNull(m14.getTestCount());
        assertEquals(Integer.valueOf(2), m14.getStatus());
        // 模块 28：已完成（模块 28 开发时由 data.sql 的 UPDATE 改为已完成）
        ModuleCompletion m28 = completionService.getById(28L);
        assertEquals(Integer.valueOf(2), m28.getStatus());
        assertEquals(Integer.valueOf(100), m28.getProgress());
        assertEquals(Integer.valueOf(99), m28.getInterfaceCount());
        assertEquals(Integer.valueOf(63), m28.getTestCount());
        assertEquals(LocalDate.of(2026, 11, 22), m28.getCompletedDate());
        // 模块 29：已完成（模块 29 开发时由 data.sql 的 UPDATE 改为已完成）
        ModuleCompletion m29 = completionService.getById(29L);
        assertEquals(Integer.valueOf(2), m29.getStatus());
        assertEquals(Integer.valueOf(100), m29.getProgress());
        assertEquals(Integer.valueOf(39), m29.getInterfaceCount());
        assertEquals(Integer.valueOf(20), m29.getTestCount());
        assertEquals(LocalDate.of(2026, 11, 22), m29.getCompletedDate());
        // 模块 30：已完成（模块 30 开发时由 data.sql 的 UPDATE 改为已完成）
        ModuleCompletion m30 = completionService.getById(30L);
        assertEquals(Integer.valueOf(2), m30.getStatus());
        assertEquals(Integer.valueOf(100), m30.getProgress());
        assertEquals(Integer.valueOf(27), m30.getInterfaceCount());
        assertEquals(Integer.valueOf(22), m30.getTestCount());
        assertEquals(LocalDate.of(2026, 11, 22), m30.getCompletedDate());
        // 模块 31：已完成（模块 31 开发时由 data.sql 的 UPDATE 改为已完成）
        ModuleCompletion m31 = completionService.getById(31L);
        assertEquals(Integer.valueOf(2), m31.getStatus());
        assertEquals(Integer.valueOf(100), m31.getProgress());
        assertEquals(Integer.valueOf(24), m31.getInterfaceCount());
        assertEquals(Integer.valueOf(20), m31.getTestCount());
        assertEquals(LocalDate.of(2026, 11, 22), m31.getCompletedDate());
    }

    @Test
    void statsOverview() {
        Map<String, Object> stats = completionService.stats();
        assertEquals(31L, stats.get("total"));
        assertEquals(31L, stats.get("completed"));
        assertEquals(0L, stats.get("inProgress"));
        assertEquals(0L, stats.get("pending"));
        assertEquals(new BigDecimal("100.00"), stats.get("completionRate"));
        assertEquals(720L, stats.get("totalInterfaces"));
        assertEquals(419L, stats.get("totalTests"));
        assertEquals(new BigDecimal("100.00"), stats.get("avgProgress"));
    }

    @Test
    void statsByCategory() {
        List<Map<String, Object>> rows = completionService.statsByCategory();
        assertEquals(5, rows.size());
        Map<String, Object> physician = rows.stream()
                .filter(r -> "医师管理".equals(r.get("category"))).findFirst().orElseThrow();
        assertEquals(8L, physician.get("total"));
        assertEquals(8L, physician.get("completed"));
        assertEquals(new BigDecimal("100.00"), physician.get("completionRate"));
        assertEquals(136L, physician.get("totalInterfaces"));
        assertEquals(81L, physician.get("totalTests"));
        Map<String, Object> quality = rows.stream()
                .filter(r -> "质量管理".equals(r.get("category"))).findFirst().orElseThrow();
        assertEquals(10L, quality.get("total"));
        assertEquals(206L, quality.get("totalInterfaces"));
        assertEquals(115L, quality.get("totalTests"));
        Map<String, Object> system = rows.stream()
                .filter(r -> "系统与移动".equals(r.get("category"))).findFirst().orElseThrow();
        assertEquals(4L, system.get("total"));
        assertEquals(4L, system.get("completed"));
        assertEquals(0L, system.get("inProgress"));
        assertEquals(0L, system.get("pending"));
        assertEquals(new BigDecimal("100.00"), system.get("completionRate"));
        assertEquals(189L, system.get("totalInterfaces"));
        assertEquals(125L, system.get("totalTests"));
    }

    @Test
    void statsByMonth() {
        List<Map<String, Object>> rows = completionService.statsByMonth();
        assertEquals(1, rows.size());
        assertEquals("2026-11", rows.get(0).get("month"));
        assertEquals(31L, rows.get(0).get("completedCount"));
    }

    @Test
    void pageQueryWithFilters() {
        ModuleCompletionQuery q = new ModuleCompletionQuery();
        q.setKeyword("质量");
        PageResult<ModuleCompletion> p = completionService.pageQuery(q);
        assertEquals(5L, p.getTotal());
        q = new ModuleCompletionQuery();
        q.setCategory("质量管理");
        assertEquals(10L, completionService.pageQuery(q).getTotal());
        q = new ModuleCompletionQuery();
        q.setStatus(0);
        assertEquals(0L, completionService.pageQuery(q).getTotal());
        q = new ModuleCompletionQuery();
        q.setStatus(2);
        assertEquals(31L, completionService.pageQuery(q).getTotal());
        q = new ModuleCompletionQuery();
        q.setModuleNo(27);
        PageResult<ModuleCompletion> one = completionService.pageQuery(q);
        assertEquals(1L, one.getTotal());
        assertEquals("医务系统完成统计", one.getRecords().get(0).getModuleName());
    }

    @Test
    void crudRoundTrip() {
        ModuleCompletion m = newModule(32, "测试模块", "系统与移动", 0, 0);
        try {
            ModuleCompletion created = completionService.create(m);
            assertNotNull(created.getId());
            assertNull(created.getCompletedDate());
            created.setProgress(30);
            created.setStatus(1);
            ModuleCompletion updated = completionService.update(created);
            assertEquals(Integer.valueOf(30), updated.getProgress());
            assertEquals(Integer.valueOf(1), updated.getStatus());
            assertEquals("测试模块", completionService.detail(created.getId()).getModuleName());
        } finally {
            if (m.getId() != null) {
                completionService.removeById(m.getId());
            }
        }
        assertEquals(31L, completionService.count());
    }

    @Test
    void duplicateModuleNoRejected() {
        ModuleCompletion dup = newModule(1, "重复编号", "医师管理", 0, 0);
        assertThrows(IllegalArgumentException.class,
                () -> completionService.create(dup));
    }

    @Test
    void invalidCategoryAndProgressRejected() {
        ModuleCompletion badCategory = newModule(33, "非法分组", "不存在的分组", 0, 0);
        assertThrows(IllegalArgumentException.class,
                () -> completionService.create(badCategory));
        ModuleCompletion badProgress = newModule(33, "进度越界", "系统与移动", 0, 101);
        assertThrows(IllegalArgumentException.class,
                () -> completionService.create(badProgress));
        ModuleCompletion negativeInterfaces = newModule(33, "接口为负", "系统与移动", 0, 0);
        negativeInterfaces.setInterfaceCount(-1);
        assertThrows(IllegalArgumentException.class,
                () -> completionService.create(negativeInterfaces));
    }

    @Test
    void completeAndReopenFlow() {
        ModuleCompletion m = newModule(33, "待完成模块", "系统与移动", 0, 0);
        try {
            completionService.create(m);
            // 重新开始只对已完成模块开放
            assertThrows(IllegalArgumentException.class,
                    () -> completionService.reopen(m.getId()));
            ModuleCompletion done = completionService.complete(m.getId());
            assertEquals(Integer.valueOf(2), done.getStatus());
            assertEquals(Integer.valueOf(100), done.getProgress());
            assertEquals(LocalDate.now(), done.getCompletedDate());
            // 重复标记完成拒绝
            assertThrows(IllegalArgumentException.class,
                    () -> completionService.complete(m.getId()));
            ModuleCompletion reopened = completionService.reopen(m.getId());
            assertEquals(Integer.valueOf(1), reopened.getStatus());
            assertEquals(Integer.valueOf(50), reopened.getProgress());
            assertNull(reopened.getCompletedDate());
        } finally {
            if (m.getId() != null) {
                completionService.removeById(m.getId());
            }
        }
    }

    @Test
    void completedCreateDefaults() {
        // 新建即已完成：进度强制 100、完成日期默认当天
        ModuleCompletion m = newModule(34, "直接完成", "科研与统计", 2, null);
        try {
            ModuleCompletion created = completionService.create(m);
            assertEquals(Integer.valueOf(100), created.getProgress());
            assertEquals(LocalDate.now(), created.getCompletedDate());
        } finally {
            if (m.getId() != null) {
                completionService.removeById(m.getId());
            }
        }
        // 未完成时完成日期会被清空
        ModuleCompletion p = newModule(34, "未完成", "科研与统计", 0, 0);
        p.setCompletedDate(LocalDate.of(2026, 11, 22));
        try {
            ModuleCompletion created = completionService.create(p);
            assertNull(created.getCompletedDate());
        } finally {
            if (p.getId() != null) {
                completionService.removeById(p.getId());
            }
        }
    }

    @Test
    void excelExportAndImportRoundTrip() throws Exception {
        // 导出：有效 xlsx（PK 开头），表头与 31 行齐全
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ModuleCompletionQuery q = new ModuleCompletionQuery();
        q.setSize(100);
        completionService.exportExcel(q, out);
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length > 0);
        assertEquals((byte) 'P', bytes[0]);
        assertEquals((byte) 'K', bytes[1]);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("模块编号", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(31, sheet.getLastRowNum());
            Row row27 = sheet.getRow(27);
            assertEquals("医务系统完成统计", row27.getCell(1).getStringCellValue());
        }
        // 导入：1 条新增（编号 35）+ 1 条重复（编号 1）→ 新建 1、跳过 1
        ModuleCompletion fresh = newModule(35, "导入新增", "系统与移动", 0, 0);
        ModuleCompletion dup = newModule(1, "重复编号", "医师管理", 0, 0);
        ByteArrayOutputStream importOut = new ByteArrayOutputStream();
        ModuleCompletionExcelUtil.write(List.of(fresh, dup), importOut);
        Map<String, Object> result = completionService.importExcel(
                new ByteArrayInputStream(importOut.toByteArray()));
        try {
            assertEquals(1, result.get("created"));
            assertEquals(1, result.get("skipped"));
            assertEquals(2, result.get("total"));
            assertEquals(32L, completionService.count());
        } finally {
            completionService.remove(new com.baomidou.mybatisplus.core.conditions.query
                    .LambdaQueryWrapper<ModuleCompletion>()
                    .eq(ModuleCompletion::getModuleNo, 35));
        }
    }

    @Test
    void typeUtilBoundaries() {
        assertEquals(new BigDecimal("87.10"),
                CompletionTypeUtil.completionRate(27, 31));
        assertEquals(new BigDecimal("0.00"), CompletionTypeUtil.completionRate(0, 0));
        assertEquals(new BigDecimal("100.00"), CompletionTypeUtil.completionRate(5, 5));
        assertEquals(new BigDecimal("88.71"), CompletionTypeUtil.avgProgress(2750, 31));
        assertEquals("已完成", CompletionTypeUtil.statusText(2));
        assertEquals("进行中", CompletionTypeUtil.statusText(1));
        assertEquals("待开发", CompletionTypeUtil.statusText(0));
        assertEquals(Integer.valueOf(2), CompletionTypeUtil.parseStatus("已完成"));
        assertEquals(Integer.valueOf(0), CompletionTypeUtil.parseStatus("0"));
        assertNull(CompletionTypeUtil.parseStatus("未知"));
        assertTrue(CompletionTypeUtil.isValidCategory("质量管理"));
        assertFalse(CompletionTypeUtil.isValidCategory("不存在"));
        assertEquals(5, CompletionTypeUtil.CATEGORIES.size());
    }

    private ModuleCompletion newModule(int moduleNo, String name, String category,
                                       Integer status, Integer progress) {
        ModuleCompletion m = new ModuleCompletion();
        m.setModuleNo(moduleNo);
        m.setModuleName(name);
        m.setCategory(category);
        m.setStatus(status);
        m.setProgress(progress);
        m.setInterfaceCount(0);
        m.setTestCount(0);
        return m;
    }
}
