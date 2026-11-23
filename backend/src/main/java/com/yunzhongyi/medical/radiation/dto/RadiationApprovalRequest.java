package com.yunzhongyi.medical.radiation.dto;

import lombok.Data;

/**
 * 放射人员资质审核请求：审核人 + 审核意见
 */
@Data
public class RadiationApprovalRequest {

    /** 审核人（医务科） */
    private String approver;

    /** 审核意见 */
    private String opinion;
}
