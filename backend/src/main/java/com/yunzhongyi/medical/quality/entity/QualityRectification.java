package com.yunzhongyi.medical.quality.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 整改任务：针对一个检查问题发出的整改任务，支持复查确认。
 * <p>
 * 覆盖需求（需求梳理.md 第 13/14/15 项）：整改反馈、整改跟踪。
 * <p>
 * 一个问题只发一个整改任务（重复发会被拒绝）。
 * <p>
 * 整改状态 status：0 待整改，1 已整改（待复查），2 已复查通过。
 * 状态流转：待整改 →（提交整改）已整改 →（复查通过）已复查通过；
 * 复查不通过时打回待整改，可重新提交。
 * <p>
 * 逾期 overdue：非数据库字段，查询时按“待整改/已整改且整改期限已过当天”
 * 动态算出，不单独存状态——期限到了还没复查通过就是逾期。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_rectification")
public class QualityRectification extends BaseEntity {

    /** 所属检查问题 id（服务端从路径参数补值，不由客户端传） */
    private Long issueId;

    /** 所属检查计划 id（冗余，取自问题） */
    private Long planId;

    /** 所属检查记录 id（冗余，取自问题） */
    private Long recordId;

    /** 整改措施 */
    @NotBlank(message = "整改措施不能为空")
    private String measure;

    /** 整改期限 */
    @NotNull(message = "整改期限不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    /** 整改责任人 */
    @NotBlank(message = "整改责任人不能为空")
    private String respPerson;

    /** 整改状态：0 待整改，1 已整改（待复查），2 已复查通过 */
    private Integer status;

    /** 整改完成日期（提交整改时填） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate rectifyDate;

    /** 复查人 */
    private String reviewer;

    /** 复查意见 */
    private String reviewOpinion;

    /** 复查日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reviewDate;

    /** 备注 */
    private String remark;

    /** 是否逾期（非数据库字段）：待整改/已整改且整改期限已过当天 */
    @TableField(exist = false)
    private Boolean overdue;
}
