package com.yunzhongyi.medical.document.dto;

import lombok.Data;

/**
 * 文件发布（一级审批）请求
 */
@Data
public class PublishRequest {

    /** 审批人（必填） */
    private String approver;

    /** 审批意见（可空，默认“同意”） */
    private String opinion;
}
