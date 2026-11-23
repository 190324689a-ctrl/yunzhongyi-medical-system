package com.yunzhongyi.medical;

import com.yunzhongyi.medical.schedule.dto.ScheduleChangeRequest;
import com.yunzhongyi.medical.schedule.dto.ScheduleGenerateRequest;
import com.yunzhongyi.medical.schedule.dto.ScheduleQuery;
import com.yunzhongyi.medical.schedule.dto.ScheduleTemplateQuery;
import com.yunzhongyi.medical.schedule.entity.Schedule;
import com.yunzhongyi.medical.schedule.entity.ScheduleTemplate;
import com.yunzhongyi.medical.schedule.excel.ScheduleExcelUtil;
import com.yunzhongyi.medical.schedule.service.ScheduleService;
import com.yunzhongyi.medical.schedule.service.ScheduleShiftUtil;
import com.yunzhongyi.medical.schedule.service.ScheduleTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 排班管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：2 个排班模板、8 条排班记录，覆盖科室/门诊/住院/急诊 4 种排班类型、
 * 白班/上午班/下午班/夜班/休息 5 种班次，含 1 条待科主任审批的调班申请。
 */
@SpringBootTest
@ActiveProfiles("h2")
class ScheduleTests {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleTemplateService templateService;

    private static final String TEMPLATE_1_PATTERN =
            "[{\"dayOfWeek\":1,\"shiftCode\":\"DAY\"},{\"dayOfWeek\":2,\"shiftCode\":\"DAY\"},"
                    + "{\"dayOfWeek\":3,\"shiftCode\":\"DAY\"},{\"dayOfWeek\":4,\"shiftCode\":\"DAY\"},"
                    + "{\"dayOfWeek\":5,\"shiftCode\":\"DAY\"},{\"dayOfWeek\":6,\"shiftCode\":\"MORNING\"},"
                    + "{\"dayOfWeek\":7,\"shiftCode\":\"REST\"}]";

