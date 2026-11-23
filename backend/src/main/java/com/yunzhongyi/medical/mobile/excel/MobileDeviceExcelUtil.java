package com.yunzhongyi.medical.mobile.excel;

import com.yunzhongyi.medical.mobile.entity.MobileDevice;
import com.yunzhongyi.medical.mobile.service.MobileTypeUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动端设备 Excel 导入导出（Apache POI）。
 * <p>
 * 导出列：设备编码 / 使用人 / 使用人姓名 / 平台 / 设备型号 / App 版本 /
 * 推送 token / 状态 / 最近活跃时间 / 备注。
 * 导入时按表头文字匹配列；平台与状态导出写中文，导入时中文与编码都认。
 */
@Slf4j
public final class MobileDeviceExcelUtil {

    private MobileDeviceExcelUtil() {
    }

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] HEADERS = {
            "设备编码", "使用人", "使用人姓名", "平台", "设备型号", "App 版本",
            "推送 token", "状态", "最近活跃时间", "备注"
    };

    public static void write(List<MobileDevice> list, OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("移动端设备");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            int rowNum = 1;
            for (MobileDevice d : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                set(row, c++, d.getDeviceCode());
                set(row, c++, d.getUsername());
                set(row, c++, d.getDisplayName());
                set(row, c++, MobileTypeUtil.platformText(d.getPlatform()));
                set(row, c++, d.getDeviceModel());
                set(row, c++, d.getAppVersion());
                set(row, c++, d.getPushToken());
                set(row, c++, MobileTypeUtil.deviceStatusText(d.getStatus()));
                set(row, c++, d.getLastActiveTime() == null ? "" : d.getLastActiveTime().format(DATE_TIME_FORMAT));
                set(row, c++, d.getRemark());
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static void writeTemplate(OutputStream out) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            Sheet sheet = wb.createSheet("移动端设备");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败: " + e.getMessage(), e);
        }
    }

    public static List<MobileDevice> read(InputStream in) {
        List<MobileDevice> list = new ArrayList<>();
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
                    MobileDevice d = new MobileDevice();
                    d.setDeviceCode(str(row, colIndex, "设备编码"));
                    d.setUsername(str(row, colIndex, "使用人"));
                    d.setDisplayName(str(row, colIndex, "使用人姓名"));
                    d.setPlatform(parsePlatform(str(row, colIndex, "平台")));
                    d.setDeviceModel(str(row, colIndex, "设备型号"));
                    d.setAppVersion(str(row, colIndex, "App 版本"));
                    d.setPushToken(str(row, colIndex, "推送 token"));
                    d.setStatus(parseStatus(str(row, colIndex, "状态")));
                    String lastActive = str(row, colIndex, "最近活跃时间");
                    if (lastActive != null && !lastActive.isBlank()) {
                        d.setLastActiveTime(LocalDateTime.parse(lastActive, DATE_TIME_FORMAT));
                    }
                    d.setRemark(str(row, colIndex, "备注"));
                    list.add(d);
                } catch (Exception e) {
                    log.warn("导入第 {} 行解析失败，已跳过: {}", r + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 Excel 失败: " + e.getMessage(), e);
        }
        return list;
    }

    /** 平台：中文与编码都认，非法返回原值（由服务层校验拒绝）。 */
    static String parsePlatform(String text) {
        if (text == null) {
            return null;
        }
        if ("iOS".equalsIgnoreCase(text) || MobileTypeUtil.PLATFORM_IOS.equalsIgnoreCase(text)) {
            return MobileTypeUtil.PLATFORM_IOS;
        }
        if ("Android".equalsIgnoreCase(text) || MobileTypeUtil.PLATFORM_ANDROID.equalsIgnoreCase(text)) {
            return MobileTypeUtil.PLATFORM_ANDROID;
        }
        return text;
    }

    /** 状态：中文与数字都认，未填返回 null（由服务层默认已绑定）。 */
    static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if ("已绑定".equals(text) || "1".equals(text)) {
            return MobileTypeUtil.DEVICE_BOUND;
        }
        if ("已解绑".equals(text) || "0".equals(text)) {
            return MobileTypeUtil.DEVICE_UNBOUND;
        }
        return null;
    }

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
        return cell.toString().trim();
    }
}
