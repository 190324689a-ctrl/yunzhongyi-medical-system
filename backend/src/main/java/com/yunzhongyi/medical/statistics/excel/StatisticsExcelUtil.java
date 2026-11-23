package com.yunzhongyi.medical.statistics.excel;

import com.yunzhongyi.medical.statistics.entity.QualityIndicator;
import com.yunzhongyi.medical.statistics.service.StatisticsTypeUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 统计报表 Excel 导出（Apache POI）：三个 sheet，分别是工作统计（按科室）、
 * 三级医院医疗质量指标、医疗质量统计。数字全部取自调用方已算好的聚合结果，
 * 本工具只负责落表，不再重新聚合。
 */
public final class StatisticsExcelUtil {

    private StatisticsExcelUtil() {
    }

    public static void write(List<Map<String, Object>> workloadByDept,
                             List<QualityIndicator> indicators,
                             Map<String, Object> qualityOverview,
                             OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            writeWorkloadSheet(wb, headerStyle, workloadByDept);
            writeIndicatorSheet(wb, headerStyle, indicators);
            writeQualitySheet(wb, headerStyle, qualityOverview);

            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出统计报表失败: " + e.getMessage(), e);
        }
    }

    /** 工作统计（按科室） */
    private static void writeWorkloadSheet(SXSSFWorkbook wb, CellStyle headerStyle,
                                           List<Map<String, Object>> byDept) {
        Sheet sheet = wb.createSheet("工作统计");
        String[] headers = {"科室", "门诊量（人次）", "住院量（人次）", "手术量（台次）", "合计", "填报人数"};
        writeHeader(sheet, headerStyle, headers);
        int rowNum = 1;
        long totalOut = 0, totalIn = 0, totalSurgery = 0, total = 0;
        for (Map<String, Object> row : byDept) {
            Row r = sheet.createRow(rowNum++);
            int c = 0;
            set(r, c++, str(row.get("deptName")));
            long outpatient = num(row.get("outpatientCount"));
            long inpatient = num(row.get("inpatientCount"));
            long surgery = num(row.get("surgeryCount"));
            long sum = num(row.get("totalWorkload"));
            setNum(r, c++, outpatient);
            setNum(r, c++, inpatient);
            setNum(r, c++, surgery);
            setNum(r, c++, sum);
            setNum(r, c++, num(row.get("physicianCount")));
            totalOut += outpatient;
            totalIn += inpatient;
            totalSurgery += surgery;
            total += sum;
        }
        Row totalRow = sheet.createRow(rowNum);
        int c = 0;
        set(totalRow, c++, "合计");
        setNum(totalRow, c++, totalOut);
        setNum(totalRow, c++, totalIn);
        setNum(totalRow, c++, totalSurgery);
        setNum(totalRow, c++, total);
    }

    /** 三级医院医疗质量指标 */
    private static void writeIndicatorSheet(SXSSFWorkbook wb, CellStyle headerStyle,
                                            List<QualityIndicator> indicators) {
        Sheet sheet = wb.createSheet("三级医院医疗质量指标");
        String[] headers = {"指标编码", "指标名称", "指标分类", "标准级别", "标准名称",
                "目标值", "实际值", "单位", "比较方向", "达标情况", "统计周期", "数据来源"};
        writeHeader(sheet, headerStyle, headers);
        int rowNum = 1;
        for (QualityIndicator q : indicators) {
            Row r = sheet.createRow(rowNum++);
            int c = 0;
            set(r, c++, q.getIndicatorCode());
            set(r, c++, q.getIndicatorName());
            set(r, c++, q.getCategory());
            set(r, c++, StatisticsTypeUtil.levelText(q.getStandardLevel()));
            set(r, c++, q.getStandardName());
            set(r, c++, StatisticsTypeUtil.fmtNumber(q.getTargetValue()));
            set(r, c++, StatisticsTypeUtil.fmtNumber(q.getActualValue()));
            set(r, c++, q.getUnit());
            set(r, c++, StatisticsTypeUtil.directionText(q.getDirection()));
            set(r, c++, StatisticsTypeUtil.complianceText(q));
            set(r, c++, q.getStatPeriod());
            set(r, c++, q.getDataSource());
        }
    }

    /** 医疗质量统计 */
    private static void writeQualitySheet(SXSSFWorkbook wb, CellStyle headerStyle,
                                          Map<String, Object> qualityOverview) {
        Sheet sheet = wb.createSheet("医疗质量统计");
        String[] headers = {"指标组", "指标", "数值"};
        writeHeader(sheet, headerStyle, headers);
        int rowNum = 1;
        Map<String, Object> quality = asMap(qualityOverview.get("quality"));
        Map<String, Object> safety = asMap(qualityOverview.get("safety"));
        Map<String, Object> patientSafety = asMap(qualityOverview.get("patientSafety"));
        Map<String, Object> newTech = asMap(qualityOverview.get("newTech"));
        rowNum = writeKv(sheet, rowNum, "医疗质量", "医师考核总数", str(quality.get("assessmentTotal")));
        rowNum = writeKv(sheet, rowNum, "医疗质量", "医师考核平均分", str(quality.get("assessmentAvgScore")));
        rowNum = writeKv(sheet, rowNum, "医疗质量", "医师考核优秀率（%）", str(quality.get("assessmentExcellentRate")));
        rowNum = writeKv(sheet, rowNum, "医疗质量", "质量检查计划总数", str(quality.get("checkPlanTotal")));
        rowNum = writeKv(sheet, rowNum, "医疗质量", "质量检查记录数", str(quality.get("checkRecordCount")));
        rowNum = writeKv(sheet, rowNum, "医疗质量", "质量检查平均分", str(quality.get("checkAvgScore")));
        rowNum = writeKv(sheet, rowNum, "医疗安全", "检查问题总数", str(safety.get("issueCount")));
        rowNum = writeKv(sheet, rowNum, "医疗安全", "严重问题数", str(safety.get("majorIssueCount")));
        rowNum = writeKv(sheet, rowNum, "医疗安全", "整改任务总数", str(safety.get("rectificationTotal")));
        rowNum = writeKv(sheet, rowNum, "医疗安全", "整改完成率（%）", str(safety.get("rectificationRate")));
        rowNum = writeKv(sheet, rowNum, "医疗安全", "逾期整改数", str(safety.get("overdueCount")));
        rowNum = writeKv(sheet, rowNum, "患者安全", "MDT 会诊总数", str(patientSafety.get("mdtTotal")));
        rowNum = writeKv(sheet, rowNum, "患者安全", "MDT 已通过数", str(patientSafety.get("mdtApproved")));
        rowNum = writeKv(sheet, rowNum, "患者安全", "紧急 MDT 会诊数", str(patientSafety.get("urgentMdtCount")));
        rowNum = writeKv(sheet, rowNum, "患者安全", "严重问题数", str(patientSafety.get("majorIssueCount")));
        rowNum = writeKv(sheet, rowNum, "患者安全", "逾期整改数", str(patientSafety.get("overdueRectificationCount")));
        rowNum = writeKv(sheet, rowNum, "新技术新项目", "项目总数", str(newTech.get("total")));
        rowNum = writeKv(sheet, rowNum, "新技术新项目", "已立项（含验收 / 推广）数", str(newTech.get("establishedCount")));
        writeKv(sheet, rowNum, "新技术新项目", "立项率（%）", str(newTech.get("establishedRate")));
    }

    // ---------- helpers ----------

    private static void writeHeader(Sheet sheet, CellStyle headerStyle, String[] headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 18 * 256);
        }
    }

    private static int writeKv(Sheet sheet, int rowNum, String group, String name, String value) {
        Row r = sheet.createRow(rowNum);
        set(r, 0, group);
        set(r, 1, name);
        set(r, 2, value);
        return rowNum + 1;
    }

    private static void set(Row row, int col, String v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v == null ? "" : v);
    }

    private static void setNum(Row row, int col, long v) {
        row.createCell(col).setCellValue(v);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object v) {
        if (v instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static long num(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }
}
