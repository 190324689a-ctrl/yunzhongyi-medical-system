package com.yunzhongyi.medical.newtech.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 新技术、新项目多条件组合查询参数
 */
@Data
public class NewTechQuery {

    /** 关键字：项目名称 / 申报人姓名 / 项目负责人 模糊匹配 */
    private String keyword;

    /** 申报科室 id */
    private Long deptId;

    /** 项目类型：NEW_TECH 新技术 / NEW_PROJECT 新项目 */
    private String projectType;

    /** 审批状态：0 待审批，1 已立项，2 已验收，3 已驳回，4 已推广 */
    private Integer status;

    /** 伦理审查状态：0 不需审查，1 待审查，2 已通过，3 未通过 */
    private Integer ethicsStatus;

    /** 申报人 id */
    private Long applicantId;

    /** 申报日期起（含） */
    private LocalDate startDate;

    /** 申报日期止（含） */
    private LocalDate endDate;

    private long page = 1;
    private long size = 10;
}
