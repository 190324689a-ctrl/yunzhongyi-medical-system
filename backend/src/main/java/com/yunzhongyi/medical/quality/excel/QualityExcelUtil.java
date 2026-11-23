package com.yunzhongyi.medical.quality.excel;

import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityRecord;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 质量检查 Excel 导入导出（Apache POI），仿照 MdtExcelUtil。
 * <p>
 * 导出：检查记录（按计划查询条件），列与检查记录一致；
 * 导入：检查计划，按模板名称找模板、按被查科室+计划日期去重；
 * 模板下载：检查计划导入模板（空表头，与导入格式一致）。
 * <p>
 * 检查类型/等级导出写中文（如“院级检查”“合格”），导入时中文与编码都认；
 * 导入按表头文字匹配列，容错性更好。
 */
@Slf4j
public final class QualityExcelUtil {

    private QualityExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 检查记录导出列：标题 */
    private static final String[] RECORD_HEADERS = {
            "检查计划", "检查类型", "被查科室", "检查人", "检查日期",
            "各项得分", "总分", "等级", "检查结果", "问题数", "备注"
    };

    /** 检查计划导入列：标题 */
    private static final String[] PLAN_HEADERS = {
            "模板名称", "检查类型", "被查科室", "检查人", "计划检查日期", "计划状态", "备注"
    };

    /** 导出检查记录 */
    public static void writeRecords(List<QualityRecord> list, Map<Long, Long> issueCountByRecord,
                                    OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("检查记录");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < RECORD_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(RECORD_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }
            int rowNum = 1;
            for (QualityRecord r : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, r.getTemplateName());
                set(row, c++, QualityTypeUtil.typeText(r.getInspectType()));
                set(row, c++, r.getDeptName());
                set(row, c++, r.getInspector());
                set(row, c++, r.getCheckDate() == null ? "" : r.getCheckDate().format(DATE_FMT));
                set(row, c++, QualityTypeUtil.itemScoresText(r.getItemScores()));
                set(row, c++, r.getTotalScore() == null ? ""
                        : r.getTotalScore().stripTrailingZeros().toPlainString());
                set(row, c++, QualityTypeUtil.gradeText(r.getGrade()));
                set(row, c++, r.getResult());
                set(row, c++, String.valueOf(issueCountByRecord.getOrDefault(r.getId(), 0L)));
                set(row, c++, r.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /** 导入模板下载：检查计划导入模板（空表头） */
    public static void writePlanTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("检查计划");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < PLAN_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(PLAN_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /** 读检查计划导入文件 */
    public static List<QualityPlan> readPlans(InputStream in) {
        List<QualityPlan> list = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return list;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return list;
            }
            // 按表头文字定位列下标
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
                    QualityPlan p = new QualityPlan();
                    p.setTemplateName(str(row, colIndex, "模板名称"));
                    p.setInspectType(QualityTypeUtil.parseType(str(row, colIndex, "检查类型")));
                    p.setDeptName(str(row, colIndex, "被查科室"));
                    p.setInspector(str(row, colIndex, "检查人"));
                    p.setPlanDate(parseDate(str(row, colIndex, "计划检查日期")));
                    p.setStatus(QualityTypeUtil.parsePlanStatus(str(row, colIndex, "计划状态")));
                    p.setRemark(str(row, colIndex, "备注"));
                    list.add(p);
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
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            // 纯数字按整数文本读，避免科学计数法
            long v = Math.round(cell.getNumericCellValue());
            return String.valueOf(v);
        }
        String s = cell.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        try {
            return LocalDate.parse(t, DATE_FMT);
        } catch (Exception e) {
            try {
                return LocalDate.parse(t);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
