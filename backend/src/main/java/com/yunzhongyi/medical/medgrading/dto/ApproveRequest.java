package com.yunzhongyi.medical.medgrading.dto;

import lombok.Data;

/** 分级评定审批请求：审批人 + 审批意见 */
@Data
public class ApproveRequest {

    /** 审批人（必填） */
    private String approver;

    /** 审批意见 */
    private String opinion;
}
