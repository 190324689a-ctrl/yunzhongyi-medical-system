package com.yunzhongyi.medical.committee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 委员会决议
 * <p>
 * 覆盖需求（需求梳理.md 第 9 项）：委员会决议管理。
 * 一条决议属于一次会议（meetingId），同时冗余 committeeId 便于按委员会统计。
 * <p>
 * 决议状态 status：0 待执行，1 执行中，2 已完成，3 已取消。
 * 流转：待执行 → 执行中 → 已完成；待执行/执行中可取消。
 * 已完成/已取消的决议不能再流转、再修改；逾期按“待执行/执行中且
 * 决议期限 deadline 已过当天”动态判断，不单独存状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("committee_resolution")
public class CommitteeResolution extends BaseEntity {

    /** 会议 id */
    @NotNull(message = "会议不能为空")
    private Long meetingId;

    /** 委员会 id（冗余） */
    private Long committeeId;

    /** 决议事项 */
    @NotBlank(message = "决议事项不能为空")
    private String title;

    /** 决议内容 */
    private String content;

    /** 责任人 */
    private String responsiblePerson;

    /** 决议期限 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    /** 决议状态：0 待执行，1 执行中，2 已完成，3 已取消 */
    private Integer status;

    /** 完成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate completeDate;

    /** 备注 */
    private String remark;
}
