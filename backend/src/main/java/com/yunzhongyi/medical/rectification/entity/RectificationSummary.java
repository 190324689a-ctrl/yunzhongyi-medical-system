package com.yunzhongyi.medical.rectification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 检查整改汇总报告：按统计周期（yyyy-MM）+ 汇总范围生成的一条汇总记录。
 * <p>
 * 覆盖需求（需求梳理.md 第 17 项）：检查问题汇总、整改汇总、统计分析。
 * <p>
 * 本模块不重复建检查问题/整改任务——问题与整改任务仍由质量检查管理
 * （第 13/14/15 项，quality_issue / quality_rectification）维护，
 * 本模块汇总它们：按科室 / 按检查类型 / 按时间汇总问题数与整改情况，
 * 并把汇总结果落成一条可查询、可导出、可重新生成的汇总报告。
 * <p>
 * 汇总范围 scopeType：ALL 全院 / DEPT 按科室 / TYPE 按检查类型。
 * ALL 时 deptName 与 inspectType 为空；DEPT 时 deptName 必填；
 * TYPE 时 inspectType 必填（SELF 科室自查 / HOSPITAL 院级检查 /
 * SPECIAL 专项检查，见 quality.service.QualityTypeUtil）。
 * <p>
 * 整改完成率 rectificationRate = 已复查通过数 / 整改任务总数 × 100，
 * 无整改任务时为 0。逾期数 overdueCount = 该范围内逾期整改任务数，
 * 逾期按“待整改/已整改且整改期限已过当天”动态判断（与质量检查模块一致）。
 * <p>
 * 同一（统计周期、汇总范围、科室、检查类型）只保留一条汇总记录：
 * 重新生成时原地更新（见 RectificationSummaryService#generate），
 * 导入时重复的同样跳过。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rectification_summary")
public class RectificationSummary extends BaseEntity {

    /** 统计周期，如 2026-11（yyyy-MM） */
    @NotBlank(message = "统计周期不能为空")
    private String statPeriod;

    /** 汇总范围：ALL 全院 / DEPT 按科室 / TYPE 按检查类型 */
    @NotBlank(message = "汇总范围不能为空")
    private String scopeType;

    /** 科室名称（按科室汇总时必填，全院/按类型汇总时为空） */
    private String deptName;

    /** 检查类型：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查（按检查类型汇总时必填） */
    private String inspectType;

    /** 检查计划数（该周期、该范围内的检查计划） */
    private Integer planCount;

    /** 检查记录数 */
    private Integer recordCount;

    /** 检查平均分（保留两位小数，无记录时为 0） */
    private BigDecimal avgScore;

    /** 检查问题总数 */
    private Integer issueCount;

    /** 严重问题数 */
    private Integer majorCount;

    /** 一般问题数 */
    private Integer minorCount;

    /** 轻微问题数 */
    private Integer mildCount;

    /** 整改任务总数 */
    private Integer rectificationTotal;

    /** 待整改任务数 */
    private Integer rectificationPending;

    /** 已整改（待复查）任务数 */
    private Integer rectificationDone;

    /** 已复查通过任务数 */
    private Integer rectificationReviewed;

    /** 整改完成率（%，保留两位小数）= 已复查通过 / 整改任务总数 × 100 */
    private BigDecimal rectificationRate;

    /** 逾期整改任务数 */
    private Integer overdueCount;

    /** 生成人 */
    private String generatedBy;

    /** 备注 */
    private String remark;
}
