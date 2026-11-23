package com.yunzhongyi.medical.mobile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.authorization.dto.AuthorizationQuery;
import com.yunzhongyi.medical.authorization.entity.Authorization;
import com.yunzhongyi.medical.authorization.service.AuthorizationService;
import com.yunzhongyi.medical.briefing.dto.BriefingQuery;
import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.briefing.service.BriefingService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.leave.dto.LeaveQuery;
import com.yunzhongyi.medical.leave.entity.LeaveApplication;
import com.yunzhongyi.medical.leave.service.LeaveApplicationService;
import com.yunzhongyi.medical.mdt.dto.MdtQuery;
import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import com.yunzhongyi.medical.mdt.service.MdtService;
import com.yunzhongyi.medical.medgrading.dto.GradingAssessmentQuery;
import com.yunzhongyi.medical.medgrading.entity.GradingAssessment;
import com.yunzhongyi.medical.medgrading.service.GradingAssessmentService;
import com.yunzhongyi.medical.mobile.dto.MobileApprovalLogQuery;
import com.yunzhongyi.medical.mobile.dto.MobileApproveRequest;
import com.yunzhongyi.medical.mobile.dto.MobilePendingItem;
import com.yunzhongyi.medical.mobile.entity.MobileApprovalLog;
import com.yunzhongyi.medical.mobile.excel.MobileApprovalLogExcelUtil;
import com.yunzhongyi.medical.mobile.mapper.MobileApprovalLogMapper;
import com.yunzhongyi.medical.mobile.service.MobileApprovalService;
import com.yunzhongyi.medical.mobile.service.MobileTypeUtil;
import com.yunzhongyi.medical.newtech.dto.NewTechQuery;
import com.yunzhongyi.medical.newtech.entity.NewTechProject;
import com.yunzhongyi.medical.newtech.service.NewTechService;
import com.yunzhongyi.medical.personnel.dto.PersonnelChangeQuery;
import com.yunzhongyi.medical.personnel.entity.PersonnelChange;
import com.yunzhongyi.medical.personnel.service.PersonnelChangeService;
import com.yunzhongyi.medical.schedule.dto.ScheduleQuery;
import com.yunzhongyi.medical.schedule.entity.Schedule;
import com.yunzhongyi.medical.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动端审批服务实现。
 * <p>
 * 待审批聚合：从 8 个业务模块的服务层按待审批状态取数，拼成统一待办列表；
 * 审批流转：直接委托各业务模块已有的审批方法（两级审批、多级守卫都在
 * 业务模块内），本服务只做参数校验、委托调用与操作留痕。
 * 委托失败时记一条 FAIL 日志（写明失败原因），再把业务模块的错误原样抛出，
 * 绝不把失败记成成功。
 */
