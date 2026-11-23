package com.yunzhongyi.medical.mobile.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.mobile.dto.MobileApprovalLogQuery;
import com.yunzhongyi.medical.mobile.dto.MobileApproveRequest;
import com.yunzhongyi.medical.mobile.entity.MobileApprovalLog;
import com.yunzhongyi.medical.mobile.service.MobileApprovalService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 移动端审批接口：待审批事项统一聚合（只读）与移动端审批通过 / 驳回。
 * <p>
 * 审批本身复用各业务模块已有的审批逻辑；业务模块返回的错误（如“已结束，
 * 不能再审批”）原样返回 400，同时记一条 FAIL 操作日志。
 */
@RestController
@RequestMapping("/api/mobile-approvals")
@RequiredArgsConstructor
public class MobileApprovalController {

    private final MobileApprovalService mobileApprovalService;

    /** 待审批事项聚合列表（可按业务模块 / 关键字过滤） */
    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(required = false) String moduleType,
                             @RequestParam(required = false) String keyword) {
        try {
            return Result.ok(mobileApprovalService.pendingList(moduleType, keyword));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 待审批计数：总数与按业务模块分组 */
    @GetMapping("/pending/count")
    public Result<Map<String, Object>> pendingCount() {
        return Result.ok(mobileApprovalService.pendingCount());
    }

    /** 移动端审批通过：委托业务模块审批 */
    @PostMapping("/{moduleType}/{id}/approve")
    public Result<MobileApprovalLog> approve(@PathVariable String moduleType,
                                             @PathVariable Long id,
                                             @RequestBody(required = false) MobileApproveRequest request) {
        try {
            return Result.ok(mobileApprovalService.approve(moduleType, id,
                    request == null ? new MobileApproveRequest() : request));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 移动端审批驳回：驳回原因必填，委托业务模块审批 */
    @PostMapping("/{moduleType}/{id}/reject")
    public Result<MobileApprovalLog> reject(@PathVariable String moduleType,
                                            @PathVariable Long id,
                                            @RequestBody(required = false) MobileApproveRequest request) {
        try {
            return Result.ok(mobileApprovalService.reject(moduleType, id,
                    request == null ? new MobileApproveRequest() : request));
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 审批操作记录分页查询 */
    @GetMapping("/logs")
    public Result<?> logs(MobileApprovalLogQuery query) {
        return Result.ok(mobileApprovalService.pageQuery(query));
    }

    /** 审批操作记录统计：总数 / 通过 / 驳回 / 成功 / 失败 / 按业务模块分组 */
    @GetMapping("/logs/stats")
    public Result<Map<String, Object>> logStats() {
        return Result.ok(mobileApprovalService.stats());
    }

    /** 导出审批操作记录 Excel（按当前查询条件） */
    @GetMapping("/logs/export")
    public void exportLogs(MobileApprovalLogQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("移动端审批记录.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        mobileApprovalService.exportExcel(query, response.getOutputStream());
    }
}
