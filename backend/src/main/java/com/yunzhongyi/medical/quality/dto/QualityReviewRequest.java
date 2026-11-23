package com.yunzhongyi.medical.quality.dto;

import lombok.Data;

/**
 * 整改复查请求：复查人 + 复查意见
 */
@Data
public class QualityReviewRequest {

    /** 复查人 */
    private String reviewer;

    /** 复查意见 */
    private String opinion;
}
