package com.yunzhongyi.medical.coresystem.dto;

import lombok.Data;

/**
 * 核心制度废止请求
 */
@Data
public class AbolishRequest {

    /** 废止原因（必填） */
    private String reason;
}
