package com.yunzhongyi.medical.notification.excel;

import com.yunzhongyi.medical.notification.entity.PushRecord;
import com.yunzhongyi.medical.notification.service.NotificationTypeUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 消息推送记录 Excel 导出（Apache POI，仅导出：推送记录由发送动作产生，不支持批量导入）。
 * <p>
 * 导出列：标题 / 内容 / 推送渠道 / 接收范围 / 接收对象 / 接收人 /
 * 发送状态 / 发送时间 / 失败原因 / 关联站内消息 id / 备注。
 */
public final class PushRecordExcelUtil {

    private PushRecordExcelUtil() {
    }

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] HEADERS = {
            "标题", "内容", "推送渠道", "接收范围", "接收对象", "接收人",
            "发送状态", "发送时间", "失败原因", "关联站内消息 id", "备注"
    };

    public static void write(List<PushRecord> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("消息推送记录");
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
            for (PushRecord p : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, p.getTitle());
                set(row, c++, p.getContent());
                set(row, c++, NotificationTypeUtil.pushChannelText(p.getChannel()));
                set(row, c++, NotificationTypeUtil.receiverScopeText(p.getReceiverScope()));
                set(row, c++, p.getReceiverValue());
                set(row, c++, p.getReceiverName());
                set(row, c++, NotificationTypeUtil.sendStatusText(p.getSendStatus()));
                set(row, c++, p.getSendTime() == null ? "" : p.getSendTime().format(DATETIME_FORMAT));
                set(row, c++, p.getFailReason());
                set(row, c++, p.getRelatedMessageId() == null ? "" : String.valueOf(p.getRelatedMessageId()));
                set(row, c++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    private static void set(Row row, int col, String v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v == null ? "" : v);
    }
}
