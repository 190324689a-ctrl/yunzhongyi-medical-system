package com.yunzhongyi.medical.statistics.service.impl;

import com.yunzhongyi.medical.assessment.service.AssessmentService;
import com.yunzhongyi.medical.mdt.service.MdtService;
import com.yunzhongyi.medical.newtech.service.NewTechService;
import com.yunzhongyi.medical.quality.entity.QualityIssue;
import com.yunzhongyi.medical.quality.mapper.QualityIssueMapper;
import com.yunzhongyi.medical.quality.service.QualityService;
import com.yunzhongyi.medical.schedule.service.ScheduleService;
import com.yunzhongyi.medical.statistics.excel.StatisticsExcelUtil;
import com.yunzhongyi.medical.statistics.service.QualityIndicatorService;
import com.yunzhongyi.medical.statistics.service.StatisticsService;
import com.yunzhongyi.medical.statistics.service.StatisticsTypeUtil;
import com.yunzhongyi.medical.statistics.service.WorkloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计汇总服务实现：第 24/25/26 项的聚合口径
 * <p>
 * 所有数字都从库内已有业务数据现算：工作量登记、排班记录、三级医院医疗质量
 * 指标表、质量检查记录 / 问题 / 整改、医师考核、MDT、新技术新项目。
 * 排班人次与诊疗量是两个口径，接口里分开返回、不混算，并在字段说明里注明。
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final WorkloadService workloadService;
    private final QualityIndicatorService qualityIndicatorService;
    private final AssessmentService assessmentService;
    private final QualityService qualityService;
    private final MdtService mdtService;
    private final NewTechService newTechService;
    private final ScheduleService scheduleService;
    private final QualityIssueMapper qualityIssueMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("work", workOverview());
        r.put("tertiary", tertiaryOverview());
        r.put("quality", qualityOverview());
        return r;
    }

    @Override
    public Map<String, Object> workOverview() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("workload", workloadService.stats());
        Map<String, Object> scheduleStats = scheduleService.stats();
        Map<String, Object> schedule = new LinkedHashMap<>();
        schedule.put("total", scheduleStats.get("total"));
        schedule.put("pendingChange", scheduleStats.get("pendingChange"));
        schedule.put("nightCount", scheduleStats.get("nightCount"));
        schedule.put("byType", scheduleStats.get("byType"));
        schedule.put("byShift", scheduleStats.get("byShift"));
        schedule.put("note", "排班人次（科室 / 门诊 / 住院 / 急诊排班条数），不是诊疗量；"
                + "门诊量 / 住院量 / 手术量见工作量登记汇总。");
        r.put("schedule", schedule);
        r.put("note", "门诊量 / 住院量 / 手术量来自工作量登记（按医师按月填报）；"
                + "排班人次来自排班记录，是另一口径。");
        return r;
    }

    @Override
    public Map<String, Object> tertiaryOverview() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.putAll(qualityIndicatorService.stats());
        com.yunzhongyi.medical.statistics.dto.QualityIndicatorQuery allQuery =
                new com.yunzhongyi.medical.statistics.dto.QualityIndicatorQuery();
        allQuery.setPage(1);
        allQuery.setSize(10000);
        r.put("indicators", qualityIndicatorService.listByQuery(allQuery));
        r.put("note", "指标实际值均注明数据来源，见每条指标的 dataSource；"
                + "未填报的指标不计入达标率。");
        return r;
    }

    @Override
    public Map<String, Object> qualityOverview() {
        Map<String, Object> r = new LinkedHashMap<>();

        // 医疗质量指标：医师考核 + 质量检查记录
        Map<String, Object> assessment = assessmentService.stats();
        Map<String, Object> qualityCheck = qualityService.stats();
        Map<String, Object> qualitySection = new LinkedHashMap<>();
        qualitySection.put("assessmentTotal", assessment.get("total"));
        qualitySection.put("assessmentAvgScore", assessment.get("avgScore"));
        qualitySection.put("assessmentExcellentCount", assessment.get("excellentCount"));
        qualitySection.put("assessmentExcellentRate", assessment.get("excellentRate"));
        qualitySection.put("assessmentByGrade", assessment.get("byGrade"));
        qualitySection.put("assessmentByDept", assessment.get("byDept"));
        qualitySection.put("checkPlanTotal", qualityCheck.get("total"));
        qualitySection.put("checkRecordCount", qualityCheck.get("recordCount"));
        qualitySection.put("checkAvgScore", qualityCheck.get("avgScore"));
        qualitySection.put("checkByGrade", qualityCheck.get("byGrade"));
        qualitySection.put("checkByDept", qualityCheck.get("byDept"));
        qualitySection.put("checkByMonth", qualityCheck.get("byMonth"));
        r.put("quality", qualitySection);

        // 医疗安全指标：检查问题（按严重程度）+ 整改跟踪
        List<QualityIssue> issues = qualityIssueMapper.selectList(null);
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (String s : List.of("MAJOR", "GENERAL", "MINOR", "MILD")) {
            bySeverity.put(s, issues.stream().filter(i -> s.equals(i.getSeverity())).count());
        }
        Map<String, Object> safety = new LinkedHashMap<>();
        safety.put("issueCount", qualityCheck.get("issueCount"));
        safety.put("bySeverity", bySeverity);
        safety.put("majorIssueCount", bySeverity.get("MAJOR"));
        safety.put("rectificationTotal", qualityCheck.get("rectificationTotal"));
        safety.put("rectificationReviewed", qualityCheck.get("rectificationReviewed"));
        safety.put("rectificationRate", qualityCheck.get("rectificationRate"));
        safety.put("overdueCount", qualityCheck.get("overdueCount"));
        r.put("safety", safety);

        // 患者安全指标：MDT 会诊（紧急会诊）+ 严重问题 + 逾期整改
        Map<String, Object> mdt = mdtService.stats();
        Map<String, Object> patientSafety = new LinkedHashMap<>();
        patientSafety.put("mdtTotal", mdt.get("total"));
        patientSafety.put("mdtApproved", mdt.get("approved"));
        patientSafety.put("mdtPending", mdt.get("pending"));
        patientSafety.put("mdtRejected", mdt.get("rejected"));
        patientSafety.put("urgentMdtCount", mdt.get("urgentCount"));
        patientSafety.put("mdtByType", mdt.get("byType"));
        patientSafety.put("majorIssueCount", bySeverity.get("MAJOR"));
        patientSafety.put("overdueRectificationCount", qualityCheck.get("overdueCount"));
        r.put("patientSafety", patientSafety);

        // 新技术新项目：立项与推广情况（医疗质量改进的另一面）
        Map<String, Object> newTech = newTechService.stats();
        long total = toLong(newTech.get("total"));
        long established = toLong(newTech.get("approved")) + toLong(newTech.get("accepted"))
                + toLong(newTech.get("promoted"));
        Map<String, Object> newTechSection = new LinkedHashMap<>();
        newTechSection.put("total", total);
        newTechSection.put("pending", newTech.get("pending"));
        newTechSection.put("approved", newTech.get("approved"));
        newTechSection.put("accepted", newTech.get("accepted"));
        newTechSection.put("promoted", newTech.get("promoted"));
        newTechSection.put("rejected", newTech.get("rejected"));
        newTechSection.put("ethicsPending", newTech.get("ethicsPending"));
        newTechSection.put("establishedCount", established);
        newTechSection.put("establishedRate", total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(established * 100.0 / total).setScale(2, RoundingMode.HALF_UP));
        newTechSection.put("byType", newTech.get("byType"));
        r.put("newTech", newTechSection);

        r.put("note", "医疗质量统计全部取自质量检查、医师考核、MDT、新技术新项目四个模块"
                + "的实时数据；整改完成率 = 已复查通过 / 整改任务总数；逾期整改按整改期限"
                + "已过当天且未复查通过动态判定。");
        return r;
    }

    @Override
    public void exportReport(OutputStream out) {
        com.yunzhongyi.medical.statistics.dto.QualityIndicatorQuery allQuery =
                new com.yunzhongyi.medical.statistics.dto.QualityIndicatorQuery();
        allQuery.setPage(1);
        allQuery.setSize(10000);
        StatisticsExcelUtil.write(
                workloadService.statsByDept(),
                qualityIndicatorService.listByQuery(allQuery),
                qualityOverview(),
                out);
    }

    private static long toLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }
}
