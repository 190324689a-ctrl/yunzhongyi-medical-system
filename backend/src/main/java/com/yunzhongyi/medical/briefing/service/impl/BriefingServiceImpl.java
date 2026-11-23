package com.yunzhongyi.medical.briefing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.assessment.entity.Assessment;
import com.yunzhongyi.medical.assessment.mapper.AssessmentMapper;
import com.yunzhongyi.medical.assessment.service.AssessmentGradeUtil;
import com.yunzhongyi.medical.briefing.dto.BriefingQuery;
import com.yunzhongyi.medical.briefing.dto.GenerateBriefingRequest;
import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.briefing.entity.BriefingTemplate;
import com.yunzhongyi.medical.briefing.excel.BriefingExcelUtil;
import com.yunzhongyi.medical.briefing.export.BriefingPdfUtil;
import com.yunzhongyi.medical.briefing.export.BriefingWordUtil;
import com.yunzhongyi.medical.briefing.mapper.BriefingMapper;
import com.yunzhongyi.medical.briefing.mapper.BriefingTemplateMapper;
import com.yunzhongyi.medical.briefing.service.BriefingService;
import com.yunzhongyi.medical.briefing.service.BriefingTypeUtil;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import com.yunzhongyi.medical.mdt.mapper.MdtApplicationMapper;
import com.yunzhongyi.medical.mdt.service.MdtTypeUtil;
import com.yunzhongyi.medical.newtech.entity.NewTechProject;
import com.yunzhongyi.medical.newtech.mapper.NewTechProjectMapper;
import com.yunzhongyi.medical.newtech.service.NewTechTypeUtil;
import com.yunzhongyi.medical.physician.mapper.PhysicianMapper;
import com.yunzhongyi.medical.quality.entity.QualityIssue;
import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityRecord;
import com.yunzhongyi.medical.quality.mapper.QualityIssueMapper;
import com.yunzhongyi.medical.quality.mapper.QualityPlanMapper;
import com.yunzhongyi.medical.quality.mapper.QualityRecordMapper;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import com.yunzhongyi.medical.statistics.entity.WorkloadRecord;
import com.yunzhongyi.medical.statistics.mapper.WorkloadRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 医疗工作简报服务实现。
 * <p>
 * 关键守卫：期号全院唯一、统计周期须为 yyyy-MM、分类须合法；只有草稿与
 * 已驳回的简报能修改；已发布的简报不可修改、不可直接删除；提交、审批、
 * 驳回只允许在对应状态下流转，已结束的简报（已发布/已驳回）不能再审批。
 * 自动生成：按模板栏目从医师档案、工作量、质量检查、MDT、新技术、考核
 * 六个模块聚合本期真实数据写正文，生成结果为草稿，须经提交审批后发布。
 */
