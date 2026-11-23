package com.yunzhongyi.medical.medgrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.medgrading.dto.GradingAssessmentQuery;
import com.yunzhongyi.medical.medgrading.entity.GradingAssessment;
import com.yunzhongyi.medical.medgrading.entity.GradingStandard;
import com.yunzhongyi.medical.medgrading.excel.GradingAssessmentExcelUtil;
import com.yunzhongyi.medical.medgrading.mapper.GradingAssessmentMapper;
import com.yunzhongyi.medical.medgrading.mapper.GradingStandardMapper;
import com.yunzhongyi.medical.medgrading.service.GradingAssessmentService;
import com.yunzhongyi.medical.medgrading.service.MedGradingTypeUtil;
import com.yunzhongyi.medical.system.entity.SysDept;
import com.yunzhongyi.medical.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 医疗分级评定服务实现。
 * <p>
 * 关键守卫：等级由评分按所属标准的分数线自动算，改评分时重算；同一科室 +
 * 同一标准 + 同一周期只允许一条评定；只有待审批的能修改 / 审批 / 删除，
 * 已通过的评定是科室当前分级的依据，不可直接删除。
 */
@Service
@RequiredArgsConstructor
public class GradingAssessmentServiceImpl
        extends ServiceImpl<GradingAssessmentMapper, GradingAssessment>
        implements GradingAssessmentService {

    private final GradingStandardMapper standardMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public PageResult<GradingAssessment> pageQuery(GradingAssessmentQuery query) {
        Page<GradingAssessment> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public GradingAssessment detail(Long id) {
        GradingAssessment a = this.getById(id);
        if (a == null) {
            throw new IllegalArgumentException("分级评定不存在：" + id);
        }
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradingAssessment create(GradingAssessment assessment) {
        GradingStandard standard = resolveStandard(assessment.getStandardId());
        validateAssessment(assessment, null);
        fillStandardInfo(assessment, standard);
        resolveDept(assessment);
        if (assessment.getAssessDate() == null) {
            assessment.setAssessDate(LocalDate.now());
        }
        assessment.setStatus(MedGradingTypeUtil.ASSESS_PENDING);
        assessment.setGrade(MedGradingTypeUtil.calcGrade(assessment.getScore(),
                standard.getExcellentMin(), standard.getGoodMin(), standard.getPassMin()));
        this.save(assessment);
        return assessment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradingAssessment update(GradingAssessment assessment) {
        GradingAssessment old = detail(assessment.getId());
        if (!Integer.valueOf(MedGradingTypeUtil.ASSESS_PENDING).equals(old.getStatus())) {
            throw new IllegalArgumentException("只有待审批的评定能修改");
        }
        GradingStandard standard = resolveStandard(assessment.getStandardId());
        validateAssessment(assessment, assessment.getId());
        fillStandardInfo(assessment, standard);
        resolveDept(assessment);
        assessment.setGrade(MedGradingTypeUtil.calcGrade(assessment.getScore(),
                standard.getExcellentMin(), standard.getGoodMin(), standard.getPassMin()));
        assessment.setStatus(MedGradingTypeUtil.ASSESS_PENDING);
        this.updateById(assessment);
        return assessment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradingAssessment approve(Long id, String approver, String opinion) {
        GradingAssessment a = detail(id);
        if (!Integer.valueOf(MedGradingTypeUtil.ASSESS_PENDING).equals(a.getStatus())) {
            throw new IllegalArgumentException("只有待审批的评定能审批通过");
        }
        if (!StringUtils.hasText(approver)) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        a.setStatus(MedGradingTypeUtil.ASSESS_APPROVED);
        a.setApprover(approver.trim());
        a.setApproveOpinion(opinion);
        a.setApproveDate(LocalDate.now());
        this.updateById(a);
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradingAssessment reject(Long id, String approver, String opinion) {
        GradingAssessment a = detail(id);
        if (!Integer.valueOf(MedGradingTypeUtil.ASSESS_PENDING).equals(a.getStatus())) {
            throw new IllegalArgumentException("只有待审批的评定能驳回");
        }
        if (!StringUtils.hasText(approver)) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        a.setStatus(MedGradingTypeUtil.ASSESS_REJECTED);
        a.setApprover(approver.trim());
        a.setApproveOpinion(opinion);
        a.setApproveDate(LocalDate.now());
        this.updateById(a);
        return a;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        GradingAssessment a = detail((Long) id);
        if (Integer.valueOf(MedGradingTypeUtil.ASSESS_APPROVED).equals(a.getStatus())) {
            throw new IllegalArgumentException("已通过的评定是科室当前分级的依据，不能直接删除");
        }
        return super.removeById(id);
    }

    @Override
    public Map<String, Object> stats() {
        List<GradingAssessment> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(a -> Integer.valueOf(MedGradingTypeUtil.ASSESS_PENDING).equals(a.getStatus())).count());
        r.put("approved", all.stream()
                .filter(a -> Integer.valueOf(MedGradingTypeUtil.ASSESS_APPROVED).equals(a.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(a -> Integer.valueOf(MedGradingTypeUtil.ASSESS_REJECTED).equals(a.getStatus())).count());
        Map<String, Long> byGrade = new LinkedHashMap<>();
        for (String g : List.of(MedGradingTypeUtil.GRADE_A, MedGradingTypeUtil.GRADE_B,
                MedGradingTypeUtil.GRADE_C, MedGradingTypeUtil.GRADE_D)) {
            byGrade.put(g, all.stream().filter(a -> g.equals(a.getGrade())).count());
        }
        r.put("byGrade", byGrade);
        r.put("avgScore", avgScore(all));
        long passed = byGrade.get(MedGradingTypeUtil.GRADE_A) + byGrade.get(MedGradingTypeUtil.GRADE_B)
                + byGrade.get(MedGradingTypeUtil.GRADE_C);
        BigDecimal passRate = all.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(passed).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP);
        r.put("passRate", passRate);
        r.put("standardsUsed", all.stream().map(GradingAssessment::getStandardId).distinct().count());
        r.put("deptsAssessed", all.stream().map(GradingAssessment::getDeptName).distinct().count());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        List<GradingAssessment> all = this.list();
        Map<String, List<GradingAssessment>> grouped = all.stream()
                .collect(Collectors.groupingBy(GradingAssessment::getDeptName,
                        LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<GradingAssessment>> e : grouped.entrySet()) {
            List<GradingAssessment> list = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", e.getKey());
            row.put("total", (long) list.size());
            row.put("approved", list.stream()
                    .filter(a -> Integer.valueOf(MedGradingTypeUtil.ASSESS_APPROVED).equals(a.getStatus())).count());
            row.put("avgScore", avgScore(list));
            // 最新已通过评定的等级即科室当前分级
            GradingAssessment latest = list.stream()
                    .filter(a -> Integer.valueOf(MedGradingTypeUtil.ASSESS_APPROVED).equals(a.getStatus()))
                    .sorted((x, y) -> {
                        int c = y.getPeriod().compareTo(x.getPeriod());
                        return c != 0 ? c : Long.compare(y.getId(), x.getId());
                    })
                    .findFirst().orElse(null);
            row.put("currentGrade", latest == null ? null : latest.getGrade());
            row.put("currentGradeText", latest == null ? "未评级"
                    : MedGradingTypeUtil.gradeText(latest.getGrade()));
            row.put("latestPeriod", latest == null ? null : latest.getPeriod());
            result.add(row);
        }
        result.sort((x, y) -> {
            int c = Long.compare((Long) y.get("total"), (Long) x.get("total"));
            return c != 0 ? c : String.valueOf(x.get("deptName")).compareTo(String.valueOf(y.get("deptName")));
        });
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        List<GradingAssessment> all = this.list();
        Map<String, List<GradingAssessment>> grouped = all.stream()
                .collect(Collectors.groupingBy(GradingAssessment::getPeriod,
                        LinkedHashMap::new, Collectors.toList()));
        List<String> periods = new ArrayList<>(grouped.keySet());
        periods.sort(String::compareTo);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String period : periods) {
            List<GradingAssessment> list = grouped.get(period);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("period", period);
            row.put("total", (long) list.size());
            row.put("approved", list.stream()
                    .filter(a -> Integer.valueOf(MedGradingTypeUtil.ASSESS_APPROVED).equals(a.getStatus())).count());
            row.put("avgScore", avgScore(list));
            result.add(row);
        }
        return result;
    }

    @Override
    public void exportExcel(GradingAssessmentQuery query, OutputStream out) {
        GradingAssessmentExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<GradingAssessment> rows = GradingAssessmentExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (GradingAssessment row : rows) {
            try {
                GradingStandard standard = resolveStandardByCode(row.getStandardCode());
                row.setStandardId(standard.getId());
                this.create(row);
                created++;
            } catch (IllegalArgumentException e) {
                skipped++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", rows.size());
        return result;
    }

    @Override
    public void writeImportTemplate(OutputStream out) {
        GradingAssessmentExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<GradingAssessment> buildWrapper(GradingAssessmentQuery query) {
        LambdaQueryWrapper<GradingAssessment> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(GradingAssessment::getDeptName, kw)
                    .or().like(GradingAssessment::getAssessor, kw));
        }
        if (query.getStandardId() != null) {
            w.eq(GradingAssessment::getStandardId, query.getStandardId());
        }
        if (query.getDeptId() != null) {
            w.eq(GradingAssessment::getDeptId, query.getDeptId());
        }
        if (StringUtils.hasText(query.getGrade())) {
            w.eq(GradingAssessment::getGrade, query.getGrade());
        }
        if (query.getStatus() != null) {
            w.eq(GradingAssessment::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getPeriod())) {
            w.eq(GradingAssessment::getPeriod, query.getPeriod().trim());
        }
        if (StringUtils.hasText(query.getPeriodFrom())) {
            w.ge(GradingAssessment::getPeriod, query.getPeriodFrom().trim());
        }
        if (StringUtils.hasText(query.getPeriodTo())) {
            w.le(GradingAssessment::getPeriod, query.getPeriodTo().trim());
        }
        w.orderByDesc(GradingAssessment::getId);
        return w;
    }

    private GradingStandard resolveStandard(Long standardId) {
        if (standardId == null) {
            throw new IllegalArgumentException("分级标准不能为空");
        }
        GradingStandard s = standardMapper.selectById(standardId);
        if (s == null) {
            throw new IllegalArgumentException("分级标准不存在：" + standardId);
        }
        if (!Integer.valueOf(MedGradingTypeUtil.STATUS_ENABLED).equals(s.getStatus())) {
            throw new IllegalArgumentException("分级标准「" + s.getStandardName() + "」已停用，不能用于新建评定");
        }
        return s;
    }

    private GradingStandard resolveStandardByCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("标准编码不能为空");
        }
        GradingStandard s = standardMapper.selectOne(new LambdaQueryWrapper<GradingStandard>()
                .eq(GradingStandard::getStandardCode, code.trim()));
        if (s == null) {
            throw new IllegalArgumentException("分级标准不存在，编码：" + code);
        }
        if (!Integer.valueOf(MedGradingTypeUtil.STATUS_ENABLED).equals(s.getStatus())) {
            throw new IllegalArgumentException("分级标准「" + s.getStandardName() + "」已停用，不能用于新建评定");
        }
        return s;
    }

    /** 按科室 id 或名称补全另一方：id 优先查库，名称查不到时 id 留空 */
    private void resolveDept(GradingAssessment a) {
        if (a.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(a.getDeptId());
            if (dept == null) {
                throw new IllegalArgumentException("科室不存在：" + a.getDeptId());
            }
            a.setDeptName(dept.getName());
            return;
        }
        if (StringUtils.hasText(a.getDeptName())) {
            SysDept dept = deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getName, a.getDeptName().trim()));
            if (dept != null) {
                a.setDeptId(dept.getId());
            }
        }
    }

    private void fillStandardInfo(GradingAssessment a, GradingStandard s) {
        a.setStandardCode(s.getStandardCode());
        a.setStandardName(s.getStandardName());
    }

    /** 评定校验：科室、周期、评分范围、同一科室 + 同一标准 + 同一周期不重复 */
    private void validateAssessment(GradingAssessment a, Long excludeId) {
        if (!StringUtils.hasText(a.getDeptName())) {
            throw new IllegalArgumentException("评定科室不能为空");
        }
        if (!MedGradingTypeUtil.isValidPeriod(a.getPeriod())) {
            throw new IllegalArgumentException("评定周期格式不正确，应为 yyyy-MM，如 2026-11");
        }
        if (a.getScore() == null) {
            throw new IllegalArgumentException("评分不能为空");
        }
        if (a.getScore().compareTo(BigDecimal.ZERO) < 0
                || a.getScore().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("评分须在 0-100 之间");
        }
        LambdaQueryWrapper<GradingAssessment> dup = new LambdaQueryWrapper<GradingAssessment>()
                .eq(GradingAssessment::getDeptName, a.getDeptName().trim())
                .eq(GradingAssessment::getStandardId, a.getStandardId())
                .eq(GradingAssessment::getPeriod, a.getPeriod().trim());
        if (excludeId != null) {
            dup.ne(GradingAssessment::getId, excludeId);
        }
        if (this.count(dup) > 0) {
            throw new IllegalArgumentException("该科室在 " + a.getPeriod().trim()
                    + " 已有同一标准的分级评定，请修改原记录");
        }
    }

    private BigDecimal avgScore(List<GradingAssessment> list) {
        if (list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (GradingAssessment a : list) {
            if (a.getScore() != null) {
                sum = sum.add(a.getScore());
            }
        }
        return sum.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
    }
}
