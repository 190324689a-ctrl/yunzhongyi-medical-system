package com.yunzhongyi.medical.ethics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医德考评记录
 * <p>
 * 覆盖需求（需求梳理.md 第 12 项）：医德考评。
 * <p>
 * 各项得分 itemScores：JSON 对象，键为模板评分项名称，值为该项得分，
 * 形如 {"医德医风":47,"服务态度":24}；总分 totalScore 由各项得分求和自动计算。
 * <p>
 * 等级 grade 由总分自动计算：EXCELLENT 优秀（≥90）/ QUALIFIED 合格（≥80）/
 * BASIC 基本合格（≥60）/ UNQUALIFIED 不合格（&lt;60）。
 * <p>
 * 审批 status：0 待审批，1 已通过，2 已驳回；一级审批（医务科）。
 * 审批通过时同步医德档案的最近考评结果（lastGrade / lastEvaluateYear），
 * 见 EthicsEvaluationServiceImpl#approve。已结束的考评不能再审批、再修改。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ethics_evaluation")
public class EthicsEvaluation extends BaseEntity {

    /** 被考评医师 id */
    @NotNull(message = "被考评医师不能为空")
    private Long physicianId;

    /** 被考评医师姓名（冗余，便于列表展示与查询） */
    @NotBlank(message = "被考评医师姓名不能为空")
    private String physicianName;

    /** 所属科室 id */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 考评模板 id */
    private Long templateId;

    /** 考评模板名称（冗余） */
    private String templateName;

    /** 考评年度 */
    private Integer assessYear;

    /** 考评周期：如“年度”“2026年第三季度” */
    private String cycle;

    /** 各项得分 JSON：{"医德医风":47,"服务态度":24} */
    private String itemScores;

    /** 总分（满分 100，由各项得分求和自动计算） */
    private BigDecimal totalScore;

    /** 等级：EXCELLENT 优秀 / QUALIFIED 合格 / BASIC 基本合格 / UNQUALIFIED 不合格 */
    private String grade;

    /** 考评结果：与等级一致，如“优秀”“合格”；不合格时为“不合格，需整改” */
    private String result;

    /** 考评人 */
    private String assessor;

    /** 考评日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate assessDate;

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 审批人 */
    private String approver;

    /** 审批记录（通过/驳回留痕） */
    private String approvalHistory;

    /** 备注 */
    private String remark;
}
