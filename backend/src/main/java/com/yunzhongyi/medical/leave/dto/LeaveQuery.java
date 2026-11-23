package com.yunzhongyi.medical.leave.dto;

import lombok.Data;

/**
 * 休假执业多条件组合查询参数
 */
@Data
public class LeaveQuery {

    /** 关键字：申请人姓名 模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 申请类型：LEAVE / PRACTICE / TRAINING / OUT_TRAINING / SURGERY_AUTH */
    private String applyType;

    /** 审批状态：0 待科主任审批，1 待医务科审批，2 已通过，3 已驳回 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
