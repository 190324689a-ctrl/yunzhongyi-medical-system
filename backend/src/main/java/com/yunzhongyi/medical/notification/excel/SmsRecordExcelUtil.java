package com.yunzhongyi.medical.notification.excel;

import com.yunzhongyi.medical.notification.entity.SmsRecord;
import com.yunzhongyi.medical.notification.service.NotificationTypeUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 短信提醒记录 Excel 导出（Apache POI，仅导出：短信记录由发送动作产生，不支持批量导入）。
 * <p>
 * 导出列：手机号 / 内容 / 接收人 / 模板编码 / 发送状态 / 发送时间 /
 * 失败原因 / 关联业务类型 / 关联业务 id / 备注。
 */
public final class SmsRecordExcelUtil {

    private SmsRecordExcelUtil() {
    }

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] HEADERS = {
            "手机号", "内容", "接收人", "模板编码", "发送状态",
            "发送时间", "失败原因", "关联业务类型", "关联业务 id", "备注"
    };

    public static void write(List<SmsRecord> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("短信提醒记录");
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
            for (SmsRecord s : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, s.getPhone());
                set(row, c++, s.getContent());
                set(row, c++, s.getReceiverName());
                set(row, c++, s.getTemplateCode());
                set(row, c++, NotificationTypeUtil.sendStatusText(s.getSendStatus()));
                set(row, c++, s.getSendTime() == null ? "" : s.getSendTime().format(DATETIME_FORMAT));
                set(row, c++, s.getFailReason());
                set(row, c++, s.getRelatedType());
                set(row, c++, s.getRelatedId() == null ? "" : String.valueOf(s.getRelatedId()));
                set(row, c++, s.getRemark());
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
