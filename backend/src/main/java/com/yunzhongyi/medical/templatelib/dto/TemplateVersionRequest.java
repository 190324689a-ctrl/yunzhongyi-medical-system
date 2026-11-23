package com.yunzhongyi.medical.templatelib.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新建模板版本请求：新一版检查项 JSON + 版本说明。
 * 检查项权重之和必须为 100（服务端校验）。
 */
@Data
public class TemplateVersionRequest {

    /** 新一版检查项 JSON：[{"name":"...","weight":30,"standard":"..."}] */
    @NotBlank(message = "检查项不能为空")
    private String checkItems;

    /** 版本说明，如“病历完整性权重由 40 调整为 35” */
    private String changeRemark;
}
