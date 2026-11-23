package com.yunzhongyi.medical.policy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.policy.dto.AbolishRequest;
import com.yunzhongyi.medical.policy.dto.PolicyQuery;
import com.yunzhongyi.medical.policy.dto.PublishRequest;
import com.yunzhongyi.medical.policy.entity.Policy;
import com.yunzhongyi.medical.policy.entity.PolicyReading;
import com.yunzhongyi.medical.policy.excel.PolicyExcelUtil;
import com.yunzhongyi.medical.policy.mapper.PolicyMapper;
import com.yunzhongyi.medical.policy.mapper.PolicyReadingMapper;
import com.yunzhongyi.medical.policy.service.PolicyService;
import com.yunzhongyi.medical.policy.service.PolicyTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 政策法规管理服务实现：法规 CRUD + 发布/废止/新建版本 + 统计 + Excel 批量导入导出 + 文本下载
 * <p>
 * 查阅学习统计直接读 PolicyReadingMapper，不依赖 PolicyReadingService，
 * 避免两个服务互相注入形成循环依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl
        extends ServiceImpl<PolicyMapper, Policy>
        implements PolicyService {

    private final PolicyReadingMapper policyReadingMapper;

    @Override
    public PageResult<Policy> pageQuery(PolicyQuery query) {
        Page<Policy> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<Policy> listByQuery(PolicyQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<Policy> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("draft", all.stream()
                .filter(p -> Integer.valueOf(PolicyTypeUtil.STATUS_DRAFT).equals(p.getStatus())).count());
        r.put("published", all.stream()
                .filter(p -> Integer.valueOf(PolicyTypeUtil.STATUS_PUBLISHED).equals(p.getStatus())).count());
        r.put("abolished", all.stream()
                .filter(p -> Integer.valueOf(PolicyTypeUtil.STATUS_ABOLISHED).equals(p.getStatus())).count());
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String c : List.of(PolicyTypeUtil.CATEGORY_LAW, PolicyTypeUtil.CATEGORY_REGULATION,
                PolicyTypeUtil.CATEGORY_RULE, PolicyTypeUtil.CATEGORY_LOCAL,
                PolicyTypeUtil.CATEGORY_NORMATIVE, PolicyTypeUtil.CATEGORY_STANDARD,
                PolicyTypeUtil.CATEGORY_OTHER)) {
            byCategory.put(c, all.stream().filter(p -> c.equals(p.getCategory())).count());
        }
        r.put("byCategory", byCategory);
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (String l : List.of(PolicyTypeUtil.LEVEL_NATIONAL, PolicyTypeUtil.LEVEL_PROVINCIAL,
                PolicyTypeUtil.LEVEL_MUNICIPAL, PolicyTypeUtil.LEVEL_HOSPITAL)) {
            byLevel.put(l, all.stream().filter(p -> l.equals(p.getLevel())).count());
        }
        r.put("byLevel", byLevel);
        r.put("byCategoryList", statsByCategory());
        r.put("byLevelList", statsByLevel());
        r.put("byMonth", statsByMonth());

        // 30 天内到期：已发布、未过期、失效日期不晚于今天 + 30 天
        r.put("expiringSoon", all.stream()
                .filter(p -> PolicyTypeUtil.isExpiringSoon(p.getStatus(), p.getExpiryDate())).count());
        // 已过期：已发布且失效日期已过当天
        r.put("expired", all.stream()
                .filter(p -> PolicyTypeUtil.isExpired(p.getStatus(), p.getExpiryDate())).count());

        // 查阅学习统计
        List<PolicyReading> readings = policyReadingMapper.selectList(null);
        r.put("totalReadings", (long) readings.size());
        r.put("learnedCount", readings.stream()
                .filter(x -> Integer.valueOf(1).equals(x.getLearned())).count());
        r.put("readPolicyCount", readings.stream().map(PolicyReading::getPolicyId).distinct().count());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByCategory() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String c : List.of(PolicyTypeUtil.CATEGORY_LAW, PolicyTypeUtil.CATEGORY_REGULATION,
                PolicyTypeUtil.CATEGORY_RULE, PolicyTypeUtil.CATEGORY_LOCAL,
                PolicyTypeUtil.CATEGORY_NORMATIVE, PolicyTypeUtil.CATEGORY_STANDARD,
                PolicyTypeUtil.CATEGORY_OTHER)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", c);
            row.put("categoryText", PolicyTypeUtil.categoryText(c));
            row.put("count", this.count(new LambdaQueryWrapper<Policy>()
                    .eq(Policy::getCategory, c)));
            row.put("publishedCount", this.count(new LambdaQueryWrapper<Policy>()
                    .eq(Policy::getCategory, c)
                    .eq(Policy::getStatus, PolicyTypeUtil.STATUS_PUBLISHED)));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByLevel() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String l : List.of(PolicyTypeUtil.LEVEL_NATIONAL, PolicyTypeUtil.LEVEL_PROVINCIAL,
                PolicyTypeUtil.LEVEL_MUNICIPAL, PolicyTypeUtil.LEVEL_HOSPITAL)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("level", l);
            row.put("levelText", PolicyTypeUtil.levelText(l));
            row.put("count", this.count(new LambdaQueryWrapper<Policy>()
                    .eq(Policy::getLevel, l)));
            row.put("publishedCount", this.count(new LambdaQueryWrapper<Policy>()
                    .eq(Policy::getLevel, l)
                    .eq(Policy::getStatus, PolicyTypeUtil.STATUS_PUBLISHED)));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        Map<String, Long> grouped = new LinkedHashMap<>();
        for (Policy p : this.list()) {
            if (p.getPublishDate() == null) {
                continue;
            }
            String month = p.getPublishDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            grouped.merge(month, 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", e.getKey());
            row.put("count", e.getValue());
            result.add(row);
        });
        return result;
    }

    @Override
    public Policy detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Policy publish(Long id, PublishRequest request) {
        Policy p = getOrThrow(id);
        if (!Integer.valueOf(PolicyTypeUtil.STATUS_DRAFT).equals(p.getStatus())) {
            throw new IllegalStateException("只有草稿才能发布（当前状态："
                    + PolicyTypeUtil.statusText(p.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getApprover())) {
            throw new IllegalArgumentException("发布审批人不能为空");
        }
        p.setApprover(request.getApprover().trim());
        LocalDate today = LocalDate.now();
        p.setPublishDate(today);
        if (p.getEffectiveDate() == null) {
            p.setEffectiveDate(today);
        }
        if (p.getExpiryDate() != null && p.getExpiryDate().isBefore(p.getEffectiveDate())) {
            throw new IllegalArgumentException("失效日期不能早于生效日期");
        }
        p.setStatus(PolicyTypeUtil.STATUS_PUBLISHED);
        String opinion = StringUtils.hasText(request.getOpinion()) ? request.getOpinion().trim() : "同意";
        p.setApprovalHistory(appendHistory(p.getApprovalHistory(),
                "医务科 " + p.getApprover() + "：发布（" + opinion + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有草稿能修改”
        // 守卫（发布中的记录正是要从草稿改为已发布，守卫会误判）
        super.updateById(p);
        // 新版本发布时，同一文号的其他已发布版本自动废止
        List<Policy> others = this.list(new LambdaQueryWrapper<Policy>()
                .eq(Policy::getPolicyNo, p.getPolicyNo())
                .eq(Policy::getStatus, PolicyTypeUtil.STATUS_PUBLISHED)
                .ne(Policy::getId, p.getId()));
        for (Policy old : others) {
            old.setStatus(PolicyTypeUtil.STATUS_ABOLISHED);
            old.setAbolishReason("已被新版本 " + p.getVersion() + " 替代");
            old.setApprovalHistory(appendHistory(old.getApprovalHistory(),
                    "系统：因新版本 " + p.getVersion() + " 发布自动废止"));
            super.updateById(old);
        }
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Policy abolish(Long id, AbolishRequest request) {
        Policy p = getOrThrow(id);
        if (!Integer.valueOf(PolicyTypeUtil.STATUS_PUBLISHED).equals(p.getStatus())) {
            throw new IllegalStateException("只有已发布的法规才能废止（当前状态："
                    + PolicyTypeUtil.statusText(p.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("废止原因不能为空");
        }
        p.setStatus(PolicyTypeUtil.STATUS_ABOLISHED);
        p.setAbolishReason(request.getReason().trim());
        p.setApprovalHistory(appendHistory(p.getApprovalHistory(),
                "医务科 " + (p.getApprover() == null ? "" : p.getApprover()) + "：废止（" + p.getAbolishReason() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有草稿能修改”守卫
        super.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Policy newVersion(Long id) {
        Policy old = getOrThrow(id);
        if (!Integer.valueOf(PolicyTypeUtil.STATUS_PUBLISHED).equals(old.getStatus())) {
            throw new IllegalStateException("只有已发布的法规才能新建版本（当前状态："
                    + PolicyTypeUtil.statusText(old.getStatus()) + "）");
        }
        String nextVersion = PolicyTypeUtil.nextVersion(old.getVersion());
        if (this.count(new LambdaQueryWrapper<Policy>()
                .eq(Policy::getPolicyNo, old.getPolicyNo())
                .eq(Policy::getVersion, nextVersion)) > 0) {
            throw new IllegalArgumentException("该文号已有 " + nextVersion + " 版本，请先处理该版本");
        }
        Policy next = new Policy();
        next.setTitle(old.getTitle());
        next.setPolicyNo(old.getPolicyNo());
        next.setCategory(old.getCategory());
        next.setLevel(old.getLevel());
        next.setIssuingAuthority(old.getIssuingAuthority());
        next.setVersion(nextVersion);
        next.setContent(old.getContent());
        next.setApplicableDept(old.getApplicableDept());
        next.setApplicablePersonnel(old.getApplicablePersonnel());
        next.setDepartment(old.getDepartment());
        next.setDrafter(old.getDrafter());
        next.setApprover(null);
        next.setPublishDate(null);
        next.setEffectiveDate(null);
        next.setExpiryDate(null);
        next.setStatus(PolicyTypeUtil.STATUS_DRAFT);
        next.setAbolishReason(null);
        next.setApprovalHistory(null);
        next.setRemark(old.getRemark());
        normalize(next, true);
        super.save(next);
        return next;
    }

    @Override
    public String downloadText(Long id) {
        Policy p = getOrThrow(id);
        StringBuilder sb = new StringBuilder();
        sb.append(p.getTitle()).append("\n");
        sb.append("文号：").append(p.getPolicyNo()).append("\n");
        sb.append("分类：").append(PolicyTypeUtil.categoryText(p.getCategory())).append("\n");
        sb.append("发布层级：").append(PolicyTypeUtil.levelText(p.getLevel())).append("\n");
        sb.append("发布机关：").append(p.getIssuingAuthority() == null ? "" : p.getIssuingAuthority()).append("\n");
        sb.append("版本：").append(p.getVersion() == null ? "" : p.getVersion()).append("\n");
        sb.append("适用科室：").append(p.getApplicableDept() == null ? "" : p.getApplicableDept()).append("\n");
        sb.append("适用人员：").append(p.getApplicablePersonnel() == null ? "" : p.getApplicablePersonnel()).append("\n");
        sb.append("归口科室：").append(p.getDepartment() == null ? "" : p.getDepartment()).append("\n");
        sb.append("状态：").append(PolicyTypeUtil.statusText(p.getStatus())).append("\n");
        sb.append("发布日期：").append(PolicyTypeUtil.fmtDate(p.getPublishDate())).append("\n");
        sb.append("生效日期：").append(PolicyTypeUtil.fmtDate(p.getEffectiveDate())).append("\n");
        sb.append("失效日期：").append(PolicyTypeUtil.fmtDate(p.getExpiryDate())).append("\n");
        sb.append("\n");
        sb.append(p.getContent() == null ? "" : p.getContent()).append("\n");
        return sb.toString();
    }

    @Override
    public void exportExcel(PolicyQuery query, OutputStream out) {
        PolicyExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<Policy> list = PolicyExcelUtil.read(in);
        int ok = 0;
        for (Policy p : list) {
            if (!StringUtils.hasText(p.getTitle()) || !StringUtils.hasText(p.getPolicyNo())) {
                log.warn("导入跳过：法规名称或文号为空的行");
                continue;
            }
            if (!StringUtils.hasText(p.getCategory()) || !PolicyTypeUtil.isValidCategory(p.getCategory())) {
                log.warn("导入跳过：法规分类为空或不正确（法规 {}）", p.getTitle());
                continue;
            }
            if (!StringUtils.hasText(p.getLevel()) || !PolicyTypeUtil.isValidLevel(p.getLevel())) {
                log.warn("导入跳过：发布层级为空或不正确（法规 {}），默认国家", p.getTitle());
                p.setLevel(PolicyTypeUtil.LEVEL_NATIONAL);
            }
            // 导入的均为草稿，需走发布审批流程；正文不进 Excel，导入后在页面补录
            p.setStatus(PolicyTypeUtil.STATUS_DRAFT);
            if (!StringUtils.hasText(p.getVersion())) {
                p.setVersion("V1.0");
            }
            p.setApprover(null);
            p.setPublishDate(null);
            p.setApprovalHistory(null);
            p.setAbolishReason(null);
            // 同一文号同一版本视为重复，跳过
            if (this.count(new LambdaQueryWrapper<Policy>()
                    .eq(Policy::getPolicyNo, p.getPolicyNo())
                    .eq(Policy::getVersion, p.getVersion())) > 0) {
                log.warn("导入跳过：重复法规 {} {}", p.getPolicyNo(), p.getVersion());
                continue;
            }
            try {
                this.save(p);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", p.getTitle(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除：已发布的法规请先废止，草稿/已废止可直接删除（同时删除其查阅学习记录） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        Policy p = getOrThrow((Long) id);
        if (Integer.valueOf(PolicyTypeUtil.STATUS_PUBLISHED).equals(p.getStatus())) {
            throw new IllegalStateException("已发布的法规不能直接删除，请先废止");
        }
        policyReadingMapper.delete(new LambdaQueryWrapper<PolicyReading>()
                .eq(PolicyReading::getPolicyId, p.getId()));
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
    private LambdaQueryWrapper<Policy> buildWrapper(PolicyQuery q) {
        LambdaQueryWrapper<Policy> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(Policy::getTitle, q.getKeyword())
                .or().like(Policy::getPolicyNo, q.getKeyword())
                .or().like(Policy::getIssuingAuthority, q.getKeyword()));
        qw.eq(StringUtils.hasText(q.getCategory()), Policy::getCategory, q.getCategory());
        qw.eq(StringUtils.hasText(q.getLevel()), Policy::getLevel, q.getLevel());
        qw.eq(q.getStatus() != null, Policy::getStatus, q.getStatus());
        qw.like(StringUtils.hasText(q.getApplicableDept()), Policy::getApplicableDept, q.getApplicableDept());
        if (Boolean.TRUE.equals(q.getExpiringSoon())) {
            // 30 天内到期：只看已发布的，失效日期在今天 ~ 今天 + 30 天之间
            qw.eq(Policy::getStatus, PolicyTypeUtil.STATUS_PUBLISHED);
            qw.ge(Policy::getExpiryDate, LocalDate.now());
            qw.le(Policy::getExpiryDate, LocalDate.now().plusDays(PolicyTypeUtil.EXPIRY_WARN_DAYS));
        }
        if (Boolean.TRUE.equals(q.getExpired())) {
            // 已过期：只看已发布的，失效日期已过当天
            qw.eq(Policy::getStatus, PolicyTypeUtil.STATUS_PUBLISHED);
            qw.lt(Policy::getExpiryDate, LocalDate.now());
        }
        qw.orderByDesc(Policy::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验分类、层级与日期 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Policy entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Policy entity) {
        // 只有草稿能修改：已发布的法规更新走新建版本，已废止的法规不可再改
        Policy old = getOrThrow(entity.getId());
        if (!Integer.valueOf(PolicyTypeUtil.STATUS_DRAFT).equals(old.getStatus())) {
            throw new IllegalStateException("只有草稿才能修改（当前状态："
                    + PolicyTypeUtil.statusText(old.getStatus()) + "），已发布的法规请新建版本");
        }
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(Policy p, boolean isCreate) {
        if (!StringUtils.hasText(p.getCategory()) || !PolicyTypeUtil.isValidCategory(p.getCategory())) {
            throw new IllegalArgumentException("法规分类不正确");
        }
        if (!StringUtils.hasText(p.getLevel()) || !PolicyTypeUtil.isValidLevel(p.getLevel())) {
            throw new IllegalArgumentException("发布层级不正确");
        }
        if (p.getEffectiveDate() != null && p.getExpiryDate() != null
                && p.getExpiryDate().isBefore(p.getEffectiveDate())) {
            throw new IllegalArgumentException("失效日期不能早于生效日期");
        }
        if (p.getStatus() != null
                && !Integer.valueOf(PolicyTypeUtil.STATUS_DRAFT).equals(p.getStatus())
                && !Integer.valueOf(PolicyTypeUtil.STATUS_PUBLISHED).equals(p.getStatus())
                && !Integer.valueOf(PolicyTypeUtil.STATUS_ABOLISHED).equals(p.getStatus())) {
            throw new IllegalArgumentException("法规状态不正确");
        }
        if (isCreate) {
            if (p.getStatus() == null) {
                p.setStatus(PolicyTypeUtil.STATUS_DRAFT);
            }
            if (!StringUtils.hasText(p.getVersion())) {
                p.setVersion("V1.0");
            }
            if (!StringUtils.hasText(p.getApplicableDept())) {
                p.setApplicableDept("全院");
            }
            // 同一文号同一版本已有记录，拒绝重复建档
            LambdaQueryWrapper<Policy> qw = new LambdaQueryWrapper<Policy>()
                    .eq(Policy::getPolicyNo, p.getPolicyNo())
                    .eq(Policy::getVersion, p.getVersion());
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("文号 " + p.getPolicyNo() + " 的 " + p.getVersion()
                        + " 版本已有记录，请勿重复建档");
            }
        }
    }

    private String appendHistory(String history, String entry) {
        if (!StringUtils.hasText(history)) {
            return entry;
        }
        return history + "；" + entry;
    }

    private Policy getOrThrow(Long id) {
        Policy p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("法规不存在：" + id);
        }
        return p;
    }
}
