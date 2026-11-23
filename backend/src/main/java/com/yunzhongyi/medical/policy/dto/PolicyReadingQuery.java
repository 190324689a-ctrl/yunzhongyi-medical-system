package com.yunzhongyi.medical.policy.dto;

import lombok.Data;

/**
 * 查阅与学习记录多条件组合查询参数
 */
@Data
public class PolicyReadingQuery {

    /** 法规 id（查某部法规的全部查阅学习记录时必填） */
    private Long policyId;

    /** 关键字：查阅人姓名 / 科室模糊匹配 */
    private String keyword;

    /** 是否已学习：0 已查阅，1 已学习 */
    private Integer learned;

    private long page = 1;
    private long size = 10;
}
