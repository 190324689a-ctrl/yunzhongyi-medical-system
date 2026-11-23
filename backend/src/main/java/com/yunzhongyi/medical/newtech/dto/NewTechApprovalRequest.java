package com.yunzhongyi.medical.newtech.dto;

import lombok.Data;

/**
 * 新技术、新项目审批请求：审批人 + 审批意见
 */
@Data
public class NewTechApprovalRequest {

    /** 审批人（医务科姓名） */
    private String approver;

    /** 审批意见 */
    private String opinion;
}
