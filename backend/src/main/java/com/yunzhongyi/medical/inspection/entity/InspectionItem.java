package com.yunzhongyi.medical.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 检查项目
 * <p>
 * 覆盖需求（需求梳理.md 第 19 项“检查维护：检查项目、检查标准、检查模板的维护”）
 * 的检查项目库：检查项目是质量检查模板的最小组成单元，模板通过
 * InspectionTemplateItem 从项目库里组装检查项（名称、权重、评分标准），
 * 不再各模板手写检查项，避免同名不同标准。
 * <p>
 * 项目分类 category：MEDICAL_RECORD 病历质量 / CORE_SYSTEM 核心制度 /
 * INFECTION 医院感染 / PHARMACY 药事管理 / NURSING 护理质量 /
 * SAFETY 医疗安全 / SERVICE 服务质量 / TCM 中医特色 /
 * MGMT 科室管理 / OTHER 其他（见 InspectionTypeUtil）。
 * <p>
 * 检查方式 checkMethod：ON_SITE 现场查看 / RECORD_REVIEW 抽查病历 /
 * INTERVIEW 访谈 / DOC_REVIEW 资料查阅。
 * <p>
 * 适用检查类型 applicableType：ALL 通用 / SELF 科室自查 /
 * HOSPITAL 院级检查 / SPECIAL 专项检查（与 QualityTypeUtil 的检查类型一致）。
 * <p>
 * 依据标准 standardId：关联 InspectionStandard（检查标准库），可为空；
 * standardText 为该项目的评分标准文字（组装进模板时写入模板 JSON 的 standard 字段）。
 * <p>
 * 状态 status：1 启用，0 停用；只有启用的项目可组装进模板；
 * 已被模板引用的项目不可停用、不可删除（先从模板里移除）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_item")
public class InspectionItem extends BaseEntity {

    /** 项目编号，如“JCXM-001”（全院唯一，不可重复） */
    @NotBlank(message = "项目编号不能为空")
    private String itemNo;

    /** 项目名称，如“病历书写规范” */
    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    /** 项目分类：MEDICAL_RECORD 病历质量 / CORE_SYSTEM 核心制度 / INFECTION 医院感染 / PHARMACY 药事管理 / NURSING 护理质量 / SAFETY 医疗安全 / SERVICE 服务质量 / TCM 中医特色 / MGMT 科室管理 / OTHER 其他 */
    @NotBlank(message = "项目分类不能为空")
    private String category;

    /** 检查方式：ON_SITE 现场查看 / RECORD_REVIEW 抽查病历 / INTERVIEW 访谈 / DOC_REVIEW 资料查阅 */
    private String checkMethod;

    /** 依据标准 id（关联 inspection_standard，可为空） */
    private Long standardId;

    /** 依据标准编号（冗余，导入导出与展示用，随标准自动补全） */
    private String standardNo;

    /** 评分标准文字，如“运行病历书写及时、完整、规范” */
    private String standardText;

    /** 满分（该项目满分分值，须大于 0，默认 100） */
    private Integer maxScore;

    /** 默认权重（组装进模板时的建议权重，须大于 0，默认 10） */
    private BigDecimal weight;

    /** 适用检查类型：ALL 通用 / SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查 */
    private String applicableType;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 排序号（同类项目按此排序） */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}
