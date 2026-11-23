package com.yunzhongyi.medical.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 文件制度
 * <p>
 * 覆盖需求（需求梳理.md 第 10 项）：文件、制度、规范、流程管理；
 * 支持分类、版本、发布、作废、查询；支持在线阅读、下载。
 * <p>
 * 文件分类 category：SYSTEM 制度 / STANDARD 规范 / PROCESS 流程 /
 * FILE 文件 / OTHER 其他（见 DocumentTypeUtil）。
 * <p>
 * 状态 status：0 草稿，1 已发布，2 已作废。
 * 生命周期：草稿 --发布（一级审批：审批人必填，审批记录留痕）--> 已发布
 * --作废（作废原因必填）--> 已作废；已发布的文件不可直接修改，改版
 * 走“新建版本”：生成一条草稿记录（文件编号相同、版本号 +1），新版本
 * 发布时旧版本自动作废（作废原因“已被新版本 Vx.x 替代”）。
 * <p>
 * 在线阅读：正文存 content（TEXT）字段，详情弹窗直接展示；下载：按
 * 正文生成 .txt 文件下载（见 DocumentService#downloadText），不引入
 * 真实文件存储。
 * <p>
 * 失效不单独存状态：已发布且失效日期已过当天即“已过期”，按失效日期
 * 动态判断；30 天内到期即“即将到期”。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {

    /** 文件标题，如“三级医师查房制度” */
    @NotBlank(message = "文件标题不能为空")
    private String title;

    /** 文件编号，如 YW-ZD-2024-001（同一编号同一版本不可重复） */
    @NotBlank(message = "文件编号不能为空")
    private String docNo;

    /** 分类：SYSTEM 制度 / STANDARD 规范 / PROCESS 流程 / FILE 文件 / OTHER 其他 */
    @NotBlank(message = "文件分类不能为空")
    private String category;

    /** 版本号，如 V1.0、V2.0 */
    private String version;

    /** 正文内容（在线阅读、下载用） */
    private String content;

    /** 发文/归口科室 */
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

    /** 状态：0 草稿，1 已发布，2 已作废 */
    private Integer status;

    /** 作废原因（作废时必填；新版本发布自动作废旧版本时写“已被新版本 Vx.x 替代”） */
    private String abolishReason;

    /** 审批记录（发布/作废留痕，如“医务科 吴文斌：发布（同意）”） */
    private String approvalHistory;

    /** 备注 */
    private String remark;
}
