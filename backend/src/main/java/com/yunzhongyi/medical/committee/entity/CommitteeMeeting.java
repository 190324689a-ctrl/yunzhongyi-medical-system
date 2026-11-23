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
 * 委员会会议记录
 * <p>
 * 覆盖需求（需求梳理.md 第 9 项）：委员会会议记录管理。
 * 一次会议属于一个委员会（committeeId）；只有在任的委员会才能新增会议，
 * 已换届/已解散的委员会只保留历史会议记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("committee_meeting")
public class CommitteeMeeting extends BaseEntity {

    /** 委员会 id */
    @NotNull(message = "委员会不能为空")
    private Long committeeId;

    /** 委员会名称（冗余） */
    private String committeeName;

    /** 会议主题 */
    @NotBlank(message = "会议主题不能为空")
    private String title;

    /** 会议日期 */
    @NotNull(message = "会议日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate meetingDate;

    /** 会议地点 */
    private String location;

    /** 主持人 */
    private String host;

    /** 记录人 */
    private String recorder;

    /** 参会人数 */
    private Integer attendeeCount;

    /** 参会人员（姓名清单） */
    private String attendees;

    /** 会议内容 */
    private String content;

    /** 备注 */
    private String remark;
}
