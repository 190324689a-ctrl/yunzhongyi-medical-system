package com.yunzhongyi.medical.rectification.dto;

import lombok.Data;

/**
 * 生成检查整改汇总报告请求：按统计周期 + 汇总范围，
 * 从质量检查的问题与整改任务实时聚合生成一条汇总记录。
 * 同一（统计周期、汇总范围、科室、检查类型）已有记录时重新生成（原地更新）。
 */
@Data
public class GenerateRequest {

    /** 统计周期，如 2026-11（yyyy-MM），必填 */
    private String statPeriod;

    /** 汇总范围：ALL 全院 / DEPT 按科室 / TYPE 按检查类型，必填 */
    private String scopeType;

    /** 科室名称（按科室汇总时必填） */
    private String deptName;

    /** 检查类型：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查（按检查类型汇总时必填） */
    private String inspectType;

    /** 生成人 */
    private String generatedBy;

    /** 备注 */
    private String remark;
}
