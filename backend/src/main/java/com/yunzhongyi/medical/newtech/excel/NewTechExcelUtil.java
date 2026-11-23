package com.yunzhongyi.medical.newtech.excel;

import com.yunzhongyi.medical.newtech.entity.NewTechProject;
import com.yunzhongyi.medical.newtech.service.NewTechTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 新技术、新项目 Excel 导入导出（Apache POI），仿照 MdtExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 项目类型/伦理审查状态/审批状态导出写中文（如“新技术”“待审查”“已立项”），
 * 导入时中文与编码都认。
 */
@Slf4j
public final class NewTechExcelUtil {

    private NewTechExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "项目名称", "项目类型", "申报科室", "申报人", "项目负责人", "项目组成员",
            "预算（万元）", "计划开始日期", "计划结束日期", "需伦理审查", "伦理审查状态",
            "审批状态", "审批意见", "备注"
    };

    public static void write(List<NewTechProject> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("新技术新项目");
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
            for (NewTechProject p : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, p.getProjectName());
                set(row, c++, NewTechTypeUtil.typeText(p.getProjectType()));
                set(row, c++, p.getDeptName());
                set(row, c++, p.getApplicantName());
                set(row, c++, p.getProjectLeader());
                set(row, c++, p.getTeamMembers());
                set(row, c++, NewTechTypeUtil.fmtBudget(p.getBudget()));
                set(row, c++, NewTechTypeUtil.fmtDate(p.getStartDate()));
                set(row, c++, NewTechTypeUtil.fmtDate(p.getEndDate()));
                set(row, c++, Boolean.TRUE.equals(p.getEthicsRequired()) ? "是" : "否");
                set(row, c++, NewTechTypeUtil.ethicsText(p.getEthicsStatus()));
                set(row, c++, NewTechTypeUtil.statusText(p.getStatus()));
                set(row, c++, p.getApprovalOpinion());
                set(row, c++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<NewTechProject> read(InputStream in) {
        List<NewTechProject> list = new ArrayList<>();
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
                    NewTechProject p = new NewTechProject();
                    p.setProjectName(str(row, colIndex, "项目名称"));
                    p.setProjectType(NewTechTypeUtil.parseType(str(row, colIndex, "项目类型")));
                    p.setDeptName(str(row, colIndex, "申报科室"));
                    p.setApplicantName(str(row, colIndex, "申报人"));
                    p.setProjectLeader(str(row, colIndex, "项目负责人"));
                    p.setTeamMembers(str(row, colIndex, "项目组成员"));
                    p.setBudget(parseBudget(str(row, colIndex, "预算（万元）")));
                    p.setStartDate(parseDate(str(row, colIndex, "计划开始日期")));
                    p.setEndDate(parseDate(str(row, colIndex, "计划结束日期")));
                    p.setEthicsRequired(NewTechTypeUtil.parseEthicsRequired(str(row, colIndex, "需伦理审查")));
                    p.setEthicsStatus(NewTechTypeUtil.parseEthicsStatus(str(row, colIndex, "伦理审查状态")));
                    p.setApprovalOpinion(str(row, colIndex, "审批意见"));
                    p.setRemark(str(row, colIndex, "备注"));
                    // 导入的审批状态：只认“已驳回”以外的终态缺省为待审批；导入行不直接带入已立项/已验收等流转状态
                    Integer status = NewTechTypeUtil.parseStatus(str(row, colIndex, "审批状态"));
                    p.setStatus(status == null ? NewTechTypeUtil.STATUS_PENDING : status);
                    list.add(p);
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

    private static BigDecimal parseBudget(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
