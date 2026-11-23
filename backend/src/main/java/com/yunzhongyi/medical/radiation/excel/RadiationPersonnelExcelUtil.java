package com.yunzhongyi.medical.radiation.excel;

import com.yunzhongyi.medical.radiation.entity.RadiationPersonnel;
import com.yunzhongyi.medical.radiation.service.RadiationTypeUtil;
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
 * 放射人员档案 Excel 导入导出（Apache POI），仿照 PersonnelChangeExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 放射类别/在岗状态导出写中文（如“放射诊断”“在岗”），导入时中文与编码都认。
 */
@Slf4j
public final class RadiationPersonnelExcelUtil {

    private RadiationPersonnelExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "姓名", "性别", "身份证号", "联系电话", "科室", "职称", "放射类别",
            "放射工作人员证号", "放射诊疗许可证号", "许可证有效期至", "从事放射工作日期", "在岗状态", "备注"
    };

    public static void write(List<RadiationPersonnel> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("放射人员档案");
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
            for (RadiationPersonnel p : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, p.getName());
                set(row, cIdx++, "M".equals(p.getGender()) ? "男" : "F".equals(p.getGender()) ? "女" : "");
                set(row, cIdx++, p.getIdCard());
                set(row, cIdx++, p.getPhone());
                set(row, cIdx++, p.getDeptName());
                set(row, cIdx++, p.getTitle());
                set(row, cIdx++, RadiationTypeUtil.workTypeText(p.getWorkType()));
                set(row, cIdx++, p.getLicenseNo());
                set(row, cIdx++, p.getPermitNo());
                set(row, cIdx++, RadiationTypeUtil.fmtDate(p.getPermitExpiry()));
                set(row, cIdx++, RadiationTypeUtil.fmtDate(p.getEntryDate()));
                set(row, cIdx++, RadiationTypeUtil.statusText(p.getStatus()));
                set(row, cIdx++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<RadiationPersonnel> read(InputStream in) {
        List<RadiationPersonnel> list = new ArrayList<>();
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
                    RadiationPersonnel p = new RadiationPersonnel();
                    p.setName(str(row, colIndex, "姓名"));
                    String gender = str(row, colIndex, "性别");
                    p.setGender("男".equals(gender) ? "M" : "女".equals(gender) ? "F" : null);
                    p.setIdCard(str(row, colIndex, "身份证号"));
                    p.setPhone(str(row, colIndex, "联系电话"));
                    p.setDeptName(str(row, colIndex, "科室"));
                    p.setTitle(str(row, colIndex, "职称"));
                    p.setWorkType(RadiationTypeUtil.parseWorkType(str(row, colIndex, "放射类别")));
                    p.setLicenseNo(str(row, colIndex, "放射工作人员证号"));
                    p.setPermitNo(str(row, colIndex, "放射诊疗许可证号"));
                    p.setPermitExpiry(parseDate(str(row, colIndex, "许可证有效期至")));
                    p.setEntryDate(parseDate(str(row, colIndex, "从事放射工作日期")));
                    Integer status = RadiationTypeUtil.parseStatus(str(row, colIndex, "在岗状态"));
                    p.setStatus(status == null ? RadiationTypeUtil.STATUS_ACTIVE : status);
                    p.setRemark(str(row, colIndex, "备注"));
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
}
