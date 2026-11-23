package com.yunzhongyi.medical.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 检查标准
 * <p>
 * 覆盖需求（需求梳理.md 第 19 项“检查维护：检查项目、检查标准、检查模板的维护”）
 * 的检查标准库：国家/省/市/院内各级检查标准的统一维护，检查项目通过
 * standardId 关联本表，标准废止后仍可追溯（项目不断链，删除时守卫）。
 * <p>
 * 标准级别 level：NATIONAL 国家 / PROVINCE 省 / CITY 市 / HOSPITAL 院内
 * （见 InspectionTypeUtil）。
 * <p>
 * 状态 status：1 现行，0 已废止；只有现行的标准可被检查项目新关联；
 * 已被检查项目引用的标准不可删除（先解除项目关联）。
 * <p>
 * 失效不单独存状态：现行且有效期已过当天即“已过期”，按有效期动态判断；
 * 30 天内到期即“即将到期”。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_standard")
public class InspectionStandard extends BaseEntity {

    /** 标准编号，如“JCBZ-001”（全院唯一，不可重复） */
    @NotBlank(message = "标准编号不能为空")
    private String standardNo;

    /** 标准名称，如“三级医院评审标准（2020年版）” */
    @NotBlank(message = "标准名称不能为空")
    private String standardName;

    /** 标准级别：NATIONAL 国家 / PROVINCE 省 / CITY 市 / HOSPITAL 院内 */
    @NotBlank(message = "标准级别不能为空")
    private String level;

    /** 依据文件，如“《三级医院评审标准（2020年版）》” */
    private String sourceDoc;

    /** 标准内容（要点摘要） */
    private String content;

    /** 适用范围，如“全院”“中医内科”等 */
    private String applicableScope;

    /** 生效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    /** 有效期（现行且有效期已过当天即已过期） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    /** 状态：1 现行，0 已废止 */
    private Integer status;

    /** 废止原因（废止时必填） */
    private String abolishReason;

    /** 备注 */
    private String remark;
}
