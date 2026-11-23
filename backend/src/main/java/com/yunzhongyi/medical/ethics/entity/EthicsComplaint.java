package com.yunzhongyi.medical.ethics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医德医风投诉
 * <p>
 * 覆盖需求（需求梳理.md 第 12 项）：医德医风投诉。
 * <p>
 * 投诉类型 complaintType：ATTITUDE 服务态度 / QUALITY 医疗质量 /
 * CHARGE 收费问题 / INTEGRITY 廉洁行医 / OTHER 其他。
 * <p>
 * 流转 status：0 待受理 --受理--> 1 调查中 --处理--> 2 已处理
 * --反馈--> 3 已反馈；待受理的投诉可不予受理 --&gt; 4 不予受理。
 * 处理期限 deadlineDate 默认为投诉日期 + 15 天；状态未到已反馈且
 * 期限已过当天即“逾期”，按期限动态判断，不单独存状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ethics_complaint")
public class EthicsComplaint extends BaseEntity {

    /** 投诉编号，如 YD-TS-2026-001（全院唯一） */
    @NotBlank(message = "投诉编号不能为空")
    private String complaintNo;

    /** 投诉人姓名 */
    @NotBlank(message = "投诉人姓名不能为空")
    private String complainantName;

    /** 投诉人电话 */
    private String complainantPhone;

    /** 被投诉医师 id（可空：投诉对象不明时先登记再核实） */
    private Long physicianId;

    /** 被投诉医师姓名（冗余） */
    private String physicianName;

    /** 被投诉医师科室名称（冗余） */
    private String deptName;

    /** 投诉类型：ATTITUDE 服务态度 / QUALITY 医疗质量 / CHARGE 收费问题 / INTEGRITY 廉洁行医 / OTHER 其他 */
    @NotBlank(message = "投诉类型不能为空")
    private String complaintType;

    /** 投诉日期（默认当天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate complaintDate;

    /** 处理期限（默认投诉日期 + 15 天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadlineDate;

    /** 投诉内容 */
    private String content;

    /** 流转状态：0 待受理，1 调查中，2 已处理，3 已反馈，4 不予受理 */
    private Integer status;

    /** 受理/调查处理人 */
    private String handler;

    /** 处理结果（处理时必填） */
    private String handleResult;

    /** 处理日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate handleDate;

    /** 反馈内容（反馈时必填） */
    private String feedbackContent;

    /** 反馈日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate feedbackDate;

    /** 不予受理原因（驳回时必填） */
    private String rejectReason;

    /** 流转记录（受理/处理/反馈/不予受理留痕） */
    private String approvalHistory;

    /** 备注 */
    private String remark;
}
