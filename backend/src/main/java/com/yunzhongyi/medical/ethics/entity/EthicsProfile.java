package com.yunzhongyi.medical.ethics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医德医风档案
 * <p>
 * 覆盖需求（需求梳理.md 第 12 项）：医德医风档案。
 * <p>
 * 一名医师建一份档案（physicianId 全院唯一），档案编号 archiveNo 全院唯一。
 * lastGrade / lastEvaluateYear 为最近一次已通过的医德考评结果（考评审批通过时
 * 同步更新，见 EthicsEvaluationServiceImpl#approve）。
 * <p>
 * 状态 status：0 在档，1 封存（医师离职/退休后封存，封存档案不可新增考评/表彰）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ethics_profile")
public class EthicsProfile extends BaseEntity {

    /** 档案编号，如 YD-2024-001（全院唯一） */
    @NotBlank(message = "档案编号不能为空")
    private String archiveNo;

    /** 医师 id（一名医师一份档案） */
    @NotNull(message = "医师不能为空")
    private Long physicianId;

    /** 医师姓名（冗余，便于列表展示与查询） */
    @NotBlank(message = "医师姓名不能为空")
    private String physicianName;

    /** 所属科室 id */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 职称（冗余） */
    private String title;

    /** 建档日期（默认当天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate establishDate;

    /** 状态：0 在档，1 封存 */
    private Integer status;

    /** 最近一次已通过考评的等级：EXCELLENT 优秀 / QUALIFIED 合格 / BASIC 基本合格 / UNQUALIFIED 不合格 */
    private String lastGrade;

    /** 最近一次已通过考评的年度 */
    private Integer lastEvaluateYear;

    /** 备注 */
    private String remark;
}
