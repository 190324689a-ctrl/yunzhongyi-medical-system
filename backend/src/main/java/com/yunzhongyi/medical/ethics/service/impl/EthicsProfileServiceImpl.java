package com.yunzhongyi.medical.ethics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.EthicsProfileQuery;
import com.yunzhongyi.medical.ethics.entity.EthicsCommendation;
import com.yunzhongyi.medical.ethics.entity.EthicsComplaint;
import com.yunzhongyi.medical.ethics.entity.EthicsEvaluation;
import com.yunzhongyi.medical.ethics.entity.EthicsProfile;
import com.yunzhongyi.medical.ethics.excel.EthicsProfileExcelUtil;
import com.yunzhongyi.medical.ethics.mapper.EthicsCommendationMapper;
import com.yunzhongyi.medical.ethics.mapper.EthicsComplaintMapper;
import com.yunzhongyi.medical.ethics.mapper.EthicsEvaluationMapper;
import com.yunzhongyi.medical.ethics.mapper.EthicsProfileMapper;
import com.yunzhongyi.medical.ethics.service.EthicsProfileService;
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
 * 医德医风档案服务实现：档案 CRUD + 统计 + Excel 批量导入导出
 * <p>
 * 表彰/投诉/考评统计直接读各自的 Mapper，不依赖其他服务，
 * 避免服务之间互相注入形成循环依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EthicsProfileServiceImpl
        extends ServiceImpl<EthicsProfileMapper, EthicsProfile>
        implements EthicsProfileService {

    private final PhysicianMapper physicianMapper;
    private final EthicsEvaluationMapper evaluationMapper;
    private final EthicsComplaintMapper complaintMapper;
    private final EthicsCommendationMapper commendationMapper;

    @Override
    public PageResult<EthicsProfile> pageQuery(EthicsProfileQuery query) {
        Page<EthicsProfile> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<EthicsProfile> listByQuery(EthicsProfileQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<EthicsProfile> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("active", all.stream()
                .filter(p -> Integer.valueOf(EthicsTypeUtil.PROFILE_ACTIVE).equals(p.getStatus())).count());
        r.put("archived", all.stream()
                .filter(p -> Integer.valueOf(EthicsTypeUtil.PROFILE_ARCHIVED).equals(p.getStatus())).count());
        r.put("byDept", statsByDept());

        // 已通过表彰数（全院）
        r.put("commendationTotal", commendationMapper.selectCount(new LambdaQueryWrapper<EthicsCommendation>()
                .eq(EthicsCommendation::getStatus, EthicsTypeUtil.APPROVAL_PASSED)));
        // 投诉总数（全院）
        r.put("complaintTotal", complaintMapper.selectCount(null));
        // 考评平均分与优秀率（全院，仅统计已通过的考评）
        List<EthicsEvaluation> passed = evaluationMapper.selectList(new LambdaQueryWrapper<EthicsEvaluation>()
                .eq(EthicsEvaluation::getStatus, EthicsTypeUtil.APPROVAL_PASSED));
        r.put("evaluationAvg", avgOf(passed));
        long excellent = passed.stream()
                .filter(e -> EthicsTypeUtil.GRADE_EXCELLENT.equals(e.getGrade())).count();
        r.put("excellentCount", excellent);
        r.put("excellentRate", rate(excellent, passed.size()));
        // 逾期未反馈投诉数
        r.put("overdueComplaints", complaintMapper.selectList(null).stream()
                .filter(c -> EthicsTypeUtil.isComplaintOverdue(c.getStatus(), c.getDeadlineDate())).count());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<EthicsProfile>> grouped = new LinkedHashMap<>();
        for (EthicsProfile p : this.list()) {
            String dept = StringUtils.hasText(p.getDeptName()) ? p.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("activeCount", list.stream()
                    .filter(p -> Integer.valueOf(EthicsTypeUtil.PROFILE_ACTIVE).equals(p.getStatus())).count());
            long excellent = list.stream()
                    .filter(p -> EthicsTypeUtil.GRADE_EXCELLENT.equals(p.getLastGrade())).count();
            row.put("excellentCount", excellent);
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public EthicsProfile detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    public void exportExcel(EthicsProfileQuery query, OutputStream out) {
        EthicsProfileExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<EthicsProfile> list = EthicsProfileExcelUtil.read(in);
        int ok = 0;
        for (EthicsProfile p : list) {
            if (!StringUtils.hasText(p.getPhysicianName())) {
                log.warn("导入跳过：医师姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(p.getArchiveNo())) {
                log.warn("导入跳过：档案编号为空的行（医师 {}）", p.getPhysicianName());
                continue;
            }
            // 同一档案编号已有记录，跳过
            if (this.count(new LambdaQueryWrapper<EthicsProfile>()
                    .eq(EthicsProfile::getArchiveNo, p.getArchiveNo())) > 0) {
                log.warn("导入跳过：重复档案编号 {}", p.getArchiveNo());
                continue;
            }
            // 同一医师已有档案，跳过
            if (p.getPhysicianId() != null && this.count(new LambdaQueryWrapper<EthicsProfile>()
                    .eq(EthicsProfile::getPhysicianId, p.getPhysicianId())) > 0) {
                log.warn("导入跳过：医师 {} 已有档案", p.getPhysicianName());
                continue;
            }
            // 按姓名查医师档案补全医师 id / 科室 / 职称
            fillPhysician(p);
            if (p.getPhysicianId() == null) {
                log.warn("导入跳过：医师 {} 在医师档案中不存在", p.getPhysicianName());
                continue;
            }
            if (this.count(new LambdaQueryWrapper<EthicsProfile>()
                    .eq(EthicsProfile::getPhysicianId, p.getPhysicianId())) > 0) {
                log.warn("导入跳过：医师 {} 已有档案", p.getPhysicianName());
                continue;
            }
            try {
                this.save(p);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", p.getPhysicianName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<EthicsProfile> buildWrapper(EthicsProfileQuery q) {
        LambdaQueryWrapper<EthicsProfile> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(EthicsProfile::getPhysicianName, q.getKeyword())
                .or().like(EthicsProfile::getArchiveNo, q.getKeyword()));
        qw.eq(q.getDeptId() != null, EthicsProfile::getDeptId, q.getDeptId());
        qw.eq(q.getStatus() != null, EthicsProfile::getStatus, q.getStatus());
        qw.eq(StringUtils.hasText(q.getLastGrade()), EthicsProfile::getLastGrade, q.getLastGrade());
        qw.orderByDesc(EthicsProfile::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验档案编号与医师唯一性 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EthicsProfile entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(EthicsProfile entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(EthicsProfile p, boolean isCreate) {
        if (!StringUtils.hasText(p.getArchiveNo())) {
            throw new IllegalArgumentException("档案编号不能为空");
        }
        if (p.getStatus() != null
                && !Integer.valueOf(EthicsTypeUtil.PROFILE_ACTIVE).equals(p.getStatus())
                && !Integer.valueOf(EthicsTypeUtil.PROFILE_ARCHIVED).equals(p.getStatus())) {
            throw new IllegalArgumentException("档案状态不正确");
        }
        if (StringUtils.hasText(p.getLastGrade())
                && EthicsTypeUtil.parseGrade(p.getLastGrade()) == null) {
            throw new IllegalArgumentException("最近考评等级不正确");
        }
        // 按姓名查医师档案补全医师 id / 科室 / 职称
        fillPhysician(p);
        if (p.getPhysicianId() == null) {
            throw new IllegalArgumentException("医师不存在：" + p.getPhysicianName());
        }
        if (isCreate) {
            if (p.getStatus() == null) {
                p.setStatus(EthicsTypeUtil.PROFILE_ACTIVE);
            }
            if (p.getEstablishDate() == null) {
                p.setEstablishDate(LocalDate.now());
            }
            // 同一医师只能建一份档案
            if (this.count(new LambdaQueryWrapper<EthicsProfile>()
                    .eq(EthicsProfile::getPhysicianId, p.getPhysicianId())) > 0) {
                throw new IllegalArgumentException("医师 " + p.getPhysicianName() + " 已有医德档案，请勿重复建档");
            }
            // 同一档案编号只能用一次
            if (this.count(new LambdaQueryWrapper<EthicsProfile>()
                    .eq(EthicsProfile::getArchiveNo, p.getArchiveNo())) > 0) {
                throw new IllegalArgumentException("档案编号 " + p.getArchiveNo() + " 已有记录，请勿重复建档");
            }
        } else {
            EthicsProfile old = getOrThrow(p.getId());
            // 改编号时不能与他人档案编号重复；改医师时不能与他人档案重复
            if (!old.getArchiveNo().equals(p.getArchiveNo()) && this.count(new LambdaQueryWrapper<EthicsProfile>()
                    .eq(EthicsProfile::getArchiveNo, p.getArchiveNo())) > 0) {
                throw new IllegalArgumentException("档案编号 " + p.getArchiveNo() + " 已有记录，请勿重复建档");
            }
            if (!old.getPhysicianId().equals(p.getPhysicianId()) && this.count(new LambdaQueryWrapper<EthicsProfile>()
                    .eq(EthicsProfile::getPhysicianId, p.getPhysicianId())) > 0) {
                throw new IllegalArgumentException("医师 " + p.getPhysicianName() + " 已有医德档案，请勿重复建档");
            }
        }
    }

    /** 按姓名查医师档案，补全医师 id / 科室 / 职称（查不到时保持原值） */
    private void fillPhysician(EthicsProfile p) {
        if (p.getPhysicianId() == null && StringUtils.hasText(p.getPhysicianName())) {
            Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                    .eq(Physician::getName, p.getPhysicianName()).last("LIMIT 1"));
            if (physician != null) {
                p.setPhysicianId(physician.getId());
            }
        }
        if (p.getPhysicianId() != null) {
            Physician physician = physicianMapper.selectById(p.getPhysicianId());
            if (physician != null) {
                if (!StringUtils.hasText(p.getPhysicianName())) {
                    p.setPhysicianName(physician.getName());
                }
                if (p.getDeptId() == null) {
                    p.setDeptId(physician.getDeptId());
                }
                if (!StringUtils.hasText(p.getDeptName())) {
                    p.setDeptName(physician.getDeptName());
                }
                if (!StringUtils.hasText(p.getTitle())) {
                    p.setTitle(physician.getTitle());
                }
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

    private EthicsProfile getOrThrow(Long id) {
        EthicsProfile p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("医德档案不存在：" + id);
        }
        return p;
    }
}
