package com.yunzhongyi.medical.medgrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医疗分级评定：某科室在某评定周期内按某分级标准的一次分级结果。
 * <p>
 * 覆盖需求（需求梳理.md 第 22 项）：医疗分级、医疗质量分级管理。
 * <p>
 * 等级 grade 由评分 score 按所属标准的分数线自动算：
 * A 甲级 / B 乙级 / C 丙级 / D 丁级，改评分时重算。
 * <p>
 * 状态 status：0 待审批，1 已通过，2 已驳回；一级审批，审批人、审批意见、
 * 审批日期留痕。同一科室 + 同一标准 + 同一周期只允许一条评定。
 * <p>
 * 科室 deptId/deptName 取自 sys_dept（科室管理底座）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("grading_assessment")
public class GradingAssessment extends BaseEntity {

    /** 所属分级标准 id */
    @NotNull(message = "分级标准不能为空")
    private Long standardId;

    /** 所属分级标准编码（冗余，用于导入导出按编码匹配） */
    private String standardCode;

    /** 所属分级标准名称（冗余） */
    private String standardName;

    /** 评定科室 id */
    private Long deptId;

    /** 评定科室名称 */
    @NotBlank(message = "评定科室不能为空")
    private String deptName;

    /** 评定周期 yyyy-MM，如 2026-11 */
    @NotBlank(message = "评定周期不能为空")
    private String period;

    /** 评分：0-100 */
    @NotNull(message = "评分不能为空")
    private BigDecimal score;

    /** 等级：A 甲级 / B 乙级 / C 丙级 / D 丁级（按标准分数线自动算） */
    private String grade;

    /** 状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 评定人 */
    private String assessor;

    /** 评定日期（默认当天） */
    private LocalDate assessDate;

    /** 审批人 */
    private String approver;

    /** 审批意见 */
    private String approveOpinion;

    /** 审批日期 */
    private LocalDate approveDate;

    /** 备注 */
    private String remark;
}
