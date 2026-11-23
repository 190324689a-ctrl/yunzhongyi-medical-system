package com.yunzhongyi.medical.auth;

import java.util.List;

/**
 * 当前登录用户上下文（ThreadLocal，由鉴权拦截器在请求内填充、请求结束清理）。
 */
public final class AuthContext {

    private AuthContext() {
    }

    public record CurrentUser(Long userId, String username, String realName, List<String> roles,
                              List<String> permissions) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String getUsername() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.username();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
