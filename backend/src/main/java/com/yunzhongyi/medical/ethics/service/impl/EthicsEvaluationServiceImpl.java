package com.yunzhongyi.medical.ethics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.ApproveRequest;
import com.yunzhongyi.medical.ethics.dto.EthicsEvaluationQuery;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsEvaluation;
import com.yunzhongyi.medical.ethics.entity.EthicsProfile;
import com.yunzhongyi.medical.ethics.entity.EthicsTemplate;
import com.yunzhongyi.medical.ethics.excel.EthicsEvaluationExcelUtil;
import com.yunzhongyi.medical.ethics.mapper.EthicsEvaluationMapper;
import com.yunzhongyi.medical.ethics.mapper.EthicsProfileMapper;
import com.yunzhongyi.medical.ethics.mapper.EthicsTemplateMapper;
import com.yunzhongyi.medical.ethics.service.EthicsEvaluationService;
import com.yunzhongyi.medical.ethics.service.EthicsTypeUtil;
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
 * 医德考评服务实现：评分录入（总分与等级自动计算）+ 一级审批 + 评分统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EthicsEvaluationServiceImpl
        extends ServiceImpl<EthicsEvaluationMapper, EthicsEvaluation>
        implements EthicsEvaluationService {

    /** Excel 里只有医师姓名没有医师 id，导入时按姓名查医师档案补全 */
    private final PhysicianMapper physicianMapper;
    private final EthicsTemplateMapper templateMapper;
    private final EthicsProfileMapper profileMapper;

    @Override
    public PageResult<EthicsEvaluation> pageQuery(EthicsEvaluationQuery query) {
        Page<EthicsEvaluation> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<EthicsEvaluation> listByQuery(EthicsEvaluationQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<EthicsEvaluation> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(e -> Integer.valueOf(EthicsTypeUtil.APPROVAL_PENDING).equals(e.getStatus())).count());
        r.put("passed", all.stream()
                .filter(e -> Integer.valueOf(EthicsTypeUtil.APPROVAL_PASSED).equals(e.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(e -> Integer.valueOf(EthicsTypeUtil.APPROVAL_REJECTED).equals(e.getStatus())).count());
        r.put("avgScore", avgOf(all));
        long excellent = all.stream().filter(e -> EthicsTypeUtil.GRADE_EXCELLENT.equals(e.getGrade())).count();
        r.put("excellentCount", excellent);
        r.put("excellentRate", rate(excellent, all.size()));
        Map<String, Long> byGrade = new LinkedHashMap<>();
        for (String g : List.of(EthicsTypeUtil.GRADE_EXCELLENT, EthicsTypeUtil.GRADE_QUALIFIED,
                EthicsTypeUtil.GRADE_BASIC, EthicsTypeUtil.GRADE_UNQUALIFIED)) {
            byGrade.put(g, all.stream().filter(e -> g.equals(e.getGrade())).count());
        }
        r.put("byGrade", byGrade);
        r.put("byDept", statsByDept());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<EthicsEvaluation>> grouped = new LinkedHashMap<>();
        for (EthicsEvaluation e : this.list()) {
            String dept = StringUtils.hasText(e.getDeptName()) ? e.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(e);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("avgScore", avgOf(list));
            long excellent = list.stream().filter(e -> EthicsTypeUtil.GRADE_EXCELLENT.equals(e.getGrade())).count();
            row.put("excellentCount", excellent);
            row.put("excellentRate", rate(excellent, list.size()));
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public EthicsEvaluation detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EthicsEvaluation approve(Long id, ApproveRequest request) {
        EthicsEvaluation e = getOrThrow(id);
        if (!Integer.valueOf(EthicsTypeUtil.APPROVAL_PENDING).equals(e.getStatus())) {
            throw new IllegalStateException("只有待审批的考评才能通过（当前状态："
                    + EthicsTypeUtil.approvalStatusText(e.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getApprover())) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        e.setApprover(request.getApprover().trim());
        e.setStatus(EthicsTypeUtil.APPROVAL_PASSED);
        String opinion = StringUtils.hasText(request.getOpinion()) ? request.getOpinion().trim() : "同意";
        e.setApprovalHistory(appendHistory(e.getApprovalHistory(),
                "医务科 " + e.getApprover() + "：通过（" + opinion + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”
        // 守卫（审批中的记录正是要从待审批改为已通过，守卫会误判）
        super.updateById(e);
        // 审批通过时同步医德档案的最近考评结果
        EthicsProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<EthicsProfile>()
                .eq(EthicsProfile::getPhysicianId, e.getPhysicianId()).last("LIMIT 1"));
        if (profile != null) {
            profile.setLastGrade(e.getGrade());
            profile.setLastEvaluateYear(e.getAssessYear());
            profileMapper.updateById(profile);
        }
        return e;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EthicsEvaluation reject(Long id, RejectRequest request) {
        EthicsEvaluation e = getOrThrow(id);
        if (!Integer.valueOf(EthicsTypeUtil.APPROVAL_PENDING).equals(e.getStatus())) {
            throw new IllegalStateException("只有待审批的考评才能驳回（当前状态："
                    + EthicsTypeUtil.approvalStatusText(e.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getApprover())) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        e.setApprover(request.getApprover().trim());
        e.setStatus(EthicsTypeUtil.APPROVAL_REJECTED);
        e.setApprovalHistory(appendHistory(e.getApprovalHistory(),
                "医务科 " + e.getApprover() + "：驳回（" + request.getReason().trim() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”守卫
        super.updateById(e);
        return e;
    }

    /** 删除：已通过的考评是正式记录，不能直接删除 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        EthicsEvaluation e = getOrThrow((Long) id);
        if (Integer.valueOf(EthicsTypeUtil.APPROVAL_PASSED).equals(e.getStatus())) {
            throw new IllegalStateException("已通过的考评不能直接删除");
        }
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(java.util.Collection<?> idList) {
        for (Object id : idList) {
            removeById((java.io.Serializable) id);
        }
        return true;
    }

    @Override
    public void exportExcel(EthicsEvaluationQuery query, OutputStream out) {
        EthicsEvaluationExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<EthicsEvaluation> list = EthicsEvaluationExcelUtil.read(in);
        int ok = 0;
        for (EthicsEvaluation e : list) {
            if (!StringUtils.hasText(e.getPhysicianName())) {
                log.warn("导入跳过：被考评医师姓名为空的行");
                continue;
            }
            // 同一医师同一模板同一年度视为重复，跳过
            LambdaQueryWrapper<EthicsEvaluation> qw = new LambdaQueryWrapper<EthicsEvaluation>()
                    .eq(EthicsEvaluation::getPhysicianName, e.getPhysicianName());
            if (e.getTemplateId() != null) {
                qw.eq(EthicsEvaluation::getTemplateId, e.getTemplateId());
            } else if (StringUtils.hasText(e.getTemplateName())) {
                qw.eq(EthicsEvaluation::getTemplateName, e.getTemplateName());
            }
            if (e.getAssessYear() != null) {
                qw.eq(EthicsEvaluation::getAssessYear, e.getAssessYear());
            }
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复考评 {} {} {}", e.getPhysicianName(), e.getTemplateName(), e.getAssessYear());
                continue;
            }
            // Excel 里没有医师 id，按姓名查医师档案补全（医师、科室）；查不到的跳过
            if (e.getPhysicianId() == null) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, e.getPhysicianName()).last("LIMIT 1"));
                if (physician == null) {
                    log.warn("导入跳过：医师 {} 在医师档案中不存在", e.getPhysicianName());
                    continue;
                }
                e.setPhysicianId(physician.getId());
                if (e.getDeptId() == null) {
                    e.setDeptId(physician.getDeptId());
                }
                if (!StringUtils.hasText(e.getDeptName())) {
                    e.setDeptName(physician.getDeptName());
                }
            }
            // 按模板名称补全模板 id
            if (e.getTemplateId() == null && StringUtils.hasText(e.getTemplateName())) {
                EthicsTemplate t = templateMapper.selectOne(new LambdaQueryWrapper<EthicsTemplate>()
                        .eq(EthicsTemplate::getTemplateName, e.getTemplateName()).last("LIMIT 1"));
                if (t != null) {
                    e.setTemplateId(t.getId());
                }
            }
            if (!StringUtils.hasText(e.getItemScores()) && e.getTotalScore() == null) {
                log.warn("导入跳过：医师 {} 既无各项得分也无总分", e.getPhysicianName());
                continue;
            }
            // 导入的均为待审批，需走审批流程
            e.setStatus(EthicsTypeUtil.APPROVAL_PENDING);
            e.setApprover(null);
            e.setApprovalHistory(null);
            if (e.getAssessYear() == null) {
                e.setAssessYear(LocalDate.now().getYear());
            }
            if (e.getAssessDate() == null) {
                e.setAssessDate(LocalDate.now());
            }
            try {
                this.save(e);
                ok++;
            } catch (IllegalArgumentException e2) {
                log.warn("导入跳过：{}（{}）", e.getPhysicianName(), e2.getMessage());
            }
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<EthicsEvaluation> buildWrapper(EthicsEvaluationQuery q) {
        LambdaQueryWrapper<EthicsEvaluation> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.like(EthicsEvaluation::getPhysicianName, q.getKeyword());
        }
        qw.eq(q.getDeptId() != null, EthicsEvaluation::getDeptId, q.getDeptId());
        qw.eq(q.getTemplateId() != null, EthicsEvaluation::getTemplateId, q.getTemplateId());
        qw.eq(q.getAssessYear() != null, EthicsEvaluation::getAssessYear, q.getAssessYear());
        qw.eq(StringUtils.hasText(q.getGrade()), EthicsEvaluation::getGrade, q.getGrade());
        qw.eq(q.getStatus() != null, EthicsEvaluation::getStatus, q.getStatus());
        qw.orderByDesc(EthicsEvaluation::getId);
        return qw;
    }

    /**
     * 保存/更新时：由各项得分求和自动算总分，再按总分自动算等级与考评结果；
     * 有模板时校验每项得分不超过该项权重。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EthicsEvaluation entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(EthicsEvaluation entity) {
        // 只有待审批的考评能修改：已通过/已驳回的考评不可再改
        EthicsEvaluation old = getOrThrow(entity.getId());
        if (!Integer.valueOf(EthicsTypeUtil.APPROVAL_PENDING).equals(old.getStatus())) {
            throw new IllegalStateException("只有待审批的考评才能修改（当前状态："
                    + EthicsTypeUtil.approvalStatusText(old.getStatus()) + "）");
        }
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(EthicsEvaluation e, boolean isCreate) {
        if (e.getPhysicianId() == null && !StringUtils.hasText(e.getPhysicianName())) {
            throw new IllegalArgumentException("被考评医师不能为空");
        }
        if (StringUtils.hasText(e.getItemScores())) {
            e.setTotalScore(EthicsTypeUtil.sumItemScores(e.getItemScores())
                    .setScale(2, RoundingMode.HALF_UP));
            validateAgainstTemplate(e);
        }
        if (e.getTotalScore() == null) {
            throw new IllegalArgumentException("请录入各项得分，总分由系统自动计算");
        }
        if (e.getTotalScore().compareTo(BigDecimal.ZERO) < 0
                || e.getTotalScore().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("总分应在 0-100 之间，当前为 "
                    + e.getTotalScore().stripTrailingZeros().toPlainString());
        }
        e.setGrade(EthicsTypeUtil.gradeOf(e.getTotalScore()));
        if (!StringUtils.hasText(e.getResult())) {
            e.setResult(EthicsTypeUtil.GRADE_UNQUALIFIED.equals(e.getGrade())
                    ? "不合格，需整改" : EthicsTypeUtil.gradeText(e.getGrade()));
        }
        if (isCreate) {
            if (e.getStatus() == null) {
                e.setStatus(EthicsTypeUtil.APPROVAL_PENDING);
            }
            if (e.getAssessYear() == null) {
                e.setAssessYear(LocalDate.now().getYear());
            }
            if (e.getAssessDate() == null) {
                e.setAssessDate(LocalDate.now());
            }
            if (!StringUtils.hasText(e.getCycle())) {
                e.setCycle("年度");
            }
            // 按模板 id 补全模板名称
            if (e.getTemplateId() != null && !StringUtils.hasText(e.getTemplateName())) {
                EthicsTemplate t = templateMapper.selectById(e.getTemplateId());
                if (t != null) {
                    e.setTemplateName(t.getTemplateName());
                }
            }
            // 按姓名查医师档案补全医师 id / 科室
            if (e.getPhysicianId() == null && StringUtils.hasText(e.getPhysicianName())) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, e.getPhysicianName()).last("LIMIT 1"));
                if (physician != null) {
                    e.setPhysicianId(physician.getId());
                    if (e.getDeptId() == null) {
                        e.setDeptId(physician.getDeptId());
                    }
                    if (!StringUtils.hasText(e.getDeptName())) {
                        e.setDeptName(physician.getDeptName());
                    }
                }
            }
        }
    }

    /** 有模板时：每项得分不得超过该项权重，且不得出现模板外的评分项 */
    private void validateAgainstTemplate(EthicsEvaluation e) {
        if (e.getTemplateId() == null) {
            return;
        }
        EthicsTemplate t = templateMapper.selectById(e.getTemplateId());
        if (t == null || !StringUtils.hasText(t.getScoreItems())) {
            return;
        }
        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        for (Map<String, Object> item : EthicsTypeUtil.parseScoreItems(t.getScoreItems())) {
            weights.put(String.valueOf(item.get("name")), new BigDecimal(String.valueOf(item.get("weight"))));
        }
        for (Map.Entry<String, BigDecimal> entry : EthicsTypeUtil.parseItemScores(e.getItemScores()).entrySet()) {
            BigDecimal w = weights.get(entry.getKey());
            if (w == null) {
                throw new IllegalArgumentException("评分项「" + entry.getKey() + "」不在模板「" + t.getTemplateName() + "」中");
            }
            if (entry.getValue().compareTo(w) > 0) {
                throw new IllegalArgumentException("评分项「" + entry.getKey() + "」得分 "
                        + entry.getValue().stripTrailingZeros().toPlainString()
                        + " 超过该项满分 " + w.stripTrailingZeros().toPlainString());
            }
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("评分项「" + entry.getKey() + "」得分不能为负数");
            }
        }
    }

    private BigDecimal avgOf(List<EthicsEvaluation> list) {
        if (list.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (EthicsEvaluation e : list) {
            if (e.getTotalScore() != null) {
                sum = sum.add(e.getTotalScore());
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

    private String appendHistory(String history, String entry) {
        if (!StringUtils.hasText(history)) {
            return entry;
        }
        return history + "；" + entry;
    }

    private EthicsEvaluation getOrThrow(Long id) {
        EthicsEvaluation e = this.getById(id);
        if (e == null) {
            throw new IllegalArgumentException("医德考评不存在：" + id);
        }
        return e;
    }
}
