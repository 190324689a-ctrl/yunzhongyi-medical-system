package com.yunzhongyi.medical.techgrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.mapper.PhysicianMapper;
import com.yunzhongyi.medical.techgrading.dto.ApproveRequest;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.dto.TechAuthorizationQuery;
import com.yunzhongyi.medical.techgrading.entity.MedicalTech;
import com.yunzhongyi.medical.techgrading.entity.TechAuthorization;
import com.yunzhongyi.medical.techgrading.excel.TechAuthorizationExcelUtil;
import com.yunzhongyi.medical.techgrading.mapper.MedicalTechMapper;
import com.yunzhongyi.medical.techgrading.mapper.TechAuthorizationMapper;
import com.yunzhongyi.medical.techgrading.service.TechAuthorizationService;
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
 * 医师医疗技术授权管理服务实现：授权 CRUD + 一级审批 + 注销 + 统计 + Excel 批量导入导出
 * <p>
 * 审批内部直接调 {@code super.updateById}，避免同一事务内 MyBatis 一级缓存
 * 导致修改守卫误判（与 MDT 模块同样处理）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechAuthorizationServiceImpl
        extends ServiceImpl<TechAuthorizationMapper, TechAuthorization>
        implements TechAuthorizationService {

    private final MedicalTechMapper medicalTechMapper;
    private final PhysicianMapper physicianMapper;

    @Override
    public PageResult<TechAuthorization> pageQuery(TechAuthorizationQuery query) {
        Page<TechAuthorization> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<TechAuthorization> listByQuery(TechAuthorizationQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<TechAuthorization> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_PENDING).equals(a.getStatus())).count());
        r.put("approved", all.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_APPROVED).equals(a.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_REJECTED).equals(a.getStatus())).count());
        r.put("revoked", all.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_REVOKED).equals(a.getStatus())).count());
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (int l : List.of(TechGradingTypeUtil.LEVEL_ONE, TechGradingTypeUtil.LEVEL_TWO,
                TechGradingTypeUtil.LEVEL_THREE)) {
            byLevel.put(String.valueOf(l), all.stream().filter(a -> Integer.valueOf(l).equals(a.getAuthLevel())).count());
        }
        r.put("byLevel", byLevel);
        r.put("byDeptList", statsByDept());

        // 90 天内到期：已通过、未过期、授权结束日期不晚于今天 + 90 天
        r.put("expiringSoon", all.stream()
                .filter(a -> TechGradingTypeUtil.isExpiringSoon(a.getStatus(), a.getEndDate())).count());
        // 已过期：已通过且授权结束日期已过当天
        r.put("expired", all.stream()
                .filter(a -> TechGradingTypeUtil.isExpired(a.getStatus(), a.getEndDate())).count());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<TechAuthorization>> grouped = new LinkedHashMap<>();
        for (TechAuthorization a : this.list()) {
            if (!StringUtils.hasText(a.getDeptName())) {
                continue;
            }
            grouped.computeIfAbsent(a.getDeptName(), k -> new ArrayList<>()).add(a);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", e.getKey());
            row.put("count", (long) e.getValue().size());
            row.put("approvedCount", e.getValue().stream()
                    .filter(a -> Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_APPROVED).equals(a.getStatus()))
                    .count());
            result.add(row);
        });
        return result;
    }

    @Override
    public List<TechAuthorization> listByTech(Long techId) {
        return this.list(new LambdaQueryWrapper<TechAuthorization>()
                .eq(TechAuthorization::getTechId, techId)
                .orderByDesc(TechAuthorization::getStartDate)
                .orderByDesc(TechAuthorization::getId));
    }

    @Override
    public List<TechAuthorization> listByPhysician(Long physicianId) {
        return this.list(new LambdaQueryWrapper<TechAuthorization>()
                .eq(TechAuthorization::getPhysicianId, physicianId)
                .orderByDesc(TechAuthorization::getStartDate)
                .orderByDesc(TechAuthorization::getId));
    }

    @Override
    public TechAuthorization detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechAuthorization approve(Long id, ApproveRequest request) {
        TechAuthorization a = getOrThrow(id);
        if (!Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_PENDING).equals(a.getStatus())) {
            throw new IllegalStateException("只有待审批的授权能通过（当前状态："
                    + TechGradingTypeUtil.authStatusText(a.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getApprover())) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        // 通过时再次校验：技术仍已准入、非禁止类、授权级别不高于技术分级
        MedicalTech tech = medicalTechMapper.selectById(a.getTechId());
        if (tech == null) {
            throw new IllegalArgumentException("关联的医疗技术不存在：" + a.getTechId());
        }
        validateAgainstTech(tech, a.getAuthLevel());
        String opinion = StringUtils.hasText(request.getOpinion()) ? request.getOpinion().trim() : "同意";
        a.setApprover(request.getApprover().trim());
        a.setStatus(TechGradingTypeUtil.AUTH_STATUS_APPROVED);
        a.setApprovalHistory(appendHistory(a.getApprovalHistory(),
                "医务科 " + a.getApprover() + "：通过（" + opinion + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”守卫
        super.updateById(a);
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechAuthorization reject(Long id, RejectRequest request) {
        TechAuthorization a = getOrThrow(id);
        if (!Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_PENDING).equals(a.getStatus())) {
            throw new IllegalStateException("只有待审批的授权能驳回（当前状态："
                    + TechGradingTypeUtil.authStatusText(a.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        a.setApprover(request.getApprover() == null ? null : request.getApprover().trim());
        a.setStatus(TechGradingTypeUtil.AUTH_STATUS_REJECTED);
        a.setApprovalHistory(appendHistory(a.getApprovalHistory(),
                "医务科 " + (a.getApprover() == null ? "" : a.getApprover())
                        + "：驳回（" + request.getReason().trim() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”守卫
        super.updateById(a);
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechAuthorization revoke(Long id, RejectRequest request) {
        TechAuthorization a = getOrThrow(id);
        if (!Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_APPROVED).equals(a.getStatus())) {
            throw new IllegalStateException("只有已通过的授权能注销（当前状态："
                    + TechGradingTypeUtil.authStatusText(a.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("注销原因不能为空");
        }
        a.setStatus(TechGradingTypeUtil.AUTH_STATUS_REVOKED);
        a.setRevokeReason(request.getReason().trim());
        a.setApprovalHistory(appendHistory(a.getApprovalHistory(),
                "医务科 " + (request.getApprover() == null ? "" : request.getApprover().trim())
                        + "：注销（" + a.getRevokeReason() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”守卫
        super.updateById(a);
        return a;
    }

    @Override
    public void exportExcel(TechAuthorizationQuery query, OutputStream out) {
        TechAuthorizationExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<TechAuthorization> list = TechAuthorizationExcelUtil.read(in);
        int ok = 0;
        for (TechAuthorization a : list) {
            if (!StringUtils.hasText(a.getPhysicianName())) {
                log.warn("导入跳过：医师姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(a.getTechName())) {
                log.warn("导入跳过：技术名称为空的行（医师 {}）", a.getPhysicianName());
                continue;
            }
            if (!TechGradingTypeUtil.isValidLevel(a.getAuthLevel())) {
                log.warn("导入跳过：授权级别为空或不正确（医师 {} 技术 {}）", a.getPhysicianName(), a.getTechName());
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
            // 按技术编号/名称查技术档案补全（技术分级快照）；查不到的跳过
            MedicalTech tech = findTech(a);
            if (tech == null) {
                log.warn("导入跳过：技术 {} 在技术档案中不存在", a.getTechName());
                continue;
            }
            a.setTechId(tech.getId());
            a.setTechNo(tech.getTechNo());
            a.setTechName(tech.getTechName());
            a.setTechLevel(tech.getTechLevel());
            // 导入的均为待审批
            a.setStatus(TechGradingTypeUtil.AUTH_STATUS_PENDING);
            a.setApprover(null);
            a.setApprovalHistory(null);
            a.setRevokeReason(null);
            // 同一医师同一技术同一开始日期已有记录，视为重复，跳过
            if (this.count(new LambdaQueryWrapper<TechAuthorization>()
                    .eq(TechAuthorization::getPhysicianId, a.getPhysicianId())
                    .eq(TechAuthorization::getTechId, a.getTechId())
                    .eq(TechAuthorization::getStartDate, a.getStartDate())) > 0) {
                log.warn("导入跳过：重复授权 {} {} {}", a.getPhysicianName(), a.getTechName(), a.getStartDate());
                continue;
            }
            try {
                this.save(a);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{} {}（{}）", a.getPhysicianName(), a.getTechName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除：已通过的授权不能直接删除，请先注销 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        TechAuthorization a = getOrThrow((Long) id);
        if (Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_APPROVED).equals(a.getStatus())) {
            throw new IllegalStateException("已通过的授权不能直接删除，请先注销");
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
    private LambdaQueryWrapper<TechAuthorization> buildWrapper(TechAuthorizationQuery q) {
        LambdaQueryWrapper<TechAuthorization> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(TechAuthorization::getPhysicianName, q.getKeyword())
                .or().like(TechAuthorization::getTechName, q.getKeyword())
                .or().like(TechAuthorization::getTechNo, q.getKeyword()));
        qw.eq(q.getTechId() != null, TechAuthorization::getTechId, q.getTechId());
        qw.eq(q.getPhysicianId() != null, TechAuthorization::getPhysicianId, q.getPhysicianId());
        qw.eq(q.getAuthLevel() != null, TechAuthorization::getAuthLevel, q.getAuthLevel());
        qw.eq(q.getStatus() != null, TechAuthorization::getStatus, q.getStatus());
        qw.like(StringUtils.hasText(q.getDeptName()), TechAuthorization::getDeptName, q.getDeptName());
        if (Boolean.TRUE.equals(q.getExpiringSoon())) {
            // 90 天内到期：只看已通过的，授权结束日期在今天 ~ 今天 + 90 天之间
            qw.eq(TechAuthorization::getStatus, TechGradingTypeUtil.AUTH_STATUS_APPROVED);
            qw.ge(TechAuthorization::getEndDate, LocalDate.now());
            qw.le(TechAuthorization::getEndDate, LocalDate.now().plusDays(TechGradingTypeUtil.EXPIRY_WARN_DAYS));
        }
        if (Boolean.TRUE.equals(q.getExpired())) {
            // 已过期：只看已通过的，授权结束日期已过当天
            qw.eq(TechAuthorization::getStatus, TechGradingTypeUtil.AUTH_STATUS_APPROVED);
            qw.lt(TechAuthorization::getEndDate, LocalDate.now());
        }
        qw.orderByDesc(TechAuthorization::getId);
        return qw;
    }

    /** 新建时补默认值并校验：技术须已准入、非禁止类、授权级别不高于技术分级、日期合法、去重 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TechAuthorization entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TechAuthorization entity) {
        // 只有待审批的授权能修改
        TechAuthorization old = getOrThrow(entity.getId());
        if (!Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_PENDING).equals(old.getStatus())) {
            throw new IllegalStateException("只有待审批的授权能修改（当前状态："
                    + TechGradingTypeUtil.authStatusText(old.getStatus()) + "）");
        }
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(TechAuthorization a, boolean isCreate) {
        if (!TechGradingTypeUtil.isValidLevel(a.getAuthLevel())) {
            throw new IllegalArgumentException("授权级别不正确");
        }
        if (a.getStartDate() != null && a.getEndDate() != null
                && a.getEndDate().isBefore(a.getStartDate())) {
            throw new IllegalArgumentException("授权结束日期不能早于开始日期");
        }
        if (a.getStatus() != null
                && !Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_PENDING).equals(a.getStatus())
                && !Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_APPROVED).equals(a.getStatus())
                && !Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_REJECTED).equals(a.getStatus())
                && !Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_REVOKED).equals(a.getStatus())) {
            throw new IllegalArgumentException("授权状态不正确");
        }
        // 关联技术档案：按 id 查不到时按编号/名称查，补全编号、名称与技术分级快照
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
        a.setTechLevel(tech.getTechLevel());
        validateAgainstTech(tech, a.getAuthLevel());
        // 关联医师档案：按 id 或姓名查医师档案，补全医师 id 与科室
        Physician physician = null;
        if (a.getPhysicianId() != null) {
            physician = physicianMapper.selectById(a.getPhysicianId());
        }
        if (physician == null && StringUtils.hasText(a.getPhysicianName())) {
            physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                    .eq(Physician::getName, a.getPhysicianName().trim()).last("LIMIT 1"));
        }
        if (physician != null) {
            if (a.getPhysicianId() == null) {
                a.setPhysicianId(physician.getId());
            }
            if (a.getDeptId() == null) {
                a.setDeptId(physician.getDeptId());
            }
            if (!StringUtils.hasText(a.getDeptName())) {
                a.setDeptName(physician.getDeptName());
            }
        }
        if (isCreate) {
            if (a.getStatus() == null) {
                a.setStatus(TechGradingTypeUtil.AUTH_STATUS_PENDING);
            }
            if (a.getStartDate() == null) {
                a.setStartDate(LocalDate.now());
            }
            // 同一医师同一技术已有待审批或已通过的授权，拒绝重复申请
            if (a.getPhysicianId() != null && this.count(new LambdaQueryWrapper<TechAuthorization>()
                    .eq(TechAuthorization::getPhysicianId, a.getPhysicianId())
                    .eq(TechAuthorization::getTechId, a.getTechId())
                    .in(TechAuthorization::getStatus,
                            TechGradingTypeUtil.AUTH_STATUS_PENDING,
                            TechGradingTypeUtil.AUTH_STATUS_APPROVED)) > 0) {
                throw new IllegalArgumentException("该医师已有该技术的待审批或已通过授权，请勿重复申请");
            }
        }
    }

    /** 授权与技术的校验：技术须已准入、非禁止类、授权级别不高于技术分级 */
    private void validateAgainstTech(MedicalTech tech, Integer authLevel) {
        if (!Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(tech.getStatus())) {
            throw new IllegalStateException("该技术尚未准入或已停用，不能授权（当前状态："
                    + TechGradingTypeUtil.techStatusText(tech.getStatus()) + "）");
        }
        if (TechGradingTypeUtil.ACCESS_PROHIBITED.equals(tech.getAccessType())) {
            throw new IllegalStateException("禁止类技术不得授权");
        }
        if (authLevel != null && tech.getTechLevel() != null && authLevel > tech.getTechLevel()) {
            throw new IllegalArgumentException("授权级别（" + TechGradingTypeUtil.levelText(authLevel)
                    + "）不能高于技术分级（" + TechGradingTypeUtil.levelText(tech.getTechLevel()) + "）");
        }
    }

    /** 按技术编号优先、再按技术名称查技术档案 */
    private MedicalTech findTech(TechAuthorization a) {
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

    private String appendHistory(String history, String entry) {
        if (!StringUtils.hasText(history)) {
            return entry;
        }
        return history + "；" + entry;
    }

    private TechAuthorization getOrThrow(Long id) {
        TechAuthorization a = this.getById(id);
        if (a == null) {
            throw new IllegalArgumentException("技术授权不存在：" + id);
        }
        return a;
    }
}
