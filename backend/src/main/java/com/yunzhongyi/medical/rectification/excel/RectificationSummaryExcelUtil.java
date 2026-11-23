package com.yunzhongyi.medical.rectification.excel;

import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import com.yunzhongyi.medical.rectification.entity.RectificationSummary;
import com.yunzhongyi.medical.rectification.service.RectificationTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 检查整改汇总报告 Excel 导入导出（Apache POI），仿照 CoreSystemExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 汇总范围/检查类型导出写中文（如“全院”“科室自查”），导入时中文与编码都认。
 * 整改完成率导出写数字（如 25.00），导入时按数字读，入库后由服务按
 * “已复查通过 / 整改任务总数”重新现算，保证口径一致。
 */
@Slf4j
public final class RectificationSummaryExcelUtil {

    private RectificationSummaryExcelUtil() {
    }

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "统计周期", "汇总范围", "科室", "检查类型", "检查计划数", "检查记录数", "平均分",
            "问题总数", "严重问题数", "一般问题数", "轻微问题数",
            "整改任务数", "待整改数", "已整改数", "已复查通过数", "整改完成率(%)", "逾期整改数",
            "生成人", "备注"
    };

    public static void write(List<RectificationSummary> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("检查整改汇总");
            // 表头
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }
            int rowNum = 1;
            for (RectificationSummary s : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, s.getStatPeriod());
                set(row, cIdx++, RectificationTypeUtil.scopeText(s.getScopeType()));
                set(row, cIdx++, s.getDeptName());
                set(row, cIdx++, s.getInspectType() == null ? "" : QualityTypeUtil.typeText(s.getInspectType()));
                setNum(row, cIdx++, s.getPlanCount());
                setNum(row, cIdx++, s.getRecordCount());
                setNum(row, cIdx++, s.getAvgScore());
                setNum(row, cIdx++, s.getIssueCount());
                setNum(row, cIdx++, s.getMajorCount());
                setNum(row, cIdx++, s.getMinorCount());
                setNum(row, cIdx++, s.getMildCount());
                setNum(row, cIdx++, s.getRectificationTotal());
                setNum(row, cIdx++, s.getRectificationPending());
                setNum(row, cIdx++, s.getRectificationDone());
                setNum(row, cIdx++, s.getRectificationReviewed());
                setNum(row, cIdx++, s.getRectificationRate());
                setNum(row, cIdx++, s.getOverdueCount());
                set(row, cIdx++, s.getGeneratedBy());
                set(row, cIdx++, s.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<RectificationSummary> read(InputStream in) {
        List<RectificationSummary> list = new ArrayList<>();
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
                    RectificationSummary s = new RectificationSummary();
                    s.setStatPeriod(str(row, colIndex, "统计周期"));
                    s.setScopeType(RectificationTypeUtil.parseScope(str(row, colIndex, "汇总范围")));
                    s.setDeptName(str(row, colIndex, "科室"));
                    s.setInspectType(QualityTypeUtil.parseType(str(row, colIndex, "检查类型")));
                    s.setPlanCount(num(row, colIndex, "检查计划数"));
                    s.setRecordCount(num(row, colIndex, "检查记录数"));
                    s.setAvgScore(decimal(row, colIndex, "平均分"));
                    s.setIssueCount(num(row, colIndex, "问题总数"));
                    s.setMajorCount(num(row, colIndex, "严重问题数"));
                    s.setMinorCount(num(row, colIndex, "一般问题数"));
                    s.setMildCount(num(row, colIndex, "轻微问题数"));
                    s.setRectificationTotal(num(row, colIndex, "整改任务数"));
                    s.setRectificationPending(num(row, colIndex, "待整改数"));
                    s.setRectificationDone(num(row, colIndex, "已整改数"));
                    s.setRectificationReviewed(num(row, colIndex, "已复查通过数"));
                    s.setRectificationRate(decimal(row, colIndex, "整改完成率(%)"));
                    s.setOverdueCount(num(row, colIndex, "逾期整改数"));
                    s.setGeneratedBy(str(row, colIndex, "生成人"));
                    s.setRemark(str(row, colIndex, "备注"));
                    list.add(s);
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

    private static void setNum(Row row, int col, Integer v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v == null ? 0 : v);
    }

    private static void setNum(Row row, int col, BigDecimal v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v == null ? 0 : v.doubleValue());
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

    private static Integer num(Row row, java.util.Map<String, Integer> colIndex, String header) {
        Double d = cellDouble(row, colIndex, header);
        if (d == null) {
            return null;
        }
        return (int) Math.round(d);
    }

    private static BigDecimal decimal(Row row, java.util.Map<String, Integer> colIndex, String header) {
        Double d = cellDouble(row, colIndex, header);
        if (d == null) {
            return null;
        }
        return BigDecimal.valueOf(d);
    }

    /** 取单元格数值：数字格直接读数值（不取整，平均分/完成率是小数），文本格按数字解析 */
    private static Double cellDouble(Row row, java.util.Map<String, Integer> colIndex, String header) {
        Integer idx = colIndex.get(header);
        if (idx == null) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return cell.getNumericCellValue();
        }
        String s = cell.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
