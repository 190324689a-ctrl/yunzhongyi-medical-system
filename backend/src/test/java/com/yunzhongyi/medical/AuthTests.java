package com.yunzhongyi.medical;

import com.yunzhongyi.medical.auth.AuthController;
import com.yunzhongyi.medical.auth.AuthService;
import com.yunzhongyi.medical.auth.JwtUtil;
import com.yunzhongyi.medical.auth.PasswordUtil;
import com.yunzhongyi.medical.system.entity.SysUser;
import com.yunzhongyi.medical.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录鉴权验证（H2 内存库；本类用 @TestPropertySource 显式开启接口拦截，
 * 其余模块测试仍走 h2 profile 默认关闭拦截，见 application.yml）。
 * 种子数据：演示账号 admin / 123456（首次登录后密码自动迁移为 PBKDF2 哈希），
 * admin 已分配 ADMIN 角色，ADMIN 角色拥有全部 33 个权限。
 * 新增 5 个接口（登录/登出/当前用户/修改密码/登录后菜单树），28 号台账接口 91→99、测试 23→52，
 * 全量合计接口 712→720、测试 379→408。
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.auth.enabled=true")
class AuthTests {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthController authController;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(jwtUtil);
        assertNotNull(authService);
        assertNotNull(authController);
    }

    @Test
    void loginSuccess() throws Exception {
        String token = loginAndGetToken("admin", "123456");
        assertNotNull(token);
        assertFalse(token.isBlank());
        // 登录后种子明文密码已自动迁移为 PBKDF2 哈希
        SysUser admin = userMapper.selectById(1L);
        assertTrue(PasswordUtil.isHashed(admin.getPassword()));
        // 角色与权限取自底座表：admin 是 ADMIN，拥有全部权限
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.roles").value(hasItem("ADMIN")))
                .andExpect(jsonPath("$.data.permissions").value(hasItem("physician:list")));
    }

    @Test
    void loginWrongPassword() throws Exception {
        // 业务失败按项目约定返回 HTTP 200 + body code（见其余模块测试）
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("账号或密码错误"));
    }

    @Test
    void loginUnknownUser() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no-such-user\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("账号或密码错误"));
    }

    @Test
    void loginDisabledUser() throws Exception {
        SysUser temp = createTempUser("disabled-user", "123456", 0);
        try {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"disabled-user\",\"password\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("账号已停用，请联系管理员"));
        } finally {
            userMapper.deleteById(temp.getId());
        }
    }

    @Test
    void loginLockoutAfterFailures() throws Exception {
        SysUser temp = createTempUser("lock-user", "123456", 1);
        try {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"lock-user\",\"password\":\"wrong\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(401));
            }
            // 第 6 次：已锁定
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"lock-user\",\"password\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(429));
        } finally {
            userMapper.deleteById(temp.getId());
            authService.clearFailures("lock-user");
        }
    }

    @Test
    void unauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void authenticatedAccessAllowed() throws Exception {
        String token = loginAndGetToken("admin", "123456");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    void tamperedTokenRejected() throws Exception {
        String token = loginAndGetToken("admin", "123456");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void expiredTokenRejected() throws Exception {
        String expired = jwtUtil.createToken(1L, "admin", "系统管理员", java.util.List.of("ADMIN"), -1000L);
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void logoutBlacklistsToken() throws Exception {
        String token = loginAndGetToken("admin", "123456");
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        // 登出后的 token 再用即 401
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void changePasswordFlow() throws Exception {
        SysUser temp = createTempUser("pwd-user", "123456", 1);
        try {
            String token = loginAndGetToken("pwd-user", "123456");
            // 原密码错误拒绝
            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"newpass123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
            // 改密成功，当前 token 随即失效
            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"oldPassword\":\"123456\",\"newPassword\":\"newpass123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
            // 旧密码登不上，新密码可以
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"pwd-user\",\"password\":\"123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
            String newToken = loginAndGetToken("pwd-user", "newpass123");
            assertNotNull(newToken);
            // 新密码是 PBKDF2 哈希存储
            assertTrue(PasswordUtil.isHashed(userMapper.selectById(temp.getId()).getPassword()));
        } finally {
            userMapper.deleteById(temp.getId());
        }
    }

    @Test
    void passwordUtilRoundTrip() {
        String hash = PasswordUtil.hash("123456");
        assertTrue(PasswordUtil.isHashed(hash));
        assertTrue(PasswordUtil.matches("123456", hash));
        assertFalse(PasswordUtil.matches("654321", hash));
        // 两次哈希盐不同，结果不同
        assertNotEquals(hash, PasswordUtil.hash("123456"));
        // 历史明文占位仍能校验（供登录迁移）
        assertTrue(PasswordUtil.matches("123456", "123456"));
        assertFalse(PasswordUtil.matches("wrong", "123456"));
    }

    // ---------- helper ----------

    /** 登录成功并取 token（data.token） */
    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"token\":\"") + 9;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private SysUser createTempUser(String username, String password, int status) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(PasswordUtil.hash(password));
        u.setRealName("测试用户");
        u.setStatus(status);
        userMapper.insert(u);
        return u;
    }
}
