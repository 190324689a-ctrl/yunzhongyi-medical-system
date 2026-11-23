package com.yunzhongyi.medical.policy.excel;

import com.yunzhongyi.medical.policy.entity.Policy;
import com.yunzhongyi.medical.policy.service.PolicyTypeUtil;
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
 * 政策法规 Excel 导入导出（Apache POI），仿照 DocumentExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 法规分类/发布层级/状态导出写中文（如“法律”“国家”“已发布”），导入时中文与编码都认。
 * 正文内容不进 Excel（过长），导入后在页面补录正文再发布。
 */
@Slf4j
public final class PolicyExcelUtil {

    private PolicyExcelUtil() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "文号", "法规名称", "分类", "发布层级", "发布机关", "版本", "适用科室", "适用人员",
            "归口科室", "起草人", "审批人", "发布日期", "生效日期", "失效日期", "状态", "备注"
    };

    public static void write(List<Policy> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("政策法规");
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
            for (Policy p : list) {
                Row row = sheet.createRow(rowNum++);
                int cIdx = 0;
                set(row, cIdx++, p.getPolicyNo());
                set(row, cIdx++, p.getTitle());
                set(row, cIdx++, PolicyTypeUtil.categoryText(p.getCategory()));
                set(row, cIdx++, PolicyTypeUtil.levelText(p.getLevel()));
                set(row, cIdx++, p.getIssuingAuthority());
                set(row, cIdx++, p.getVersion());
                set(row, cIdx++, p.getApplicableDept());
                set(row, cIdx++, p.getApplicablePersonnel());
                set(row, cIdx++, p.getDepartment());
                set(row, cIdx++, p.getDrafter());
                set(row, cIdx++, p.getApprover());
                set(row, cIdx++, PolicyTypeUtil.fmtDate(p.getPublishDate()));
                set(row, cIdx++, PolicyTypeUtil.fmtDate(p.getEffectiveDate()));
                set(row, cIdx++, PolicyTypeUtil.fmtDate(p.getExpiryDate()));
                set(row, cIdx++, PolicyTypeUtil.statusText(p.getStatus()));
                set(row, cIdx++, p.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<Policy> read(InputStream in) {
        List<Policy> list = new ArrayList<>();
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
                    Policy p = new Policy();
                    p.setPolicyNo(str(row, colIndex, "文号"));
                    p.setTitle(str(row, colIndex, "法规名称"));
                    p.setCategory(PolicyTypeUtil.parseCategory(str(row, colIndex, "分类")));
                    p.setLevel(PolicyTypeUtil.parseLevel(str(row, colIndex, "发布层级")));
                    p.setIssuingAuthority(str(row, colIndex, "发布机关"));
                    p.setVersion(str(row, colIndex, "版本"));
                    p.setApplicableDept(str(row, colIndex, "适用科室"));
                    p.setApplicablePersonnel(str(row, colIndex, "适用人员"));
                    p.setDepartment(str(row, colIndex, "归口科室"));
                    p.setDrafter(str(row, colIndex, "起草人"));
                    p.setApprover(str(row, colIndex, "审批人"));
                    p.setPublishDate(parseDate(str(row, colIndex, "发布日期")));
                    p.setEffectiveDate(parseDate(str(row, colIndex, "生效日期")));
                    p.setExpiryDate(parseDate(str(row, colIndex, "失效日期")));
                    Integer status = PolicyTypeUtil.parseStatus(str(row, colIndex, "状态"));
                    p.setStatus(status == null ? PolicyTypeUtil.STATUS_DRAFT : status);
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
