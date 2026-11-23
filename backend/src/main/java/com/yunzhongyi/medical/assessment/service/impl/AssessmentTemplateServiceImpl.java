package com.yunzhongyi.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.assessment.dto.AssessmentTemplateQuery;
import com.yunzhongyi.medical.assessment.entity.AssessmentTemplate;
import com.yunzhongyi.medical.assessment.mapper.AssessmentTemplateMapper;
import com.yunzhongyi.medical.assessment.service.AssessmentGradeUtil;
import com.yunzhongyi.medical.assessment.service.AssessmentTemplateService;
import com.yunzhongyi.medical.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 考核模板服务实现：自定义模板 + 权重合计 100 校验
 */
@Service
@RequiredArgsConstructor
public class AssessmentTemplateServiceImpl
        extends ServiceImpl<AssessmentTemplateMapper, AssessmentTemplate>
        implements AssessmentTemplateService {

    @Override
    public PageResult<AssessmentTemplate> pageQuery(AssessmentTemplateQuery query) {
        LambdaQueryWrapper<AssessmentTemplate> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            qw.like(AssessmentTemplate::getTemplateName, query.getKeyword());
        }
        qw.eq(StringUtils.hasText(query.getAssessType()), AssessmentTemplate::getAssessType, query.getAssessType());
        qw.eq(query.getStatus() != null, AssessmentTemplate::getStatus, query.getStatus());
        qw.orderByDesc(AssessmentTemplate::getId);
        Page<AssessmentTemplate> p = this.page(new Page<>(query.getPage(), query.getSize()), qw);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<AssessmentTemplate> listEnabled() {
        return this.list(new LambdaQueryWrapper<AssessmentTemplate>()
                .eq(AssessmentTemplate::getStatus, 1)
                .orderByDesc(AssessmentTemplate::getId));
    }

    /** 新建默认启用；保存/更新时校验评分项权重之和为 100 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(AssessmentTemplate entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        validateScoreItems(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(AssessmentTemplate entity) {
        validateScoreItems(entity);
        return super.updateById(entity);
    }

    private void validateScoreItems(AssessmentTemplate t) {
        if (!StringUtils.hasText(t.getScoreItems())) {
            throw new IllegalArgumentException("评分项不能为空，至少包含一个评分维度");
        }
        List<Map<String, Object>> items = AssessmentGradeUtil.parseScoreItems(t.getScoreItems());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("评分项格式不正确，应为 JSON 数组，如 [{\"name\":\"医德医风\",\"weight\":30}]");
        }
        for (Map<String, Object> item : items) {
            if (!StringUtils.hasText(String.valueOf(item.get("name")).replace("null", ""))) {
                throw new IllegalArgumentException("评分项名称不能为空");
            }
            Object w = item.get("weight");
            if (w == null) {
                throw new IllegalArgumentException("评分项「" + item.get("name") + "」缺少权重 weight");
            }
            try {
                BigDecimal weight = new BigDecimal(String.valueOf(w));
                if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("评分项「" + item.get("name") + "」权重必须大于 0");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("评分项「" + item.get("name") + "」权重不是数字");
            }
        }
        if (AssessmentGradeUtil.sumWeights(t.getScoreItems()).compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("评分项权重之和必须为 100，当前为 "
                    + AssessmentGradeUtil.sumWeights(t.getScoreItems()).stripTrailingZeros().toPlainString());
        }
    }
}
