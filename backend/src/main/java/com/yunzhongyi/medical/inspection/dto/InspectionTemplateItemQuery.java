package com.yunzhongyi.medical.inspection.dto;

import lombok.Data;

/**
 * 模板检查项配置查询参数
 */
@Data
public class InspectionTemplateItemQuery {

    /** 检查模板 id */
    private Long templateId;

    /** 检查项目 id */
    private Long itemId;

    /** 关键字：模板名称 / 项目名称 / 项目编号模糊匹配 */
    private String keyword;

    private long page = 1;
    private long size = 10;
}
