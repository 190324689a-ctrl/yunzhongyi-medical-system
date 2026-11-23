package com.yunzhongyi.medical.medgrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医疗分级标准：一次分级评定所依据的分数线方案。
 * <p>
 * 覆盖需求（需求梳理.md 第 22 项）：医疗分级、医疗质量分级管理。
 * <p>
 * 分级范围 scope：QUALITY 医疗质量分级 / SERVICE 医疗服务分级 /
 * COMPREHENSIVE 综合分级。
 * <p>
 * 分数线：excellentMin 甲级线 / goodMin 乙级线 / passMin 丙级线，
 * 须满足 0 &lt; passMin &lt; goodMin &lt; excellentMin &le; 100；
 * 低于丙级线为丁级。评定打分时按所属标准的分数线自动算等级。
 * <p>
 * 状态 status：1 启用，0 停用；只有启用的标准可用于新建分级评定。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("grading_standard")
public class GradingStandard extends BaseEntity {

    /** 标准编码（唯一），如 STD-Q001 */
    @NotBlank(message = "标准编码不能为空")
    private String standardCode;

    /** 标准名称，如 医疗质量分级标准 */
    @NotBlank(message = "标准名称不能为空")
    private String standardName;

    /** 分级范围：QUALITY / SERVICE / COMPREHENSIVE */
    @NotBlank(message = "分级范围不能为空")
    private String scope;

    /** 甲级线（默认 90） */
    private BigDecimal excellentMin;

    /** 乙级线（默认 80） */
    private BigDecimal goodMin;

    /** 丙级线（默认 60） */
    private BigDecimal passMin;

    /** 有效期起 */
    private LocalDate validFrom;

    /** 有效期止 */
    private LocalDate validTo;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
