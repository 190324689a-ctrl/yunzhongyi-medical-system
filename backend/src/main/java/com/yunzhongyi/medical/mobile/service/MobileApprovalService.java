package com.yunzhongyi.medical.mobile.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.mobile.dto.MobileApprovalLogQuery;
import com.yunzhongyi.medical.mobile.dto.MobileApproveRequest;
import com.yunzhongyi.medical.mobile.dto.MobilePendingItem;
import com.yunzhongyi.medical.mobile.entity.MobileApprovalLog;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 移动端审批服务：待审批事项统一聚合（只读）与移动端审批流转。
 * <p>
 * 审批本身复用各业务模块已有的审批逻辑，本服务只做聚合查询、
 * 委托调用与操作留痕，不另造审批流。
 */
public interface MobileApprovalService extends IService<MobileApprovalLog> {

    /** 待审批事项聚合列表（可按业务模块过滤；按提交时间倒序） */
    List<MobilePendingItem> pendingList(String moduleType, String keyword);

    /** 待审批计数：总数与按业务模块分组 */
    Map<String, Object> pendingCount();

    /** 移动端审批通过：委托业务模块审批，成功与失败都记操作日志 */
    MobileApprovalLog approve(String moduleType, Long relatedId, MobileApproveRequest request);

    /** 移动端审批驳回：驳回原因必填，委托业务模块审批，成功与失败都记操作日志 */
    MobileApprovalLog reject(String moduleType, Long relatedId, MobileApproveRequest request);

    PageResult<MobileApprovalLog> pageQuery(MobileApprovalLogQuery query);

    Map<String, Object> stats();

    void exportExcel(MobileApprovalLogQuery query, OutputStream out);
}
