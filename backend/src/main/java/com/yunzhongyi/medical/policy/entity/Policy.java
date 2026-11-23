package com.yunzhongyi.medical.policy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 政策法规
 * <p>
 * 覆盖需求（需求梳理.md 第 11 项）：政策、法规、文件管理；
 * 支持分类、发布、查询、更新。
 * <p>
 * 法规分类 category：LAW 法律 / REGULATION 行政法规 / RULE 部门规章 /
 * LOCAL 地方性法规 / NORMATIVE 规范性文件 / STANDARD 行业标准 /
 * OTHER 其他（见 PolicyTypeUtil）。
 * <p>
 * 发布层级 level：NATIONAL 国家 / PROVINCIAL 省 / MUNICIPAL 市 /
 * HOSPITAL 院内。
 * <p>
 * 状态 status：0 草稿，1 已发布，2 已废止。
 * 生命周期与文件制度管理（模块 10）一致：草稿 --发布（一级审批：
 * 审批人必填，审批记录留痕）--> 已发布 --废止（废止原因必填）-->
 * 已废止；已发布的法规不可直接修改，更新走“新建版本”：生成一条
 * 草稿记录（文号相同、版本号 +1），新版本发布时旧版本自动废止
 * （废止原因“已被新版本 Vx.x 替代”）。
 * <p>
 * 适用范围：适用科室 applicableDept（如“全院”或“中医内科、骨伤科”）、
 * 适用人员 applicablePersonnel（如“全体医师”“全体医务人员”）。
 * <p>
 * 在线阅读：正文存 content（TEXT）字段，详情弹窗直接展示；下载：按
 * 正文生成 .txt 文件下载（见 PolicyService#downloadText），不引入
 * 真实文件存储。查阅与学习记录见 PolicyReading。
 * <p>
 * 失效不单独存状态：已发布且失效日期已过当天即“已过期”，按失效日期
 * 动态判断；30 天内到期即“即将到期”。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("policy")
public class Policy extends BaseEntity {

    /** 法规名称，如“中华人民共和国医师法” */
    @NotBlank(message = "法规名称不能为空")
    private String title;

    /** 文号，如“主席令第95号”“卫医发〔2016〕28号”（同一文号同一版本不可重复） */
    @NotBlank(message = "文号不能为空")
    private String policyNo;

    /** 分类：LAW 法律 / REGULATION 行政法规 / RULE 部门规章 / LOCAL 地方性法规 / NORMATIVE 规范性文件 / STANDARD 行业标准 / OTHER 其他 */
    @NotBlank(message = "法规分类不能为空")
    private String category;

    /** 发布层级：NATIONAL 国家 / PROVINCIAL 省 / MUNICIPAL 市 / HOSPITAL 院内 */
    @NotBlank(message = "发布层级不能为空")
    private String level;

    /** 发布机关，如“全国人民代表大会常务委员会”“国家卫生健康委员会” */
    private String issuingAuthority;

    /** 版本号，如 V1.0、V2.0 */
    private String version;

    /** 正文内容（在线阅读、下载用） */
    private String content;

    /** 适用科室，如“全院”或“中医内科、骨伤科” */
    private String applicableDept;

    /** 适用人员，如“全体医师”“全体医务人员” */
    private String applicablePersonnel;

    /** 归口科室 */
    private String department;

    /** 起草人 */
    private String drafter;

    /** 审批人（发布时必填） */
    private String approver;

    /** 发布日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishDate;

    /** 生效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    /** 失效日期（可空；已发布且失效日期已过当天即已过期） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    /** 状态：0 草稿，1 已发布，2 已废止 */
    private Integer status;

    /** 废止原因（废止时必填；新版本发布自动废止旧版本时写“已被新版本 Vx.x 替代”） */
    private String abolishReason;

    /** 审批记录（发布/废止留痕，如“医务科 吴文斌：发布（同意）”） */
    private String approvalHistory;

    /** 备注 */
    private String remark;
}
