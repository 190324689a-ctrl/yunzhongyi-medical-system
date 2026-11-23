package com.yunzhongyi.medical.authorization.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 重点授权申请
 * <p>
 * 覆盖需求（需求梳理.md 第 3 项）：手术、麻醉、抗肿瘤、精神类等重点授权的
 * 申请、审批、查询、统计；支持批量导入、导出。
 * <p>
 * 授权类型 authType：SURGERY 手术分级授权 / ANESTHESIA 麻醉药品处方权 /
 * ANTITUMOR 抗肿瘤处方权 / PSYCHOTROPIC 精神药品处方权。
 * <p>
 * 审批状态 status：0 待科主任审批 / 1 待医务科审批 / 2 已通过 / 3 已驳回。
 * 多级审批：新建为 0；科主任通过后变为 1；医务科通过后变为 2；任一级驳回变为 3。
 * <p>
 * 授权有效期：validFrom / validTo（结束日期不能早于开始日期）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("key_authorization")
public class Authorization extends BaseEntity {

    /** 申请人医师 id */
    @NotNull(message = "申请人不能为空")
    private Long physicianId;

    /** 申请人姓名（冗余，便于列表展示与查询） */
    @NotBlank(message = "申请人姓名不能为空")
    private String physicianName;

    /** 所属科室 id */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 授权类型：SURGERY / ANESTHESIA / ANTITUMOR / PSYCHOTROPIC */
    @NotBlank(message = "授权类型不能为空")
    private String authType;

    /** 手术分级（仅 SURGERY 授权用：1-4 级） */
    private Integer surgeryLevel;

    /** 授权有效期开始日期 */
    private LocalDate validFrom;

    /** 授权有效期结束日期 */
    private LocalDate validTo;

    /** 申请事由 */
    private String reason;

    /** 审批状态：0 待科主任审批，1 待医务科审批，2 已通过，3 已驳回 */
    private Integer status;

    /** 当前审批人 */
    private String currentApprover;

    /** 最新审批意见 */
    private String approvalOpinion;

    /** 审批记录（多级审批留痕，如“科主任 张主任：通过；医务科 吴科长：通过”） */
    private String approvalHistory;

    /** 申请日期 */
    private LocalDate applyDate;

    /** 备注 */
    private String remark;
}
