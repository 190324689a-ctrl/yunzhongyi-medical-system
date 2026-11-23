package com.yunzhongyi.medical.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求 */
@Data
public class LoginRequest {

    @NotBlank(message = "登录账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
