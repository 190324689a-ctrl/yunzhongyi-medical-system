package com.yunzhongyi.medical.newtech.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.newtech.dto.NewTechQuery;
import com.yunzhongyi.medical.newtech.entity.NewTechProject;
import com.yunzhongyi.medical.newtech.entity.NewTechRecord;
import com.yunzhongyi.medical.newtech.excel.NewTechExcelUtil;
import com.yunzhongyi.medical.newtech.mapper.NewTechProjectMapper;
import com.yunzhongyi.medical.newtech.mapper.NewTechRecordMapper;
import com.yunzhongyi.medical.newtech.service.NewTechService;
import com.yunzhongyi.medical.newtech.service.NewTechTypeUtil;
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
 * 新技术、新项目管理服务实现：申报 CRUD + 一级审批（医务科）+ 伦理审查 +
 * 进展/验收/推广记录（驱动状态流转）+ 统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewTechServiceImpl
        extends ServiceImpl<NewTechProjectMapper, NewTechProject>
        implements NewTechService {

    private final NewTechRecordMapper recordMapper;

    @Override
    public PageResult<NewTechProject> pageQuery(NewTechQuery query) {
        Page<NewTechProject> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<NewTechProject> listByQuery(NewTechQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<NewTechProject> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_PENDING).equals(p.getStatus())).count());
        r.put("approved", all.stream()
                .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_APPROVED).equals(p.getStatus())).count());
        r.put("accepted", all.stream()
                .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_ACCEPTED).equals(p.getStatus())).count());
        r.put("promoted", all.stream()
                .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_PROMOTED).equals(p.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_REJECTED).equals(p.getStatus())).count());
        r.put("ethicsPending", all.stream()
                .filter(p -> Integer.valueOf(NewTechTypeUtil.ETHICS_PENDING).equals(p.getEthicsStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(NewTechTypeUtil.TYPE_NEW_TECH, NewTechTypeUtil.TYPE_NEW_PROJECT)) {
            byType.put(t, all.stream().filter(p -> t.equals(p.getProjectType())).count());
        }
        r.put("byType", byType);
        r.put("byDept", statsByDept());
        r.put("byMonth", statsByMonth());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<NewTechProject>> grouped = new LinkedHashMap<>();
        for (NewTechProject p : this.list()) {
            String dept = StringUtils.hasText(p.getDeptName()) ? p.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("pendingCount", list.stream()
                    .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_PENDING).equals(p.getStatus())).count());
            row.put("approvedCount", list.stream()
                    .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_APPROVED).equals(p.getStatus())).count());
            row.put("acceptedCount", list.stream()
                    .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_ACCEPTED).equals(p.getStatus())).count());
            row.put("promotedCount", list.stream()
                    .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_PROMOTED).equals(p.getStatus())).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        Map<YearMonth, List<NewTechProject>> grouped = new LinkedHashMap<>();
        for (NewTechProject p : this.list()) {
            if (p.getApplyDate() == null) {
                continue;
            }
            grouped.computeIfAbsent(YearMonth.from(p.getApplyDate()), k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.keySet().stream().sorted().forEach(month -> {
            List<NewTechProject> list = grouped.get(month);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            row.put("count", (long) list.size());
            row.put("approvedCount", list.stream()
                    .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_APPROVED).equals(p.getStatus())
                            || Integer.valueOf(NewTechTypeUtil.STATUS_ACCEPTED).equals(p.getStatus())
                            || Integer.valueOf(NewTechTypeUtil.STATUS_PROMOTED).equals(p.getStatus())).count());
            row.put("pendingCount", list.stream()
                    .filter(p -> Integer.valueOf(NewTechTypeUtil.STATUS_PENDING).equals(p.getStatus())).count());
            result.add(row);
        });
        return result;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        NewTechProject project = getOrThrow(id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("project", project);
        r.put("records", listRecords(id));
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NewTechProject approve(Long id, String approver, String opinion) {
        NewTechProject p = getOrThrow(id);
        if (!Integer.valueOf(NewTechTypeUtil.STATUS_PENDING).equals(p.getStatus())) {
            throw new IllegalStateException("该项目已结束（" + NewTechTypeUtil.statusText(p.getStatus())
                    + "），不能再审批");
        }
        if (Boolean.TRUE.equals(p.getEthicsRequired())
                && !Integer.valueOf(NewTechTypeUtil.ETHICS_PASSED).equals(p.getEthicsStatus())) {
            throw new IllegalStateException("该项目需伦理审查通过后才能立项，当前伦理审查状态："
                    + NewTechTypeUtil.ethicsText(p.getEthicsStatus()));
        }
        appendHistory(p, "医务科", approver, "通过", opinion);
        p.setStatus(NewTechTypeUtil.STATUS_APPROVED);
        p.setApprover(approver);
        p.setApprovalOpinion(opinion);
        // 直接调 super.updateById：审批已校验待审批状态；走本类 updateById 的
        // normalize 会在同一事务内因 MyBatis 一级缓存拿到已被改过的同一对象而误判
        super.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NewTechProject reject(Long id, String approver, String opinion) {
        NewTechProject p = getOrThrow(id);
        if (!Integer.valueOf(NewTechTypeUtil.STATUS_PENDING).equals(p.getStatus())) {
            throw new IllegalStateException("该项目已结束（" + NewTechTypeUtil.statusText(p.getStatus())
                    + "），不能再审批");
        }
        appendHistory(p, "医务科", approver, "驳回", opinion);
        p.setStatus(NewTechTypeUtil.STATUS_REJECTED);
        p.setApprover(approver);
        p.setApprovalOpinion(opinion);
        // 同 approve：直接调 super.updateById，跳过 normalize 的修改守卫
        super.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NewTechProject ethicsReview(Long id, Boolean passed, String reviewer, String opinion) {
        NewTechProject p = getOrThrow(id);
        if (!Boolean.TRUE.equals(p.getEthicsRequired())) {
            throw new IllegalStateException("该项目不需伦理审查");
        }
        if (!Integer.valueOf(NewTechTypeUtil.ETHICS_PENDING).equals(p.getEthicsStatus())) {
            throw new IllegalStateException("该项目伦理审查已结束（"
                    + NewTechTypeUtil.ethicsText(p.getEthicsStatus()) + "），不能再审查");
        }
        if (passed == null) {
            throw new IllegalArgumentException("请给出伦理审查结论（通过/未通过）");
        }
        p.setEthicsStatus(passed ? NewTechTypeUtil.ETHICS_PASSED : NewTechTypeUtil.ETHICS_FAILED);
        p.setEthicsReviewer(reviewer);
        p.setEthicsOpinion(opinion);
        p.setEthicsReviewDate(LocalDate.now());
        String line = "伦理审查" + (StringUtils.hasText(reviewer) ? " " + reviewer : "")
                + "：" + (passed ? "通过" : "未通过")
                + (StringUtils.hasText(opinion) ? "（" + opinion + "）" : "");
        p.setApprovalHistory(StringUtils.hasText(p.getApprovalHistory())
                ? p.getApprovalHistory() + "；" + line : line);
        // 同 approve：直接调 super.updateById，跳过 normalize 的修改守卫
        super.updateById(p);
        return p;
    }

    @Override
    public List<NewTechRecord> listRecords(Long projectId) {
        getOrThrow(projectId);
        return recordMapper.selectList(new LambdaQueryWrapper<NewTechRecord>()
                .eq(NewTechRecord::getProjectId, projectId)
                .orderByAsc(NewTechRecord::getRecordDate).orderByAsc(NewTechRecord::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NewTechRecord addRecord(Long projectId, NewTechRecord record) {
        NewTechProject p = getOrThrow(projectId);
        if (!StringUtils.hasText(record.getRecordType())
                || !NewTechTypeUtil.isValidRecordType(record.getRecordType())) {
            throw new IllegalArgumentException("记录类型不正确");
        }
        if (!StringUtils.hasText(record.getContent())) {
            throw new IllegalArgumentException("记录内容不能为空");
        }
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }
        record.setProjectId(projectId);
        Integer status = p.getStatus();
        switch (record.getRecordType()) {
            case NewTechTypeUtil.RECORD_PROGRESS -> {
                if (!Integer.valueOf(NewTechTypeUtil.STATUS_APPROVED).equals(status)
                        && !Integer.valueOf(NewTechTypeUtil.STATUS_ACCEPTED).equals(status)) {
                    throw new IllegalStateException("只有已立项/已验收的项目才能添加进展记录，当前状态："
                            + NewTechTypeUtil.statusText(status));
                }
            }
            case NewTechTypeUtil.RECORD_ACCEPTANCE -> {
                if (!Integer.valueOf(NewTechTypeUtil.STATUS_APPROVED).equals(status)) {
                    throw new IllegalStateException("只有已立项的项目才能添加验收记录，当前状态："
                            + NewTechTypeUtil.statusText(status));
                }
            }
            case NewTechTypeUtil.RECORD_PROMOTION -> {
                if (!Integer.valueOf(NewTechTypeUtil.STATUS_ACCEPTED).equals(status)) {
                    throw new IllegalStateException("只有已验收的项目才能添加推广记录，当前状态："
                            + NewTechTypeUtil.statusText(status));
                }
            }
            default -> throw new IllegalArgumentException("记录类型不正确");
        }
        recordMapper.insert(record);
        // 验收记录 → 项目变为已验收；推广记录 → 项目变为已推广
        Integer nextStatus = switch (record.getRecordType()) {
            case NewTechTypeUtil.RECORD_ACCEPTANCE -> NewTechTypeUtil.STATUS_ACCEPTED;
            case NewTechTypeUtil.RECORD_PROMOTION -> NewTechTypeUtil.STATUS_PROMOTED;
            default -> null;
        };
        if (nextStatus != null) {
            p.setStatus(nextStatus);
            // 直接调 super.updateById，跳过 normalize 的修改守卫（项目已不是待审批）
            super.updateById(p);
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRecord(Long projectId, Long recordId) {
        getOrThrow(projectId);
        NewTechRecord record = recordMapper.selectById(recordId);
        if (record == null || !projectId.equals(record.getProjectId())) {
            throw new IllegalArgumentException("进展记录不存在：" + recordId);
        }
        return recordMapper.deleteById(recordId) > 0;
    }

    @Override
    public void exportExcel(NewTechQuery query, OutputStream out) {
        NewTechExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<NewTechProject> list = NewTechExcelUtil.read(in);
        int ok = 0;
        for (NewTechProject p : list) {
            if (!StringUtils.hasText(p.getProjectName())) {
                log.warn("导入跳过：项目名称为空的行");
                continue;
            }
            if (!StringUtils.hasText(p.getProjectType()) || !NewTechTypeUtil.isValidType(p.getProjectType())) {
                log.warn("导入跳过：项目类型为空或不正确（项目 {}）", p.getProjectName());
                continue;
            }
            if (!StringUtils.hasText(p.getApplicantName())) {
                log.warn("导入跳过：申报人为空（项目 {}）", p.getProjectName());
                continue;
            }
            if (p.getEthicsStatus() == null) {
                p.setEthicsStatus(Boolean.TRUE.equals(p.getEthicsRequired())
                        ? NewTechTypeUtil.ETHICS_PENDING : NewTechTypeUtil.ETHICS_NOT_REQUIRED);
            }
            if (p.getStatus() == null) {
                p.setStatus(NewTechTypeUtil.STATUS_PENDING);
            }
            // 同一项目名称同一申报科室视为重复，跳过
            LambdaQueryWrapper<NewTechProject> qw = new LambdaQueryWrapper<NewTechProject>()
                    .eq(NewTechProject::getProjectName, p.getProjectName());
            if (StringUtils.hasText(p.getDeptName())) {
                qw.eq(NewTechProject::getDeptName, p.getDeptName());
            }
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复新技术、新项目 {} {}", p.getProjectName(), p.getDeptName());
                continue;
            }
            this.save(p);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<NewTechProject> buildWrapper(NewTechQuery q) {
        LambdaQueryWrapper<NewTechProject> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.and(w -> w.like(NewTechProject::getProjectName, q.getKeyword())
                    .or().like(NewTechProject::getApplicantName, q.getKeyword())
                    .or().like(NewTechProject::getProjectLeader, q.getKeyword()));
        }
        qw.eq(q.getDeptId() != null, NewTechProject::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getProjectType()), NewTechProject::getProjectType, q.getProjectType());
        qw.eq(q.getStatus() != null, NewTechProject::getStatus, q.getStatus());
        qw.eq(q.getEthicsStatus() != null, NewTechProject::getEthicsStatus, q.getEthicsStatus());
        qw.eq(q.getApplicantId() != null, NewTechProject::getApplicantId, q.getApplicantId());
        qw.ge(q.getStartDate() != null, NewTechProject::getApplyDate, q.getStartDate());
        qw.le(q.getEndDate() != null, NewTechProject::getApplyDate, q.getEndDate());
        qw.orderByDesc(NewTechProject::getApplyDate).orderByDesc(NewTechProject::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时只允许改待审批的项目 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(NewTechProject entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(NewTechProject entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(NewTechProject p, boolean isCreate) {
        if (!StringUtils.hasText(p.getProjectType()) || !NewTechTypeUtil.isValidType(p.getProjectType())) {
            throw new IllegalArgumentException("项目类型不正确");
        }
        if (p.getStartDate() != null && p.getEndDate() != null && p.getEndDate().isBefore(p.getStartDate())) {
            throw new IllegalArgumentException("计划结束日期不能早于计划开始日期");
        }
        if (p.getBudget() != null && p.getBudget().signum() < 0) {
            throw new IllegalArgumentException("经费预算不能为负数");
        }
        if (p.getEthicsRequired() == null) {
            p.setEthicsRequired(Boolean.FALSE);
        }
        if (isCreate) {
            if (p.getStatus() == null) {
                p.setStatus(NewTechTypeUtil.STATUS_PENDING);
            }
            if (p.getEthicsStatus() == null) {
                p.setEthicsStatus(Boolean.TRUE.equals(p.getEthicsRequired())
                        ? NewTechTypeUtil.ETHICS_PENDING : NewTechTypeUtil.ETHICS_NOT_REQUIRED);
            }
            if (p.getApplyDate() == null) {
                p.setApplyDate(LocalDate.now());
            }
        } else {
            // 更新只允许改待审批的项目（审批/伦理/验收流转不走这里）。
            // 注意：这里用 selectOne 而不用 getById——同一事务内 getById 可能因
            // MyBatis 一级缓存返回调用方已改过的同一对象，导致状态判断失真。
            NewTechProject existing = this.baseMapper.selectOne(new LambdaQueryWrapper<NewTechProject>()
                    .eq(NewTechProject::getId, p.getId()));
            if (existing == null) {
                throw new IllegalArgumentException("新技术、新项目不存在：" + p.getId());
            }
            if (!Integer.valueOf(NewTechTypeUtil.STATUS_PENDING).equals(existing.getStatus())) {
                throw new IllegalStateException("已结束申报阶段的项目（"
                        + NewTechTypeUtil.statusText(existing.getStatus()) + "）不能再修改申报内容");
            }
            // 已有伦理审查结论的项目，修改时不允许改动伦理审查状态
            if (existing.getEthicsStatus() != null
                    && !Integer.valueOf(NewTechTypeUtil.ETHICS_PENDING).equals(existing.getEthicsStatus())
                    && !Integer.valueOf(NewTechTypeUtil.ETHICS_NOT_REQUIRED).equals(existing.getEthicsStatus())) {
                p.setEthicsStatus(existing.getEthicsStatus());
                p.setEthicsReviewer(existing.getEthicsReviewer());
                p.setEthicsOpinion(existing.getEthicsOpinion());
                p.setEthicsReviewDate(existing.getEthicsReviewDate());
            }
        }
    }

    private NewTechProject getOrThrow(Long id) {
        NewTechProject p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("新技术、新项目不存在：" + id);
        }
        return p;
    }

    private void appendHistory(NewTechProject p, String step, String approver, String action, String opinion) {
        String who = StringUtils.hasText(approver) && !approver.equals(step) ? " " + approver : "";
        String line = step + who + "：" + action
                + (StringUtils.hasText(opinion) ? "（" + opinion + "）" : "");
        p.setApprovalHistory(StringUtils.hasText(p.getApprovalHistory())
                ? p.getApprovalHistory() + "；" + line : line);
    }
}
