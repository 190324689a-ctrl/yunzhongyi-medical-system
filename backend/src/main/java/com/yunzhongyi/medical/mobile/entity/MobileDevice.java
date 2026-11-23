package com.yunzhongyi.medical.mobile.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 移动端设备登记：医师在手机上安装医务管理 App 后登记的设备。
 * <p>
 * 覆盖需求（需求梳理.md 第 31 项）：移动端支持 iOS/Android。
 * 平台 platform：IOS / ANDROID；状态 status：1 已绑定，0 已解绑；
 * 设备编码 deviceCode 全院唯一，一台设备只登记一次，重复登记拒绝。
 * 解绑后可重新绑定；已绑定的设备不可直接删除，须先解绑。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mobile_device")
public class MobileDevice extends BaseEntity {

    /** 设备编码，全院唯一，如 DEV-001 */
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    /** 使用人用户名 */
    @NotBlank(message = "使用人不能为空")
    private String username;

    /** 使用人姓名（数据库列 user_name） */
    @TableField("user_name")
    private String displayName;

    /** 平台：IOS / ANDROID */
    private String platform;

    /** 设备型号，如 iPhone 15 Pro / Huawei Mate 60 */
    private String deviceModel;

    /** App 版本，如 1.0.0 */
    private String appVersion;

    /** 推送 token（用于消息推送，通道未对接时仅登记不调用） */
    private String pushToken;

    /** 状态：1 已绑定，0 已解绑 */
    private Integer status;

    /** 最近活跃时间 */
    private LocalDateTime lastActiveTime;

    /** 备注 */
    private String remark;
}
