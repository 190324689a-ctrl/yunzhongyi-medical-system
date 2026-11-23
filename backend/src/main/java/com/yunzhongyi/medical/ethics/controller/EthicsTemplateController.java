package com.yunzhongyi.medical.ethics.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.ethics.dto.EthicsTemplateQuery;
import com.yunzhongyi.medical.ethics.entity.EthicsTemplate;
import com.yunzhongyi.medical.ethics.service.EthicsTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医德考评模板接口：自定义考评模板、评分项权重校验、启用/停用
 */
@RestController
@RequestMapping("/api/ethics-templates")
@RequiredArgsConstructor
public class EthicsTemplateController {

    private final EthicsTemplateService templateService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(EthicsTemplateQuery query) {
        return Result.ok(templateService.pageQuery(query));
    }

    /** 全部启用的模板（新建医德考评记录时下拉用） */
    @GetMapping("/enabled")
    public Result<List<EthicsTemplate>> enabled() {
        return Result.ok(templateService.listEnabled());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<EthicsTemplate> get(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    /** 新增（评分项权重之和必须为 100） */
    @PostMapping
    public Result<EthicsTemplate> create(@Valid @RequestBody EthicsTemplate template) {
        try {
            templateService.save(template);
            return Result.ok(template);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<EthicsTemplate> update(@PathVariable Long id, @Valid @RequestBody EthicsTemplate template) {
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
