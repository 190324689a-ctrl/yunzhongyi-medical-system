package com.yunzhongyi.medical.coresystem.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 核心制度管理编码工具：制度类别 / 适用层级 / 制度状态对照、版本号递增、过期判定。
 * <p>
 * 制度类别 category：MEDICAL 医疗 / NURSING 护理 / PHARMACY 药事 /
 * MANAGEMENT 管理 / SAFETY 安全 / OTHER 其他。
 * <p>
 * 适用层级 level：HOSPITAL 院级 / DEPT 科室级。
 * <p>
 * 状态 status：0 草稿，1 已发布，2 已废止。
 */
public final class CoreSystemTypeUtil {

    private CoreSystemTypeUtil() {
    }

    // ---------- 制度类别 ----------
    public static final String CATEGORY_MEDICAL = "MEDICAL";
    public static final String CATEGORY_NURSING = "NURSING";
    public static final String CATEGORY_PHARMACY = "PHARMACY";
    public static final String CATEGORY_MANAGEMENT = "MANAGEMENT";
    public static final String CATEGORY_SAFETY = "SAFETY";
    public static final String CATEGORY_OTHER = "OTHER";

    // ---------- 适用层级 ----------
    public static final String LEVEL_HOSPITAL = "HOSPITAL";
    public static final String LEVEL_DEPT = "DEPT";

    // ---------- 制度状态 ----------
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_ABOLISHED = 2;

    /** 即将到期提醒窗口：30 天内到期即提醒 */
    public static final int EXPIRY_WARN_DAYS = 30;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)\\.(\\d+)$");

    /** 制度类别编码 → 中文 */
    public static String categoryText(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case CATEGORY_MEDICAL -> "医疗";
            case CATEGORY_NURSING -> "护理";
            case CATEGORY_PHARMACY -> "药事";
            case CATEGORY_MANAGEMENT -> "管理";
            case CATEGORY_SAFETY -> "安全";
            case CATEGORY_OTHER -> "其他";
            default -> category;
        };
    }

    /** 制度类别中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "医疗", "MEDICAL" -> CATEGORY_MEDICAL;
            case "护理", "NURSING" -> CATEGORY_NURSING;
            case "药事", "PHARMACY" -> CATEGORY_PHARMACY;
            case "管理", "MANAGEMENT" -> CATEGORY_MANAGEMENT;
            case "安全", "SAFETY" -> CATEGORY_SAFETY;
            case "其他", "OTHER" -> CATEGORY_OTHER;
            default -> null;
        };
    }

    public static boolean isValidCategory(String category) {
        return CATEGORY_MEDICAL.equals(category) || CATEGORY_NURSING.equals(category)
                || CATEGORY_PHARMACY.equals(category) || CATEGORY_MANAGEMENT.equals(category)
                || CATEGORY_SAFETY.equals(category) || CATEGORY_OTHER.equals(category);
    }

    /** 适用层级编码 → 中文 */
    public static String levelText(String level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case LEVEL_HOSPITAL -> "院级";
            case LEVEL_DEPT -> "科室级";
            default -> level;
        };
    }

    /** 适用层级中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseLevel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "院级", "HOSPITAL" -> LEVEL_HOSPITAL;
            case "科室级", "DEPT" -> LEVEL_DEPT;
            default -> null;
        };
    }

    public static boolean isValidLevel(String level) {
        return LEVEL_HOSPITAL.equals(level) || LEVEL_DEPT.equals(level);
    }

    /** 制度状态 → 中文 */
    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PUBLISHED -> "已发布";
            case STATUS_ABOLISHED -> "已废止";
            default -> String.valueOf(status);
        };
    }

    /** 制度状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "草稿" -> STATUS_DRAFT;
            case "已发布" -> STATUS_PUBLISHED;
            case "已废止" -> STATUS_ABOLISHED;
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
