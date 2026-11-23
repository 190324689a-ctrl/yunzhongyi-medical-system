package com.yunzhongyi.medical.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考核模板
 * <p>
 * 覆盖需求（需求梳理.md 第 4 项）：支持自定义考核模板。
 * <p>
 * 考核类型 assessType：PERIODIC 定期考核 / ETHICS 医德考评 / QUALITY 医疗质量考核。
 * <p>
 * 评分项 scoreItems：JSON 数组，形如
 * [{"name":"医德医风","weight":30},{"name":"业务能力","weight":30}]，
 * 各维度权重之和应为 100（保存时校验）。
 * <p>
 * 状态 status：1 启用，0 停用；只有启用的模板可用于新建考核记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_template")
public class AssessmentTemplate extends BaseEntity {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /** 考核类型：PERIODIC 定期考核 / ETHICS 医德考评 / QUALITY 医疗质量考核 */
    @NotBlank(message = "考核类型不能为空")
    private String assessType;

    /** 考核周期：年度 / 半年度 / 季度 */
    private String cycle;

    /** 评分项 JSON：[{"name":"医德医风","weight":30}, ...]，权重之和为 100 */
    private String scoreItems;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
