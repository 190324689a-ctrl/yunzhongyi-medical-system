package com.yunzhongyi.medical.quality.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.quality.dto.QualityPlanQuery;
import com.yunzhongyi.medical.quality.dto.QualityRectificationQuery;
import com.yunzhongyi.medical.quality.entity.QualityIssue;
import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityRecord;
import com.yunzhongyi.medical.quality.entity.QualityRectification;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 质量检查管理服务：检查计划 + 检查记录（逐项打分、总分与等级自动算）+
 * 检查问题 + 整改跟踪（含复查）+ 检查报告 + 统计 + Excel 导入导出
 */
public interface QualityService extends IService<QualityPlan> {

    /** 多条件组合分页查询检查计划 */
    PageResult<QualityPlan> pageQuery(QualityPlanQuery query);

    /** 按同样条件查全部计划 */
    List<QualityPlan> listByQuery(QualityPlanQuery query);

    /**
     * 质量检查统计：计划总数 / 按状态分组 / 按检查类型分组 / 按科室汇总 /
     * 按月趋势 / 检查记录数 / 平均分 / 问题数 / 整改完成率 / 逾期整改数
     */
    Map<String, Object> stats();

    /** 按科室汇总统计（同 stats 中的 byDept，单独提供给前端表格） */
    List<Map<String, Object>> statsByDept();

    /** 按月趋势统计（计划月份 → 计划数 / 已完成数，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 详情：计划 + 全部检查记录 + 全部问题 + 全部整改任务 */
    Map<String, Object> detail(Long planId);

    /** 开始检查：待执行 → 进行中 */
    QualityPlan start(Long id);

    /** 完成检查：进行中 → 已完成（需要至少一条检查记录） */
    QualityPlan complete(Long id);

    /** 取消计划：待执行/进行中 → 已取消 */
    QualityPlan cancel(Long id);

    /** 查某计划的全部检查记录（按检查日期升序） */
    List<QualityRecord> listRecords(Long planId);

    /** 添加检查记录：只有进行中的计划才能记录；逐项打分，总分与等级自动算 */
    QualityRecord addRecord(Long planId, QualityRecord record);

    /** 删除一条检查记录（同时删掉该记录下的问题与整改任务） */
    boolean removeRecord(Long planId, Long recordId);

    /** 查某记录的全部问题（按发现日期升序） */
    List<QualityIssue> listIssues(Long recordId);

    /** 添加问题：记录须存在；严重程度须合法 */
    QualityIssue addIssue(Long recordId, QualityIssue issue);

    /** 删除一条问题（同时删掉该问题下的整改任务） */
    boolean removeIssue(Long recordId, Long issueId);

    /** 查某计划的全部整改任务（按整改期限升序，逾期标记动态算出） */
    List<QualityRectification> listRectifications(Long planId);

    /** 整改任务分页查询（责任人/状态/只看逾期/计划） */
    PageResult<QualityRectification> pageRectifications(QualityRectificationQuery query);

    /** 针对一个问题发整改任务：一个问题只发一个 */
    QualityRectification addRectification(Long issueId, QualityRectification rectification);

    /** 提交整改：待整改 → 已整改（待复查），整改完成日期默认为当天 */
    QualityRectification submitRectification(Long rectificationId);

    /** 复查通过：已整改 → 已复查通过 */
    QualityRectification reviewPass(Long rectificationId, String reviewer, String opinion);

    /** 复查打回：已整改 → 待整改，可重新提交 */
    QualityRectification reviewReject(Long rectificationId, String reviewer, String opinion);

    /** 检查报告：按计划生成报告摘要（总分、等级、问题数、整改完成率） */
    Map<String, Object> report(Long planId);

    /** 导出 Excel：检查记录（按当前查询条件对应的计划） */
    void exportExcel(QualityPlanQuery query, OutputStream out);

    /** 批量导入 Excel（检查计划），返回导入结果：导入数/跳过数/说明 */
    Map<String, Object> importExcel(InputStream in);
}
