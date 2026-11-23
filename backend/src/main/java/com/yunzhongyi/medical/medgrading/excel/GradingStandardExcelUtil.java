package com.yunzhongyi.medical.medgrading.excel;

import com.yunzhongyi.medical.medgrading.entity.GradingStandard;
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
 * 分级标准 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：标准编码 / 标准名称 / 分级范围 / 甲级线 / 乙级线 / 丙级线 /
 * 有效期起 / 有效期止 / 状态 / 备注。
 * 导入时按表头文字匹配列；分级范围与状态导出写中文，导入时中文与编码都认；
 * 分数线导入时只做解析，顺序校验由服务端做。
 */
@Slf4j
public final class GradingStandardExcelUtil {

    private GradingStandardExcelUtil() {
    }

    private static final String[] HEADERS = {
            "标准编码", "标准名称", "分级范围", "甲级线", "乙级线", "丙级线",
            "有效期起", "有效期止", "状态", "备注"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出分级标准 */
    public static void write(List<GradingStandard> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("分级标准");
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
            for (GradingStandard s : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, s.getStandardCode());
                set(row, cIdx++, s.getStandardName());
                set(row, cIdx++, MedGradingTypeUtil.scopeText(s.getScope()));
                setNum(row, cIdx++, s.getExcellentMin());
                setNum(row, cIdx++, s.getGoodMin());
                setNum(row, cIdx++, s.getPassMin());
                set(row, cIdx++, s.getValidFrom() == null ? "" : s.getValidFrom().format(DATE_FMT));
                set(row, cIdx++, s.getValidTo() == null ? "" : s.getValidTo().format(DATE_FMT));
                set(row, cIdx++, Integer.valueOf(1).equals(s.getStatus()) ? "启用" : "停用");
                set(row, cIdx++, s.getRemark());
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
            Sheet sheet = wb.createSheet("分级标准");
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

    /** 读取导入的分级标准行（只做解析，校验与去重由服务端做） */
    public static List<GradingStandard> read(InputStream in) {
        List<GradingStandard> list = new ArrayList<>();
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
                    GradingStandard s = new GradingStandard();
                    s.setStandardCode(str(row, colIndex, "标准编码"));
                    s.setStandardName(str(row, colIndex, "标准名称"));
                    s.setScope(MedGradingTypeUtil.parseScope(str(row, colIndex, "分级范围")));
                    s.setExcellentMin(parseDecimal(str(row, colIndex, "甲级线")));
                    s.setGoodMin(parseDecimal(str(row, colIndex, "乙级线")));
                    s.setPassMin(parseDecimal(str(row, colIndex, "丙级线")));
                    s.setValidFrom(parseDate(str(row, colIndex, "有效期起")));
                    s.setValidTo(parseDate(str(row, colIndex, "有效期止")));
                    s.setStatus(parseStatus(str(row, colIndex, "状态")));
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

    private static void setNum(Row row, int col, BigDecimal v) {
        Cell cell = row.createCell(col);
        if (v != null) {
            cell.setCellValue(v.doubleValue());
        } else {
            cell.setCellValue("");
        }
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

    /** 状态中文 / 编码 → 编码：启用 1 / 停用 0，认不出时返回 null（服务端默认启用） */
    private static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "启用", "1" -> 1;
            case "停用", "0" -> 0;
            default -> null;
        };
    }
}
