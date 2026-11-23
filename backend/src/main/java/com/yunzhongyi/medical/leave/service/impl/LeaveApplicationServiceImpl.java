package com.yunzhongyi.medical.leave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.leave.dto.LeaveQuery;
import com.yunzhongyi.medical.leave.entity.LeaveApplication;
import com.yunzhongyi.medical.leave.mapper.LeaveApplicationMapper;
import com.yunzhongyi.medical.leave.service.LeaveApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 休假执业服务实现：在线申请 + 多级审批（科主任 → 医务科）+ 统计
 */
@Service
public class LeaveApplicationServiceImpl
        extends ServiceImpl<LeaveApplicationMapper, LeaveApplication>
        implements LeaveApplicationService {

    @Override
    public PageResult<LeaveApplication> pageQuery(LeaveQuery query) {
        Page<LeaveApplication> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<LeaveApplication> listByQuery(LeaveQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", this.count());
        r.put("pendingDept", this.count(new LambdaQueryWrapper<LeaveApplication>().eq(LeaveApplication::getStatus, 0)));
        r.put("pendingMedical", this.count(new LambdaQueryWrapper<LeaveApplication>().eq(LeaveApplication::getStatus, 1)));
        r.put("pending", this.count(new LambdaQueryWrapper<LeaveApplication>().in(LeaveApplication::getStatus, List.of(0, 1))));
        r.put("approved", this.count(new LambdaQueryWrapper<LeaveApplication>().eq(LeaveApplication::getStatus, 2)));
        r.put("rejected", this.count(new LambdaQueryWrapper<LeaveApplication>().eq(LeaveApplication::getStatus, 3)));
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of("LEAVE", "PRACTICE", "TRAINING", "OUT_TRAINING", "SURGERY_AUTH")) {
            byType.put(type, this.count(new LambdaQueryWrapper<LeaveApplication>().eq(LeaveApplication::getApplyType, type)));
        }
        r.put("byType", byType);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveApplication approve(Long id, String approver, String opinion) {
        LeaveApplication a = getOrThrow(id);
        if (a.getStatus() == null || a.getStatus() == 2 || a.getStatus() == 3) {
            throw new IllegalStateException("该申请已结束审批，不能再通过");
        }
        String step = a.getStatus() == 0 ? "科主任" : "医务科";
        appendHistory(a, step, approver, "通过", opinion);
        a.setStatus(a.getStatus() == 0 ? 1 : 2);
        a.setCurrentApprover(a.getStatus() == 1 ? "医务科" : null);
        a.setApprovalOpinion(opinion);
        this.updateById(a);
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveApplication reject(Long id, String approver, String opinion) {
        LeaveApplication a = getOrThrow(id);
        if (a.getStatus() == null || a.getStatus() == 2 || a.getStatus() == 3) {
            throw new IllegalStateException("该申请已结束审批，不能再驳回");
        }
        String step = a.getStatus() == 0 ? "科主任" : "医务科";
        appendHistory(a, step, approver, "驳回", opinion);
        a.setStatus(3);
        a.setCurrentApprover(null);
        a.setApprovalOpinion(opinion);
        this.updateById(a);
        return a;
    }

    private LeaveApplication getOrThrow(Long id) {
        LeaveApplication a = this.getById(id);
        if (a == null) {
            throw new IllegalArgumentException("申请不存在：" + id);
        }
        return a;
    }

    private void appendHistory(LeaveApplication a, String step, String approver, String action, String opinion) {
        // 前端审批人默认就填“科主任 / 医务科”，与环节名相同时不重复写
        String who = StringUtils.hasText(approver) && !approver.equals(step) ? " " + approver : "";
        String line = step + who + "：" + action
                + (StringUtils.hasText(opinion) ? "（" + opinion + "）" : "");
        a.setApprovalHistory(StringUtils.hasText(a.getApprovalHistory())
                ? a.getApprovalHistory() + "；" + line : line);
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<LeaveApplication> buildWrapper(LeaveQuery q) {
        LambdaQueryWrapper<LeaveApplication> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.like(LeaveApplication::getPhysicianName, q.getKeyword());
        }
        qw.eq(q.getDeptId() != null, LeaveApplication::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getApplyType()), LeaveApplication::getApplyType, q.getApplyType());
        qw.eq(q.getStatus() != null, LeaveApplication::getStatus, q.getStatus());
        qw.orderByDesc(LeaveApplication::getId);
        return qw;
    }

    /** 新建时补默认值并校验日期、算天数；更新时同样校验 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(LeaveApplication entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(LeaveApplication entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(LeaveApplication a, boolean isCreate) {
        if (a.getStartDate() != null && a.getEndDate() != null && a.getEndDate().isBefore(a.getStartDate())) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        if (a.getStartDate() != null && a.getEndDate() != null) {
            a.setDays((int) ChronoUnit.DAYS.between(a.getStartDate(), a.getEndDate()) + 1);
        }
        if (isCreate) {
            if (a.getStatus() == null) {
                a.setStatus(0);
            }
            if (a.getApplyDate() == null) {
                a.setApplyDate(LocalDate.now());
            }
            if (!StringUtils.hasText(a.getCurrentApprover())) {
                a.setCurrentApprover("科主任");
            }
        }
    }
}
