package com.yunzhongyi.medical.quality.dto;

import lombok.Data;

/**
 * 检查模板查询参数
 */
@Data
public class QualityTemplateQuery {

    /** 关键字：模板名称模糊匹配 */
    private String keyword;

    /** 检查类型：SELF / HOSPITAL / SPECIAL */
    private String inspectType;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
