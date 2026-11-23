package com.yunzhongyi.medical.committee.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.committee.dto.CommitteeResolutionQuery;
import com.yunzhongyi.medical.committee.entity.CommitteeResolution;
import com.yunzhongyi.medical.committee.service.CommitteeResolutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 委员会决议接口：决议查询/增删改、执行流转（开始/完成/取消）
 */
@RestController
@RequestMapping("/api/committee-resolutions")
@RequiredArgsConstructor
public class CommitteeResolutionController {

    private final CommitteeResolutionService resolutionService;

    /** 多条件组合分页查询（含只看逾期） */
    @GetMapping
    public Result<?> page(CommitteeResolutionQuery query) {
        return Result.ok(resolutionService.pageQuery(query));
    }

    /** 查某会议的全部决议（按决议期限升序） */
    @GetMapping("/by-meeting/{meetingId}")
    public Result<List<CommitteeResolution>> listByMeeting(@PathVariable Long meetingId) {
        return Result.ok(resolutionService.listByMeeting(meetingId));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<CommitteeResolution> get(@PathVariable Long id) {
        try {
            return Result.ok(resolutionService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增决议（默认待执行） */
    @PostMapping
    public Result<CommitteeResolution> create(@Valid @RequestBody CommitteeResolution resolution) {
        try {
            resolutionService.save(resolution);
            return Result.ok(resolution);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有未结束的决议能修改） */
    @PutMapping("/{id}")
    public Result<CommitteeResolution> update(@PathVariable Long id, @Valid @RequestBody CommitteeResolution resolution) {
        try {
            resolution.setId(id);
            resolutionService.updateById(resolution);
            return Result.ok(resolution);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        resolutionService.removeById(id);
        return Result.ok();
    }

    /** 开始执行：待执行 → 执行中 */
    @PostMapping("/{id}/start")
    public Result<CommitteeResolution> start(@PathVariable Long id) {
        try {
            return Result.ok(resolutionService.start(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 完成：执行中 → 已完成（完成日期默认为当天） */
    @PostMapping("/{id}/complete")
    public Result<CommitteeResolution> complete(@PathVariable Long id) {
        try {
            return Result.ok(resolutionService.complete(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 取消：待执行/执行中 → 已取消 */
    @PostMapping("/{id}/cancel")
    public Result<CommitteeResolution> cancel(@PathVariable Long id) {
        try {
            return Result.ok(resolutionService.cancel(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
