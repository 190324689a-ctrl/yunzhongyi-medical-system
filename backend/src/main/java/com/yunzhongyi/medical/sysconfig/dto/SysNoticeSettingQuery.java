package com.yunzhongyi.medical.sysconfig.dto;

import lombok.Data;

/** 消息提醒设置多条件组合查询参数 */
@Data
public class SysNoticeSettingQuery {
    /** 关键字：设置名称 / 设置编码 / 触发事件模糊匹配 */
    private String keyword;
    /** 提醒渠道：SITE / SMS / WECHAT / EMAIL */
    private String channel;
    /** 是否启用：1 启用，0 停用 */
    private Integer enabled;
    private long page = 1;
    private long size = 10;
}
