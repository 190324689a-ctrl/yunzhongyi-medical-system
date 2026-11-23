package com.yunzhongyi.medical.personnel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医疗人员动态（人员变动）记录
 * <p>
 * 覆盖需求（需求梳理.md 第 7 项）：医疗人员动态、人员变动、人员调动、
 * 人员离职、人员退休等管理。
 * <p>
 * 变动类型 changeType：TRANSFER 调动 / RESIGN 离职 / RETIRE 退休 /
 * JOIN 入职 / REHIRE 返聘（见 PersonnelChangeTypeUtil）。
 * <p>
 * 审批状态 status：0 待审批，1 已通过，2 已驳回。
 * 全流程：登记变动（待审批）→ 医务科一级审批通过 → 已通过，审批记录留痕；
 * 任一环节驳回即已驳回。已结束的变动（已通过/已驳回）不能再审批、再修改。
 * <p>
 * 审批通过时同步医师档案（见 PersonnelChangeServiceImpl.approve）：
 * 调动改科室，离职改在职状态为离职，退休改为退休，入职/返聘改回在职并
 * 落到新科室；入职人员若尚未建档（physicianId 为空）则不同步，需在
 * 医师档案里补建档案。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("personnel_change")
public class PersonnelChange extends BaseEntity {

    /** 人员 id（医师档案 id；新入职人员尚未建档时可为空） */
    private Long physicianId;

    /** 人员姓名（冗余） */
    @NotBlank(message = "人员姓名不能为空")
    private String physicianName;

    /** 变动类型：TRANSFER 调动 / RESIGN 离职 / RETIRE 退休 / JOIN 入职 / REHIRE 返聘 */
    @NotBlank(message = "变动类型不能为空")
    private String changeType;

    /** 原科室 id（调动/离职/退休/返聘为变动前科室；入职为空） */
    private Long fromDeptId;

    /** 原科室名称（冗余） */
    private String fromDeptName;

    /** 调入/新科室 id（调动/入职/返聘为变动后科室；离职/退休为空） */
    private Long toDeptId;

    /** 调入/新科室名称（冗余） */
    private String toDeptName;

    /** 生效日期（变动正式生效的日期） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate changeDate;

    /** 申请/登记日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyDate;

    /** 变动原因 */
    private String reason;

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 审批人（医务科） */
    private String approver;

    /** 最新审批意见 */
    private String approvalOpinion;

    /** 审批记录（留痕，如“医务科 吴文斌：通过（同意调动）”） */
    private String approvalHistory;

    /** 备注 */
    private String remark;
}
