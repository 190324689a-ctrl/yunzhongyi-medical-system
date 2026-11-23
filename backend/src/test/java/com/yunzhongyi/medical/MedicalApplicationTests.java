package com.yunzhongyi.medical;

import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.physician.dto.PhysicianQuery;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.service.PhysicianService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 启动冒烟 + 医师档案核心流程验证（H2 内存库；生产默认 profile 是 MySQL）
 */
@SpringBootTest
@ActiveProfiles("h2")
class MedicalApplicationTests {

    @Autowired
    private PhysicianService physicianService;

    @Test
    void contextLoads() {
        assertNotNull(physicianService);
    }

    @Test
    void seedDataLoaded() {
        // data.sql 预置 12 名医师
        assertEquals(12, physicianService.count());
    }

    @Test
    void pageQueryWithFilters() {
        PhysicianQuery q = new PhysicianQuery();
        q.setDeptId(1L);
        PageResult<Physician> r = physicianService.pageQuery(q);
        assertEquals(2, r.getTotal()); // 中医内科：陈国强、李雅琴

        PhysicianQuery q2 = new PhysicianQuery();
        q2.setSurgeryAuth(1);
        q2.setSize(100);
        assertTrue(physicianService.pageQuery(q2).getTotal() > 0);

        PhysicianQuery q3 = new PhysicianQuery();
        q3.setKeyword("陈国强");
        assertEquals(1, physicianService.pageQuery(q3).getTotal());
    }

    @Test
    void crudRoundTrip() {
        Physician p = new Physician();
        p.setName("测试医师");
        p.setGender("M");
        p.setDeptId(1L);
        p.setDeptName("中医内科");
        p.setTitle("主治医师");
        p.setSurgeryLevel(2);
        p.setSurgeryAuth(1);
        p.setAntitumorRx(0);
        p.setAnesthesiaRx(0);
        p.setPsychotropicRx(0);
        p.setStatus(1);
        assertTrue(physicianService.save(p));
        assertNotNull(p.getId());

        Physician found = physicianService.getById(p.getId());
        assertEquals("测试医师", found.getName());

        found.setTitle("副主任医师");
        assertTrue(physicianService.updateById(found));
        assertEquals("副主任医师", physicianService.getById(p.getId()).getTitle());

        assertTrue(physicianService.removeById(p.getId()));
        assertNull(physicianService.getById(p.getId()));
    }

    @Test
    void excelExportAndImportRoundTrip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        physicianService.exportExcel(new PhysicianQuery(), out);
        assertTrue(out.size() > 0);

        int before = (int) physicianService.count();
        int imported = physicianService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        // 导出后再导入：同名同科室去重，应为 0
        assertEquals(0, imported);
        assertEquals(before, (int) physicianService.count());
    }
}
