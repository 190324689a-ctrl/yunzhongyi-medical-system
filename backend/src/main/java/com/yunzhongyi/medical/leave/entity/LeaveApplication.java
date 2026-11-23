package com.yunzhongyi.medical.leave.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 休假执业申请
 * <p>
 * 覆盖需求（需求梳理.md 第 2 项）：医师休假、执业、进修、外出进修、手术分级授权等
 * 申请与审批流程，支持在线申请、多级审批、查询、统计、打印预览。
 * <p>
 * 申请类型 applyType：LEAVE 休假 / PRACTICE 执业 / TRAINING 进修 / OUT_TRAINING 外出进修 /
 * SURGERY_AUTH 手术分级授权。
 * <p>
 * 审批状态 status：0 待科主任审批 / 1 待医务科审批 / 2 已通过 / 3 已驳回。
 * 多级审批：新建为 0；科主任通过后变为 1；医务科通过后变为 2；任一级驳回变为 3。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("leave_application")
public class LeaveApplication extends BaseEntity {

    /** 申请人医师 id */
    @NotNull(message = "申请人不能为空")
    private Long physicianId;

    /** 申请人姓名（冗余，便于列表展示与查询） */
    @NotBlank(message = "申请人姓名不能为空")
    private String physicianName;

    /** 所属科室 id */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 申请类型：LEAVE / PRACTICE / TRAINING / OUT_TRAINING / SURGERY_AUTH */
    @NotBlank(message = "申请类型不能为空")
    private String applyType;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 天数（开始-结束，含首尾；由服务端按日期算出） */
    private Integer days;

    /** 申请事由 */
    private String reason;

    /** 进修/执业地点：进修单位、执业地点等 */
    private String destination;

    /** 手术分级（仅 SURGERY_AUTH 申请用：1-4 级） */
    private Integer surgeryLevel;

    /** 审批状态：0 待科主任审批，1 待医务科审批，2 已通过，3 已驳回 */
    private Integer status;

    /** 当前审批人 */
    private String currentApprover;

    /** 最新审批意见 */
    private String approvalOpinion;

    /** 审批记录（多级审批留痕，如“科主任 张主任：通过；医务科 吴科长：通过”） */
    private String approvalHistory;

    /** 申请日期 */
    private LocalDate applyDate;

    /** 备注 */
    private String remark;
}
