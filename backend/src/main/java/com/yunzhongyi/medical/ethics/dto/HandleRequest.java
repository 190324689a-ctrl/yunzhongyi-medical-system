package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 投诉处理请求
 */
@Data
public class HandleRequest {

    /** 处理人（必填） */
    private String handler;

    /** 处理结果（必填） */
    private String handleResult;
}
