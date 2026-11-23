package com.yunzhongyi.medical.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 接口鉴权拦截器：除登录接口外，所有 /api/** 请求必须携带有效 JWT。
 * <p>
 * 无 token / token 非法 / token 过期 / 已登出：直接返回 HTTP 401 + 统一 Result 体，
 * 不进入业务接口。token 校验通过后，再按 {@link ApiPermissionMapping} 查本次请求
 * 所需权限（取自 token 里的登录时权限快照，ADMIN 放行全部），无权限返回 HTTP 403。
 * 开关 {@code app.auth.enabled}（默认 true）：
 * h2 测试 profile 默认关闭（历史模块测试不带 token，见 application.yml），
 * 生产 mysql profile 默认开启；AuthTests / InterfacePermissionTests 用
 * @TestPropertySource 显式开启验证。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Value("${app.auth.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        if (!enabled) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            unauthorized(response, "未登录，请先登录");
            return false;
        }
        String token = header.substring(7);
        if (authService.isBlacklisted(token)) {
            unauthorized(response, "登录已失效，请重新登录");
            return false;
        }
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (ExpiredJwtException e) {
            unauthorized(response, "登录已过期，请重新登录");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(response, "登录凭证无效，请重新登录");
            return false;
        }
        Long userId = JwtUtil.userIdOf(claims);
        if (userId == null) {
            unauthorized(response, "登录凭证无效，请重新登录");
            return false;
        }
        AuthContext.set(new AuthContext.CurrentUser(
                userId, claims.getSubject(), claims.get("realName", String.class),
                JwtUtil.rolesOf(claims), JwtUtil.permissionsOf(claims)));
        // 接口级权限：token 校验通过后，按集中式映射查本次请求所需权限。
        // 权限取自 token 里的登录时快照（角色改权限后需重新登录才生效）；
        // ADMIN 角色放行全部接口；未登记映射的接口默认拒绝。
        String required = ApiPermissionMapping.requiredPermission(request.getMethod(), request.getRequestURI());
        if (ApiPermissionMapping.DENY.equals(required)) {
            forbidden(response, "接口未配置权限映射，请联系管理员");
            return false;
        }
        if (required != null) {
            boolean isAdmin = JwtUtil.rolesOf(claims).contains("ADMIN");
            if (!isAdmin && !JwtUtil.permissionsOf(claims).contains(required)) {
                forbidden(response, "无权限访问该接口");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void unauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    /** 已登录但无权访问：HTTP 403 + 统一 Result 体（不清除登录态，前端只提示不登出）。 */
    private void forbidden(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":403,\"msg\":\"" + msg + "\",\"data\":null}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
