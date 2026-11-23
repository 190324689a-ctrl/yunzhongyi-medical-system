package com.yunzhongyi.medical.medgrading.excel;

import com.yunzhongyi.medical.medgrading.entity.GradingAssessment;
import com.yunzhongyi.medical.medgrading.service.MedGradingTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 分级评定 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：科室名称 / 标准编码 / 分级标准 / 评定周期 / 评分 / 等级 /
 * 状态 / 评定人 / 评定日期 / 备注。
 * 导入列：科室名称 / 标准编码 / 评定周期 / 评分 / 评定人 / 评定日期 / 备注；
 * 等级由服务端按标准分数线自动算，导入的评定默认待审批。
 * 标准按编码匹配，科室名称原样导入（服务端按 sys_dept 补全科室 id）。
 */
@Slf4j
public final class GradingAssessmentExcelUtil {

    private GradingAssessmentExcelUtil() {
    }

    private static final String[] HEADERS = {
            "科室名称", "标准编码", "分级标准", "评定周期", "评分", "等级",
            "状态", "评定人", "评定日期", "备注"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出分级评定 */
    public static void write(List<GradingAssessment> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("分级评定");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            int rowNum = 1;
            for (GradingAssessment a : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, a.getDeptName());
                set(row, cIdx++, a.getStandardCode());
                set(row, cIdx++, a.getStandardName());
                set(row, cIdx++, a.getPeriod());
                Cell scoreCell = row.createCell(cIdx++);
                if (a.getScore() != null) {
                    scoreCell.setCellValue(a.getScore().doubleValue());
                } else {
                    scoreCell.setCellValue("");
                }
                set(row, cIdx++, MedGradingTypeUtil.gradeText(a.getGrade()));
                set(row, cIdx++, MedGradingTypeUtil.assessStatusText(a.getStatus()));
                set(row, cIdx++, a.getAssessor());
                set(row, cIdx++, a.getAssessDate() == null ? "" : a.getAssessDate().format(DATE_FMT));
                set(row, cIdx++, a.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("分级评定");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取导入的分级评定行（只做解析，校验与去重由服务端做）。
     * 标准编码读入 standardCode 字段，服务端按编码查标准后再回填标准 id 与名称。
     */
    public static List<GradingAssessment> read(InputStream in) {
        List<GradingAssessment> list = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return list;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return list;
            }
            java.util.Map<String, Integer> colIndex = new java.util.HashMap<>();
            for (Cell cell : headerRow) {
                colIndex.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                try {
                    GradingAssessment a = new GradingAssessment();
                    a.setDeptName(str(row, colIndex, "科室名称"));
                    a.setStandardCode(str(row, colIndex, "标准编码"));
                    a.setPeriod(str(row, colIndex, "评定周期"));
                    a.setScore(parseDecimal(str(row, colIndex, "评分")));
                    a.setAssessor(str(row, colIndex, "评定人"));
                    a.setAssessDate(parseDate(str(row, colIndex, "评定日期")));
                    a.setRemark(str(row, colIndex, "备注"));
                    list.add(a);
                } catch (Exception e) {
                    log.warn("导入第 {} 行解析失败，已跳过: {}", r + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 Excel 失败: " + e.getMessage(), e);
        }
        return list;
    }

    // ---------- helpers ----------

    private static void set(Row row, int col, String v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v == null ? "" : v);
    }

    private static boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !cell.toString().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String str(Row row, java.util.Map<String, Integer> colIndex, String header) {
        Integer idx = colIndex.get(header);
        if (idx == null) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return null;
        }
        return cell.toString().trim();
    }

    private static BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
