package com.yunzhongyi.medical.document.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件制度管理编码工具：文件分类 / 文件状态对照、版本号递增、过期判定。
 * <p>
 * 文件分类 category：SYSTEM 制度 / STANDARD 规范 / PROCESS 流程 /
 * FILE 文件 / OTHER 其他。
 * <p>
 * 状态 status：0 草稿，1 已发布，2 已作废。
 */
public final class DocumentTypeUtil {

    private DocumentTypeUtil() {
    }

    // ---------- 文件分类 ----------
    public static final String CATEGORY_SYSTEM = "SYSTEM";
    public static final String CATEGORY_STANDARD = "STANDARD";
    public static final String CATEGORY_PROCESS = "PROCESS";
    public static final String CATEGORY_FILE = "FILE";
    public static final String CATEGORY_OTHER = "OTHER";

    // ---------- 文件状态 ----------
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_ABOLISHED = 2;

    /** 即将到期提醒窗口：30 天内到期即提醒 */
    public static final int EXPIRY_WARN_DAYS = 30;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)\\.(\\d+)$");

    /** 文件分类编码 → 中文 */
    public static String categoryText(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case CATEGORY_SYSTEM -> "制度";
            case CATEGORY_STANDARD -> "规范";
            case CATEGORY_PROCESS -> "流程";
            case CATEGORY_FILE -> "文件";
            case CATEGORY_OTHER -> "其他";
            default -> category;
        };
    }

    /** 文件分类中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "制度", "SYSTEM" -> CATEGORY_SYSTEM;
            case "规范", "STANDARD" -> CATEGORY_STANDARD;
            case "流程", "PROCESS" -> CATEGORY_PROCESS;
            case "文件", "FILE" -> CATEGORY_FILE;
            case "其他", "OTHER" -> CATEGORY_OTHER;
            default -> null;
        };
    }

    public static boolean isValidCategory(String category) {
        return CATEGORY_SYSTEM.equals(category) || CATEGORY_STANDARD.equals(category)
                || CATEGORY_PROCESS.equals(category) || CATEGORY_FILE.equals(category)
                || CATEGORY_OTHER.equals(category);
    }

    /** 文件状态 → 中文 */
    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PUBLISHED -> "已发布";
            case STATUS_ABOLISHED -> "已作废";
            default -> String.valueOf(status);
        };
    }

    /** 文件状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "草稿" -> STATUS_DRAFT;
            case "已发布" -> STATUS_PUBLISHED;
            case "已作废" -> STATUS_ABOLISHED;
            default -> null;
        };
    }

    /** 是否已过期（已发布且失效日期已过当天） */
    public static boolean isExpired(Integer status, LocalDate expiryDate) {
        if (expiryDate == null) {
            return false;
        }
        return Integer.valueOf(STATUS_PUBLISHED).equals(status)
                && expiryDate.isBefore(LocalDate.now());
    }

    /** 是否即将到期（已发布、未过期、失效日期在 30 天内） */
    public static boolean isExpiringSoon(Integer status, LocalDate expiryDate) {
        if (expiryDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return Integer.valueOf(STATUS_PUBLISHED).equals(status)
                && !expiryDate.isBefore(today)
                && !expiryDate.isAfter(today.plusDays(EXPIRY_WARN_DAYS));
    }

    /** 版本号递增：V1.0 → V2.0，V1.2 → V2.0；格式不对时返回 V1.0 */
    public static String nextVersion(String version) {
        if (version == null || version.isBlank()) {
            return "V1.0";
        }
        Matcher m = VERSION_PATTERN.matcher(version.trim());
        if (!m.matches()) {
            return "V1.0";
        }
        int major = Integer.parseInt(m.group(1));
        return "V" + (major + 1) + ".0";
    }

    /** 日期格式化（yyyy-MM-dd），为空返回 "" */
    public static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }
}
