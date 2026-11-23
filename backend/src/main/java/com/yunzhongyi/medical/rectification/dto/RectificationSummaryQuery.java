package com.yunzhongyi.medical.rectification.dto;

import lombok.Data;

/**
 * 检查整改汇总报告多条件组合查询参数
 */
@Data
public class RectificationSummaryQuery {

    /** 统计周期，如 2026-11（yyyy-MM，精确匹配） */
    private String statPeriod;

    /** 汇总范围：ALL 全院 / DEPT 按科室 / TYPE 按检查类型 */
    private String scopeType;

    /** 科室名称（模糊匹配） */
    private String deptName;

    /** 检查类型：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查 */
    private String inspectType;

    private long page = 1;
    private long size = 10;
}
