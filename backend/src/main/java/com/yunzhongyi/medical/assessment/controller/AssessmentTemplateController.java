package com.yunzhongyi.medical.assessment.controller;

import com.yunzhongyi.medical.assessment.dto.AssessmentTemplateQuery;
import com.yunzhongyi.medical.assessment.entity.AssessmentTemplate;
import com.yunzhongyi.medical.assessment.service.AssessmentTemplateService;
import com.yunzhongyi.medical.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考核模板管理接口：自定义考核模板、评分项权重校验、启用/停用
 */
@RestController
@RequestMapping("/api/assessment-templates")
@RequiredArgsConstructor
public class AssessmentTemplateController {

    private final AssessmentTemplateService templateService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(AssessmentTemplateQuery query) {
        return Result.ok(templateService.pageQuery(query));
    }

    /** 全部启用的模板（新建考核记录时下拉用） */
    @GetMapping("/enabled")
    public Result<List<AssessmentTemplate>> enabled() {
        return Result.ok(templateService.listEnabled());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<AssessmentTemplate> get(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    /** 新增（评分项权重之和必须为 100） */
    @PostMapping
    public Result<AssessmentTemplate> create(@Valid @RequestBody AssessmentTemplate template) {
        try {
            templateService.save(template);
            return Result.ok(template);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<AssessmentTemplate> update(@PathVariable Long id, @Valid @RequestBody AssessmentTemplate template) {
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
