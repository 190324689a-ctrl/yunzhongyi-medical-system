package com.yunzhongyi.medical.policy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.policy.dto.PolicyReadingQuery;
import com.yunzhongyi.medical.policy.entity.Policy;
import com.yunzhongyi.medical.policy.entity.PolicyReading;
import com.yunzhongyi.medical.policy.mapper.PolicyMapper;
import com.yunzhongyi.medical.policy.mapper.PolicyReadingMapper;
import com.yunzhongyi.medical.policy.service.PolicyReadingService;
import com.yunzhongyi.medical.policy.service.PolicyTypeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 政策法规查阅与学习记录服务实现
 * <p>
 * 法规存在性与发布状态直接读 PolicyMapper，不依赖 PolicyService，
 * 避免两个服务互相注入形成循环依赖。
 */
@Service
@RequiredArgsConstructor
public class PolicyReadingServiceImpl
        extends ServiceImpl<PolicyReadingMapper, PolicyReading>
        implements PolicyReadingService {

    private final PolicyMapper policyMapper;

    @Override
    public PageResult<PolicyReading> pageQuery(PolicyReadingQuery query) {
        Page<PolicyReading> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<PolicyReading> listByPolicy(Long policyId) {
        return this.list(new LambdaQueryWrapper<PolicyReading>()
                .eq(PolicyReading::getPolicyId, policyId)
                .orderByDesc(PolicyReading::getReadDate)
                .orderByDesc(PolicyReading::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PolicyReading create(PolicyReading reading) {
        if (reading.getPolicyId() == null) {
            throw new IllegalArgumentException("法规不能为空");
        }
        Policy policy = policyMapper.selectById(reading.getPolicyId());
        if (policy == null) {
            throw new IllegalArgumentException("法规不存在：" + reading.getPolicyId());
        }
        if (!Integer.valueOf(PolicyTypeUtil.STATUS_PUBLISHED).equals(policy.getStatus())) {
            throw new IllegalStateException("只有已发布的法规才能登记查阅学习（当前状态："
                    + PolicyTypeUtil.statusText(policy.getStatus()) + "）");
        }
        if (!StringUtils.hasText(reading.getReaderName())) {
            throw new IllegalArgumentException("查阅人姓名不能为空");
        }
        reading.setReaderName(reading.getReaderName().trim());
        if (reading.getReadDate() == null) {
            reading.setReadDate(LocalDate.now());
        }
        if (reading.getLearned() == null) {
            reading.setLearned(0);
        }
        if (!Integer.valueOf(0).equals(reading.getLearned())
                && !Integer.valueOf(1).equals(reading.getLearned())) {
            throw new IllegalArgumentException("是否已学习不正确（0 已查阅 / 1 已学习）");
        }
        reading.setPolicyTitle(policy.getTitle());
        // 同一人同一法规同一天只登记一次
        if (this.count(new LambdaQueryWrapper<PolicyReading>()
                .eq(PolicyReading::getPolicyId, reading.getPolicyId())
                .eq(PolicyReading::getReaderName, reading.getReaderName())
                .eq(PolicyReading::getReadDate, reading.getReadDate())) > 0) {
            throw new IllegalArgumentException(reading.getReaderName() + " 已于 "
                    + reading.getReadDate() + " 登记过该法规的查阅学习，请勿重复登记");
        }
        this.save(reading);
        return reading;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PolicyReading markLearned(Long id) {
        PolicyReading r = getOrThrow(id);
        if (Integer.valueOf(1).equals(r.getLearned())) {
            throw new IllegalStateException("该记录已标记为已学习");
        }
        r.setLearned(1);
        super.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReading(Long id) {
        getOrThrow(id);
        return super.removeById(id);
    }

    private LambdaQueryWrapper<PolicyReading> buildWrapper(PolicyReadingQuery q) {
        LambdaQueryWrapper<PolicyReading> qw = new LambdaQueryWrapper<>();
        qw.eq(q.getPolicyId() != null, PolicyReading::getPolicyId, q.getPolicyId());
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(PolicyReading::getReaderName, q.getKeyword())
                .or().like(PolicyReading::getDeptName, q.getKeyword()));
        qw.eq(q.getLearned() != null, PolicyReading::getLearned, q.getLearned());
        qw.orderByDesc(PolicyReading::getReadDate);
        qw.orderByDesc(PolicyReading::getId);
        return qw;
    }

    private PolicyReading getOrThrow(Long id) {
        PolicyReading r = this.getById(id);
        if (r == null) {
            throw new IllegalArgumentException("查阅学习记录不存在：" + id);
        }
        return r;
    }
}
