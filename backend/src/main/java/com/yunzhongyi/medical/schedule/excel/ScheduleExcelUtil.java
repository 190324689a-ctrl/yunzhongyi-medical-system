package com.yunzhongyi.medical.schedule.excel;

import com.yunzhongyi.medical.schedule.entity.Schedule;
import com.yunzhongyi.medical.schedule.service.ScheduleShiftUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 排班记录 Excel 导入导出（Apache POI），仿照 AssessmentExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 班次/排班类型导出写中文（如“白班”“门诊排班”），导入时中文与编码都认。
 */
@Slf4j
public final class ScheduleExcelUtil {

    private ScheduleExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "医师", "科室", "排班日期", "排班类型", "班次", "开始时间", "结束时间", "调班/代班状态", "备注"
    };

    public static void write(List<Schedule> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("排班记录");
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
            for (Schedule s : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, s.getPhysicianName());
                set(row, c++, s.getDeptName());
                set(row, c++, fmtDate(s.getScheduleDate()));
                set(row, c++, ScheduleShiftUtil.typeText(s.getScheduleType()));
                set(row, c++, ScheduleShiftUtil.shiftText(s.getShiftCode()));
                set(row, c++, fmtTime(s.getStartTime()));
                set(row, c++, fmtTime(s.getEndTime()));
                set(row, c++, ScheduleShiftUtil.changeStatusText(s.getChangeStatus()));
                set(row, c++, s.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<Schedule> read(InputStream in) {
        List<Schedule> list = new ArrayList<>();
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
                    Schedule s = new Schedule();
                    s.setPhysicianName(str(row, colIndex, "医师"));
                    s.setDeptName(str(row, colIndex, "科室"));
                    s.setScheduleDate(parseDate(str(row, colIndex, "排班日期")));
                    s.setScheduleType(ScheduleShiftUtil.parseType(str(row, colIndex, "排班类型")));
                    s.setShiftCode(ScheduleShiftUtil.parseShift(str(row, colIndex, "班次")));
                    s.setStartTime(parseTime(str(row, colIndex, "开始时间")));
                    s.setEndTime(parseTime(str(row, colIndex, "结束时间")));
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

    private static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    private static String fmtTime(LocalTime t) {
        return t == null ? "" : t.format(TIME_FMT);
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
        try {
            return LocalDate.parse(s.trim(), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        try {
            return LocalTime.parse(t, TIME_FMT);
        } catch (Exception e) {
            try {
                return LocalTime.parse(t);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
