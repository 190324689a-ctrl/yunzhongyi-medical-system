package com.yunzhongyi.medical;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunzhongyi.medical.auth.ApiPermissionMapping;
import com.yunzhongyi.medical.auth.JwtUtil;
import com.yunzhongyi.medical.system.entity.SysRolePermission;
import com.yunzhongyi.medical.system.mapper.SysRolePermissionMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 接口级权限验证（H2 内存库；本类用 @TestPropertySource 显式开启接口拦截，
 * 其余模块测试仍走 h2 profile 默认关闭拦截，见 application.yml）。
 * <p>
 * 权限模型：登录时把角色权限编码快照写进 JWT，拦截器只认 token 里的快照
 * （ADMIN 角色放行全部接口）；角色改权限后需重新登录才生效。
 * 种子角色权限：ADMIN 全部 33 个、MEDICAL_ADMIN（wuwenbin）25 个、
 * DEPT_HEAD（chen）14 个、DOCTOR（sunxiaohong）6 个（physician/leave/schedule/mdt/notification/briefing）。
 * 仅需登录态的接口：/api/auth/**、GET /api/depts/enabled、GET /api/dicts/{dictCode}。
 * 新增 0 个接口（只加拦截逻辑），全量合计接口 720 不变、测试 408→419。
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.auth.enabled=true")
class InterfacePermissionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    @Test
    void unauthenticatedBusinessApiDenied() throws Exception {
        mockMvc.perform(get("/api/physicians"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/statistics/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void doctorAllowedOwnPages() throws Exception {
        String token = loginAs("sunxiaohong");
        for (String path : List.of("/api/physicians", "/api/leaves", "/api/schedules",
                "/api/mdts", "/api/notifications/overview", "/api/briefings")) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Test
    void doctorForbiddenAdminPages() throws Exception {
        String token = loginAs("sunxiaohong");
        for (String path : List.of("/api/users", "/api/roles", "/api/depts",
                "/api/quality-inspections", "/api/statistics/overview",
                "/api/system-settings/overview")) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("无权限访问该接口"));
        }
    }

    @Test
    void doctorReferenceLookupsAllowed() throws Exception {
        String token = loginAs("sunxiaohong");
        // 启用科室下拉框：业务页面共用，不绑定科室管理权限
        mockMvc.perform(get("/api/depts/enabled").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        // 字典下拉框：按编码取数据项，不绑定字典管理权限
        mockMvc.perform(get("/api/dicts/title").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        // 但科室/字典管理接口本身仍要对应权限
        mockMvc.perform(get("/api/dicts").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/depts/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deptHeadAllowedAndForbidden() throws Exception {
        String token = loginAs("chen");
        mockMvc.perform(get("/api/quality-inspections").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/assessments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/statistics/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/new-techs").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicalAdminAllowedAndForbidden() throws Exception {
        String token = loginAs("wuwenbin");
        mockMvc.perform(get("/api/statistics/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/mobile/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/system-settings/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/module-completions").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAllPass() throws Exception {
        String token = loginAs("admin");
        for (String path : List.of("/api/users", "/api/roles", "/api/system-settings/overview",
                "/api/module-completions", "/api/statistics/overview", "/api/depts")) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Test
    void authEndpointsNeedOnlyLogin() throws Exception {
        String token = loginAs("sunxiaohong");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/auth/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void permissionChangeRequiresRelogin() throws Exception {
        // 旧 token：普通医师没有用户管理权限
        String oldToken = loginAs("sunxiaohong");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isForbidden());
        // 管理员给 DOCTOR 角色加上 user:list
        SysRolePermission link = new SysRolePermission();
        link.setRoleId(3L);
        link.setPermissionId(7L);
        try {
            rolePermissionMapper.insert(link);
            // 旧 token 仍是登录时的快照：继续 403
            mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + oldToken))
                    .andExpect(status().isForbidden());
            // 重新登录后新 token 带有新权限：放行
            String newToken = loginAs("sunxiaohong");
            mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + newToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, 3L)
                    .eq(SysRolePermission::getPermissionId, 7L));
        }
    }

    @Test
    void tokenCarriesPermissionSnapshot() throws Exception {
        String token = loginAs("sunxiaohong");
        Claims claims = jwtUtil.parse(token);
        assertTrue(JwtUtil.permissionsOf(claims).contains("physician:list"));
        assertTrue(JwtUtil.permissionsOf(claims).contains("leave:list"));
        assertFalse(JwtUtil.permissionsOf(claims).contains("user:list"));
        // 只有角色、没有权限快照的老式 token：连自己的业务接口也进不去（只认快照）
        String legacyToken = jwtUtil.createToken(4L, "sunxiaohong", "孙小红", List.of("DOCTOR"));
        mockMvc.perform(get("/api/leaves").header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isForbidden());
        // ADMIN 角色即使没有权限快照也放行（向后兼容 + 超级管理员）
        String legacyAdmin = jwtUtil.createToken(1L, "admin", "系统管理员", List.of("ADMIN"));
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + legacyAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void mappingCoversAllControllers() {
        // 66 个非鉴权 Controller 前缀全部登记在映射表里（与 sys_permission.code 一致）
        Map<String, String> expected = Map.ofEntries(
                Map.entry("/api/physicians", "physician:list"),
                Map.entry("/api/leaves", "leave:list"),
                Map.entry("/api/authorizations", "authorization:list"),
                Map.entry("/api/assessments", "assessment:list"),
                Map.entry("/api/assessment-templates", "assessment:list"),
                Map.entry("/api/schedules", "schedule:list"),
                Map.entry("/api/schedule-templates", "schedule:list"),
                Map.entry("/api/users", "user:list"),
                Map.entry("/api/roles", "role:list"),
                Map.entry("/api/depts", "dept:list"),
                Map.entry("/api/dicts", "dict:list"),
                Map.entry("/api/permissions", "role:list"),
                Map.entry("/api/oper-logs", "log:list"),
                Map.entry("/api/mdts", "mdt:list"),
                Map.entry("/api/quality-inspections", "quality:list"),
                Map.entry("/api/quality-templates", "quality:list"),
                Map.entry("/api/new-techs", "newtech:list"),
                Map.entry("/api/statistics", "statistics:list"),
                Map.entry("/api/workloads", "statistics:list"),
                Map.entry("/api/quality-indicators", "statistics:list"),
                Map.entry("/api/personnel-changes", "personnel-change:list"),
                Map.entry("/api/radiation-personnel", "radiation:list"),
                Map.entry("/api/radiation-qualifications", "radiation:list"),
                Map.entry("/api/radiation-trainings", "radiation:list"),
                Map.entry("/api/radiation-health", "radiation:list"),
                Map.entry("/api/committees", "committee:list"),
                Map.entry("/api/committee-meetings", "committee:list"),
                Map.entry("/api/committee-resolutions", "committee:list"),
                Map.entry("/api/documents", "document:list"),
                Map.entry("/api/policies", "policy:list"),
                Map.entry("/api/policy-readings", "policy:list"),
                Map.entry("/api/ethics-profiles", "ethics:list"),
                Map.entry("/api/ethics-templates", "ethics:list"),
                Map.entry("/api/ethics-evaluations", "ethics:list"),
                Map.entry("/api/ethics-complaints", "ethics:list"),
                Map.entry("/api/ethics-commendations", "ethics:list"),
                Map.entry("/api/core-systems", "core-system:list"),
                Map.entry("/api/core-system-readings", "core-system:list"),
                Map.entry("/api/rectification-summaries", "rectification:list"),
                Map.entry("/api/medical-techs", "tech-grading:list"),
                Map.entry("/api/tech-admissions", "tech-grading:list"),
                Map.entry("/api/tech-authorizations", "tech-grading:list"),
                Map.entry("/api/inspection-items", "inspection:list"),
                Map.entry("/api/inspection-standards", "inspection:list"),
                Map.entry("/api/inspection-template-items", "inspection:list"),
                Map.entry("/api/inspection-categories", "inspection-category:list"),
                Map.entry("/api/template-library", "template-library:list"),
                Map.entry("/api/grading-standards", "med-grading:list"),
                Map.entry("/api/grading-assessments", "med-grading:list"),
                Map.entry("/api/module-completions", "completion:list"),
                Map.entry("/api/system-settings", "system-settings:list"),
                Map.entry("/api/sys-params", "system-settings:list"),
                Map.entry("/api/sys-backups", "system-settings:list"),
                Map.entry("/api/sys-interfaces", "system-settings:list"),
                Map.entry("/api/sys-notice-settings", "system-settings:list"),
                Map.entry("/api/sys-knowledge", "system-settings:list"),
                Map.entry("/api/notifications", "notification:list"),
                Map.entry("/api/site-messages", "notification:list"),
                Map.entry("/api/todo-reminders", "notification:list"),
                Map.entry("/api/push-records", "notification:list"),
                Map.entry("/api/sms-records", "notification:list"),
                Map.entry("/api/briefings", "briefing:list"),
                Map.entry("/api/briefing-templates", "briefing:list"),
                Map.entry("/api/mobile", "mobile:list"),
                Map.entry("/api/mobile-devices", "mobile:list"),
                Map.entry("/api/mobile-approvals", "mobile:list"));
        assertEquals(66, expected.size());
        expected.forEach((prefix, code) -> {
            assertEquals(code, ApiPermissionMapping.requiredPermission("GET", prefix),
                    "映射缺失或错误：" + prefix);
            assertEquals(code, ApiPermissionMapping.requiredPermission("POST", prefix + "/1/approve"),
                    "子路径应继承前缀权限：" + prefix);
        });
        // 段边界：/api/committees 不得误匹配 /api/committee-meetings
        assertEquals("committee:list",
                ApiPermissionMapping.requiredPermission("GET", "/api/committee-meetings"));
        // 仅需登录态
        assertNull(ApiPermissionMapping.requiredPermission("GET", "/api/auth/me"));
        assertNull(ApiPermissionMapping.requiredPermission("GET", "/api/auth/menus"));
        assertNull(ApiPermissionMapping.requiredPermission("POST", "/api/auth/logout"));
        assertNull(ApiPermissionMapping.requiredPermission("GET", "/api/depts/enabled"));
        assertNull(ApiPermissionMapping.requiredPermission("GET", "/api/dicts/title"));
        assertNull(ApiPermissionMapping.requiredPermission("GET", "/api/dicts/education"));
        // 字典管理端点不走参照特例
        assertEquals("dict:list", ApiPermissionMapping.requiredPermission("GET", "/api/dicts/stats"));
        assertEquals("dict:list", ApiPermissionMapping.requiredPermission("GET", "/api/dicts"));
        // 未登记的接口默认拒绝
        assertEquals(ApiPermissionMapping.DENY,
                ApiPermissionMapping.requiredPermission("GET", "/api/future-module"));
        assertEquals(ApiPermissionMapping.DENY,
                ApiPermissionMapping.requiredPermission("GET", "/api/unknown"));
    }

    // ---------- helper ----------

    /** 登录成功并取 token（data.token） */
    private String loginAs(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"token\":\"") + 9;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }
}
