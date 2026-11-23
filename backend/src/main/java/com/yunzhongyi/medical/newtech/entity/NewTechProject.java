package com.yunzhongyi.medical.newtech.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新技术、新项目申报
 * <p>
 * 覆盖需求（需求梳理.md 第 23 项）：新技术、新项目申报、审批、立项、验收、
 * 推广；支持在线申报、专家评审、伦理审查。
 * <p>
 * 项目类型 projectType：NEW_TECH 新技术 / NEW_PROJECT 新项目
 * （见 NewTechTypeUtil）。
 * <p>
 * 审批状态 status：0 待审批，1 已立项，2 已验收，3 已驳回，4 已推广。
 * 全流程：申报（待审批）→ 医务科一级审批通过 → 已立项 → 添加验收记录 →
 * 已验收 → 添加推广记录 → 已推广；任一环节驳回即已驳回。已结束的项目
 * （已驳回）不能再审批、再修改；已立项及之后的项目不能再修改申报内容。
 * <p>
 * 伦理审查 ethicsStatus：0 不需审查，1 待审查，2 已通过，3 未通过。
 * 需伦理审查（ethicsRequired=true）的项目，伦理未通过前不能审批立项。
 * 验收/推广进展见 {@link NewTechRecord}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("newtech_project")
public class NewTechProject extends BaseEntity {

    /** 项目名称 */
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    /** 项目类型：NEW_TECH 新技术 / NEW_PROJECT 新项目 */
    @NotBlank(message = "项目类型不能为空")
    private String projectType;

    /** 申报科室 id */
    private Long deptId;

    /** 申报科室名称（冗余） */
    private String deptName;

    /** 申报人 id */
    private Long applicantId;

    /** 申报人姓名（冗余） */
    @NotBlank(message = "申报人不能为空")
    private String applicantName;

    /** 项目负责人 */
    private String projectLeader;

    /** 项目组成员（多个用顿号分隔，如“陈国强、李雅琴”） */
    private String teamMembers;

    /** 立项依据 / 项目背景 */
    private String background;

    /** 项目目标 */
    private String objective;

    /** 技术方案 / 实施方案 */
    private String techPlan;

    /** 预期效益 */
    private String expectedBenefit;

    /** 经费预算（万元） */
    private BigDecimal budget;

    /** 计划开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** 计划结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 是否需伦理审查：true 需审查，false 不需 */
    private Boolean ethicsRequired;

    /** 伦理审查状态：0 不需审查，1 待审查，2 已通过，3 未通过 */
    private Integer ethicsStatus;

    /** 伦理审查人 */
    private String ethicsReviewer;

    /** 伦理审查意见 */
    private String ethicsOpinion;

    /** 伦理审查日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate ethicsReviewDate;

    /** 审批状态：0 待审批，1 已立项，2 已验收，3 已驳回，4 已推广 */
    private Integer status;

    /** 审批人（医务科） */
    private String approver;

    /** 最新审批意见 */
    private String approvalOpinion;

    /** 审批记录（留痕，如“医务科 吴文斌：通过（同意立项）”） */
    private String approvalHistory;

    /** 申报日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyDate;

    /** 备注 */
    private String remark;
}
