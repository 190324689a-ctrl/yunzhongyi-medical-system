package com.yunzhongyi.medical.techgrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医疗技术准入申请
 * <p>
 * 覆盖需求（需求梳理.md 第 18 项）：医疗技术准入。一级审批（医务科），
 * 审批记录留痕；审批通过时同步医疗技术档案（见 TechAdmissionService#approve）。
 * <p>
 * 申请类型 applyType：NEW 新技术准入 / ADD_DEPT 新增开展科室 /
 * CHANGE_LEVEL 变更分级 / RENEW 延续准入。
 * <p>
 * 审批通过时的同步规则：
 * <ul>
 *   <li>NEW：档案变为已准入，准入日期填当天（有效期为空时默认准入日期 + 3 年）；</li>
 *   <li>ADD_DEPT：newDept 追加进档案的开展科室（已有的科室不重复追加）；</li>
 *   <li>CHANGE_LEVEL：档案的技术分级改为 newLevel；</li>
 *   <li>RENEW：档案的有效期改为 newExpiryDate。</li>
 * </ul>
 * <p>
 * 申请状态 status：0 待审批，1 已通过，2 已驳回。只有待审批的申请能修改、
 * 能审批；已结束的申请不能再审批、再修改。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tech_admission")
public class TechAdmission extends BaseEntity {

    /** 医疗技术档案 id（准入申请必须关联一条技术档案） */
    private Long techId;

    /** 技术编号（按技术档案补全） */
    private String techNo;

    /** 技术名称（按技术档案补全） */
    @NotBlank(message = "技术名称不能为空")
    private String techName;

    /** 申请类型：NEW 新技术准入 / ADD_DEPT 新增开展科室 / CHANGE_LEVEL 变更分级 / RENEW 延续准入 */
    @NotBlank(message = "申请类型不能为空")
    private String applyType;

    /** 申请科室 */
    private String applyDept;

    /** 申请人 */
    @NotBlank(message = "申请人不能为空")
    private String applicant;

    /** 申请日期（默认当天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyDate;

    /** 申请理由 / 说明 */
    private String reason;

    /** 变更分级：变更后的技术分级（CHANGE_LEVEL 时必填，1/2/3） */
    private Integer newLevel;

    /** 新增开展科室（ADD_DEPT 时必填） */
    private String newDept;

    /** 延续准入：新的有效期（RENEW 时必填） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate newExpiryDate;

    /** 申请状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 审批人（审批时必填） */
    private String approver;

    /** 审批记录（通过/驳回留痕，如“医务科 吴文斌：通过（同意准入）”） */
    private String approvalHistory;

    /** 备注 */
    private String remark;
}
