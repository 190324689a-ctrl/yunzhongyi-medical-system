package com.yunzhongyi.medical.sysconfig.excel;

import com.yunzhongyi.medical.sysconfig.entity.KnowledgeItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：知识编码 / 知识标题 / 知识分类 / 知识正文 / 关键词 / 状态 / 查阅量 / 排序值 / 备注。
 * 导入时按表头文字匹配列；查阅量导入时忽略（新建条目从 0 开始计）。
 */
@Slf4j
public final class KnowledgeItemExcelUtil {

    private KnowledgeItemExcelUtil() {
    }

    private static final String[] HEADERS = {
            "知识编码", "知识标题", "知识分类", "知识正文", "关键词", "状态", "查阅量", "排序值", "备注"
    };

    public static void write(List<KnowledgeItem> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("知识库");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            int rowNum = 1;
            for (KnowledgeItem k : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, k.getKbCode());
                set(row, c++, k.getTitle());
                set(row, c++, k.getCategory());
                set(row, c++, k.getContent());
                set(row, c++, k.getKeywords());
                set(row, c++, Integer.valueOf(1).equals(k.getStatus()) ? "启用" : "停用");
                setNum(row, c++, k.getViewCount());
                setNum(row, c++, k.getSortOrder() == null ? null : k.getSortOrder().longValue());
                set(row, c++, k.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("知识库");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<KnowledgeItem> read(InputStream in) {
        List<KnowledgeItem> list = new ArrayList<>();
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
                    KnowledgeItem k = new KnowledgeItem();
                    k.setKbCode(str(row, colIndex, "知识编码"));
                    k.setTitle(str(row, colIndex, "知识标题"));
                    k.setCategory(str(row, colIndex, "知识分类"));
                    k.setContent(str(row, colIndex, "知识正文"));
                    k.setKeywords(str(row, colIndex, "关键词"));
                    k.setStatus(parseStatus(str(row, colIndex, "状态")));
                    Long sort = parseLong(str(row, colIndex, "排序值"));
                    k.setSortOrder(sort == null ? null : sort.intValue());
                    k.setRemark(str(row, colIndex, "备注"));
                    list.add(k);
                } catch (Exception e) {
                    log.warn("导入第 {} 行解析失败，已跳过: {}", r + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 Excel 失败: " + e.getMessage(), e);
        }
        return list;
    }

    private static void set(Row row, int col, String v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v == null ? "" : v);
    }

    private static void setNum(Row row, int col, Long v) {
        Cell cell = row.createCell(col);
        if (v != null) {
            cell.setCellValue(v.doubleValue());
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

    private static String str(Row row, Map<String, Integer> colIndex, String header) {
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

    private static Long parseLong(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return (long) Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
