package com.yunzhongyi.medical.inspection.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模板检查项配置请求：把一批检查项目组装进指定模板。
 * <p>
 * 校验：模板须存在；至少一项；项目须存在且已启用；同一模板内项目不重复；
 * 每项权重须大于 0；各项权重之和必须为 100。
 */
@Data
public class TemplateItemConfigRequest {

    /** 检查模板 id（关联 quality_template） */
    @NotNull(message = "检查模板不能为空")
    private Long templateId;

    /** 待组装的检查项列表（按 sortOrder 排序） */
    private List<TemplateItem> items;

    /** 单个检查项配置 */
    @Data
    public static class TemplateItem {

        /** 检查项目 id（关联 inspection_item） */
        @NotNull(message = "检查项目不能为空")
        private Long itemId;

        /** 权重（该项在模板里的权重，须大于 0） */
        private BigDecimal weight;

        /** 排序号（模板内检查项顺序，默认按列表顺序） */
        private Integer sortOrder;
    }
}
