package com.yunzhongyi.medical.statistics.excel;

import com.yunzhongyi.medical.statistics.entity.WorkloadRecord;
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
 * 工作量登记 Excel 导入导出（Apache POI），仿照 NewTechExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 */
@Slf4j
public final class WorkloadExcelUtil {

    private WorkloadExcelUtil() {
    }

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "医师姓名", "科室", "统计月份", "门诊量（人次）", "住院量（人次）", "手术量（台次）", "备注"
    };

    public static void write(List<WorkloadRecord> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("工作量登记");
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
            for (WorkloadRecord w : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, w.getPhysicianName());
                set(row, c++, w.getDeptName());
                set(row, c++, w.getStatMonth());
                setNum(row, c++, w.getOutpatientCount());
                setNum(row, c++, w.getInpatientCount());
                setNum(row, c++, w.getSurgeryCount());
                set(row, c++, w.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<WorkloadRecord> read(InputStream in) {
        List<WorkloadRecord> list = new ArrayList<>();
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
                    WorkloadRecord w = new WorkloadRecord();
                    w.setPhysicianName(str(row, colIndex, "医师姓名"));
                    w.setDeptName(str(row, colIndex, "科室"));
                    w.setStatMonth(str(row, colIndex, "统计月份"));
                    w.setOutpatientCount(parseInt(str(row, colIndex, "门诊量（人次）")));
                    w.setInpatientCount(parseInt(str(row, colIndex, "住院量（人次）")));
                    w.setSurgeryCount(parseInt(str(row, colIndex, "手术量（台次）")));
                    w.setRemark(str(row, colIndex, "备注"));
                    list.add(w);
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
        if (v != null) {
            cell.setCellValue(v);
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
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            long v = Math.round(cell.getNumericCellValue());
            return String.valueOf(v);
        }
        String s = cell.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
