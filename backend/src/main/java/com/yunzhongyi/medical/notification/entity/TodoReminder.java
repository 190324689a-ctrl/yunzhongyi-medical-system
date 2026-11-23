package com.yunzhongyi.medical.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办提醒：由各业务待审批事项汇总生成，或手工创建。
 * <p>
 * 覆盖需求（需求梳理.md 第 29 项）：待办提醒。
 * 待办类型 todoType：APPROVAL 审批待办 / EXPIRE 到期提醒 / RECTIFY 整改待办 / OTHER 其他待办。
 * 状态 status：0 待办，1 已完成，2 已逾期；逾期由逾期扫描判定（待办且截止日期早于当天）。
 * 来源 source：MANUAL 手工创建 / AUTO 系统生成 / SETTING 按 28 号提醒设置生成；
 * 按提醒设置生成时 settingKey 填 28 号提醒设置的编码，设置停用或不存在时拒绝生成。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_todo")
public class TodoReminder extends BaseEntity {

    /** 待办标题 */
    @NotBlank(message = "待办标题不能为空")
    private String title;

    /** 待办内容 */
    private String content;

    /** 待办类型：APPROVAL / EXPIRE / RECTIFY / OTHER */
    private String todoType;

    /** 关联业务类型，如 LEAVE / AUTH / SCHEDULE / QUALITY */
    private String relatedType;

    /** 关联业务 id */
    private Long relatedId;

    /** 责任人用户名 */
    private String assignee;

    /** 责任人姓名 */
    private String assigneeName;

    /** 责任科室 */
    private String deptName;

    /** 截止日期 */
    private LocalDate dueDate;

    /** 状态：0 待办，1 已完成，2 已逾期 */
    private Integer status;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 来源：MANUAL / AUTO / SETTING */
    private String source;

    /** 关联的 28 号提醒设置编码（来源为 SETTING 时填写） */
    private String settingKey;

    /** 备注 */
    private String remark;
}
