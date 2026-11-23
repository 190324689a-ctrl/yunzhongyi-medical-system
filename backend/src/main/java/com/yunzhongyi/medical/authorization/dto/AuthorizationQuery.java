package com.yunzhongyi.medical.authorization.dto;

import lombok.Data;

/**
 * 重点授权多条件组合查询参数
 */
@Data
public class AuthorizationQuery {

    /** 关键字：申请人姓名 模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 授权类型：SURGERY / ANESTHESIA / ANTITUMOR / PSYCHOTROPIC */
    private String authType;

    /** 审批状态：0 待科主任审批，1 待医务科审批，2 已通过，3 已驳回 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
