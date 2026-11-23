package com.yunzhongyi.medical.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.statistics.dto.QualityIndicatorQuery;
import com.yunzhongyi.medical.statistics.entity.QualityIndicator;
import com.yunzhongyi.medical.statistics.excel.QualityIndicatorExcelUtil;
import com.yunzhongyi.medical.statistics.mapper.QualityIndicatorMapper;
import com.yunzhongyi.medical.statistics.service.QualityIndicatorService;
import com.yunzhongyi.medical.statistics.service.StatisticsTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 三级医院医疗质量指标统计服务实现：指标 CRUD + 按国家 / 省 / 市标准统计
 * （达标率）+ Excel 批量导入导出
 */
@Slf4j
@Service
public class QualityIndicatorServiceImpl
        extends ServiceImpl<QualityIndicatorMapper, QualityIndicator>
        implements QualityIndicatorService {

    @Override
    public PageResult<QualityIndicator> pageQuery(QualityIndicatorQuery query) {
        Page<QualityIndicator> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        p.getRecords().forEach(this::fillCompliance);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<QualityIndicator> listByQuery(QualityIndicatorQuery query) {
        List<QualityIndicator> list = this.list(buildWrapper(query));
        list.forEach(this::fillCompliance);
        return list;
    }

    @Override
    public Map<String, Object> stats() {
        List<QualityIndicator> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("reportedCount", countReported(all));
        r.put("compliantCount", countCompliant(all));
        r.put("complianceRate", StatisticsTypeUtil.rate(countCompliant(all), countReported(all)));
        r.put("byLevel", statsByLevel());
        r.put("byCategory", statsByCategory());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByLevel() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String level : List.of(StatisticsTypeUtil.LEVEL_NATIONAL,
                StatisticsTypeUtil.LEVEL_PROVINCE, StatisticsTypeUtil.LEVEL_CITY)) {
            List<QualityIndicator> list = this.list(new LambdaQueryWrapper<QualityIndicator>()
                    .eq(QualityIndicator::getStandardLevel, level));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("standardLevel", level);
            row.put("levelText", StatisticsTypeUtil.levelText(level));
            row.put("total", (long) list.size());
            row.put("reportedCount", countReported(list));
            row.put("compliantCount", countCompliant(list));
            row.put("complianceRate", StatisticsTypeUtil.rate(countCompliant(list), countReported(list)));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByCategory() {
        Map<String, List<QualityIndicator>> grouped = new LinkedHashMap<>();
        for (QualityIndicator q : this.list()) {
            String category = StringUtils.hasText(q.getCategory()) ? q.getCategory() : "未分类";
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(q);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((category, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", category);
            row.put("total", (long) list.size());
            row.put("reportedCount", countReported(list));
            row.put("compliantCount", countCompliant(list));
            row.put("complianceRate", StatisticsTypeUtil.rate(countCompliant(list), countReported(list)));
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("total"), (Long) a.get("total")));
        return result;
    }

    @Override
    public void exportExcel(QualityIndicatorQuery query, OutputStream out) {
        QualityIndicatorExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<QualityIndicator> list = QualityIndicatorExcelUtil.read(in);
        int ok = 0;
        for (QualityIndicator q : list) {
            if (!StringUtils.hasText(q.getIndicatorCode())) {
                log.warn("导入跳过：指标编码为空的行");
                continue;
            }
            if (!StringUtils.hasText(q.getIndicatorName())) {
                log.warn("导入跳过：指标名称为空（编码 {}）", q.getIndicatorCode());
                continue;
            }
            if (!StatisticsTypeUtil.isValidLevel(q.getStandardLevel())) {
                log.warn("导入跳过：标准级别不正确（指标 {}）", q.getIndicatorCode());
                continue;
            }
            if (!StatisticsTypeUtil.isValidCategory(q.getCategory())) {
                log.warn("导入跳过：指标分类不正确（指标 {}）", q.getIndicatorCode());
                continue;
            }
            if (q.getDirection() == null) {
                q.setDirection(StatisticsTypeUtil.DIRECTION_HIGHER);
            }
            // 同一指标编码视为重复，跳过
            if (this.count(new LambdaQueryWrapper<QualityIndicator>()
                    .eq(QualityIndicator::getIndicatorCode, q.getIndicatorCode())) > 0) {
                log.warn("导入跳过：重复指标编码 {}", q.getIndicatorCode());
                continue;
            }
            this.save(q);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<QualityIndicator> buildWrapper(QualityIndicatorQuery q) {
        LambdaQueryWrapper<QualityIndicator> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.and(w -> w.like(QualityIndicator::getIndicatorCode, q.getKeyword())
                    .or().like(QualityIndicator::getIndicatorName, q.getKeyword()));
        }
        qw.eq(StringUtils.hasText(q.getCategory()), QualityIndicator::getCategory, q.getCategory());
        qw.eq(StringUtils.hasText(q.getStandardLevel()), QualityIndicator::getStandardLevel, q.getStandardLevel());
        qw.eq(StringUtils.hasText(q.getStatPeriod()), QualityIndicator::getStatPeriod, q.getStatPeriod());
        qw.eq(StringUtils.hasText(q.getDeptName()), QualityIndicator::getDeptName, q.getDeptName());
        qw.orderByAsc(QualityIndicator::getIndicatorCode).orderByDesc(QualityIndicator::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验本条之外的编码重复 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(QualityIndicator entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(QualityIndicator entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(QualityIndicator q, boolean isCreate) {
        if (!StatisticsTypeUtil.isValidLevel(q.getStandardLevel())) {
            throw new IllegalArgumentException("标准级别不正确（应为国家 / 省 / 市）");
        }
        if (!StatisticsTypeUtil.isValidCategory(q.getCategory())) {
            throw new IllegalArgumentException("指标分类不正确（应为医疗质量 / 医疗安全 / 患者安全 / 工作效率）");
        }
        if (q.getDirection() != null
                && !StatisticsTypeUtil.DIRECTION_HIGHER.equals(q.getDirection())
                && !StatisticsTypeUtil.DIRECTION_LOWER.equals(q.getDirection())) {
            throw new IllegalArgumentException("比较方向不正确（应为越高越好 / 越低越好）");
        }
        if (q.getDirection() == null) {
            q.setDirection(StatisticsTypeUtil.DIRECTION_HIGHER);
        }
        if (q.getTargetValue() != null && q.getTargetValue().signum() < 0
                || q.getActualValue() != null && q.getActualValue().signum() < 0) {
            throw new IllegalArgumentException("目标值与实际值不能为负数");
        }
        LambdaQueryWrapper<QualityIndicator> qw = new LambdaQueryWrapper<QualityIndicator>()
                .eq(QualityIndicator::getIndicatorCode, q.getIndicatorCode());
        if (!isCreate) {
            qw.ne(QualityIndicator::getId, q.getId());
        }
        if (this.count(qw) > 0) {
            throw new IllegalArgumentException("指标编码已存在：" + q.getIndicatorCode());
        }
    }

    /** 给一条指标现算达标情况（不存库） */
    private void fillCompliance(QualityIndicator q) {
        Boolean ok = StatisticsTypeUtil.evaluate(q);
        q.setCompliant(ok);
        q.setComplianceText(StatisticsTypeUtil.complianceText(q));
    }

    private static long countReported(List<QualityIndicator> list) {
        return list.stream().filter(q -> q.getActualValue() != null).count();
    }

    private static long countCompliant(List<QualityIndicator> list) {
        return list.stream().filter(q -> Boolean.TRUE.equals(StatisticsTypeUtil.evaluate(q))).count();
    }
}
