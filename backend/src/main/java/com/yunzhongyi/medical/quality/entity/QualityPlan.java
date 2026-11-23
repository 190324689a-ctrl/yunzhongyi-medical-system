package com.yunzhongyi.medical.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 质量检查计划：按模板创建的一次检查安排。
 * <p>
 * 覆盖需求（需求梳理.md 第 13/14/15 项）：检查计划（科室自查 / 院级检查 /
 * 专项检查的计划、记录、报告、整改）。
 * <p>
 * 检查类型 inspectType：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查
 * （与模板的检查类型一致，见 QualityTypeUtil）。
 * <p>
 * 计划状态 status：0 待执行，1 进行中，2 已完成，3 已取消。
 * 状态流转：待执行 →（开始）进行中 →（完成）已完成；待执行/进行中可取消。
 * 只有待执行的计划能修改；已完成的计划需要至少一条检查记录。
 * 检查记录见 {@link QualityRecord}，问题与整改见 {@link QualityIssue}、
 * {@link QualityRectification}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_plan")
public class QualityPlan extends BaseEntity {

    /** 检查模板 id */
    private Long templateId;

    /** 检查模板名称（冗余） */
    private String templateName;

    /** 检查类型：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查 */
    @NotBlank(message = "检查类型不能为空")
    private String inspectType;

    /** 被查科室 id */
    private Long deptId;

    /** 被查科室名称（冗余） */
    @NotBlank(message = "被查科室不能为空")
    private String deptName;

    /** 检查人 / 检查组（多个用顿号分隔，如“吴文斌、陈国强”） */
    @NotBlank(message = "检查人不能为空")
    private String inspector;

    /** 计划检查日期 */
    @NotNull(message = "计划检查日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    /** 计划状态：0 待执行，1 进行中，2 已完成，3 已取消 */
    private Integer status;

    /** 备注 */
    private String remark;
}
