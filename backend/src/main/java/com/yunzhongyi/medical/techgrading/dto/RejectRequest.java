package com.yunzhongyi.medical.techgrading.dto;

import lombok.Data;

/** 审批驳回 / 停用 / 注销请求：审批人 + 原因 */
@Data
public class RejectRequest {

    /** 审批人（驳回时必填；停用/注销时可空） */
    private String approver;

    /** 原因（必填）：驳回原因 / 停用原因 / 注销原因 */
    private String reason;
}
