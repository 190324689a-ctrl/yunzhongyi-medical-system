package com.yunzhongyi.medical.ethics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.ApproveRequest;
import com.yunzhongyi.medical.ethics.dto.EthicsEvaluationQuery;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsEvaluation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/** 医德考评服务：评分录入（总分与等级自动计算）+ 一级审批 + 评分统计 + Excel 批量导入导出 */
public interface EthicsEvaluationService extends IService<EthicsEvaluation> {

    PageResult<EthicsEvaluation> pageQuery(EthicsEvaluationQuery query);

    List<EthicsEvaluation> listByQuery(EthicsEvaluationQuery query);

    /** 评分统计：总数、平均分、优秀人数与优秀率、按等级分组、按科室汇总 */
    Map<String, Object> stats();

    /** 按科室汇总统计（人数、平均分、优秀率） */
    List<Map<String, Object>> statsByDept();

    EthicsEvaluation detail(Long id);

    /** 一级审批通过：只有待审批的考评能通过，审批记录留痕；通过时同步医德档案的最近考评结果 */
    EthicsEvaluation approve(Long id, ApproveRequest request);

    /** 一级审批驳回：只有待审批的考评能驳回，审批记录留痕；已结束的考评不能再审批 */
    EthicsEvaluation reject(Long id, RejectRequest request);

    void exportExcel(EthicsEvaluationQuery query, OutputStream out);

    int importExcel(InputStream in);
}
