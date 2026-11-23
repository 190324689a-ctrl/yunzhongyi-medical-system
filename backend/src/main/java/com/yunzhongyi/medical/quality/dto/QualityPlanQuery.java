package com.yunzhongyi.medical.quality.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 检查计划多条件组合查询参数
 */
@Data
public class QualityPlanQuery {

    /** 关键字：模板名称 / 被查科室 / 检查人 模糊匹配 */
    private String keyword;

    /** 检查类型：SELF / HOSPITAL / SPECIAL */
    private String inspectType;

    /** 被查科室 id */
    private Long deptId;

    /** 检查模板 id */
    private Long templateId;

    /** 计划状态：0 待执行，1 进行中，2 已完成，3 已取消 */
    private Integer status;

    /** 检查人 */
    private String inspector;

    /** 计划检查日期起（含） */
    private LocalDate startDate;

    /** 计划检查日期止（含） */
    private LocalDate endDate;

    private long page = 1;
    private long size = 10;
}
