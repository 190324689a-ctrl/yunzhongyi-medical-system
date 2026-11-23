package com.yunzhongyi.medical.techgrading.excel;

import com.yunzhongyi.medical.techgrading.entity.TechAuthorization;
import com.yunzhongyi.medical.techgrading.service.TechGradingTypeUtil;
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

/**
 * 医师医疗技术授权 Excel 导入导出（Apache POI），仿照 CoreSystemExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 技术分级/授权级别/授权状态导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class TechAuthorizationExcelUtil {

    private TechAuthorizationExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "医师姓名", "科室", "技术编号", "技术名称", "技术分级", "授权级别",
            "授权开始日期", "授权结束日期", "授权状态", "审批人", "备注"
    };

    public static void write(List<TechAuthorization> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医师技术授权");
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
            for (TechAuthorization p : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, p.getPhysicianName());
                set(row, cIdx++, p.getDeptName());
                set(row, cIdx++, p.getTechNo());
                set(row, cIdx++, p.getTechName());
                set(row, cIdx++, TechGradingTypeUtil.levelText(p.getTechLevel()));
                set(row, cIdx++, TechGradingTypeUtil.levelText(p.getAuthLevel()));
                set(row, cIdx++, TechGradingTypeUtil.fmtDate(p.getStartDate()));
                set(row, cIdx++, TechGradingTypeUtil.fmtDate(p.getEndDate()));
                set(row, cIdx++, TechGradingTypeUtil.authStatusText(p.getStatus()));
                set(row, cIdx++, p.getApprover());
                set(row, cIdx++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<TechAuthorization> read(InputStream in) {
        List<TechAuthorization> list = new ArrayList<>();
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
                    TechAuthorization p = new TechAuthorization();
                    p.setPhysicianName(str(row, colIndex, "医师姓名"));
                    p.setDeptName(str(row, colIndex, "科室"));
                    p.setTechNo(str(row, colIndex, "技术编号"));
                    p.setTechName(str(row, colIndex, "技术名称"));
                    p.setTechLevel(TechGradingTypeUtil.parseLevel(str(row, colIndex, "技术分级")));
                    p.setAuthLevel(TechGradingTypeUtil.parseLevel(str(row, colIndex, "授权级别")));
                    p.setStartDate(parseDate(str(row, colIndex, "授权开始日期")));
                    p.setEndDate(parseDate(str(row, colIndex, "授权结束日期")));
                    Integer status = TechGradingTypeUtil.parseAuthStatus(str(row, colIndex, "授权状态"));
                    p.setStatus(status == null ? TechGradingTypeUtil.AUTH_STATUS_PENDING : status);
                    p.setApprover(str(row, colIndex, "审批人"));
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
