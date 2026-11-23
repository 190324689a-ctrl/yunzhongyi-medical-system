package com.yunzhongyi.medical.coresystem.dto;

import lombok.Data;

/**
 * 查阅与学习记录多条件组合查询参数
 */
@Data
public class CoreSystemReadingQuery {

    /** 制度 id（查某项制度的全部查阅学习记录时必填） */
    private Long coreSystemId;

    /** 关键字：查阅人姓名 / 科室模糊匹配 */
    private String keyword;

    /** 是否已学习：0 已查阅，1 已学习 */
    private Integer learned;

    private long page = 1;
    private long size = 10;
}
