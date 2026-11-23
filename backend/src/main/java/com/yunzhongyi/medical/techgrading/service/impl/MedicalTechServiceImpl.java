package com.yunzhongyi.medical.techgrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.techgrading.dto.MedicalTechQuery;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.entity.MedicalTech;
import com.yunzhongyi.medical.techgrading.entity.TechAdmission;
import com.yunzhongyi.medical.techgrading.entity.TechAuthorization;
import com.yunzhongyi.medical.techgrading.excel.MedicalTechExcelUtil;
import com.yunzhongyi.medical.techgrading.mapper.MedicalTechMapper;
import com.yunzhongyi.medical.techgrading.mapper.TechAdmissionMapper;
import com.yunzhongyi.medical.techgrading.mapper.TechAuthorizationMapper;
import com.yunzhongyi.medical.techgrading.service.MedicalTechService;
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
 * 医疗技术档案管理服务实现：档案 CRUD + 停用/重新启用 + 统计 + Excel 批量导入导出
 * <p>
 * 准入申请与授权统计直接读 TechAdmissionMapper / TechAuthorizationMapper，
 * 不依赖另外两个服务，避免互相注入形成循环依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalTechServiceImpl
        extends ServiceImpl<MedicalTechMapper, MedicalTech>
        implements MedicalTechService {

    private final TechAdmissionMapper techAdmissionMapper;
    private final TechAuthorizationMapper techAuthorizationMapper;

    @Override
    public PageResult<MedicalTech> pageQuery(MedicalTechQuery query) {
        Page<MedicalTech> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<MedicalTech> listByQuery(MedicalTechQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<MedicalTech> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(p -> Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_PENDING).equals(p.getStatus())).count());
        r.put("admitted", all.stream()
                .filter(p -> Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(p.getStatus())).count());
        r.put("suspended", all.stream()
                .filter(p -> Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_SUSPENDED).equals(p.getStatus())).count());
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (int l : List.of(TechGradingTypeUtil.LEVEL_ONE, TechGradingTypeUtil.LEVEL_TWO,
                TechGradingTypeUtil.LEVEL_THREE)) {
            byLevel.put(String.valueOf(l), all.stream().filter(p -> Integer.valueOf(l).equals(p.getTechLevel())).count());
        }
        r.put("byLevel", byLevel);
        Map<String, Long> byAccessType = new LinkedHashMap<>();
        for (String a : List.of(TechGradingTypeUtil.ACCESS_NORMAL, TechGradingTypeUtil.ACCESS_RESTRICTED,
                TechGradingTypeUtil.ACCESS_PROHIBITED)) {
            byAccessType.put(a, all.stream().filter(p -> a.equals(p.getAccessType())).count());
        }
        r.put("byAccessType", byAccessType);
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String c : List.of(TechGradingTypeUtil.CATEGORY_SURGICAL, TechGradingTypeUtil.CATEGORY_NON_SURGICAL,
                TechGradingTypeUtil.CATEGORY_TCM, TechGradingTypeUtil.CATEGORY_DIAGNOSTIC,
                TechGradingTypeUtil.CATEGORY_OTHER)) {
            byCategory.put(c, all.stream().filter(p -> c.equals(p.getCategory())).count());
        }
        r.put("byCategory", byCategory);
        r.put("byLevelList", statsByLevel());
        r.put("byCategoryList", statsByCategory());
        r.put("byDeptList", statsByDept());

        // 90 天内到期：已准入、未过期、有效期不晚于今天 + 90 天
        r.put("expiringSoon", all.stream()
                .filter(p -> TechGradingTypeUtil.isExpiringSoon(p.getStatus(), p.getExpiryDate())).count());
        // 已过期：已准入且有效期已过当天
        r.put("expired", all.stream()
                .filter(p -> TechGradingTypeUtil.isExpired(p.getStatus(), p.getExpiryDate())).count());

        // 准入申请统计
        List<TechAdmission> admissions = techAdmissionMapper.selectList(null);
        r.put("admissionTotal", (long) admissions.size());
        r.put("admissionPending", admissions.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_PENDING).equals(a.getStatus())).count());
        r.put("admissionApproved", admissions.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_APPROVED).equals(a.getStatus())).count());
        r.put("admissionRejected", admissions.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.ADMISSION_STATUS_REJECTED).equals(a.getStatus())).count());

        // 医师授权统计
        List<TechAuthorization> auths = techAuthorizationMapper.selectList(null);
        r.put("authTotal", (long) auths.size());
        r.put("authPending", auths.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_PENDING).equals(a.getStatus())).count());
        r.put("authApproved", auths.stream()
                .filter(a -> Integer.valueOf(TechGradingTypeUtil.AUTH_STATUS_APPROVED).equals(a.getStatus())).count());
        r.put("authExpiringSoon", auths.stream()
                .filter(a -> TechGradingTypeUtil.isExpiringSoon(a.getStatus(), a.getEndDate())).count());
        r.put("authExpired", auths.stream()
                .filter(a -> TechGradingTypeUtil.isExpired(a.getStatus(), a.getEndDate())).count());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByLevel() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int l : List.of(TechGradingTypeUtil.LEVEL_ONE, TechGradingTypeUtil.LEVEL_TWO,
                TechGradingTypeUtil.LEVEL_THREE)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("techLevel", l);
            row.put("techLevelText", TechGradingTypeUtil.levelText(l));
            row.put("count", this.count(new LambdaQueryWrapper<MedicalTech>()
                    .eq(MedicalTech::getTechLevel, l)));
            row.put("admittedCount", this.count(new LambdaQueryWrapper<MedicalTech>()
                    .eq(MedicalTech::getTechLevel, l)
                    .eq(MedicalTech::getStatus, TechGradingTypeUtil.TECH_STATUS_ADMITTED)));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByCategory() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String c : List.of(TechGradingTypeUtil.CATEGORY_SURGICAL, TechGradingTypeUtil.CATEGORY_NON_SURGICAL,
                TechGradingTypeUtil.CATEGORY_TCM, TechGradingTypeUtil.CATEGORY_DIAGNOSTIC,
                TechGradingTypeUtil.CATEGORY_OTHER)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", c);
            row.put("categoryText", TechGradingTypeUtil.categoryText(c));
            row.put("count", this.count(new LambdaQueryWrapper<MedicalTech>()
                    .eq(MedicalTech::getCategory, c)));
            row.put("admittedCount", this.count(new LambdaQueryWrapper<MedicalTech>()
                    .eq(MedicalTech::getCategory, c)
                    .eq(MedicalTech::getStatus, TechGradingTypeUtil.TECH_STATUS_ADMITTED)));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<MedicalTech>> grouped = new LinkedHashMap<>();
        for (MedicalTech p : this.list()) {
            if (!StringUtils.hasText(p.getDeptName())) {
                continue;
            }
            // 开展科室用顿号分隔，一项技术可计入多个科室
            for (String dept : p.getDeptName().split("、")) {
                String d = dept.trim();
                if (!d.isEmpty()) {
                    grouped.computeIfAbsent(d, k -> new ArrayList<>()).add(p);
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", e.getKey());
            row.put("count", (long) e.getValue().size());
            row.put("admittedCount", e.getValue().stream()
                    .filter(p -> Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(p.getStatus()))
                    .count());
            result.add(row);
        });
        return result;
    }

    @Override
    public MedicalTech detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalTech suspend(Long id, RejectRequest request) {
        MedicalTech p = getOrThrow(id);
        if (!Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(p.getStatus())) {
            throw new IllegalStateException("只有已准入的技术才能停用（当前状态："
                    + TechGradingTypeUtil.techStatusText(p.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("停用原因不能为空");
        }
        p.setStatus(TechGradingTypeUtil.TECH_STATUS_SUSPENDED);
        p.setSuspendReason(request.getReason().trim());
        p.setApprovalHistory(appendHistory(p.getApprovalHistory(),
                "医务科 " + (request.getApprover() == null ? "" : request.getApprover().trim())
                        + "：停用（" + p.getSuspendReason() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待准入/已停用能修改”守卫
        super.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicalTech resume(Long id) {
        MedicalTech p = getOrThrow(id);
        if (!Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_SUSPENDED).equals(p.getStatus())) {
            throw new IllegalStateException("只有已停用的技术才能重新启用（当前状态："
                    + TechGradingTypeUtil.techStatusText(p.getStatus()) + "）");
        }
        p.setStatus(TechGradingTypeUtil.TECH_STATUS_ADMITTED);
        p.setSuspendReason(null);
        p.setApprovalHistory(appendHistory(p.getApprovalHistory(), "医务科：重新启用"));
        super.updateById(p);
        return p;
    }

    @Override
    public void exportExcel(MedicalTechQuery query, OutputStream out) {
        MedicalTechExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<MedicalTech> list = MedicalTechExcelUtil.read(in);
        int ok = 0;
        for (MedicalTech p : list) {
            if (!StringUtils.hasText(p.getTechName()) || !StringUtils.hasText(p.getTechNo())) {
                log.warn("导入跳过：技术名称或技术编号为空的行");
                continue;
            }
            if (!StringUtils.hasText(p.getCategory()) || !TechGradingTypeUtil.isValidCategory(p.getCategory())) {
                log.warn("导入跳过：技术分类为空或不正确（技术 {}）", p.getTechName());
                continue;
            }
            if (!TechGradingTypeUtil.isValidLevel(p.getTechLevel())) {
                log.warn("导入跳过：技术分级为空或不正确（技术 {}），默认一级", p.getTechName());
                p.setTechLevel(TechGradingTypeUtil.LEVEL_ONE);
            }
            if (!StringUtils.hasText(p.getAccessType()) || !TechGradingTypeUtil.isValidAccessType(p.getAccessType())) {
                log.warn("导入跳过：准入类别为空或不正确（技术 {}），默认常规技术", p.getTechName());
                p.setAccessType(TechGradingTypeUtil.ACCESS_NORMAL);
            }
            if (!TechGradingTypeUtil.isValidRiskLevel(p.getRiskLevel())) {
                log.warn("导入跳过：风险等级不正确（技术 {}），默认低", p.getTechName());
                p.setRiskLevel(TechGradingTypeUtil.RISK_LOW);
            }
            // 导入的均为待准入，需走准入审批流程
            p.setStatus(TechGradingTypeUtil.TECH_STATUS_PENDING);
            p.setApprover(null);
            p.setAdmissionDate(null);
            p.setApprovalHistory(null);
            p.setSuspendReason(null);
            // 同一技术编号视为重复，跳过
            if (this.count(new LambdaQueryWrapper<MedicalTech>()
                    .eq(MedicalTech::getTechNo, p.getTechNo())) > 0) {
                log.warn("导入跳过：重复技术编号 {}", p.getTechNo());
                continue;
            }
            try {
                this.save(p);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", p.getTechName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除：已准入的档案不能直接删除，请先停用；删除时同时删除其准入申请与授权记录 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        MedicalTech p = getOrThrow((Long) id);
        if (Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(p.getStatus())) {
            throw new IllegalStateException("已准入的技术不能直接删除，请先停用");
        }
        techAdmissionMapper.delete(new LambdaQueryWrapper<TechAdmission>()
                .eq(TechAdmission::getTechId, p.getId()));
        techAuthorizationMapper.delete(new LambdaQueryWrapper<TechAuthorization>()
                .eq(TechAuthorization::getTechId, p.getId()));
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
    private LambdaQueryWrapper<MedicalTech> buildWrapper(MedicalTechQuery q) {
        LambdaQueryWrapper<MedicalTech> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(MedicalTech::getTechName, q.getKeyword())
                .or().like(MedicalTech::getTechNo, q.getKeyword()));
        qw.eq(StringUtils.hasText(q.getCategory()), MedicalTech::getCategory, q.getCategory());
        qw.eq(q.getTechLevel() != null, MedicalTech::getTechLevel, q.getTechLevel());
        qw.eq(StringUtils.hasText(q.getAccessType()), MedicalTech::getAccessType, q.getAccessType());
        qw.eq(StringUtils.hasText(q.getRiskLevel()), MedicalTech::getRiskLevel, q.getRiskLevel());
        qw.eq(q.getStatus() != null, MedicalTech::getStatus, q.getStatus());
        qw.like(StringUtils.hasText(q.getDeptName()), MedicalTech::getDeptName, q.getDeptName());
        if (Boolean.TRUE.equals(q.getExpiringSoon())) {
            // 90 天内到期：只看已准入的，有效期在今天 ~ 今天 + 90 天之间
            qw.eq(MedicalTech::getStatus, TechGradingTypeUtil.TECH_STATUS_ADMITTED);
            qw.ge(MedicalTech::getExpiryDate, LocalDate.now());
            qw.le(MedicalTech::getExpiryDate, LocalDate.now().plusDays(TechGradingTypeUtil.EXPIRY_WARN_DAYS));
        }
        if (Boolean.TRUE.equals(q.getExpired())) {
            // 已过期：只看已准入的，有效期已过当天
            qw.eq(MedicalTech::getStatus, TechGradingTypeUtil.TECH_STATUS_ADMITTED);
            qw.lt(MedicalTech::getExpiryDate, LocalDate.now());
        }
        qw.orderByDesc(MedicalTech::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验分类、分级、准入类别与日期 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(MedicalTech entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(MedicalTech entity) {
        // 已准入的档案不能直接修改（分级/科室/有效期变更走准入申请；停用走停用接口）
        MedicalTech old = getOrThrow(entity.getId());
        if (Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(old.getStatus())) {
            throw new IllegalStateException("已准入的技术不能直接修改（当前状态："
                    + TechGradingTypeUtil.techStatusText(old.getStatus())
                    + "），分级/科室/有效期变更请走准入申请，停用请走停用接口");
        }
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(MedicalTech p, boolean isCreate) {
        if (!StringUtils.hasText(p.getCategory()) || !TechGradingTypeUtil.isValidCategory(p.getCategory())) {
            throw new IllegalArgumentException("技术分类不正确");
        }
        if (!TechGradingTypeUtil.isValidLevel(p.getTechLevel())) {
            throw new IllegalArgumentException("技术分级不正确");
        }
        if (!StringUtils.hasText(p.getAccessType()) || !TechGradingTypeUtil.isValidAccessType(p.getAccessType())) {
            throw new IllegalArgumentException("准入类别不正确");
        }
        if (!TechGradingTypeUtil.isValidRiskLevel(p.getRiskLevel())) {
            throw new IllegalArgumentException("风险等级不正确");
        }
        if (p.getAdmissionDate() != null && p.getExpiryDate() != null
                && p.getExpiryDate().isBefore(p.getAdmissionDate())) {
            throw new IllegalArgumentException("有效期不能早于准入日期");
        }
        if (p.getStatus() != null
                && !Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_PENDING).equals(p.getStatus())
                && !Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_ADMITTED).equals(p.getStatus())
                && !Integer.valueOf(TechGradingTypeUtil.TECH_STATUS_SUSPENDED).equals(p.getStatus())) {
            throw new IllegalArgumentException("档案状态不正确");
        }
        if (isCreate) {
            if (p.getStatus() == null) {
                p.setStatus(TechGradingTypeUtil.TECH_STATUS_PENDING);
            }
            if (!StringUtils.hasText(p.getRiskLevel())) {
                p.setRiskLevel(TechGradingTypeUtil.RISK_LOW);
            }
            if (p.getEthicsRequired() == null) {
                p.setEthicsRequired(0);
            }
            // 同一技术编号已有记录，拒绝重复建档
            if (this.count(new LambdaQueryWrapper<MedicalTech>()
                    .eq(MedicalTech::getTechNo, p.getTechNo())) > 0) {
                throw new IllegalArgumentException("技术编号 " + p.getTechNo() + " 已有记录，请勿重复建档");
            }
        }
    }

    private String appendHistory(String history, String entry) {
        if (!StringUtils.hasText(history)) {
            return entry;
        }
        return history + "；" + entry;
    }

    private MedicalTech getOrThrow(Long id) {
        MedicalTech p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("医疗技术不存在：" + id);
        }
        return p;
    }
}
