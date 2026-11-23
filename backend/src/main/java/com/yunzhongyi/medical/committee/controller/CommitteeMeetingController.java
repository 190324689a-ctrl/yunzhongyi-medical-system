package com.yunzhongyi.medical.committee.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.committee.dto.CommitteeMeetingQuery;
import com.yunzhongyi.medical.committee.entity.CommitteeMeeting;
import com.yunzhongyi.medical.committee.service.CommitteeMeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 委员会会议记录接口：会议查询/增删改；只有在任的委员会才能新增会议
 */
@RestController
@RequestMapping("/api/committee-meetings")
@RequiredArgsConstructor
public class CommitteeMeetingController {

    private final CommitteeMeetingService meetingService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(CommitteeMeetingQuery query) {
        return Result.ok(meetingService.pageQuery(query));
    }

    /** 查某委员会的全部会议（按会议日期降序） */
    @GetMapping("/by-committee/{committeeId}")
    public Result<List<CommitteeMeeting>> listByCommittee(@PathVariable Long committeeId) {
        return Result.ok(meetingService.listByCommittee(committeeId));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<CommitteeMeeting> get(@PathVariable Long id) {
        try {
            return Result.ok(meetingService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增会议记录（只有在任的委员会才能新增） */
    @PostMapping
    public Result<CommitteeMeeting> create(@Valid @RequestBody CommitteeMeeting meeting) {
        try {
            meetingService.save(meeting);
            return Result.ok(meeting);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<CommitteeMeeting> update(@PathVariable Long id, @Valid @RequestBody CommitteeMeeting meeting) {
        try {
            meeting.setId(id);
            meetingService.updateById(meeting);
            return Result.ok(meeting);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（同时删除该会议的决议） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        meetingService.removeById(id);
        return Result.ok();
    }
}
