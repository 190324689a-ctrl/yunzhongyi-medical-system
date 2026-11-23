package com.yunzhongyi.medical.assessment.excel;

import com.yunzhongyi.medical.assessment.entity.Assessment;
import com.yunzhongyi.medical.assessment.service.AssessmentGradeUtil;
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
 * 医师考核 Excel 导入导出（Apache POI），仿照 AuthorizationExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 各项得分以 JSON 文本存放在一列（如 {"医德医风":28,"业务能力":28}），
 * 导出时同时展开成“医德医风 28；业务能力 28”便于阅读，导入时优先读 JSON 列。
 */
@Slf4j
public final class AssessmentExcelUtil {

    private AssessmentExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "医师", "科室", "考核模板", "考核类型", "考核年度", "考核周期",
            "各项得分", "总分", "等级", "考核结果", "考核人", "考核日期", "备注"
    };

    public static void write(List<Assessment> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医师考核");
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
            for (Assessment a : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, a.getPhysicianName());
                set(row, c++, a.getDeptName());
                set(row, c++, a.getTemplateName());
                set(row, c++, AssessmentGradeUtil.typeText(a.getAssessType()));
                set(row, c++, a.getAssessYear() == null ? "" : String.valueOf(a.getAssessYear()));
                set(row, c++, a.getCycle());
                set(row, c++, AssessmentGradeUtil.itemScoresText(a.getItemScores()));
                set(row, c++, a.getTotalScore() == null ? "" : a.getTotalScore().stripTrailingZeros().toPlainString());
                set(row, c++, AssessmentGradeUtil.gradeText(a.getGrade()));
                set(row, c++, a.getResult());
                set(row, c++, a.getAssessor());
                set(row, c++, fmtDate(a.getAssessDate()));
                set(row, c++, a.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<Assessment> read(InputStream in) {
        List<Assessment> list = new ArrayList<>();
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
                    Assessment a = new Assessment();
                    a.setPhysicianName(str(row, colIndex, "医师"));
                    a.setDeptName(str(row, colIndex, "科室"));
                    a.setTemplateName(str(row, colIndex, "考核模板"));
                    a.setAssessType(AssessmentGradeUtil.parseType(str(row, colIndex, "考核类型")));
                    a.setAssessYear(parseInt(str(row, colIndex, "考核年度")));
                    a.setCycle(str(row, colIndex, "考核周期"));
                    a.setItemScores(parseItemScores(str(row, colIndex, "各项得分")));
                    a.setGrade(AssessmentGradeUtil.parseGrade(str(row, colIndex, "等级")));
                    a.setResult(str(row, colIndex, "考核结果"));
                    a.setAssessor(str(row, colIndex, "考核人"));
                    a.setAssessDate(parseDate(str(row, colIndex, "考核日期")));
                    a.setRemark(str(row, colIndex, "备注"));
                    // 总分列仅作参考：保存时由各项得分重新计算；无各项得分时用总分兜底
                    if (!org.springframework.util.StringUtils.hasText(a.getItemScores())) {
                        String total = str(row, colIndex, "总分");
                        if (total != null) {
                            try {
                                a.setTotalScore(new BigDecimal(total));
                            } catch (NumberFormatException ignored) {
                                // 非数字总分忽略，保存时校验会报错
                            }
                        }
                    }
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

    /**
     * 各项得分列两种写法都支持：
     * 1) JSON：{"医德医风":28,"业务能力":28}（导出写出的就是展开文本，见下）
     * 2) 展开文本：医德医风 28；业务能力 28（导出实际写出的格式，读回时还原成 JSON）
     */
    static String parseItemScores(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        if (t.startsWith("{")) {
            return t;
        }
        // 展开文本 → JSON
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String part : t.split("[;；]")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int idx = p.lastIndexOf(' ');
            if (idx <= 0) {
                continue;
            }
            String name = p.substring(0, idx).trim();
            String score = p.substring(idx + 1).trim();
            try {
                new BigDecimal(score);
            } catch (NumberFormatException e) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(name.replace("\"", "")).append("\":").append(score);
        }
        sb.append("}");
        return first ? null : sb.toString();
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
