package com.yunzhongyi.medical.mobile.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 移动端待审批事项：各业务模块待审批事项的统一聚合视图（只读）。
 * 由 {@code MobileApprovalService.pendingList} 聚合生成，不单独建表。
 */
@Data
public class MobilePendingItem {

    /** 业务模块：LEAVE / AUTH / SCHEDULE / MDT / NEWTECH / PERSONNEL / MEDGRADING / BRIEFING */
    private String moduleType;

    /** 业务模块中文名，如“休假执业” */
    private String moduleName;

    /** 关联业务 id */
    private Long relatedId;

    /** 事项标题，如“孙小红休假申请” */
    private String title;

    /** 申请人 / 责任人姓名 */
    private String applicant;

    /** 所属科室 */
    private String deptName;

    /** 业务模块内的审批状态码 */
    private Integer status;

    /** 审批状态中文，如“待科主任审批” */
    private String statusText;

    /** 申请提交时间 */
    private LocalDateTime createTime;
}
