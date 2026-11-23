package com.yunzhongyi.medical.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医师考核记录
 * <p>
 * 覆盖需求（需求梳理.md 第 4 项）：医师定期考核、医师考核、医德考评、医疗质量考核；
 * 支持评分统计、结果查询、导出。
 * <p>
 * 各项得分 itemScores：JSON 对象，键为模板评分项名称，值为该项得分，
 * 形如 {"医德医风":28,"业务能力":28}；总分 totalScore 由各项得分求和自动计算。
 * <p>
 * 等级 grade 由总分自动计算：EXCELLENT 优秀（≥90）/ QUALIFIED 合格（≥80）/
 * BASIC 基本合格（≥60）/ UNQUALIFIED 不合格（&lt;60）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("physician_assessment")
public class Assessment extends BaseEntity {

    /** 被考核医师 id */
    @NotNull(message = "被考核医师不能为空")
    private Long physicianId;

    /** 被考核医师姓名（冗余，便于列表展示与查询） */
    @NotBlank(message = "被考核医师姓名不能为空")
    private String physicianName;

    /** 所属科室 id */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 考核模板 id */
    private Long templateId;

    /** 考核模板名称（冗余） */
    private String templateName;

    /** 考核类型：PERIODIC 定期考核 / ETHICS 医德考评 / QUALITY 医疗质量考核 */
    @NotBlank(message = "考核类型不能为空")
    private String assessType;

    /** 考核年度 */
    private Integer assessYear;

    /** 考核周期：如“年度”“2026年第三季度” */
    private String cycle;

    /** 各项得分 JSON：{"医德医风":28,"业务能力":28} */
    private String itemScores;

    /** 总分（满分 100，由各项得分求和自动计算） */
    private BigDecimal totalScore;

    /** 等级：EXCELLENT 优秀 / QUALIFIED 合格 / BASIC 基本合格 / UNQUALIFIED 不合格 */
    private String grade;

    /** 考核结果：与等级一致，如“优秀”“合格”；不合格时为“不合格，需整改” */
    private String result;

    /** 考核人 */
    private String assessor;

    /** 考核日期 */
    private LocalDate assessDate;

    /** 备注 */
    private String remark;
}
