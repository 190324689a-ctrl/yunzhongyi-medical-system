package com.yunzhongyi.medical.templatelib.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检查模板版本记录：模板检查项内容的一次快照。
 * <p>
 * 覆盖需求（需求梳理.md 第 21 项）：检查模板管理——版本管理。
 * <p>
 * 一个模板可有多个版本；每次新建版本 / 从历史版本恢复时追加一条记录，
 * 旧的当前版本改为历史版本，历史记录只追加不改写。
 * <p>
 * 版本号 versionNo：V1.0 / V2.0 / V3.0……按该模板已有版本数顺延。
 * <p>
 * 状态 status：1 当前版本，0 历史版本；一个模板同一时间只有一个当前版本。
 * <p>
 * checkItems 与模板表同一口径：JSON 数组
 * [{"name":"病历书写规范","weight":30,"standard":"..."}]，权重之和为 100。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("template_version")
public class TemplateVersion extends BaseEntity {

    /** 所属检查模板 id */
    @NotNull(message = "所属模板不能为空")
    private Long templateId;

    /** 所属检查模板名称（冗余） */
    private String templateName;

    /** 版本号：V1.0 / V2.0 / V3.0…… */
    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    /** 本版本检查项 JSON：[{"name":"...","weight":30,"standard":"..."}]，权重之和为 100 */
    private String checkItems;

    /** 版本说明：新建 / 恢复时填写，如“增加合理用药权重至 30” */
    private String changeRemark;

    /** 状态：1 当前版本，0 历史版本 */
    private Integer status;
}
