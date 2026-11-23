package com.yunzhongyi.medical.personnel.dto;

import lombok.Data;

/**
 * 医疗人员动态审批请求：审批人 + 审批意见
 */
@Data
public class PersonnelChangeApprovalRequest {

    /** 审批人（医务科姓名） */
    private String approver;

    /** 审批意见 */
    private String opinion;
}
