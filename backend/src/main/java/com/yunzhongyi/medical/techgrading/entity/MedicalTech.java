package com.yunzhongyi.medical.techgrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医疗技术档案
 * <p>
 * 覆盖需求（需求梳理.md 第 18 项）：医疗技术分级、准入、授权、档案管理。
 * 本表管技术档案库：技术分级、准入类别、开展科室、风险等级、伦理要求、
 * 准入状态与有效期；准入申请见 TechAdmission，医师授权见 TechAuthorization。
 * <p>
 * 技术分级 techLevel（按《医疗技术临床应用管理办法》）：1 一级（安全性、
 * 有效性确切，技术难度低、风险低，常规技术）/ 2 二级（安全性、有效性
 * 有一定保障，技术难度较大、有一定风险）/ 3 三级（安全性、有效性不确切，
 * 技术难度大、风险大）。
 * <p>
 * 准入类别 accessType：NORMAL 常规技术 / RESTRICTED 限制类技术 /
 * PROHIBITED 禁止类技术（国家限制类技术目录；禁止类技术不得开展、不得授权）。
 * <p>
 * 技术分类 category：SURGICAL 手术类 / NON_SURGICAL 非手术类 / TCM 中医技术 /
 * DIAGNOSTIC 诊疗技术 / OTHER 其他（见 TechGradingTypeUtil）。
 * <p>
 * 风险等级 riskLevel：LOW 低 / MEDIUM 中 / HIGH 高。
 * <p>
 * 档案状态 status：0 待准入，1 已准入，2 已停用。新技术先建档案（待准入），
 * 走准入申请审批通过后变为已准入；已准入的档案可停用，停用后可重新启用。
 * <p>
 * 失效不单独存状态：已准入且有效期已过当天即“已过期”，按有效期动态判断；
 * 90 天内到期即“即将到期”。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_tech")
public class MedicalTech extends BaseEntity {

    /** 技术编号，如“JS-2024-001”（全院唯一，不可重复） */
    @NotBlank(message = "技术编号不能为空")
    private String techNo;

    /** 技术名称，如“小针刀松解术” */
    @NotBlank(message = "技术名称不能为空")
    private String techName;

    /** 技术分类：SURGICAL 手术类 / NON_SURGICAL 非手术类 / TCM 中医技术 / DIAGNOSTIC 诊疗技术 / OTHER 其他 */
    @NotBlank(message = "技术分类不能为空")
    private String category;

    /** 技术分级：1 一级 / 2 二级 / 3 三级 */
    private Integer techLevel;

    /** 准入类别：NORMAL 常规技术 / RESTRICTED 限制类技术 / PROHIBITED 禁止类技术 */
    @NotBlank(message = "准入类别不能为空")
    private String accessType;

    /** 开展科室，如“骨伤科、针灸推拿科”（多个科室用顿号分隔） */
    private String deptName;

    /** 风险等级：LOW 低 / MEDIUM 中 / HIGH 高 */
    private String riskLevel;

    /** 是否需要伦理审查：0 否，1 是 */
    private Integer ethicsRequired;

    /** 档案状态：0 待准入，1 已准入，2 已停用 */
    private Integer status;

    /** 准入日期（准入审批通过时自动填当天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate admissionDate;

    /** 有效期（准入有效期；已准入且有效期已过当天即已过期） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    /** 技术简介 / 适应症 / 禁忌症等说明 */
    private String description;

    /** 准入审批人（准入审批通过时填） */
    private String approver;

    /** 审批记录（准入/停用/重新启用留痕） */
    private String approvalHistory;

    /** 停用原因（停用时必填） */
    private String suspendReason;

    /** 备注 */
    private String remark;
}
