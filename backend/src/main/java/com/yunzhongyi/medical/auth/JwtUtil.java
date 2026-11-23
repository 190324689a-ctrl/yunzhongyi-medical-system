package com.yunzhongyi.medical.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 工具（JJWT 0.11.5，HS256）。
 * <p>
 * 密钥来自配置 {@code app.auth.jwt-secret}，可用环境变量 AUTH_JWT_SECRET 覆盖；
 * 生产务必换成足够长的随机密钥（HS256 要求至少 32 字节）。
 */
@Component
public class JwtUtil {

    @Value("${app.auth.jwt-secret:yunzhongyi-medical-dev-secret-key-change-in-production-please-use-long-random-secret}")
    private String secret;

    /** token 有效期（小时），默认 8 */
    @Value("${app.auth.jwt-expire-hours:8}")
    private long expireHours;

    public long getExpireSeconds() {
        return expireHours * 3600;
    }

    private SecretKey signingKey() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT 密钥过短（HS256 至少 32 字节），请配置更长的 app.auth.jwt-secret");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    /** 签发 token（默认有效期） */
    public String createToken(Long userId, String username, String realName, List<String> roles) {
        return createToken(userId, username, realName, roles, List.of());
    }

    /** 签发 token（默认有效期，带登录时快照的权限编码） */
    public String createToken(Long userId, String username, String realName,
                              List<String> roles, List<String> permissions) {
        return createToken(userId, username, realName, roles, permissions, expireHours * 3600 * 1000L);
    }

    /** 签发 token（指定有效期毫秒；测试过期 token 用） */
    public String createToken(Long userId, String username, String realName, List<String> roles, long expireMillis) {
        return createToken(userId, username, realName, roles, List.of(), expireMillis);
    }

    /** 签发 token（指定有效期毫秒与权限快照；测试用） */
    public String createToken(Long userId, String username, String realName,
                              List<String> roles, List<String> permissions, long expireMillis) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .claim("userId", userId)
                .claim("realName", realName)
                .claim("roles", roles)
                // 权限编码快照：登录时从角色-权限关联实时查库写入；
                // 接口权限校验只认这份快照，角色改权限后需重新登录才生效。
                .claim("permissions", permissions)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析并校验 token：签名不对、过期都会抛 JwtException */
    public Claims parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 从 claims 取 userId（Jackson 可能把数字反序列化为 Integer，用 Number 兼容） */
    public static Long userIdOf(Claims claims) {
        Object v = claims.get("userId");
        return v instanceof Number number ? number.longValue() : null;
    }

    /** 从 claims 取角色编码列表 */
    @SuppressWarnings("unchecked")
    public static List<String> rolesOf(Claims claims) {
        Object v = claims.get("roles");
        return v instanceof List ? (List<String>) v : List.of();
    }

    /** 从 claims 取登录时快照的权限编码列表（老 token 没有该 claim 时为空） */
    @SuppressWarnings("unchecked")
    public static List<String> permissionsOf(Claims claims) {
        Object v = claims.get("permissions");
        return v instanceof List ? (List<String>) v : List.of();
    }
}
