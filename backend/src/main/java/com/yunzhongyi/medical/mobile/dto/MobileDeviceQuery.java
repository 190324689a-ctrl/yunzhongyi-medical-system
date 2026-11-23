package com.yunzhongyi.medical.mobile.dto;

import lombok.Data;

/** 移动端设备多条件组合查询参数 */
@Data
public class MobileDeviceQuery {
    /** 关键字：设备编码 / 使用人 / 设备型号模糊匹配 */
    private String keyword;
    /** 平台：IOS / ANDROID */
    private String platform;
    /** 状态：1 已绑定，0 已解绑 */
    private Integer status;
    /** 使用人用户名 */
    private String username;
    private long page = 1;
    private long size = 10;
}
