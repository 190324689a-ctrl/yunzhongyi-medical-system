package com.yunzhongyi.medical.briefing.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简报模板：医疗工作简报的版式与栏目配置。
 * <p>
 * 覆盖需求（需求梳理.md 第 30 项）：支持模板、自动生成。
 * 简报分类 category：MONTHLY 月报 / QUARTERLY 季报 / ANNUAL 年报 /
 * SPECIAL 专项简报 / DYNAMIC 工作动态（见 BriefingTypeUtil）。
 * 栏目 sections：JSON 数组字符串，如 ["工作概况","医疗质量","重点工作","下月计划"]，
 * 自动生成时按栏目从各业务模块聚合数据写正文。
 * 启用状态 status：1 启用，0 停用；停用的模板不能用来新建/自动生成简报。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("briefing_template")
public class BriefingTemplate extends BaseEntity {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String name;

    /** 简报分类：MONTHLY / QUARTERLY / ANNUAL / SPECIAL / DYNAMIC */
    private String category;

    /** 栏目配置：JSON 数组字符串，如 ["工作概况","医疗质量","重点工作","下月计划"] */
    @TableField("sections")
    private String sections;

    /** 启用状态：1 启用，0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
