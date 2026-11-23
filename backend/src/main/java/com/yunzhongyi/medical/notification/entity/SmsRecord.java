package com.yunzhongyi.medical.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 短信提醒记录：向手机发送短信提醒的发送记录。
 * <p>
 * 覆盖需求（需求梳理.md 第 29 项）：短信提醒。
 * 短信网关本机没有真实服务：发送调用为预留占位，明确返回失败、如实记录
 * （sendStatus=2 发送失败，failReason 写明网关未对接），绝不伪造发送成功。
 * 发送状态 sendStatus：0 待发送，1 已发送，2 发送失败。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_sms")
public class SmsRecord extends BaseEntity {

    /** 接收手机号（11 位大陆手机号） */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 短信内容 */
    private String content;

    /** 接收人姓名 */
    private String receiverName;

    /** 短信模板编码，如 AUTH_EXPIRE */
    private String templateCode;

    /** 发送状态：0 待发送，1 已发送，2 发送失败 */
    private Integer sendStatus;

    /** 发送时间（实际尝试发送的时间） */
    private LocalDateTime sendTime;

    /** 失败原因（发送失败时填写，如 网关未对接） */
    private String failReason;

    /** 关联业务类型，如 AUTH / QUALITY / SCHEDULE */
    private String relatedType;

    /** 关联业务 id */
    private Long relatedId;

    /** 备注 */
    private String remark;
}
