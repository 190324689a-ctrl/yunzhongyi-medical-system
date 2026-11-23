package com.yunzhongyi.medical.ethics.excel;

import com.yunzhongyi.medical.ethics.entity.EthicsComplaint;
import com.yunzhongyi.medical.ethics.service.EthicsTypeUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 医德医风投诉 Excel 导入导出（Apache POI），仿照 PolicyExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 投诉类型/流转状态导出写中文（如“服务态度”“已反馈”），导入时中文与编码都认。
 */
@Slf4j
public final class EthicsComplaintExcelUtil {

    private EthicsComplaintExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "投诉编号", "投诉人", "投诉人电话", "被投诉医师", "科室", "投诉类型", "投诉日期", "处理期限",
            "投诉内容", "状态", "处理人", "处理结果", "处理日期", "反馈内容", "反馈日期", "备注"
    };

    public static void write(List<EthicsComplaint> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医德医风投诉");
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
            for (EthicsComplaint c : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, c.getComplaintNo());
                set(row, cIdx++, c.getComplainantName());
                set(row, cIdx++, c.getComplainantPhone());
                set(row, cIdx++, c.getPhysicianName());
                set(row, cIdx++, c.getDeptName());
                set(row, cIdx++, EthicsTypeUtil.complaintTypeText(c.getComplaintType()));
                set(row, cIdx++, EthicsTypeUtil.fmtDate(c.getComplaintDate()));
                set(row, cIdx++, EthicsTypeUtil.fmtDate(c.getDeadlineDate()));
                set(row, cIdx++, c.getContent());
                set(row, cIdx++, EthicsTypeUtil.complaintStatusText(c.getStatus()));
                set(row, cIdx++, c.getHandler());
                set(row, cIdx++, c.getHandleResult());
                set(row, cIdx++, EthicsTypeUtil.fmtDate(c.getHandleDate()));
                set(row, cIdx++, c.getFeedbackContent());
                set(row, cIdx++, EthicsTypeUtil.fmtDate(c.getFeedbackDate()));
                set(row, cIdx++, c.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<EthicsComplaint> read(InputStream in) {
        List<EthicsComplaint> list = new ArrayList<>();
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
                    EthicsComplaint c = new EthicsComplaint();
                    c.setComplaintNo(str(row, colIndex, "投诉编号"));
                    c.setComplainantName(str(row, colIndex, "投诉人"));
                    c.setComplainantPhone(str(row, colIndex, "投诉人电话"));
                    c.setPhysicianName(str(row, colIndex, "被投诉医师"));
                    c.setDeptName(str(row, colIndex, "科室"));
                    c.setComplaintType(EthicsTypeUtil.parseComplaintType(str(row, colIndex, "投诉类型")));
                    c.setComplaintDate(parseDate(str(row, colIndex, "投诉日期")));
                    c.setDeadlineDate(parseDate(str(row, colIndex, "处理期限")));
                    c.setContent(str(row, colIndex, "投诉内容"));
                    c.setStatus(EthicsTypeUtil.parseComplaintStatus(str(row, colIndex, "状态")));
                    c.setHandler(str(row, colIndex, "处理人"));
                    c.setHandleResult(str(row, colIndex, "处理结果"));
                    c.setHandleDate(parseDate(str(row, colIndex, "处理日期")));
                    c.setFeedbackContent(str(row, colIndex, "反馈内容"));
                    c.setFeedbackDate(parseDate(str(row, colIndex, "反馈日期")));
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
