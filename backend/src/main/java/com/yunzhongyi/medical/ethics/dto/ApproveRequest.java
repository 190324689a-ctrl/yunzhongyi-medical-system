package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 医德考评 / 表彰一级审批请求
 */
@Data
public class ApproveRequest {

    /** 审批人（必填） */
    private String approver;

    /** 审批意见（可空，默认“同意”） */
    private String opinion;
}
