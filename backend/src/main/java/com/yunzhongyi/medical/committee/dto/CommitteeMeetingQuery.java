package com.yunzhongyi.medical.committee.dto;

import lombok.Data;

/**
 * 委员会会议记录多条件组合查询参数
 */
@Data
public class CommitteeMeetingQuery {

    /** 关键字：会议主题 / 主持人 / 委员会名称模糊匹配 */
    private String keyword;

    /** 委员会 id */
    private Long committeeId;

    /** 会议开始日期（yyyy-MM-dd） */
    private String dateFrom;

    /** 会议结束日期（yyyy-MM-dd） */
    private String dateTo;

    private long page = 1;
    private long size = 10;
}
