package com.yunzhongyi.medical.schedule.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 排班多条件组合查询参数
 */
@Data
public class ScheduleQuery {

    /** 关键字：排班医师姓名 模糊匹配 */
    private String keyword;

    /** 医师 id */
    private Long physicianId;

    /** 科室 id */
    private Long deptId;

    /** 排班类型：DEPT / OUTPATIENT / INPATIENT / EMERGENCY */
    private String scheduleType;

    /** 班次：DAY / MORNING / AFTERNOON / NIGHT / REST */
    private String shiftCode;

    /** 排班日期起（含） */
    private LocalDate startDate;

    /** 排班日期止（含） */
    private LocalDate endDate;

    /** 调班/代班状态：0 无申请，1 待审批，2 已通过，3 已驳回 */
    private Integer changeStatus;

    /** 来源排班模板 id */
    private Long templateId;

    private long page = 1;
    private long size = 10;
}
