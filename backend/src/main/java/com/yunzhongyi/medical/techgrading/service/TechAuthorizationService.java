package com.yunzhongyi.medical.techgrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.techgrading.dto.ApproveRequest;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.dto.TechAuthorizationQuery;
import com.yunzhongyi.medical.techgrading.entity.TechAuthorization;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 医师医疗技术授权管理服务
 */
public interface TechAuthorizationService extends IService<TechAuthorization> {

    /** 多条件组合分页查询 */
    PageResult<TechAuthorization> pageQuery(TechAuthorizationQuery query);

    /** 按同样条件查全部 */
    List<TechAuthorization> listByQuery(TechAuthorizationQuery query);

    /**
     * 授权统计：总数 / 待审批 / 已通过 / 已驳回 / 已注销 / 按授权级别分组 /
     * 按科室汇总 / 90 天内到期数 / 已过期数
     */
    Map<String, Object> stats();

    /** 按科室汇总统计（授权数、已通过数） */
    List<Map<String, Object>> statsByDept();

    /** 查某项技术的全部授权（按授权开始日期降序） */
    List<TechAuthorization> listByTech(Long techId);

    /** 查某位医师的全部授权（按授权开始日期降序） */
    List<TechAuthorization> listByPhysician(Long physicianId);

    /** 详情 */
    TechAuthorization detail(Long id);

    /**
     * 审批通过（一级审批，医务科）：只有待审批的授权能通过。审批人必填，
     * 审批记录留痕；通过时再次校验技术仍已准入且非禁止类、授权级别不高于
     * 技术分级（防止审批期间档案被停用或改级）。
     */
    TechAuthorization approve(Long id, ApproveRequest request);

    /** 审批驳回（一级审批，医务科）：只有待审批的授权能驳回，驳回原因必填 */
    TechAuthorization reject(Long id, RejectRequest request);

    /** 注销：只有已通过的授权能注销，注销原因必填，审批记录留痕 */
    TechAuthorization revoke(Long id, RejectRequest request);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(TechAuthorizationQuery query, OutputStream out);

    /** 批量导入 Excel（导入的均为待审批），返回成功导入条数 */
    int importExcel(InputStream in);
}
