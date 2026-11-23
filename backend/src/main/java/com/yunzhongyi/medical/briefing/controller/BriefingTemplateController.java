package com.yunzhongyi.medical.briefing.controller;

import com.yunzhongyi.medical.briefing.dto.BriefingTemplateQuery;
import com.yunzhongyi.medical.briefing.entity.BriefingTemplate;
import com.yunzhongyi.medical.briefing.service.BriefingTemplateService;
import com.yunzhongyi.medical.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 简报模板管理接口：模板查询/增删改、启用/停用、启用模板下拉。
 */
@RestController
@RequestMapping("/api/briefing-templates")
@RequiredArgsConstructor
public class BriefingTemplateController {

    private final BriefingTemplateService briefingTemplateService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(BriefingTemplateQuery query) {
        return Result.ok(briefingTemplateService.pageQuery(query));
    }

    /** 全部启用的模板（新建/自动生成简报时下拉用） */
    @GetMapping("/enabled")
    public Result<List<BriefingTemplate>> enabled() {
        return Result.ok(briefingTemplateService.enabledList());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<BriefingTemplate> get(@PathVariable Long id) {
        try {
            return Result.ok(briefingTemplateService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增模板（默认启用、默认月报分类） */
    @PostMapping
    public Result<BriefingTemplate> create(@Valid @RequestBody BriefingTemplate template) {
        try {
            return Result.ok(briefingTemplateService.create(template));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改模板（启用状态不在此改，走启用/停用接口） */
    @PutMapping("/{id}")
    public Result<BriefingTemplate> update(@PathVariable Long id,
                                           @Valid @RequestBody BriefingTemplate template) {
        try {
            template.setId(id);
            return Result.ok(briefingTemplateService.update(template));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除模板（已被简报引用的模板不允许删除） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            briefingTemplateService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 启用模板 */
    @PostMapping("/{id}/enable")
    public Result<BriefingTemplate> enable(@PathVariable Long id) {
        try {
            return Result.ok(briefingTemplateService.enable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 停用模板（停用的模板不能用来新建/自动生成简报） */
    @PostMapping("/{id}/disable")
    public Result<BriefingTemplate> disable(@PathVariable Long id) {
        try {
            return Result.ok(briefingTemplateService.disable(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
