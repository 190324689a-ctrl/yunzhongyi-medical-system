package com.yunzhongyi.medical;

import com.yunzhongyi.medical.authorization.dto.AuthorizationQuery;
import com.yunzhongyi.medical.authorization.entity.Authorization;
import com.yunzhongyi.medical.authorization.excel.AuthorizationExcelUtil;
import com.yunzhongyi.medical.authorization.service.AuthorizationService;
import com.yunzhongyi.medical.common.PageResult;
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
 * 重点授权模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据 6 条，覆盖 4 种授权类型、4 种审批状态。
 */
@SpringBootTest
@ActiveProfiles("h2")
class AuthorizationTests {

    @Autowired
    private AuthorizationService authorizationService;

    @Test
    void contextLoads() {
        assertNotNull(authorizationService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(authorizationService.count() >= 6);
        // 种子里唯一的三级手术分级授权申请：王建军，待科主任审批
        AuthorizationQuery q = new AuthorizationQuery();
        q.setAuthType("SURGERY");
        PageResult<Authorization> r = authorizationService.pageQuery(q);
        assertEquals(2, r.getTotal());
        Authorization wang = r.getRecords().stream()
                .filter(a -> "王建军".equals(a.getPhysicianName())).findFirst().orElseThrow();
        assertEquals(3, wang.getSurgeryLevel());
        assertEquals(0, wang.getStatus());
        assertEquals(LocalDate.of(2027, 1, 1), wang.getValidFrom());
        assertEquals(LocalDate.of(2029, 12, 31), wang.getValidTo());
    }

    @Test
    void pageQueryWithFilters() {
        AuthorizationQuery q = new AuthorizationQuery();
        q.setStatus(2);
        assertTrue(authorizationService.pageQuery(q).getTotal() >= 1); // 已通过：陈国强

        AuthorizationQuery q2 = new AuthorizationQuery();
        q2.setKeyword("李雅琴");
        assertEquals(1, authorizationService.pageQuery(q2).getTotal());

        AuthorizationQuery q3 = new AuthorizationQuery();
        q3.setAuthType("ANESTHESIA");
        assertEquals(2, authorizationService.pageQuery(q3).getTotal());
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = authorizationService.stats();
        assertTrue((Long) s.get("total") >= 6);
        assertTrue((Long) s.get("pending") >= 4);
        assertTrue((Long) s.get("approved") >= 1);
        assertTrue((Long) s.get("rejected") >= 1);
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(2L, byType.get("SURGERY"));
        assertEquals(2L, byType.get("ANESTHESIA"));
        assertEquals(1L, byType.get("ANTITUMOR"));
        assertEquals(1L, byType.get("PSYCHOTROPIC"));
    }

    @Test
    void approveFlowTwoLevels() {
        Authorization a = new Authorization();
        a.setPhysicianId(1L);
        a.setPhysicianName("审批流测试");
        a.setDeptId(1L);
        a.setDeptName("中医内科");
        a.setAuthType("ANTITUMOR");
        a.setValidFrom(LocalDate.of(2027, 1, 1));
        a.setValidTo(LocalDate.of(2027, 12, 31));
        a.setReason("测试多级审批");
        assertTrue(authorizationService.save(a));
        assertEquals(0, a.getStatus());

        Authorization step1 = authorizationService.approve(a.getId(), "科主任", "同意");
        assertEquals(1, step1.getStatus()); // 待医务科审批
        assertTrue(step1.getApprovalHistory().contains("科主任"));

        Authorization step2 = authorizationService.approve(a.getId(), "医务科", "同意");
        assertEquals(2, step2.getStatus()); // 已通过
        assertTrue(step2.getApprovalHistory().contains("医务科"));

        // 已通过后不能再审批
        assertThrows(IllegalStateException.class, () -> authorizationService.approve(a.getId(), "医务科", "同意"));
        assertTrue(authorizationService.removeById(a.getId()));
    }

    @Test
    void rejectFlow() {
        Authorization a = new Authorization();
        a.setPhysicianId(2L);
        a.setPhysicianName("驳回测试");
        a.setDeptId(2L);
        a.setDeptName("针灸推拿科");
        a.setAuthType("PSYCHOTROPIC");
        a.setValidFrom(LocalDate.of(2027, 1, 1));
        a.setValidTo(LocalDate.of(2027, 12, 31));
        a.setReason("测试驳回");
        assertTrue(authorizationService.save(a));

        Authorization r = authorizationService.reject(a.getId(), "科主任", "材料不全");
        assertEquals(3, r.getStatus());
        assertTrue(r.getApprovalHistory().contains("驳回"));

        assertThrows(IllegalStateException.class, () -> authorizationService.approve(a.getId(), "医务科", "同意"));
        assertTrue(authorizationService.removeById(a.getId()));
    }

    @Test
    void invalidDateRangeRejected() {
        Authorization a = new Authorization();
        a.setPhysicianId(1L);
        a.setPhysicianName("日期测试");
        a.setAuthType("SURGERY");
        a.setSurgeryLevel(2);
        a.setValidFrom(LocalDate.of(2027, 12, 31));
        a.setValidTo(LocalDate.of(2027, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> authorizationService.save(a));
    }

    @Test
    void crudRoundTrip() {
        Authorization a = new Authorization();
        a.setPhysicianId(5L);
        a.setPhysicianName("增删改测试");
        a.setDeptId(5L);
        a.setDeptName("儿科");
        a.setAuthType("ANESTHESIA");
        a.setValidFrom(LocalDate.of(2027, 3, 1));
        a.setValidTo(LocalDate.of(2028, 2, 29));
        a.setReason("测试");
        assertTrue(authorizationService.save(a));
        assertNotNull(a.getId());

        Authorization found = authorizationService.getById(a.getId());
        assertEquals("增删改测试", found.getPhysicianName());

        found.setReason("修改后");
        assertTrue(authorizationService.updateById(found));
        assertEquals("修改后", authorizationService.getById(a.getId()).getReason());

        assertTrue(authorizationService.removeById(a.getId()));
        assertNull(authorizationService.getById(a.getId()));
    }

    @Test
    void excelRoundTrip() throws Exception {
        AuthorizationQuery q = new AuthorizationQuery();
        q.setPage(1);
        q.setSize(100);
        List<Authorization> all = authorizationService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AuthorizationExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<Authorization> back = AuthorizationExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（申请人、授权类型、有效期、审批状态）
        Authorization first = all.get(0);
        Authorization firstBack = back.stream()
                .filter(a -> first.getPhysicianName().equals(a.getPhysicianName())
                        && first.getAuthType().equals(a.getAuthType()))
                .findFirst().orElseThrow();
        assertEquals(first.getValidFrom(), firstBack.getValidFrom());
        assertEquals(first.getValidTo(), firstBack.getValidTo());
        assertEquals(first.getStatus(), firstBack.getStatus());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条授权 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （项目用逻辑删除，删后重导按“同医师同类型重复”规则会跳过种子行，只补回被删的那条）
        Authorization fresh = new Authorization();
        fresh.setPhysicianId(12L);
        fresh.setPhysicianName("郑秀兰");
        fresh.setDeptId(4L);
        fresh.setDeptName("妇科");
        fresh.setAuthType("ANTITUMOR");
        fresh.setValidFrom(LocalDate.of(2027, 1, 1));
        fresh.setValidTo(LocalDate.of(2027, 12, 31));
        fresh.setReason("导入往返测试");
        assertTrue(authorizationService.save(fresh));

        AuthorizationQuery q = new AuthorizationQuery();
        q.setPage(1);
        q.setSize(100);
        List<Authorization> all = authorizationService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AuthorizationExcelUtil.write(all, out);

        assertTrue(authorizationService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, authorizationService.listByQuery(q).size());

        int imported = authorizationService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因同医师同类型重复而跳过，只补回郑秀兰这一条
        assertEquals(all.size(), authorizationService.listByQuery(q).size());
        Authorization restored = authorizationService.listByQuery(q).stream()
                .filter(a -> "郑秀兰".equals(a.getPhysicianName())).findFirst().orElseThrow();
        assertEquals("ANTITUMOR", restored.getAuthType());
        assertEquals(12L, restored.getPhysicianId()); // 导入时按姓名查医师档案补全医师 id
        assertTrue(authorizationService.removeById(restored.getId()));
        assertEquals(all.size() - 1, authorizationService.listByQuery(q).size());
    }
}
