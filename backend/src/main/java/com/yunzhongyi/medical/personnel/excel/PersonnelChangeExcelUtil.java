package com.yunzhongyi.medical.personnel.excel;

import com.yunzhongyi.medical.personnel.entity.PersonnelChange;
import com.yunzhongyi.medical.personnel.service.PersonnelChangeTypeUtil;
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
 * 医疗人员动态 Excel 导入导出（Apache POI），仿照 NewTechExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 变动类型/审批状态导出写中文（如“调动”“待审批”），导入时中文与编码都认。
 */
@Slf4j
public final class PersonnelChangeExcelUtil {

    private PersonnelChangeExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "人员姓名", "变动类型", "原科室", "调入/新科室", "生效日期", "申请日期",
            "变动原因", "审批状态", "审批人", "审批意见", "备注"
    };

    public static void write(List<PersonnelChange> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医疗人员动态");
            // 表头
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }
            int rowNum = 1;
            for (PersonnelChange c : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, c.getPhysicianName());
                set(row, cIdx++, PersonnelChangeTypeUtil.typeText(c.getChangeType()));
                set(row, cIdx++, c.getFromDeptName());
                set(row, cIdx++, c.getToDeptName());
                set(row, cIdx++, PersonnelChangeTypeUtil.fmtDate(c.getChangeDate()));
                set(row, cIdx++, PersonnelChangeTypeUtil.fmtDate(c.getApplyDate()));
                set(row, cIdx++, c.getReason());
                set(row, cIdx++, PersonnelChangeTypeUtil.statusText(c.getStatus()));
                set(row, cIdx++, c.getApprover());
                set(row, cIdx++, c.getApprovalOpinion());
                set(row, cIdx++, c.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<PersonnelChange> read(InputStream in) {
        List<PersonnelChange> list = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return list;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return list;
            }
            // 按表头文字定位列下标
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
                    PersonnelChange c = new PersonnelChange();
                    c.setPhysicianName(str(row, colIndex, "人员姓名"));
                    c.setChangeType(PersonnelChangeTypeUtil.parseType(str(row, colIndex, "变动类型")));
                    c.setFromDeptName(str(row, colIndex, "原科室"));
                    c.setToDeptName(str(row, colIndex, "调入/新科室"));
                    c.setChangeDate(parseDate(str(row, colIndex, "生效日期")));
                    c.setApplyDate(parseDate(str(row, colIndex, "申请日期")));
                    c.setReason(str(row, colIndex, "变动原因"));
                    c.setApprover(str(row, colIndex, "审批人"));
                    c.setApprovalOpinion(str(row, colIndex, "审批意见"));
                    c.setRemark(str(row, colIndex, "备注"));
                    // 导入的审批状态：认不出时默认待审批；导入行不直接带入审批流转结果
                    Integer status = PersonnelChangeTypeUtil.parseStatus(str(row, colIndex, "审批状态"));
                    c.setStatus(status == null ? PersonnelChangeTypeUtil.STATUS_PENDING : status);
                    list.add(c);
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
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            // 纯数字按整数文本读，避免科学计数法
            long v = Math.round(cell.getNumericCellValue());
            return String.valueOf(v);
        }
        String s = cell.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        try {
            return LocalDate.parse(t, DATE_FMT);
        } catch (Exception e) {
            try {
                return LocalDate.parse(t);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
