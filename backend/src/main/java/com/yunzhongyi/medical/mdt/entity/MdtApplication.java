package com.yunzhongyi.medical.mdt.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MDT 多学科会诊申请
 * <p>
 * 覆盖需求（需求梳理.md 第 6 项）：多学科会诊管理，支持 MDT 申请、审批、记录、统计。
 * <p>
 * 会诊类型 mdtType：IN_HOSPITAL 院内会诊 / OUT_HOSPITAL 院外会诊 /
 * REMOTE 远程会诊 / EMERGENCY 急会诊（见 MdtTypeUtil）。
 * <p>
 * 紧急程度 urgency：NORMAL 一般 / URGENT 紧急。
 * <p>
 * 审批状态 status：0 待审批，1 已通过，2 已驳回；一级审批（医务科/科主任）。
 * 已结束的申请（已通过/已驳回）不能再审批，也不能再修改；审批记录留痕在
 * approvalHistory。会诊记录见 {@link MdtRecord}，一次会诊可多次补充记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdt_application")
public class MdtApplication extends BaseEntity {

    /** 患者姓名 */
    @NotBlank(message = "患者姓名不能为空")
    private String patientName;

    /** 住院号 */
    private String inpatientNo;

    /** 会诊类型：IN_HOSPITAL 院内 / OUT_HOSPITAL 院外 / REMOTE 远程 / EMERGENCY 急会诊 */
    @NotBlank(message = "会诊类型不能为空")
    private String mdtType;

    /** 申请科室 id */
    private Long deptId;

    /** 申请科室名称（冗余） */
    private String deptName;

    /** 申请医师 id */
    private Long applyPhysicianId;

    /** 申请医师姓名（冗余） */
    @NotBlank(message = "申请医师不能为空")
    private String applyPhysicianName;

    /** 病情摘要 */
    private String conditionSummary;

    /** 拟邀请科室（多个用顿号分隔，如“骨伤科、放射科”） */
    private String invitedDepts;

    /** 拟邀请专家（多个用顿号分隔，如“王建军、周建国”） */
    private String invitedExperts;

    /** 期望会诊时间（前后端统一用 yyyy-MM-dd HH:mm:ss） */
    @NotNull(message = "期望会诊时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectedTime;

    /** 紧急程度：NORMAL 一般 / URGENT 紧急 */
    @NotBlank(message = "紧急程度不能为空")
    private String urgency;

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 审批人（医务科/科主任） */
    private String approver;

    /** 最新审批意见 */
    private String approvalOpinion;

    /** 审批记录（留痕，如“医务科 吴文斌：通过（同意）”） */
    private String approvalHistory;

    /** 申请日期 */
    private LocalDate applyDate;

    /** 备注 */
    private String remark;
}
