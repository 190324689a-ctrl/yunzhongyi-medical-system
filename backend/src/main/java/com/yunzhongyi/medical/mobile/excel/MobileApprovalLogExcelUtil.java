package com.yunzhongyi.medical.mobile.excel;

import com.yunzhongyi.medical.mobile.entity.MobileApprovalLog;
import com.yunzhongyi.medical.mobile.service.MobileTypeUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 移动端审批操作记录 Excel 导出（Apache POI，仅导出）。
 * <p>
 * 导出列：业务模块 / 关联业务 id / 业务标题 / 操作 / 审批人 / 设备编码 /
 * 审批意见 / 结果 / 失败原因 / 操作时间 / 备注。
 */
public final class MobileApprovalLogExcelUtil {

    private MobileApprovalLogExcelUtil() {
    }

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] HEADERS = {
            "业务模块", "关联业务 id", "业务标题", "操作", "审批人", "设备编码",
            "审批意见", "结果", "失败原因", "操作时间", "备注"
    };

    public static void write(List<MobileApprovalLog> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("移动端审批记录");
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
            for (MobileApprovalLog l : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, MobileTypeUtil.moduleText(l.getModuleType()));
                set(row, c++, l.getRelatedId() == null ? "" : String.valueOf(l.getRelatedId()));
                set(row, c++, l.getRelatedTitle());
                set(row, c++, MobileTypeUtil.actionText(l.getAction()));
                set(row, c++, l.getOperator());
                set(row, c++, l.getDeviceCode());
                set(row, c++, l.getOpinion());
                set(row, c++, MobileTypeUtil.resultText(l.getResult()));
                set(row, c++, l.getFailReason());
                set(row, c++, l.getActionTime() == null ? "" : l.getActionTime().format(DATE_TIME_FORMAT));
                set(row, c++, l.getRemark());
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
