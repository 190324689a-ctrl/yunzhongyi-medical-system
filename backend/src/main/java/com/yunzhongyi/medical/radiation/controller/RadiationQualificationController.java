package com.yunzhongyi.medical.radiation.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.radiation.dto.RadiationApprovalRequest;
import com.yunzhongyi.medical.radiation.dto.RadiationQualificationQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationQualification;
import com.yunzhongyi.medical.radiation.service.RadiationQualificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 放射人员资质管理接口：资质查询/增删改、一级审核（医务科）
 */
@RestController
@RequestMapping("/api/radiation-qualifications")
@RequiredArgsConstructor
public class RadiationQualificationController {

    private final RadiationQualificationService radiationQualificationService;

    /** 多条件组合分页查询（含只看 90 天内到期） */
    @GetMapping
    public Result<?> page(RadiationQualificationQuery query) {
        return Result.ok(radiationQualificationService.pageQuery(query));
    }

    /** 查某放射人员的全部资质（按有效期升序） */
    @GetMapping("/by-personnel/{personnelId}")
    public Result<List<RadiationQualification>> listByPersonnel(@PathVariable Long personnelId) {
        return Result.ok(radiationQualificationService.listByPersonnel(personnelId));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<RadiationQualification> get(@PathVariable Long id) {
        try {
            return Result.ok(radiationQualificationService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增资质登记（默认待审核） */
    @PostMapping
    public Result<RadiationQualification> create(@Valid @RequestBody RadiationQualification qualification) {
        try {
            radiationQualificationService.save(qualification);
            return Result.ok(qualification);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待审核的资质能修改） */
    @PutMapping("/{id}")
    public Result<RadiationQualification> update(@PathVariable Long id,
                                                 @Valid @RequestBody RadiationQualification qualification) {
        try {
            qualification.setId(id);
            radiationQualificationService.updateById(qualification);
            return Result.ok(qualification);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        radiationQualificationService.removeById(id);
        return Result.ok();
    }

    /** 一级审核通过（医务科）：只有待审核的资质能通过，审核记录留痕 */
    @PostMapping("/{id}/approve")
    public Result<RadiationQualification> approve(@PathVariable Long id,
                                                  @RequestBody(required = false) RadiationApprovalRequest request) {
        String approver = request == null ? null : request.getApprover();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(radiationQualificationService.approve(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一级审核驳回（医务科）：只有待审核的资质能驳回，审核记录留痕 */
    @PostMapping("/{id}/reject")
    public Result<RadiationQualification> reject(@PathVariable Long id,
                                                 @RequestBody(required = false) RadiationApprovalRequest request) {
        String approver = request == null ? null : request.getApprover();
        String opinion = request == null ? null : request.getOpinion();
        try {
            return Result.ok(radiationQualificationService.reject(id, approver, opinion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
