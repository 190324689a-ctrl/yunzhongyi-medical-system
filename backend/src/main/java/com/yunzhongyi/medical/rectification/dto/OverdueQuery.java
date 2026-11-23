package com.yunzhongyi.medical.rectification.dto;

import lombok.Data;

/**
 * 逾期整改任务查询参数
 */
@Data
public class OverdueQuery {

    /** 关键字：整改责任人 / 问题描述模糊匹配 */
    private String keyword;

    /** 责任科室名称（模糊匹配，按问题责任科室） */
    private String deptName;

    private long page = 1;
    private long size = 10;
}
