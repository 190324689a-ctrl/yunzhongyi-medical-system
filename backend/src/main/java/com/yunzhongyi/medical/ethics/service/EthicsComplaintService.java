package com.yunzhongyi.medical.ethics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.EthicsComplaintQuery;
import com.yunzhongyi.medical.ethics.dto.FeedbackRequest;
import com.yunzhongyi.medical.ethics.dto.HandleRequest;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsComplaint;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/** 医德医风投诉服务：受理 / 调查 / 处理 / 反馈流转 + 统计 + Excel 批量导入导出 */
public interface EthicsComplaintService extends IService<EthicsComplaint> {

    PageResult<EthicsComplaint> pageQuery(EthicsComplaintQuery query);

    List<EthicsComplaint> listByQuery(EthicsComplaintQuery query);

    /** 投诉统计：总数、按流转状态分组、按投诉类型分组、按科室汇总、逾期数、反馈率 */
    Map<String, Object> stats();

    /** 按科室汇总统计（投诉数、待受理、调查中、已反馈、逾期） */
    List<Map<String, Object>> statsByDept();

    EthicsComplaint detail(Long id);

    /** 受理：待受理 → 调查中 */
    EthicsComplaint accept(Long id, String handler);

    /** 处理：调查中 → 已处理，处理结果必填 */
    EthicsComplaint handle(Long id, HandleRequest request);

    /** 反馈：已处理 → 已反馈，反馈内容必填 */
    EthicsComplaint feedback(Long id, FeedbackRequest request);

    /** 不予受理：待受理 → 不予受理，原因必填 */
    EthicsComplaint reject(Long id, RejectRequest request);

    void exportExcel(EthicsComplaintQuery query, OutputStream out);

    int importExcel(InputStream in);
}
