package com.yunzhongyi.medical.schedule.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.schedule.dto.ScheduleTemplateQuery;
import com.yunzhongyi.medical.schedule.entity.ScheduleTemplate;
import com.yunzhongyi.medical.schedule.service.ScheduleTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排班模板管理接口：自定义排班模板、班次组合校验、启用/停用
 */
@RestController
@RequestMapping("/api/schedule-templates")
@RequiredArgsConstructor
public class ScheduleTemplateController {

    private final ScheduleTemplateService templateService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(ScheduleTemplateQuery query) {
        return Result.ok(templateService.pageQuery(query));
    }

    /** 全部启用的模板（批量生成排班时下拉用） */
    @GetMapping("/enabled")
    public Result<List<ScheduleTemplate>> enabled() {
        return Result.ok(templateService.listEnabled());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<ScheduleTemplate> get(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    /** 新增（班次组合必须合法：星期 1-7 不重复、班次编码合法） */
    @PostMapping
    public Result<ScheduleTemplate> create(@Valid @RequestBody ScheduleTemplate template) {
        try {
            templateService.save(template);
            return Result.ok(template);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<ScheduleTemplate> update(@PathVariable Long id, @Valid @RequestBody ScheduleTemplate template) {
        try {
            template.setId(id);
            templateService.updateById(template);
            return Result.ok(template);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        templateService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        templateService.removeByIds(ids);
        return Result.ok(java.util.Map.of("deleted", ids.size()));
    }
}
