package com.yunzhongyi.medical.statistics.service;

import java.io.OutputStream;
import java.util.Map;

/**
 * 统计汇总服务：第 24/25/26 项的聚合口径
 * <p>
 * 所有数字都从库内已有业务数据现算，不另造业务数据：
 * <ul>
 *   <li>工作统计：工作量登记 + 排班记录（排班人次是另一口径，单独列示）；</li>
 *   <li>三级医院指标统计：三级医院医疗质量指标表（填报值 + 达标判定）；</li>
 *   <li>医疗质量统计：质量检查记录 / 问题 / 整改、医师考核、MDT、新技术新项目
 *       四个模块的实时聚合（医疗质量 / 医疗安全 / 患者安全三组指标）。</li>
 * </ul>
 */
public interface StatisticsService {

    /** 三组统计一次取全：工作统计 + 三级医院指标统计 + 医疗质量统计 */
    Map<String, Object> overview();

    /** 工作统计（24）：工作量登记汇总 + 排班人次（另一口径，单独列示） */
    Map<String, Object> workOverview();

    /** 三级医院医疗质量指标统计（25）：指标表汇总 */
    Map<String, Object> tertiaryOverview();

    /** 医疗质量统计（26）：医疗质量 / 医疗安全 / 患者安全三组实时聚合指标 */
    Map<String, Object> qualityOverview();

    /** 导出统计报表 Excel：工作统计（按科室）、三级医院指标、医疗质量统计三个 sheet */
    void exportReport(OutputStream out);
}
