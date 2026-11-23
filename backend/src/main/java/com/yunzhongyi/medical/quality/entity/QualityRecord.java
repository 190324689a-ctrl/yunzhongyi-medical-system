package com.yunzhongyi.medical.quality.entity;

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
 * 质量检查记录：一次检查的逐项打分结果。
 * <p>
 * 覆盖需求（需求梳理.md 第 13/14/15 项）：检查记录（按计划逐项打分）。
 * <p>
 * 只有进行中的检查计划才能添加检查记录；一个计划可多次检查、多次记录。
 * <p>
 * 各项得分 itemScores：JSON 对象，形如 {"病历书写规范":27,"核心制度执行":26}，
 * 每项得分不能超过该项在模板中的权重；总分 totalScore 由各项得分求和自动算，
 * 等级 grade 按总分自动算（≥90 优秀 / ≥80 合格 / ≥60 基本合格 / &lt;60 不合格，
 * 见 QualityTypeUtil）。
 * <p>
 * 发现的问题见 {@link QualityIssue}，一次检查记录可发现多个问题。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_record")
public class QualityRecord extends BaseEntity {

    /** 所属检查计划 id（服务端从路径参数补值，不由客户端传） */
    private Long planId;

    /** 检查模板 id（冗余，取自计划） */
    private Long templateId;

    /** 检查模板名称（冗余） */
    private String templateName;

    /** 检查类型：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查（冗余，取自计划） */
    private String inspectType;

    /** 被查科室 id（冗余，取自计划） */
    private Long deptId;

    /** 被查科室名称（冗余，取自计划） */
    private String deptName;

    /** 各项得分 JSON：{"病历书写规范":27, ...}，每项不超过该项权重 */
    @NotBlank(message = "各项得分不能为空")
    private String itemScores;

    /** 总分（满分 100，由各项得分求和自动算） */
    private BigDecimal totalScore;

    /** 等级：EXCELLENT 优秀 / QUALIFIED 合格 / BASIC 基本合格 / UNQUALIFIED 不合格（自动算） */
    private String grade;

    /** 检查结果（结论文字，如“合格，需对 2 个问题整改”） */
    private String result;

    /** 检查人 / 检查组 */
    @NotBlank(message = "检查人不能为空")
    private String inspector;

    /** 检查日期 */
    @NotNull(message = "检查日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkDate;

    /** 备注 */
    private String remark;
}
