package com.yunzhongyi.medical.policy.dto;

import lombok.Data;

/**
 * 法规废止请求
 */
@Data
public class AbolishRequest {

    /** 废止原因（必填） */
    private String reason;
}
