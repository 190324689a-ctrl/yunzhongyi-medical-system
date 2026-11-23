package com.yunzhongyi.medical.personnel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.personnel.dto.PersonnelChangeQuery;
import com.yunzhongyi.medical.personnel.entity.PersonnelChange;
import com.yunzhongyi.medical.personnel.excel.PersonnelChangeExcelUtil;
import com.yunzhongyi.medical.personnel.mapper.PersonnelChangeMapper;
import com.yunzhongyi.medical.personnel.service.PersonnelChangeService;
import com.yunzhongyi.medical.personnel.service.PersonnelChangeTypeUtil;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.mapper.PhysicianMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医疗人员动态管理服务实现：人员变动登记 CRUD + 一级审批（医务科）+
 * 审批通过同步医师档案 + 统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonnelChangeServiceImpl
        extends ServiceImpl<PersonnelChangeMapper, PersonnelChange>
        implements PersonnelChangeService {

    private final PhysicianMapper physicianMapper;

    @Override
    public PageResult<PersonnelChange> pageQuery(PersonnelChangeQuery query) {
        Page<PersonnelChange> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<PersonnelChange> listByQuery(PersonnelChangeQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<PersonnelChange> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(c -> Integer.valueOf(PersonnelChangeTypeUtil.STATUS_PENDING).equals(c.getStatus())).count());
        r.put("approved", all.stream()
                .filter(c -> Integer.valueOf(PersonnelChangeTypeUtil.STATUS_APPROVED).equals(c.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(c -> Integer.valueOf(PersonnelChangeTypeUtil.STATUS_REJECTED).equals(c.getStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(PersonnelChangeTypeUtil.TYPE_TRANSFER, PersonnelChangeTypeUtil.TYPE_RESIGN,
                PersonnelChangeTypeUtil.TYPE_RETIRE, PersonnelChangeTypeUtil.TYPE_JOIN,
                PersonnelChangeTypeUtil.TYPE_REHIRE)) {
            byType.put(t, all.stream().filter(c -> t.equals(c.getChangeType())).count());
        }
        r.put("byType", byType);
        r.put("byDept", statsByDept());
        r.put("byMonth", statsByMonth());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<PersonnelChange>> grouped = new LinkedHashMap<>();
        for (PersonnelChange c : this.list()) {
            String dept = relatedDeptName(c);
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("pendingCount", list.stream()
                    .filter(c -> Integer.valueOf(PersonnelChangeTypeUtil.STATUS_PENDING).equals(c.getStatus())).count());
            row.put("approvedCount", list.stream()
                    .filter(c -> Integer.valueOf(PersonnelChangeTypeUtil.STATUS_APPROVED).equals(c.getStatus())).count());
            row.put("rejectedCount", list.stream()
                    .filter(c -> Integer.valueOf(PersonnelChangeTypeUtil.STATUS_REJECTED).equals(c.getStatus())).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        Map<YearMonth, List<PersonnelChange>> grouped = new LinkedHashMap<>();
        for (PersonnelChange c : this.list()) {
            if (c.getChangeDate() == null) {
                continue;
            }
            grouped.computeIfAbsent(YearMonth.from(c.getChangeDate()), k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.keySet().stream().sorted().forEach(month -> {
            List<PersonnelChange> list = grouped.get(month);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            row.put("count", (long) list.size());
            row.put("approvedCount", list.stream()
                    .filter(c -> Integer.valueOf(PersonnelChangeTypeUtil.STATUS_APPROVED).equals(c.getStatus())).count());
            row.put("pendingCount", list.stream()
                    .filter(c -> Integer.valueOf(PersonnelChangeTypeUtil.STATUS_PENDING).equals(c.getStatus())).count());
            result.add(row);
        });
        return result;
    }

    @Override
    public PersonnelChange detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersonnelChange approve(Long id, String approver, String opinion) {
        PersonnelChange c = getOrThrow(id);
        if (!Integer.valueOf(PersonnelChangeTypeUtil.STATUS_PENDING).equals(c.getStatus())) {
            throw new IllegalStateException("该变动已结束（" + PersonnelChangeTypeUtil.statusText(c.getStatus())
                    + "），不能再审批");
        }
        appendHistory(c, "医务科", approver, "通过", opinion);
        c.setStatus(PersonnelChangeTypeUtil.STATUS_APPROVED);
        c.setApprover(approver);
        c.setApprovalOpinion(opinion);
        // 直接调 super.updateById：审批已校验待审批状态；走本类 updateById 的
        // normalize 会在同一事务内因 MyBatis 一级缓存拿到已被改过的同一对象而误判
        super.updateById(c);
        // 审批通过，同步医师档案：调动改科室，离职/退休改在职状态，入职/返聘改回在职
        syncPhysician(c);
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersonnelChange reject(Long id, String approver, String opinion) {
        PersonnelChange c = getOrThrow(id);
        if (!Integer.valueOf(PersonnelChangeTypeUtil.STATUS_PENDING).equals(c.getStatus())) {
            throw new IllegalStateException("该变动已结束（" + PersonnelChangeTypeUtil.statusText(c.getStatus())
                    + "），不能再审批");
        }
        appendHistory(c, "医务科", approver, "驳回", opinion);
        c.setStatus(PersonnelChangeTypeUtil.STATUS_REJECTED);
        c.setApprover(approver);
        c.setApprovalOpinion(opinion);
        // 同 approve：直接调 super.updateById，跳过 normalize 的修改守卫
        super.updateById(c);
        return c;
    }

    @Override
    public void exportExcel(PersonnelChangeQuery query, OutputStream out) {
        PersonnelChangeExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<PersonnelChange> list = PersonnelChangeExcelUtil.read(in);
        int ok = 0;
        for (PersonnelChange c : list) {
            if (!StringUtils.hasText(c.getPhysicianName())) {
                log.warn("导入跳过：人员姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(c.getChangeType()) || !PersonnelChangeTypeUtil.isValidType(c.getChangeType())) {
                log.warn("导入跳过：变动类型为空或不正确（人员 {}）", c.getPhysicianName());
                continue;
            }
            if (c.getChangeDate() == null) {
                log.warn("导入跳过：生效日期为空（人员 {}）", c.getPhysicianName());
                continue;
            }
            if (c.getStatus() == null) {
                c.setStatus(PersonnelChangeTypeUtil.STATUS_PENDING);
            }
            if (c.getApplyDate() == null) {
                c.setApplyDate(LocalDate.now());
            }
            // 按姓名查医师档案补全人员 id（同名取第一条），便于审批通过时同步档案
            if (c.getPhysicianId() == null) {
                List<Physician> found = physicianMapper.selectList(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, c.getPhysicianName())
                        .orderByAsc(Physician::getId));
                if (!found.isEmpty()) {
                    c.setPhysicianId(found.get(0).getId());
                }
            }
            // 同一人员同一变动类型同一生效日期视为重复，跳过
            LambdaQueryWrapper<PersonnelChange> qw = new LambdaQueryWrapper<PersonnelChange>()
                    .eq(PersonnelChange::getPhysicianName, c.getPhysicianName())
                    .eq(PersonnelChange::getChangeType, c.getChangeType())
                    .eq(PersonnelChange::getChangeDate, c.getChangeDate());
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复人员变动 {} {} {}", c.getPhysicianName(), c.getChangeType(), c.getChangeDate());
                continue;
            }
            this.save(c);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<PersonnelChange> buildWrapper(PersonnelChangeQuery q) {
        LambdaQueryWrapper<PersonnelChange> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(q.getKeyword()), PersonnelChange::getPhysicianName, q.getKeyword());
        // 相关科室：原科室或调入/新科室任一匹配
        if (q.getDeptId() != null) {
            qw.and(w -> w.eq(PersonnelChange::getFromDeptId, q.getDeptId())
                    .or().eq(PersonnelChange::getToDeptId, q.getDeptId()));
        }
        qw.eq(StringUtils.hasText(q.getChangeType()), PersonnelChange::getChangeType, q.getChangeType());
        qw.eq(q.getStatus() != null, PersonnelChange::getStatus, q.getStatus());
        qw.eq(q.getPhysicianId() != null, PersonnelChange::getPhysicianId, q.getPhysicianId());
        qw.ge(q.getStartDate() != null, PersonnelChange::getChangeDate, q.getStartDate());
        qw.le(q.getEndDate() != null, PersonnelChange::getChangeDate, q.getEndDate());
        qw.orderByDesc(PersonnelChange::getChangeDate).orderByDesc(PersonnelChange::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时只允许改待审批的变动 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PersonnelChange entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PersonnelChange entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(PersonnelChange c, boolean isCreate) {
        if (!StringUtils.hasText(c.getChangeType()) || !PersonnelChangeTypeUtil.isValidType(c.getChangeType())) {
            throw new IllegalArgumentException("变动类型不正确");
        }
        if (c.getChangeDate() == null) {
            throw new IllegalArgumentException("生效日期不能为空");
        }
        // 按变动类型校验科室：调动须有调入科室且与原科室不同；离职/退休须有原科室；
        // 入职/返聘须有调入（新）科室
        switch (c.getChangeType()) {
            case PersonnelChangeTypeUtil.TYPE_TRANSFER -> {
                if (!StringUtils.hasText(c.getToDeptName())) {
                    throw new IllegalArgumentException("调动须填写调入科室");
                }
                if (c.getFromDeptId() != null && c.getToDeptId() != null
                        && c.getFromDeptId().equals(c.getToDeptId())) {
                    throw new IllegalArgumentException("调动前后科室不能相同");
                }
                if (StringUtils.hasText(c.getFromDeptName()) && StringUtils.hasText(c.getToDeptName())
                        && c.getFromDeptName().equals(c.getToDeptName())) {
                    throw new IllegalArgumentException("调动前后科室不能相同");
                }
            }
            case PersonnelChangeTypeUtil.TYPE_RESIGN, PersonnelChangeTypeUtil.TYPE_RETIRE -> {
                if (!StringUtils.hasText(c.getFromDeptName())) {
                    throw new IllegalArgumentException("离职/退休须填写原科室");
                }
            }
            case PersonnelChangeTypeUtil.TYPE_JOIN, PersonnelChangeTypeUtil.TYPE_REHIRE -> {
                if (!StringUtils.hasText(c.getToDeptName())) {
                    throw new IllegalArgumentException("入职/返聘须填写调入（新）科室");
                }
            }
            default -> throw new IllegalArgumentException("变动类型不正确");
        }
        if (isCreate) {
            if (c.getStatus() == null) {
                c.setStatus(PersonnelChangeTypeUtil.STATUS_PENDING);
            }
            if (c.getApplyDate() == null) {
                c.setApplyDate(LocalDate.now());
            }
            // 同一人员同一变动类型同一生效日期的待审批记录已存在，拒绝重复登记
            LambdaQueryWrapper<PersonnelChange> qw = new LambdaQueryWrapper<PersonnelChange>()
                    .eq(PersonnelChange::getPhysicianName, c.getPhysicianName())
                    .eq(PersonnelChange::getChangeType, c.getChangeType())
                    .eq(PersonnelChange::getChangeDate, c.getChangeDate())
                    .eq(PersonnelChange::getStatus, PersonnelChangeTypeUtil.STATUS_PENDING);
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("该人员同一变动类型同一生效日期已有待审批记录，请勿重复登记");
            }
        } else {
            // 更新只允许改待审批的变动（审批流转不走这里）。
            // 注意：这里用 selectOne 而不用 getById——同一事务内 getById 可能因
            // MyBatis 一级缓存返回调用方已改过的同一对象，导致状态判断失真。
            PersonnelChange existing = this.baseMapper.selectOne(new LambdaQueryWrapper<PersonnelChange>()
                    .eq(PersonnelChange::getId, c.getId()));
            if (existing == null) {
                throw new IllegalArgumentException("人员变动不存在：" + c.getId());
            }
            if (!Integer.valueOf(PersonnelChangeTypeUtil.STATUS_PENDING).equals(existing.getStatus())) {
                throw new IllegalStateException("已结束审批的变动（"
                        + PersonnelChangeTypeUtil.statusText(existing.getStatus()) + "）不能再修改");
            }
        }
    }

    /**
     * 审批通过时同步医师档案（与审批同一事务）：
     * 调动改科室，离职改在职状态为离职，退休改为退休，入职/返聘改回在职并落到新科室。
     * 入职人员尚未建档（physicianId 为空）或档案查不到时只记日志、不阻断审批，
     * 入职建档需在医师档案里补建。
     */
    private void syncPhysician(PersonnelChange c) {
        if (c.getPhysicianId() == null) {
            log.info("人员变动 {}（{}）审批通过：人员尚未建档，未同步医师档案", c.getPhysicianName(), c.getChangeType());
            return;
        }
        Physician p = physicianMapper.selectById(c.getPhysicianId());
        if (p == null) {
            log.warn("人员变动 {}（{}）审批通过：医师档案不存在（id={}），未同步", c.getPhysicianName(),
                    c.getChangeType(), c.getPhysicianId());
            return;
        }
        Physician update = new Physician();
        update.setId(p.getId());
        switch (c.getChangeType()) {
            case PersonnelChangeTypeUtil.TYPE_TRANSFER -> {
                update.setDeptId(c.getToDeptId());
                update.setDeptName(c.getToDeptName());
            }
            case PersonnelChangeTypeUtil.TYPE_RESIGN ->
                    update.setStatus(PersonnelChangeTypeUtil.PHYSICIAN_RESIGNED);
            case PersonnelChangeTypeUtil.TYPE_RETIRE ->
                    update.setStatus(PersonnelChangeTypeUtil.PHYSICIAN_RETIRED);
            case PersonnelChangeTypeUtil.TYPE_JOIN, PersonnelChangeTypeUtil.TYPE_REHIRE -> {
                update.setStatus(PersonnelChangeTypeUtil.PHYSICIAN_ACTIVE);
                update.setDeptId(c.getToDeptId());
                update.setDeptName(c.getToDeptName());
            }
            default -> {
                return;
            }
        }
        physicianMapper.updateById(update);
        log.info("人员变动 {}（{}）审批通过：已同步医师档案 id={}", c.getPhysicianName(), c.getChangeType(), p.getId());
    }

    /** 相关科室：原科室优先，入职等无原科室时用调入/新科室 */
    private String relatedDeptName(PersonnelChange c) {
        if (StringUtils.hasText(c.getFromDeptName())) {
            return c.getFromDeptName();
        }
        if (StringUtils.hasText(c.getToDeptName())) {
            return c.getToDeptName();
        }
        return "未分科室";
    }

    private PersonnelChange getOrThrow(Long id) {
        PersonnelChange c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("人员变动不存在：" + id);
        }
        return c;
    }

    private void appendHistory(PersonnelChange c, String step, String approver, String action, String opinion) {
        String who = StringUtils.hasText(approver) && !approver.equals(step) ? " " + approver : "";
        String line = step + who + "：" + action
                + (StringUtils.hasText(opinion) ? "（" + opinion + "）" : "");
        c.setApprovalHistory(StringUtils.hasText(c.getApprovalHistory())
                ? c.getApprovalHistory() + "；" + line : line);
    }
}
