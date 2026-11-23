package com.yunzhongyi.medical.mdt.dto;

import lombok.Data;

/**
 * MDT 审批请求：审批人 + 审批意见
 */
@Data
public class MdtApprovalRequest {

    /** 审批人（医务科/科主任姓名） */
    private String approver;

    /** 审批意见 */
    private String opinion;
}
