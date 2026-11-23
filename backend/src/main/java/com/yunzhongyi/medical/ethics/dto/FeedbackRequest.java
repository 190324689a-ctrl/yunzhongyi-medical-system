package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 投诉反馈请求
 */
@Data
public class FeedbackRequest {

    /** 反馈人（可空，默认处理人） */
    private String handler;

    /** 反馈内容（必填） */
    private String feedbackContent;
}
