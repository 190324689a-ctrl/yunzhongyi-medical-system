package com.yunzhongyi.medical.notification.excel;

import com.yunzhongyi.medical.notification.entity.SiteMessage;
import com.yunzhongyi.medical.notification.service.NotificationTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：标题 / 内容 / 分类 / 发送人 / 接收范围 / 接收对象 / 接收人 /
 * 已读状态 / 发送时间 / 关联业务类型 / 关联业务 id / 备注。
 * 导入时按表头文字匹配列；分类、接收范围与已读状态导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class SiteMessageExcelUtil {

    private SiteMessageExcelUtil() {
    }

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] HEADERS = {
            "标题", "内容", "分类", "发送人", "接收范围",
            "接收对象", "接收人", "已读状态", "发送时间", "关联业务类型", "关联业务 id", "备注"
    };

    public static void write(List<SiteMessage> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("站内消息");
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
            for (SiteMessage m : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, m.getTitle());
                set(row, c++, m.getContent());
                set(row, c++, NotificationTypeUtil.messageCategoryText(m.getCategory()));
                set(row, c++, m.getSenderName());
                set(row, c++, NotificationTypeUtil.receiverScopeText(m.getReceiverScope()));
                set(row, c++, m.getReceiverValue());
                set(row, c++, m.getReceiverName());
                set(row, c++, NotificationTypeUtil.readStatusText(m.getReadStatus()));
                set(row, c++, m.getSendTime() == null ? "" : m.getSendTime().format(DATETIME_FORMAT));
                set(row, c++, m.getRelatedType());
                set(row, c++, m.getRelatedId() == null ? "" : String.valueOf(m.getRelatedId()));
                set(row, c++, m.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("站内消息");
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

    public static List<SiteMessage> read(InputStream in) {
        List<SiteMessage> list = new ArrayList<>();
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
                    SiteMessage m = new SiteMessage();
                    m.setTitle(str(row, colIndex, "标题"));
                    m.setContent(str(row, colIndex, "内容"));
                    m.setCategory(NotificationTypeUtil.parseMessageCategory(str(row, colIndex, "分类")));
                    m.setSenderName(str(row, colIndex, "发送人"));
                    m.setReceiverScope(NotificationTypeUtil.parseReceiverScope(str(row, colIndex, "接收范围")));
                    m.setReceiverValue(str(row, colIndex, "接收对象"));
                    m.setReceiverName(str(row, colIndex, "接收人"));
                    m.setReadStatus(NotificationTypeUtil.parseReadStatus(str(row, colIndex, "已读状态")));
                    String sendTime = str(row, colIndex, "发送时间");
                    if (sendTime != null && !sendTime.isBlank()) {
                        m.setSendTime(LocalDateTime.parse(sendTime, DATETIME_FORMAT));
                    }
                    m.setRelatedType(str(row, colIndex, "关联业务类型"));
                    String relatedId = str(row, colIndex, "关联业务 id");
                    if (relatedId != null && !relatedId.isBlank()) {
                        m.setRelatedId(Long.parseLong(relatedId));
                    }
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
