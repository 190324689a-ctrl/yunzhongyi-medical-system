package com.yunzhongyi.medical.leave.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.leave.dto.LeaveQuery;
import com.yunzhongyi.medical.leave.entity.LeaveApplication;

import java.util.List;
import java.util.Map;

/**
 * 休假执业服务
 */
public interface LeaveApplicationService extends IService<LeaveApplication> {

    /** 多条件组合分页查询 */
    PageResult<LeaveApplication> pageQuery(LeaveQuery query);

    /** 按同样条件查全部 */
    List<LeaveApplication> listByQuery(LeaveQuery query);

    /** 统计：总数 / 待审批 / 已通过 / 已驳回 / 按类型分组 */
    Map<String, Object> stats();

    /**
     * 多级审批：通过当前一级。
     * 状态 0（待科主任）通过后变 1（待医务科）；状态 1 通过后变 2（已通过）。
     * 已通过 / 已驳回的申请不能再审批。
     */
    LeaveApplication approve(Long id, String approver, String opinion);

    /**
     * 多级审批：驳回。任一待审批状态（0 或 1）驳回后变 3（已驳回）。
     */
    LeaveApplication reject(Long id, String approver, String opinion);
}
