package com.yunzhongyi.medical.system.excel;

import com.yunzhongyi.medical.system.entity.SysDept;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 科室 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：科室名称 / 科室编码 / 科室类型 / 科主任 / 联系电话 / 排序 / 状态。
 * 科室类型导出写中文（临床 / 医技 / 行政 / 门诊），导入时中文与编码都认。
 */
@Slf4j
public final class SysDeptExcelUtil {

    private SysDeptExcelUtil() {
    }

    private static final String[] HEADERS = {
            "科室名称", "科室编码", "科室类型", "科主任", "联系电话", "排序", "状态"
    };

    public static void write(List<SysDept> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("科室");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            int rowNum = 1;
            for (SysDept d : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, d.getName());
                set(row, c++, d.getCode());
                set(row, c++, typeText(d.getType()));
                set(row, c++, d.getDirector());
                set(row, c++, d.getPhone());
                setNum(row, c++, d.getSortOrder() == null ? null : d.getSortOrder().longValue());
                set(row, c++, Integer.valueOf(1).equals(d.getStatus()) ? "启用" : "停用");
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("科室");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<SysDept> read(InputStream in) {
        List<SysDept> list = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return list;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return list;
            }
            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                colIndex.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                try {
                    SysDept d = new SysDept();
                    d.setName(str(row, colIndex, "科室名称"));
                    d.setCode(str(row, colIndex, "科室编码"));
                    d.setType(parseType(str(row, colIndex, "科室类型")));
                    d.setDirector(str(row, colIndex, "科主任"));
                    d.setPhone(str(row, colIndex, "联系电话"));
                    Long sort = parseLong(str(row, colIndex, "排序"));
                    d.setSortOrder(sort == null ? null : sort.intValue());
                    d.setStatus(parseStatus(str(row, colIndex, "状态")));
                    list.add(d);
                } catch (Exception e) {
                    log.warn("导入第 {} 行解析失败，已跳过: {}", r + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 Excel 失败: " + e.getMessage(), e);
        }
        return list;
    }

    public static String typeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "clinical" -> "临床";
            case "medical-technical" -> "医技";
            case "admin" -> "行政";
            case "outpatient" -> "门诊";
            default -> type;
        };
    }

    public static String parseType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "临床", "clinical" -> "clinical";
            case "医技", "medical-technical" -> "medical-technical";
            case "行政", "admin" -> "admin";
            case "门诊", "outpatient" -> "outpatient";
            default -> text.trim();
        };
    }

    private static void set(Row row, int col, String v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v == null ? "" : v);
    }

    private static void setNum(Row row, int col, Long v) {
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

    private static String str(Row row, Map<String, Integer> colIndex, String header) {
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

    private static Long parseLong(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return (long) Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
