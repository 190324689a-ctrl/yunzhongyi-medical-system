package com.yunzhongyi.medical.templatelib.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 复制模板请求：新模板名称。
 * 复制后新模板默认停用，检查项与原模板一致，版本号从 V1.0 重新开始。
 */
@Data
public class TemplateCopyRequest {

    /** 新模板名称 */
    @NotBlank(message = "新模板名称不能为空")
    private String templateName;
}
