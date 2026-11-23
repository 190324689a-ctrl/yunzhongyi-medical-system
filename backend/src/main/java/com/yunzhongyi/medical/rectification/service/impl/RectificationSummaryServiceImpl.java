package com.yunzhongyi.medical.rectification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.quality.entity.QualityIssue;
import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityRecord;
import com.yunzhongyi.medical.quality.entity.QualityRectification;
import com.yunzhongyi.medical.quality.mapper.QualityIssueMapper;
import com.yunzhongyi.medical.quality.mapper.QualityPlanMapper;
import com.yunzhongyi.medical.quality.mapper.QualityRecordMapper;
import com.yunzhongyi.medical.quality.mapper.QualityRectificationMapper;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import com.yunzhongyi.medical.rectification.dto.GenerateRequest;
import com.yunzhongyi.medical.rectification.dto.IssueSummaryQuery;
import com.yunzhongyi.medical.rectification.dto.OverdueQuery;
import com.yunzhongyi.medical.rectification.dto.RectificationSummaryQuery;
import com.yunzhongyi.medical.rectification.entity.RectificationSummary;
import com.yunzhongyi.medical.rectification.excel.RectificationSummaryExcelUtil;
import com.yunzhongyi.medical.rectification.mapper.RectificationSummaryMapper;
import com.yunzhongyi.medical.rectification.service.RectificationSummaryService;
import com.yunzhongyi.medical.rectification.service.RectificationTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检查整改汇总服务实现：汇总报告 CRUD + 按统计周期生成汇总 +
 * 检查问题/整改任务实时汇总统计 + 逾期整改与问题汇总查询 + Excel 导入导出
 * <p>
 * 问题与整改任务本身由质量检查管理模块维护，本模块只读
 * quality_issue / quality_rectification（另加 quality_plan /
 * quality_record 取科室、检查类型与平均分），不改写它们。
 * <p>
 * 逾期判断与质量检查模块一致：待整改/已整改且整改期限已过当天
 * （已复查通过的不算逾期），见 QualityTypeUtil#isOverdue。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RectificationSummaryServiceImpl
        extends ServiceImpl<RectificationSummaryMapper, RectificationSummary>
        implements RectificationSummaryService {

    private final QualityIssueMapper issueMapper;
    private final QualityRectificationMapper rectificationMapper;
    private final QualityPlanMapper planMapper;
    private final QualityRecordMapper recordMapper;

    @Override
    public PageResult<RectificationSummary> pageQuery(RectificationSummaryQuery query) {
        Page<RectificationSummary> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<RectificationSummary> listByQuery(RectificationSummaryQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public RectificationSummary detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    public Map<String, Object> stats() {
        List<QualityIssue> issues = issueMapper.selectList(null);
        List<QualityRectification> rects = rectificationMapper.selectList(null);
        List<QualityPlan> plans = planMapper.selectList(null);
        List<QualityRecord> records = recordMapper.selectList(null);
        Map<String, Object> r = aggregate(issues, rects);
        r.put("planCount", (long) plans.size());
        r.put("recordCount", (long) records.size());
        r.put("avgScore", QualityTypeUtil.avgScore(
                records.stream().map(QualityRecord::getTotalScore).collect(Collectors.toList())));
        r.put("byDept", statsByDept());
        r.put("byType", statsByType());
        r.put("byMonth", statsByMonth());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        List<QualityIssue> issues = issueMapper.selectList(null);
        List<QualityRectification> rects = rectificationMapper.selectList(null);
        List<QualityPlan> plans = planMapper.selectList(null);
        List<QualityRecord> records = recordMapper.selectList(null);
        Map<Long, String> planTypeById = plans.stream()
                .collect(Collectors.toMap(QualityPlan::getId, QualityPlan::getInspectType,
                        (a, b) -> a));
        // 科室集合：问题责任科室 + 检查计划被查科室，去重后按科室名排序
        List<String> depts = new ArrayList<>();
        for (QualityIssue i : issues) {
            addDept(depts, i.getRespDeptName());
        }
        for (QualityPlan p : plans) {
            addDept(depts, p.getDeptName());
        }
        depts.sort(String::compareTo);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String dept : depts) {
            List<QualityIssue> deptIssues = issues.stream()
                    .filter(i -> dept.equals(i.getRespDeptName())).collect(Collectors.toList());
            List<QualityPlan> deptPlans = plans.stream()
                    .filter(p -> dept.equals(p.getDeptName())).collect(Collectors.toList());
            List<QualityRecord> deptRecords = records.stream()
                    .filter(rec -> dept.equals(rec.getDeptName())).collect(Collectors.toList());
            Map<String, Object> row = aggregate(deptIssues, rectsOf(deptIssues, rects));
            row.put("deptName", dept);
            row.put("planCount", (long) deptPlans.size());
            row.put("recordCount", (long) deptRecords.size());
            row.put("avgScore", QualityTypeUtil.avgScore(
                    deptRecords.stream().map(QualityRecord::getTotalScore).collect(Collectors.toList())));
            // 该科室的问题按检查类型分组（类型取自所属计划）
            Map<String, Long> byType = new LinkedHashMap<>();
            for (String t : List.of(QualityTypeUtil.TYPE_SELF, QualityTypeUtil.TYPE_HOSPITAL,
                    QualityTypeUtil.TYPE_SPECIAL)) {
                byType.put(t, deptIssues.stream()
                        .filter(i -> t.equals(planTypeById.get(i.getPlanId()))).count());
            }
            row.put("byType", byType);
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByType() {
        List<QualityIssue> issues = issueMapper.selectList(null);
        List<QualityRectification> rects = rectificationMapper.selectList(null);
        List<QualityPlan> plans = planMapper.selectList(null);
        List<QualityRecord> records = recordMapper.selectList(null);
        Map<Long, String> planTypeById = plans.stream()
                .collect(Collectors.toMap(QualityPlan::getId, QualityPlan::getInspectType,
                        (a, b) -> a));
        List<Map<String, Object>> result = new ArrayList<>();
        for (String type : List.of(QualityTypeUtil.TYPE_SELF, QualityTypeUtil.TYPE_HOSPITAL,
                QualityTypeUtil.TYPE_SPECIAL)) {
            List<QualityIssue> typeIssues = issues.stream()
                    .filter(i -> type.equals(planTypeById.get(i.getPlanId()))).collect(Collectors.toList());
            List<QualityPlan> typePlans = plans.stream()
                    .filter(p -> type.equals(p.getInspectType())).collect(Collectors.toList());
            List<QualityRecord> typeRecords = records.stream()
                    .filter(rec -> type.equals(rec.getInspectType())).collect(Collectors.toList());
            Map<String, Object> row = aggregate(typeIssues, rectsOf(typeIssues, rects));
            row.put("inspectType", type);
            row.put("typeText", QualityTypeUtil.typeText(type));
            row.put("planCount", (long) typePlans.size());
            row.put("recordCount", (long) typeRecords.size());
            row.put("avgScore", QualityTypeUtil.avgScore(
                    typeRecords.stream().map(QualityRecord::getTotalScore).collect(Collectors.toList())));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        List<QualityIssue> issues = issueMapper.selectList(null);
        List<QualityRectification> rects = rectificationMapper.selectList(null);
        List<QualityPlan> plans = planMapper.selectList(null);
        List<QualityRecord> records = recordMapper.selectList(null);
        // 月份集合：问题发现月份 + 检查计划月份，去重后升序
        List<YearMonth> months = new ArrayList<>();
        for (QualityIssue i : issues) {
            if (i.getFoundDate() != null && !months.contains(YearMonth.from(i.getFoundDate()))) {
                months.add(YearMonth.from(i.getFoundDate()));
            }
        }
        for (QualityPlan p : plans) {
            if (p.getPlanDate() != null && !months.contains(YearMonth.from(p.getPlanDate()))) {
                months.add(YearMonth.from(p.getPlanDate()));
            }
        }
        months.sort(YearMonth::compareTo);
        List<Map<String, Object>> result = new ArrayList<>();
        for (YearMonth month : months) {
            List<QualityIssue> monthIssues = issues.stream()
                    .filter(i -> i.getFoundDate() != null && YearMonth.from(i.getFoundDate()).equals(month))
                    .collect(Collectors.toList());
            List<QualityPlan> monthPlans = plans.stream()
                    .filter(p -> p.getPlanDate() != null && YearMonth.from(p.getPlanDate()).equals(month))
                    .collect(Collectors.toList());
            List<QualityRecord> monthRecords = records.stream()
                    .filter(rec -> rec.getCheckDate() != null && YearMonth.from(rec.getCheckDate()).equals(month))
                    .collect(Collectors.toList());
            Map<String, Object> row = aggregate(monthIssues, rectsOf(monthIssues, rects));
            row.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            row.put("planCount", (long) monthPlans.size());
            row.put("recordCount", (long) monthRecords.size());
            row.put("avgScore", QualityTypeUtil.avgScore(
                    monthRecords.stream().map(QualityRecord::getTotalScore).collect(Collectors.toList())));
            result.add(row);
        }
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> pageOverdue(OverdueQuery query) {
        List<QualityIssue> issues = issueMapper.selectList(null);
        Map<Long, QualityIssue> issueById = issues.stream()
                .collect(Collectors.toMap(QualityIssue::getId, i -> i, (a, b) -> a));
        List<QualityPlan> plans = planMapper.selectList(null);
        Map<Long, QualityPlan> planById = plans.stream()
                .collect(Collectors.toMap(QualityPlan::getId, p -> p, (a, b) -> a));
        List<QualityRectification> overdue = rectificationMapper.selectList(null).stream()
                .filter(r -> QualityTypeUtil.isOverdue(r.getStatus(), r.getDeadline()))
                .filter(r -> {
                    if (!StringUtils.hasText(query.getKeyword())) {
                        return true;
                    }
                    QualityIssue issue = issueById.get(r.getIssueId());
                    String desc = issue == null ? "" : issue.getDescription();
                    return (r.getRespPerson() != null && r.getRespPerson().contains(query.getKeyword()))
                            || (desc != null && desc.contains(query.getKeyword()));
                })
                .filter(r -> {
                    if (!StringUtils.hasText(query.getDeptName())) {
                        return true;
                    }
                    QualityIssue issue = issueById.get(r.getIssueId());
                    return issue != null && issue.getRespDeptName() != null
                            && issue.getRespDeptName().contains(query.getDeptName());
                })
                .sorted((a, b) -> {
                    int c = a.getDeadline().compareTo(b.getDeadline());
                    return c != 0 ? c : a.getId().compareTo(b.getId());
                })
                .collect(Collectors.toList());
        List<Map<String, Object>> rows = overdue.stream()
                .map(r -> rectificationRow(r, issueById.get(r.getIssueId()),
                        planById.get(r.getPlanId())))
                .collect(Collectors.toList());
        return paginate(rows, query.getPage(), query.getSize());
    }

    @Override
    public PageResult<Map<String, Object>> pageIssueSummary(IssueSummaryQuery query) {
        List<QualityIssue> issues = issueMapper.selectList(null);
        List<QualityRectification> rects = rectificationMapper.selectList(null);
        Map<Long, QualityRectification> rectByIssueId = rects.stream()
                .collect(Collectors.toMap(QualityRectification::getIssueId, r -> r, (a, b) -> a));
        List<QualityPlan> plans = planMapper.selectList(null);
        Map<Long, QualityPlan> planById = plans.stream()
                .collect(Collectors.toMap(QualityPlan::getId, p -> p, (a, b) -> a));
        List<QualityIssue> filtered = issues.stream()
                .filter(i -> {
                    if (!StringUtils.hasText(query.getKeyword())) {
                        return true;
                    }
                    return (i.getDescription() != null && i.getDescription().contains(query.getKeyword()))
                            || (i.getRespPerson() != null && i.getRespPerson().contains(query.getKeyword()));
                })
                .filter(i -> !StringUtils.hasText(query.getDeptName())
                        || (i.getRespDeptName() != null && i.getRespDeptName().contains(query.getDeptName())))
                .filter(i -> !StringUtils.hasText(query.getSeverity())
                        || query.getSeverity().equals(i.getSeverity()))
                .filter(i -> {
                    if (!StringUtils.hasText(query.getInspectType())) {
                        return true;
                    }
                    QualityPlan plan = planById.get(i.getPlanId());
                    return plan != null && query.getInspectType().equals(plan.getInspectType());
                })
                .filter(i -> query.getStartDate() == null
                        || (i.getFoundDate() != null && !i.getFoundDate().isBefore(query.getStartDate())))
                .filter(i -> query.getEndDate() == null
                        || (i.getFoundDate() != null && !i.getFoundDate().isAfter(query.getEndDate())))
                .filter(i -> {
                    if (!Boolean.TRUE.equals(query.getOverdueOnly())) {
                        return true;
                    }
                    QualityRectification r = rectByIssueId.get(i.getId());
                    return r != null && QualityTypeUtil.isOverdue(r.getStatus(), r.getDeadline());
                })
                .filter(i -> {
                    if (!Boolean.TRUE.equals(query.getUnrectifiedOnly())) {
                        return true;
                    }
                    QualityRectification r = rectByIssueId.get(i.getId());
                    return r == null || !Integer.valueOf(QualityTypeUtil.RECTIFY_REVIEWED).equals(r.getStatus());
                })
                .sorted((a, b) -> {
                    LocalDate da = a.getFoundDate() == null ? LocalDate.MIN : a.getFoundDate();
                    LocalDate db = b.getFoundDate() == null ? LocalDate.MIN : b.getFoundDate();
                    int c = db.compareTo(da);
                    return c != 0 ? c : b.getId().compareTo(a.getId());
                })
                .collect(Collectors.toList());
        List<Map<String, Object>> rows = filtered.stream()
                .map(i -> issueRow(i, rectByIssueId.get(i.getId()), planById.get(i.getPlanId())))
                .collect(Collectors.toList());
        return paginate(rows, query.getPage(), query.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RectificationSummary generate(GenerateRequest request) {
        validateGenerateRequest(request);
        YearMonth month = YearMonth.parse(request.getStatPeriod());
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<QualityIssue> issues = issueMapper.selectList(null);
        List<QualityRectification> rects = rectificationMapper.selectList(null);
        List<QualityPlan> plans = planMapper.selectList(null);
        List<QualityRecord> records = recordMapper.selectList(null);
        Map<Long, String> planTypeById = plans.stream()
                .collect(Collectors.toMap(QualityPlan::getId, QualityPlan::getInspectType,
                        (a, b) -> a));

        // 该周期内发现的问题（按发现日期落在统计月份内）
        List<QualityIssue> monthIssues = issues.stream()
                .filter(i -> i.getFoundDate() != null
                        && !i.getFoundDate().isBefore(start) && !i.getFoundDate().isAfter(end))
                .filter(i -> {
                    if (RectificationTypeUtil.SCOPE_DEPT.equals(request.getScopeType())) {
                        return request.getDeptName().equals(i.getRespDeptName());
                    }
                    if (RectificationTypeUtil.SCOPE_TYPE.equals(request.getScopeType())) {
                        return request.getInspectType().equals(planTypeById.get(i.getPlanId()));
                    }
                    return true;
                })
                .collect(Collectors.toList());
        List<QualityPlan> monthPlans = plans.stream()
                .filter(p -> p.getPlanDate() != null
                        && !p.getPlanDate().isBefore(start) && !p.getPlanDate().isAfter(end))
                .filter(p -> {
                    if (RectificationTypeUtil.SCOPE_DEPT.equals(request.getScopeType())) {
                        return request.getDeptName().equals(p.getDeptName());
                    }
                    if (RectificationTypeUtil.SCOPE_TYPE.equals(request.getScopeType())) {
                        return request.getInspectType().equals(p.getInspectType());
                    }
                    return true;
                })
                .collect(Collectors.toList());
        List<QualityRecord> monthRecords = records.stream()
                .filter(rec -> rec.getCheckDate() != null
                        && !rec.getCheckDate().isBefore(start) && !rec.getCheckDate().isAfter(end))
                .filter(rec -> {
                    if (RectificationTypeUtil.SCOPE_DEPT.equals(request.getScopeType())) {
                        return request.getDeptName().equals(rec.getDeptName());
                    }
                    if (RectificationTypeUtil.SCOPE_TYPE.equals(request.getScopeType())) {
                        return request.getInspectType().equals(rec.getInspectType());
                    }
                    return true;
                })
                .collect(Collectors.toList());

        Map<String, Object> agg = aggregate(monthIssues, rectsOf(monthIssues, rects));
        RectificationSummary summary = findExisting(request);
        if (summary == null) {
            summary = new RectificationSummary();
            summary.setStatPeriod(request.getStatPeriod());
            summary.setScopeType(request.getScopeType());
            summary.setDeptName(RectificationTypeUtil.SCOPE_DEPT.equals(request.getScopeType())
                    ? request.getDeptName() : null);
            summary.setInspectType(RectificationTypeUtil.SCOPE_TYPE.equals(request.getScopeType())
                    ? request.getInspectType() : null);
        }
        summary.setPlanCount(monthPlans.size());
        summary.setRecordCount(monthRecords.size());
        summary.setAvgScore(QualityTypeUtil.avgScore(
                monthRecords.stream().map(QualityRecord::getTotalScore).collect(Collectors.toList())));
        summary.setIssueCount(((Long) agg.get("issueCount")).intValue());
        summary.setMajorCount(((Long) ((Map<?, ?>) agg.get("bySeverity")).get(QualityTypeUtil.SEVERITY_MAJOR)).intValue());
        summary.setMinorCount(((Long) ((Map<?, ?>) agg.get("bySeverity")).get(QualityTypeUtil.SEVERITY_MINOR)).intValue());
        summary.setMildCount(((Long) ((Map<?, ?>) agg.get("bySeverity")).get(QualityTypeUtil.SEVERITY_MILD)).intValue());
        summary.setRectificationTotal(((Long) agg.get("rectificationTotal")).intValue());
        summary.setRectificationPending(((Long) agg.get("rectificationPending")).intValue());
        summary.setRectificationDone(((Long) agg.get("rectificationDone")).intValue());
        summary.setRectificationReviewed(((Long) agg.get("rectificationReviewed")).intValue());
        summary.setRectificationRate((BigDecimal) agg.get("rectificationRate"));
        summary.setOverdueCount(((Long) agg.get("overdueCount")).intValue());
        summary.setGeneratedBy(request.getGeneratedBy());
        summary.setRemark(request.getRemark());
        if (summary.getId() == null) {
            super.save(summary);
        } else {
            super.updateById(summary);
        }
        return summary;
    }

    @Override
    public void exportExcel(RectificationSummaryQuery query, OutputStream out) {
        RectificationSummaryExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<RectificationSummary> list = RectificationSummaryExcelUtil.read(in);
        int ok = 0;
        for (RectificationSummary s : list) {
            if (!StringUtils.hasText(s.getStatPeriod())
                    || !RectificationTypeUtil.isValidPeriod(s.getStatPeriod())) {
                log.warn("导入跳过：统计周期为空或格式不正确（{}）", s.getStatPeriod());
                continue;
            }
            if (!StringUtils.hasText(s.getScopeType())
                    || !RectificationTypeUtil.isValidScope(s.getScopeType())) {
                log.warn("导入跳过：汇总范围为空或不正确（{}）", s.getStatPeriod());
                continue;
            }
            // 同一周期同一范围同一科室/类型视为重复，跳过
            if (findExisting(s.getStatPeriod(), s.getScopeType(), s.getDeptName(), s.getInspectType()) != null) {
                log.warn("导入跳过：重复汇总报告 {} {}", s.getStatPeriod(),
                        RectificationTypeUtil.scopeText(s.getScopeType()));
                continue;
            }
            try {
                this.save(s);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", s.getStatPeriod(), e.getMessage());
            }
        }
        return ok;
    }

    /** 新建时补默认值并校验；更新时只校验（汇总报告可直接修改数字） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(RectificationSummary entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(RectificationSummary entity) {
        RectificationSummary old = getOrThrow(entity.getId());
        // 汇总口径（周期/范围/科室/类型）不可改，改口径请重新生成；
        // 其余字段按传入的非空值合并，避免部分更新把未传的计数归零
        if (entity.getPlanCount() != null) {
            old.setPlanCount(entity.getPlanCount());
        }
        if (entity.getRecordCount() != null) {
            old.setRecordCount(entity.getRecordCount());
        }
        if (entity.getAvgScore() != null) {
            old.setAvgScore(entity.getAvgScore());
        }
        if (entity.getIssueCount() != null) {
            old.setIssueCount(entity.getIssueCount());
        }
        if (entity.getMajorCount() != null) {
            old.setMajorCount(entity.getMajorCount());
        }
        if (entity.getMinorCount() != null) {
            old.setMinorCount(entity.getMinorCount());
        }
        if (entity.getMildCount() != null) {
            old.setMildCount(entity.getMildCount());
        }
        if (entity.getRectificationTotal() != null) {
            old.setRectificationTotal(entity.getRectificationTotal());
        }
        if (entity.getRectificationPending() != null) {
            old.setRectificationPending(entity.getRectificationPending());
        }
        if (entity.getRectificationDone() != null) {
            old.setRectificationDone(entity.getRectificationDone());
        }
        if (entity.getRectificationReviewed() != null) {
            old.setRectificationReviewed(entity.getRectificationReviewed());
        }
        if (entity.getOverdueCount() != null) {
            old.setOverdueCount(entity.getOverdueCount());
        }
        if (entity.getGeneratedBy() != null) {
            old.setGeneratedBy(entity.getGeneratedBy());
        }
        if (entity.getRemark() != null) {
            old.setRemark(entity.getRemark());
        }
        normalize(old, false);
        return super.updateById(old);
    }

    private void normalize(RectificationSummary s, boolean isCreate) {
        if (!StringUtils.hasText(s.getStatPeriod())
                || !RectificationTypeUtil.isValidPeriod(s.getStatPeriod())) {
            throw new IllegalArgumentException("统计周期不正确，应为 yyyy-MM 格式，如 2026-11");
        }
        if (!StringUtils.hasText(s.getScopeType())
                || !RectificationTypeUtil.isValidScope(s.getScopeType())) {
            throw new IllegalArgumentException("汇总范围不正确");
        }
        if (RectificationTypeUtil.SCOPE_DEPT.equals(s.getScopeType())
                && !StringUtils.hasText(s.getDeptName())) {
            throw new IllegalArgumentException("按科室汇总时科室名称不能为空");
        }
        if (RectificationTypeUtil.SCOPE_TYPE.equals(s.getScopeType())
                && (!StringUtils.hasText(s.getInspectType())
                || !QualityTypeUtil.isValidType(s.getInspectType()))) {
            throw new IllegalArgumentException("按检查类型汇总时检查类型不正确");
        }
        if (RectificationTypeUtil.SCOPE_ALL.equals(s.getScopeType())) {
            s.setDeptName(null);
            s.setInspectType(null);
        }
        // 各项计数不能为负；空值按 0 处理
        s.setPlanCount(nonNegative(s.getPlanCount(), "检查计划数"));
        s.setRecordCount(nonNegative(s.getRecordCount(), "检查记录数"));
        s.setIssueCount(nonNegative(s.getIssueCount(), "检查问题数"));
        s.setMajorCount(nonNegative(s.getMajorCount(), "严重问题数"));
        s.setMinorCount(nonNegative(s.getMinorCount(), "一般问题数"));
        s.setMildCount(nonNegative(s.getMildCount(), "轻微问题数"));
        s.setRectificationTotal(nonNegative(s.getRectificationTotal(), "整改任务数"));
        s.setRectificationPending(nonNegative(s.getRectificationPending(), "待整改任务数"));
        s.setRectificationDone(nonNegative(s.getRectificationDone(), "已整改任务数"));
        s.setRectificationReviewed(nonNegative(s.getRectificationReviewed(), "已复查通过任务数"));
        s.setOverdueCount(nonNegative(s.getOverdueCount(), "逾期整改数"));
        if (s.getAvgScore() == null) {
            s.setAvgScore(BigDecimal.ZERO);
        }
        if (s.getAvgScore().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("检查平均分不能为负数");
        }
        // 整改完成率始终按“已复查通过 / 整改任务总数”现算，保证口径一致
        s.setRectificationRate(RectificationTypeUtil.rectificationRate(
                s.getRectificationReviewed(), s.getRectificationTotal()));
        if (isCreate) {
            // 同一周期同一范围同一科室/类型已有记录，拒绝重复建档（请重新生成）
            if (findExisting(s.getStatPeriod(), s.getScopeType(), s.getDeptName(), s.getInspectType()) != null) {
                throw new IllegalArgumentException("该统计周期已有相同范围的汇总报告，请重新生成而不是新建");
            }
        }
    }

    private int nonNegative(Integer v, String name) {
        if (v == null) {
            return 0;
        }
        if (v < 0) {
            throw new IllegalArgumentException(name + "不能为负数");
        }
        return v;
    }

    private void validateGenerateRequest(GenerateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("生成请求不能为空");
        }
        if (!StringUtils.hasText(request.getStatPeriod())
                || !RectificationTypeUtil.isValidPeriod(request.getStatPeriod())) {
            throw new IllegalArgumentException("统计周期不正确，应为 yyyy-MM 格式，如 2026-11");
        }
        if (!StringUtils.hasText(request.getScopeType())
                || !RectificationTypeUtil.isValidScope(request.getScopeType())) {
            throw new IllegalArgumentException("汇总范围不正确");
        }
        if (RectificationTypeUtil.SCOPE_DEPT.equals(request.getScopeType())
                && !StringUtils.hasText(request.getDeptName())) {
            throw new IllegalArgumentException("按科室汇总时科室名称不能为空");
        }
        if (RectificationTypeUtil.SCOPE_TYPE.equals(request.getScopeType())
                && (!StringUtils.hasText(request.getInspectType())
                || !QualityTypeUtil.isValidType(request.getInspectType()))) {
            throw new IllegalArgumentException("按检查类型汇总时检查类型不正确");
        }
    }

    private RectificationSummary findExisting(GenerateRequest request) {
        return findExisting(request.getStatPeriod(), request.getScopeType(),
                RectificationTypeUtil.SCOPE_DEPT.equals(request.getScopeType()) ? request.getDeptName() : null,
                RectificationTypeUtil.SCOPE_TYPE.equals(request.getScopeType()) ? request.getInspectType() : null);
    }

    private RectificationSummary findExisting(String statPeriod, String scopeType,
                                              String deptName, String inspectType) {
        LambdaQueryWrapper<RectificationSummary> qw = new LambdaQueryWrapper<RectificationSummary>()
                .eq(RectificationSummary::getStatPeriod, statPeriod)
                .eq(RectificationSummary::getScopeType, scopeType);
        if (deptName == null) {
            qw.isNull(RectificationSummary::getDeptName);
        } else {
            qw.eq(RectificationSummary::getDeptName, deptName);
        }
        if (inspectType == null) {
            qw.isNull(RectificationSummary::getInspectType);
        } else {
            qw.eq(RectificationSummary::getInspectType, inspectType);
        }
        return this.getOne(qw);
    }

    /**
     * 对一组问题 + 其整改任务做聚合：问题总数、按严重程度分组、
     * 整改任务数、待整改/已整改/已复查通过数、整改完成率、逾期数。
     * rects 参数应已限定为这些问题的整改任务（见 rectsOf）。
     */
    private Map<String, Object> aggregate(List<QualityIssue> issues, List<QualityRectification> rects) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("issueCount", (long) issues.size());
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (String s : List.of(QualityTypeUtil.SEVERITY_MAJOR, QualityTypeUtil.SEVERITY_MINOR,
                QualityTypeUtil.SEVERITY_MILD)) {
            bySeverity.put(s, issues.stream().filter(i -> s.equals(i.getSeverity())).count());
        }
        r.put("bySeverity", bySeverity);
        r.put("rectificationTotal", (long) rects.size());
        long pending = rects.stream()
                .filter(x -> Integer.valueOf(QualityTypeUtil.RECTIFY_PENDING).equals(x.getStatus())).count();
        long done = rects.stream()
                .filter(x -> Integer.valueOf(QualityTypeUtil.RECTIFY_DONE).equals(x.getStatus())).count();
        long reviewed = rects.stream()
                .filter(x -> Integer.valueOf(QualityTypeUtil.RECTIFY_REVIEWED).equals(x.getStatus())).count();
        r.put("rectificationPending", pending);
        r.put("rectificationDone", done);
        r.put("rectificationReviewed", reviewed);
        r.put("rectificationRate", RectificationTypeUtil.rectificationRate(reviewed, rects.size()));
        r.put("overdueCount", rects.stream()
                .filter(x -> QualityTypeUtil.isOverdue(x.getStatus(), x.getDeadline())).count());
        return r;
    }

    /** 取属于给定问题集合的整改任务 */
    private List<QualityRectification> rectsOf(List<QualityIssue> issues, List<QualityRectification> all) {
        List<Long> ids = issues.stream().map(QualityIssue::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return List.of();
        }
        return all.stream().filter(r -> ids.contains(r.getIssueId())).collect(Collectors.toList());
    }

    /** 逾期整改任务行：整改任务 + 问题描述/严重程度/责任科室 + 检查类型 */
    private Map<String, Object> rectificationRow(QualityRectification r, QualityIssue issue, QualityPlan plan) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getId());
        row.put("issueId", r.getIssueId());
        row.put("planId", r.getPlanId());
        row.put("recordId", r.getRecordId());
        row.put("measure", r.getMeasure());
        row.put("deadline", r.getDeadline());
        row.put("respPerson", r.getRespPerson());
        row.put("status", r.getStatus());
        row.put("statusText", QualityTypeUtil.rectifyStatusText(r.getStatus()));
        row.put("rectifyDate", r.getRectifyDate());
        row.put("reviewer", r.getReviewer());
        row.put("reviewDate", r.getReviewDate());
        row.put("overdue", true);
        if (issue != null) {
            row.put("issueDescription", issue.getDescription());
            row.put("severity", issue.getSeverity());
            row.put("severityText", QualityTypeUtil.severityText(issue.getSeverity()));
            row.put("deptName", issue.getRespDeptName());
            row.put("respDeptName", issue.getRespDeptName());
            row.put("foundDate", issue.getFoundDate());
        }
        if (plan != null) {
            row.put("inspectType", plan.getInspectType());
            row.put("inspectTypeText", QualityTypeUtil.typeText(plan.getInspectType()));
            row.put("templateName", plan.getTemplateName());
        }
        return row;
    }

    /** 检查问题汇总行：问题 + 其整改任务状态/期限/逾期标记 + 检查类型 */
    private Map<String, Object> issueRow(QualityIssue issue, QualityRectification rect, QualityPlan plan) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", issue.getId());
        row.put("recordId", issue.getRecordId());
        row.put("planId", issue.getPlanId());
        row.put("description", issue.getDescription());
        row.put("severity", issue.getSeverity());
        row.put("severityText", QualityTypeUtil.severityText(issue.getSeverity()));
        row.put("respDeptName", issue.getRespDeptName());
        row.put("deptName", issue.getRespDeptName());
        row.put("respPerson", issue.getRespPerson());
        row.put("foundDate", issue.getFoundDate());
        if (plan != null) {
            row.put("inspectType", plan.getInspectType());
            row.put("inspectTypeText", QualityTypeUtil.typeText(plan.getInspectType()));
            row.put("templateName", plan.getTemplateName());
        }
        if (rect != null) {
            row.put("rectificationId", rect.getId());
            row.put("rectificationStatus", rect.getStatus());
            row.put("rectificationStatusText", QualityTypeUtil.rectifyStatusText(rect.getStatus()));
            row.put("measure", rect.getMeasure());
            row.put("deadline", rect.getDeadline());
            row.put("rectifyDate", rect.getRectifyDate());
            row.put("overdue", QualityTypeUtil.isOverdue(rect.getStatus(), rect.getDeadline()));
        } else {
            row.put("rectificationId", null);
            row.put("rectificationStatus", null);
            row.put("rectificationStatusText", "未发整改任务");
            row.put("overdue", false);
        }
        return row;
    }

    /** 内存分页（统计行已在内存里聚合好，按页切片） */
    private PageResult<Map<String, Object>> paginate(List<Map<String, Object>> rows, long page, long size) {
        long total = rows.size();
        long from = Math.min((page - 1) * size, total);
        long to = Math.min(from + size, total);
        return PageResult.of(total, page, size, rows.subList((int) from, (int) to));
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<RectificationSummary> buildWrapper(RectificationSummaryQuery q) {
        LambdaQueryWrapper<RectificationSummary> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.hasText(q.getStatPeriod()), RectificationSummary::getStatPeriod, q.getStatPeriod());
        qw.eq(StringUtils.hasText(q.getScopeType()), RectificationSummary::getScopeType, q.getScopeType());
        qw.like(StringUtils.hasText(q.getDeptName()), RectificationSummary::getDeptName, q.getDeptName());
        qw.eq(StringUtils.hasText(q.getInspectType()), RectificationSummary::getInspectType, q.getInspectType());
        qw.orderByDesc(RectificationSummary::getStatPeriod)
                .orderByAsc(RectificationSummary::getScopeType)
                .orderByAsc(RectificationSummary::getDeptName)
                .orderByAsc(RectificationSummary::getInspectType)
                .orderByDesc(RectificationSummary::getId);
        return qw;
    }

    private void addDept(List<String> depts, String dept) {
        if (StringUtils.hasText(dept) && !depts.contains(dept)) {
            depts.add(dept);
        }
    }

    private RectificationSummary getOrThrow(Long id) {
        RectificationSummary s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("检查整改汇总报告不存在：" + id);
        }
        return s;
    }
}
