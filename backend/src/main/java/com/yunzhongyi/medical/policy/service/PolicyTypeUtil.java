package com.yunzhongyi.medical.policy.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 政策法规管理编码工具：法规分类 / 发布层级 / 法规状态对照、版本号递增、过期判定。
 * <p>
 * 法规分类 category：LAW 法律 / REGULATION 行政法规 / RULE 部门规章 /
 * LOCAL 地方性法规 / NORMATIVE 规范性文件 / STANDARD 行业标准 / OTHER 其他。
 * <p>
 * 发布层级 level：NATIONAL 国家 / PROVINCIAL 省 / MUNICIPAL 市 / HOSPITAL 院内。
 * <p>
 * 状态 status：0 草稿，1 已发布，2 已废止。
 */
public final class PolicyTypeUtil {

    private PolicyTypeUtil() {
    }

    // ---------- 法规分类 ----------
    public static final String CATEGORY_LAW = "LAW";
    public static final String CATEGORY_REGULATION = "REGULATION";
    public static final String CATEGORY_RULE = "RULE";
    public static final String CATEGORY_LOCAL = "LOCAL";
    public static final String CATEGORY_NORMATIVE = "NORMATIVE";
    public static final String CATEGORY_STANDARD = "STANDARD";
    public static final String CATEGORY_OTHER = "OTHER";

    // ---------- 发布层级 ----------
    public static final String LEVEL_NATIONAL = "NATIONAL";
    public static final String LEVEL_PROVINCIAL = "PROVINCIAL";
    public static final String LEVEL_MUNICIPAL = "MUNICIPAL";
    public static final String LEVEL_HOSPITAL = "HOSPITAL";

    // ---------- 法规状态 ----------
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_ABOLISHED = 2;

    /** 即将到期提醒窗口：30 天内到期即提醒 */
    public static final int EXPIRY_WARN_DAYS = 30;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)\\.(\\d+)$");

    /** 法规分类编码 → 中文 */
    public static String categoryText(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case CATEGORY_LAW -> "法律";
            case CATEGORY_REGULATION -> "行政法规";
            case CATEGORY_RULE -> "部门规章";
            case CATEGORY_LOCAL -> "地方性法规";
            case CATEGORY_NORMATIVE -> "规范性文件";
            case CATEGORY_STANDARD -> "行业标准";
            case CATEGORY_OTHER -> "其他";
            default -> category;
        };
    }

    /** 法规分类中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "法律", "LAW" -> CATEGORY_LAW;
            case "行政法规", "REGULATION" -> CATEGORY_REGULATION;
            case "部门规章", "RULE" -> CATEGORY_RULE;
            case "地方性法规", "LOCAL" -> CATEGORY_LOCAL;
            case "规范性文件", "NORMATIVE" -> CATEGORY_NORMATIVE;
            case "行业标准", "STANDARD" -> CATEGORY_STANDARD;
            case "其他", "OTHER" -> CATEGORY_OTHER;
            default -> null;
        };
    }

    public static boolean isValidCategory(String category) {
        return CATEGORY_LAW.equals(category) || CATEGORY_REGULATION.equals(category)
                || CATEGORY_RULE.equals(category) || CATEGORY_LOCAL.equals(category)
                || CATEGORY_NORMATIVE.equals(category) || CATEGORY_STANDARD.equals(category)
                || CATEGORY_OTHER.equals(category);
    }

    /** 发布层级编码 → 中文 */
    public static String levelText(String level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case LEVEL_NATIONAL -> "国家";
            case LEVEL_PROVINCIAL -> "省";
            case LEVEL_MUNICIPAL -> "市";
            case LEVEL_HOSPITAL -> "院内";
            default -> level;
        };
    }

    /** 发布层级中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseLevel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "国家", "NATIONAL" -> LEVEL_NATIONAL;
            case "省", "PROVINCIAL" -> LEVEL_PROVINCIAL;
            case "市", "MUNICIPAL" -> LEVEL_MUNICIPAL;
            case "院内", "HOSPITAL" -> LEVEL_HOSPITAL;
            default -> null;
        };
    }

    public static boolean isValidLevel(String level) {
        return LEVEL_NATIONAL.equals(level) || LEVEL_PROVINCIAL.equals(level)
                || LEVEL_MUNICIPAL.equals(level) || LEVEL_HOSPITAL.equals(level);
    }

    /** 法规状态 → 中文 */
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

    /** 法规状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
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
