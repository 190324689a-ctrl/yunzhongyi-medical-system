package com.yunzhongyi.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.assessment.dto.AssessmentQuery;
import com.yunzhongyi.medical.assessment.entity.Assessment;
import com.yunzhongyi.medical.assessment.entity.AssessmentTemplate;
import com.yunzhongyi.medical.assessment.excel.AssessmentExcelUtil;
import com.yunzhongyi.medical.assessment.mapper.AssessmentMapper;
import com.yunzhongyi.medical.assessment.mapper.AssessmentTemplateMapper;
import com.yunzhongyi.medical.assessment.service.AssessmentGradeUtil;
import com.yunzhongyi.medical.assessment.service.AssessmentService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.mapper.PhysicianMapper;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医师考核服务实现：评分录入（总分与等级自动计算）+ 评分统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl
        extends ServiceImpl<AssessmentMapper, Assessment>
        implements AssessmentService {

    /** Excel 里只有医师姓名没有医师 id，导入时按姓名查医师档案补全 */
    private final PhysicianMapper physicianMapper;
    private final AssessmentTemplateMapper templateMapper;

    @Override
    public PageResult<Assessment> pageQuery(AssessmentQuery query) {
        Page<Assessment> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<Assessment> listByQuery(AssessmentQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<Assessment> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("avgScore", avgOf(all));
        long excellent = all.stream().filter(a -> AssessmentGradeUtil.EXCELLENT.equals(a.getGrade())).count();
        r.put("excellentCount", excellent);
        r.put("excellentRate", rate(excellent, all.size()));
        Map<String, Long> byGrade = new LinkedHashMap<>();
        for (String g : List.of(AssessmentGradeUtil.EXCELLENT, AssessmentGradeUtil.QUALIFIED,
                AssessmentGradeUtil.BASIC, AssessmentGradeUtil.UNQUALIFIED)) {
            String grade = g;
            byGrade.put(grade, all.stream().filter(a -> grade.equals(a.getGrade())).count());
        }
        r.put("byGrade", byGrade);
        r.put("byDept", statsByDept());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<Assessment>> grouped = new LinkedHashMap<>();
        for (Assessment a : this.list()) {
            String dept = StringUtils.hasText(a.getDeptName()) ? a.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(a);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("avgScore", avgOf(list));
            long excellent = list.stream().filter(a -> AssessmentGradeUtil.EXCELLENT.equals(a.getGrade())).count();
            row.put("excellentCount", excellent);
            row.put("excellentRate", rate(excellent, list.size()));
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public void exportExcel(AssessmentQuery query, OutputStream out) {
        AssessmentExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<Assessment> list = AssessmentExcelUtil.read(in);
        int ok = 0;
        for (Assessment a : list) {
            if (!StringUtils.hasText(a.getPhysicianName())) {
                log.warn("导入跳过：被考核医师姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(a.getAssessType())) {
                log.warn("导入跳过：考核类型为空的行（医师 {}）", a.getPhysicianName());
                continue;
            }
            // 同一医师同一模板同一年度视为重复，跳过
            LambdaQueryWrapper<Assessment> qw = new LambdaQueryWrapper<Assessment>()
                    .eq(Assessment::getPhysicianName, a.getPhysicianName());
            if (a.getTemplateId() != null) {
                qw.eq(Assessment::getTemplateId, a.getTemplateId());
            } else if (StringUtils.hasText(a.getTemplateName())) {
                qw.eq(Assessment::getTemplateName, a.getTemplateName());
            }
            if (a.getAssessYear() != null) {
                qw.eq(Assessment::getAssessYear, a.getAssessYear());
            }
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复考核 {} {} {}", a.getPhysicianName(), a.getTemplateName(), a.getAssessYear());
                continue;
            }
            // Excel 里没有医师 id，按姓名查医师档案补全（医师、科室）；查不到的跳过
            if (a.getPhysicianId() == null) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, a.getPhysicianName()).last("LIMIT 1"));
                if (physician == null) {
                    log.warn("导入跳过：医师 {} 在医师档案中不存在", a.getPhysicianName());
                    continue;
                }
                a.setPhysicianId(physician.getId());
                if (a.getDeptId() == null) {
                    a.setDeptId(physician.getDeptId());
                }
                if (!StringUtils.hasText(a.getDeptName())) {
                    a.setDeptName(physician.getDeptName());
                }
            }
            // 按模板名称补全模板 id / 类型 / 周期
            if (a.getTemplateId() == null && StringUtils.hasText(a.getTemplateName())) {
                AssessmentTemplate t = templateMapper.selectOne(new LambdaQueryWrapper<AssessmentTemplate>()
                        .eq(AssessmentTemplate::getTemplateName, a.getTemplateName()).last("LIMIT 1"));
                if (t != null) {
                    a.setTemplateId(t.getId());
                    if (!StringUtils.hasText(a.getAssessType()) || a.getAssessType() == null) {
                        a.setAssessType(t.getAssessType());
                    }
                    if (!StringUtils.hasText(a.getCycle())) {
                        a.setCycle(t.getCycle());
                    }
                }
            }
            if (!StringUtils.hasText(a.getItemScores()) && a.getTotalScore() == null) {
                log.warn("导入跳过：医师 {} 既无各项得分也无总分", a.getPhysicianName());
                continue;
            }
            if (a.getAssessYear() == null) {
                a.setAssessYear(LocalDate.now().getYear());
            }
            if (a.getAssessDate() == null) {
                a.setAssessDate(LocalDate.now());
            }
            this.save(a);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<Assessment> buildWrapper(AssessmentQuery q) {
        LambdaQueryWrapper<Assessment> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.like(Assessment::getPhysicianName, q.getKeyword());
        }
        qw.eq(q.getDeptId() != null, Assessment::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getAssessType()), Assessment::getAssessType, q.getAssessType());
        qw.eq(q.getAssessYear() != null, Assessment::getAssessYear, q.getAssessYear());
        qw.eq(StringUtils.hasText(q.getGrade()), Assessment::getGrade, q.getGrade());
        qw.eq(q.getTemplateId() != null, Assessment::getTemplateId, q.getTemplateId());
        qw.orderByDesc(Assessment::getId);
        return qw;
    }

    /**
     * 保存/更新时：由各项得分求和自动算总分，再按总分自动算等级与考核结果；
     * 有模板时校验每项得分不超过该项权重。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Assessment entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Assessment entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(Assessment a, boolean isCreate) {
        if (StringUtils.hasText(a.getItemScores())) {
            a.setTotalScore(AssessmentGradeUtil.sumItemScores(a.getItemScores())
                    .setScale(2, RoundingMode.HALF_UP));
            validateAgainstTemplate(a);
        }
        if (a.getTotalScore() == null) {
            throw new IllegalArgumentException("请录入各项得分，总分由系统自动计算");
        }
        if (a.getTotalScore().compareTo(BigDecimal.ZERO) < 0
                || a.getTotalScore().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("总分应在 0-100 之间，当前为 "
                    + a.getTotalScore().stripTrailingZeros().toPlainString());
        }
        a.setGrade(AssessmentGradeUtil.gradeOf(a.getTotalScore()));
        if (!StringUtils.hasText(a.getResult())) {
            a.setResult(AssessmentGradeUtil.UNQUALIFIED.equals(a.getGrade())
                    ? "不合格，需整改" : AssessmentGradeUtil.gradeText(a.getGrade()));
        }
        if (isCreate) {
            if (a.getAssessYear() == null) {
                a.setAssessYear(LocalDate.now().getYear());
            }
            if (a.getAssessDate() == null) {
                a.setAssessDate(LocalDate.now());
            }
            if (!StringUtils.hasText(a.getCycle())) {
                a.setCycle("年度");
            }
        }
    }

    /** 有模板时：每项得分不得超过该项权重，且不得出现模板外的评分项 */
    private void validateAgainstTemplate(Assessment a) {
        if (a.getTemplateId() == null) {
            return;
        }
        AssessmentTemplate t = templateMapper.selectById(a.getTemplateId());
        if (t == null || !StringUtils.hasText(t.getScoreItems())) {
            return;
        }
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        for (Map<String, Object> item : AssessmentGradeUtil.parseScoreItems(t.getScoreItems())) {
            weights.put(String.valueOf(item.get("name")), new BigDecimal(String.valueOf(item.get("weight"))));
        }
        for (Map.Entry<String, BigDecimal> e : AssessmentGradeUtil.parseItemScores(a.getItemScores()).entrySet()) {
            BigDecimal w = weights.get(e.getKey());
            if (w == null) {
                throw new IllegalArgumentException("评分项「" + e.getKey() + "」不在模板「" + t.getTemplateName() + "」中");
            }
            if (e.getValue().compareTo(w) > 0) {
                throw new IllegalArgumentException("评分项「" + e.getKey() + "」得分 "
                        + e.getValue().stripTrailingZeros().toPlainString()
                        + " 超过该项满分 " + w.stripTrailingZeros().toPlainString());
            }
            if (e.getValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("评分项「" + e.getKey() + "」得分不能为负数");
            }
        }
    }

    private BigDecimal avgOf(List<Assessment> list) {
        if (list.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (Assessment a : list) {
            if (a.getTotalScore() != null) {
                sum = sum.add(a.getTotalScore());
                n++;
            }
        }
        if (n == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }
}
