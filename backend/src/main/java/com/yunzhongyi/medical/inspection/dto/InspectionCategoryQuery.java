package com.yunzhongyi.medical.inspection.dto;

import lombok.Data;

/**
 * 检查分类多条件组合查询参数
 */
@Data
public class InspectionCategoryQuery {

    /** 关键字：分类名称 / 分类编码模糊匹配 */
    private String keyword;

    /** 上级分类 id（查某一级分类的全部下级） */
    private Long parentId;

    /** 层级：1 一级，2 二级 */
    private Integer level;

    /** 适用检查类型：ALL 通用 / SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查 */
    private String applicableType;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 只看已有检查项目归属的分类 */
    private Boolean withItems;

    /** 只看尚无检查项目归属的分类 */
    private Boolean withoutItems;

    private long page = 1;
    private long size = 10;
}
