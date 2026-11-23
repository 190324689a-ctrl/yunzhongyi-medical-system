package com.yunzhongyi.medical.sysconfig.excel;

import com.yunzhongyi.medical.sysconfig.entity.SysInterface;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统接口 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：接口编码 / 接口名称 / 接口类型 / 接口地址 / 鉴权方式 / 状态 /
 * 超时时间（秒） / 最近测试时间 / 最近测试结果 / 最近测试说明 / 备注。
 * 鉴权凭证不导出、不导入（安全原因）。
 * 导入时按表头文字匹配列；类型与鉴权方式导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class SysInterfaceExcelUtil {

    private SysInterfaceExcelUtil() {
    }

    private static final String[] HEADERS = {
            "接口编码", "接口名称", "接口类型", "接口地址", "鉴权方式", "状态",
            "超时时间（秒）", "最近测试时间", "最近测试结果", "最近测试说明", "备注"
    };

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void write(List<SysInterface> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("系统接口");
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
            for (SysInterface s : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, s.getInterfaceCode());
                set(row, c++, s.getInterfaceName());
                set(row, c++, SysConfigTypeUtil.interfaceTypeText(s.getType()));
                set(row, c++, s.getUrl());
                set(row, c++, SysConfigTypeUtil.authTypeText(s.getAuthType()));
                set(row, c++, Integer.valueOf(1).equals(s.getStatus()) ? "启用" : "停用");
                setNum(row, c++, s.getTimeoutSeconds() == null ? null : s.getTimeoutSeconds().longValue());
                set(row, c++, s.getLastTestTime() == null ? "" : s.getLastTestTime().format(DT_FMT));
                set(row, c++, SysConfigTypeUtil.testResultText(s.getLastTestResult()));
                set(row, c++, s.getLastTestMsg());
                set(row, c++, s.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("系统接口");
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

    public static List<SysInterface> read(InputStream in) {
        List<SysInterface> list = new ArrayList<>();
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
                    SysInterface s = new SysInterface();
                    s.setInterfaceCode(str(row, colIndex, "接口编码"));
                    s.setInterfaceName(str(row, colIndex, "接口名称"));
                    s.setType(SysConfigTypeUtil.parseInterfaceType(str(row, colIndex, "接口类型")));
                    s.setUrl(str(row, colIndex, "接口地址"));
                    s.setAuthType(SysConfigTypeUtil.parseAuthType(str(row, colIndex, "鉴权方式")));
                    s.setStatus(parseStatus(str(row, colIndex, "状态")));
                    Long timeout = parseLong(str(row, colIndex, "超时时间（秒）"));
                    s.setTimeoutSeconds(timeout == null ? null : timeout.intValue());
                    s.setLastTestTime(parseDateTime(str(row, colIndex, "最近测试时间")));
                    s.setLastTestResult(SysConfigTypeUtil.parseTestResult(str(row, colIndex, "最近测试结果")));
                    s.setLastTestMsg(str(row, colIndex, "最近测试说明"));
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

    private static LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DT_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
