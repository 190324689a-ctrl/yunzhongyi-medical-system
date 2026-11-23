package com.yunzhongyi.medical.document.dto;

import lombok.Data;

/**
 * 文件作废请求
 */
@Data
public class AbolishRequest {

    /** 作废原因（必填） */
    private String reason;
}
