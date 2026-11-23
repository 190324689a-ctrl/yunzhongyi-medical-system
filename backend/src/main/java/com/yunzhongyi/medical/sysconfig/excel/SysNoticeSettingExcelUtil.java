package com.yunzhongyi.medical.sysconfig.excel;

import com.yunzhongyi.medical.sysconfig.entity.SysNoticeSetting;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
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
 * 消息提醒设置 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：设置编码 / 设置名称 / 提醒渠道 / 触发事件 / 消息模板 / 接收范围 /
 * 接收对象 / 是否启用 / 备注。
 * 导入时按表头文字匹配列；渠道与接收范围导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class SysNoticeSettingExcelUtil {

    private SysNoticeSettingExcelUtil() {
    }

    private static final String[] HEADERS = {
            "设置编码", "设置名称", "提醒渠道", "触发事件", "消息模板", "接收范围",
            "接收对象", "是否启用", "备注"
    };

    public static void write(List<SysNoticeSetting> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("消息提醒设置");
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
            for (SysNoticeSetting s : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, s.getSettingKey());
                set(row, c++, s.getSettingName());
                set(row, c++, SysConfigTypeUtil.channelText(s.getChannel()));
                set(row, c++, s.getTriggerEvent());
                set(row, c++, s.getTemplateContent());
                set(row, c++, SysConfigTypeUtil.receiverScopeText(s.getReceiverScope()));
                set(row, c++, s.getReceiverValue());
                set(row, c++, Integer.valueOf(1).equals(s.getEnabled()) ? "启用" : "停用");
                set(row, c++, s.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("消息提醒设置");
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

    public static List<SysNoticeSetting> read(InputStream in) {
        List<SysNoticeSetting> list = new ArrayList<>();
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
                    SysNoticeSetting s = new SysNoticeSetting();
                    s.setSettingKey(str(row, colIndex, "设置编码"));
                    s.setSettingName(str(row, colIndex, "设置名称"));
                    s.setChannel(SysConfigTypeUtil.parseChannel(str(row, colIndex, "提醒渠道")));
                    s.setTriggerEvent(str(row, colIndex, "触发事件"));
                    s.setTemplateContent(str(row, colIndex, "消息模板"));
                    s.setReceiverScope(SysConfigTypeUtil.parseReceiverScope(str(row, colIndex, "接收范围")));
                    s.setReceiverValue(str(row, colIndex, "接收对象"));
                    s.setEnabled(parseEnabled(str(row, colIndex, "是否启用")));
                    s.setRemark(str(row, colIndex, "备注"));
                    list.add(s);
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

    private static Integer parseEnabled(String text) {
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
