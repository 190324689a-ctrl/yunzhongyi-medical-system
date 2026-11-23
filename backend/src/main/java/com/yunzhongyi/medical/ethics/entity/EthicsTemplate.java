package com.yunzhongyi.medical.ethics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 医德考评模板
 * <p>
 * 覆盖需求（需求梳理.md 第 12 项）：医德考评支持自定义模板。
 * <p>
 * 评分项 scoreItems：JSON 数组，形如
 * [{"name":"医德医风","weight":50},{"name":"服务态度","weight":25}]，
 * 各维度权重之和应为 100（保存时校验，见 EthicsTemplateServiceImpl）。
 * <p>
 * 状态 status：1 启用，0 停用；只有启用的模板可用于新建医德考评记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ethics_template")
public class EthicsTemplate extends BaseEntity {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /** 评分项 JSON：[{"name":"医德医风","weight":50}, ...]，权重之和为 100 */
    private String scoreItems;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
