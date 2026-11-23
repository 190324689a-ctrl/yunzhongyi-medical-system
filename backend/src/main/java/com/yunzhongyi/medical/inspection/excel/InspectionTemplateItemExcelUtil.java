package com.yunzhongyi.medical.inspection.excel;

import com.yunzhongyi.medical.inspection.entity.InspectionTemplateItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板检查项配置 Excel 导入导出（Apache POI），仿照 MedicalTechExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 导入以“项目编号”关联检查项目库，权重列为该项在模板里的权重。
 */
@Slf4j
public final class InspectionTemplateItemExcelUtil {

    private InspectionTemplateItemExcelUtil() {
    }

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "模板名称", "项目编号", "项目名称", "权重", "排序号", "备注"
    };

    public static void write(List<InspectionTemplateItem> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("模板检查项");
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
            for (InspectionTemplateItem p : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, p.getTemplateName());
                set(row, cIdx++, p.getItemNo());
                set(row, cIdx++, p.getItemName());
                set(row, cIdx++, p.getWeight() == null ? "" : p.getWeight().stripTrailingZeros().toPlainString());
                set(row, cIdx++, p.getSortOrder() == null ? "" : String.valueOf(p.getSortOrder()));
                set(row, cIdx++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<InspectionTemplateItem> read(InputStream in) {
        List<InspectionTemplateItem> list = new ArrayList<>();
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
                    InspectionTemplateItem p = new InspectionTemplateItem();
                    p.setTemplateName(str(row, colIndex, "模板名称"));
                    p.setItemNo(str(row, colIndex, "项目编号"));
                    p.setItemName(str(row, colIndex, "项目名称"));
                    p.setWeight(parseDecimal(str(row, colIndex, "权重")));
                    p.setSortOrder(parseInt(str(row, colIndex, "排序号")));
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

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            try {
                return (int) Math.round(Double.parseDouble(s.trim()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
