package com.yunzhongyi.medical.medgrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.medgrading.dto.GradingAssessmentQuery;
import com.yunzhongyi.medical.medgrading.entity.GradingAssessment;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 医疗分级评定服务：科室分级评定的增删改、一级审批、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 22 项）：医疗分级、医疗质量分级管理。
 * 等级由评分按所属标准的分数线自动算：甲级 / 乙级 / 丙级 / 丁级。
 */
public interface GradingAssessmentService extends IService<GradingAssessment> {

    /** 多条件组合分页查询 */
    PageResult<GradingAssessment> pageQuery(GradingAssessmentQuery query);

    /** 详情 */
    GradingAssessment detail(Long id);

    /** 新增评定：标准须启用、评分 0-100、周期 yyyy-MM、同一科室 + 同一标准 + 同一周期不重复 */
    GradingAssessment create(GradingAssessment assessment);

    /** 修改评定：只有待审批的能修改，改评分时重算等级 */
    GradingAssessment update(GradingAssessment assessment);

    /** 一级审批通过：只有待审批的能通过，审批人、审批意见、审批日期留痕 */
    GradingAssessment approve(Long id, String approver, String opinion);

    /** 一级审批驳回：只有待审批的能驳回，审批人、审批意见、审批日期留痕 */
    GradingAssessment reject(Long id, String approver, String opinion);

    /** 分级评定统计：总数、待审批/已通过/已驳回、按等级分组、平均分、达标率（甲+乙+丙占比） */
    Map<String, Object> stats();

    /** 按科室汇总：各科室最新已通过等级、平均分、评定次数 */
    List<Map<String, Object>> statsByDept();

    /** 按月趋势：每月评定数、已通过数、平均分，按月份升序 */
    List<Map<String, Object>> statsByMonth();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(GradingAssessmentQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；重复或非法行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
