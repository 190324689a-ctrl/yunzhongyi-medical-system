package com.yunzhongyi.medical.leave.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.leave.dto.LeaveQuery;
import com.yunzhongyi.medical.leave.entity.LeaveApplication;
import com.yunzhongyi.medical.leave.service.LeaveApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 休假执业管理接口：在线申请、多级审批、查询、统计
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationService leaveService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(LeaveQuery query) {
        return Result.ok(leaveService.pageQuery(query));
    }

    /** 统计：总数 / 待审批 / 已通过 / 已驳回 / 按类型分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(leaveService.stats());
    }

    /** 详情（打印预览用） */
    @GetMapping("/{id}")
    public Result<LeaveApplication> get(@PathVariable Long id) {
        return Result.ok(leaveService.getById(id));
    }

    /** 在线申请 */
    @PostMapping
    public Result<LeaveApplication> create(@Valid @RequestBody LeaveApplication application) {
        leaveService.save(application);
        return Result.ok(application);
    }

    /** 修改（仅待审批的申请可改，由前端控制；后端不拦截已通过的编辑以便补录） */
    @PutMapping("/{id}")
    public Result<LeaveApplication> update(@PathVariable Long id, @Valid @RequestBody LeaveApplication application) {
        application.setId(id);
        leaveService.updateById(application);
        return Result.ok(application);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        leaveService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        leaveService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 多级审批：通过当前一级（科主任 → 医务科） */
    @PostMapping("/{id}/approve")
    public Result<LeaveApplication> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String approver = body == null ? null : body.get("approver");
        String opinion = body == null ? null : body.get("opinion");
        try {
            return Result.ok(leaveService.approve(id, approver, opinion));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 多级审批：驳回 */
    @PostMapping("/{id}/reject")
    public Result<LeaveApplication> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String approver = body == null ? null : body.get("approver");
        String opinion = body == null ? null : body.get("opinion");
        try {
            return Result.ok(leaveService.reject(id, approver, opinion));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
