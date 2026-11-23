package com.yunzhongyi.medical.mobile.dto;

import lombok.Data;

/** 移动端审批操作记录多条件组合查询参数 */
@Data
public class MobileApprovalLogQuery {
    /** 关键字：业务标题 / 审批人 / 设备编码模糊匹配 */
    private String keyword;
    /** 业务模块：LEAVE / AUTH / SCHEDULE / MDT / NEWTECH / PERSONNEL / MEDGRADING / BRIEFING */
    private String moduleType;
    /** 操作：APPROVE 通过 / REJECT 驳回 */
    private String action;
    /** 结果：SUCCESS 成功 / FAIL 失败 */
    private String result;
    /** 审批人用户名 */
    private String operator;
    private long page = 1;
    private long size = 10;
}
