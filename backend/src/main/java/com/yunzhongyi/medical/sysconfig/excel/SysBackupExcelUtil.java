package com.yunzhongyi.medical.sysconfig.excel;

import com.yunzhongyi.medical.sysconfig.entity.SysBackup;
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
 * 数据备份 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：备份名称 / 备份类型 / 备份范围 / 记录条数 / 备份文件名 / 文件大小（字节） /
 * 状态 / 执行人 / 已恢复次数 / 最近恢复时间 / 备注。
 * 导入的是历史备份记录：类型与状态导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class SysBackupExcelUtil {

    private SysBackupExcelUtil() {
    }

    private static final String[] HEADERS = {
            "备份名称", "备份类型", "备份范围", "记录条数", "备份文件名", "文件大小（字节）",
            "状态", "执行人", "已恢复次数", "最近恢复时间", "备注"
    };

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void write(List<SysBackup> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("数据备份");
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
            for (SysBackup b : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, b.getBackupName());
                set(row, c++, SysConfigTypeUtil.backupTypeText(b.getBackupType()));
                set(row, c++, b.getBackupScope());
                setNum(row, c++, b.getRecordCount());
                set(row, c++, b.getFileName());
                setNum(row, c++, b.getFileSize());
                set(row, c++, SysConfigTypeUtil.backupStatusText(b.getStatus()));
                set(row, c++, b.getCreatedBy());
                setNum(row, c++, b.getRestoreCount() == null ? null : b.getRestoreCount().longValue());
                set(row, c++, b.getLastRestoreTime() == null ? "" : b.getLastRestoreTime().format(DT_FMT));
                set(row, c++, b.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("数据备份");
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

    public static List<SysBackup> read(InputStream in) {
        List<SysBackup> list = new ArrayList<>();
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
                    SysBackup b = new SysBackup();
                    b.setBackupName(str(row, colIndex, "备份名称"));
                    b.setBackupType(SysConfigTypeUtil.parseBackupType(str(row, colIndex, "备份类型")));
                    b.setBackupScope(str(row, colIndex, "备份范围"));
                    b.setRecordCount(parseLong(str(row, colIndex, "记录条数")));
                    b.setFileName(str(row, colIndex, "备份文件名"));
                    b.setFileSize(parseLong(str(row, colIndex, "文件大小（字节）")));
                    b.setStatus(SysConfigTypeUtil.parseBackupStatus(str(row, colIndex, "状态")));
                    b.setCreatedBy(str(row, colIndex, "执行人"));
                    Long rc = parseLong(str(row, colIndex, "已恢复次数"));
                    b.setRestoreCount(rc == null ? null : rc.intValue());
                    b.setLastRestoreTime(parseDateTime(str(row, colIndex, "最近恢复时间")));
                    b.setRemark(str(row, colIndex, "备注"));
                    list.add(b);
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
