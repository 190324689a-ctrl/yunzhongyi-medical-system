package com.yunzhongyi.medical.briefing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按模板自动生成简报请求：模板须启用；统计周期 yyyy-MM；标题为空时按
 * “{模板名称}（{统计周期}）”自动生成；期号为空时按“{统计周期}-序号”自动生成。
 */
@Data
public class GenerateBriefingRequest {

    /** 模板 id */
    @NotNull(message = "模板不能为空")
    private Long templateId;

    /** 统计周期：yyyy-MM，如 2026-11 */
    @NotBlank(message = "统计周期不能为空")
    private String period;

    /** 简报标题（为空自动生成） */
    private String title;

    /** 期号（为空自动生成，全院唯一） */
    private String issueNo;

    /** 撰稿人 */
    private String authorName;
}
