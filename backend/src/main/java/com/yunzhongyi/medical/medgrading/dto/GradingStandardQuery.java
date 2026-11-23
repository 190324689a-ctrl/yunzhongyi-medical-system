package com.yunzhongyi.medical.medgrading.dto;

import lombok.Data;

/**
 * 分级标准多条件组合查询参数
 */
@Data
public class GradingStandardQuery {

    /** 关键字：标准名称 / 标准编码模糊匹配 */
    private String keyword;

    /** 分级范围：QUALITY 医疗质量分级 / SERVICE 医疗服务分级 / COMPREHENSIVE 综合分级 */
    private String scope;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
