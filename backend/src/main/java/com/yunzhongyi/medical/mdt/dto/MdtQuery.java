package com.yunzhongyi.medical.mdt.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * MDT 多条件组合查询参数
 */
@Data
public class MdtQuery {

    /** 关键字：患者姓名 / 住院号 / 申请医师姓名 模糊匹配 */
    private String keyword;

    /** 申请科室 id */
    private Long deptId;

    /** 会诊类型：IN_HOSPITAL / OUT_HOSPITAL / REMOTE / EMERGENCY */
    private String mdtType;

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 紧急程度：NORMAL 一般 / URGENT 紧急 */
    private String urgency;

    /** 申请医师 id */
    private Long applyPhysicianId;

    /** 期望会诊日期起（含） */
    private LocalDate startDate;

    /** 期望会诊日期止（含） */
    private LocalDate endDate;

    private long page = 1;
    private long size = 10;
}
