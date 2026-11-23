package com.yunzhongyi.medical.coresystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 核心制度
 * <p>
 * 覆盖需求（需求梳理.md 第 16 项）：核心制度落实、记录、检查。
 * 本模块管核心制度库：制度分类、发布审批、废止、新版本、在线阅读/下载、
 * 学习记录（见 CoreSystemReading）。
 * <p>
 * 制度类别 category：MEDICAL 医疗 / NURSING 护理 / PHARMACY 药事 /
 * MANAGEMENT 管理 / SAFETY 安全 / OTHER 其他（见 CoreSystemTypeUtil）。
 * <p>
 * 适用层级 level：HOSPITAL 院级 / DEPT 科室级。
 * <p>
 * 状态 status：0 草稿，1 已发布，2 已废止。
 * 生命周期与政策法规管理（模块 11）一致：草稿 --发布（一级审批：
 * 审批人必填，审批记录留痕）--> 已发布 --废止（废止原因必填）-->
 * 已废止；已发布的制度不可直接修改，更新走“新建版本”：生成一条
 * 草稿记录（制度编号相同、版本号 +1），新版本发布时旧版本自动废止
 * （废止原因“已被新版本 Vx.x 替代”）。
 * <p>
 * 适用范围：适用科室 applicableDept（如“全院”或“中医内科、骨伤科”）、
 * 适用人员 applicablePersonnel（如“全体医师”“全体医务人员”）。
 * <p>
 * 在线阅读：正文存 content（TEXT）字段，详情弹窗直接展示；下载：按
 * 正文生成 .txt 文件下载（见 CoreSystemService#downloadText），不引入
 * 真实文件存储。查阅与学习记录见 CoreSystemReading。
 * <p>
 * 失效不单独存状态：已发布且失效日期已过当天即“已过期”，按失效日期
 * 动态判断；30 天内到期即“即将到期”。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("core_system")
public class CoreSystem extends BaseEntity {

    /** 制度名称，如“首诊负责制度” */
    @NotBlank(message = "制度名称不能为空")
    private String title;

    /** 制度编号，如“HX-ZD-2024-001”（同一编号同一版本不可重复） */
    @NotBlank(message = "制度编号不能为空")
    private String systemNo;

    /** 类别：MEDICAL 医疗 / NURSING 护理 / PHARMACY 药事 / MANAGEMENT 管理 / SAFETY 安全 / OTHER 其他 */
    @NotBlank(message = "制度类别不能为空")
    private String category;

    /** 适用层级：HOSPITAL 院级 / DEPT 科室级 */
    @NotBlank(message = "适用层级不能为空")
    private String level;

    /** 制定部门，如“医务科”“护理部” */
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
