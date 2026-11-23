package com.yunzhongyi.medical.statistics.dto;

import lombok.Data;

/**
 * 工作量多条件组合查询参数
 */
@Data
public class WorkloadQuery {

    /** 关键字：医师姓名 / 科室名称 模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 医师 id */
    private Long physicianId;

    /** 统计月份：yyyy-MM，如 2026-11 */
    private String statMonth;

    /** 统计月份起（含），yyyy-MM */
    private String startMonth;

    /** 统计月份止（含），yyyy-MM */
    private String endMonth;

    private long page = 1;
    private long size = 10;
}
