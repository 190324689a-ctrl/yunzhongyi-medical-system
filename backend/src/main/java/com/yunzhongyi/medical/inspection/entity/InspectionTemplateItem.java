package com.yunzhongyi.medical.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 模板检查项配置
 * <p>
 * 覆盖需求（需求梳理.md 第 19 项“检查维护：检查项目、检查标准、检查模板的维护”）
 * 的模板维护：把检查项目库里的项目组装进质量检查模板
 * （quality_template），替代模板里手写的 checkItems JSON。
 * <p>
 * 一条记录 = 一个模板里的一个检查项：templateId + itemId 唯一，
 * weight 为该项在模板里的权重，各检查项权重之和必须为 100
 * （与 QualityTemplateServiceImpl 的模板权重校验一致）。
 * <p>
 * 配置保存时同步回写 QualityTemplate.checkItems JSON
 * （name 取项目名称、weight 取本表权重、standard 取项目评分标准文字），
 * 模板与项目库保持一致；模板本身的增删改与启用/停用仍在质量检查模块。
 * <p>
 * 删除守卫：一个模板至少保留一项，不能删除最后一项。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_template_item")
public class InspectionTemplateItem extends BaseEntity {

    /** 检查模板 id（关联 quality_template） */
    private Long templateId;

    /** 模板名称（冗余，展示与导入导出用） */
    private String templateName;

    /** 检查项目 id（关联 inspection_item） */
    private Long itemId;

    /** 项目编号（冗余，展示与导入导出用） */
    private String itemNo;

    /** 项目名称（冗余，展示与导入导出用） */
    private String itemName;

    /** 权重（该项在模板里的权重，须大于 0；同一模板各项权重之和为 100） */
    private BigDecimal weight;

    /** 排序号（模板内检查项顺序） */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}
