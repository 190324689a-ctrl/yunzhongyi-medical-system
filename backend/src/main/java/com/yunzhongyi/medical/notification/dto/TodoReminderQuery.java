package com.yunzhongyi.medical.notification.dto;

import lombok.Data;

/** 待办提醒多条件组合查询参数 */
@Data
public class TodoReminderQuery {
    /** 关键字：标题 / 内容 / 责任人模糊匹配 */
    private String keyword;
    /** 待办类型：APPROVAL / EXPIRE / RECTIFY / OTHER */
    private String todoType;
    /** 状态：0 待办，1 已完成，2 已逾期 */
    private Integer status;
    /** 责任人用户名 */
    private String assignee;
    /** 来源：MANUAL / AUTO / SETTING */
    private String source;
    private long page = 1;
    private long size = 10;
}
