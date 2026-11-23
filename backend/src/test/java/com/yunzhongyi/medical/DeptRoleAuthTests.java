package com.yunzhongyi.medical;

import com.yunzhongyi.medical.auth.AuthService;
import com.yunzhongyi.medical.system.entity.SysDept;
import com.yunzhongyi.medical.system.entity.SysPermission;
import com.yunzhongyi.medical.system.entity.SysRole;
import com.yunzhongyi.medical.system.entity.SysRolePermission;
import com.yunzhongyi.medical.system.mapper.SysDeptMapper;
import com.yunzhongyi.medical.system.mapper.SysPermissionMapper;
import com.yunzhongyi.medical.system.mapper.SysRoleMapper;
import com.yunzhongyi.medical.system.mapper.SysRolePermissionMapper;
import com.yunzhongyi.medical.system.service.SysDeptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 部门角色授权增强验证（H2 内存库；本类显式开启接口拦截）。
 * <p>
 * 种子数据：科室 11 个（含 3 个下级科室：内科门诊/内科病房挂在中医内科下、
 * 急诊内科挂在急诊科下）；演示账号 4 个（admin/chen/wuwenbin/sunxiaohong，
 * 密码均为 123456）；角色可见页面：ADMIN 全部 33 个、MEDICAL_ADMIN 25 个、
 * DEPT_HEAD 14 个、DOCTOR 6 个；角色-科室绑定：MEDICAL_ADMIN→医务科、
 * DEPT_HEAD→中医内科、DOCTOR→全部临床科室。
 * 新增 4 个接口（科室树、角色绑定科室查/改、登录后菜单树），28 号台账接口 95→99、
 * 测试 36→52，全量合计接口 716→720、测试 392→408。
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.auth.enabled=true")
class DeptRoleAuthTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private SysDeptService deptService;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    /**
     * 各测试类共用同一 H2：其他类可能改动角色权限（如 SystemSettingsTests 会给角色 3
     * 分配权限做验证），每个测试前把三角色的权限种子恢复为 data.sql 的状态，
     * 本类断言不依赖执行顺序。
     */
    @BeforeEach
    void restoreRolePermissionSeeds() {
        restoreRolePermissions(2L, List.of(1L, 2L, 3L, 4L, 5L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L,
                21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 31L, 32L, 33L));
        restoreRolePermissions(3L, List.of(1L, 2L, 5L, 12L, 31L, 32L));
        restoreRolePermissions(4L, List.of(1L, 2L, 3L, 4L, 5L, 12L, 13L, 16L, 22L, 23L, 24L, 28L, 31L, 32L));
    }

    private void restoreRolePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        for (Long permissionId : permissionIds) {
            SysRolePermission link = new SysRolePermission();
            link.setRoleId(roleId);
            link.setPermissionId(permissionId);
            rolePermissionMapper.insert(link);
        }
    }

    @Test
    void contextLoads() {
        assertNotNull(authService);
        assertNotNull(deptService);
    }

    @Test
    void deptTreeStructure() throws Exception {
        // 接口：科室树 8 个顶级科室
        mockMvc.perform(get("/api/depts/tree").header("Authorization", "Bearer " + loginAs("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$..code", hasItems("NK-MZ", "NK-BF", "JZ-NK")));
        // 服务层：中医内科下 2 个下级科室，急诊科下 1 个
        List<Map<String, Object>> tree = deptService.tree();
        assertEquals(8, tree.size());
        Map<String, Object> zyNk = tree.stream()
                .filter(n -> "中医内科".equals(n.get("name"))).findFirst().orElseThrow();
        assertEquals(2, childrenOf(zyNk).size());
        assertTrue(childrenOf(zyNk).stream().map(n -> n.get("name")).toList()
                .containsAll(List.of("内科门诊", "内科病房")));
        Map<String, Object> jz = tree.stream()
                .filter(n -> "急诊科".equals(n.get("name"))).findFirst().orElseThrow();
        assertEquals(1, childrenOf(jz).size());
        assertEquals("急诊内科", childrenOf(jz).get(0).get("name"));
    }

    @Test
    void deptStatsTotal11() throws Exception {
        mockMvc.perform(get("/api/depts/stats").header("Authorization", "Bearer " + loginAs("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.enabled").value(11));
    }

    @Test
    void roleDeptsAssignGetAndCleanup() throws Exception {
        SysRole temp = new SysRole();
        temp.setCode("TEMP_ROLE");
        temp.setName("临时角色");
        temp.setStatus(1);
        roleMapper.insert(temp);
        String token = loginAs("admin");
        try {
            // 绑定两个科室后可查回
            mockMvc.perform(post("/api/roles/" + temp.getId() + "/depts")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deptIds\":[1,8]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.assigned").value(2));
            mockMvc.perform(get("/api/roles/" + temp.getId() + "/depts")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$..name", hasItems("中医内科", "医务科")));
            // 不存在的科室拒绝
            mockMvc.perform(post("/api/roles/" + temp.getId() + "/depts")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deptIds\":[9999]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
            // 不存在的角色拒绝
            mockMvc.perform(get("/api/roles/9999/depts")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        } finally {
            // 删除角色时科室绑定一并清理
            mockMvc.perform(delete("/api/roles/" + temp.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/roles/" + temp.getId() + "/depts")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Test
    void roleDeptSeedBindings() throws Exception {
        String token = loginAs("admin");
        // 医务科管理员只绑定医务科
        mockMvc.perform(get("/api/roles/2/depts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("医务科"));
        // 科主任只绑定中医内科
        mockMvc.perform(get("/api/roles/4/depts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("中医内科"));
        // 普通医师角色绑定全部 6 个临床科室
        mockMvc.perform(get("/api/roles/3/depts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$..name", hasItems("中医内科", "针灸推拿科", "骨伤科", "妇科", "儿科", "急诊科")));
    }

    @Test
    void menusAdminAll() {
        List<Map<String, Object>> menus = authService.menusOf(1L);
        // 33 个权限全部是目录/菜单：28 个顶级节点，扁平后 33 个
        assertEquals(28, menus.size());
        assertEquals(33, flatten(menus).size());
        Map<String, Object> systemDir = menus.stream()
                .filter(n -> "system:setting".equals(n.get("code"))).findFirst().orElseThrow();
        assertEquals(5, childrenOf(systemDir).size());
    }

    @Test
    void menusMedicalAdmin() throws Exception {
        String token = loginAs("wuwenbin");
        mockMvc.perform(get("/api/auth/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(25))
                .andExpect(jsonPath("$..code", hasItems("physician:list", "quality:list", "statistics:list")))
                .andExpect(jsonPath("$..code", not(hasItem("user:list"))))
                .andExpect(jsonPath("$..code", not(hasItem("role:list"))))
                .andExpect(jsonPath("$..code", not(hasItem("system-settings:list"))))
                .andExpect(jsonPath("$..code", not(hasItem("completion:list"))));
    }

    @Test
    void menusDeptHead() throws Exception {
        String token = loginAs("chen");
        mockMvc.perform(get("/api/auth/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(14))
                .andExpect(jsonPath("$..code", hasItems("assessment:list", "tech-grading:list", "med-grading:list")))
                .andExpect(jsonPath("$..code", not(hasItem("statistics:list"))))
                .andExpect(jsonPath("$..code", not(hasItem("system-settings:list"))))
                .andExpect(jsonPath("$..code", not(hasItem("newtech:list"))));
    }

    @Test
    void menusDoctor() throws Exception {
        String token = loginAs("sunxiaohong");
        mockMvc.perform(get("/api/auth/menus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$..code", hasItems("physician:list", "leave:list", "schedule:list", "mdt:list")))
                .andExpect(jsonPath("$..code", not(hasItem("quality:list"))))
                .andExpect(jsonPath("$..code", not(hasItem("assessment:list"))));
    }

    @Test
    void menusRequireAuth() throws Exception {
        mockMvc.perform(get("/api/auth/menus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void meIncludesDeptAndScope() throws Exception {
        // 科主任 chen：所属中医内科，角色绑定范围也是中医内科
        String chenToken = loginAs("chen");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + chenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.deptName").value("中医内科"))
                .andExpect(jsonPath("$.data.roles", hasItem("DEPT_HEAD")))
                .andExpect(jsonPath("$.data.deptScope.length()").value(1))
                .andExpect(jsonPath("$.data.deptScope[0]").value("中医内科"));
        // 医务科管理员：所属医务科，范围医务科
        String wuToken = loginAs("wuwenbin");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + wuToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.deptName").value("医务科"))
                .andExpect(jsonPath("$.data.roles", hasItem("MEDICAL_ADMIN")))
                .andExpect(jsonPath("$.data.deptScope[0]").value("医务科"));
    }

    @Test
    void demoAccountsLogin() throws Exception {
        // 医务科账号登录返回角色与权限
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wuwenbin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roles", hasItem("MEDICAL_ADMIN")))
                .andExpect(jsonPath("$.data.permissions", hasItem("physician:list")))
                .andExpect(jsonPath("$.data.user.deptName").value("医务科"));
        // 普通医师账号：有 MDT、无质量检查
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sunxiaohong\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roles", hasItem("DOCTOR")))
                .andExpect(jsonPath("$.data.permissions", hasItem("mdt:list")))
                .andExpect(jsonPath("$.data.permissions", not(hasItem("quality:list"))));
    }

    @Test
    void rolePermissionSeedCounts() throws Exception {
        String token = loginAs("admin");
        mockMvc.perform(get("/api/roles/2/permissions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(25));
        mockMvc.perform(get("/api/roles/3/permissions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));
        mockMvc.perform(get("/api/roles/4/permissions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(14));
    }

    @Test
    void deptHierarchyGuards() throws Exception {
        String token = loginAs("admin");
        SysDept temp = new SysDept();
        temp.setName("临时下级科室");
        temp.setCode("TEMP-DEPT");
        temp.setType("clinical");
        temp.setParentId(4L); // 挂在妇科下
        temp.setStatus(1);
        deptMapper.insert(temp);
        try {
            // 有下级科室的妇科不可停用、不可直接删除
            mockMvc.perform(post("/api/depts/4/disable").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
            mockMvc.perform(delete("/api/depts/4").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        } finally {
            deptMapper.deleteById(temp.getId());
        }
        // 下级删掉后，妇科停用/启用恢复正常
        mockMvc.perform(post("/api/depts/4/disable").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/depts/4/enable").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void menusExcludeDisabledPermission() {
        // 停用 MDT 菜单权限后，普通医师的菜单里不再出现 MDT
        SysPermission mdt = permissionMapper.selectById(12L);
        assertEquals(Integer.valueOf(1), mdt.getStatus());
        try {
            mdt.setStatus(0);
            permissionMapper.updateById(mdt);
            List<String> codes = flatten(authService.menusOf(4L)).stream()
                    .map(n -> (String) n.get("code")).toList();
            assertFalse(codes.contains("mdt:list"));
            assertEquals(5, codes.size());
        } finally {
            mdt.setStatus(1);
            permissionMapper.updateById(mdt);
        }
        assertTrue(flatten(authService.menusOf(4L)).stream()
                .map(n -> (String) n.get("code")).toList().contains("mdt:list"));
    }

    @Test
    void assignRoleDeptsRejectsUnknownDept() throws Exception {
        String token = loginAs("admin");
        mockMvc.perform(post("/api/roles/3/depts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deptIds\":[9999]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/roles/9999/depts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deptIds\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childrenOf(Map<String, Object> node) {
        return (List<Map<String, Object>>) node.get("children");
    }

    /** 菜单/科室树扁平化（前序） */
    private List<Map<String, Object>> flatten(List<Map<String, Object>> tree) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> node : tree) {
            out.add(node);
            out.addAll(flatten(childrenOf(node)));
        }
        return out;
    }
}
