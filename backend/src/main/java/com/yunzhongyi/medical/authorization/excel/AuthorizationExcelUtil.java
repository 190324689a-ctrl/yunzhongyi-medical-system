package com.yunzhongyi.medical.authorization.excel;

import com.yunzhongyi.medical.authorization.entity.Authorization;
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
 * 重点授权 Excel 导入导出（Apache POI）
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 */
@Slf4j
public final class AuthorizationExcelUtil {

    private AuthorizationExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "申请人", "科室", "授权类型", "手术分级", "有效期开始", "有效期结束",
            "申请事由", "审批状态", "当前审批", "审批意见", "审批记录", "申请日期", "备注"
    };

    public static void write(List<Authorization> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("重点授权");
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
            for (Authorization a : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, a.getPhysicianName());
                set(row, c++, a.getDeptName());
                set(row, c++, typeText(a.getAuthType()));
                set(row, c++, a.getSurgeryLevel() == null ? "" : String.valueOf(a.getSurgeryLevel()));
                set(row, c++, fmtDate(a.getValidFrom()));
                set(row, c++, fmtDate(a.getValidTo()));
                set(row, c++, a.getReason());
                set(row, c++, statusText(a.getStatus()));
                set(row, c++, a.getCurrentApprover());
                set(row, c++, a.getApprovalOpinion());
                set(row, c++, a.getApprovalHistory());
                set(row, c++, fmtDate(a.getApplyDate()));
                set(row, c++, a.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<Authorization> read(InputStream in) {
        List<Authorization> list = new ArrayList<>();
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
                    Authorization a = new Authorization();
                    a.setPhysicianName(str(row, colIndex, "申请人"));
                    a.setDeptName(str(row, colIndex, "科室"));
                    a.setAuthType(parseType(str(row, colIndex, "授权类型")));
                    a.setSurgeryLevel(parseInt(str(row, colIndex, "手术分级")));
                    a.setValidFrom(parseDate(str(row, colIndex, "有效期开始")));
                    a.setValidTo(parseDate(str(row, colIndex, "有效期结束")));
                    a.setReason(str(row, colIndex, "申请事由"));
                    a.setStatus(parseStatus(str(row, colIndex, "审批状态")));
                    a.setCurrentApprover(str(row, colIndex, "当前审批"));
                    a.setApprovalOpinion(str(row, colIndex, "审批意见"));
                    a.setApprovalHistory(str(row, colIndex, "审批记录"));
                    a.setApplyDate(parseDate(str(row, colIndex, "申请日期")));
                    a.setRemark(str(row, colIndex, "备注"));
                    list.add(a);
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

    private static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    /** 授权类型编码 → 中文 */
    public static String typeText(String v) {
        if (v == null) {
            return "";
        }
        return switch (v) {
            case "SURGERY" -> "手术分级授权";
            case "ANESTHESIA" -> "麻醉药品处方权";
            case "ANTITUMOR" -> "抗肿瘤处方权";
            case "PSYCHOTROPIC" -> "精神药品处方权";
            default -> v;
        };
    }

    /** 授权类型中文 / 编码 → 编码 */
    public static String parseType(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        return switch (t) {
            case "手术分级授权", "SURGERY" -> "SURGERY";
            case "麻醉药品处方权", "ANESTHESIA" -> "ANESTHESIA";
            case "抗肿瘤处方权", "ANTITUMOR" -> "ANTITUMOR";
            case "精神药品处方权", "PSYCHOTROPIC" -> "PSYCHOTROPIC";
            default -> null;
        };
    }

    private static String statusText(Integer v) {
        if (v == null) {
            return "";
        }
        return switch (v) {
            case 0 -> "待科主任审批";
            case 1 -> "待医务科审批";
            case 2 -> "已通过";
            case 3 -> "已驳回";
            default -> "";
        };
    }

    private static Integer parseStatus(String s) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        return switch (s.trim()) {
            case "待科主任审批" -> 0;
            case "待医务科审批" -> 1;
            case "已通过" -> 2;
            case "已驳回" -> 3;
            default -> 0;
        };
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
        try {
            return LocalDate.parse(s.trim(), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
