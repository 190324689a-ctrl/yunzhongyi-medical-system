package com.yunzhongyi.medical.medgrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.medgrading.dto.GradingStandardQuery;
import com.yunzhongyi.medical.medgrading.entity.GradingAssessment;
import com.yunzhongyi.medical.medgrading.entity.GradingStandard;
import com.yunzhongyi.medical.medgrading.excel.GradingStandardExcelUtil;
import com.yunzhongyi.medical.medgrading.mapper.GradingAssessmentMapper;
import com.yunzhongyi.medical.medgrading.mapper.GradingStandardMapper;
import com.yunzhongyi.medical.medgrading.service.GradingStandardService;
import com.yunzhongyi.medical.medgrading.service.MedGradingTypeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医疗分级标准服务实现。
 * <p>
 * 关键守卫：分数线须满足 0 &lt; passMin &lt; goodMin &lt; excellentMin &le; 100；
 * 有评定的标准不可删除；有待审批评定的标准不可停用（分数线变更不追溯已有评定，
 * 停用会阻断新评定，因此待审批评定存在时不允许停用）。
 */
@Service
@RequiredArgsConstructor
public class GradingStandardServiceImpl
        extends ServiceImpl<GradingStandardMapper, GradingStandard>
        implements GradingStandardService {

    private final GradingAssessmentMapper assessmentMapper;

    @Override
    public PageResult<GradingStandard> pageQuery(GradingStandardQuery query) {
        Page<GradingStandard> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public GradingStandard detail(Long id) {
        GradingStandard s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("分级标准不存在：" + id);
        }
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradingStandard create(GradingStandard standard) {
        validateStandard(standard, null);
        if (standard.getStatus() == null) {
            standard.setStatus(MedGradingTypeUtil.STATUS_ENABLED);
        }
        applyDefaults(standard);
        this.save(standard);
        return standard;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradingStandard update(GradingStandard standard) {
        GradingStandard old = detail(standard.getId());
        validateStandard(standard, standard.getId());
        // 已有评定引用时编码不可改（评定按编码冗余存储）
        if (!old.getStandardCode().equals(standard.getStandardCode())
                && assessmentMapper.selectCount(new LambdaQueryWrapper<GradingAssessment>()
                .eq(GradingAssessment::getStandardId, standard.getId())) > 0) {
            throw new IllegalArgumentException("该标准已有分级评定，编码不可修改");
        }
        applyDefaults(standard);
        this.updateById(standard);
        return standard;
    }

    @Override
    public GradingStandard enable(Long id) {
        GradingStandard s = detail(id);
        s.setStatus(MedGradingTypeUtil.STATUS_ENABLED);
        this.updateById(s);
        return s;
    }

    @Override
    public GradingStandard disable(Long id) {
        GradingStandard s = detail(id);
        long pending = assessmentMapper.selectCount(new LambdaQueryWrapper<GradingAssessment>()
                .eq(GradingAssessment::getStandardId, id)
                .eq(GradingAssessment::getStatus, MedGradingTypeUtil.ASSESS_PENDING));
        if (pending > 0) {
            throw new IllegalArgumentException("该标准有 " + pending + " 条待审批评定，不能停用");
        }
        s.setStatus(MedGradingTypeUtil.STATUS_DISABLED);
        this.updateById(s);
        return s;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        long used = assessmentMapper.selectCount(new LambdaQueryWrapper<GradingAssessment>()
                .eq(GradingAssessment::getStandardId, id));
        if (used > 0) {
            throw new IllegalArgumentException("该标准已有 " + used + " 条分级评定，不能直接删除");
        }
        return super.removeById(id);
    }

    @Override
    public List<GradingStandard> enabled() {
        return this.list(new LambdaQueryWrapper<GradingStandard>()
                .eq(GradingStandard::getStatus, MedGradingTypeUtil.STATUS_ENABLED)
                .orderByAsc(GradingStandard::getStandardCode));
    }

    @Override
    public Map<String, Object> stats() {
        List<GradingStandard> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream()
                .filter(s -> Integer.valueOf(MedGradingTypeUtil.STATUS_ENABLED).equals(s.getStatus())).count());
        r.put("disabled", all.stream()
                .filter(s -> Integer.valueOf(MedGradingTypeUtil.STATUS_DISABLED).equals(s.getStatus())).count());
        Map<String, Long> byScope = new LinkedHashMap<>();
        for (String scope : List.of(MedGradingTypeUtil.SCOPE_QUALITY,
                MedGradingTypeUtil.SCOPE_SERVICE, MedGradingTypeUtil.SCOPE_COMPREHENSIVE)) {
            byScope.put(scope, all.stream().filter(s -> scope.equals(s.getScope())).count());
        }
        r.put("byScope", byScope);
        long used = all.stream()
                .filter(s -> assessmentMapper.selectCount(new LambdaQueryWrapper<GradingAssessment>()
                        .eq(GradingAssessment::getStandardId, s.getId())) > 0)
                .count();
        r.put("usedStandards", used);
        r.put("unusedStandards", all.size() - used);
        return r;
    }

    @Override
    public void exportExcel(GradingStandardQuery query, OutputStream out) {
        GradingStandardExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<GradingStandard> rows = GradingStandardExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (GradingStandard row : rows) {
            try {
                validateStandard(row, null);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<GradingStandard>()
                    .eq(GradingStandard::getStandardCode, row.getStandardCode()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            if (row.getStatus() == null) {
                row.setStatus(MedGradingTypeUtil.STATUS_ENABLED);
            }
            applyDefaults(row);
            this.save(row);
            created++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", rows.size());
        return result;
    }

    @Override
    public void writeImportTemplate(OutputStream out) {
        GradingStandardExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<GradingStandard> buildWrapper(GradingStandardQuery query) {
        LambdaQueryWrapper<GradingStandard> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(GradingStandard::getStandardName, kw)
                    .or().like(GradingStandard::getStandardCode, kw));
        }
        if (StringUtils.hasText(query.getScope())) {
            w.eq(GradingStandard::getScope, query.getScope());
        }
        if (query.getStatus() != null) {
            w.eq(GradingStandard::getStatus, query.getStatus());
        }
        w.orderByDesc(GradingStandard::getId);
        return w;
    }

    /** 分数线默认值：甲级 90 / 乙级 80 / 丙级 60 */
    private void applyDefaults(GradingStandard s) {
        if (s.getExcellentMin() == null) {
            s.setExcellentMin(BigDecimal.valueOf(90));
        }
        if (s.getGoodMin() == null) {
            s.setGoodMin(BigDecimal.valueOf(80));
        }
        if (s.getPassMin() == null) {
            s.setPassMin(BigDecimal.valueOf(60));
        }
        if (s.getValidFrom() != null && s.getValidTo() != null
                && s.getValidFrom().isAfter(s.getValidTo())) {
            throw new IllegalArgumentException("有效期起不能晚于有效期止");
        }
    }

    /** 标准校验：编码/名称/范围必填、编码唯一、分数线顺序 */
    private void validateStandard(GradingStandard s, Long excludeId) {
        if (!StringUtils.hasText(s.getStandardCode())) {
            throw new IllegalArgumentException("标准编码不能为空");
        }
        if (!StringUtils.hasText(s.getStandardName())) {
            throw new IllegalArgumentException("标准名称不能为空");
        }
        if (!MedGradingTypeUtil.isValidScope(s.getScope())) {
            throw new IllegalArgumentException("分级范围不正确：" + s.getScope());
        }
        LambdaQueryWrapper<GradingStandard> dup = new LambdaQueryWrapper<GradingStandard>()
                .eq(GradingStandard::getStandardCode, s.getStandardCode().trim());
        if (excludeId != null) {
            dup.ne(GradingStandard::getId, excludeId);
        }
        if (this.count(dup) > 0) {
            throw new IllegalArgumentException("标准编码已存在：" + s.getStandardCode());
        }
        BigDecimal excellent = s.getExcellentMin() == null ? BigDecimal.valueOf(90) : s.getExcellentMin();
        BigDecimal good = s.getGoodMin() == null ? BigDecimal.valueOf(80) : s.getGoodMin();
        BigDecimal pass = s.getPassMin() == null ? BigDecimal.valueOf(60) : s.getPassMin();
        if (pass.compareTo(BigDecimal.ZERO) <= 0 || excellent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("分数线须在 0-100 之间");
        }
        if (!(pass.compareTo(good) < 0 && good.compareTo(excellent) < 0)) {
            throw new IllegalArgumentException("分数线须满足 丙级线 < 乙级线 < 甲级线");
        }
    }
}
