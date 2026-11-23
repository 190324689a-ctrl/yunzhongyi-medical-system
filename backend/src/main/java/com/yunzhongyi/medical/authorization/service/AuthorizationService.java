package com.yunzhongyi.medical.authorization.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.authorization.dto.AuthorizationQuery;
import com.yunzhongyi.medical.authorization.entity.Authorization;
import com.yunzhongyi.medical.common.PageResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 重点授权服务
 */
public interface AuthorizationService extends IService<Authorization> {

    /** 多条件组合分页查询 */
    PageResult<Authorization> pageQuery(AuthorizationQuery query);

    /** 按同样条件查全部 */
    List<Authorization> listByQuery(AuthorizationQuery query);

    /** 统计：总数 / 待审批 / 已通过 / 已驳回 / 按类型分组 */
    Map<String, Object> stats();

    /**
     * 多级审批：通过当前一级。
     * 状态 0（待科主任）通过后变 1（待医务科）；状态 1 通过后变 2（已通过）。
     * 已通过 / 已驳回的申请不能再审批。
     */
    Authorization approve(Long id, String approver, String opinion);

    /**
     * 多级审批：驳回。任一待审批状态（0 或 1）驳回后变 3（已驳回）。
     */
    Authorization reject(Long id, String approver, String opinion);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(AuthorizationQuery query, OutputStream out);

    /** 批量导入 Excel，返回成功导入条数 */
    int importExcel(InputStream in);
}
