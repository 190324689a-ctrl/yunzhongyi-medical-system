package com.yunzhongyi.medical.radiation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.radiation.dto.RadiationPersonnelQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationHealth;
import com.yunzhongyi.medical.radiation.entity.RadiationPersonnel;
import com.yunzhongyi.medical.radiation.entity.RadiationQualification;
import com.yunzhongyi.medical.radiation.entity.RadiationTraining;
import com.yunzhongyi.medical.radiation.excel.RadiationPersonnelExcelUtil;
import com.yunzhongyi.medical.radiation.mapper.RadiationHealthMapper;
import com.yunzhongyi.medical.radiation.mapper.RadiationPersonnelMapper;
import com.yunzhongyi.medical.radiation.mapper.RadiationQualificationMapper;
import com.yunzhongyi.medical.radiation.mapper.RadiationTrainingMapper;
import com.yunzhongyi.medical.radiation.service.RadiationPersonnelService;
import com.yunzhongyi.medical.radiation.service.RadiationTypeUtil;
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
 * 放射人员档案管理服务实现：档案 CRUD + 四表聚合统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RadiationPersonnelServiceImpl
        extends ServiceImpl<RadiationPersonnelMapper, RadiationPersonnel>
        implements RadiationPersonnelService {

    private final RadiationQualificationMapper qualificationMapper;
    private final RadiationTrainingMapper trainingMapper;
    private final RadiationHealthMapper healthMapper;

    @Override
    public PageResult<RadiationPersonnel> pageQuery(RadiationPersonnelQuery query) {
        Page<RadiationPersonnel> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<RadiationPersonnel> listByQuery(RadiationPersonnelQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<RadiationPersonnel> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("active", all.stream()
                .filter(p -> Integer.valueOf(RadiationTypeUtil.STATUS_ACTIVE).equals(p.getStatus())).count());
        r.put("suspended", all.stream()
                .filter(p -> Integer.valueOf(RadiationTypeUtil.STATUS_SUSPENDED).equals(p.getStatus())).count());
        r.put("left", all.stream()
                .filter(p -> Integer.valueOf(RadiationTypeUtil.STATUS_LEFT).equals(p.getStatus())).count());
        Map<String, Long> byWorkType = new LinkedHashMap<>();
        for (String t : List.of(RadiationTypeUtil.WORK_DIAGNOSIS, RadiationTypeUtil.WORK_INTERVENTION,
                RadiationTypeUtil.WORK_NUCLEAR_MED, RadiationTypeUtil.WORK_RADIOTHERAPY)) {
            byWorkType.put(t, all.stream().filter(p -> t.equals(p.getWorkType())).count());
        }
        r.put("byWorkType", byWorkType);
        r.put("byDept", statsByDept());

        // 资质统计
        List<RadiationQualification> quals = qualificationMapper.selectList(null);
        LocalDate today = LocalDate.now();
        LocalDate warnDate = today.plusDays(RadiationTypeUtil.EXPIRY_WARN_DAYS);
        r.put("qualTotal", (long) quals.size());
        r.put("qualPending", quals.stream()
                .filter(q -> Integer.valueOf(RadiationTypeUtil.APPROVE_PENDING).equals(q.getStatus())).count());
        r.put("qualApproved", quals.stream()
                .filter(q -> Integer.valueOf(RadiationTypeUtil.APPROVE_APPROVED).equals(q.getStatus())).count());
        r.put("qualRejected", quals.stream()
                .filter(q -> Integer.valueOf(RadiationTypeUtil.APPROVE_REJECTED).equals(q.getStatus())).count());
        // 90 天内到期：已通过且有效期在今天~90 天后之间（含已过期，有效期 < 今天）
        r.put("qualExpiringSoon", quals.stream()
                .filter(q -> Integer.valueOf(RadiationTypeUtil.APPROVE_APPROVED).equals(q.getStatus()))
                .filter(q -> q.getExpiryDate() != null && !q.getExpiryDate().isAfter(warnDate)).count());
        r.put("qualExpired", quals.stream()
                .filter(q -> Integer.valueOf(RadiationTypeUtil.APPROVE_APPROVED).equals(q.getStatus()))
                .filter(q -> q.getExpiryDate() != null && q.getExpiryDate().isBefore(today)).count());

        // 培训统计
        List<RadiationTraining> trainings = trainingMapper.selectList(null);
        r.put("trainingTotal", (long) trainings.size());
        r.put("trainingPassed", trainings.stream()
                .filter(t -> RadiationTypeUtil.TRAINING_PASS.equals(t.getResult())).count());
        r.put("trainingFailed", trainings.stream()
                .filter(t -> RadiationTypeUtil.TRAINING_FAIL.equals(t.getResult())).count());

        // 健康统计
        List<RadiationHealth> healths = healthMapper.selectList(null);
        r.put("healthTotal", (long) healths.size());
        List<BigDecimal> doses = healths.stream()
                .map(RadiationHealth::getDose).filter(d -> d != null).toList();
        BigDecimal avgDose = doses.isEmpty() ? BigDecimal.ZERO
                : doses.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(doses.size()), 2, RoundingMode.HALF_UP);
        r.put("avgDose", avgDose);
        r.put("overLimitCount", healths.stream()
                .filter(h -> RadiationTypeUtil.isDoseOverLimit(h.getDose())).count());
        r.put("reviewCount", healths.stream()
                .filter(h -> RadiationTypeUtil.EXAM_REVIEW.equals(h.getResult())).count());
        r.put("unfitCount", healths.stream()
                .filter(h -> RadiationTypeUtil.EXAM_UNFIT.equals(h.getResult())).count());
        r.put("overdueExamCount", overdueExamCount(all, healths, today));
        return r;
    }

    /**
     * 体检逾期人数：在岗人员中，最近一次体检超过 365 天（含从未体检）的人数。
     * 体检有效期按一年计，逾期即应安排在岗期间体检。
     */
    private long overdueExamCount(List<RadiationPersonnel> all, List<RadiationHealth> healths, LocalDate today) {
        Map<Long, LocalDate> latestExam = new LinkedHashMap<>();
        for (RadiationHealth h : healths) {
            if (h.getPersonnelId() == null || h.getExamDate() == null) {
                continue;
            }
            latestExam.merge(h.getPersonnelId(), h.getExamDate(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }
        LocalDate deadline = today.minusDays(365);
        return all.stream()
                .filter(p -> Integer.valueOf(RadiationTypeUtil.STATUS_ACTIVE).equals(p.getStatus()))
                .filter(p -> {
                    LocalDate latest = latestExam.get(p.getId());
                    return latest == null || latest.isBefore(deadline);
                }).count();
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<RadiationPersonnel>> grouped = new LinkedHashMap<>();
        for (RadiationPersonnel p : this.list()) {
            String dept = StringUtils.hasText(p.getDeptName()) ? p.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(p);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("activeCount", list.stream()
                    .filter(p -> Integer.valueOf(RadiationTypeUtil.STATUS_ACTIVE).equals(p.getStatus())).count());
            row.put("suspendedCount", list.stream()
                    .filter(p -> Integer.valueOf(RadiationTypeUtil.STATUS_SUSPENDED).equals(p.getStatus())).count());
            row.put("leftCount", list.stream()
                    .filter(p -> Integer.valueOf(RadiationTypeUtil.STATUS_LEFT).equals(p.getStatus())).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByWorkType() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String t : List.of(RadiationTypeUtil.WORK_DIAGNOSIS, RadiationTypeUtil.WORK_INTERVENTION,
                RadiationTypeUtil.WORK_NUCLEAR_MED, RadiationTypeUtil.WORK_RADIOTHERAPY)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workType", t);
            row.put("workTypeText", RadiationTypeUtil.workTypeText(t));
            row.put("count", this.count(new LambdaQueryWrapper<RadiationPersonnel>()
                    .eq(RadiationPersonnel::getWorkType, t)));
            result.add(row);
        }
        return result;
    }

    @Override
    public RadiationPersonnel detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    public void exportExcel(RadiationPersonnelQuery query, OutputStream out) {
        RadiationPersonnelExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<RadiationPersonnel> list = RadiationPersonnelExcelUtil.read(in);
        int ok = 0;
        for (RadiationPersonnel p : list) {
            if (!StringUtils.hasText(p.getName())) {
                log.warn("导入跳过：姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(p.getWorkType()) || !RadiationTypeUtil.isValidWorkType(p.getWorkType())) {
                log.warn("导入跳过：放射类别为空或不正确（人员 {}）", p.getName());
                continue;
            }
            if (p.getStatus() == null) {
                p.setStatus(RadiationTypeUtil.STATUS_ACTIVE);
            }
            // 同一姓名同一科室视为重复，跳过
            LambdaQueryWrapper<RadiationPersonnel> qw = new LambdaQueryWrapper<RadiationPersonnel>()
                    .eq(RadiationPersonnel::getName, p.getName());
            if (StringUtils.hasText(p.getDeptName())) {
                qw.eq(RadiationPersonnel::getDeptName, p.getDeptName());
            }
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复放射人员 {} {}", p.getName(), p.getDeptName());
                continue;
            }
            this.save(p);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<RadiationPersonnel> buildWrapper(RadiationPersonnelQuery q) {
        LambdaQueryWrapper<RadiationPersonnel> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(RadiationPersonnel::getName, q.getKeyword())
                .or().like(RadiationPersonnel::getLicenseNo, q.getKeyword()));
        qw.eq(q.getDeptId() != null, RadiationPersonnel::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getWorkType()), RadiationPersonnel::getWorkType, q.getWorkType());
        qw.eq(q.getStatus() != null, RadiationPersonnel::getStatus, q.getStatus());
        qw.eq(q.getPhysicianId() != null, RadiationPersonnel::getPhysicianId, q.getPhysicianId());
        qw.orderByDesc(RadiationPersonnel::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验放射类别与在岗状态 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(RadiationPersonnel entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(RadiationPersonnel entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(RadiationPersonnel p, boolean isCreate) {
        if (!StringUtils.hasText(p.getWorkType()) || !RadiationTypeUtil.isValidWorkType(p.getWorkType())) {
            throw new IllegalArgumentException("放射类别不正确");
        }
        if (p.getStatus() != null
                && !Integer.valueOf(RadiationTypeUtil.STATUS_ACTIVE).equals(p.getStatus())
                && !Integer.valueOf(RadiationTypeUtil.STATUS_SUSPENDED).equals(p.getStatus())
                && !Integer.valueOf(RadiationTypeUtil.STATUS_LEFT).equals(p.getStatus())) {
            throw new IllegalArgumentException("在岗状态不正确");
        }
        if (p.getPermitExpiry() != null && p.getEntryDate() != null
                && p.getPermitExpiry().isBefore(p.getEntryDate())) {
            throw new IllegalArgumentException("放射诊疗许可证有效期不能早于从事放射工作日期");
        }
        if (isCreate) {
            if (p.getStatus() == null) {
                p.setStatus(RadiationTypeUtil.STATUS_ACTIVE);
            }
            // 同一姓名同一科室的档案已存在，拒绝重复建档
            LambdaQueryWrapper<RadiationPersonnel> qw = new LambdaQueryWrapper<RadiationPersonnel>()
                    .eq(RadiationPersonnel::getName, p.getName());
            if (StringUtils.hasText(p.getDeptName())) {
                qw.eq(RadiationPersonnel::getDeptName, p.getDeptName());
            }
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("该人员在本科室已有放射人员档案，请勿重复建档");
            }
        }
    }

    private RadiationPersonnel getOrThrow(Long id) {
        RadiationPersonnel p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("放射人员不存在：" + id);
        }
        return p;
    }
}
