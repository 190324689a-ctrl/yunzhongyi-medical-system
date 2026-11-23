package com.yunzhongyi.medical.committee.dto;

import lombok.Data;

/**
 * 委员会多条件组合查询参数
 */
@Data
public class CommitteeQuery {

    /** 关键字：委员会名称 / 主任委员模糊匹配 */
    private String keyword;

    /** 委员会类别：MEDICAL_QUALITY / PHARMACY / INFECTION / TRANSFUSION / MEDICAL_RECORD / OTHER */
    private String type;

    /** 状态：1 在任，2 已换届，3 已解散 */
    private Integer status;

    /** 只看任期 90 天内到期（在任） */
    private Boolean expiringSoon;

    private long page = 1;
    private long size = 10;
}
