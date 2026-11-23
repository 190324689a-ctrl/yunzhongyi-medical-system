package com.yunzhongyi.medical.notification.dto;

import lombok.Data;

/** 站内消息多条件组合查询参数 */
@Data
public class SiteMessageQuery {
    /** 关键字：标题 / 内容 / 发送人模糊匹配 */
    private String keyword;
    /** 消息分类：NOTICE / ANNOUNCE / TODO / SYSTEM */
    private String category;
    /** 已读状态：0 未读，1 已读 */
    private Integer readStatus;
    /** 接收范围：ALL / ROLE / USER */
    private String receiverScope;
    private long page = 1;
    private long size = 10;
}
