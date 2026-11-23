package com.yunzhongyi.medical.schedule.dto;

import lombok.Data;

/**
 * 排班模板查询参数
 */
@Data
public class ScheduleTemplateQuery {

    /** 关键字：模板名称 模糊匹配 */
    private String keyword;

    /** 适用科室 id */
    private Long deptId;

    /** 排班类型：DEPT / OUTPATIENT / INPATIENT / EMERGENCY */
    private String scheduleType;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
