package com.yunzhongyi.medical.auth.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 登录成功返回：token + 当前用户信息 + 角色与权限编码 */
@Data
public class LoginResult {

    /** JWT */
    private String token;
    /** token 类型，固定 Bearer */
    private String tokenType = "Bearer";
    /** 有效期（秒） */
    private long expiresIn;
    /** 当前用户（id/username/realName/phone/deptId，不含密码） */
    private Map<String, Object> user;
    /** 角色编码，如 ADMIN */
    private List<String> roles;
    /** 权限标识，如 physician:list */
    private List<String> permissions;
}
