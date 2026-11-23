package com.yunzhongyi.medical.notification.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 按 28 号提醒设置生成待办的请求：设置编码必填，设置须存在且启用；
 * 责任人必填；标题为空时用设置名称，待办类型为空时默认审批待办。
 */
@Data
public class GenerateTodoRequest {
    /** 28 号提醒设置编码，如 LEAVE_APPROVE */
    private String settingKey;
    /** 待办标题（为空时用设置名称） */
    private String title;
    /** 待办内容 */
    private String content;
    /** 待办类型：APPROVAL / EXPIRE / RECTIFY / OTHER */
    private String todoType;
    /** 责任人用户名 */
    private String assignee;
    /** 责任人姓名 */
    private String assigneeName;
    /** 责任科室 */
    private String deptName;
    /** 截止日期 */
    private LocalDate dueDate;
    /** 关联业务类型，如 LEAVE / AUTH */
    private String relatedType;
    /** 关联业务 id */
    private Long relatedId;
}
