package com.yunzhongyi.medical.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质量检查模板
 * <p>
 * 覆盖需求（需求梳理.md 第 13/14/15 项）：检查模板、检查计划、检查记录、
 * 检查报告、整改跟踪；检查类型覆盖科室自查、院级检查、专项检查三种。
 * <p>
 * 检查类型 inspectType：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查
 * （见 QualityTypeUtil）。
 * <p>
 * 检查项 checkItems：JSON 数组，形如
 * [{"name":"病历书写规范","weight":30,"standard":"运行病历书写及时、完整"},
 * {"name":"核心制度执行","weight":30,"standard":"三级查房、会诊等制度落实"}]，
 * 各检查项权重之和应为 100（保存时校验）。
 * <p>
 * 状态 status：1 启用，0 停用；只有启用的模板可用于新建检查计划。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_template")
public class QualityTemplate extends BaseEntity {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /** 检查类型：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查 */
    @NotBlank(message = "检查类型不能为空")
    private String inspectType;

    /** 检查项 JSON：[{"name":"病历书写规范","weight":30,"standard":"..."}, ...]，权重之和为 100 */
    private String checkItems;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
