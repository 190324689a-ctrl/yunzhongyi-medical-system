package com.yunzhongyi.medical.committee.excel;

import com.yunzhongyi.medical.committee.entity.Committee;
import com.yunzhongyi.medical.committee.service.CommitteeTypeUtil;
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
 * 委员会 Excel 导入导出（Apache POI），仿照 RadiationPersonnelExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 委员会类别/状态导出写中文（如“医疗质量”“在任”），导入时中文与编码都认。
 */
@Slf4j
public final class CommitteeExcelUtil {

    private CommitteeExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "委员会名称", "委员会类别", "主任委员", "副主任委员", "秘书", "委员人数",
            "届次", "任期开始日期", "任期结束日期", "状态", "备注"
    };

    public static void write(List<Committee> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("委员会");
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
            for (Committee c : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, c.getName());
                set(row, cIdx++, CommitteeTypeUtil.typeText(c.getType()));
                set(row, cIdx++, c.getChairperson());
                set(row, cIdx++, c.getViceChairperson());
                set(row, cIdx++, c.getSecretary());
                set(row, cIdx++, c.getMemberCount() == null ? "" : String.valueOf(c.getMemberCount()));
                set(row, cIdx++, c.getTermNo() == null ? "" : String.valueOf(c.getTermNo()));
                set(row, cIdx++, CommitteeTypeUtil.fmtDate(c.getTermStartDate()));
                set(row, cIdx++, CommitteeTypeUtil.fmtDate(c.getTermEndDate()));
                set(row, cIdx++, CommitteeTypeUtil.statusText(c.getStatus()));
                set(row, cIdx++, c.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<Committee> read(InputStream in) {
        List<Committee> list = new ArrayList<>();
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
                    Committee c = new Committee();
                    c.setName(str(row, colIndex, "委员会名称"));
                    c.setType(CommitteeTypeUtil.parseType(str(row, colIndex, "委员会类别")));
                    c.setChairperson(str(row, colIndex, "主任委员"));
                    c.setViceChairperson(str(row, colIndex, "副主任委员"));
                    c.setSecretary(str(row, colIndex, "秘书"));
                    c.setMemberCount(parseInt(str(row, colIndex, "委员人数")));
                    c.setTermNo(parseInt(str(row, colIndex, "届次")));
                    c.setTermStartDate(parseDate(str(row, colIndex, "任期开始日期")));
                    c.setTermEndDate(parseDate(str(row, colIndex, "任期结束日期")));
                    Integer status = CommitteeTypeUtil.parseStatus(str(row, colIndex, "状态"));
                    c.setStatus(status == null ? CommitteeTypeUtil.STATUS_ACTIVE : status);
                    c.setRemark(str(row, colIndex, "备注"));
                    list.add(c);
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

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
