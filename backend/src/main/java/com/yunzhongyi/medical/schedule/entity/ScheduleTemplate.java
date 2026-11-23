package com.yunzhongyi.medical.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 排班模板
 * <p>
 * 覆盖需求（需求梳理.md 第 5 项）：支持排班模板、按模板批量排班。
 * <p>
 * 排班类型 scheduleType：DEPT 科室排班 / OUTPATIENT 门诊排班 /
 * INPATIENT 住院排班 / EMERGENCY 急诊排班；一个模板对应一种排班类型。
 * <p>
 * 班次组合 shiftPattern：JSON 数组，按星期几排班，形如
 * [{"dayOfWeek":1,"shiftCode":"DAY"},{"dayOfWeek":6,"shiftCode":"MORNING"},
 *  {"dayOfWeek":7,"shiftCode":"REST"}]，dayOfWeek 1=周一 … 7=周日；
 * 未列出的星期几不排班，REST 表示休息（批量生成时跳过，不建记录）。
 * <p>
 * 状态 status：1 启用，0 停用；只有启用的模板可用于批量生成排班。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_template")
public class ScheduleTemplate extends BaseEntity {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /** 适用科室 id（为空表示全院通用） */
    private Long deptId;

    /** 适用科室名称（冗余） */
    private String deptName;

    /** 排班类型：DEPT 科室 / OUTPATIENT 门诊 / INPATIENT 住院 / EMERGENCY 急诊 */
    @NotBlank(message = "排班类型不能为空")
    private String scheduleType;

    /** 班次组合 JSON：[{"dayOfWeek":1,"shiftCode":"DAY"}, ...] */
    private String shiftPattern;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
