package com.yunzhongyi.medical.rectification.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 检查问题汇总查询参数：分页查检查问题，并带出其整改任务状态、
 * 整改期限与逾期标记（问题汇总视图用）。
 */
@Data
public class IssueSummaryQuery {

    /** 关键字：问题描述 / 责任人模糊匹配 */
    private String keyword;

    /** 责任科室名称（模糊匹配） */
    private String deptName;

    /** 严重程度：MAJOR 严重 / MINOR 一般 / MILD 轻微 */
    private String severity;

    /** 检查类型：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查 */
    private String inspectType;

    /** 发现日期起 */
    private LocalDate startDate;

    /** 发现日期止 */
    private LocalDate endDate;

    /** 只看逾期（整改任务待整改/已整改且期限已过当天） */
    private Boolean overdueOnly;

    /** 只看未完成整改（无整改任务，或整改任务未复查通过） */
    private Boolean unrectifiedOnly;

    private long page = 1;
    private long size = 10;
}
