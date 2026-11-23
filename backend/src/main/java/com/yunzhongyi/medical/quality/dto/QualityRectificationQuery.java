package com.yunzhongyi.medical.quality.dto;

import lombok.Data;

/**
 * 整改任务查询参数
 */
@Data
public class QualityRectificationQuery {

    /** 整改责任人（模糊匹配） */
    private String respPerson;

    /** 整改状态：0 待整改，1 已整改（待复查），2 已复查通过 */
    private Integer status;

    /** 只查逾期：true 时只返回待整改/已整改且期限已过的任务 */
    private Boolean overdueOnly;

    /** 所属检查计划 id */
    private Long planId;

    private long page = 1;
    private long size = 10;
}
