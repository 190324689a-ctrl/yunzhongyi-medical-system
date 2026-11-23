package com.yunzhongyi.medical;

import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.leave.dto.LeaveQuery;
import com.yunzhongyi.medical.leave.entity.LeaveApplication;
import com.yunzhongyi.medical.leave.service.LeaveApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 休假执业模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据 6 条，覆盖 5 种申请类型、4 种审批状态。
 */
@SpringBootTest
@ActiveProfiles("h2")
class LeaveApplicationTests {

    @Autowired
    private LeaveApplicationService leaveService;

    @Test
    void contextLoads() {
        assertNotNull(leaveService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(leaveService.count() >= 6);
        // 种子里唯一的手术分级授权申请：王建军，三级，待科主任审批
        LeaveQuery q = new LeaveQuery();
        q.setApplyType("SURGERY_AUTH");
        PageResult<LeaveApplication> r = leaveService.pageQuery(q);
        assertEquals(1, r.getTotal());
        assertEquals("王建军", r.getRecords().get(0).getPhysicianName());
        assertEquals(3, r.getRecords().get(0).getSurgeryLevel());
        assertEquals(0, r.getRecords().get(0).getStatus());
    }

    @Test
    void pageQueryWithFilters() {
        LeaveQuery q = new LeaveQuery();
        q.setStatus(2);
        assertTrue(leaveService.pageQuery(q).getTotal() >= 1); // 已通过：陈国强

        LeaveQuery q2 = new LeaveQuery();
        q2.setKeyword("林晓梅");
        assertEquals(1, leaveService.pageQuery(q2).getTotal());

        LeaveQuery q3 = new LeaveQuery();
        q3.setApplyType("LEAVE");
        assertTrue(leaveService.pageQuery(q3).getTotal() >= 2);
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = leaveService.stats();
        assertTrue((Long) s.get("total") >= 6);
        assertTrue((Long) s.get("pending") >= 4);
        assertTrue((Long) s.get("approved") >= 1);
        assertTrue((Long) s.get("rejected") >= 1);
    }

    @Test
    void approveFlowTwoLevels() {
        LeaveApplication a = new LeaveApplication();
        a.setPhysicianId(1L);
        a.setPhysicianName("审批流测试");
        a.setDeptId(1L);
        a.setDeptName("中医内科");
        a.setApplyType("LEAVE");
        a.setStartDate(LocalDate.of(2026, 12, 1));
        a.setEndDate(LocalDate.of(2026, 12, 3));
        a.setReason("测试多级审批");
        assertTrue(leaveService.save(a));
        assertEquals(0, a.getStatus());
        assertEquals(3, a.getDays()); // 含首尾 3 天

        LeaveApplication step1 = leaveService.approve(a.getId(), "科主任", "同意");
        assertEquals(1, step1.getStatus()); // 待医务科审批
        assertTrue(step1.getApprovalHistory().contains("科主任"));

        LeaveApplication step2 = leaveService.approve(a.getId(), "医务科", "同意");
        assertEquals(2, step2.getStatus()); // 已通过
        assertTrue(step2.getApprovalHistory().contains("医务科"));

        // 已通过后不能再审批
        assertThrows(IllegalStateException.class, () -> leaveService.approve(a.getId(), "医务科", "同意"));
        assertTrue(leaveService.removeById(a.getId()));
    }

    @Test
    void rejectFlow() {
        LeaveApplication a = new LeaveApplication();
        a.setPhysicianId(2L);
        a.setPhysicianName("驳回测试");
        a.setDeptId(2L);
        a.setDeptName("针灸推拿科");
        a.setApplyType("PRACTICE");
        a.setReason("测试驳回");
        assertTrue(leaveService.save(a));

        LeaveApplication r = leaveService.reject(a.getId(), "科主任", "材料不全");
        assertEquals(3, r.getStatus());
        assertTrue(r.getApprovalHistory().contains("驳回"));

        assertThrows(IllegalStateException.class, () -> leaveService.approve(a.getId(), "医务科", "同意"));
        assertTrue(leaveService.removeById(a.getId()));
    }

    @Test
    void invalidDateRangeRejected() {
        LeaveApplication a = new LeaveApplication();
        a.setPhysicianId(1L);
        a.setPhysicianName("日期测试");
        a.setApplyType("LEAVE");
        a.setStartDate(LocalDate.of(2026, 12, 5));
        a.setEndDate(LocalDate.of(2026, 12, 1));
        assertThrows(IllegalArgumentException.class, () -> leaveService.save(a));
    }

    @Test
    void crudRoundTrip() {
        LeaveApplication a = new LeaveApplication();
        a.setPhysicianId(5L);
        a.setPhysicianName("增删改测试");
        a.setDeptId(5L);
        a.setDeptName("儿科");
        a.setApplyType("TRAINING");
        a.setDestination("广东省中医院");
        a.setReason("测试");
        assertTrue(leaveService.save(a));
        assertNotNull(a.getId());

        LeaveApplication found = leaveService.getById(a.getId());
        assertEquals("增删改测试", found.getPhysicianName());

        found.setReason("修改后");
        assertTrue(leaveService.updateById(found));
        assertEquals("修改后", leaveService.getById(a.getId()).getReason());

        assertTrue(leaveService.removeById(a.getId()));
        assertNull(leaveService.getById(a.getId()));
    }
}
