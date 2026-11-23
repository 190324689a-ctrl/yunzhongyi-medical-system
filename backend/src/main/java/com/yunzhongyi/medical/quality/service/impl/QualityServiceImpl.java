package com.yunzhongyi.medical.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.quality.dto.QualityPlanQuery;
import com.yunzhongyi.medical.quality.dto.QualityRectificationQuery;
import com.yunzhongyi.medical.quality.entity.QualityIssue;
import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityRecord;
import com.yunzhongyi.medical.quality.entity.QualityRectification;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.excel.QualityExcelUtil;
import com.yunzhongyi.medical.quality.mapper.QualityIssueMapper;
import com.yunzhongyi.medical.quality.mapper.QualityPlanMapper;
import com.yunzhongyi.medical.quality.mapper.QualityRecordMapper;
import com.yunzhongyi.medical.quality.mapper.QualityRectificationMapper;
import com.yunzhongyi.medical.quality.mapper.QualityTemplateMapper;
import com.yunzhongyi.medical.quality.service.QualityService;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 质量检查管理服务实现：检查计划 CRUD + 状态流转（待执行→进行中→已完成/
 * 已取消）+ 检查记录（逐项打分、总分与等级自动算）+ 检查问题 + 整改跟踪
 * （提交整改→复查通过/打回）+ 检查报告 + 统计 + Excel 导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityServiceImpl
        extends ServiceImpl<QualityPlanMapper, QualityPlan>
        implements QualityService {

    private final QualityRecordMapper recordMapper;
    private final QualityIssueMapper issueMapper;
    private final QualityRectificationMapper rectificationMapper;
    private final QualityTemplateMapper templateMapper;

    @Override
    public PageResult<QualityPlan> pageQuery(QualityPlanQuery query) {
        Page<QualityPlan> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<QualityPlan> listByQuery(QualityPlanQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<QualityPlan> plans = this.list();
        List<QualityRecord> records = recordMapper.selectList(null);
        List<QualityIssue> issues = issueMapper.selectList(null);
        List<QualityRectification> rects = rectificationMapper.selectList(null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) plans.size());
        r.put("pending", countPlans(plans, QualityTypeUtil.PLAN_PENDING));
        r.put("inProgress", countPlans(plans, QualityTypeUtil.PLAN_IN_PROGRESS));
        r.put("completed", countPlans(plans, QualityTypeUtil.PLAN_COMPLETED));
        r.put("cancelled", countPlans(plans, QualityTypeUtil.PLAN_CANCELLED));
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(QualityTypeUtil.TYPE_SELF, QualityTypeUtil.TYPE_HOSPITAL,
                QualityTypeUtil.TYPE_SPECIAL)) {
            byType.put(t, plans.stream().filter(p -> t.equals(p.getInspectType())).count());
        }
        r.put("byType", byType);
        r.put("byDept", statsByDept());
        r.put("byMonth", statsByMonth());
        r.put("recordCount", (long) records.size());
        r.put("avgScore", QualityTypeUtil.avgScore(
                records.stream().map(QualityRecord::getTotalScore).collect(Collectors.toList())));
        Map<String, Long> byGrade = new LinkedHashMap<>();
        for (String g : List.of(QualityTypeUtil.GRADE_EXCELLENT, QualityTypeUtil.GRADE_QUALIFIED,
                QualityTypeUtil.GRADE_BASIC, QualityTypeUtil.GRADE_UNQUALIFIED)) {
            byGrade.put(g, records.stream().filter(rec -> g.equals(rec.getGrade())).count());
        }
        r.put("byGrade", byGrade);
        r.put("issueCount", (long) issues.size());
        r.put("rectificationTotal", (long) rects.size());
        long reviewed = rects.stream()
                .filter(x -> Integer.valueOf(QualityTypeUtil.RECTIFY_REVIEWED).equals(x.getStatus())).count();
        r.put("rectificationReviewed", reviewed);
        r.put("rectificationRate", rects.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(reviewed * 100.0 / rects.size()).setScale(2, RoundingMode.HALF_UP));
        r.put("overdueCount", rects.stream()
                .filter(x -> QualityTypeUtil.isOverdue(x.getStatus(), x.getDeadline())).count());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<QualityPlan>> grouped = new LinkedHashMap<>();
        for (QualityPlan p : this.list()) {
            String dept = StringUtils.hasText(p.getDeptName()) ? p.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("completedCount", list.stream()
                    .filter(p -> Integer.valueOf(QualityTypeUtil.PLAN_COMPLETED).equals(p.getStatus())).count());
            row.put("inProgressCount", list.stream()
                    .filter(p -> Integer.valueOf(QualityTypeUtil.PLAN_IN_PROGRESS).equals(p.getStatus())).count());
            row.put("pendingCount", list.stream()
                    .filter(p -> Integer.valueOf(QualityTypeUtil.PLAN_PENDING).equals(p.getStatus())).count());
            List<QualityRecord> deptRecords = recordMapper.selectList(
                    new LambdaQueryWrapper<QualityRecord>().in(QualityRecord::getPlanId,
                            list.stream().map(QualityPlan::getId).collect(Collectors.toList())));
            row.put("recordCount", (long) deptRecords.size());
            row.put("avgScore", QualityTypeUtil.avgScore(
                    deptRecords.stream().map(QualityRecord::getTotalScore).collect(Collectors.toList())));
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        Map<YearMonth, List<QualityPlan>> grouped = new LinkedHashMap<>();
        for (QualityPlan p : this.list()) {
            if (p.getPlanDate() == null) {
                continue;
            }
            grouped.computeIfAbsent(YearMonth.from(p.getPlanDate()), k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.keySet().stream().sorted().forEach(month -> {
            List<QualityPlan> list = grouped.get(month);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            row.put("count", (long) list.size());
            row.put("completedCount", list.stream()
                    .filter(p -> Integer.valueOf(QualityTypeUtil.PLAN_COMPLETED).equals(p.getStatus())).count());
            row.put("inProgressCount", list.stream()
                    .filter(p -> Integer.valueOf(QualityTypeUtil.PLAN_IN_PROGRESS).equals(p.getStatus())).count());
            result.add(row);
        });
        return result;
    }

    @Override
    public Map<String, Object> detail(Long planId) {
        QualityPlan plan = getOrThrow(planId);
        List<QualityRecord> records = listRecords(planId);
        List<QualityIssue> issues = issueMapper.selectList(new LambdaQueryWrapper<QualityIssue>()
                .eq(QualityIssue::getPlanId, planId)
                .orderByAsc(QualityIssue::getFoundDate).orderByAsc(QualityIssue::getId));
        List<QualityRectification> rects = listRectifications(planId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("plan", plan);
        r.put("records", records);
        r.put("issues", issues);
        r.put("rectifications", rects);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityPlan start(Long id) {
        QualityPlan p = getOrThrow(id);
        if (!Integer.valueOf(QualityTypeUtil.PLAN_PENDING).equals(p.getStatus())) {
            throw new IllegalStateException("只有待执行的检查计划才能开始检查，当前状态："
                    + QualityTypeUtil.planStatusText(p.getStatus()));
        }
        p.setStatus(QualityTypeUtil.PLAN_IN_PROGRESS);
        // 同 MDT：直接调 super.updateById，跳过 normalize 的修改守卫
        super.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityPlan complete(Long id) {
        QualityPlan p = getOrThrow(id);
        if (!Integer.valueOf(QualityTypeUtil.PLAN_IN_PROGRESS).equals(p.getStatus())) {
            throw new IllegalStateException("只有进行中的检查计划才能完成，当前状态："
                    + QualityTypeUtil.planStatusText(p.getStatus()));
        }
        if (recordMapper.selectCount(new LambdaQueryWrapper<QualityRecord>()
                .eq(QualityRecord::getPlanId, id)) == 0) {
            throw new IllegalStateException("该检查计划还没有检查记录，不能完成");
        }
        p.setStatus(QualityTypeUtil.PLAN_COMPLETED);
        super.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityPlan cancel(Long id) {
        QualityPlan p = getOrThrow(id);
        if (Integer.valueOf(QualityTypeUtil.PLAN_COMPLETED).equals(p.getStatus())
                || Integer.valueOf(QualityTypeUtil.PLAN_CANCELLED).equals(p.getStatus())) {
            throw new IllegalStateException("已完成/已取消的检查计划不能再取消，当前状态："
                    + QualityTypeUtil.planStatusText(p.getStatus()));
        }
        p.setStatus(QualityTypeUtil.PLAN_CANCELLED);
        super.updateById(p);
        return p;
    }

    @Override
    public List<QualityRecord> listRecords(Long planId) {
        getOrThrow(planId);
        return recordMapper.selectList(new LambdaQueryWrapper<QualityRecord>()
                .eq(QualityRecord::getPlanId, planId)
                .orderByAsc(QualityRecord::getCheckDate).orderByAsc(QualityRecord::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityRecord addRecord(Long planId, QualityRecord record) {
        QualityPlan plan = getOrThrow(planId);
        if (!Integer.valueOf(QualityTypeUtil.PLAN_IN_PROGRESS).equals(plan.getStatus())) {
            throw new IllegalStateException("只有进行中的检查计划才能添加检查记录，当前状态："
                    + QualityTypeUtil.planStatusText(plan.getStatus()));
        }
        Map<String, BigDecimal> scores = QualityTypeUtil.parseItemScores(record.getItemScores());
        if (scores.isEmpty()) {
            throw new IllegalArgumentException("各项得分不能为空，至少填写一项");
        }
        // 每项得分不能超过该项在模板中的权重，也不能为负数
        Map<String, BigDecimal> weights = Map.of();
        if (plan.getTemplateId() != null) {
            QualityTemplate template = templateMapper.selectById(plan.getTemplateId());
            if (template != null) {
                weights = QualityTypeUtil.weightMap(template.getCheckItems());
            }
        }
        for (Map.Entry<String, BigDecimal> e : scores.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("检查项「" + e.getKey() + "」得分不能为负数");
            }
            BigDecimal w = weights.get(e.getKey());
            if (w != null && e.getValue().compareTo(w) > 0) {
                throw new IllegalArgumentException("检查项「" + e.getKey() + "」得分 "
                        + e.getValue().stripTrailingZeros().toPlainString()
                        + " 超过该项权重 " + w.stripTrailingZeros().toPlainString());
            }
        }
        record.setPlanId(planId);
        record.setTemplateId(plan.getTemplateId());
        record.setTemplateName(plan.getTemplateName());
        record.setInspectType(plan.getInspectType());
        record.setDeptId(plan.getDeptId());
        record.setDeptName(plan.getDeptName());
        record.setTotalScore(QualityTypeUtil.sumItemScores(record.getItemScores()));
        record.setGrade(QualityTypeUtil.gradeOf(record.getTotalScore()));
        if (record.getCheckDate() == null) {
            record.setCheckDate(LocalDate.now());
        }
        recordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRecord(Long planId, Long recordId) {
        getOrThrow(planId);
        QualityRecord record = recordMapper.selectById(recordId);
        if (record == null || !planId.equals(record.getPlanId())) {
            throw new IllegalArgumentException("检查记录不存在：" + recordId);
        }
        List<QualityIssue> issues = issueMapper.selectList(new LambdaQueryWrapper<QualityIssue>()
                .eq(QualityIssue::getRecordId, recordId));
        for (QualityIssue issue : issues) {
            rectificationMapper.delete(new LambdaQueryWrapper<QualityRectification>()
                    .eq(QualityRectification::getIssueId, issue.getId()));
        }
        issueMapper.delete(new LambdaQueryWrapper<QualityIssue>()
                .eq(QualityIssue::getRecordId, recordId));
        return recordMapper.deleteById(recordId) > 0;
    }

    @Override
    public List<QualityIssue> listIssues(Long recordId) {
        getRecordOrThrow(recordId);
        return issueMapper.selectList(new LambdaQueryWrapper<QualityIssue>()
                .eq(QualityIssue::getRecordId, recordId)
                .orderByAsc(QualityIssue::getFoundDate).orderByAsc(QualityIssue::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityIssue addIssue(Long recordId, QualityIssue issue) {
        QualityRecord record = getRecordOrThrow(recordId);
        if (!StringUtils.hasText(issue.getDescription())) {
            throw new IllegalArgumentException("问题描述不能为空");
        }
        if (!StringUtils.hasText(issue.getSeverity()) || !QualityTypeUtil.isValidSeverity(issue.getSeverity())) {
            throw new IllegalArgumentException("严重程度不正确");
        }
        issue.setRecordId(recordId);
        issue.setPlanId(record.getPlanId());
        if (issue.getFoundDate() == null) {
            issue.setFoundDate(LocalDate.now());
        }
        if (!StringUtils.hasText(issue.getRespDeptName())) {
            issue.setRespDeptName(record.getDeptName());
        }
        issueMapper.insert(issue);
        return issue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeIssue(Long recordId, Long issueId) {
        getRecordOrThrow(recordId);
        QualityIssue issue = issueMapper.selectById(issueId);
        if (issue == null || !recordId.equals(issue.getRecordId())) {
            throw new IllegalArgumentException("检查问题不存在：" + issueId);
        }
        rectificationMapper.delete(new LambdaQueryWrapper<QualityRectification>()
                .eq(QualityRectification::getIssueId, issueId));
        return issueMapper.deleteById(issueId) > 0;
    }

    @Override
    public List<QualityRectification> listRectifications(Long planId) {
        getOrThrow(planId);
        List<QualityRectification> list = rectificationMapper.selectList(
                new LambdaQueryWrapper<QualityRectification>()
                        .eq(QualityRectification::getPlanId, planId)
                        .orderByAsc(QualityRectification::getDeadline).orderByAsc(QualityRectification::getId));
        fillOverdue(list);
        return list;
    }

    @Override
    public PageResult<QualityRectification> pageRectifications(QualityRectificationQuery query) {
        LambdaQueryWrapper<QualityRectification> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(query.getRespPerson()),
                QualityRectification::getRespPerson, query.getRespPerson());
        qw.eq(query.getStatus() != null, QualityRectification::getStatus, query.getStatus());
        qw.eq(query.getPlanId() != null, QualityRectification::getPlanId, query.getPlanId());
        if (Boolean.TRUE.equals(query.getOverdueOnly())) {
            // 逾期 = 待整改/已整改且期限已过当天
            qw.lt(QualityRectification::getDeadline, LocalDate.now());
            qw.in(QualityRectification::getStatus,
                    List.of(QualityTypeUtil.RECTIFY_PENDING, QualityTypeUtil.RECTIFY_DONE));
        }
        qw.orderByAsc(QualityRectification::getDeadline).orderByAsc(QualityRectification::getId);
        Page<QualityRectification> p = rectificationMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), qw);
        fillOverdue(p.getRecords());
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityRectification addRectification(Long issueId, QualityRectification rectification) {
        QualityIssue issue = getIssueOrThrow(issueId);
        if (rectificationMapper.selectCount(new LambdaQueryWrapper<QualityRectification>()
                .eq(QualityRectification::getIssueId, issueId)) > 0) {
            throw new IllegalStateException("该问题已有整改任务，不能重复发");
        }
        if (!StringUtils.hasText(rectification.getMeasure())) {
            throw new IllegalArgumentException("整改措施不能为空");
        }
        if (rectification.getDeadline() == null) {
            throw new IllegalArgumentException("整改期限不能为空");
        }
        if (!StringUtils.hasText(rectification.getRespPerson())) {
            throw new IllegalArgumentException("整改责任人不能为空");
        }
        rectification.setIssueId(issueId);
        rectification.setPlanId(issue.getPlanId());
        rectification.setRecordId(issue.getRecordId());
        if (rectification.getStatus() == null) {
            rectification.setStatus(QualityTypeUtil.RECTIFY_PENDING);
        }
        rectificationMapper.insert(rectification);
        fillOverdue(List.of(rectification));
        return rectification;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityRectification submitRectification(Long rectificationId) {
        QualityRectification r = getRectificationOrThrow(rectificationId);
        if (!Integer.valueOf(QualityTypeUtil.RECTIFY_PENDING).equals(r.getStatus())) {
            throw new IllegalStateException("只有待整改的任务才能提交整改，当前状态："
                    + QualityTypeUtil.rectifyStatusText(r.getStatus()));
        }
        r.setStatus(QualityTypeUtil.RECTIFY_DONE);
        r.setRectifyDate(LocalDate.now());
        rectificationMapper.updateById(r);
        fillOverdue(List.of(r));
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityRectification reviewPass(Long rectificationId, String reviewer, String opinion) {
        QualityRectification r = getRectificationOrThrow(rectificationId);
        if (!Integer.valueOf(QualityTypeUtil.RECTIFY_DONE).equals(r.getStatus())) {
            throw new IllegalStateException("只有已整改（待复查）的任务才能复查通过，当前状态："
                    + QualityTypeUtil.rectifyStatusText(r.getStatus()));
        }
        r.setStatus(QualityTypeUtil.RECTIFY_REVIEWED);
        r.setReviewer(reviewer);
        r.setReviewOpinion(opinion);
        r.setReviewDate(LocalDate.now());
        rectificationMapper.updateById(r);
        fillOverdue(List.of(r));
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityRectification reviewReject(Long rectificationId, String reviewer, String opinion) {
        QualityRectification r = getRectificationOrThrow(rectificationId);
        if (!Integer.valueOf(QualityTypeUtil.RECTIFY_DONE).equals(r.getStatus())) {
            throw new IllegalStateException("只有已整改（待复查）的任务才能复查打回，当前状态："
                    + QualityTypeUtil.rectifyStatusText(r.getStatus()));
        }
        r.setStatus(QualityTypeUtil.RECTIFY_PENDING);
        r.setReviewer(reviewer);
        r.setReviewOpinion(opinion);
        r.setReviewDate(LocalDate.now());
        r.setRectifyDate(null);
        rectificationMapper.updateById(r);
        fillOverdue(List.of(r));
        return r;
    }

    @Override
    public Map<String, Object> report(Long planId) {
        QualityPlan plan = getOrThrow(planId);
        List<QualityRecord> records = listRecords(planId);
        List<QualityIssue> issues = issueMapper.selectList(new LambdaQueryWrapper<QualityIssue>()
                .eq(QualityIssue::getPlanId, planId));
        List<QualityRectification> rects = listRectifications(planId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("plan", plan);
        r.put("records", records);
        r.put("recordCount", (long) records.size());
        r.put("avgScore", QualityTypeUtil.avgScore(
                records.stream().map(QualityRecord::getTotalScore).collect(Collectors.toList())));
        Map<String, Long> byGrade = new LinkedHashMap<>();
        for (String g : List.of(QualityTypeUtil.GRADE_EXCELLENT, QualityTypeUtil.GRADE_QUALIFIED,
                QualityTypeUtil.GRADE_BASIC, QualityTypeUtil.GRADE_UNQUALIFIED)) {
            byGrade.put(g, records.stream().filter(rec -> g.equals(rec.getGrade())).count());
        }
        r.put("byGrade", byGrade);
        r.put("issueCount", (long) issues.size());
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (String s : List.of(QualityTypeUtil.SEVERITY_MAJOR, QualityTypeUtil.SEVERITY_MINOR,
                QualityTypeUtil.SEVERITY_MILD)) {
            bySeverity.put(s, issues.stream().filter(i -> s.equals(i.getSeverity())).count());
        }
        r.put("bySeverity", bySeverity);
        r.put("issues", issues);
        r.put("rectifications", rects);
        r.put("rectificationTotal", (long) rects.size());
        long reviewed = rects.stream()
                .filter(x -> Integer.valueOf(QualityTypeUtil.RECTIFY_REVIEWED).equals(x.getStatus())).count();
        r.put("rectificationReviewed", reviewed);
        r.put("rectificationRate", rects.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(reviewed * 100.0 / rects.size()).setScale(2, RoundingMode.HALF_UP));
        r.put("overdueCount", rects.stream()
                .filter(x -> QualityTypeUtil.isOverdue(x.getStatus(), x.getDeadline())).count());
        return r;
    }

    @Override
    public void exportExcel(QualityPlanQuery query, OutputStream out) {
        List<QualityPlan> plans = listByQuery(query);
        List<Long> planIds = plans.stream().map(QualityPlan::getId).collect(Collectors.toList());
        List<QualityRecord> records = planIds.isEmpty() ? List.of()
                : recordMapper.selectList(new LambdaQueryWrapper<QualityRecord>()
                        .in(QualityRecord::getPlanId, planIds)
                        .orderByAsc(QualityRecord::getCheckDate).orderByAsc(QualityRecord::getId));
        Map<Long, Long> issueCountByRecord = issueMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(QualityIssue::getRecordId, Collectors.counting()));
        QualityExcelUtil.writeRecords(records, issueCountByRecord, out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<QualityPlan> list = QualityExcelUtil.readPlans(in);
        int ok = 0;
        int skipped = 0;
        List<String> details = new ArrayList<>();
        for (QualityPlan p : list) {
            // 模板名称必填，按名称找模板；找不到则跳过并说明
            QualityTemplate template = null;
            if (StringUtils.hasText(p.getTemplateName())) {
                template = templateMapper.selectOne(new LambdaQueryWrapper<QualityTemplate>()
                        .eq(QualityTemplate::getTemplateName, p.getTemplateName().trim()));
            }
            if (template == null) {
                skipped++;
                details.add("跳过：模板「" + p.getTemplateName() + "」不存在（"
                        + planLabel(p) + "）");
                continue;
            }
            p.setTemplateId(template.getId());
            // 检查类型非法时按模板类型给默认值
            if (!StringUtils.hasText(p.getInspectType()) || !QualityTypeUtil.isValidType(p.getInspectType())) {
                p.setInspectType(template.getInspectType());
                details.add("默认值：检查类型非法，已按模板「" + template.getTemplateName()
                        + "」补为" + QualityTypeUtil.typeText(template.getInspectType())
                        + "（" + planLabel(p) + "）");
            }
            if (!StringUtils.hasText(p.getDeptName())) {
                skipped++;
                details.add("跳过：被查科室为空（" + planLabel(p) + "）");
                continue;
            }
            if (!StringUtils.hasText(p.getInspector())) {
                skipped++;
                details.add("跳过：检查人为空（" + planLabel(p) + "）");
                continue;
            }
            if (p.getPlanDate() == null) {
                skipped++;
                details.add("跳过：计划检查日期为空（" + planLabel(p) + "）");
                continue;
            }
            if (p.getStatus() == null || (p.getStatus() != QualityTypeUtil.PLAN_PENDING
                    && p.getStatus() != QualityTypeUtil.PLAN_IN_PROGRESS
                    && p.getStatus() != QualityTypeUtil.PLAN_COMPLETED
                    && p.getStatus() != QualityTypeUtil.PLAN_CANCELLED)) {
                p.setStatus(QualityTypeUtil.PLAN_PENDING);
                details.add("默认值：计划状态非法，已补为待执行（" + planLabel(p) + "）");
            }
            // 同一模板同一科室同一计划日期视为重复，跳过
            LambdaQueryWrapper<QualityPlan> qw = new LambdaQueryWrapper<QualityPlan>()
                    .eq(QualityPlan::getTemplateId, p.getTemplateId())
                    .eq(QualityPlan::getDeptName, p.getDeptName())
                    .eq(QualityPlan::getPlanDate, p.getPlanDate());
            if (this.count(qw) > 0) {
                skipped++;
                details.add("跳过：重复检查计划（" + planLabel(p) + "）");
                continue;
            }
            this.save(p);
            ok++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", ok);
        result.put("skipped", skipped);
        result.put("details", details);
        return result;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<QualityPlan> buildWrapper(QualityPlanQuery q) {
        LambdaQueryWrapper<QualityPlan> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.and(w -> w.like(QualityPlan::getTemplateName, q.getKeyword())
                    .or().like(QualityPlan::getDeptName, q.getKeyword())
                    .or().like(QualityPlan::getInspector, q.getKeyword()));
        }
        qw.eq(StringUtils.hasText(q.getInspectType()), QualityPlan::getInspectType, q.getInspectType());
        qw.eq(q.getDeptId() != null, QualityPlan::getDeptId, q.getDeptId());
        qw.eq(q.getTemplateId() != null, QualityPlan::getTemplateId, q.getTemplateId());
        qw.eq(q.getStatus() != null, QualityPlan::getStatus, q.getStatus());
        qw.like(StringUtils.hasText(q.getInspector()), QualityPlan::getInspector, q.getInspector());
        qw.ge(q.getStartDate() != null, QualityPlan::getPlanDate, q.getStartDate());
        qw.le(q.getEndDate() != null, QualityPlan::getPlanDate, q.getEndDate());
        qw.orderByDesc(QualityPlan::getPlanDate).orderByDesc(QualityPlan::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时只允许改待执行的计划 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(QualityPlan entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(QualityPlan entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(QualityPlan p, boolean isCreate) {
        if (!StringUtils.hasText(p.getInspectType()) || !QualityTypeUtil.isValidType(p.getInspectType())) {
            throw new IllegalArgumentException("检查类型不正确");
        }
        if (p.getPlanDate() == null) {
            throw new IllegalArgumentException("计划检查日期不能为空");
        }
        if (p.getTemplateId() != null) {
            QualityTemplate template = templateMapper.selectById(p.getTemplateId());
            if (template == null) {
                throw new IllegalArgumentException("检查模板不存在：" + p.getTemplateId());
            }
            if (!Integer.valueOf(1).equals(template.getStatus())) {
                throw new IllegalArgumentException("检查模板「" + template.getTemplateName() + "」已停用，不能用于新建计划");
            }
            if (!StringUtils.hasText(p.getTemplateName())) {
                p.setTemplateName(template.getTemplateName());
            }
            // 计划的检查类型与模板一致
            p.setInspectType(template.getInspectType());
        }
        if (isCreate) {
            if (p.getStatus() == null) {
                p.setStatus(QualityTypeUtil.PLAN_PENDING);
            }
        } else {
            // 更新只允许改待执行的计划（开始/完成/取消不走这里，见 start/complete/cancel）。
            // 注意：这里用 selectOne 而不用 getById——同一事务内 getById 可能因
            // MyBatis 一级缓存返回调用方已改过的同一对象，导致状态判断失真。
            QualityPlan existing = this.baseMapper.selectOne(new LambdaQueryWrapper<QualityPlan>()
                    .eq(QualityPlan::getId, p.getId()));
            if (existing == null) {
                throw new IllegalArgumentException("检查计划不存在：" + p.getId());
            }
            if (!Integer.valueOf(QualityTypeUtil.PLAN_PENDING).equals(existing.getStatus())) {
                throw new IllegalStateException("只有待执行的检查计划能修改，当前状态："
                        + QualityTypeUtil.planStatusText(existing.getStatus()));
            }
        }
    }

    private QualityPlan getOrThrow(Long id) {
        QualityPlan p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("检查计划不存在：" + id);
        }
        return p;
    }

    private QualityRecord getRecordOrThrow(Long id) {
        QualityRecord r = recordMapper.selectById(id);
        if (r == null) {
            throw new IllegalArgumentException("检查记录不存在：" + id);
        }
        return r;
    }

    private QualityIssue getIssueOrThrow(Long id) {
        QualityIssue i = issueMapper.selectById(id);
        if (i == null) {
            throw new IllegalArgumentException("检查问题不存在：" + id);
        }
        return i;
    }

    private QualityRectification getRectificationOrThrow(Long id) {
        QualityRectification r = rectificationMapper.selectById(id);
        if (r == null) {
            throw new IllegalArgumentException("整改任务不存在：" + id);
        }
        return r;
    }

    private long countPlans(List<QualityPlan> plans, int status) {
        return plans.stream()
                .filter(p -> Integer.valueOf(status).equals(p.getStatus())).count();
    }

    /** 给整改任务补逾期标记 */
    private void fillOverdue(List<QualityRectification> list) {
        for (QualityRectification r : list) {
            r.setOverdue(QualityTypeUtil.isOverdue(r.getStatus(), r.getDeadline()));
        }
    }

    private String planLabel(QualityPlan p) {
        return (p.getTemplateName() == null ? "" : p.getTemplateName())
                + " / " + (p.getDeptName() == null ? "" : p.getDeptName())
                + " / " + (p.getPlanDate() == null ? "" : p.getPlanDate());
    }
}
