package com.yunzhongyi.medical.coresystem.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery;
import com.yunzhongyi.medical.coresystem.entity.CoreSystemReading;
import com.yunzhongyi.medical.coresystem.service.CoreSystemReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 核心制度查阅与学习记录接口：登记查阅学习、标记已学习、按制度查询、删除
 */
@RestController
@RequestMapping("/api/core-system-readings")
@RequiredArgsConstructor
public class CoreSystemReadingController {

    private final CoreSystemReadingService coreSystemReadingService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(CoreSystemReadingQuery query) {
        return Result.ok(coreSystemReadingService.pageQuery(query));
    }

    /** 查某项制度的全部查阅学习记录（按查阅日期降序） */
    @GetMapping("/by-core-system/{coreSystemId}")
    public Result<List<CoreSystemReading>> listByCoreSystem(@PathVariable Long coreSystemId) {
        return Result.ok(coreSystemReadingService.listByCoreSystem(coreSystemId));
    }

    /** 登记查阅学习：只有已发布的制度才能登记；同一人同一制度同一天只登记一次 */
    @PostMapping
    public Result<CoreSystemReading> create(@Valid @RequestBody CoreSystemReading reading) {
        try {
            coreSystemReadingService.create(reading);
            return Result.ok(reading);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 标记已学习：已查阅 → 已学习 */
    @PostMapping("/{id}/learned")
    public Result<CoreSystemReading> markLearned(@PathVariable Long id) {
        try {
            return Result.ok(coreSystemReadingService.markLearned(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除一条查阅学习记录 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            coreSystemReadingService.deleteReading(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            coreSystemReadingService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
