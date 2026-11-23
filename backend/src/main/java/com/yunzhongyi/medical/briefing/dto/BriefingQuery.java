package com.yunzhongyi.medical.briefing.dto;

import lombok.Data;

/**
 * 医疗工作简报分页查询条件：关键字（期号/标题/撰稿人）/ 分类 / 发布状态 / 统计周期 / 模板。
 */
@Data
public class BriefingQuery {

    /** 关键字：期号 / 标题 / 撰稿人 */
    private String keyword;

    /** 简报分类：MONTHLY / QUARTERLY / ANNUAL / SPECIAL / DYNAMIC */
    private String category;

    /** 发布状态：0 草稿，1 待审批，2 已发布，3 已驳回 */
    private Integer status;

    /** 统计周期：yyyy-MM */
    private String period;

    /** 模板 id */
    private Long templateId;

    /** 只看已发布 */
    private Boolean publishedOnly;

    private long page = 1;

    private long size = 10;
}
