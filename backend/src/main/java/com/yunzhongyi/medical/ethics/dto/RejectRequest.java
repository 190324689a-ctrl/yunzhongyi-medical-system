package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 投诉不予受理 / 考评表彰驳回请求
 */
@Data
public class RejectRequest {

    /** 审批人/处理人（必填） */
    private String approver;

    /** 驳回原因（必填） */
    private String reason;
}
