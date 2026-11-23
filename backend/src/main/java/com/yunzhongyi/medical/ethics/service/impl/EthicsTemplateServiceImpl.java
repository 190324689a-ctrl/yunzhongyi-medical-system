package com.yunzhongyi.medical.ethics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.EthicsTemplateQuery;
import com.yunzhongyi.medical.ethics.entity.EthicsTemplate;
import com.yunzhongyi.medical.ethics.mapper.EthicsTemplateMapper;
import com.yunzhongyi.medical.ethics.service.EthicsTemplateService;
import com.yunzhongyi.medical.ethics.service.EthicsTypeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 医德考评模板服务实现：自定义模板 + 权重合计 100 校验
 */
@Service
@RequiredArgsConstructor
public class EthicsTemplateServiceImpl
        extends ServiceImpl<EthicsTemplateMapper, EthicsTemplate>
        implements EthicsTemplateService {

    @Override
    public PageResult<EthicsTemplate> pageQuery(EthicsTemplateQuery query) {
        LambdaQueryWrapper<EthicsTemplate> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            qw.like(EthicsTemplate::getTemplateName, query.getKeyword());
        }
        qw.eq(query.getStatus() != null, EthicsTemplate::getStatus, query.getStatus());
        qw.orderByDesc(EthicsTemplate::getId);
        Page<EthicsTemplate> p = this.page(new Page<>(query.getPage(), query.getSize()), qw);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<EthicsTemplate> listEnabled() {
        return this.list(new LambdaQueryWrapper<EthicsTemplate>()
                .eq(EthicsTemplate::getStatus, 1)
                .orderByDesc(EthicsTemplate::getId));
    }

    /** 新建默认启用；保存/更新时校验评分项权重之和为 100 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EthicsTemplate entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        validateScoreItems(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(EthicsTemplate entity) {
        validateScoreItems(entity);
        return super.updateById(entity);
    }

    private void validateScoreItems(EthicsTemplate t) {
        if (!StringUtils.hasText(t.getScoreItems())) {
            throw new IllegalArgumentException("评分项不能为空，至少包含一个评分维度");
        }
        List<Map<String, Object>> items = EthicsTypeUtil.parseScoreItems(t.getScoreItems());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("评分项格式不正确，应为 JSON 数组，如 [{\"name\":\"医德医风\",\"weight\":50}]");
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
        if (EthicsTypeUtil.sumWeights(t.getScoreItems()).compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("评分项权重之和必须为 100，当前为 "
                    + EthicsTypeUtil.sumWeights(t.getScoreItems()).stripTrailingZeros().toPlainString());
        }
    }
}
