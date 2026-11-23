package com.yunzhongyi.medical.mobile.dto;

import lombok.Data;

/** 移动端审批请求：审批人、执行设备与审批意见 */
@Data
public class MobileApproveRequest {
    /** 审批人用户名 */
    private String operator;
    /** 执行审批的设备编码 */
    private String deviceCode;
    /** 审批意见（驳回时必填，为驳回原因） */
    private String opinion;
}
