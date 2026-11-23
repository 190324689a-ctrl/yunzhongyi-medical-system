package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 医德考评记录多条件组合查询参数
 */
@Data
public class EthicsEvaluationQuery {

    /** 关键字：被考评医师姓名模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 考评模板 id */
    private Long templateId;

    /** 考评年度 */
    private Integer assessYear;

    /** 等级：EXCELLENT 优秀 / QUALIFIED 合格 / BASIC 基本合格 / UNQUALIFIED 不合格 */
    private String grade;

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
