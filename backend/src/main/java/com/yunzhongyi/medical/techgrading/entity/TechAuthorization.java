package com.yunzhongyi.medical.techgrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医师医疗技术授权
 * <p>
 * 覆盖需求（需求梳理.md 第 18 项）：医疗技术授权。一级审批（医务科），
 * 审批记录留痕；审批通过后医师方可开展该项医疗技术。
 * <p>
 * 授权规则：
 * <ul>
 *   <li>技术须已准入（档案状态为已准入），待准入/已停用的技术不能授权；</li>
 *   <li>禁止类技术不得授权；</li>
 *   <li>授权级别 authLevel 不能高于技术分级 techLevel（如三级技术可授权一/二/三级，
 *   一级技术只能授权一级）；</li>
 *   <li>同一医师同一技术已有待审批或已通过的授权时，拒绝重复申请。</li>
 * </ul>
 * <p>
 * 授权状态 status：0 待审批，1 已通过，2 已驳回，3 已注销。只有待审批的
 * 授权能修改、能审批；已通过的授权可注销（注销原因必填）；已通过的授权
 * 不能直接删除，请先注销。
 * <p>
 * 失效不单独存状态：已通过且授权有效期已过当天即“已过期”，按有效期动态判断；
 * 90 天内到期即“即将到期”。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tech_authorization")
public class TechAuthorization extends BaseEntity {

    /** 医师档案 id */
    private Long physicianId;

    /** 医师姓名 */
    @NotBlank(message = "医师姓名不能为空")
    private String physicianName;

    /** 科室 id */
    private Long deptId;

    /** 科室名称（按医师档案补全） */
    private String deptName;

    /** 医疗技术档案 id */
    private Long techId;

    /** 技术编号（按技术档案补全） */
    private String techNo;

    /** 技术名称（按技术档案补全） */
    @NotBlank(message = "技术名称不能为空")
    private String techName;

    /** 技术分级（按技术档案补全，快照：1/2/3） */
    private Integer techLevel;

    /** 授权级别：1 一级 / 2 二级 / 3 三级（不能高于技术分级） */
    private Integer authLevel;

    /** 授权开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** 授权结束日期（有效期；已通过且已过当天即已过期） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** 授权状态：0 待审批，1 已通过，2 已驳回，3 已注销 */
    private Integer status;

    /** 审批人（审批时必填） */
    private String approver;

    /** 审批记录（通过/驳回/注销留痕） */
    private String approvalHistory;

    /** 注销原因（注销时必填） */
    private String revokeReason;

    /** 备注 */
    private String remark;
}
