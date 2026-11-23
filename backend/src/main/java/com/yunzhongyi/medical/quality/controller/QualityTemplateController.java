package com.yunzhongyi.medical.quality.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.quality.dto.QualityTemplateQuery;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.service.QualityTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 质量检查模板接口：模板查询/增删改、启用/停用。
 * 模板保存时校验检查项权重之和必须为 100。
 */
@RestController
@RequestMapping("/api/quality-templates")
@RequiredArgsConstructor
public class QualityTemplateController {

    private final QualityTemplateService templateService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(QualityTemplateQuery query) {
        return Result.ok(templateService.pageQuery(query));
    }

    /** 全部启用的模板（新建检查计划时下拉用） */
    @GetMapping("/enabled")
    public Result<List<QualityTemplate>> listEnabled() {
        return Result.ok(templateService.listEnabled());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<QualityTemplate> get(@PathVariable Long id) {
        QualityTemplate t = templateService.getById(id);
        if (t == null) {
            return Result.fail(400, "检查模板不存在：" + id);
        }
        return Result.ok(t);
    }

    /** 新增模板（检查项权重之和必须为 100，默认启用） */
    @PostMapping
    public Result<QualityTemplate> create(@Valid @RequestBody QualityTemplate template) {
        try {
            templateService.save(template);
            return Result.ok(template);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改模板（检查项权重之和必须为 100） */
    @PutMapping("/{id}")
    public Result<QualityTemplate> update(@PathVariable Long id, @Valid @RequestBody QualityTemplate template) {
        try {
            template.setId(id);
            templateService.updateById(template);
            return Result.ok(template);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除模板（已有检查计划引用时拒绝，返回 400） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            templateService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用模板 */
    @PostMapping("/{id}/enable")
    public Result<QualityTemplate> enable(@PathVariable Long id) {
        try {
            return Result.ok(templateService.enable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用模板（停用的模板不能用于新建检查计划） */
    @PostMapping("/{id}/disable")
    public Result<QualityTemplate> disable(@PathVariable Long id) {
        try {
            return Result.ok(templateService.disable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