@Service
@RequiredArgsConstructor
public class MobileApprovalServiceImpl
        extends ServiceImpl<MobileApprovalLogMapper, MobileApprovalLog>
        implements MobileApprovalService {

    private final LeaveApplicationService leaveApplicationService;
    private final AuthorizationService authorizationService;
    private final ScheduleService scheduleService;
    private final MdtService mdtService;
    private final NewTechService newTechService;
    private final PersonnelChangeService personnelChangeService;
    private final GradingAssessmentService gradingAssessmentService;
    private final BriefingService briefingService;

    @Override
    public List<MobilePendingItem> pendingList(String moduleType, String keyword) {
        if (StringUtils.hasText(moduleType) && !MobileTypeUtil.MODULE_TYPES.contains(moduleType)) {
            throw new IllegalArgumentException("业务模块不合法：" + moduleType);
        }
        List<MobilePendingItem> items = new ArrayList<>();
        if (!StringUtils.hasText(moduleType) || MobileTypeUtil.MODULE_LEAVE.equals(moduleType)) {
            items.addAll(pendingLeaves());
        }
        if (!StringUtils.hasText(moduleType) || MobileTypeUtil.MODULE_AUTH.equals(moduleType)) {
            items.addAll(pendingAuthorizations());
        }
        if (!StringUtils.hasText(moduleType) || MobileTypeUtil.MODULE_SCHEDULE.equals(moduleType)) {
            items.addAll(pendingSchedules());
        }
        if (!StringUtils.hasText(moduleType) || MobileTypeUtil.MODULE_MDT.equals(moduleType)) {
            items.addAll(pendingMdts());
        }
        if (!StringUtils.hasText(moduleType) || MobileTypeUtil.MODULE_NEWTECH.equals(moduleType)) {
            items.addAll(pendingNewTechs());
        }
        if (!StringUtils.hasText(moduleType) || MobileTypeUtil.MODULE_PERSONNEL.equals(moduleType)) {
            items.addAll(pendingPersonnelChanges());
        }
        if (!StringUtils.hasText(moduleType) || MobileTypeUtil.MODULE_MEDGRADING.equals(moduleType)) {
            items.addAll(pendingGradingAssessments());
        }
        if (!StringUtils.hasText(moduleType) || MobileTypeUtil.MODULE_BRIEFING.equals(moduleType)) {
            items.addAll(pendingBriefings());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            items.removeIf(i -> !(contains(i.getTitle(), kw)
                    || contains(i.getApplicant(), kw)
                    || contains(i.getDeptName(), kw)));
        }
        items.sort(Comparator.comparing(MobilePendingItem::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MobilePendingItem::getModuleType)
                .thenComparing(MobilePendingItem::getRelatedId, Comparator.reverseOrder()));
        return items;
    }

    @Override
    public Map<String, Object> pendingCount() {
        List<MobilePendingItem> items = pendingList(null, null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) items.size());
        Map<String, Long> byModule = new LinkedHashMap<>();
        for (String module : MobileTypeUtil.MODULE_TYPES) {
            byModule.put(module, items.stream()
                    .filter(i -> module.equals(i.getModuleType())).count());
        }
        r.put("byModule", byModule);
        return r;
    }

    @Override
    public MobileApprovalLog approve(String moduleType, Long relatedId, MobileApproveRequest request) {
        return doApproval(moduleType, relatedId, request, MobileTypeUtil.ACTION_APPROVE);
    }

    @Override
    public MobileApprovalLog reject(String moduleType, Long relatedId, MobileApproveRequest request) {
        return doApproval(moduleType, relatedId, request, MobileTypeUtil.ACTION_REJECT);
    }

    @Override
    public PageResult<MobileApprovalLog> pageQuery(MobileApprovalLogQuery query) {
        Page<MobileApprovalLog> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public Map<String, Object> stats() {
        List<MobileApprovalLog> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("approved", all.stream()
                .filter(l -> MobileTypeUtil.ACTION_APPROVE.equals(l.getAction())).count());
        r.put("rejected", all.stream()
                .filter(l -> MobileTypeUtil.ACTION_REJECT.equals(l.getAction())).count());
        r.put("success", all.stream()
                .filter(l -> MobileTypeUtil.RESULT_SUCCESS.equals(l.getResult())).count());
        r.put("failed", all.stream()
                .filter(l -> MobileTypeUtil.RESULT_FAIL.equals(l.getResult())).count());
        Map<String, Long> byModule = new LinkedHashMap<>();
        for (String module : MobileTypeUtil.MODULE_TYPES) {
            byModule.put(module, all.stream().filter(l -> module.equals(l.getModuleType())).count());
        }
        r.put("byModule", byModule);
        return r;
    }

    @Override
    public void exportExcel(MobileApprovalLogQuery query, OutputStream out) {
        MobileApprovalLogExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    // ---------- 审批流转 ----------

    private MobileApprovalLog doApproval(String moduleType, Long relatedId,
                                         MobileApproveRequest request, String action) {
        if (!StringUtils.hasText(moduleType) || !MobileTypeUtil.MODULE_TYPES.contains(moduleType)) {
            throw new IllegalArgumentException("业务模块不合法：" + moduleType);
        }
        if (relatedId == null) {
            throw new IllegalArgumentException("待审批事项 id 不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getOperator())) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        if (MobileTypeUtil.ACTION_REJECT.equals(action) && !StringUtils.hasText(request.getOpinion())) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        String title = resolveTitle(moduleType, relatedId);
        MobileApprovalLog log = new MobileApprovalLog();
        log.setModuleType(moduleType);
        log.setRelatedId(relatedId);
        log.setRelatedTitle(title);
        log.setAction(action);
        log.setOperator(request.getOperator().trim());
        log.setDeviceCode(request.getDeviceCode());
        log.setOpinion(request.getOpinion());
        log.setActionTime(LocalDateTime.now());
        try {
            delegate(moduleType, relatedId, request, action);
            log.setResult(MobileTypeUtil.RESULT_SUCCESS);
            log.setFailReason(null);
        } catch (RuntimeException e) {
            log.setResult(MobileTypeUtil.RESULT_FAIL);
            log.setFailReason(e.getMessage());
            this.save(log);
            throw e;
        }
        this.save(log);
        return log;
    }

    /** 委托各业务模块已有的审批方法；守卫（已结束不可再审批等）都在业务模块内。 */
    private void delegate(String moduleType, Long relatedId, MobileApproveRequest request, String action) {
        String operator = request.getOperator().trim();
        String opinion = request.getOpinion();
        boolean approve = MobileTypeUtil.ACTION_APPROVE.equals(action);
        switch (moduleType) {
            case MobileTypeUtil.MODULE_LEAVE -> {
                if (approve) {
                    leaveApplicationService.approve(relatedId, operator, opinion);
                } else {
                    leaveApplicationService.reject(relatedId, operator, opinion);
                }
            }
            case MobileTypeUtil.MODULE_AUTH -> {
                if (approve) {
                    authorizationService.approve(relatedId, operator, opinion);
                } else {
                    authorizationService.reject(relatedId, operator, opinion);
                }
            }
            case MobileTypeUtil.MODULE_SCHEDULE -> {
                if (approve) {
                    scheduleService.approveChange(relatedId, operator, opinion);
                } else {
                    scheduleService.rejectChange(relatedId, operator, opinion);
                }
            }
            case MobileTypeUtil.MODULE_MDT -> {
                if (approve) {
                    mdtService.approve(relatedId, operator, opinion);
                } else {
                    mdtService.reject(relatedId, operator, opinion);
                }
            }
            case MobileTypeUtil.MODULE_NEWTECH -> {
                if (approve) {
                    newTechService.approve(relatedId, operator, opinion);
                } else {
                    newTechService.reject(relatedId, operator, opinion);
                }
            }
            case MobileTypeUtil.MODULE_PERSONNEL -> {
                if (approve) {
                    personnelChangeService.approve(relatedId, operator, opinion);
                } else {
                    personnelChangeService.reject(relatedId, operator, opinion);
                }
            }
            case MobileTypeUtil.MODULE_MEDGRADING -> {
                if (approve) {
                    gradingAssessmentService.approve(relatedId, operator, opinion);
                } else {
                    gradingAssessmentService.reject(relatedId, operator, opinion);
                }
            }
            case MobileTypeUtil.MODULE_BRIEFING -> {
                if (approve) {
                    briefingService.approve(relatedId, operator, opinion);
                } else {
                    briefingService.reject(relatedId, operator, opinion);
                }
            }
            default -> throw new IllegalArgumentException("业务模块不合法：" + moduleType);
        }
    }

    /** 取业务实体的标题快照；实体不存在时拒绝审批。 */
    private String resolveTitle(String moduleType, Long relatedId) {
        return switch (moduleType) {
            case MobileTypeUtil.MODULE_LEAVE -> {
                LeaveApplication a = leaveApplicationService.getById(relatedId);
                if (a == null) {
                    throw new IllegalArgumentException("休假执业申请不存在：" + relatedId);
                }
                yield a.getPhysicianName() + MobileTypeUtil.leaveApplyTypeText(a.getApplyType()) + "申请";
            }
            case MobileTypeUtil.MODULE_AUTH -> {
                Authorization a = authorizationService.getById(relatedId);
                if (a == null) {
                    throw new IllegalArgumentException("重点授权申请不存在：" + relatedId);
                }
                yield a.getPhysicianName() + MobileTypeUtil.authTypeText(a.getAuthType()) + "申请";
            }
            case MobileTypeUtil.MODULE_SCHEDULE -> {
                Schedule s = scheduleService.getById(relatedId);
                if (s == null) {
                    throw new IllegalArgumentException("排班调班申请不存在：" + relatedId);
                }
                yield s.getPhysicianName()
                        + ("SUBSTITUTE".equals(s.getChangeType()) ? "代班" : "调班")
                        + "申请（" + s.getScheduleDate() + "）";
            }
            case MobileTypeUtil.MODULE_MDT -> {
                MdtApplication m = mdtService.getById(relatedId);
                if (m == null) {
                    throw new IllegalArgumentException("MDT 会诊申请不存在：" + relatedId);
                }
                yield m.getPatientName() + "会诊申请";
            }
            case MobileTypeUtil.MODULE_NEWTECH -> {
                NewTechProject p = newTechService.getById(relatedId);
                if (p == null) {
                    throw new IllegalArgumentException("新技术新项目不存在：" + relatedId);
                }
                yield p.getProjectName();
            }
            case MobileTypeUtil.MODULE_PERSONNEL -> {
                PersonnelChange c = personnelChangeService.getById(relatedId);
                if (c == null) {
                    throw new IllegalArgumentException("医疗人员动态申请不存在：" + relatedId);
                }
                yield c.getPhysicianName() + c.getChangeType() + "申请";
            }
            case MobileTypeUtil.MODULE_MEDGRADING -> {
                GradingAssessment g = gradingAssessmentService.getById(relatedId);
                if (g == null) {
                    throw new IllegalArgumentException("医疗分级评定不存在：" + relatedId);
                }
                yield g.getDeptName() + g.getStandardName() + "（" + g.getPeriod() + "）";
            }
            case MobileTypeUtil.MODULE_BRIEFING -> {
                Briefing b = briefingService.getById(relatedId);
                if (b == null) {
                    throw new IllegalArgumentException("医疗工作简报不存在：" + relatedId);
                }
                yield b.getTitle();
            }
            default -> throw new IllegalArgumentException("业务模块不合法：" + moduleType);
        };
    }

    // ---------- 待审批聚合 ----------

    private List<MobilePendingItem> pendingLeaves() {
        List<MobilePendingItem> items = new ArrayList<>();
        for (int status : List.of(0, 1)) {
            LeaveQuery q = new LeaveQuery();
            q.setStatus(status);
            for (LeaveApplication a : leaveApplicationService.listByQuery(q)) {
                MobilePendingItem item = base(MobileTypeUtil.MODULE_LEAVE, a.getId(),
                        a.getPhysicianName() + MobileTypeUtil.leaveApplyTypeText(a.getApplyType()) + "申请",
                        a.getPhysicianName(), a.getDeptName(), a.getStatus(),
                        MobileTypeUtil.twoLevelStatusText(a.getStatus()), a.getCreateTime());
                items.add(item);
            }
        }
        return items;
    }

    private List<MobilePendingItem> pendingAuthorizations() {
        List<MobilePendingItem> items = new ArrayList<>();
        for (int status : List.of(0, 1)) {
            AuthorizationQuery q = new AuthorizationQuery();
            q.setStatus(status);
            for (Authorization a : authorizationService.listByQuery(q)) {
                items.add(base(MobileTypeUtil.MODULE_AUTH, a.getId(),
                        a.getPhysicianName() + MobileTypeUtil.authTypeText(a.getAuthType()) + "申请",
                        a.getPhysicianName(), a.getDeptName(), a.getStatus(),
                        MobileTypeUtil.twoLevelStatusText(a.getStatus()), a.getCreateTime()));
            }
        }
        return items;
    }

    private List<MobilePendingItem> pendingSchedules() {
        List<MobilePendingItem> items = new ArrayList<>();
        ScheduleQuery q = new ScheduleQuery();
        q.setChangeStatus(1);
        for (Schedule s : scheduleService.listByQuery(q)) {
            items.add(base(MobileTypeUtil.MODULE_SCHEDULE, s.getId(),
                    s.getPhysicianName()
                            + ("SUBSTITUTE".equals(s.getChangeType()) ? "代班" : "调班")
                            + "申请（" + s.getScheduleDate() + "）",
                    s.getPhysicianName(), s.getDeptName(), s.getChangeStatus(),
                    "待科主任审批", s.getCreateTime()));
        }
        return items;
    }

    private List<MobilePendingItem> pendingMdts() {
        List<MobilePendingItem> items = new ArrayList<>();
        MdtQuery q = new MdtQuery();
        q.setStatus(0);
        for (MdtApplication m : mdtService.listByQuery(q)) {
            items.add(base(MobileTypeUtil.MODULE_MDT, m.getId(),
                    m.getPatientName() + "会诊申请",
                    m.getApplyPhysicianName(), m.getDeptName(), m.getStatus(),
                    "待审批", m.getCreateTime()));
        }
        return items;
    }

    private List<MobilePendingItem> pendingNewTechs() {
        List<MobilePendingItem> items = new ArrayList<>();
        NewTechQuery q = new NewTechQuery();
        q.setStatus(0);
        for (NewTechProject p : newTechService.listByQuery(q)) {
            items.add(base(MobileTypeUtil.MODULE_NEWTECH, p.getId(),
                    p.getProjectName(),
                    p.getApplicantName(), p.getDeptName(), p.getStatus(),
                    "待审批", p.getCreateTime()));
        }
        return items;
    }

    private List<MobilePendingItem> pendingPersonnelChanges() {
        List<MobilePendingItem> items = new ArrayList<>();
        PersonnelChangeQuery q = new PersonnelChangeQuery();
        q.setStatus(0);
        for (PersonnelChange c : personnelChangeService.listByQuery(q)) {
            items.add(base(MobileTypeUtil.MODULE_PERSONNEL, c.getId(),
                    c.getPhysicianName() + c.getChangeType() + "申请",
                    c.getPhysicianName(), c.getFromDeptName(), c.getStatus(),
                    "待审批", c.getCreateTime()));
        }
        return items;
    }

    private List<MobilePendingItem> pendingGradingAssessments() {
        List<MobilePendingItem> items = new ArrayList<>();
        GradingAssessmentQuery q = new GradingAssessmentQuery();
        q.setStatus(0);
        q.setSize(100);
        for (GradingAssessment g : gradingAssessmentService.pageQuery(q).getRecords()) {
            items.add(base(MobileTypeUtil.MODULE_MEDGRADING, g.getId(),
                    g.getDeptName() + g.getStandardName() + "（" + g.getPeriod() + "）",
                    g.getAssessor(), g.getDeptName(), g.getStatus(),
                    "待审批", g.getCreateTime()));
        }
        return items;
    }

    private List<MobilePendingItem> pendingBriefings() {
        List<MobilePendingItem> items = new ArrayList<>();
        BriefingQuery q = new BriefingQuery();
        q.setStatus(1);
        q.setSize(100);
        for (Briefing b : briefingService.pageQuery(q).getRecords()) {
            items.add(base(MobileTypeUtil.MODULE_BRIEFING, b.getId(),
                    b.getTitle(),
                    b.getAuthorName(), null, b.getStatus(),
                    "待审批", b.getCreateTime()));
        }
        return items;
    }

    private MobilePendingItem base(String moduleType, Long relatedId, String title,
                                   String applicant, String deptName, Integer status,
                                   String statusText, LocalDateTime createTime) {
        MobilePendingItem item = new MobilePendingItem();
        item.setModuleType(moduleType);
        item.setModuleName(MobileTypeUtil.moduleText(moduleType));
        item.setRelatedId(relatedId);
        item.setTitle(title);
        item.setApplicant(applicant);
        item.setDeptName(deptName);
        item.setStatus(status);
        item.setStatusText(statusText);
        item.setCreateTime(createTime);
        return item;
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }

    private LambdaQueryWrapper<MobileApprovalLog> buildWrapper(MobileApprovalLogQuery query) {
        LambdaQueryWrapper<MobileApprovalLog> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(MobileApprovalLog::getRelatedTitle, kw)
                    .or().like(MobileApprovalLog::getOperator, kw)
                    .or().like(MobileApprovalLog::getDeviceCode, kw));
        }
        if (StringUtils.hasText(query.getModuleType())) {
            w.eq(MobileApprovalLog::getModuleType, query.getModuleType());
        }
        if (StringUtils.hasText(query.getAction())) {
            w.eq(MobileApprovalLog::getAction, query.getAction());
        }
        if (StringUtils.hasText(query.getResult())) {
            w.eq(MobileApprovalLog::getResult, query.getResult());
        }
        if (StringUtils.hasText(query.getOperator())) {
            w.eq(MobileApprovalLog::getOperator, query.getOperator());
        }
        w.orderByDesc(MobileApprovalLog::getActionTime).orderByDesc(MobileApprovalLog::getId);
        return w;
    }
}
