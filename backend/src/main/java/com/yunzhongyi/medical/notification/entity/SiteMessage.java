package com.yunzhongyi.medical.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 站内消息：消息提醒中心收到的消息本体。
 * <p>
 * 覆盖需求（需求梳理.md 第 29 项）：站内消息。
 * 接收范围 receiverScope：ALL 全部人员 / ROLE 按角色 / USER 按人员；
 * 按角色时 receiverValue 填角色编码（如 MEDICAL_ADMIN），按人员时填用户名。
 * 已读状态 readStatus：0 未读，1 已读；标已读时写 readTime。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_message")
public class SiteMessage extends BaseEntity {

    /** 消息标题 */
    @NotBlank(message = "消息标题不能为空")
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息分类：NOTICE 通知 / ANNOUNCE 公告 / TODO 待办提醒 / SYSTEM 系统消息 */
    private String category;

    /** 发送人姓名，如 医务科 / 系统 */
    private String senderName;

    /** 接收范围：ALL / ROLE / USER */
    private String receiverScope;

    /** 接收对象：按角色填角色编码，按人员填用户名，全部人员时为空 */
    private String receiverValue;

    /** 接收人姓名（按人员接收时填写） */
    private String receiverName;

    /** 已读状态：0 未读，1 已读 */
    private Integer readStatus;

    /** 已读时间 */
    private LocalDateTime readTime;

    /** 发送时间 */
    private LocalDateTime sendTime;

    /** 关联业务类型，如 LEAVE / AUTH / SCHEDULE / QUALITY */
    private String relatedType;

    /** 关联业务 id */
    private Long relatedId;

    /** 备注 */
    private String remark;
}
