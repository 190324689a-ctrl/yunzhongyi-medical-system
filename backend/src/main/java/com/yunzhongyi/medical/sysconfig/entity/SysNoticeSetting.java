package com.yunzhongyi.medical.sysconfig.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息提醒设置：各类业务事件的消息推送开关与模板。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：消息提醒设置。
 * 注意：本模块只管“提醒规则的配置”（开/关、渠道、模板、接收范围）；
 * 实际的消息发送与待办提醒在第 29 项“消息提醒”模块实现。
 * <p>
 * 提醒渠道 channel：SITE 站内消息 / SMS 短信 / WECHAT 微信 / EMAIL 邮件。
 * 接收范围 receiverScope：ALL 全部人员 / ROLE 按角色 / USER 按人员；
 * 按角色时 receiverValue 填角色编码（如 MEDICAL_ADMIN），按人员时填用户名。
 * 状态 enabled：1 启用，0 停用；停用的规则不触发提醒。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice_setting")
public class SysNoticeSetting extends BaseEntity {

    /** 设置编码（唯一），如 LEAVE_APPROVE */
    @NotBlank(message = "设置编码不能为空")
    private String settingKey;

    /** 设置名称，如 休假审批提醒 */
    @NotBlank(message = "设置名称不能为空")
    private String settingName;

    /** 提醒渠道：SITE / SMS / WECHAT / EMAIL */
    private String channel;

    /** 触发事件，如 休假申请待审批 / 重点授权即将到期 */
    private String triggerEvent;

    /** 消息模板内容（支持 {姓名} {科室} 等占位符） */
    private String templateContent;

    /** 接收范围：ALL / ROLE / USER */
    private String receiverScope;

    /** 接收对象：按角色填角色编码，按人员填用户名，全部人员时为空 */
    private String receiverValue;

    /** 是否启用：1 启用，0 停用 */
    private Integer enabled;

    /** 备注 */
    private String remark;
}
