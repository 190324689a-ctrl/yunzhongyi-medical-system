package com.yunzhongyi.medical.notification.excel;

import com.yunzhongyi.medical.notification.entity.TodoReminder;
import com.yunzhongyi.medical.notification.service.NotificationTypeUtil;
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
 * 待办提醒 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：标题 / 内容 / 待办类型 / 关联业务类型 / 关联业务 id / 责任人 /
 * 责任人姓名 / 责任科室 / 截止日期 / 状态 / 来源 / 提醒设置编码 / 备注。
 * 导入时按表头文字匹配列；待办类型、状态与来源导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class TodoReminderExcelUtil {

    private TodoReminderExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] HEADERS = {
            "标题", "内容", "待办类型", "关联业务类型", "关联业务 id", "责任人",
            "责任人姓名", "责任科室", "截止日期", "状态", "来源", "提醒设置编码", "备注"
    };

    public static void write(List<TodoReminder> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("待办提醒");
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
            for (TodoReminder t : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, t.getTitle());
                set(row, c++, t.getContent());
                set(row, c++, NotificationTypeUtil.todoTypeText(t.getTodoType()));
                set(row, c++, t.getRelatedType());
                set(row, c++, t.getRelatedId() == null ? "" : String.valueOf(t.getRelatedId()));
                set(row, c++, t.getAssignee());
                set(row, c++, t.getAssigneeName());
                set(row, c++, t.getDeptName());
                set(row, c++, t.getDueDate() == null ? "" : t.getDueDate().format(DATE_FORMAT));
                set(row, c++, NotificationTypeUtil.todoStatusText(t.getStatus()));
                set(row, c++, NotificationTypeUtil.todoSourceText(t.getSource()));
                set(row, c++, t.getSettingKey());
                set(row, c++, t.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("待办提醒");
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

    public static List<TodoReminder> read(InputStream in) {
        List<TodoReminder> list = new ArrayList<>();
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
                    TodoReminder t = new TodoReminder();
                    t.setTitle(str(row, colIndex, "标题"));
                    t.setContent(str(row, colIndex, "内容"));
                    t.setTodoType(NotificationTypeUtil.parseTodoType(str(row, colIndex, "待办类型")));
                    t.setRelatedType(str(row, colIndex, "关联业务类型"));
                    String relatedId = str(row, colIndex, "关联业务 id");
                    if (relatedId != null && !relatedId.isBlank()) {
                        t.setRelatedId(Long.parseLong(relatedId));
                    }
                    t.setAssignee(str(row, colIndex, "责任人"));
                    t.setAssigneeName(str(row, colIndex, "责任人姓名"));
                    t.setDeptName(str(row, colIndex, "责任科室"));
                    String dueDate = str(row, colIndex, "截止日期");
                    if (dueDate != null && !dueDate.isBlank()) {
                        t.setDueDate(LocalDate.parse(dueDate, DATE_FORMAT));
                    }
                    t.setStatus(NotificationTypeUtil.parseTodoStatus(str(row, colIndex, "状态")));
                    t.setSource(NotificationTypeUtil.parseTodoSource(str(row, colIndex, "来源")));
                    t.setSettingKey(str(row, colIndex, "提醒设置编码"));
                    t.setRemark(str(row, colIndex, "备注"));
                    list.add(t);
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
