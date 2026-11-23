package com.yunzhongyi.medical.assessment.dto;

import lombok.Data;

/**
 * 医师考核多条件组合查询参数
 */
@Data
public class AssessmentQuery {

    /** 关键字：被考核医师姓名 模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 考核类型：PERIODIC / ETHICS / QUALITY */
    private String assessType;

    /** 考核年度 */
    private Integer assessYear;

    /** 等级：EXCELLENT / QUALIFIED / BASIC / UNQUALIFIED */
    private String grade;

    /** 考核模板 id */
    private Long templateId;

    private long page = 1;
    private long size = 10;
}
