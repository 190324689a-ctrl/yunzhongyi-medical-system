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
 * MDT 会诊记录：一次会诊的实际开展情况，可多次补充记录。
 * <p>
 * 只有审批通过的 MDT 申请才能添加会诊记录；记录写在哪个申请下由 mdtId 关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdt_record")
public class MdtRecord extends BaseEntity {

    /** 所属 MDT 申请 id（服务端从路径参数补值，不由客户端传） */
    private Long mdtId;

    /** 实际参会专家（多个用顿号分隔，如“陈国强、王建军、周建国”） */
    @NotBlank(message = "实际参会专家不能为空")
    private String attendees;

    /** 实际会诊时间（前后端统一用 yyyy-MM-dd HH:mm:ss） */
    @NotNull(message = "会诊时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime meetingTime;

    /** 会诊地点（如“医务科会议室”“远程会诊中心”） */
    private String meetingPlace;

    /** 会诊意见/结论 */
    @NotBlank(message = "会诊意见/结论不能为空")
    private String opinion;

    /** 后续治疗建议 */
    private String suggestion;

    /** 记录人 */
    private String recorder;

    /** 记录日期 */
    private LocalDate recordDate;
}
