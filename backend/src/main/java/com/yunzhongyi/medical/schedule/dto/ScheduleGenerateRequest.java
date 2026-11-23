package com.yunzhongyi.medical.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 按模板批量生成排班请求
 * <p>
 * 人员来源二选一：physicianIds 指定人员，或 deptId 指定科室（取该科室全部在职医师）；
 * 两者都给时以 physicianIds 为准。日期范围含首尾。
 */
@Data
public class ScheduleGenerateRequest {

    /** 排班模板 id */
    @NotNull(message = "排班模板不能为空")
    private Long templateId;

    /** 开始日期 */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /** 结束日期 */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /** 指定医师 id 列表 */
    private List<Long> physicianIds;

    /** 指定科室 id（未指定人员时用） */
    private Long deptId;
}
