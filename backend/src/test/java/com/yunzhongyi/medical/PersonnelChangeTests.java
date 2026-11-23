package com.yunzhongyi.medical;

import com.yunzhongyi.medical.personnel.dto.PersonnelChangeQuery;
import com.yunzhongyi.medical.personnel.entity.PersonnelChange;
import com.yunzhongyi.medical.personnel.excel.PersonnelChangeExcelUtil;
import com.yunzhongyi.medical.personnel.service.PersonnelChangeService;
import com.yunzhongyi.medical.personnel.service.PersonnelChangeTypeUtil;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.mapper.PhysicianMapper;
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
 * 医疗人员动态管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 条变动，覆盖调动/离职/退休/入职/返聘 5 种类型、
 * 待审批/已通过/已驳回 3 种审批状态；退休 1 条已通过，与医师档案
 * 郑秀兰已退休状态一致。
 */
@SpringBootTest
@ActiveProfiles("h2")
class PersonnelChangeTests {

    @Autowired
    private PersonnelChangeService personnelChangeService;

    @Autowired
    private PhysicianMapper physicianMapper;

    @Test
    void contextLoads() {
        assertNotNull(personnelChangeService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(personnelChangeService.count() >= 6);

        // 种子变动 1：郑秀兰退休，妇科，已通过，审批记录留痕
        PersonnelChange c1 = personnelChangeService.getById(1L);
        assertEquals("郑秀兰", c1.getPhysicianName());
        assertEquals(12L, c1.getPhysicianId());
        assertEquals("RETIRE", c1.getChangeType());
        assertEquals("妇科", c1.getFromDeptName());
        assertNull(c1.getToDeptName());
        assertEquals(LocalDate.of(2025, 12, 31), c1.getChangeDate());
        assertEquals(1, c1.getStatus());
        assertEquals("吴文斌", c1.getApprover());
        assertTrue(c1.getApprovalHistory().contains("通过"));
        assertTrue(c1.getApprovalHistory().contains("同意退休"));

        // 种子变动 2：陈国强调动，中医内科 → 针灸推拿科，待审批
        PersonnelChange c2 = personnelChangeService.getById(2L);
        assertEquals("TRANSFER", c2.getChangeType());
        assertEquals("中医内科", c2.getFromDeptName());
        assertEquals("针灸推拿科", c2.getToDeptName());
        assertEquals(0, c2.getStatus());

        // 种子变动 3：赵晓峰入职，儿科，尚未建档，待审批
        PersonnelChange c3 = personnelChangeService.getById(3L);
        assertEquals("JOIN", c3.getChangeType());
        assertNull(c3.getPhysicianId());
        assertNull(c3.getFromDeptName());
        assertEquals("儿科", c3.getToDeptName());
        assertEquals(0, c3.getStatus());

        // 种子变动 4：刘志强离职，急诊科，待审批
        PersonnelChange c4 = personnelChangeService.getById(4L);
        assertEquals("RESIGN", c4.getChangeType());
        assertEquals("急诊科", c4.getFromDeptName());
        assertEquals(0, c4.getStatus());

        // 种子变动 5：孙小红返聘，儿科，已驳回
        PersonnelChange c5 = personnelChangeService.getById(5L);
        assertEquals("REHIRE", c5.getChangeType());
        assertEquals(2, c5.getStatus());
        assertTrue(c5.getApprovalHistory().contains("驳回"));

        // 种子变动 6：赵丽华调动，妇科 → 儿科，待审批
        PersonnelChange c6 = personnelChangeService.getById(6L);
        assertEquals("TRANSFER", c6.getChangeType());
        assertEquals("妇科", c6.getFromDeptName());
        assertEquals("儿科", c6.getToDeptName());
        assertEquals(0, c6.getStatus());
    }

    @Test
    void pageQueryWithFilters() {
        PersonnelChangeQuery q = new PersonnelChangeQuery();
        q.setChangeType("TRANSFER");
        assertEquals(2, personnelChangeService.pageQuery(q).getTotal()); // 陈国强、赵丽华

        PersonnelChangeQuery q2 = new PersonnelChangeQuery();
        q2.setChangeType("RETIRE");
        assertEquals(1, personnelChangeService.pageQuery(q2).getTotal()); // 郑秀兰

        PersonnelChangeQuery q3 = new PersonnelChangeQuery();
        q3.setChangeType("JOIN");
        assertEquals(1, personnelChangeService.pageQuery(q3).getTotal()); // 赵晓峰

        PersonnelChangeQuery q4 = new PersonnelChangeQuery();
        q4.setStatus(0);
        assertEquals(4, personnelChangeService.pageQuery(q4).getTotal()); // 待审批 4 条

        PersonnelChangeQuery q5 = new PersonnelChangeQuery();
        q5.setStatus(1);
        assertEquals(1, personnelChangeService.pageQuery(q5).getTotal()); // 已通过：郑秀兰

        PersonnelChangeQuery q6 = new PersonnelChangeQuery();
        q6.setStatus(2);
        assertEquals(1, personnelChangeService.pageQuery(q6).getTotal()); // 已驳回：孙小红

        PersonnelChangeQuery q7 = new PersonnelChangeQuery();
        q7.setDeptId(4L);
        assertEquals(2, personnelChangeService.pageQuery(q7).getTotal()); // 相关科室妇科：郑秀兰、赵丽华

        PersonnelChangeQuery q8 = new PersonnelChangeQuery();
        q8.setDeptId(5L);
        assertEquals(3, personnelChangeService.pageQuery(q8).getTotal()); // 相关科室儿科：赵晓峰、孙小红、赵丽华

        PersonnelChangeQuery q9 = new PersonnelChangeQuery();
        q9.setStartDate(LocalDate.of(2026, 11, 1));
        q9.setEndDate(LocalDate.of(2026, 11, 30));
        assertEquals(2, personnelChangeService.pageQuery(q9).getTotal()); // 11 月生效：赵晓峰、孙小红

        PersonnelChangeQuery q10 = new PersonnelChangeQuery();
        q10.setKeyword("赵丽华");
        assertEquals(1, personnelChangeService.pageQuery(q10).getTotal());

        PersonnelChangeQuery q11 = new PersonnelChangeQuery();
        q11.setPhysicianId(1L);
        assertEquals(1, personnelChangeService.pageQuery(q11).getTotal()); // 陈国强
    }

    @Test
    void saveValidationAndDefaults() {
        // 变动类型非法，拒绝保存
        PersonnelChange badType = newChange("测试人员", "TRANSFER");
        badType.setChangeType("XXX");
        assertThrows(IllegalArgumentException.class, () -> personnelChangeService.save(badType));

        // 生效日期为空，拒绝保存
        PersonnelChange badDate = newChange("测试人员", "TRANSFER");
        badDate.setChangeDate(null);
        assertThrows(IllegalArgumentException.class, () -> personnelChangeService.save(badDate));

        // 调动未填调入科室，拒绝保存
        PersonnelChange noToDept = newChange("测试人员", "TRANSFER");
        noToDept.setToDeptName(null);
        assertThrows(IllegalArgumentException.class, () -> personnelChangeService.save(noToDept));

        // 调动前后科室相同，拒绝保存
        PersonnelChange sameDept = newChange("测试人员", "TRANSFER");
        sameDept.setToDeptId(1L);
        sameDept.setToDeptName("中医内科");
        assertThrows(IllegalArgumentException.class, () -> personnelChangeService.save(sameDept));

        // 离职未填原科室，拒绝保存
        PersonnelChange noFromDept = newChange("测试人员", "RESIGN");
        noFromDept.setFromDeptName(null);
        assertThrows(IllegalArgumentException.class, () -> personnelChangeService.save(noFromDept));

        // 入职未填调入科室，拒绝保存
        PersonnelChange joinNoDept = newChange("测试人员", "JOIN");
        joinNoDept.setToDeptName(null);
        assertThrows(IllegalArgumentException.class, () -> personnelChangeService.save(joinNoDept));

        // 合法登记：默认待审批、申请日期为当天
        PersonnelChange c = newChange("测试人员", "TRANSFER");
        assertTrue(personnelChangeService.save(c));
        try {
            assertNotNull(c.getId());
            assertEquals(PersonnelChangeTypeUtil.STATUS_PENDING, c.getStatus());
            assertEquals(LocalDate.now(), c.getApplyDate());

            // 同一人员同一变动类型同一生效日期已有待审批记录，拒绝重复登记
            PersonnelChange dup = newChange("测试人员", "TRANSFER");
            assertThrows(IllegalArgumentException.class, () -> personnelChangeService.save(dup));
        } finally {
            personnelChangeService.removeById(c.getId());
        }

        // 驳回后同一人员同一类型同一日期可重新登记（待审批记录已不存在）
        PersonnelChange c2 = newChange("测试人员二", "RESIGN");
        assertTrue(personnelChangeService.save(c2));
        try {
            personnelChangeService.reject(c2.getId(), "吴文斌", "材料不全");
            PersonnelChange again = newChange("测试人员二", "RESIGN");
            assertTrue(personnelChangeService.save(again));
            personnelChangeService.removeById(again.getId());
        } finally {
            personnelChangeService.removeById(c2.getId());
        }
    }

    @Test
    void approvalFlowTransferSyncsPhysician() {
        // 王建军调动：骨伤科 → 中医内科，审批通过后医师档案科室同步改写
        PersonnelChange c = newChange("王建军", "TRANSFER");
        c.setPhysicianId(3L);
        c.setFromDeptId(3L);
        c.setFromDeptName("骨伤科");
        c.setToDeptId(1L);
        c.setToDeptName("中医内科");
        assertTrue(personnelChangeService.save(c));
        try {
            assertEquals("骨伤科", physicianMapper.selectById(3L).getDeptName());

            PersonnelChange approved = personnelChangeService.approve(c.getId(), "吴文斌", "同意调动");
            assertEquals(1, approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertEquals("同意调动", approved.getApprovalOpinion());
            assertTrue(approved.getApprovalHistory().contains("医务科"));
            assertTrue(approved.getApprovalHistory().contains("通过"));
            // 医师档案已同步：科室改为中医内科，在职状态不变
            Physician p = physicianMapper.selectById(3L);
            assertEquals(1L, p.getDeptId());
            assertEquals("中医内科", p.getDeptName());
            assertEquals(1, p.getStatus());

            // 已结束的变动不能再通过、再驳回、再修改
            assertThrows(IllegalStateException.class,
                    () -> personnelChangeService.approve(c.getId(), "吴文斌", "再次通过"));
            assertThrows(IllegalStateException.class,
                    () -> personnelChangeService.reject(c.getId(), "吴文斌", "改主意了"));
            approved.setReason("改原因");
            assertThrows(IllegalStateException.class, () -> personnelChangeService.updateById(approved));
        } finally {
            // 恢复医师档案科室，避免污染其他测试
            Physician restore = new Physician();
            restore.setId(3L);
            restore.setDeptId(3L);
            restore.setDeptName("骨伤科");
            physicianMapper.updateById(restore);
            personnelChangeService.removeById(c.getId());
        }

        // 驳回流程：状态变为已驳回，审批记录留痕；驳回后不能再通过
        PersonnelChange c2 = newChange("驳回测试人员", "TRANSFER");
        assertTrue(personnelChangeService.save(c2));
        try {
            PersonnelChange rejected = personnelChangeService.reject(c2.getId(), "吴文斌", "暂缓调动");
            assertEquals(2, rejected.getStatus());
            assertTrue(rejected.getApprovalHistory().contains("驳回"));
            assertThrows(IllegalStateException.class,
                    () -> personnelChangeService.approve(c2.getId(), "吴文斌", "改主意了"));
        } finally {
            personnelChangeService.removeById(c2.getId());
        }
    }

    @Test
    void approvalFlowResignAndRetireSyncPhysicianStatus() {
        // 张伟离职：审批通过后医师档案在职状态变为离职（4）
        PersonnelChange c = newChange("张伟", "RESIGN");
        c.setPhysicianId(10L);
        c.setFromDeptId(3L);
        c.setFromDeptName("骨伤科");
        c.setToDeptId(null);
        c.setToDeptName(null);
        assertTrue(personnelChangeService.save(c));
        try {
            assertEquals(1, physicianMapper.selectById(10L).getStatus());
            personnelChangeService.approve(c.getId(), "吴文斌", "同意离职");
            assertEquals(4, physicianMapper.selectById(10L).getStatus());
        } finally {
            Physician restore = new Physician();
            restore.setId(10L);
            restore.setStatus(1);
            physicianMapper.updateById(restore);
            personnelChangeService.removeById(c.getId());
        }

        // 周建国退休：审批通过后医师档案在职状态变为退休（5）
        PersonnelChange c2 = newChange("周建国", "RETIRE");
        c2.setPhysicianId(7L);
        c2.setFromDeptId(7L);
        c2.setFromDeptName("放射科");
        c2.setToDeptId(null);
        c2.setToDeptName(null);
        assertTrue(personnelChangeService.save(c2));
        try {
            personnelChangeService.approve(c2.getId(), "吴文斌", "同意退休");
            assertEquals(5, physicianMapper.selectById(7L).getStatus());
        } finally {
            Physician restore = new Physician();
            restore.setId(7L);
            restore.setStatus(1);
            physicianMapper.updateById(restore);
            personnelChangeService.removeById(c2.getId());
        }
    }

    @Test
    void approvalFlowRehireSyncsPhysicianStatus() {
        // 李雅琴返聘：当前在职状态为进修（3），审批通过后改回在职（1）并落到原科室
        PersonnelChange c = newChange("李雅琴", "REHIRE");
        c.setPhysicianId(9L);
        c.setFromDeptId(1L);
        c.setFromDeptName("中医内科");
        c.setToDeptId(1L);
        c.setToDeptName("中医内科");
        assertTrue(personnelChangeService.save(c));
        try {
            assertEquals(3, physicianMapper.selectById(9L).getStatus());
            personnelChangeService.approve(c.getId(), "吴文斌", "同意返聘");
            Physician p = physicianMapper.selectById(9L);
            assertEquals(1, p.getStatus());
            assertEquals("中医内科", p.getDeptName());
        } finally {
            Physician restore = new Physician();
            restore.setId(9L);
            restore.setStatus(3);
            physicianMapper.updateById(restore);
            personnelChangeService.removeById(c.getId());
        }
    }

    @Test
    void joinWithoutPhysicianIdApprovesWithoutSync() {
        // 新入职人员尚未建档（physicianId 为空）：审批通过不报错、不阻断
        PersonnelChange c = newChange("新入职测试", "JOIN");
        c.setPhysicianId(null);
        c.setFromDeptId(null);
        c.setFromDeptName(null);
        c.setToDeptId(5L);
        c.setToDeptName("儿科");
        assertTrue(personnelChangeService.save(c));
        try {
            PersonnelChange approved = personnelChangeService.approve(c.getId(), "吴文斌", "同意入职");
            assertEquals(1, approved.getStatus());
        } finally {
            personnelChangeService.removeById(c.getId());
        }
    }

    @Test
    void updatePendingOnly() {
        // 待审批的变动可以修改
        PersonnelChange c = newChange("修改测试人员", "TRANSFER");
        assertTrue(personnelChangeService.save(c));
        try {
            c.setReason("更新后的变动原因");
            assertTrue(personnelChangeService.updateById(c));
            assertEquals("更新后的变动原因", personnelChangeService.getById(c.getId()).getReason());
        } finally {
            personnelChangeService.removeById(c.getId());
        }
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = personnelChangeService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(4L, s.get("pending")); // 待审批：陈国强、赵晓峰、刘志强、赵丽华
        assertEquals(1L, s.get("approved")); // 已通过：郑秀兰
        assertEquals(1L, s.get("rejected")); // 已驳回：孙小红
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(2L, byType.get("TRANSFER"));
        assertEquals(1L, byType.get("RESIGN"));
        assertEquals(1L, byType.get("RETIRE"));
        assertEquals(1L, byType.get("JOIN"));
        assertEquals(1L, byType.get("REHIRE"));

        // 按相关科室汇总：妇科 2、儿科 2、中医内科 1、急诊科 1
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        assertEquals(4, byDept.size());
        Map<String, Object> fk = byDept.stream()
                .filter(d -> "妇科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(2L, fk.get("count"));
        assertEquals(1L, fk.get("pendingCount"));
        assertEquals(1L, fk.get("approvedCount"));
        Map<String, Object> ek = byDept.stream()
                .filter(d -> "儿科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(2L, ek.get("count"));
        assertEquals(1L, ek.get("pendingCount"));
        assertEquals(1L, ek.get("rejectedCount"));

        // 按月趋势：2025-12 1 条（已通过 1）、2026-11 2 条（待审批 1）、2026-12 3 条（待审批 3）
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byMonth = (List<Map<String, Object>>) s.get("byMonth");
        assertEquals(3, byMonth.size());
        assertEquals("2025-12", byMonth.get(0).get("month"));
        assertEquals(1L, byMonth.get(0).get("count"));
        assertEquals(1L, byMonth.get(0).get("approvedCount"));
        assertEquals(0L, byMonth.get(0).get("pendingCount"));
        assertEquals("2026-11", byMonth.get(1).get("month"));
        assertEquals(2L, byMonth.get(1).get("count"));
        assertEquals(0L, byMonth.get(1).get("approvedCount"));
        assertEquals(1L, byMonth.get(1).get("pendingCount"));
        assertEquals("2026-12", byMonth.get(2).get("month"));
        assertEquals(3L, byMonth.get(2).get("count"));
        assertEquals(0L, byMonth.get(2).get("approvedCount"));
        assertEquals(3L, byMonth.get(2).get("pendingCount"));
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> personnelChangeService.detail(999999L));
    }

    @Test
    void excelRoundTrip() {
        PersonnelChangeQuery q = new PersonnelChangeQuery();
        q.setPage(1);
        q.setSize(100);
        List<PersonnelChange> all = personnelChangeService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PersonnelChangeExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<PersonnelChange> back = PersonnelChangeExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（人员姓名、变动类型、原科室、调入科室、生效日期、申请日期、审批状态）
        PersonnelChange first = all.stream()
                .filter(c -> "刘志强".equals(c.getPhysicianName()))
                .findFirst().orElseThrow();
        PersonnelChange firstBack = back.stream()
                .filter(c -> "刘志强".equals(c.getPhysicianName()))
                .findFirst().orElseThrow();
        assertEquals(first.getChangeType(), firstBack.getChangeType());
        assertEquals(first.getFromDeptName(), firstBack.getFromDeptName());
        assertEquals(first.getToDeptName(), firstBack.getToDeptName());
        assertEquals(first.getChangeDate(), firstBack.getChangeDate());
        assertEquals(first.getApplyDate(), firstBack.getApplyDate());
        assertEquals(first.getStatus(), firstBack.getStatus());
        assertEquals(first.getReason(), firstBack.getReason());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条变动 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （变动用逻辑删除，删后重导按“同一人员同一变动类型同一生效日期重复”规则会跳过种子行，只补回被删的那条）
        PersonnelChange fresh = newChange("导入测试人员", "TRANSFER");
        fresh.setChangeDate(LocalDate.of(2026, 12, 20));
        assertTrue(personnelChangeService.save(fresh));

        PersonnelChangeQuery q = new PersonnelChangeQuery();
        q.setPage(1);
        q.setSize(100);
        List<PersonnelChange> all = personnelChangeService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PersonnelChangeExcelUtil.write(all, out);

        assertTrue(personnelChangeService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, personnelChangeService.listByQuery(q).size());

        int imported = personnelChangeService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因重复而跳过，只补回导入测试人员这一条
        assertEquals(all.size(), personnelChangeService.listByQuery(q).size());
        PersonnelChange restored = personnelChangeService.listByQuery(q).stream()
                .filter(c -> "导入测试人员".equals(c.getPhysicianName()))
                .findFirst().orElseThrow();
        assertEquals("TRANSFER", restored.getChangeType());
        assertEquals("中医内科", restored.getFromDeptName());
        assertEquals("针灸推拿科", restored.getToDeptName());
        assertEquals(0, restored.getStatus()); // 导入的变动默认待审批
        assertTrue(personnelChangeService.removeById(restored.getId()));
        assertEquals(all.size() - 1, personnelChangeService.listByQuery(q).size());
    }

    private PersonnelChange newChange(String physicianName, String changeType) {
        PersonnelChange c = new PersonnelChange();
        c.setPhysicianName(physicianName);
        c.setChangeType(changeType);
        c.setFromDeptId(1L);
        c.setFromDeptName("中医内科");
        c.setToDeptId(2L);
        c.setToDeptName("针灸推拿科");
        c.setChangeDate(LocalDate.of(2026, 12, 10));
        c.setReason("变动原因（测试）");
        return c;
    }
}
