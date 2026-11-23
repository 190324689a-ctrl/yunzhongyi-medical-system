package com.yunzhongyi.medical.briefing.dto;

import lombok.Data;

/**
 * 简报模板分页查询条件：关键字（模板名称）/ 分类 / 启用状态。
 */
@Data
public class BriefingTemplateQuery {

    /** 关键字：模板名称 */
    private String keyword;

    /** 简报分类：MONTHLY / QUARTERLY / ANNUAL / SPECIAL / DYNAMIC */
    private String category;

    /** 启用状态：1 启用，0 停用 */
    private Integer status;

    private long page = 1;

    private long size = 10;
}
