package com.yunzhongyi.medical.briefing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医疗工作简报。
 * <p>
 * 覆盖需求（需求梳理.md 第 30 项）：工作简报生成、发布、统计；
 * 支持模板、自动生成、导出 PDF/Word。
 * <p>
 * 发布状态 status：0 草稿，1 待审批，2 已发布，3 已驳回；一级审批（医务科）。
 * 流转：草稿 --提交--> 待审批 --通过--> 已发布 / --驳回--> 已驳回；
 * 已发布的简报不可修改、不可删除（请先作废重发新一期）；已驳回的简报可改回草稿重提。
 * 期号 issueNo 全院唯一，如 2026-11-01；统计周期 period 为 yyyy-MM。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("briefing")
public class Briefing extends BaseEntity {

    /** 期号，全院唯一，如 2026-11-01 */
    @NotBlank(message = "期号不能为空")
    private String issueNo;

    /** 简报标题 */
    @NotBlank(message = "简报标题不能为空")
    private String title;

    /** 模板 id */
    private Long templateId;

    /** 模板名称（冗余，模板删除后仍可看） */
    private String templateName;

    /** 简报分类：MONTHLY / QUARTERLY / ANNUAL / SPECIAL / DYNAMIC */
    private String category;

    /** 统计周期：yyyy-MM，如 2026-11 */
    private String period;

    /** 正文内容（自动生成或手工撰写） */
    private String content;

    /** 发布状态：0 草稿，1 待审批，2 已发布，3 已驳回 */
    private Integer status;

    /** 撰稿人 */
    private String authorName;

    /** 发布日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishDate;

    /** 审批人（医务科） */
    private String approver;

    /** 最新审批意见 */
    private String approvalOpinion;

    /** 审批记录（留痕，如“医务科 吴文斌：通过（同意发布）”） */
    private String approvalHistory;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 驳回原因 */
    private String rejectReason;

    /** 备注 */
    private String remark;
}
