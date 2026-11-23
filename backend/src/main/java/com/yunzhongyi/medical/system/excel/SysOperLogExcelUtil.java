package com.yunzhongyi.medical.system.excel;

import com.yunzhongyi.medical.system.entity.SysOperLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志 Excel 导出（Apache POI，仅导出，不导入：日志只读）。
 * <p>
 * 导出列：操作人 / 模块 / 操作 / 请求方法 / 参数摘要 / 结果 / 耗时（毫秒） / 错误信息 / 操作时间。
 */
@Slf4j
public final class SysOperLogExcelUtil {

    private SysOperLogExcelUtil() {
    }

    private static final String[] HEADERS = {
            "操作人", "模块", "操作", "请求方法", "参数摘要", "结果", "耗时（毫秒）", "错误信息", "操作时间"
    };

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void write(List<SysOperLog> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("操作日志");
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
            for (SysOperLog log : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, log.getUsername());
                set(row, c++, log.getModule());
                set(row, c++, log.getOperation());
                set(row, c++, log.getMethod());
                set(row, c++, log.getParams());
                set(row, c++, Integer.valueOf(1).equals(log.getResult()) ? "成功" : "失败");
                setNum(row, c++, log.getCostMs());
                set(row, c++, log.getErrorMsg());
                set(row, c++, log.getCreateTime() == null ? "" : log.getCreateTime().format(DT_FMT));
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

    private static void setNum(Row row, int col, Long v) {
        Cell cell = row.createCell(col);
        if (v != null) {
            cell.setCellValue(v.doubleValue());
        } else {
            cell.setCellValue("");
        }
    }
}
