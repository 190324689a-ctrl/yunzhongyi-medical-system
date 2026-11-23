package com.yunzhongyi.medical.techgrading.dto;

import lombok.Data;

/** 审批通过请求：审批人 + 审批意见 */
@Data
public class ApproveRequest {

    /** 审批人（必填） */
    private String approver;

    /** 审批意见（为空时默认“同意”） */
    private String opinion;
}
