package com.yunzhongyi.medical.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 接口级权限映射（集中式）：请求路径前缀 → 所需权限编码。
 * <p>
 * 由 {@link AuthInterceptor} 在 token 校验通过后调用：命中映射的接口，
 * 只有 token 权限快照里持有对应编码（或角色为 ADMIN）才能访问，否则返回 403。
 * <p>
 * 映射规则（按顺序判定，先命中先生效）：
 * <ol>
 *   <li>{@code /api/auth/**}：仅需登录态，不需要特定权限（登录/登出/当前用户/菜单树/改密）。</li>
 *   <li>跨页面共用的只读参照数据，仅需登录态：
 *     {@code GET /api/depts/enabled}（启用科室下拉框）、
 *     {@code GET /api/dicts/{dictCode}}（按字典编码取数据项，如职称/学历下拉框）。
 *     这两个接口被多个业务页面共用，而调用方（如普通医师）未必持有科室/字典管理权限，
 *     因此不绑定管理权限；写操作仍走各自的管理权限。</li>
 *   <li>其余接口按 Controller 前缀映射到对应页面权限（与 sys_permission 的 code 一致，
 *     如 {@code /api/leaves} → {@code leave:list}；同一页面的子接口共用一个权限，
 *     如 {@code /api/assessment-templates} 与 {@code /api/assessments} 同属 assessment:list）。</li>
 *   <li>未登记的 {@code /api/**} 路径默认拒绝（{@link #DENY}）：新接口上线前必须先在这里登记映射，
 *     避免新接口悄悄绕过权限控制。</li>
 * </ol>
 * <p>
 * 前缀匹配按“段边界”判定：路径等于前缀、或以“前缀 + /”开头才算命中，
 * 如 {@code /api/committees} 不会误匹配 {@code /api/committee-meetings}。
 */
public final class ApiPermissionMapping {

    private ApiPermissionMapping() {
    }

    /** 未在映射表里的接口：默认拒绝，提示联系管理员登记映射。 */
    public static final String DENY = "__DENY__";

    /** 仅需登录态、不需要特定权限时，{@link #requiredPermission(String, String)} 返回 null。 */

    /** 字典参照接口里属于管理端点的固定子路径（不走“仅需登录态”特例）。 */
    private static final Set<String> DICT_MANAGEMENT_SEGMENTS = Set.of("stats", "types", "export", "template");

    /** Controller 前缀 → 权限编码（按 sys_permission.code）。 */
    private static final Map<String, String> PREFIX_TO_PERMISSION = new LinkedHashMap<>();

    static {
        PREFIX_TO_PERMISSION.put("/api/physicians", "physician:list");
        PREFIX_TO_PERMISSION.put("/api/leaves", "leave:list");
        PREFIX_TO_PERMISSION.put("/api/authorizations", "authorization:list");
        PREFIX_TO_PERMISSION.put("/api/assessments", "assessment:list");
        PREFIX_TO_PERMISSION.put("/api/assessment-templates", "assessment:list");
        PREFIX_TO_PERMISSION.put("/api/schedules", "schedule:list");
        PREFIX_TO_PERMISSION.put("/api/schedule-templates", "schedule:list");
        PREFIX_TO_PERMISSION.put("/api/users", "user:list");
        PREFIX_TO_PERMISSION.put("/api/roles", "role:list");
        PREFIX_TO_PERMISSION.put("/api/depts", "dept:list");
        PREFIX_TO_PERMISSION.put("/api/dicts", "dict:list");
        // 权限管理没有独立页面，挂在角色管理页（角色页的权限分配树用它）
        PREFIX_TO_PERMISSION.put("/api/permissions", "role:list");
        PREFIX_TO_PERMISSION.put("/api/oper-logs", "log:list");
        PREFIX_TO_PERMISSION.put("/api/mdts", "mdt:list");
        PREFIX_TO_PERMISSION.put("/api/quality-inspections", "quality:list");
        PREFIX_TO_PERMISSION.put("/api/quality-templates", "quality:list");
        PREFIX_TO_PERMISSION.put("/api/new-techs", "newtech:list");
        PREFIX_TO_PERMISSION.put("/api/statistics", "statistics:list");
        PREFIX_TO_PERMISSION.put("/api/workloads", "statistics:list");
        PREFIX_TO_PERMISSION.put("/api/quality-indicators", "statistics:list");
        PREFIX_TO_PERMISSION.put("/api/personnel-changes", "personnel-change:list");
        PREFIX_TO_PERMISSION.put("/api/radiation-personnel", "radiation:list");
        PREFIX_TO_PERMISSION.put("/api/radiation-qualifications", "radiation:list");
        PREFIX_TO_PERMISSION.put("/api/radiation-trainings", "radiation:list");
        PREFIX_TO_PERMISSION.put("/api/radiation-health", "radiation:list");
        PREFIX_TO_PERMISSION.put("/api/committees", "committee:list");
        PREFIX_TO_PERMISSION.put("/api/committee-meetings", "committee:list");
        PREFIX_TO_PERMISSION.put("/api/committee-resolutions", "committee:list");
        PREFIX_TO_PERMISSION.put("/api/documents", "document:list");
        PREFIX_TO_PERMISSION.put("/api/policies", "policy:list");
        PREFIX_TO_PERMISSION.put("/api/policy-readings", "policy:list");
        PREFIX_TO_PERMISSION.put("/api/ethics-profiles", "ethics:list");
        PREFIX_TO_PERMISSION.put("/api/ethics-templates", "ethics:list");
        PREFIX_TO_PERMISSION.put("/api/ethics-evaluations", "ethics:list");
        PREFIX_TO_PERMISSION.put("/api/ethics-complaints", "ethics:list");
        PREFIX_TO_PERMISSION.put("/api/ethics-commendations", "ethics:list");
        PREFIX_TO_PERMISSION.put("/api/core-systems", "core-system:list");
        PREFIX_TO_PERMISSION.put("/api/core-system-readings", "core-system:list");
        PREFIX_TO_PERMISSION.put("/api/rectification-summaries", "rectification:list");
        PREFIX_TO_PERMISSION.put("/api/medical-techs", "tech-grading:list");
        PREFIX_TO_PERMISSION.put("/api/tech-admissions", "tech-grading:list");
        PREFIX_TO_PERMISSION.put("/api/tech-authorizations", "tech-grading:list");
        PREFIX_TO_PERMISSION.put("/api/inspection-items", "inspection:list");
        PREFIX_TO_PERMISSION.put("/api/inspection-standards", "inspection:list");
        PREFIX_TO_PERMISSION.put("/api/inspection-template-items", "inspection:list");
        PREFIX_TO_PERMISSION.put("/api/inspection-categories", "inspection-category:list");
        PREFIX_TO_PERMISSION.put("/api/template-library", "template-library:list");
        PREFIX_TO_PERMISSION.put("/api/grading-standards", "med-grading:list");
        PREFIX_TO_PERMISSION.put("/api/grading-assessments", "med-grading:list");
        PREFIX_TO_PERMISSION.put("/api/module-completions", "completion:list");
        PREFIX_TO_PERMISSION.put("/api/system-settings", "system-settings:list");
        PREFIX_TO_PERMISSION.put("/api/sys-params", "system-settings:list");
        PREFIX_TO_PERMISSION.put("/api/sys-backups", "system-settings:list");
        PREFIX_TO_PERMISSION.put("/api/sys-interfaces", "system-settings:list");
        PREFIX_TO_PERMISSION.put("/api/sys-notice-settings", "system-settings:list");
        PREFIX_TO_PERMISSION.put("/api/sys-knowledge", "system-settings:list");
        PREFIX_TO_PERMISSION.put("/api/notifications", "notification:list");
        PREFIX_TO_PERMISSION.put("/api/site-messages", "notification:list");
        PREFIX_TO_PERMISSION.put("/api/todo-reminders", "notification:list");
        PREFIX_TO_PERMISSION.put("/api/push-records", "notification:list");
        PREFIX_TO_PERMISSION.put("/api/sms-records", "notification:list");
        PREFIX_TO_PERMISSION.put("/api/briefings", "briefing:list");
        PREFIX_TO_PERMISSION.put("/api/briefing-templates", "briefing:list");
        PREFIX_TO_PERMISSION.put("/api/mobile", "mobile:list");
        PREFIX_TO_PERMISSION.put("/api/mobile-devices", "mobile:list");
        PREFIX_TO_PERMISSION.put("/api/mobile-approvals", "mobile:list");
    }

    /**
     * 解析某请求所需的权限编码。
     *
     * @param method HTTP 方法（如 GET / POST）
     * @param path   请求路径（如 /api/leaves/stats）
     * @return null：仅需登录态；权限编码：需要该权限；{@link #DENY}：未登记映射，默认拒绝
     */
    public static String requiredPermission(String method, String path) {
        if (path == null) {
            return DENY;
        }
        // 1. 鉴权接口本身：仅需登录态（/api/auth/login 已在拦截器注册时放行）
        if (path.equals("/api/auth") || path.startsWith("/api/auth/")) {
            return null;
        }
        // 2. 跨页面共用的只读参照数据：仅需登录态
        if ("GET".equalsIgnoreCase(method) && "/api/depts/enabled".equals(path)) {
            return null;
        }
        if ("GET".equalsIgnoreCase(method) && isDictCodeLookup(path)) {
            return null;
        }
        // 3. 按 Controller 前缀映射（段边界匹配）
        for (Map.Entry<String, String> entry : PREFIX_TO_PERMISSION.entrySet()) {
            String prefix = entry.getKey();
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return entry.getValue();
            }
        }
        // 4. 未登记的接口：默认拒绝
        return DENY;
    }

    /** 是否为字典参照查询：GET /api/dicts/{dictCode}（单段编码，且不是管理端点）。 */
    private static boolean isDictCodeLookup(String path) {
        if (!path.startsWith("/api/dicts/")) {
            return false;
        }
        String rest = path.substring("/api/dicts/".length());
        // 只能是一段；带斜杠的路径不是按编码查询
        if (rest.isEmpty() || rest.contains("/")) {
            return false;
        }
        return !DICT_MANAGEMENT_SEGMENTS.contains(rest);
    }
}
