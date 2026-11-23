package com.yunzhongyi.medical.statistics.dto;

import lombok.Data;

/**
 * 三级医院医疗质量指标多条件组合查询参数
 */
@Data
public class QualityIndicatorQuery {

    /** 关键字：指标编码 / 指标名称 模糊匹配 */
    private String keyword;

    /** 指标分类：医疗质量 / 医疗安全 / 患者安全 / 工作效率 */
    private String category;

    /** 标准级别：NATIONAL 国家 / PROVINCE 省 / CITY 市 */
    private String standardLevel;

    /** 统计周期，如 2026-11 / 2026 年 */
    private String statPeriod;

    /** 科室名称 */
    private String deptName;

    private long page = 1;
    private long size = 10;
}
