package com.yunzhongyi.medical.rectification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.rectification.dto.GenerateRequest;
import com.yunzhongyi.medical.rectification.dto.IssueSummaryQuery;
import com.yunzhongyi.medical.rectification.dto.OverdueQuery;
import com.yunzhongyi.medical.rectification.dto.RectificationSummaryQuery;
import com.yunzhongyi.medical.rectification.entity.RectificationSummary;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 检查整改汇总服务：汇总报告 CRUD + 按统计周期生成汇总 +
 * 检查问题/整改任务实时汇总统计（按科室/按类型/按时间、整改完成率、逾期统计）
 * + 逾期整改与问题汇总分页查询 + Excel 导入导出
 */
public interface RectificationSummaryService extends IService<RectificationSummary> {

    /** 汇总报告分页 + 多条件组合查询 */
    PageResult<RectificationSummary> pageQuery(RectificationSummaryQuery query);

    /** 按查询条件查全部汇总报告（导出用） */
    List<RectificationSummary> listByQuery(RectificationSummaryQuery query);

    /** 详情 */
    RectificationSummary detail(Long id);

    /**
     * 实时汇总统计（直接读质量检查的问题与整改任务，不依赖已生成的汇总报告）：
     * 问题总数、按严重程度分组、整改任务数、已复查通过数、整改完成率、
     * 逾期整改数、检查计划/记录数、平均分、按科室/按类型/按月汇总
     */
    Map<String, Object> stats();

    /** 按责任科室汇总统计（问题数、按严重程度分组、整改数、整改完成率、逾期数） */
    List<Map<String, Object>> statsByDept();

    /** 按检查类型汇总统计 */
    List<Map<String, Object>> statsByType();

    /** 按月汇总统计（按问题发现月份，月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 逾期整改任务分页查询（带问题描述、严重程度、责任科室） */
    PageResult<Map<String, Object>> pageOverdue(OverdueQuery query);

    /** 检查问题汇总分页查询（带整改任务状态、整改期限与逾期标记） */
    PageResult<Map<String, Object>> pageIssueSummary(IssueSummaryQuery query);

    /**
     * 生成汇总报告：按统计周期 + 汇总范围，从质量检查数据实时聚合生成；
     * 同一（统计周期、汇总范围、科室、检查类型）已有记录时重新生成（原地更新）
     */
    RectificationSummary generate(GenerateRequest request);

    /** 导出汇总报告 Excel（按当前查询条件） */
    void exportExcel(RectificationSummaryQuery query, OutputStream out);

    /** 批量导入汇总报告 Excel（同一周期同一范围同一科室/类型重复跳过），返回导入条数 */
    int importExcel(InputStream in);
}