    @Test
    void contextLoads() {
        assertNotNull(scheduleService);
        assertNotNull(templateService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(templateService.count() >= 2);
        assertTrue(scheduleService.count() >= 8);

        ScheduleTemplate t = templateService.getById(1L);
        assertEquals("中医内科常规排班模板", t.getTemplateName());
        assertEquals("DEPT", t.getScheduleType());
        assertEquals(1, t.getStatus());
        assertEquals("DAY", ScheduleShiftUtil.shiftForDayOfWeek(t.getShiftPattern(), 1)); // 周一白班
        assertEquals("MORNING", ScheduleShiftUtil.shiftForDayOfWeek(t.getShiftPattern(), 6)); // 周六上午班
        assertEquals("REST", ScheduleShiftUtil.shiftForDayOfWeek(t.getShiftPattern(), 7)); // 周日休息

        // 种子排班：陈国强 2026-11-23（周一）科室白班 08:00-17:30
        ScheduleQuery q = new ScheduleQuery();
        q.setKeyword("陈国强");
        assertEquals(1, scheduleService.pageQuery(q).getTotal());
        Schedule chen = scheduleService.pageQuery(q).getRecords().get(0);
        assertEquals(LocalDate.of(2026, 11, 23), chen.getScheduleDate());
        assertEquals("DEPT", chen.getScheduleType());
        assertEquals("DAY", chen.getShiftCode());
        assertEquals(LocalTime.of(8, 0), chen.getStartTime());
        assertEquals(LocalTime.of(17, 30), chen.getEndTime());
        assertEquals(1L, chen.getTemplateId());

        // 种子待审批调班：李雅琴 2026-11-25 门诊下午班，申请调至 2026-11-26 白班
        Schedule swap = scheduleService.getById(7L);
        assertEquals("李雅琴", swap.getPhysicianName());
        assertEquals("SWAP", swap.getChangeType());
        assertEquals(1, swap.getChangeStatus());
        assertEquals(LocalDate.of(2026, 11, 26), swap.getTargetDate());
        assertEquals("DAY", swap.getTargetShiftCode());
    }

    @Test
    void pageQueryWithFilters() {
        ScheduleQuery q = new ScheduleQuery();
        q.setScheduleType("DEPT");
        assertEquals(3, scheduleService.pageQuery(q).getTotal()); // 陈国强、赵丽华、黄志明

        ScheduleQuery q2 = new ScheduleQuery();
        q2.setScheduleType("OUTPATIENT");
        assertEquals(3, scheduleService.pageQuery(q2).getTotal()); // 林晓梅、孙小红、李雅琴

        ScheduleQuery q3 = new ScheduleQuery();
        q3.setShiftCode("NIGHT");
        assertEquals(2, scheduleService.pageQuery(q3).getTotal()); // 王建军、刘志强

        ScheduleQuery q4 = new ScheduleQuery();
        q4.setDeptId(1L);
        assertEquals(2, scheduleService.pageQuery(q4).getTotal()); // 中医内科：陈国强、李雅琴

        ScheduleQuery q5 = new ScheduleQuery();
        q5.setStartDate(LocalDate.of(2026, 11, 23));
        q5.setEndDate(LocalDate.of(2026, 11, 23));
        assertEquals(4, scheduleService.pageQuery(q5).getTotal()); // 11 月 23 日 4 人排班

        ScheduleQuery q6 = new ScheduleQuery();
        q6.setChangeStatus(1);
        assertEquals(1, scheduleService.pageQuery(q6).getTotal()); // 待审批调班：李雅琴

        ScheduleQuery q7 = new ScheduleQuery();
        q7.setPhysicianId(6L);
        assertEquals(1, scheduleService.pageQuery(q7).getTotal()); // 刘志强急诊夜班

        ScheduleQuery q8 = new ScheduleQuery();
        q8.setKeyword("王建军");
        assertEquals(1, scheduleService.pageQuery(q8).getTotal());
    }

    @Test
    void saveValidationAndDefaultTimes() {
        // 同一医师同一天重复排班：陈国强 2026-11-23 已有种子排班，拒绝保存
        Schedule dup = newSchedule(1L, "陈国强", 1L, "中医内科",
                LocalDate.of(2026, 11, 23), "DEPT", "DAY");
        assertThrows(IllegalArgumentException.class, () -> scheduleService.save(dup));

        // 上午班不填时间段：按班次默认时间补全 08:00-12:00
        Schedule morning = newSchedule(12L, "郑秀兰", 4L, "妇科",
                LocalDate.of(2026, 12, 12), "OUTPATIENT", "MORNING");
        try {
            assertTrue(scheduleService.save(morning));
            assertEquals(LocalTime.of(8, 0), morning.getStartTime());
            assertEquals(LocalTime.of(12, 0), morning.getEndTime());
            assertEquals(0, morning.getChangeStatus());
            assertEquals(0, morning.getHisSyncStatus());

            // 改到同一医师已有排班的那天：更新时同样拒绝
            Schedule other = newSchedule(12L, "郑秀兰", 4L, "妇科",
                    LocalDate.of(2026, 12, 13), "DEPT", "DAY");
            assertTrue(scheduleService.save(other));
            try {
                other.setScheduleDate(LocalDate.of(2026, 12, 12));
                assertThrows(IllegalArgumentException.class, () -> scheduleService.updateById(other));
            } finally {
                scheduleService.removeById(other.getId());
            }

            // 休息：即使填了时间段也会被清空
            Schedule rest = newSchedule(12L, "郑秀兰", 4L, "妇科",
                    LocalDate.of(2026, 12, 14), "DEPT", "REST");
            rest.setStartTime(LocalTime.of(8, 0));
            rest.setEndTime(LocalTime.of(17, 30));
            assertTrue(scheduleService.save(rest));
            assertNull(rest.getStartTime());
            assertNull(rest.getEndTime());
            scheduleService.removeById(rest.getId());
        } finally {
            scheduleService.removeById(morning.getId());
        }
    }

    @Test
    void generateByTemplateWithConflictSkip() {
        // 中医内科常规模板给孙小红排 2026-11-23（周一）至 2026-11-29（周日）：
        // 周一至周五白班、周六上午班、周日休息；其中 11 月 24 日已有种子排班，应跳过
        ScheduleGenerateRequest req = new ScheduleGenerateRequest();
        req.setTemplateId(1L);
        req.setStartDate(LocalDate.of(2026, 11, 23));
        req.setEndDate(LocalDate.of(2026, 11, 29));
        req.setPhysicianIds(List.of(5L));
        Map<String, Object> result = scheduleService.generateByTemplate(req);
        assertEquals(5, result.get("created"));   // 23、25、26、27 日白班 + 28 日上午班
        assertEquals(1, result.get("skipped"));   // 24 日冲突跳过
        assertEquals(1, result.get("restDays"));  // 29 日周日休息
        assertEquals(6, result.get("total"));

        try {
            // 生成的记录带模板信息与默认时间段
            ScheduleQuery q = new ScheduleQuery();
            q.setPhysicianId(5L);
            q.setTemplateId(1L);
            assertEquals(5, scheduleService.pageQuery(q).getTotal());
            Schedule sat = scheduleService.listByQuery(q).stream()
                    .filter(s -> LocalDate.of(2026, 11, 28).equals(s.getScheduleDate()))
                    .findFirst().orElseThrow();
            assertEquals("MORNING", sat.getShiftCode());
            assertEquals(LocalTime.of(8, 0), sat.getStartTime());
            assertEquals(LocalTime.of(12, 0), sat.getEndTime());
            assertEquals("中医内科常规排班模板", sat.getTemplateName());

            // 同样范围再生成一次：全部冲突，0 新建
            Map<String, Object> again = scheduleService.generateByTemplate(req);
            assertEquals(0, again.get("created"));
            assertEquals(6, again.get("skipped"));
        } finally {
            ScheduleQuery q = new ScheduleQuery();
            q.setPhysicianId(5L);
            q.setTemplateId(1L);
            scheduleService.removeByIds(scheduleService.listByQuery(q).stream().map(Schedule::getId).toList());
        }

        // 结束日期早于开始日期，拒绝
        ScheduleGenerateRequest bad = new ScheduleGenerateRequest();
        bad.setTemplateId(1L);
        bad.setStartDate(LocalDate.of(2026, 11, 29));
        bad.setEndDate(LocalDate.of(2026, 11, 23));
        bad.setPhysicianIds(List.of(5L));
        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateByTemplate(bad));

        // 不存在的模板，拒绝
        ScheduleGenerateRequest noTemplate = new ScheduleGenerateRequest();
        noTemplate.setTemplateId(9999L);
        noTemplate.setStartDate(LocalDate.of(2026, 11, 23));
        noTemplate.setEndDate(LocalDate.of(2026, 11, 24));
        noTemplate.setPhysicianIds(List.of(5L));
        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateByTemplate(noTemplate));

