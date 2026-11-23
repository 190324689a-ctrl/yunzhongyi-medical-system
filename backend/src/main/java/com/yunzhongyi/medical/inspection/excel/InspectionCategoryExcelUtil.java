package com.yunzhongyi.medical.inspection.excel;

import com.yunzhongyi.medical.inspection.entity.InspectionCategory;
import com.yunzhongyi.medical.inspection.service.InspectionTypeUtil;
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
 * 检查分类 Excel 导入导出（Apache POI），仿照 InspectionStandardExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 适用检查类型/状态导出写中文，导入时中文与编码都认；
 * 上级分类导出写编码，导入时按编码解析（查不到跳过该行）。
 */
@Slf4j
public final class InspectionCategoryExcelUtil {

    private InspectionCategoryExcelUtil() {
    }

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "分类编码", "分类名称", "上级分类编码", "适用检查类型", "分类说明",
            "状态", "排序号", "备注"
    };

    public static void write(List<InspectionCategory> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("检查分类");
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
            for (InspectionCategory c : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, c.getCategoryCode());
                set(row, cIdx++, c.getCategoryName());
                set(row, cIdx++, c.getParentCode());
                set(row, cIdx++, InspectionTypeUtil.applicableTypeText(c.getApplicableType()));
                set(row, cIdx++, c.getDescription());
                set(row, cIdx++, InspectionTypeUtil.itemStatusText(c.getStatus()));
                setNum(row, cIdx++, c.getSortOrder());
                set(row, cIdx++, c.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<InspectionCategory> read(InputStream in) {
        List<InspectionCategory> list = new ArrayList<>();
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
                    InspectionCategory c = new InspectionCategory();
                    c.setCategoryCode(str(row, colIndex, "分类编码"));
                    c.setCategoryName(str(row, colIndex, "分类名称"));
                    c.setParentCode(str(row, colIndex, "上级分类编码"));
                    c.setApplicableType(InspectionTypeUtil.parseApplicableType(str(row, colIndex, "适用检查类型")));
                    c.setDescription(str(row, colIndex, "分类说明"));
                    Integer status = InspectionTypeUtil.parseItemStatus(str(row, colIndex, "状态"));
                    c.setStatus(status == null ? InspectionTypeUtil.ITEM_STATUS_ENABLED : status);
                    c.setSortOrder(parseInt(str(row, colIndex, "排序号")));
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

    private static void setNum(Row row, int col, Integer v) {
        Cell cell = row.createCell(col);
        if (v != null) {
            cell.setCellValue(v);
        } else {
            cell.setCellValue("");
        }
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
}
