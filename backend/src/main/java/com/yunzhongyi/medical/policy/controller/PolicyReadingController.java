package com.yunzhongyi.medical.policy.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.policy.dto.PolicyReadingQuery;
import com.yunzhongyi.medical.policy.entity.PolicyReading;
import com.yunzhongyi.medical.policy.service.PolicyReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 政策法规查阅与学习记录接口：登记查阅学习、标记已学习、按法规查询、删除
 */
@RestController
@RequestMapping("/api/policy-readings")
@RequiredArgsConstructor
public class PolicyReadingController {

    private final PolicyReadingService policyReadingService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(PolicyReadingQuery query) {
        return Result.ok(policyReadingService.pageQuery(query));
    }

    /** 查某部法规的全部查阅学习记录（按查阅日期降序） */
    @GetMapping("/by-policy/{policyId}")
    public Result<List<PolicyReading>> listByPolicy(@PathVariable Long policyId) {
        return Result.ok(policyReadingService.listByPolicy(policyId));
    }

    /** 登记查阅学习：只有已发布的法规才能登记；同一人同一法规同一天只登记一次 */
    @PostMapping
    public Result<PolicyReading> create(@Valid @RequestBody PolicyReading reading) {
        try {
            policyReadingService.create(reading);
            return Result.ok(reading);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 标记已学习：已查阅 → 已学习 */
    @PostMapping("/{id}/learned")
    public Result<PolicyReading> markLearned(@PathVariable Long id) {
        try {
            return Result.ok(policyReadingService.markLearned(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除一条查阅学习记录 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            policyReadingService.deleteReading(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            policyReadingService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
