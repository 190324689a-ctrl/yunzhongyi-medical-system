package com.yunzhongyi.medical.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 检查发现的问题：检查记录中发现的每一个问题。
 * <p>
 * 覆盖需求（需求梳理.md 第 13/14/15 项）：检查记录的问题清单、整改反馈。
 * <p>
 * 严重程度 severity：MAJOR 严重 / MINOR 一般 / MILD 轻微（见 QualityTypeUtil）。
 * <p>
 * 每个问题对应一个整改任务，见 {@link QualityRectification}；问题本身不单独
 * 设状态，整改进度以整改任务的状态为准。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("quality_issue")
public class QualityIssue extends BaseEntity {

    /** 所属检查记录 id（服务端从路径参数补值，不由客户端传） */
    private Long recordId;

    /** 所属检查计划 id（冗余，取自记录） */
    private Long planId;

    /** 问题描述 */
    @NotBlank(message = "问题描述不能为空")
    private String description;

    /** 严重程度：MAJOR 严重 / MINOR 一般 / MILD 轻微 */
    @NotBlank(message = "严重程度不能为空")
    private String severity;

    /** 责任科室名称 */
    private String respDeptName;

    /** 责任人 */
    private String respPerson;

    /** 发现日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate foundDate;
}
