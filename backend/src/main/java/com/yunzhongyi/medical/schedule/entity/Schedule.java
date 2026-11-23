package com.yunzhongyi.medical.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 排班记录
 * <p>
 * 覆盖需求（需求梳理.md 第 5 项）：科室、门诊、住院、急诊排班；支持按科室、
 * 按人员、按时间排班；支持排班模板、批量排班、调班、代班；支持与 HIS 对接。
 * <p>
 * 排班类型 scheduleType：DEPT 科室排班 / OUTPATIENT 门诊排班 /
 * INPATIENT 住院排班 / EMERGENCY 急诊排班。
 * <p>
 * 班次 shiftCode：DAY 白班 / MORNING 上午班 / AFTERNOON 下午班 /
 * NIGHT 夜班 / REST 休息；班次默认时间段见 ScheduleShiftUtil。
 * <p>
 * 校验：同一医师同一天只能有一条排班记录（保存/批量生成/调班代班审批时都校验）。
 * <p>
 * 调班/代班 changeType：SWAP 调班（换班次/换日期）/ SUBSTITUTE 代班（换人）；
 * 一级审批（科主任审批）：changeStatus 0 无申请 / 1 待科主任审批 / 2 已通过 / 3 已驳回。
 * 通过后把目标班次/目标人员写回本记录，审批记录留痕在 approvalHistory。
 * <p>
 * HIS 对接：hisSyncStatus 0 未同步 / 1 已同步 / 2 同步失败；目前没有真实 HIS 系统
 * 可连，同步接口只是预留占位（见 ScheduleService#syncToHis），不伪造同步成功。
 * <p>
 * 表名用 duty_schedule：schedule 在部分数据库是保留字，建表会报语法错误。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("duty_schedule")
public class Schedule extends BaseEntity {

    /** 排班医师 id */
    @NotNull(message = "排班医师不能为空")
    private Long physicianId;

    /** 排班医师姓名（冗余，便于列表展示与查询） */
    @NotBlank(message = "排班医师姓名不能为空")
    private String physicianName;

    /** 所属科室 id */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 排班日期 */
    @NotNull(message = "排班日期不能为空")
    private LocalDate scheduleDate;

    /** 排班类型：DEPT 科室 / OUTPATIENT 门诊 / INPATIENT 住院 / EMERGENCY 急诊 */
    @NotBlank(message = "排班类型不能为空")
    private String scheduleType;

    /** 班次：DAY 白班 / MORNING 上午班 / AFTERNOON 下午班 / NIGHT 夜班 / REST 休息 */
    @NotBlank(message = "班次不能为空")
    private String shiftCode;

    /** 班次开始时间（如 08:00；夜班 17:30；休息为空） */
    private LocalTime startTime;

    /** 班次结束时间（如 17:30；夜班次日 08:00；休息为空） */
    private LocalTime endTime;

    /** 来源排班模板 id（手工排班为空） */
    private Long templateId;

    /** 来源排班模板名称（冗余） */
    private String templateName;

    /** 调班/代班类型：SWAP 调班 / SUBSTITUTE 代班；无申请为空 */
    private String changeType;

    /** 调班/代班状态：0 无申请，1 待科主任审批，2 已通过，3 已驳回 */
    private Integer changeStatus;

    /** 调班目标日期（仅 SWAP 用） */
    private LocalDate targetDate;

    /** 调班目标班次（仅 SWAP 用） */
    private String targetShiftCode;

    /** 调班目标班次开始时间（仅 SWAP 用） */
    private LocalTime targetStartTime;

    /** 调班目标班次结束时间（仅 SWAP 用） */
    private LocalTime targetEndTime;

    /** 代班目标医师 id（仅 SUBSTITUTE 用） */
    private Long targetPhysicianId;

    /** 代班目标医师姓名（仅 SUBSTITUTE 用） */
    private String targetPhysicianName;

    /** 调班/代班事由 */
    private String changeReason;

    /** 审批人（科主任） */
    private String approver;

    /** 最新审批意见 */
    private String approvalOpinion;

    /** 审批记录（留痕，如“科主任 林晓梅：通过（调班至 2026-11-26 白班）”） */
    private String approvalHistory;

    /** 申请日期 */
    private LocalDate applyDate;

    /** HIS 同步状态：0 未同步，1 已同步，2 同步失败（预留，未真实对接） */
    private Integer hisSyncStatus;

    /** HIS 同步结果说明 */
    private String hisSyncMsg;

    /** 备注 */
    private String remark;
}
