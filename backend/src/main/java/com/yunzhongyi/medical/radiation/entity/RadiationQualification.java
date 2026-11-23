package com.yunzhongyi.medical.radiation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 放射人员资质证书
 * <p>
 * 覆盖需求（需求梳理.md 第 8 项）：放射人员资质管理。
 * <p>
 * 证书类型 certType：WORKER_CERT 放射工作人员证 / EQUIPMENT 大型设备上岗证 /
 * PROTECTION 放射防护培训合格证 / PRACTICE 执业医师证（见 RadiationTypeUtil）。
 * <p>
 * 审核状态 status：0 待审核，1 已通过，2 已驳回。
 * 全流程：登记资质（待审核）→ 医务科一级审核通过 → 已通过，审核记录留痕；
 * 驳回即已驳回。已结束的资质（已通过/已驳回）不能再审核、再修改。
 * 证书有效期 expiryDate 用于统计“90 天内到期”提醒与“已过期”提醒。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("radiation_qualification")
public class RadiationQualification extends BaseEntity {

    /** 放射人员 id（档案 id） */
    private Long personnelId;

    /** 人员姓名（冗余） */
    @NotBlank(message = "人员姓名不能为空")
    private String personnelName;

    /** 证书类型：WORKER_CERT / EQUIPMENT / PROTECTION / PRACTICE */
    @NotBlank(message = "证书类型不能为空")
    private String certType;

    /** 证书名称 */
    @NotBlank(message = "证书名称不能为空")
    private String certName;

    /** 证书编号 */
    private String certNo;

    /** 发证日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    /** 有效期至 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    /** 审核状态：0 待审核，1 已通过，2 已驳回 */
    private Integer status;

    /** 审核人（医务科） */
    private String approver;

    /** 最新审核意见 */
    private String approvalOpinion;

    /** 审核记录（留痕，如“医务科 吴文斌：通过（同意）”） */
    private String approvalHistory;

    /** 备注 */
    private String remark;
}
