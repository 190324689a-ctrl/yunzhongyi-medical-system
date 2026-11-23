package com.yunzhongyi.medical.sysconfig.excel;

import com.yunzhongyi.medical.sysconfig.entity.SysParam;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
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
 * 系统参数 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：参数键 / 参数名称 / 参数值 / 参数分组 / 值类型 / 状态 / 是否内置 / 备注。
 * 导入时按表头文字匹配列；分组与值类型导出写中文，导入时中文与编码都认；
 * 值与值类型的匹配校验由服务端做。
 */
@Slf4j
public final class SysParamExcelUtil {

    private SysParamExcelUtil() {
    }

    private static final String[] HEADERS = {
            "参数键", "参数名称", "参数值", "参数分组", "值类型", "状态", "是否内置", "备注"
    };

    public static void write(List<SysParam> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("系统参数");
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
            for (SysParam p : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, p.getParamKey());
                set(row, c++, p.getParamName());
                set(row, c++, p.getParamValue());
                set(row, c++, SysConfigTypeUtil.paramGroupText(p.getParamGroup()));
                set(row, c++, SysConfigTypeUtil.valueTypeText(p.getValueType()));
                set(row, c++, Integer.valueOf(1).equals(p.getStatus()) ? "启用" : "停用");
                set(row, c++, Integer.valueOf(1).equals(p.getIsSystem()) ? "是" : "否");
                set(row, c++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("系统参数");
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

    public static List<SysParam> read(InputStream in) {
        List<SysParam> list = new ArrayList<>();
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
                    SysParam p = new SysParam();
                    p.setParamKey(str(row, colIndex, "参数键"));
                    p.setParamName(str(row, colIndex, "参数名称"));
                    p.setParamValue(str(row, colIndex, "参数值"));
                    p.setParamGroup(SysConfigTypeUtil.parseParamGroup(str(row, colIndex, "参数分组")));
                    p.setValueType(SysConfigTypeUtil.parseValueType(str(row, colIndex, "值类型")));
                    p.setStatus(parseStatus(str(row, colIndex, "状态")));
                    p.setIsSystem(parseYesNo(str(row, colIndex, "是否内置")));
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

    private static Integer parseYesNo(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "是", "1" -> 1;
            case "否", "0" -> 0;
            default -> null;
        };
    }
}
