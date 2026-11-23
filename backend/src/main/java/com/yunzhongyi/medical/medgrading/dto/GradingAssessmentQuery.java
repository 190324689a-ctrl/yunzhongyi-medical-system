package com.yunzhongyi.medical.medgrading.dto;

import lombok.Data;

/**
 * 分级评定多条件组合查询参数
 */
@Data
public class GradingAssessmentQuery {

    /** 关键字：科室名称 / 评定人模糊匹配 */
    private String keyword;

    /** 分级标准 id */
    private Long standardId;

    /** 科室 id */
    private Long deptId;

    /** 等级：A 甲级 / B 乙级 / C 丙级 / D 丁级 */
    private String grade;

    /** 状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 评定周期 yyyy-MM */
    private String period;

    /** 评定周期起 yyyy-MM */
    private String periodFrom;

    /** 评定周期止 yyyy-MM */
    private String periodTo;

    private long page = 1;
    private long size = 10;
}
