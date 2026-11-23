package com.yunzhongyi.medical.personnel.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 医疗人员动态多条件组合查询参数
 */
@Data
public class PersonnelChangeQuery {

    /** 关键字：人员姓名模糊匹配 */
    private String keyword;

    /** 相关科室 id：原科室或调入/新科室任一匹配 */
    private Long deptId;

    /** 变动类型：TRANSFER 调动 / RESIGN 离职 / RETIRE 退休 / JOIN 入职 / REHIRE 返聘 */
    private String changeType;

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 人员 id */
    private Long physicianId;

    /** 生效日期起（含） */
    private LocalDate startDate;

    /** 生效日期止（含） */
    private LocalDate endDate;

    private long page = 1;
    private long size = 10;
}
