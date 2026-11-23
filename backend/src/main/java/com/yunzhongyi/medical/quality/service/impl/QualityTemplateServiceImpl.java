package com.yunzhongyi.medical.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.quality.dto.QualityTemplateQuery;
import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.mapper.QualityPlanMapper;
import com.yunzhongyi.medical.quality.mapper.QualityTemplateMapper;
import com.yunzhongyi.medical.quality.service.QualityTemplateService;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 质量检查模板管理服务实现：模板 CRUD + 启用/停用。
 * <p>
 * 保存时校验：检查类型须合法；检查项至少一项；每项须有名称与正数权重；
 * 各项权重之和必须为 100（满分 100）。
 */
@Service
@RequiredArgsConstructor
public class QualityTemplateServiceImpl
        extends ServiceImpl<QualityTemplateMapper, QualityTemplate>
        implements QualityTemplateService {

    private final QualityPlanMapper planMapper;

    @Override
    public PageResult<QualityTemplate> pageQuery(QualityTemplateQuery query) {
        LambdaQueryWrapper<QualityTemplate> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(query.getKeyword()), QualityTemplate::getTemplateName, query.getKeyword());
        qw.eq(StringUtils.hasText(query.getInspectType()), QualityTemplate::getInspectType, query.getInspectType());
        qw.eq(query.getStatus() != null, QualityTemplate::getStatus, query.getStatus());
        qw.orderByDesc(QualityTemplate::getId);
        Page<QualityTemplate> p = this.page(new Page<>(query.getPage(), query.getSize()), qw);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<QualityTemplate> listEnabled() {
        return this.list(new LambdaQueryWrapper<QualityTemplate>()
                .eq(QualityTemplate::getStatus, 1)
                .orderByDesc(QualityTemplate::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityTemplate enable(Long id) {
        QualityTemplate t = getOrThrow(id);
        t.setStatus(1);
        this.updateById(t);
        return t;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityTemplate disable(Long id) {
        QualityTemplate t = getOrThrow(id);
        long active = planMapper.selectCount(new LambdaQueryWrapper<QualityPlan>()
                .eq(QualityPlan::getTemplateId, id)
                .in(QualityPlan::getStatus, QualityTypeUtil.PLAN_PENDING, QualityTypeUtil.PLAN_IN_PROGRESS));
        if (active > 0) {
            throw new IllegalArgumentException("该模板有 " + active + " 个进行中的检查计划，不能停用（请先完成或取消相关计划）");
        }
        t.setStatus(0);
        this.updateById(t);
        return t;
    }

    /** 删除模板：已有检查计划引用时拒绝（避免检查记录失去模板依据） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        long plans = planMapper.selectCount(new LambdaQueryWrapper<QualityPlan>()
                .eq(QualityPlan::getTemplateId, (Long) id));
        if (plans > 0) {
            throw new IllegalArgumentException("该模板已被 " + plans + " 个检查计划引用，不能直接删除");
        }
        return super.removeById(id);
    }

    /** 新建时补默认状态并校验；更新时同样校验 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(QualityTemplate entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(QualityTemplate entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(QualityTemplate t, boolean isCreate) {
        if (!StringUtils.hasText(t.getInspectType()) || !QualityTypeUtil.isValidType(t.getInspectType())) {
            throw new IllegalArgumentException("检查类型不正确");
        }
        List<java.util.Map<String, Object>> items = QualityTypeUtil.parseCheckItems(t.getCheckItems());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("检查项不能为空，至少填写一项");
        }
        for (java.util.Map<String, Object> item : items) {
            Object name = item.get("name");
            Object weight = item.get("weight");
            if (name == null || String.valueOf(name).isBlank()) {
                throw new IllegalArgumentException("检查项名称不能为空");
            }
            if (weight == null) {
                throw new IllegalArgumentException("检查项「" + name + "」权重不能为空");
            }
            try {
                BigDecimal w = new BigDecimal(String.valueOf(weight));
                if (w.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("检查项「" + name + "」权重必须大于 0");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("检查项「" + name + "」权重不是数字");
            }
        }
        BigDecimal sum = QualityTypeUtil.sumWeights(t.getCheckItems());
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("检查项权重之和必须为 100，当前为 "
                    + sum.stripTrailingZeros().toPlainString());
        }
        if (isCreate && t.getStatus() == null) {
            t.setStatus(1);
        }
    }

    private QualityTemplate getOrThrow(Long id) {
        QualityTemplate t = this.getById(id);
        if (t == null) {
            throw new IllegalArgumentException("检查模板不存在：" + id);
        }
        return t;
    }
}
