package com.yunzhongyi.medical.templatelib.excel;

import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 检查模板 Excel 导入导出（Apache POI），仿照 InspectionCategoryExcelUtil。
 * <p>
 * 导出列：模板名称 / 检查类型 / 检查项 JSON / 状态 / 备注。
 * 导入时按表头文字匹配列；检查类型与状态导出写中文，导入时中文与编码都认；
 * 检查项 JSON 原样导入，权重之和须为 100（服务端导入时再校验，不合规的行跳过）。
 */
@Slf4j
public final class TemplateLibraryExcelUtil {

    private TemplateLibraryExcelUtil() {
    }

    /** 导出 / 导入列：标题 */
    private static final String[] HEADERS = {
            "模板名称", "检查类型", "检查项 JSON", "状态", "备注"
    };

    /** 导出全部模板 */
    public static void write(List<QualityTemplate> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("检查模板");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            sheet.setColumnWidth(2, 60 * 256);
            int rowNum = 1;
            for (QualityTemplate t : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, t.getTemplateName());
                set(row, cIdx++, QualityTypeUtil.typeText(t.getInspectType()));
                set(row, cIdx++, t.getCheckItems());
                set(row, cIdx++, Integer.valueOf(1).equals(t.getStatus()) ? "启用" : "停用");
                set(row, cIdx++, t.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("检查模板");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            sheet.setColumnWidth(2, 60 * 256);
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /** 读取导入的模板行（只做解析，校验与去重由服务端做） */
    public static List<QualityTemplate> read(InputStream in) {
        List<QualityTemplate> list = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return list;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return list;
            }
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
                    QualityTemplate t = new QualityTemplate();
                    t.setTemplateName(str(row, colIndex, "模板名称"));
                    t.setInspectType(QualityTypeUtil.parseType(str(row, colIndex, "检查类型")));
                    t.setCheckItems(str(row, colIndex, "检查项 JSON"));
                    t.setStatus(parseStatus(str(row, colIndex, "状态")));
                    t.setRemark(str(row, colIndex, "备注"));
                    list.add(t);
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
        return cell.toString().trim();
    }

    /** 状态中文 / 编码 → 编码：启用 1 / 停用 0，认不出时返回 null（服务端默认启用） */
    private static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "启用", "1" -> 1;
            case "停用", "0" -> 0;
            default -> null;
        };
    }
}
