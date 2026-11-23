package com.yunzhongyi.medical.radiation.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.radiation.dto.RadiationTrainingQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationTraining;
import com.yunzhongyi.medical.radiation.service.RadiationTrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 放射人员培训管理接口：培训记录查询/增删改
 */
@RestController
@RequestMapping("/api/radiation-trainings")
@RequiredArgsConstructor
public class RadiationTrainingController {

    private final RadiationTrainingService radiationTrainingService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(RadiationTrainingQuery query) {
        return Result.ok(radiationTrainingService.pageQuery(query));
    }

    /** 查某放射人员的全部培训记录（按培训日期降序） */
    @GetMapping("/by-personnel/{personnelId}")
    public Result<List<RadiationTraining>> listByPersonnel(@PathVariable Long personnelId) {
        return Result.ok(radiationTrainingService.listByPersonnel(personnelId));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<RadiationTraining> get(@PathVariable Long id) {
        try {
            return Result.ok(radiationTrainingService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增培训记录（结果默认合格） */
    @PostMapping
    public Result<RadiationTraining> create(@Valid @RequestBody RadiationTraining training) {
        try {
            radiationTrainingService.save(training);
            return Result.ok(training);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<RadiationTraining> update(@PathVariable Long id, @Valid @RequestBody RadiationTraining training) {
        try {
            training.setId(id);
            radiationTrainingService.updateById(training);
            return Result.ok(training);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        radiationTrainingService.removeById(id);
        return Result.ok();
    }
}
