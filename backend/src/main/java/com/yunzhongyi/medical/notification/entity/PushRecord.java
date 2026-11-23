package com.yunzhongyi.medical.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息推送记录：向微信 / 移动端 / 邮件推送消息的发送记录。
 * <p>
 * 覆盖需求（需求梳理.md 第 29 项）：消息推送。
 * 推送通道本机没有真实服务：发送调用为预留占位，明确返回失败、如实记录
 * （sendStatus=2 发送失败，failReason 写明通道未对接），绝不伪造发送成功。
 * 发送状态 sendStatus：0 待发送，1 已发送，2 发送失败。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_push")
public class PushRecord extends BaseEntity {

    /** 推送标题 */
    @NotBlank(message = "推送标题不能为空")
    private String title;

    /** 推送内容 */
    private String content;

    /** 推送渠道：WECHAT 微信 / APP 移动端 / EMAIL 邮件 */
    private String channel;

    /** 接收范围：ALL 全部人员 / ROLE 按角色 / USER 按人员 */
    private String receiverScope;

    /** 接收对象：按角色填角色编码，按人员填用户名，全部人员时为空 */
    private String receiverValue;

    /** 接收人姓名（按人员推送时填写） */
    private String receiverName;

    /** 发送状态：0 待发送，1 已发送，2 发送失败 */
    private Integer sendStatus;

    /** 发送时间（实际尝试发送的时间） */
    private LocalDateTime sendTime;

    /** 失败原因（发送失败时填写，如 通道未对接） */
    private String failReason;

    /** 关联站内消息 id */
    private Long relatedMessageId;

    /** 备注 */
    private String remark;
}
