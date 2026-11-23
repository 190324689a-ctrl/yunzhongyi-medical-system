package com.yunzhongyi.medical.notification.dto;

import lombok.Data;

/** 消息推送记录多条件组合查询参数 */
@Data
public class PushRecordQuery {
    /** 关键字：标题 / 内容 / 接收人模糊匹配 */
    private String keyword;
    /** 推送渠道：WECHAT / APP / EMAIL */
    private String channel;
    /** 发送状态：0 待发送，1 已发送，2 发送失败 */
    private Integer sendStatus;
    private long page = 1;
    private long size = 10;
}