        // 既不指定人员也不指定科室，拒绝
        ScheduleGenerateRequest noPeople = new ScheduleGenerateRequest();
        noPeople.setTemplateId(1L);
        noPeople.setStartDate(LocalDate.of(2026, 11, 23));
        noPeople.setEndDate(LocalDate.of(2026, 11, 24));
        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateByTemplate(noPeople));
    }

    @Test
    void swapApprovalFlow() {
        Schedule s = newSchedule(12L, "郑秀兰", 4L, "妇科",
                LocalDate.of(2026, 12, 1), "DEPT", "DAY");
        assertTrue(scheduleService.save(s));
        try {
            // 申请调班：12 月 1 日白班 → 12 月 2 日夜班
            ScheduleChangeRequest req = new ScheduleChangeRequest();
            req.setTargetDate(LocalDate.of(2026, 12, 2));
            req.setTargetShiftCode("NIGHT");
            req.setChangeReason("12月1日家中有事");
            Schedule applied = scheduleService.applySwap(s.getId(), req);
            assertEquals("SWAP", applied.getChangeType());
            assertEquals(1, applied.getChangeStatus());
            assertEquals(LocalDate.of(2026, 12, 2), applied.getTargetDate());
            assertEquals("NIGHT", applied.getTargetShiftCode());
            assertEquals(LocalTime.of(17, 30), applied.getTargetStartTime()); // 夜班默认时间补全
            assertEquals(LocalTime.of(8, 0), applied.getTargetEndTime());

            // 科主任通过：日期与班次写回本记录，状态变为已通过，审批记录留痕
            Schedule approved = scheduleService.approveChange(s.getId(), "陈国强", "同意");
            assertEquals(LocalDate.of(2026, 12, 2), approved.getScheduleDate());
            assertEquals("NIGHT", approved.getShiftCode());
            assertEquals(LocalTime.of(17, 30), approved.getStartTime());
            assertEquals(LocalTime.of(8, 0), approved.getEndTime());
            assertEquals(2, approved.getChangeStatus());
            assertTrue(approved.getApprovalHistory().contains("科主任"));
            assertTrue(approved.getApprovalHistory().contains("通过"));
            assertTrue(approved.getApprovalHistory().contains("调班"));

            // 已结束审批的申请不能再通过
            assertThrows(IllegalStateException.class,
                    () -> scheduleService.approveChange(s.getId(), "陈国强", "再次通过"));
        } finally {
            scheduleService.removeById(s.getId());
        }
    }

    @Test
    void substituteApprovalFlowAndReject() {
        Schedule s = newSchedule(10L, "张伟", 3L, "骨伤科",
                LocalDate.of(2026, 12, 3), "DEPT", "DAY");
        assertTrue(scheduleService.save(s));
        try {
            // 申请代班：张伟 → 郑秀兰
            ScheduleChangeRequest req = new ScheduleChangeRequest();
            req.setTargetPhysicianId(12L);
            req.setChangeReason("12月3日外出学习");
            Schedule applied = scheduleService.applySubstitute(s.getId(), req);
            assertEquals("SUBSTITUTE", applied.getChangeType());
            assertEquals(1, applied.getChangeStatus());
            assertEquals(12L, applied.getTargetPhysicianId());
            assertEquals("郑秀兰", applied.getTargetPhysicianName());

            // 科主任通过：人员换成郑秀兰（科室随之变为妇科），状态已通过
            Schedule approved = scheduleService.approveChange(s.getId(), "王建军", "同意代班");
            assertEquals(12L, approved.getPhysicianId());
            assertEquals("郑秀兰", approved.getPhysicianName());
            assertEquals(4L, approved.getDeptId());
            assertEquals("妇科", approved.getDeptName());
            assertEquals(2, approved.getChangeStatus());
            assertTrue(approved.getApprovalHistory().contains("代班"));
        } finally {
            scheduleService.removeById(s.getId());
        }

        // 驳回流程：原排班不变，状态变为已驳回；驳回后不能再通过
        Schedule s2 = newSchedule(10L, "张伟", 3L, "骨伤科",
                LocalDate.of(2026, 12, 4), "DEPT", "DAY");
        assertTrue(scheduleService.save(s2));
        try {
            ScheduleChangeRequest req = new ScheduleChangeRequest();
            req.setTargetPhysicianId(12L);
            req.setChangeReason("12月4日外出学习");
            scheduleService.applySubstitute(s2.getId(), req);
            Schedule rejected = scheduleService.rejectChange(s2.getId(), "王建军", "人手不足，暂不同意");
            assertEquals(3, rejected.getChangeStatus());
            assertEquals(10L, rejected.getPhysicianId()); // 仍是张伟
            assertEquals("张伟", rejected.getPhysicianName());
            assertTrue(rejected.getApprovalHistory().contains("驳回"));
            assertThrows(IllegalStateException.class,
                    () -> scheduleService.approveChange(s2.getId(), "王建军", "改主意了"));
        } finally {
            scheduleService.removeById(s2.getId());
        }
    }

    @Test
    void approveChangeConflictRejected() {
        // 陈国强与林晓梅 2026-12-05 都有排班；林晓梅申请由陈国强代班，通过时应因冲突被拒绝
        Schedule a = newSchedule(1L, "陈国强", 1L, "中医内科",
                LocalDate.of(2026, 12, 5), "DEPT", "DAY");
        Schedule b = newSchedule(2L, "林晓梅", 2L, "针灸推拿科",
                LocalDate.of(2026, 12, 5), "DEPT", "DAY");
        assertTrue(scheduleService.save(a));
        assertTrue(scheduleService.save(b));
        try {
            ScheduleChangeRequest req = new ScheduleChangeRequest();
            req.setTargetPhysicianId(1L);
            req.setChangeReason("请陈国强代班");
            scheduleService.applySubstitute(b.getId(), req);
            assertThrows(IllegalArgumentException.class,
                    () -> scheduleService.approveChange(b.getId(), "林晓梅", "同意"));
            // 审批未通过，申请仍是待审批状态，原排班不变
            assertEquals(1, scheduleService.getById(b.getId()).getChangeStatus());
            assertEquals(2L, scheduleService.getById(b.getId()).getPhysicianId());

            // 有待审批申请时不能再提新的调班/代班申请
            ScheduleChangeRequest again = new ScheduleChangeRequest();
            again.setTargetDate(LocalDate.of(2026, 12, 6));
            again.setTargetShiftCode("DAY");
            assertThrows(IllegalStateException.class, () -> scheduleService.applySwap(b.getId(), again));

            // 代班目标不能是本人
            ScheduleChangeRequest self = new ScheduleChangeRequest();
            self.setTargetPhysicianId(1L);
            assertThrows(IllegalArgumentException.class, () -> scheduleService.applySubstitute(a.getId(), self));
        } finally {
            scheduleService.removeById(a.getId());
            scheduleService.removeById(b.getId());
        }

        // HIS 同步：未配置对接，明确抛错、绝不伪造成功
        assertThrows(UnsupportedOperationException.class, () -> scheduleService.syncToHis(1L));
        assertEquals(0, scheduleService.getById(1L).getHisSyncStatus()); // 仍是未同步
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = scheduleService.stats();
        assertEquals(8L, s.get("total"));
        assertEquals(1L, s.get("pendingChange")); // 待审批调班：李雅琴
        assertEquals(2L, s.get("nightCount")); // 夜班：王建军、刘志强
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(3L, byType.get("DEPT"));
        assertEquals(3L, byType.get("OUTPATIENT"));
        assertEquals(1L, byType.get("INPATIENT"));
        assertEquals(1L, byType.get("EMERGENCY"));
        Map<?, ?> byShift = (Map<?, ?>) s.get("byShift");
        assertEquals(2L, byShift.get("DAY"));
        assertEquals(2L, byShift.get("MORNING"));
        assertEquals(1L, byShift.get("AFTERNOON"));
        assertEquals(2L, byShift.get("NIGHT"));
        assertEquals(1L, byShift.get("REST"));

        // 按科室汇总：中医内科 2 条（陈国强、李雅琴）
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        Map<String, Object> nk = byDept.stream()
                .filter(d -> "中医内科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(2L, nk.get("count"));

        // 按人员汇总：每人 1 条，夜班各 1 天的是王建军与刘志强
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byPhysician = (List<Map<String, Object>>) s.get("byPhysician");
        assertEquals(8, byPhysician.size());
        Map<String, Object> wang = byPhysician.stream()
                .filter(d -> "王建军".equals(d.get("physicianName"))).findFirst().orElseThrow();
        assertEquals(1L, wang.get("count"));
        assertEquals(1L, wang.get("nightCount"));
    }

    @Test
    void templateCrudAndPatternValidation() {
        // 星期重复，拒绝保存
        ScheduleTemplate badDay = new ScheduleTemplate();
        badDay.setTemplateName("星期重复模板");
        badDay.setScheduleType("DEPT");
        badDay.setShiftPattern("[{\"dayOfWeek\":1,\"shiftCode\":\"DAY\"},{\"dayOfWeek\":1,\"shiftCode\":\"NIGHT\"}]");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(badDay));

        // 班次编码非法，拒绝保存
        ScheduleTemplate badShift = new ScheduleTemplate();
        badShift.setTemplateName("班次错误模板");
        badShift.setScheduleType("DEPT");
        badShift.setShiftPattern("[{\"dayOfWeek\":1,\"shiftCode\":\"XXX\"}]");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(badShift));

        // 班次组合为空，拒绝保存
        ScheduleTemplate empty = new ScheduleTemplate();
        empty.setTemplateName("空模板");
        empty.setScheduleType("DEPT");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(empty));

        // 合法模板正常保存；默认启用
        ScheduleTemplate t = new ScheduleTemplate();
        t.setTemplateName("测试模板");
        t.setDeptId(5L);
        t.setDeptName("儿科");
        t.setScheduleType("OUTPATIENT");
        t.setShiftPattern(TEMPLATE_1_PATTERN);
        assertTrue(templateService.save(t));
        assertNotNull(t.getId());
        assertEquals(1, t.getStatus());

        ScheduleTemplate found = templateService.getById(t.getId());
        assertEquals("测试模板", found.getTemplateName());

        // 停用后不在启用列表里
        found.setStatus(0);
        assertTrue(templateService.updateById(found));
        assertTrue(templateService.listEnabled().stream().noneMatch(x -> x.getId().equals(t.getId())));

        // 启用列表仍包含 2 个种子模板
        assertTrue(templateService.listEnabled().size() >= 2);

        // 停用的模板不能用于批量生成
        ScheduleGenerateRequest req = new ScheduleGenerateRequest();
        req.setTemplateId(t.getId());
        req.setStartDate(LocalDate.of(2026, 12, 7));
        req.setEndDate(LocalDate.of(2026, 12, 7));
        req.setPhysicianIds(List.of(5L));
        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateByTemplate(req));

        assertTrue(templateService.removeById(t.getId()));
        assertNull(templateService.getById(t.getId()));
    }

    @Test
    void excelRoundTrip() {
        ScheduleQuery q = new ScheduleQuery();
        q.setPage(1);
        q.setSize(100);
        List<Schedule> all = scheduleService.listByQuery(q);
        assertTrue(all.size() >= 8);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScheduleExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<Schedule> back = ScheduleExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        // 导出再读回：关键字段一致（医师、日期、类型、班次、时间段）
        Schedule first = all.get(0);
        Schedule firstBack = back.stream()
                .filter(s -> first.getPhysicianName().equals(s.getPhysicianName())
                        && first.getScheduleDate().equals(s.getScheduleDate()))
                .findFirst().orElseThrow();
        assertEquals(first.getScheduleType(), firstBack.getScheduleType());
        assertEquals(first.getShiftCode(), firstBack.getShiftCode());
        assertEquals(first.getStartTime(), firstBack.getStartTime());
        assertEquals(first.getEndTime(), firstBack.getEndTime());
        // 休息记录时间段为空，往返后仍为空
        Schedule restBack = back.stream()
                .filter(s -> "REST".equals(s.getShiftCode())).findFirst().orElseThrow();
        assertNull(restBack.getStartTime());
        assertNull(restBack.getEndTime());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条排班 → 导出全部 → 删掉这条 → 再导回同一文件，应只补回这一条
        // （项目用逻辑删除，删后重导按“同一医师同一天重复”规则会跳过种子行，只补回被删的那条）
        Schedule fresh = newSchedule(12L, "郑秀兰", 4L, "妇科",
                LocalDate.of(2026, 12, 7), "OUTPATIENT", "MORNING");
        assertTrue(scheduleService.save(fresh));

        ScheduleQuery q = new ScheduleQuery();
        q.setPage(1);
        q.setSize(100);
        List<Schedule> all = scheduleService.listByQuery(q);
        assertTrue(all.size() >= 9);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScheduleExcelUtil.write(all, out);

        assertTrue(scheduleService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, scheduleService.listByQuery(q).size());

        int imported = scheduleService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 8 条种子因同一医师同一天重复而跳过，只补回郑秀兰这一条
        assertEquals(all.size(), scheduleService.listByQuery(q).size());
        Schedule restored = scheduleService.listByQuery(q).stream()
                .filter(s -> "郑秀兰".equals(s.getPhysicianName())
                        && LocalDate.of(2026, 12, 7).equals(s.getScheduleDate()))
                .findFirst().orElseThrow();
        assertEquals("OUTPATIENT", restored.getScheduleType());
        assertEquals("MORNING", restored.getShiftCode());
        assertEquals(12L, restored.getPhysicianId()); // 导入时按姓名查医师档案补全医师 id
        assertEquals(LocalTime.of(8, 0), restored.getStartTime());
        assertEquals(LocalTime.of(12, 0), restored.getEndTime());
        assertTrue(scheduleService.removeById(restored.getId()));
        assertEquals(all.size() - 1, scheduleService.listByQuery(q).size());
    }

    private Schedule newSchedule(Long physicianId, String physicianName, Long deptId, String deptName,
                                 LocalDate date, String scheduleType, String shiftCode) {
        Schedule s = new Schedule();
        s.setPhysicianId(physicianId);
        s.setPhysicianName(physicianName);
        s.setDeptId(deptId);
        s.setDeptName(deptName);
        s.setScheduleDate(date);
        s.setScheduleType(scheduleType);
        s.setShiftCode(shiftCode);
        return s;
    }
}
