package com.yunzhongyi.medical.auth;

import com.yunzhongyi.medical.auth.dto.ChangePasswordRequest;
import com.yunzhongyi.medical.auth.dto.LoginRequest;
import com.yunzhongyi.medical.auth.dto.LoginResult;
import com.yunzhongyi.medical.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录鉴权接口（5 个）。
 * <p>
 * 只有 {@code /api/auth/login} 不需要 token；其余四个走鉴权拦截器，
 * 未登录 / token 过期 / 已登出均返回 401。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录：账号密码校验，成功返回 JWT + 用户信息 + 角色与权限 */
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req.getUsername(), req.getPassword()));
    }

    /** 登出：当前 token 加入黑名单，之后再用即 401 */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        authService.logout(extractToken(request));
        return Result.ok(Map.of("message", "已退出登录"));
    }

    /** 当前登录用户信息：数据库实时查角色与权限 */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(authService.currentUser(AuthContext.getUserId()));
    }

    /** 当前登录用户可见的菜单树：按角色权限过滤，无权限的页面不出现 */
    @GetMapping("/menus")
    public Result<java.util.List<Map<String, Object>>> menus() {
        return Result.ok(authService.menusOf(AuthContext.getUserId()));
    }

    /** 修改密码：校验原密码，新密码写 PBKDF2 哈希；成功后当前 token 失效，需重新登录 */
    @PostMapping("/change-password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                    HttpServletRequest request) {
        authService.changePassword(AuthContext.getUserId(), req.getOldPassword(), req.getNewPassword());
        authService.logout(extractToken(request));
        return Result.ok(Map.of("message", "密码修改成功，请重新登录"));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
