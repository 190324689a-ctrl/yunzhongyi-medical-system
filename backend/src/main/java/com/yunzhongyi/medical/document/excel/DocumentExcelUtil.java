package com.yunzhongyi.medical.document.excel;

import com.yunzhongyi.medical.document.entity.Document;
import com.yunzhongyi.medical.document.service.DocumentTypeUtil;
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
 * 文件制度 Excel 导入导出（Apache POI），仿照 CommitteeExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 文件分类/状态导出写中文（如“制度”“已发布”），导入时中文与编码都认。
 * 正文内容不进 Excel（过长），导入后在页面补录正文再发布。
 */
@Slf4j
public final class DocumentExcelUtil {

    private DocumentExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "文件编号", "文件标题", "分类", "版本", "归口科室", "起草人", "审批人",
            "发布日期", "生效日期", "失效日期", "状态", "备注"
    };

    public static void write(List<Document> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("文件制度");
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
            for (Document d : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, d.getDocNo());
                set(row, cIdx++, d.getTitle());
                set(row, cIdx++, DocumentTypeUtil.categoryText(d.getCategory()));
                set(row, cIdx++, d.getVersion());
                set(row, cIdx++, d.getDepartment());
                set(row, cIdx++, d.getDrafter());
                set(row, cIdx++, d.getApprover());
                set(row, cIdx++, DocumentTypeUtil.fmtDate(d.getPublishDate()));
                set(row, cIdx++, DocumentTypeUtil.fmtDate(d.getEffectiveDate()));
                set(row, cIdx++, DocumentTypeUtil.fmtDate(d.getExpiryDate()));
                set(row, cIdx++, DocumentTypeUtil.statusText(d.getStatus()));
                set(row, cIdx++, d.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<Document> read(InputStream in) {
        List<Document> list = new ArrayList<>();
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
                    Document d = new Document();
                    d.setDocNo(str(row, colIndex, "文件编号"));
                    d.setTitle(str(row, colIndex, "文件标题"));
                    d.setCategory(DocumentTypeUtil.parseCategory(str(row, colIndex, "分类")));
                    d.setVersion(str(row, colIndex, "版本"));
                    d.setDepartment(str(row, colIndex, "归口科室"));
                    d.setDrafter(str(row, colIndex, "起草人"));
                    d.setApprover(str(row, colIndex, "审批人"));
                    d.setPublishDate(parseDate(str(row, colIndex, "发布日期")));
                    d.setEffectiveDate(parseDate(str(row, colIndex, "生效日期")));
                    d.setExpiryDate(parseDate(str(row, colIndex, "失效日期")));
                    Integer status = DocumentTypeUtil.parseStatus(str(row, colIndex, "状态"));
                    d.setStatus(status == null ? DocumentTypeUtil.STATUS_DRAFT : status);
                    d.setRemark(str(row, colIndex, "备注"));
                    list.add(d);
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
