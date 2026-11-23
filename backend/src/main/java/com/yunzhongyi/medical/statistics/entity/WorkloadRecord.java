package com.yunzhongyi.medical.statistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作量登记（第 24 项“工作统计”的基础数据）
 * <p>
 * 覆盖需求（需求梳理.md 第 24 项）：手术量 / 工作量 / 门诊量 / 住院量统计，
 * 支持按科室、按人员、按时间统计。
 * <p>
 * 一条记录 = 一名医师一个月的填报工作量。同一医师同一统计月份只允许一条
 * 记录（新建与导入时都去重），避免重复填报导致统计虚增。
 * <p>
 * 门诊量 outpatientCount / 住院量 inpatientCount / 手术量 surgeryCount 均为
 * 当月人次（手术量为台次）；三者之和即该医师当月工作总量，由统计接口汇总，
 * 不单独存库。排班人次来自排班记录（duty_schedule），是另一口径，统计接口
 * 里单独列示，不与这里的诊疗量混算。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workload_record")
public class WorkloadRecord extends BaseEntity {

    /** 医师 id */
    private Long physicianId;

    /** 医师姓名 */
    @NotBlank(message = "医师姓名不能为空")
    private String physicianName;

    /** 科室 id */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 统计月份：yyyy-MM，如 2026-11 */
    @NotBlank(message = "统计月份不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "统计月份格式应为 yyyy-MM，如 2026-11")
    private String statMonth;

    /** 门诊量（人次） */
    @Min(value = 0, message = "门诊量不能为负数")
    private Integer outpatientCount;

    /** 住院量（人次） */
    @Min(value = 0, message = "住院量不能为负数")
    private Integer inpatientCount;

    /** 手术量（台次） */
    @Min(value = 0, message = "手术量不能为负数")
    private Integer surgeryCount;

    /** 备注 */
    private String remark;
}
