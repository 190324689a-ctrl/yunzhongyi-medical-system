package com.yunzhongyi.medical.statistics.excel;

import com.yunzhongyi.medical.statistics.entity.QualityIndicator;
import com.yunzhongyi.medical.statistics.service.StatisticsTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 三级医院医疗质量指标 Excel 导入导出（Apache POI），仿照 NewTechExcelUtil。
 * <p>
 * 列顺序与前端导入模板保持一致；导入时按表头文字匹配列，容错性更好。
 * 标准级别 / 指标分类 / 比较方向导出写中文（如“国家”“医疗质量”“越高越好”），
 * 导入时中文与编码都认。
 */
@Slf4j
public final class QualityIndicatorExcelUtil {

    private QualityIndicatorExcelUtil() {
    }

    /** 导出列：标题 */
    private static final String[] HEADERS = {
            "指标编码", "指标名称", "指标分类", "标准级别", "标准名称",
            "目标值", "实际值", "单位", "比较方向", "统计周期", "适用科室", "数据来源", "备注"
    };

    public static void write(List<QualityIndicator> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("三级医院医疗质量指标");
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
            for (QualityIndicator q : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, q.getIndicatorCode());
                set(row, c++, q.getIndicatorName());
                set(row, c++, q.getCategory());
                set(row, c++, StatisticsTypeUtil.levelText(q.getStandardLevel()));
                set(row, c++, q.getStandardName());
                set(row, c++, StatisticsTypeUtil.fmtNumber(q.getTargetValue()));
                set(row, c++, StatisticsTypeUtil.fmtNumber(q.getActualValue()));
                set(row, c++, q.getUnit());
                set(row, c++, StatisticsTypeUtil.directionText(q.getDirection()));
                set(row, c++, q.getStatPeriod());
                set(row, c++, q.getDeptName());
                set(row, c++, q.getDataSource());
                set(row, c++, q.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<QualityIndicator> read(InputStream in) {
        List<QualityIndicator> list = new ArrayList<>();
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
                    QualityIndicator q = new QualityIndicator();
                    q.setIndicatorCode(str(row, colIndex, "指标编码"));
                    q.setIndicatorName(str(row, colIndex, "指标名称"));
                    q.setCategory(StatisticsTypeUtil.parseCategory(str(row, colIndex, "指标分类")));
                    q.setStandardLevel(StatisticsTypeUtil.parseLevel(str(row, colIndex, "标准级别")));
                    q.setStandardName(str(row, colIndex, "标准名称"));
                    q.setTargetValue(parseDecimal(str(row, colIndex, "目标值")));
                    q.setActualValue(parseDecimal(str(row, colIndex, "实际值")));
                    q.setUnit(str(row, colIndex, "单位"));
                    q.setDirection(StatisticsTypeUtil.parseDirection(str(row, colIndex, "比较方向")));
                    q.setStatPeriod(str(row, colIndex, "统计周期"));
                    q.setDeptName(str(row, colIndex, "适用科室"));
                    q.setDataSource(str(row, colIndex, "数据来源"));
                    q.setRemark(str(row, colIndex, "备注"));
                    list.add(q);
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
            double d = cell.getNumericCellValue();
            if (d == Math.rint(d)) {
                return String.valueOf(Math.round(d));
            }
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }
        String s = cell.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static BigDecimal parseDecimal(String s) {
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
