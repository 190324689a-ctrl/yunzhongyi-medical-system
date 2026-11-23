package com.yunzhongyi.medical.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检查分类
 * <p>
 * 覆盖需求（需求梳理.md 第 20 项“检查分类：检查分类管理”）：
 * 检查项目的分类不再是写死在代码里的 10 个枚举值，改为可维护的分类库；
 * 检查项目按分类归类（InspectionItem.category 存分类编码），
 * 模板通过 InspectionTemplateItem 组装项目、间接引用分类。
 * <p>
 * 两级分类：parentId 为空是一级分类，有上级是二级分类，不建三级。
 * 种子 10 个一级分类的编码与模块 19 既有口径一致
 * （MEDICAL_RECORD 病历质量 / CORE_SYSTEM 核心制度 / INFECTION 医院感染 /
 * PHARMACY 药事管理 / NURSING 护理质量 / SAFETY 医疗安全 /
 * SERVICE 服务质量 / TCM 中医特色 / MGMT 科室管理 / OTHER 其他），
 * 既有检查项目无需改动即可对应上。
 * <p>
 * 适用检查类型 applicableType：ALL 通用 / SELF 科室自查 /
 * HOSPITAL 院级检查 / SPECIAL 专项检查（与 InspectionTypeUtil 一致）。
 * <p>
 * 状态 status：1 启用，0 停用；只有启用的分类可被检查项目新归属；
 * 已有检查项目归属的分类不可删除、不可停用、不可改编码；
 * 分类下有检查项目已被模板引用时，同样不可删除、不可停用、不可改编码
 * （先从模板里移除项目或把项目改归其他分类）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_category")
public class InspectionCategory extends BaseEntity {

    /** 分类编码，如“MEDICAL_RECORD”（全院唯一，不可重复） */
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    /** 分类名称，如“病历质量”（同一上级下不可重复） */
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    /** 上级分类 id（空为一级分类；有上级为二级分类） */
    private Long parentId;

    /** 层级：1 一级，2 二级（由 parentId 决定，后端自动算，不接受前端传入） */
    private Integer level;

    /** 适用检查类型：ALL 通用 / SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查 */
    private String applicableType;

    /** 分类说明 */
    private String description;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 排序号（同级分类按此排序） */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    /** 上级分类编码（非库字段，导入导出与展示用） */
    @TableField(exist = false)
    private String parentCode;

    /** 上级分类名称（非库字段，展示用） */
    @TableField(exist = false)
    private String parentName;
}
