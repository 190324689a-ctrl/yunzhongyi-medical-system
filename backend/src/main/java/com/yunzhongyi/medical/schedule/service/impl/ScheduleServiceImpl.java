package com.yunzhongyi.medical.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.config.HisIntegrationProperties;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.mapper.PhysicianMapper;
import com.yunzhongyi.medical.schedule.dto.ScheduleChangeRequest;
import com.yunzhongyi.medical.schedule.dto.ScheduleGenerateRequest;
import com.yunzhongyi.medical.schedule.dto.ScheduleQuery;
import com.yunzhongyi.medical.schedule.entity.Schedule;
import com.yunzhongyi.medical.schedule.entity.ScheduleTemplate;
import com.yunzhongyi.medical.schedule.excel.ScheduleExcelUtil;
import com.yunzhongyi.medical.schedule.mapper.ScheduleMapper;
import com.yunzhongyi.medical.schedule.mapper.ScheduleTemplateMapper;
import com.yunzhongyi.medical.schedule.service.ScheduleService;
import com.yunzhongyi.medical.schedule.service.ScheduleShiftUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 排班管理服务实现：排班 CRUD + 按模板批量生成（冲突跳过）+
 * 调班/代班一级审批（科主任）+ 统计 + Excel 批量导入导出 + HIS 同步预留
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl
        extends ServiceImpl<ScheduleMapper, Schedule>
        implements ScheduleService {

    private final PhysicianMapper physicianMapper;
    private final ScheduleTemplateMapper templateMapper;
    private final HisIntegrationProperties hisProperties;

    @Override
    public PageResult<Schedule> pageQuery(ScheduleQuery query) {
        Page<Schedule> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<Schedule> listByQuery(ScheduleQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<Schedule> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pendingChange", all.stream()
                .filter(s -> Integer.valueOf(ScheduleShiftUtil.CHANGE_PENDING).equals(s.getChangeStatus())).count());
        r.put("nightCount", all.stream()
                .filter(s -> ScheduleShiftUtil.SHIFT_NIGHT.equals(s.getShiftCode())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(ScheduleShiftUtil.TYPE_DEPT, ScheduleShiftUtil.TYPE_OUTPATIENT,
                ScheduleShiftUtil.TYPE_INPATIENT, ScheduleShiftUtil.TYPE_EMERGENCY)) {
            byType.put(t, all.stream().filter(s -> t.equals(s.getScheduleType())).count());
        }
        r.put("byType", byType);
        Map<String, Long> byShift = new LinkedHashMap<>();
        for (String s : List.of(ScheduleShiftUtil.SHIFT_DAY, ScheduleShiftUtil.SHIFT_MORNING,
                ScheduleShiftUtil.SHIFT_AFTERNOON, ScheduleShiftUtil.SHIFT_NIGHT, ScheduleShiftUtil.SHIFT_REST)) {
            byShift.put(s, all.stream().filter(x -> s.equals(x.getShiftCode())).count());
        }
        r.put("byShift", byShift);
        r.put("byDept", statsByDept());
        r.put("byPhysician", statsByPhysician());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<Schedule>> grouped = new LinkedHashMap<>();
        for (Schedule s : this.list()) {
            String dept = StringUtils.hasText(s.getDeptName()) ? s.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(s);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("nightCount", list.stream()
                    .filter(s -> ScheduleShiftUtil.SHIFT_NIGHT.equals(s.getShiftCode())).count());
            row.put("emergencyCount", list.stream()
                    .filter(s -> ScheduleShiftUtil.TYPE_EMERGENCY.equals(s.getScheduleType())).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByPhysician() {
        Map<String, List<Schedule>> grouped = new LinkedHashMap<>();
        for (Schedule s : this.list()) {
            String name = StringUtils.hasText(s.getPhysicianName()) ? s.getPhysicianName() : "未知";
            grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(s);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((name, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("physicianName", name);
            row.put("deptName", list.get(0).getDeptName());
            row.put("count", (long) list.size());
            row.put("nightCount", list.stream()
                    .filter(s -> ScheduleShiftUtil.SHIFT_NIGHT.equals(s.getShiftCode())).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateByTemplate(ScheduleGenerateRequest req) {
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new IllegalArgumentException("开始日期和结束日期不能为空");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        ScheduleTemplate template = templateMapper.selectById(req.getTemplateId());
        if (template == null) {
            throw new IllegalArgumentException("排班模板不存在：" + req.getTemplateId());
        }
        if (!Integer.valueOf(1).equals(template.getStatus())) {
            throw new IllegalArgumentException("排班模板「" + template.getTemplateName() + "」已停用，不能用于批量生成");
        }
        List<Physician> physicians = resolvePhysicians(req);
        if (physicians.isEmpty()) {
            throw new IllegalArgumentException("未找到可排班的医师，请指定人员或科室");
        }
        int created = 0;
        int skipped = 0;
        int restDays = 0;
        int total = 0;
        for (Physician p : physicians) {
            for (LocalDate date = req.getStartDate(); !date.isAfter(req.getEndDate()); date = date.plusDays(1)) {
                String shiftCode = ScheduleShiftUtil.shiftForDayOfWeek(
                        template.getShiftPattern(), date.getDayOfWeek().getValue());
                if (shiftCode == null) {
                    continue; // 模板未列出这天：不排班
                }
                if (ScheduleShiftUtil.SHIFT_REST.equals(shiftCode)) {
                    restDays++;
                    continue; // 休息：不建排班记录
                }
                total++;
                if (hasConflict(p.getId(), date, null)) {
                    log.warn("批量生成跳过：医师 {} 在 {} 已有排班", p.getName(), date);
                    skipped++;
                    continue;
                }
                Schedule s = new Schedule();
                s.setPhysicianId(p.getId());
                s.setPhysicianName(p.getName());
                s.setDeptId(p.getDeptId());
                s.setDeptName(p.getDeptName());
                s.setScheduleDate(date);
                s.setScheduleType(template.getScheduleType());
                s.setShiftCode(shiftCode);
                s.setStartTime(ScheduleShiftUtil.defaultStartTime(shiftCode));
                s.setEndTime(ScheduleShiftUtil.defaultEndTime(shiftCode));
                s.setTemplateId(template.getId());
                s.setTemplateName(template.getTemplateName());
                s.setChangeStatus(ScheduleShiftUtil.CHANGE_NONE);
                s.setHisSyncStatus(0);
                this.save(s);
                created++;
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("created", created);
        r.put("skipped", skipped);
        r.put("restDays", restDays);
        r.put("total", total);
        r.put("templateName", template.getTemplateName());
        return r;
    }

    /** 批量生成的人员来源：指定人员优先，否则取指定科室的全部在职医师 */
    private List<Physician> resolvePhysicians(ScheduleGenerateRequest req) {
        List<Physician> result = new ArrayList<>();
        if (req.getPhysicianIds() != null && !req.getPhysicianIds().isEmpty()) {
            for (Long id : req.getPhysicianIds()) {
                Physician p = physicianMapper.selectById(id);
                if (p == null) {
                    throw new IllegalArgumentException("医师不存在：" + id);
                }
                result.add(p);
            }
            return result;
        }
        if (req.getDeptId() != null) {
            return physicianMapper.selectList(new LambdaQueryWrapper<Physician>()
                    .eq(Physician::getDeptId, req.getDeptId())
                    .orderByAsc(Physician::getId));
        }
        throw new IllegalArgumentException("请指定排班人员或科室");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Schedule applySwap(Long id, ScheduleChangeRequest req) {
        Schedule s = getOrThrow(id);
        requireNoPendingChange(s);
        if (req.getTargetDate() == null) {
            throw new IllegalArgumentException("调班目标日期不能为空");
        }
        if (!StringUtils.hasText(req.getTargetShiftCode())
                || !ScheduleShiftUtil.isValidShift(req.getTargetShiftCode())) {
            throw new IllegalArgumentException("调班目标班次不正确");
        }
        s.setChangeType(ScheduleShiftUtil.CHANGE_SWAP);
        s.setChangeStatus(ScheduleShiftUtil.CHANGE_PENDING);
        s.setTargetDate(req.getTargetDate());
        s.setTargetShiftCode(req.getTargetShiftCode());
        s.setTargetStartTime(req.getTargetStartTime() != null ? req.getTargetStartTime()
                : ScheduleShiftUtil.defaultStartTime(req.getTargetShiftCode()));
        s.setTargetEndTime(req.getTargetEndTime() != null ? req.getTargetEndTime()
                : ScheduleShiftUtil.defaultEndTime(req.getTargetShiftCode()));
        s.setTargetPhysicianId(null);
        s.setTargetPhysicianName(null);
        s.setChangeReason(req.getChangeReason());
        s.setApplyDate(LocalDate.now());
        s.setApprovalOpinion(null);
        this.updateById(s);
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Schedule applySubstitute(Long id, ScheduleChangeRequest req) {
        Schedule s = getOrThrow(id);
        requireNoPendingChange(s);
        if (req.getTargetPhysicianId() == null) {
            throw new IllegalArgumentException("代班目标医师不能为空");
        }
        if (req.getTargetPhysicianId().equals(s.getPhysicianId())) {
            throw new IllegalArgumentException("代班目标医师不能是本人");
        }
        Physician target = physicianMapper.selectById(req.getTargetPhysicianId());
        if (target == null) {
            throw new IllegalArgumentException("代班目标医师不存在：" + req.getTargetPhysicianId());
        }
        s.setChangeType(ScheduleShiftUtil.CHANGE_SUBSTITUTE);
        s.setChangeStatus(ScheduleShiftUtil.CHANGE_PENDING);
        s.setTargetPhysicianId(target.getId());
        s.setTargetPhysicianName(target.getName());
        s.setTargetDate(null);
        s.setTargetShiftCode(null);
        s.setTargetStartTime(null);
        s.setTargetEndTime(null);
        s.setChangeReason(req.getChangeReason());
        s.setApplyDate(LocalDate.now());
        s.setApprovalOpinion(null);
        this.updateById(s);
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Schedule approveChange(Long id, String approver, String opinion) {
        Schedule s = getOrThrow(id);
        if (!Integer.valueOf(ScheduleShiftUtil.CHANGE_PENDING).equals(s.getChangeStatus())) {
            throw new IllegalStateException("该排班没有待审批的调班/代班申请，不能通过");
        }
        String step = "科主任";
        if (ScheduleShiftUtil.CHANGE_SWAP.equals(s.getChangeType())) {
            // 调班后仍不能冲突：同一医师在目标日期已有其他排班时不予通过
            if (hasConflict(s.getPhysicianId(), s.getTargetDate(), s.getId())) {
                throw new IllegalArgumentException("调班无法通过：该医师在 " + s.getTargetDate() + " 已有排班");
            }
            String from = s.getScheduleDate() + " " + ScheduleShiftUtil.shiftText(s.getShiftCode());
            String to = s.getTargetDate() + " " + ScheduleShiftUtil.shiftText(s.getTargetShiftCode());
            appendHistory(s, step, approver, "通过（调班 " + from + " → " + to + "）", opinion);
            s.setScheduleDate(s.getTargetDate());
            s.setShiftCode(s.getTargetShiftCode());
            s.setStartTime(s.getTargetStartTime());
            s.setEndTime(s.getTargetEndTime());
        } else if (ScheduleShiftUtil.CHANGE_SUBSTITUTE.equals(s.getChangeType())) {
            // 代班后仍不能冲突：目标医师当天已有排班时不予通过
            if (hasConflict(s.getTargetPhysicianId(), s.getScheduleDate(), s.getId())) {
                throw new IllegalArgumentException("代班无法通过：医师 " + s.getTargetPhysicianName()
                        + " 在 " + s.getScheduleDate() + " 已有排班");
            }
            Physician target = physicianMapper.selectById(s.getTargetPhysicianId());
            appendHistory(s, step, approver, "通过（代班 " + s.getPhysicianName()
                    + " → " + s.getTargetPhysicianName() + "）", opinion);
            s.setPhysicianId(s.getTargetPhysicianId());
            s.setPhysicianName(s.getTargetPhysicianName());
            if (target != null) {
                s.setDeptId(target.getDeptId());
                s.setDeptName(target.getDeptName());
            }
        } else {
            throw new IllegalStateException("未知的调班/代班类型：" + s.getChangeType());
        }
        s.setChangeStatus(ScheduleShiftUtil.CHANGE_APPROVED);
        s.setApprover(approver);
        s.setApprovalOpinion(opinion);
        this.updateById(s);
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Schedule rejectChange(Long id, String approver, String opinion) {
        Schedule s = getOrThrow(id);
        if (!Integer.valueOf(ScheduleShiftUtil.CHANGE_PENDING).equals(s.getChangeStatus())) {
            throw new IllegalStateException("该排班没有待审批的调班/代班申请，不能驳回");
        }
        appendHistory(s, "科主任", approver, "驳回", opinion);
        s.setChangeStatus(ScheduleShiftUtil.CHANGE_REJECTED);
        s.setApprover(approver);
        s.setApprovalOpinion(opinion);
        this.updateById(s);
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncToHis(Long id) {
        getOrThrow(id);
        // 没有真实 HIS 系统可连：只做预留，绝不伪造同步成功
        if (!hisProperties.isEnabled()) {
            throw new UnsupportedOperationException("HIS 对接未启用：排班同步为预留占位，未向任何 HIS 系统发送数据");
        }
        if (!StringUtils.hasText(hisProperties.getBaseUrl())) {
            throw new IllegalStateException("HIS 接口地址未配置，无法同步排班");
        }
        throw new UnsupportedOperationException("HIS 接口调用尚未按医院 HIS 规范实现，同步未执行");
    }

    @Override
    public void exportExcel(ScheduleQuery query, OutputStream out) {
        ScheduleExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<Schedule> list = ScheduleExcelUtil.read(in);
        int ok = 0;
        for (Schedule s : list) {
            if (!StringUtils.hasText(s.getPhysicianName())) {
                log.warn("导入跳过：医师姓名为空的行");
                continue;
            }
            if (s.getScheduleDate() == null) {
                log.warn("导入跳过：排班日期为空的行（医师 {}）", s.getPhysicianName());
                continue;
            }
            if (!StringUtils.hasText(s.getScheduleType())) {
                log.warn("导入跳过：排班类型为空的行（医师 {}）", s.getPhysicianName());
                continue;
            }
            if (!StringUtils.hasText(s.getShiftCode())) {
                log.warn("导入跳过：班次为空的行（医师 {}）", s.getPhysicianName());
                continue;
            }
            // Excel 里没有医师 id，按姓名查医师档案补全（医师、科室）；查不到的跳过
            if (s.getPhysicianId() == null) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, s.getPhysicianName()).last("LIMIT 1"));
                if (physician == null) {
                    log.warn("导入跳过：医师 {} 在医师档案中不存在", s.getPhysicianName());
                    continue;
                }
                s.setPhysicianId(physician.getId());
                if (s.getDeptId() == null) {
                    s.setDeptId(physician.getDeptId());
                }
                if (!StringUtils.hasText(s.getDeptName())) {
                    s.setDeptName(physician.getDeptName());
                }
            }
            // 同一医师同一天已有排班视为重复，跳过
            if (hasConflict(s.getPhysicianId(), s.getScheduleDate(), null)) {
                log.warn("导入跳过：重复排班 {} {}", s.getPhysicianName(), s.getScheduleDate());
                continue;
            }
            fillDefaultTimes(s);
            this.save(s);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<Schedule> buildWrapper(ScheduleQuery q) {
        LambdaQueryWrapper<Schedule> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.like(Schedule::getPhysicianName, q.getKeyword());
        }
        qw.eq(q.getPhysicianId() != null, Schedule::getPhysicianId, q.getPhysicianId());
        qw.eq(q.getDeptId() != null, Schedule::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getScheduleType()), Schedule::getScheduleType, q.getScheduleType());
        qw.eq(StringUtils.hasText(q.getShiftCode()), Schedule::getShiftCode, q.getShiftCode());
        qw.eq(q.getChangeStatus() != null, Schedule::getChangeStatus, q.getChangeStatus());
        qw.eq(q.getTemplateId() != null, Schedule::getTemplateId, q.getTemplateId());
        qw.ge(q.getStartDate() != null, Schedule::getScheduleDate, q.getStartDate());
        qw.le(q.getEndDate() != null, Schedule::getScheduleDate, q.getEndDate());
        qw.orderByAsc(Schedule::getScheduleDate).orderByDesc(Schedule::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时同样校验（同一医师同一天不能重复排班） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Schedule entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Schedule entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(Schedule s, boolean isCreate) {
        if (!StringUtils.hasText(s.getScheduleType())
                || !ScheduleShiftUtil.isValidType(s.getScheduleType())) {
            throw new IllegalArgumentException("排班类型不正确");
        }
        if (!StringUtils.hasText(s.getShiftCode())
                || !ScheduleShiftUtil.isValidShift(s.getShiftCode())) {
            throw new IllegalArgumentException("班次不正确");
        }
        fillDefaultTimes(s);
        if (hasConflict(s.getPhysicianId(), s.getScheduleDate(), isCreate ? null : s.getId())) {
            throw new IllegalArgumentException("该医师在 " + s.getScheduleDate() + " 已有排班，不能重复排班");
        }
        if (isCreate) {
            if (s.getChangeStatus() == null) {
                s.setChangeStatus(ScheduleShiftUtil.CHANGE_NONE);
            }
            if (s.getHisSyncStatus() == null) {
                s.setHisSyncStatus(0);
            }
        }
    }

    /** 班次时间段：休息清空时间；其他班次缺失时按班次默认时间补全 */
    private void fillDefaultTimes(Schedule s) {
        if (ScheduleShiftUtil.SHIFT_REST.equals(s.getShiftCode())) {
            s.setStartTime(null);
            s.setEndTime(null);
            return;
        }
        if (s.getStartTime() == null) {
            s.setStartTime(ScheduleShiftUtil.defaultStartTime(s.getShiftCode()));
        }
        if (s.getEndTime() == null) {
            s.setEndTime(ScheduleShiftUtil.defaultEndTime(s.getShiftCode()));
        }
    }

    /** 同一医师同一天是否已有排班（excludeId 排除自身，用于更新/审批时校验） */
    private boolean hasConflict(Long physicianId, LocalDate date, Long excludeId) {
        if (physicianId == null || date == null) {
            return false;
        }
        LambdaQueryWrapper<Schedule> qw = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getPhysicianId, physicianId)
                .eq(Schedule::getScheduleDate, date);
        if (excludeId != null) {
            qw.ne(Schedule::getId, excludeId);
        }
        return this.count(qw) > 0;
    }

    private Schedule getOrThrow(Long id) {
        Schedule s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("排班记录不存在：" + id);
        }
        return s;
    }

    private void requireNoPendingChange(Schedule s) {
        if (Integer.valueOf(ScheduleShiftUtil.CHANGE_PENDING).equals(s.getChangeStatus())) {
            throw new IllegalStateException("该排班已有待审批的调班/代班申请，请先处理");
        }
    }

    private void appendHistory(Schedule s, String step, String approver, String action, String opinion) {
        String who = StringUtils.hasText(approver) && !approver.equals(step) ? " " + approver : "";
        String line = step + who + "：" + action
                + (StringUtils.hasText(opinion) ? "（" + opinion + "）" : "");
        s.setApprovalHistory(StringUtils.hasText(s.getApprovalHistory())
                ? s.getApprovalHistory() + "；" + line : line);
    }
}
