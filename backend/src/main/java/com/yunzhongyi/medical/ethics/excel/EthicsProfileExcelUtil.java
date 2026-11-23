package com.yunzhongyi.medical.ethics.excel;

import com.yunzhongyi.medical.ethics.entity.EthicsProfile;
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
 * 医德医风档案 Excel 导入导出（Apache POI），仿照 PolicyExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 档案状态导出写中文（如“在档”“封存”），导入时中文与编码都认。
 * 最近考评等级导出写中文（如“优秀”），导入时中文与编码都认。
 */
@Slf4j
public final class EthicsProfileExcelUtil {

    private EthicsProfileExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "档案编号", "医师姓名", "科室", "职称", "建档日期", "状态", "最近考评等级", "最近考评年度", "备注"
    };

    public static void write(List<EthicsProfile> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医德医风档案");
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
            for (EthicsProfile p : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, p.getArchiveNo());
                set(row, cIdx++, p.getPhysicianName());
                set(row, cIdx++, p.getDeptName());
                set(row, cIdx++, p.getTitle());
                set(row, cIdx++, EthicsTypeUtil.fmtDate(p.getEstablishDate()));
                set(row, cIdx++, EthicsTypeUtil.profileStatusText(p.getStatus()));
                set(row, cIdx++, EthicsTypeUtil.gradeText(p.getLastGrade()));
                set(row, cIdx++, p.getLastEvaluateYear() == null ? "" : String.valueOf(p.getLastEvaluateYear()));
                set(row, cIdx++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<EthicsProfile> read(InputStream in) {
        List<EthicsProfile> list = new ArrayList<>();
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
                    EthicsProfile p = new EthicsProfile();
                    p.setArchiveNo(str(row, colIndex, "档案编号"));
                    p.setPhysicianName(str(row, colIndex, "医师姓名"));
                    p.setDeptName(str(row, colIndex, "科室"));
                    p.setTitle(str(row, colIndex, "职称"));
                    p.setEstablishDate(parseDate(str(row, colIndex, "建档日期")));
                    p.setStatus(parseProfileStatus(str(row, colIndex, "状态")));
                    p.setLastGrade(EthicsTypeUtil.parseGrade(str(row, colIndex, "最近考评等级")));
                    String year = str(row, colIndex, "最近考评年度");
                    p.setLastEvaluateYear(year == null ? null : Integer.parseInt(year));
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

    private static Integer parseProfileStatus(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "在档" -> EthicsTypeUtil.PROFILE_ACTIVE;
            case "封存" -> EthicsTypeUtil.PROFILE_ARCHIVED;
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
