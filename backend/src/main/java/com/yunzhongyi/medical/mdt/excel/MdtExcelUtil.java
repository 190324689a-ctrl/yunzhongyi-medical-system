package com.yunzhongyi.medical.mdt.excel;

import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import com.yunzhongyi.medical.mdt.service.MdtTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * MDT 会诊申请 Excel 导入导出（Apache POI），仿照 ScheduleExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 会诊类型/紧急程度/审批状态导出写中文（如“院内会诊”“紧急”“待审批”），
 * 导入时中文与编码都认。
 */
@Slf4j
public final class MdtExcelUtil {

    private MdtExcelUtil() {
    }

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "患者", "住院号", "会诊类型", "申请科室", "申请医师", "病情摘要",
            "拟邀请科室", "拟邀请专家", "期望会诊时间", "紧急程度", "审批状态", "审批意见", "备注"
    };

    public static void write(List<MdtApplication> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("MDT 会诊申请");
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
            for (MdtApplication a : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, a.getPatientName());
                set(row, c++, a.getInpatientNo());
                set(row, c++, MdtTypeUtil.typeText(a.getMdtType()));
                set(row, c++, a.getDeptName());
                set(row, c++, a.getApplyPhysicianName());
                set(row, c++, a.getConditionSummary());
                set(row, c++, a.getInvitedDepts());
                set(row, c++, a.getInvitedExperts());
                set(row, c++, MdtTypeUtil.fmtDateTime(a.getExpectedTime()));
                set(row, c++, MdtTypeUtil.urgencyText(a.getUrgency()));
                set(row, c++, MdtTypeUtil.statusText(a.getStatus()));
                set(row, c++, a.getApprovalOpinion());
                set(row, c++, a.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<MdtApplication> read(InputStream in) {
        List<MdtApplication> list = new ArrayList<>();
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
                    MdtApplication a = new MdtApplication();
                    a.setPatientName(str(row, colIndex, "患者"));
                    a.setInpatientNo(str(row, colIndex, "住院号"));
                    a.setMdtType(MdtTypeUtil.parseType(str(row, colIndex, "会诊类型")));
                    a.setDeptName(str(row, colIndex, "申请科室"));
                    a.setApplyPhysicianName(str(row, colIndex, "申请医师"));
                    a.setConditionSummary(str(row, colIndex, "病情摘要"));
                    a.setInvitedDepts(str(row, colIndex, "拟邀请科室"));
                    a.setInvitedExperts(str(row, colIndex, "拟邀请专家"));
                    a.setExpectedTime(parseDateTime(str(row, colIndex, "期望会诊时间")));
                    a.setUrgency(MdtTypeUtil.parseUrgency(str(row, colIndex, "紧急程度")));
                    a.setApprovalOpinion(str(row, colIndex, "审批意见"));
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

    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        try {
            return LocalDateTime.parse(t, DATE_TIME_FMT);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(t);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
