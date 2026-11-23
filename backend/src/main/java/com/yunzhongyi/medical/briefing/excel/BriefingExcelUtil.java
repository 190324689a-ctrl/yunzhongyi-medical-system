package com.yunzhongyi.medical.briefing.excel;

import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.briefing.service.BriefingTypeUtil;
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
 * 医疗工作简报 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：期号 / 标题 / 模板 / 分类 / 统计周期 / 正文 / 状态 / 撰稿人 /
 * 发布日期 / 审批人 / 审批意见 / 备注。
 * 导入时按表头文字匹配列；分类与状态导出写中文，导入时中文与编码都认；
 * 导入的简报均为草稿，需走提交审批流程。
 */
@Slf4j
public final class BriefingExcelUtil {

    private BriefingExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] HEADERS = {
            "期号", "标题", "模板", "分类", "统计周期", "正文",
            "状态", "撰稿人", "发布日期", "审批人", "审批意见", "备注"
    };

    public static void write(List<Briefing> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医疗工作简报");
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
            for (Briefing b : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, b.getIssueNo());
                set(row, c++, b.getTitle());
                set(row, c++, b.getTemplateName());
                set(row, c++, BriefingTypeUtil.categoryText(b.getCategory()));
                set(row, c++, b.getPeriod());
                set(row, c++, b.getContent());
                set(row, c++, BriefingTypeUtil.statusText(b.getStatus()));
                set(row, c++, b.getAuthorName());
                set(row, c++, b.getPublishDate() == null ? "" : b.getPublishDate().format(DATE_FORMAT));
                set(row, c++, b.getApprover());
                set(row, c++, b.getApprovalOpinion());
                set(row, c++, b.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医疗工作简报");
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

    public static List<Briefing> read(InputStream in) {
        List<Briefing> list = new ArrayList<>();
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
                    Briefing b = new Briefing();
                    b.setIssueNo(str(row, colIndex, "期号"));
                    b.setTitle(str(row, colIndex, "标题"));
                    b.setTemplateName(str(row, colIndex, "模板"));
                    b.setCategory(BriefingTypeUtil.parseCategory(str(row, colIndex, "分类")));
                    b.setPeriod(str(row, colIndex, "统计周期"));
                    b.setContent(str(row, colIndex, "正文"));
                    b.setStatus(BriefingTypeUtil.parseStatus(str(row, colIndex, "状态")));
                    b.setAuthorName(str(row, colIndex, "撰稿人"));
                    String publishDate = str(row, colIndex, "发布日期");
                    if (publishDate != null && !publishDate.isBlank()) {
                        b.setPublishDate(LocalDate.parse(publishDate, DATE_FORMAT));
                    }
                    b.setApprover(str(row, colIndex, "审批人"));
                    b.setApprovalOpinion(str(row, colIndex, "审批意见"));
                    b.setRemark(str(row, colIndex, "备注"));
                    list.add(b);
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
}
