package com.yunzhongyi.medical.statistics.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 三级医院医疗质量指标（第 25 项“三级医院医疗质量指标统计”的基础数据）
 * <p>
 * 覆盖需求（需求梳理.md 第 25 项）：三级医院评审指标、医疗质量指标统计，
 * 支持按国家 / 省 / 市标准统计。
 * <p>
 * 一条记录 = 一个指标在一个统计周期内的填报值。指标编码 indicatorCode 全院
 * 唯一。达标与否由目标值 targetValue、实际值 actualValue 与比较方向
 * direction 现算，不单独存库：方向为越高越好时实际值 ≥ 目标值即达标，
 * 越低越好时实际值 ≤ 目标值即达标；实际值未填报时为“未填报”，不计入达标率。
 * <p>
 * 实际值必须有据可查：dataSource 注明数据来源（如“医师考核记录（2026 年度，
 * 共 6 条）”“工作量登记（2026-11）”），种子指标的实际值均按当前库内已有
 * 业务数据的聚合结果填写，见 data.sql 注释；不是凭空编造的业务数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_indicator")
public class QualityIndicator extends BaseEntity {

    /** 指标编码，全院唯一，如 QI-001 */
    @NotBlank(message = "指标编码不能为空")
    private String indicatorCode;

    /** 指标名称，如 医师考核优秀率 */
    @NotBlank(message = "指标名称不能为空")
    private String indicatorName;

    /** 指标分类：医疗质量 / 医疗安全 / 患者安全 / 工作效率 */
    @NotBlank(message = "指标分类不能为空")
    private String category;

    /** 标准级别：NATIONAL 国家 / PROVINCE 省 / CITY 市 */
    @NotBlank(message = "标准级别不能为空")
    private String standardLevel;

    /** 标准名称，如 三级中医医院评审标准 */
    private String standardName;

    /** 目标值 */
    private BigDecimal targetValue;

    /** 实际值（未填报时为空） */
    private BigDecimal actualValue;

    /** 单位：%、分、人次、台 等 */
    private String unit;

    /** 比较方向：HIGHER_BETTER 越高越好 / LOWER_BETTER 越低越好，默认越高越好 */
    private String direction;

    /** 统计周期，如 2026-11 / 2026 年 */
    private String statPeriod;

    /** 适用科室名称（为空表示全院级指标） */
    private String deptName;

    /** 数据来源说明：注明实际值取自哪个模块的哪些数据 */
    private String dataSource;

    /** 备注 */
    private String remark;

    /** 是否达标（接口现算，不存库） */
    @TableField(exist = false)
    private Boolean compliant;

    /** 达标情况中文：达标 / 未达标 / 未填报（接口现算，不存库） */
    @TableField(exist = false)
    private String complianceText;
}
