package com.yunzhongyi.medical.committee.dto;

import lombok.Data;

/**
 * 委员会决议多条件组合查询参数
 */
@Data
public class CommitteeResolutionQuery {

    /** 关键字：决议事项 / 责任人模糊匹配 */
    private String keyword;

    /** 会议 id */
    private Long meetingId;

    /** 委员会 id */
    private Long committeeId;

    /** 决议状态：0 待执行，1 执行中，2 已完成，3 已取消 */
    private Integer status;

    /** 只看逾期（待执行/执行中且决议期限已过当天） */
    private Boolean overdue;

    private long page = 1;
    private long size = 10;
}
