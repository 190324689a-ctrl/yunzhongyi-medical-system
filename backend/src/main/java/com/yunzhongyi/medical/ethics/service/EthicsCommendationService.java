package com.yunzhongyi.medical.ethics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.ApproveRequest;
import com.yunzhongyi.medical.ethics.dto.EthicsCommendationQuery;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsCommendation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/** 医德医风表彰服务：表彰登记 + 一级审批 + 统计 + Excel 批量导入导出 */
public interface EthicsCommendationService extends IService<EthicsCommendation> {

    PageResult<EthicsCommendation> pageQuery(EthicsCommendationQuery query);

    List<EthicsCommendation> listByQuery(EthicsCommendationQuery query);

    /** 表彰统计：总数、待审批、已通过、已驳回、按表彰类型分组、按表彰级别分组、按科室汇总 */
    Map<String, Object> stats();

    /** 按科室汇总统计（表彰数、已通过数） */
    List<Map<String, Object>> statsByDept();

    EthicsCommendation detail(Long id);

    /** 一级审批通过：只有待审批的表彰能通过，审批记录留痕 */
    EthicsCommendation approve(Long id, ApproveRequest request);

    /** 一级审批驳回：只有待审批的表彰能驳回，审批记录留痕；已结束的表彰不能再审批 */
    EthicsCommendation reject(Long id, RejectRequest request);

    void exportExcel(EthicsCommendationQuery query, OutputStream out);

    int importExcel(InputStream in);
}
