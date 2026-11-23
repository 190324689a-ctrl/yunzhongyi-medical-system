package com.yunzhongyi.medical.radiation.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.radiation.dto.RadiationHealthQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationHealth;
import com.yunzhongyi.medical.radiation.service.RadiationHealthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 放射人员健康管理接口：体检记录查询/增删改（含剂量超标过滤）
 */
@RestController
@RequestMapping("/api/radiation-health")
@RequiredArgsConstructor
public class RadiationHealthController {

    private final RadiationHealthService radiationHealthService;

    /** 多条件组合分页查询（含只看剂量超标） */
    @GetMapping
    public Result<?> page(RadiationHealthQuery query) {
        return Result.ok(radiationHealthService.pageQuery(query));
    }

    /** 查某放射人员的全部体检记录（按体检日期降序） */
    @GetMapping("/by-personnel/{personnelId}")
    public Result<List<RadiationHealth>> listByPersonnel(@PathVariable Long personnelId) {
        return Result.ok(radiationHealthService.listByPersonnel(personnelId));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<RadiationHealth> get(@PathVariable Long id) {
        try {
            return Result.ok(radiationHealthService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增体检记录（结论默认合格；剂量超标时后端记日志提醒复查调岗） */
    @PostMapping
    public Result<RadiationHealth> create(@Valid @RequestBody RadiationHealth health) {
        try {
            radiationHealthService.save(health);
            return Result.ok(health);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<RadiationHealth> update(@PathVariable Long id, @Valid @RequestBody RadiationHealth health) {
        try {
            health.setId(id);
            radiationHealthService.updateById(health);
            return Result.ok(health);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        radiationHealthService.removeById(id);
        return Result.ok();
    }
}