@Service
@RequiredArgsConstructor
public class BriefingServiceImpl
        extends ServiceImpl<BriefingMapper, Briefing>
        implements BriefingService {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final BriefingTemplateMapper briefingTemplateMapper;
    private final PhysicianMapper physicianMapper;
    private final WorkloadRecordMapper workloadRecordMapper;
    private final QualityPlanMapper qualityPlanMapper;
    private final QualityIssueMapper qualityIssueMapper;
    private final QualityRecordMapper qualityRecordMapper;
    private final MdtApplicationMapper mdtApplicationMapper;
    private final NewTechProjectMapper newTechProjectMapper;
    private final AssessmentMapper assessmentMapper;

    @Override
    public PageResult<Briefing> pageQuery(BriefingQuery query) {
        Page<Briefing> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public Map<String, Object> stats() {
        List<Briefing> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("draft", countByStatus(all, BriefingTypeUtil.STATUS_DRAFT));
        r.put("pending", countByStatus(all, BriefingTypeUtil.STATUS_PENDING));
        r.put("published", countByStatus(all, BriefingTypeUtil.STATUS_PUBLISHED));
        r.put("rejected", countByStatus(all, BriefingTypeUtil.STATUS_REJECTED));
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String c : BriefingTypeUtil.CATEGORIES) {
            byCategory.put(c, all.stream().filter(b -> c.equals(b.getCategory())).count());
        }
        r.put("byCategory", byCategory);
        r.put("templateTotal", briefingTemplateMapper.selectCount(null));
        r.put("templateEnabled", briefingTemplateMapper.selectCount(
                new LambdaQueryWrapper<BriefingTemplate>().eq(BriefingTemplate::getStatus, 1)));
        return r;
    }

    @Override
    public Map<String, Object> statsByCategory() {
        List<Briefing> all = this.list();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String c : BriefingTypeUtil.CATEGORIES) {
            List<Briefing> group = all.stream().filter(b -> c.equals(b.getCategory())).toList();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", c);
            row.put("categoryText", BriefingTypeUtil.categoryText(c));
            row.put("total", (long) group.size());
            row.put("published", group.stream()
                    .filter(b -> Integer.valueOf(BriefingTypeUtil.STATUS_PUBLISHED).equals(b.getStatus())).count());
            rows.add(row);
        }
        return Map.of("byCategory", rows);
    }

    @Override
    public Map<String, Object> statsByMonth() {
        List<Briefing> all = this.list();
        Map<String, List<Briefing>> grouped = new LinkedHashMap<>();
        for (Briefing b : all) {
            grouped.computeIfAbsent(b.getPeriod() == null ? "" : b.getPeriod(),
                    k -> new ArrayList<>()).add(b);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        grouped.keySet().stream().sorted().forEach(period -> {
            List<Briefing> group = grouped.get(period);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("period", period);
            row.put("total", (long) group.size());
            row.put("published", group.stream()
                    .filter(b -> Integer.valueOf(BriefingTypeUtil.STATUS_PUBLISHED).equals(b.getStatus())).count());
            rows.add(row);
        });
        return Map.of("byMonth", rows);
    }

    @Override
    public Briefing detail(Long id) {
        Briefing b = this.getById(id);
        if (b == null) {
            throw new IllegalArgumentException("医疗工作简报不存在：" + id);
        }
        return b;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Briefing create(Briefing briefing) {
        validateBriefing(briefing);
        applyDefaults(briefing);
        checkIssueNoUnique(briefing.getIssueNo(), null);
        briefing.setStatus(BriefingTypeUtil.STATUS_DRAFT);
        briefing.setPublishDate(null);
        this.save(briefing);
        return briefing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Briefing update(Briefing briefing) {
        Briefing old = detail(briefing.getId());
        if (!Integer.valueOf(BriefingTypeUtil.STATUS_DRAFT).equals(old.getStatus())
                && !Integer.valueOf(BriefingTypeUtil.STATUS_REJECTED).equals(old.getStatus())) {
            throw new IllegalArgumentException("只有草稿与已驳回的简报能修改");
        }
        validateBriefing(briefing);
        applyDefaults(briefing);
        checkIssueNoUnique(briefing.getIssueNo(), briefing.getId());
        briefing.setStatus(BriefingTypeUtil.STATUS_DRAFT);
        briefing.setPublishDate(old.getPublishDate());
        briefing.setApprover(old.getApprover());
        briefing.setApprovalOpinion(old.getApprovalOpinion());
        briefing.setApprovalHistory(old.getApprovalHistory());
        briefing.setApproveTime(old.getApproveTime());
        briefing.setRejectReason(null);
        this.updateById(briefing);
        return briefing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Briefing old = detail(id);
        if (Integer.valueOf(BriefingTypeUtil.STATUS_PUBLISHED).equals(old.getStatus())) {
            throw new IllegalArgumentException("已发布的简报不能直接删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Briefing submit(Long id) {
        Briefing b = detail(id);
        if (!Integer.valueOf(BriefingTypeUtil.STATUS_DRAFT).equals(b.getStatus())
                && !Integer.valueOf(BriefingTypeUtil.STATUS_REJECTED).equals(b.getStatus())) {
            throw new IllegalArgumentException("只有草稿与已驳回的简报能提交审批");
        }
        b.setStatus(BriefingTypeUtil.STATUS_PENDING);
        b.setRejectReason(null);
        this.updateById(b);
        return b;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Briefing approve(Long id, String approver, String opinion) {
        Briefing b = detail(id);
        if (!Integer.valueOf(BriefingTypeUtil.STATUS_PENDING).equals(b.getStatus())) {
            throw new IllegalArgumentException("只有待审批的简报能审批通过");
        }
        if (!StringUtils.hasText(approver)) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        b.setStatus(BriefingTypeUtil.STATUS_PUBLISHED);
        b.setApprover(approver.trim());
        b.setApprovalOpinion(opinion);
        b.setApproveTime(LocalDateTime.now());
        b.setPublishDate(LocalDate.now());
        b.setRejectReason(null);
        appendApprovalHistory(b, approver.trim(), "通过（" + (StringUtils.hasText(opinion) ? opinion.trim() : "同意发布") + "）");
        this.updateById(b);
        return b;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Briefing reject(Long id, String approver, String reason) {
        Briefing b = detail(id);
        if (!Integer.valueOf(BriefingTypeUtil.STATUS_PENDING).equals(b.getStatus())) {
            throw new IllegalArgumentException("只有待审批的简报能驳回");
        }
        if (!StringUtils.hasText(approver)) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        b.setStatus(BriefingTypeUtil.STATUS_REJECTED);
        b.setApprover(approver.trim());
        b.setRejectReason(reason.trim());
        b.setApprovalOpinion(reason.trim());
        b.setApproveTime(LocalDateTime.now());
        appendApprovalHistory(b, approver.trim(), "驳回（" + reason.trim() + "）");
        this.updateById(b);
        return b;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Briefing generate(GenerateBriefingRequest request) {
        BriefingTemplate template = briefingTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new IllegalArgumentException("简报模板不存在：" + request.getTemplateId());
        }
        if (!Integer.valueOf(1).equals(template.getStatus())) {
            throw new IllegalArgumentException("模板已停用，不能用来自动生成简报");
        }
        if (!StringUtils.hasText(request.getPeriod())
                || !PERIOD_PATTERN.matcher(request.getPeriod().trim()).matches()) {
            throw new IllegalArgumentException("统计周期须为 yyyy-MM 格式，如 2026-11");
        }
        String period = request.getPeriod().trim();
        String issueNo = StringUtils.hasText(request.getIssueNo())
                ? request.getIssueNo().trim() : nextIssueNo(period);
        checkIssueNoUnique(issueNo, null);

        Briefing briefing = new Briefing();
        briefing.setIssueNo(issueNo);
        briefing.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim()
                : template.getName() + "（" + period + "）");
        briefing.setTemplateId(template.getId());
        briefing.setTemplateName(template.getName());
        briefing.setCategory(template.getCategory());
        briefing.setPeriod(period);
        briefing.setAuthorName(request.getAuthorName());
        briefing.setContent(buildContent(template, period));
        briefing.setStatus(BriefingTypeUtil.STATUS_DRAFT);
        this.save(briefing);
        return briefing;
    }

    @Override
    public void exportWord(Long id, OutputStream out) {
        BriefingWordUtil.write(detail(id), out);
    }

    @Override
    public void exportPdf(Long id, OutputStream out) {
        BriefingPdfUtil.write(detail(id), out);
    }

    @Override
    public void downloadText(Long id, OutputStream out) {
        try {
            out.write(BriefingWordUtil.toPlainText(detail(id)).getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException("下载正文失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void exportExcel(BriefingQuery query, OutputStream out) {
        BriefingExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<Briefing> rows = BriefingExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (Briefing row : rows) {
            try {
                validateBriefing(row);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<Briefing>()
                    .eq(Briefing::getIssueNo, row.getIssueNo().trim()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            applyDefaults(row);
            row.setIssueNo(row.getIssueNo().trim());
            row.setStatus(BriefingTypeUtil.STATUS_DRAFT);
            row.setPublishDate(null);
            row.setApprover(null);
            row.setApprovalOpinion(null);
            row.setApprovalHistory(null);
            row.setApproveTime(null);
            row.setRejectReason(null);
            this.save(row);
            created++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", rows.size());
        return result;
    }

    @Override
    public void writeImportTemplate(OutputStream out) {
        BriefingExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private long countByStatus(List<Briefing> all, int status) {
        return all.stream().filter(b -> Integer.valueOf(status).equals(b.getStatus())).count();
    }

    private LambdaQueryWrapper<Briefing> buildWrapper(BriefingQuery query) {
        LambdaQueryWrapper<Briefing> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(Briefing::getIssueNo, kw)
                    .or().like(Briefing::getTitle, kw)
                    .or().like(Briefing::getAuthorName, kw));
        }
        if (StringUtils.hasText(query.getCategory())) {
            w.eq(Briefing::getCategory, query.getCategory());
        }
        if (query.getStatus() != null) {
            w.eq(Briefing::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getPeriod())) {
            w.eq(Briefing::getPeriod, query.getPeriod().trim());
        }
        if (query.getTemplateId() != null) {
            w.eq(Briefing::getTemplateId, query.getTemplateId());
        }
        if (Boolean.TRUE.equals(query.getPublishedOnly())) {
            w.eq(Briefing::getStatus, BriefingTypeUtil.STATUS_PUBLISHED);
        }
        w.orderByDesc(Briefing::getPeriod).orderByDesc(Briefing::getIssueNo).orderByDesc(Briefing::getId);
        return w;
    }

    private void applyDefaults(Briefing b) {
        if (b.getCategory() == null) {
            b.setCategory(BriefingTypeUtil.CATEGORY_MONTHLY);
        }
        if (b.getTemplateId() != null && !StringUtils.hasText(b.getTemplateName())) {
            BriefingTemplate t = briefingTemplateMapper.selectById(b.getTemplateId());
            if (t != null) {
                b.setTemplateName(t.getName());
                if (b.getCategory() == null) {
                    b.setCategory(t.getCategory());
                }
            }
        }
    }

    private void validateBriefing(Briefing b) {
        if (!StringUtils.hasText(b.getIssueNo())) {
            throw new IllegalArgumentException("期号不能为空");
        }
        if (!StringUtils.hasText(b.getTitle())) {
            throw new IllegalArgumentException("简报标题不能为空");
        }
        if (b.getCategory() != null && !BriefingTypeUtil.CATEGORIES.contains(b.getCategory())) {
            throw new IllegalArgumentException("简报分类不合法：" + b.getCategory());
        }
        if (b.getPeriod() != null && !b.getPeriod().isBlank()
                && !PERIOD_PATTERN.matcher(b.getPeriod().trim()).matches()) {
            throw new IllegalArgumentException("统计周期须为 yyyy-MM 格式，如 2026-11");
        }
    }

    private void checkIssueNoUnique(String issueNo, Long excludeId) {
        LambdaQueryWrapper<Briefing> w = new LambdaQueryWrapper<Briefing>()
                .eq(Briefing::getIssueNo, issueNo.trim());
        if (excludeId != null) {
            w.ne(Briefing::getId, excludeId);
        }
        if (this.count(w) > 0) {
            throw new IllegalArgumentException("期号已存在：" + issueNo.trim());
        }
    }

    /** 期号自动生成：{统计周期}-序号（同一周期内按已有期数顺延） */
    private String nextIssueNo(String period) {
        long count = this.count(new LambdaQueryWrapper<Briefing>()
                .eq(Briefing::getPeriod, period));
        for (int seq = (int) count + 1; seq < 100; seq++) {
            String candidate = period + "-" + String.format("%02d", seq);
            if (this.count(new LambdaQueryWrapper<Briefing>().eq(Briefing::getIssueNo, candidate)) == 0) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("该统计周期期号已用完");
    }

    private void appendApprovalHistory(Briefing b, String approver, String text) {
        String line = "医务科 " + approver + "：" + text;
        b.setApprovalHistory(StringUtils.hasText(b.getApprovalHistory())
                ? b.getApprovalHistory() + "\n" + line : line);
    }

    // ---------- 自动生成正文 ----------

    /** 按模板栏目逐段聚合本期真实数据；认不出的栏目如实写“暂无自动汇总数据” */
    private String buildContent(BriefingTemplate template, String period) {
        List<String> sections = BriefingTemplateServiceImpl.parseSections(template.getSections());
        LocalDate start = LocalDate.parse(period + "-01");
        LocalDate end = start.plusMonths(1).minusDays(1);
        StringBuilder sb = new StringBuilder();
        for (String section : sections) {
            sb.append(section.trim()).append("：\n");
            sb.append(buildSection(section.trim(), period, start, end)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String buildSection(String section, String period, LocalDate start, LocalDate end) {
        if (section.contains("概况") || section.contains("工作量") || section.contains("业务")) {
            return buildWorkloadSection(period);
        }
        if (section.contains("质量")) {
            return buildQualitySection(start, end);
        }
        if (section.contains("会诊") || section.contains("新技术") || section.contains("重点")) {
            return buildMdtNewTechSection(start, end);
        }
        if (section.contains("考核") || section.contains("人员")) {
            return buildAssessmentSection(period);
        }
        if (section.contains("计划")) {
            return "下月重点工作请结合本期情况手工补充。";
        }
        return "本期该栏目暂无自动汇总数据，请手工补充。";
    }

    private String buildWorkloadSection(String period) {
        long physicianCount = physicianMapper.selectCount(null);
        List<WorkloadRecord> records = workloadRecordMapper.selectList(
                new LambdaQueryWrapper<WorkloadRecord>().eq(WorkloadRecord::getStatMonth, period));
        long outpatient = records.stream().mapToLong(r -> r.getOutpatientCount() == null ? 0 : r.getOutpatientCount()).sum();
        long inpatient = records.stream().mapToLong(r -> r.getInpatientCount() == null ? 0 : r.getInpatientCount()).sum();
        long surgery = records.stream().mapToLong(r -> r.getSurgeryCount() == null ? 0 : r.getSurgeryCount()).sum();
        return "全院在册医师 " + physicianCount + " 人；本期工作量登记 " + records.size() + " 条，"
                + "门诊 " + outpatient + " 人次、住院 " + inpatient + " 人次、手术 " + surgery + " 台。";
    }

    private String buildQualitySection(LocalDate start, LocalDate end) {
        List<QualityPlan> plans = qualityPlanMapper.selectList(
                new LambdaQueryWrapper<QualityPlan>()
                        .ge(QualityPlan::getPlanDate, start)
                        .le(QualityPlan::getPlanDate, end));
        long completed = plans.stream().filter(p -> Integer.valueOf(QualityTypeUtil.PLAN_COMPLETED).equals(p.getStatus())).count();
        long inProgress = plans.stream().filter(p -> Integer.valueOf(QualityTypeUtil.PLAN_IN_PROGRESS).equals(p.getStatus())).count();
        List<QualityRecord> records = qualityRecordMapper.selectList(
                new LambdaQueryWrapper<QualityRecord>()
                        .ge(QualityRecord::getCheckDate, start)
                        .le(QualityRecord::getCheckDate, end));
        String avg = "暂无";
        if (!records.isEmpty()) {
            BigDecimal sum = records.stream()
                    .map(r -> r.getTotalScore() == null ? BigDecimal.ZERO : r.getTotalScore())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avg = sum.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP).toString() + " 分";
        }
        List<QualityIssue> issues = qualityIssueMapper.selectList(
                new LambdaQueryWrapper<QualityIssue>()
                        .ge(QualityIssue::getFoundDate, start)
                        .le(QualityIssue::getFoundDate, end));
        long major = issues.stream().filter(i -> QualityTypeUtil.SEVERITY_MAJOR.equals(i.getSeverity())).count();
        return "本期检查计划 " + plans.size() + " 个（已完成 " + completed + " 个、进行中 " + inProgress + " 个），"
                + "检查记录 " + records.size() + " 条、平均分 " + avg + "；"
                + "发现问题 " + issues.size() + " 个，其中严重问题 " + major + " 个。";
    }

    private String buildMdtNewTechSection(LocalDate start, LocalDate end) {
        List<MdtApplication> mdts = mdtApplicationMapper.selectList(
                new LambdaQueryWrapper<MdtApplication>()
                        .ge(MdtApplication::getApplyDate, start)
                        .le(MdtApplication::getApplyDate, end));
        long mdtApproved = mdts.stream().filter(m -> Integer.valueOf(MdtTypeUtil.STATUS_APPROVED).equals(m.getStatus())).count();
        long mdtUrgent = mdts.stream().filter(m -> MdtTypeUtil.URGENCY_URGENT.equals(m.getUrgency())).count();
        List<NewTechProject> projects = newTechProjectMapper.selectList(
                new LambdaQueryWrapper<NewTechProject>()
                        .ge(NewTechProject::getApplyDate, start)
                        .le(NewTechProject::getApplyDate, end));
        long approved = projects.stream().filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_APPROVED).equals(p.getStatus())).count();
        long promoted = projects.stream().filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_PROMOTED).equals(p.getStatus())).count();
        return "本期 MDT 会诊申请 " + mdts.size() + " 例（已通过 " + mdtApproved + " 例，其中紧急会诊 " + mdtUrgent + " 例）；"
                + "新技术、新项目申报 " + projects.size() + " 项（已立项 " + approved + " 项、已推广 " + promoted + " 项）。";
    }

    private String buildAssessmentSection(String period) {
        int year = Integer.parseInt(period.substring(0, 4));
        List<Assessment> records = assessmentMapper.selectList(
                new LambdaQueryWrapper<Assessment>().eq(Assessment::getAssessYear, year));
        if (records.isEmpty()) {
            return "本年度暂无考核记录。";
        }
        BigDecimal sum = records.stream()
                .map(r -> r.getTotalScore() == null ? BigDecimal.ZERO : r.getTotalScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
        long excellent = records.stream().filter(r -> AssessmentGradeUtil.EXCELLENT.equals(r.getGrade())).count();
        return "本年度考核 " + records.size() + " 人次，平均分 " + avg + " 分，优秀 " + excellent + " 人。";
    }
}
