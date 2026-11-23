package com.yunzhongyi.medical.techgrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.techgrading.dto.ApproveRequest;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.dto.TechAdmissionQuery;
import com.yunzhongyi.medical.techgrading.entity.TechAdmission;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 医疗技术准入申请管理服务
 */
public interface TechAdmissionService extends IService<TechAdmission> {

    /** 多条件组合分页查询 */
    PageResult<TechAdmission> pageQuery(TechAdmissionQuery query);

    /** 按同样条件查全部 */
    List<TechAdmission> listByQuery(TechAdmissionQuery query);

    /** 准入申请统计：总数 / 待审批 / 已通过 / 已驳回 / 按申请类型分组 */
    Map<String, Object> stats();

    /** 查某项技术的全部准入申请（按申请日期降序） */
    List<TechAdmission> listByTech(Long techId);

    /** 详情 */
    TechAdmission detail(Long id);

    /**
     * 审批通过（一级审批，医务科）：只有待审批的申请能通过。审批人必填，
     * 审批记录留痕；通过时同步医疗技术档案：新技术准入→档案变为已准入、
     * 准入日期填当天（有效期为空时默认 + 3 年）；新增开展科室→科室追加进
     * 档案；变更分级→档案分级改为新分级；延续准入→档案有效期改为新有效期。
     */
    TechAdmission approve(Long id, ApproveRequest request);

    /** 审批驳回（一级审批，医务科）：只有待审批的申请能驳回，驳回原因必填 */
    TechAdmission reject(Long id, RejectRequest request);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(TechAdmissionQuery query, OutputStream out);

    /** 批量导入 Excel（导入的均为待审批），返回成功导入条数 */
    int importExcel(InputStream in);
}
