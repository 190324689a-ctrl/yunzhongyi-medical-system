package com.yunzhongyi.medical.techgrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.techgrading.dto.ApproveRequest;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.dto.TechAdmissionQuery;
import com.yunzhongyi.medical.techgrading.entity.MedicalTech;
import com.yunzhongyi.medical.techgrading.entity.TechAdmission;
import com.yunzhongyi.medical.techgrading.excel.TechAdmissionExcelUtil;
import com.yunzhongyi.medical.techgrading.mapper.MedicalTechMapper;
import com.yunzhongyi.medical.techgrading.mapper.TechAdmissionMapper;
import com.yunzhongyi.medical.techgrading.service.TechAdmissionService;
import com.yunzhongyi.medical.techgrading.service.TechGradingTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医疗技术准入申请管理服务实现：申请 CRUD + 一级审批（通过时同步技术档案）+ 统计 + Excel 批量导入导出
 * <p>
 * 审批内部直接调 {@code super.updateById}，避免同一事务内 MyBatis 一级缓存
 * 导致修改守卫误判（与 MDT 模块同样处理）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechAdmissionServiceImpl
        extends ServiceImpl<TechAdmissionMapper, TechAdmission>
        implements TechAdmissionService {

    private final MedicalTechMapper medicalTechMapper;

    @Override
    public PageResult<TechAdmission> pageQuery(TechAdmissionQuery query) {
        Page<TechAdmission> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<TechAdmission> listByQuery(TechAdmissionQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<TechAdmission> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_PENDING).equals(a.getStatus())).count());
        r.put("approved", all.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_APPROVED).equals(a.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_REJECTED).equals(a.getStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(TechGradingTypeUtil.APPLY_NEW, TechGradingTypeUtil.APPLY_ADD_DEPT,
                TechGradingTypeUtil.APPLY_CHANGE_LEVEL, TechGradingTypeUtil.APPLY_RENEW)) {
            byType.put(t, all.stream().filter(a -> t.equals(a.getApplyType())).count());
        }
        r.put("byType", byType);
        return r;
    }

    @Override
    public List<TechAdmission> listByTech(Long techId) {
        return this.list(new LambdaQueryWrapper<TechAdmission>()
                .eq(TechAdmission::getTechId, techId)
                .orderByDesc(TechAdmission::getApplyDate)
                .orderByDesc(TechAdmission::getId));
    }

    @Override
    public TechAdmission detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechAdmission approve(Long id, ApproveRequest request) {
        TechAdmission a = getOrThrow(id);
        if (!Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_PENDING).equals(a.getStatus())) {
            throw new IllegalStateException("只有待审批的申请能通过（当前状态："
                    + TechGradingTypeUtil.admissionStatusText(a.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getApprover())) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        MedicalTech tech = medicalTechMapper.selectById(a.getTechId());
        if (tech == null) {
            throw new IllegalArgumentException("关联的医疗技术不存在：" + a.getTechId());
        }
        // 按申请类型同步技术档案
        LocalDate today = LocalDate.now();
        String opinion = StringUtils.hasText(request.getOpinion()) ? request.getOpinion().trim() : "同意";
        switch (a.getApplyType()) {
            case TechGradingTypeUtil.APPLY_NEW -> {
                if (Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(tech.getStatus())) {
                    throw new IllegalStateException("该技术已准入，无需重复准入");
                }
                if (TechGradingTypeUtil.ACCESS_PROHIBITED.equals(tech.getAccessType())) {
                    throw new IllegalStateException("禁止类技术不得准入");
                }
                tech.setStatus(TechGradingTypeUtil.TECH_STATUS_ADMITTED);
                tech.setAdmissionDate(today);
                if (tech.getExpiryDate() == null) {
                    tech.setExpiryDate(today.plusYears(TechGradingTypeUtil.DEFAULT_ADMISSION_YEARS));
                }
                tech.setApprover(request.getApprover().trim());
                tech.setApprovalHistory(appendHistory(tech.getApprovalHistory(),
                        "医务科 " + request.getApprover().trim() + "：准入通过（" + opinion + "）"));
                medicalTechMapper.updateById(tech);
            }
            case TechGradingTypeUtil.APPLY_ADD_DEPT -> {
                if (!Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(tech.getStatus())) {
                    throw new IllegalStateException("只有已准入的技术才能新增开展科室（当前状态："
                            + TechGradingTypeUtil.techStatusText(tech.getStatus()) + "）");
                }
                String merged = mergeDept(tech.getDeptName(), a.getNewDept());
                tech.setDeptName(merged);
                tech.setApprovalHistory(appendHistory(tech.getApprovalHistory(),
                        "医务科 " + request.getApprover().trim() + "：新增开展科室 " + a.getNewDept() + "（" + opinion + "）"));
                medicalTechMapper.updateById(tech);
            }
            case TechGradingTypeUtil.APPLY_CHANGE_LEVEL -> {
                if (!Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(tech.getStatus())) {
                    throw new IllegalStateException("只有已准入的技术才能变更分级（当前状态："
                            + TechGradingTypeUtil.techStatusText(tech.getStatus()) + "）");
                }
                tech.setTechLevel(a.getNewLevel());
                tech.setApprovalHistory(appendHistory(tech.getApprovalHistory(),
                        "医务科 " + request.getApprover().trim() + "：分级变更为 "
                                + TechGradingTypeUtil.levelText(a.getNewLevel()) + "（" + opinion + "）"));
                medicalTechMapper.updateById(tech);
            }
            case TechGradingTypeUtil.APPLY_RENEW -> {
                if (!Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(tech.getStatus())) {
                    throw new IllegalStateException("只有已准入的技术才能延续准入（当前状态："
                            + TechGradingTypeUtil.techStatusText(tech.getStatus()) + "）");
                }
                tech.setExpiryDate(a.getNewExpiryDate());
                tech.setApprovalHistory(appendHistory(tech.getApprovalHistory(),
                        "医务科 " + request.getApprover().trim() + "：延续准入，有效期至 "
                                + TechGradingTypeUtil.fmtDate(a.getNewExpiryDate()) + "（" + opinion + "）"));
                medicalTechMapper.updateById(tech);
            }
            default -> throw new IllegalArgumentException("申请类型不正确：" + a.getApplyType());
        }
        a.setApprover(request.getApprover().trim());
        a.setStatus(TechGradingTypeUtil.ADMISSION_STATUS_APPROVED);
        a.setApprovalHistory(appendHistory(a.getApprovalHistory(),
                "医务科 " + a.getApprover() + "：通过（" + opinion + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”守卫
        super.updateById(a);
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechAdmission reject(Long id, RejectRequest request) {
        TechAdmission a = getOrThrow(id);
        if (!Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_PENDING).equals(a.getStatus())) {
            throw new IllegalStateException("只有待审批的申请能驳回（当前状态："
                    + TechGradingTypeUtil.admissionStatusText(a.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        a.setApprover(request.getApprover() == null ? null : request.getApprover().trim());
        a.setStatus(TechGradingTypeUtil.ADMISSION_STATUS_REJECTED);
        a.setApprovalHistory(appendHistory(a.getApprovalHistory(),
                "医务科 " + (a.getApprover() == null ? "" : a.getApprover())
                        + "：驳回（" + request.getReason().trim() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”守卫
        super.updateById(a);
        return a;
    }

    @Override
    public void exportExcel(TechAdmissionQuery query, OutputStream out) {
        TechAdmissionExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<TechAdmission> list = TechAdmissionExcelUtil.read(in);
        int ok = 0;
        for (TechAdmission a : list) {
            if (!StringUtils.hasText(a.getTechName())) {
                log.warn("导入跳过：技术名称为空的行");
                continue;
            }
            if (!StringUtils.hasText(a.getApplicant())) {
                log.warn("导入跳过：申请人为空的行（技术 {}）", a.getTechName());
                continue;
            }
            if (!StringUtils.hasText(a.getApplyType())
                    || !TechGradingTypeUtil.isValidApplyType(a.getApplyType())) {
                log.warn("导入跳过：申请类型为空或不正确（技术 {}），默认新技术准入", a.getTechName());
                a.setApplyType(TechGradingTypeUtil.APPLY_NEW);
            }
            // 按技术编号/名称查技术档案补全关联；查不到的跳过
            MedicalTech tech = findTech(a);
            if (tech == null) {
                log.warn("导入跳过：技术 {} 在技术档案中不存在", a.getTechName());
                continue;
            }
            a.setTechId(tech.getId());
            a.setTechNo(tech.getTechNo());
            a.setTechName(tech.getTechName());
            // 同一技术同一申请类型同一申请日期已有记录，视为重复，跳过
            if (this.count(new LambdaQueryWrapper<TechAdmission>()
                    .eq(TechAdmission::getTechId, a.getTechId())
                    .eq(TechAdmission::getApplyType, a.getApplyType())
                    .eq(TechAdmission::getApplyDate, a.getApplyDate())) > 0) {
                log.warn("导入跳过：重复准入申请 {} {} {}", a.getTechName(), a.getApplyType(), a.getApplyDate());
                continue;
            }
            a.setStatus(TechGradingTypeUtil.ADMISSION_STATUS_PENDING);
            a.setApprover(null);
            a.setApprovalHistory(null);
            try {
                this.save(a);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", a.getTechName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除：已通过的准入申请不能直接删除（审批记录留痕） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        TechAdmission a = getOrThrow((Long) id);
        if (Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_APPROVED).equals(a.getStatus())) {
            throw new IllegalStateException("已通过的准入申请不能直接删除");
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

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<TechAdmission> buildWrapper(TechAdmissionQuery q) {
        LambdaQueryWrapper<TechAdmission> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(TechAdmission::getTechName, q.getKeyword())
                .or().like(TechAdmission::getTechNo, q.getKeyword())
                .or().like(TechAdmission::getApplicant, q.getKeyword()));
        qw.eq(q.getTechId() != null, TechAdmission::getTechId, q.getTechId());
        qw.eq(StringUtils.hasText(q.getApplyType()), TechAdmission::getApplyType, q.getApplyType());
        qw.like(StringUtils.hasText(q.getApplyDept()), TechAdmission::getApplyDept, q.getApplyDept());
        qw.eq(q.getStatus() != null, TechAdmission::getStatus, q.getStatus());
        qw.orderByDesc(TechAdmission::getId);
        return qw;
    }

    /** 新建时补默认值并校验：按申请类型校验必填项、关联技术档案 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TechAdmission entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TechAdmission entity) {
        // 只有待审批的申请能修改
        TechAdmission old = getOrThrow(entity.getId());
        if (!Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_PENDING).equals(old.getStatus())) {
            throw new IllegalStateException("只有待审批的申请能修改（当前状态："
                    + TechGradingTypeUtil.admissionStatusText(old.getStatus()) + "）");
        }
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(TechAdmission a, boolean isCreate) {
        if (!StringUtils.hasText(a.getApplyType())
                || !TechGradingTypeUtil.isValidApplyType(a.getApplyType())) {
            throw new IllegalArgumentException("申请类型不正确");
        }
        // 按申请类型校验必填项
        if (TechGradingTypeUtil.APPLY_CHANGE_LEVEL.equals(a.getApplyType())
                && !TechGradingTypeUtil.isValidLevel(a.getNewLevel())) {
            throw new IllegalArgumentException("变更分级时，变更后分级必填且须为一/二/三级");
        }
        if (TechGradingTypeUtil.APPLY_ADD_DEPT.equals(a.getApplyType())
                && !StringUtils.hasText(a.getNewDept())) {
            throw new IllegalArgumentException("新增开展科室时，新增科室必填");
        }
        if (TechGradingTypeUtil.APPLY_RENEW.equals(a.getApplyType())) {
            if (a.getNewExpiryDate() == null) {
                throw new IllegalArgumentException("延续准入时，延续后有效期必填");
            }
            if (a.getNewExpiryDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("延续后有效期不能早于当天");
            }
        }
        if (a.getStatus() != null
                && !Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_PENDING).equals(a.getStatus())
                && !Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_APPROVED).equals(a.getStatus())
                && !Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_REJECTED).equals(a.getStatus())) {
            throw new IllegalArgumentException("申请状态不正确");
        }
        // 关联技术档案：按 id 查不到时按编号/名称查，补全编号与名称
        MedicalTech tech = null;
        if (a.getTechId() != null) {
            tech = medicalTechMapper.selectById(a.getTechId());
        }
        if (tech == null) {
            tech = findTech(a);
        }
        if (tech == null) {
            throw new IllegalArgumentException("关联的医疗技术不存在："
                    + (StringUtils.hasText(a.getTechNo()) ? a.getTechNo() : a.getTechName()));
        }
        a.setTechId(tech.getId());
        a.setTechNo(tech.getTechNo());
        a.setTechName(tech.getTechName());
        if (isCreate) {
            if (a.getStatus() == null) {
                a.setStatus(TechGradingTypeUtil.ADMISSION_STATUS_PENDING);
            }
            if (a.getApplyDate() == null) {
                a.setApplyDate(LocalDate.now());
            }
            // 同一技术同一申请类型已有待审批记录，拒绝重复申请
            if (this.count(new LambdaQueryWrapper<TechAdmission>()
                    .eq(TechAdmission::getTechId, a.getTechId())
                    .eq(TechAdmission::getApplyType, a.getApplyType())
                    .eq(TechAdmission::getStatus, TechGradingTypeUtil.ADMISSION_STATUS_PENDING)) > 0) {
                throw new IllegalArgumentException("该技术已有待审批的"
                        + TechGradingTypeUtil.applyTypeText(a.getApplyType()) + "申请，请勿重复申请");
            }
        }
    }

    /** 按技术编号优先、再按技术名称查技术档案 */
    private MedicalTech findTech(TechAdmission a) {
        if (StringUtils.hasText(a.getTechNo())) {
            MedicalTech t = medicalTechMapper.selectOne(new LambdaQueryWrapper<MedicalTech>()
                    .eq(MedicalTech::getTechNo, a.getTechNo().trim()).last("LIMIT 1"));
            if (t != null) {
                return t;
            }
        }
        if (StringUtils.hasText(a.getTechName())) {
            return medicalTechMapper.selectOne(new LambdaQueryWrapper<MedicalTech>()
                    .eq(MedicalTech::getTechName, a.getTechName().trim()).last("LIMIT 1"));
        }
        return null;
    }

    /** 开展科室合并：新科室追加进原科室列表，已有的科室不重复追加 */
    private String mergeDept(String oldDept, String newDept) {
        if (!StringUtils.hasText(newDept)) {
            return oldDept;
        }
        String nd = newDept.trim();
        if (!StringUtils.hasText(oldDept)) {
            return nd;
        }
        List<String> parts = new ArrayList<>(List.of(oldDept.split("、")));
        if (parts.stream().noneMatch(d -> d.trim().equals(nd))) {
            parts.add(nd);
        }
        return String.join("、", parts);
    }

    private String appendHistory(String history, String entry) {
        if (!StringUtils.hasText(history)) {
            return entry;
        }
        return history + "；" + entry;
    }

    private TechAdmission getOrThrow(Long id) {
        TechAdmission a = this.getById(id);
        if (a == null) {
            throw new IllegalArgumentException("准入申请不存在：" + id);
        }
        return a;
    }
}
