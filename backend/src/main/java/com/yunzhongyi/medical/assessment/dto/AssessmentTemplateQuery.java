package com.yunzhongyi.medical.assessment.dto;

import lombok.Data;

/**
 * 考核模板查询参数
 */
@Data
public class AssessmentTemplateQuery {

    /** 关键字：模板名称 模糊匹配 */
    private String keyword;

    /** 考核类型：PERIODIC / ETHICS / QUALITY */
    private String assessType;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
