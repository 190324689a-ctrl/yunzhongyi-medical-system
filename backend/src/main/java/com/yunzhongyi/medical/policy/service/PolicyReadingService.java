package com.yunzhongyi.medical.policy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.policy.dto.PolicyReadingQuery;
import com.yunzhongyi.medical.policy.entity.PolicyReading;

import java.util.List;

/**
 * 政策法规查阅与学习记录服务
 */
public interface PolicyReadingService extends IService<PolicyReading> {

    /** 多条件组合分页查询 */
    PageResult<PolicyReading> pageQuery(PolicyReadingQuery query);

    /** 查某部法规的全部查阅学习记录（按查阅日期降序） */
    List<PolicyReading> listByPolicy(Long policyId);

    /**
     * 登记查阅学习：只有已发布的法规才能登记；查阅日期默认为当天；
     * 同一人同一法规同一天只登记一次，重复登记拒绝。
     */
    PolicyReading create(PolicyReading reading);

    /** 标记已学习：已查阅 → 已学习 */
    PolicyReading markLearned(Long id);

    /** 删除一条查阅学习记录 */
    boolean deleteReading(Long id);
}
