package com.yunzhongyi.medical.ethics.excel;

import com.yunzhongyi.medical.ethics.entity.EthicsCommendation;
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
 * 医德医风表彰 Excel 导入导出（Apache POI），仿照 PolicyExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 表彰类型/级别/审批状态导出写中文（如“锦旗”“院级”“已通过”），导入时中文与编码都认。
 */
@Slf4j
public final class EthicsCommendationExcelUtil {

    private EthicsCommendationExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "医师姓名", "科室", "表彰类型", "表彰级别", "表彰标题", "表彰事迹", "授予方",
            "表彰日期", "审批状态", "审批人", "备注"
    };

    public static void write(List<EthicsCommendation> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医德医风表彰");
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
            for (EthicsCommendation c : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, c.getPhysicianName());
                set(row, cIdx++, c.getDeptName());
                set(row, cIdx++, EthicsTypeUtil.commendTypeText(c.getCommendType()));
                set(row, cIdx++, EthicsTypeUtil.commendLevelText(c.getCommendLevel()));
                set(row, cIdx++, c.getTitle());
                set(row, cIdx++, c.getContent());
                set(row, cIdx++, c.getGrantedBy());
                set(row, cIdx++, EthicsTypeUtil.fmtDate(c.getCommendDate()));
                set(row, cIdx++, EthicsTypeUtil.approvalStatusText(c.getStatus()));
                set(row, cIdx++, c.getApprover());
                set(row, cIdx++, c.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<EthicsCommendation> read(InputStream in) {
        List<EthicsCommendation> list = new ArrayList<>();
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
                    EthicsCommendation c = new EthicsCommendation();
                    c.setPhysicianName(str(row, colIndex, "医师姓名"));
                    c.setDeptName(str(row, colIndex, "科室"));
                    c.setCommendType(EthicsTypeUtil.parseCommendType(str(row, colIndex, "表彰类型")));
                    c.setCommendLevel(EthicsTypeUtil.parseCommendLevel(str(row, colIndex, "表彰级别")));
                    c.setTitle(str(row, colIndex, "表彰标题"));
                    c.setContent(str(row, colIndex, "表彰事迹"));
                    c.setGrantedBy(str(row, colIndex, "授予方"));
                    c.setCommendDate(parseDate(str(row, colIndex, "表彰日期")));
                    c.setStatus(parseApprovalStatus(str(row, colIndex, "审批状态")));
                    c.setApprover(str(row, colIndex, "审批人"));
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

    private static Integer parseApprovalStatus(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "待审批" -> EthicsTypeUtil.APPROVAL_PENDING;
            case "已通过" -> EthicsTypeUtil.APPROVAL_PASSED;
            case "已驳回" -> EthicsTypeUtil.APPROVAL_REJECTED;
            default -> null;
        };
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
