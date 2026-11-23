package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 医德考评模板多条件组合查询参数
 */
@Data
public class EthicsTemplateQuery {

    /** 关键字：模板名称模糊匹配 */
    private String keyword;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
