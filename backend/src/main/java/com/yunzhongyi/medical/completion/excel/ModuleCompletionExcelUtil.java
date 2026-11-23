package com.yunzhongyi.medical.completion.excel;

import com.yunzhongyi.medical.completion.entity.ModuleCompletion;
import com.yunzhongyi.medical.completion.service.CompletionTypeUtil;
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
 * 模块完成情况 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：模块编号 / 模块名称 / 分组 / 状态 / 完成进度 / 接口数 / 测试数 /
 * 完成日期 / 备注。导入时按表头文字匹配列；分组与状态导出写中文，
 * 导入时中文与编码都认；接口数 / 测试数为空表示计入其他模块。
 */
@Slf4j
public final class ModuleCompletionExcelUtil {

    private ModuleCompletionExcelUtil() {
    }

    private static final String[] HEADERS = {
            "模块编号", "模块名称", "分组", "状态", "完成进度", "接口数", "测试数",
            "完成日期", "备注"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出模块完成情况 */
    public static void write(List<ModuleCompletion> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("模块完成情况");
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
            int rowNum = 1;
            for (ModuleCompletion r : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                setNum(row, cIdx++, r.getModuleNo());
                set(row, cIdx++, r.getModuleName());
                set(row, cIdx++, r.getCategory());
                set(row, cIdx++, CompletionTypeUtil.statusText(r.getStatus()));
                setNum(row, cIdx++, r.getProgress());
                setNum(row, cIdx++, r.getInterfaceCount());
                setNum(row, cIdx++, r.getTestCount());
                set(row, cIdx++, r.getCompletedDate() == null ? ""
                        : r.getCompletedDate().format(DATE_FMT));
                set(row, cIdx++, r.getRemark());
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
            Sheet sheet = wb.createSheet("模块完成情况");
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
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    /** 读取导入的模块完成情况行（只做解析，校验与去重由服务端做） */
    public static List<ModuleCompletion> read(InputStream in) {
        List<ModuleCompletion> list = new ArrayList<>();
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
                    ModuleCompletion m = new ModuleCompletion();
                    m.setModuleNo(parseInt(str(row, colIndex, "模块编号")));
                    m.setModuleName(str(row, colIndex, "模块名称"));
                    m.setCategory(CompletionTypeUtil.parseCategory(str(row, colIndex, "分组")));
                    m.setStatus(CompletionTypeUtil.parseStatus(str(row, colIndex, "状态")));
                    m.setProgress(parseInt(str(row, colIndex, "完成进度")));
                    m.setInterfaceCount(parseInt(str(row, colIndex, "接口数")));
                    m.setTestCount(parseInt(str(row, colIndex, "测试数")));
                    m.setCompletedDate(parseDate(str(row, colIndex, "完成日期")));
                    m.setRemark(str(row, colIndex, "备注"));
                    list.add(m);
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

    private static Integer parseInt(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return (int) Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
