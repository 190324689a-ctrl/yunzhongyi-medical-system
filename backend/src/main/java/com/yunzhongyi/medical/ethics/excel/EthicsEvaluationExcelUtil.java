package com.yunzhongyi.medical.ethics.excel;

import com.yunzhongyi.medical.ethics.entity.EthicsEvaluation;
import com.yunzhongyi.medical.ethics.service.EthicsTypeUtil;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医德考评记录 Excel 导入导出（Apache POI），仿照 AssessmentExcelUtil。
 * <p>
 * 各项得分导出为“医德医风 47；服务态度 24”文本，导入时按“项名 得分”解析回 JSON。
 * 等级/审批状态导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class EthicsEvaluationExcelUtil {

    private EthicsEvaluationExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "医师姓名", "科室", "考评模板", "考评年度", "考评周期", "各项得分", "总分", "等级", "考评结果",
            "考评人", "考评日期", "审批状态", "审批人", "备注"
    };

    public static void write(List<EthicsEvaluation> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("医德考评");
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
            for (EthicsEvaluation e : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, e.getPhysicianName());
                set(row, cIdx++, e.getDeptName());
                set(row, cIdx++, e.getTemplateName());
                set(row, cIdx++, e.getAssessYear() == null ? "" : String.valueOf(e.getAssessYear()));
                set(row, cIdx++, e.getCycle());
                set(row, cIdx++, EthicsTypeUtil.itemScoresText(e.getItemScores()));
                set(row, cIdx++, e.getTotalScore() == null ? ""
                        : e.getTotalScore().stripTrailingZeros().toPlainString());
                set(row, cIdx++, EthicsTypeUtil.gradeText(e.getGrade()));
                set(row, cIdx++, e.getResult());
                set(row, cIdx++, e.getAssessor());
                set(row, cIdx++, EthicsTypeUtil.fmtDate(e.getAssessDate()));
                set(row, cIdx++, EthicsTypeUtil.approvalStatusText(e.getStatus()));
                set(row, cIdx++, e.getApprover());
                set(row, cIdx++, e.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<EthicsEvaluation> read(InputStream in) {
        List<EthicsEvaluation> list = new ArrayList<>();
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
                    EthicsEvaluation e = new EthicsEvaluation();
                    e.setPhysicianName(str(row, colIndex, "医师姓名"));
                    e.setDeptName(str(row, colIndex, "科室"));
                    e.setTemplateName(str(row, colIndex, "考评模板"));
                    String year = str(row, colIndex, "考评年度");
                    e.setAssessYear(year == null ? null : Integer.parseInt(year));
                    e.setCycle(str(row, colIndex, "考评周期"));
                    e.setItemScores(parseItemScoresText(str(row, colIndex, "各项得分")));
                    String total = str(row, colIndex, "总分");
                    e.setTotalScore(total == null ? null : new BigDecimal(total));
                    e.setGrade(EthicsTypeUtil.parseGrade(str(row, colIndex, "等级")));
                    e.setResult(str(row, colIndex, "考评结果"));
                    e.setAssessor(str(row, colIndex, "考评人"));
                    e.setAssessDate(parseDate(str(row, colIndex, "考评日期")));
                    e.setStatus(parseApprovalStatus(str(row, colIndex, "审批状态")));
                    e.setApprover(str(row, colIndex, "审批人"));
                    e.setRemark(str(row, colIndex, "备注"));
                    list.add(e);
                } catch (Exception e) {
                    log.warn("导入第 {} 行解析失败，已跳过: {}", r + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 Excel 失败: " + e.getMessage(), e);
        }
        return list;
    }

    /** “医德医风 47；服务态度 24” → JSON {"医德医风":47,"服务态度":24} */
    private static String parseItemScoresText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        for (String part : text.split("[；;]")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int idx = p.lastIndexOf(' ');
            if (idx <= 0) {
                continue;
            }
            try {
                scores.put(p.substring(0, idx).trim(), new BigDecimal(p.substring(idx + 1).trim()));
            } catch (NumberFormatException ignored) {
                // 该项得分不是数字，跳过
            }
        }
        if (scores.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("{");
        scores.forEach((k, v) -> {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append("\"").append(k).append("\":").append(v.stripTrailingZeros().toPlainString());
        });
        sb.append("}");
        return sb.toString();
    }

    private static Integer parseApprovalStatus(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "待审批" -> EthicsTypeUtil.APPROVAL_PENDING;
            case "已通过" -> EthicsTypeUtil.APPROVAL_PASSED;
            case "已驳回" -> EthicsTypeUtil.APPROVAL_REJECTED;
            default -> null;
        };
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

    private static String str(Row row, Map<String, Integer> colIndex, String header) {
        Integer idx = colIndex.get(header);
        if (idx == null) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
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
