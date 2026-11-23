package com.yunzhongyi.medical.physician.excel;

import com.yunzhongyi.medical.physician.entity.Physician;
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
 * 医师档案 Excel 导入导出（Apache POI）
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 */
@Slf4j
public final class PhysicianExcelUtil {

    private PhysicianExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "姓名", "性别", "身份证号", "手机号", "科室", "职称", "最高学历", "毕业院校", "专业",
            "执业证号", "执业范围", "执业类别", "人员类别", "聘期开始", "聘期结束", "证书",
            "进修经历", "手术分级", "手术分级授权", "抗肿瘤处方权", "麻醉药品处方权", "精神药品处方权",
            "在职状态", "备注"
    };

    public static void write(List<Physician> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医师档案");
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
            for (Physician p : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, p.getName());
                set(row, c++, "M".equals(p.getGender()) ? "男" : "F".equals(p.getGender()) ? "女" : "");
                set(row, c++, p.getIdCard());
                set(row, c++, p.getPhone());
                set(row, c++, p.getDeptName());
                set(row, c++, p.getTitle());
                set(row, c++, p.getEducation());
                set(row, c++, p.getGraduateSchool());
                set(row, c++, p.getMajor());
                set(row, c++, p.getPracticeNo());
                set(row, c++, p.getPracticeScope());
                set(row, c++, p.getPracticeCategory());
                set(row, c++, p.getStaffType());
                set(row, c++, fmtDate(p.getHireStartDate()));
                set(row, c++, fmtDate(p.getHireEndDate()));
                set(row, c++, p.getCertificates());
                set(row, c++, p.getTraining());
                set(row, c++, p.getSurgeryLevel() == null ? "" : String.valueOf(p.getSurgeryLevel()));
                set(row, c++, yn(p.getSurgeryAuth()));
                set(row, c++, yn(p.getAntitumorRx()));
                set(row, c++, yn(p.getAnesthesiaRx()));
                set(row, c++, yn(p.getPsychotropicRx()));
                set(row, c++, statusText(p.getStatus()));
                set(row, c++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<Physician> read(InputStream in) {
        List<Physician> list = new ArrayList<>();
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
                    Physician p = new Physician();
                    p.setName(str(row, colIndex, "姓名"));
                    String gender = str(row, colIndex, "性别");
                    p.setGender("男".equals(gender) ? "M" : "女".equals(gender) ? "F" : null);
                    p.setIdCard(str(row, colIndex, "身份证号"));
                    p.setPhone(str(row, colIndex, "手机号"));
                    p.setDeptName(str(row, colIndex, "科室"));
                    p.setTitle(str(row, colIndex, "职称"));
                    p.setEducation(str(row, colIndex, "最高学历"));
                    p.setGraduateSchool(str(row, colIndex, "毕业院校"));
                    p.setMajor(str(row, colIndex, "专业"));
                    p.setPracticeNo(str(row, colIndex, "执业证号"));
                    p.setPracticeScope(str(row, colIndex, "执业范围"));
                    p.setPracticeCategory(str(row, colIndex, "执业类别"));
                    p.setStaffType(str(row, colIndex, "人员类别"));
                    p.setHireStartDate(parseDate(str(row, colIndex, "聘期开始")));
                    p.setHireEndDate(parseDate(str(row, colIndex, "聘期结束")));
                    p.setCertificates(str(row, colIndex, "证书"));
                    p.setTraining(str(row, colIndex, "进修经历"));
                    p.setSurgeryLevel(parseInt(str(row, colIndex, "手术分级")));
                    p.setSurgeryAuth(parseYn(str(row, colIndex, "手术分级授权")));
                    p.setAntitumorRx(parseYn(str(row, colIndex, "抗肿瘤处方权")));
                    p.setAnesthesiaRx(parseYn(str(row, colIndex, "麻醉药品处方权")));
                    p.setPsychotropicRx(parseYn(str(row, colIndex, "精神药品处方权")));
                    p.setStatus(parseStatus(str(row, colIndex, "在职状态")));
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

    private static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    private static String yn(Integer v) {
        if (v == null) {
            return "";
        }
        return v == 1 ? "是" : "否";
    }

    private static String statusText(Integer v) {
        if (v == null) {
            return "";
        }
        return switch (v) {
            case 1 -> "在职";
            case 2 -> "休假";
            case 3 -> "进修";
            case 4 -> "离职";
            case 5 -> "退休";
            default -> "";
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
            // 手机号/身份证等纯数字按整数文本读，避免科学计数法
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

    /** "是/有/1" -> 1，"否/无/0" -> 0 */
    private static Integer parseYn(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        if ("是".equals(t) || "有".equals(t) || "1".equals(t)) {
            return 1;
        }
        if ("否".equals(t) || "无".equals(t) || "0".equals(t)) {
            return 0;
        }
        return null;
    }

    private static Integer parseStatus(String s) {
        if (s == null || s.isBlank()) {
            return 1;
        }
        return switch (s.trim()) {
            case "在职" -> 1;
            case "休假" -> 2;
            case "进修" -> 3;
            case "离职" -> 4;
            case "退休" -> 5;
            default -> 1;
        };
    }
}
