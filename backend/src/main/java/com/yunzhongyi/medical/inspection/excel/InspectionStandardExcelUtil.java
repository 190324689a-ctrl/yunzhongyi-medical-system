package com.yunzhongyi.medical.inspection.excel;

import com.yunzhongyi.medical.inspection.entity.InspectionStandard;
import com.yunzhongyi.medical.inspection.service.InspectionTypeUtil;
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
 * 检查标准 Excel 导入导出（Apache POI），仿照 MedicalTechExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 标准级别/状态导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class InspectionStandardExcelUtil {

    private InspectionStandardExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "标准编号", "标准名称", "标准级别", "依据文件", "标准内容", "适用范围",
            "生效日期", "有效期", "状态", "废止原因", "备注"
    };

    public static void write(List<InspectionStandard> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("检查标准");
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
            for (InspectionStandard p : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, p.getStandardNo());
                set(row, cIdx++, p.getStandardName());
                set(row, cIdx++, InspectionTypeUtil.levelText(p.getLevel()));
                set(row, cIdx++, p.getSourceDoc());
                set(row, cIdx++, p.getContent());
                set(row, cIdx++, p.getApplicableScope());
                set(row, cIdx++, InspectionTypeUtil.fmtDate(p.getEffectiveDate()));
                set(row, cIdx++, InspectionTypeUtil.fmtDate(p.getExpiryDate()));
                set(row, cIdx++, InspectionTypeUtil.standardStatusText(p.getStatus()));
                set(row, cIdx++, p.getAbolishReason());
                set(row, cIdx++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<InspectionStandard> read(InputStream in) {
        List<InspectionStandard> list = new ArrayList<>();
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
                    InspectionStandard p = new InspectionStandard();
                    p.setStandardNo(str(row, colIndex, "标准编号"));
                    p.setStandardName(str(row, colIndex, "标准名称"));
                    p.setLevel(InspectionTypeUtil.parseLevel(str(row, colIndex, "标准级别")));
                    p.setSourceDoc(str(row, colIndex, "依据文件"));
                    p.setContent(str(row, colIndex, "标准内容"));
                    p.setApplicableScope(str(row, colIndex, "适用范围"));
                    p.setEffectiveDate(parseDate(str(row, colIndex, "生效日期")));
                    p.setExpiryDate(parseDate(str(row, colIndex, "有效期")));
                    Integer status = InspectionTypeUtil.parseStandardStatus(str(row, colIndex, "状态"));
                    p.setStatus(status == null ? InspectionTypeUtil.STANDARD_STATUS_CURRENT : status);
                    p.setAbolishReason(str(row, colIndex, "废止原因"));
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
