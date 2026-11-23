package com.yunzhongyi.medical.techgrading.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 医疗技术分级管理编码工具：技术分类 / 技术分级 / 准入类别 / 风险等级 /
 * 档案状态 / 准入申请类型 / 授权状态对照、过期判定。
 * <p>
 * 技术分类 category：SURGICAL 手术类 / NON_SURGICAL 非手术类 / TCM 中医技术 /
 * DIAGNOSTIC 诊疗技术 / OTHER 其他。
 * <p>
 * 技术分级 techLevel：1 一级 / 2 二级 / 3 三级。
 * <p>
 * 准入类别 accessType：NORMAL 常规技术 / RESTRICTED 限制类技术 / PROHIBITED 禁止类技术。
 * <p>
 * 风险等级 riskLevel：LOW 低 / MEDIUM 中 / HIGH 高。
 * <p>
 * 档案状态 status：0 待准入，1 已准入，2 已停用。
 * <p>
 * 准入申请类型 applyType：NEW 新技术准入 / ADD_DEPT 新增开展科室 /
 * CHANGE_LEVEL 变更分级 / RENEW 延续准入。
 * <p>
 * 准入申请状态：0 待审批，1 已通过，2 已驳回。
 * <p>
 * 授权状态：0 待审批，1 已通过，2 已驳回，3 已注销。
 */
public final class TechGradingTypeUtil {

    private TechGradingTypeUtil() {
    }

    // ---------- 技术分类 ----------
    public static final String CATEGORY_SURGICAL = "SURGICAL";
    public static final String CATEGORY_NON_SURGICAL = "NON_SURGICAL";
    public static final String CATEGORY_TCM = "TCM";
    public static final String CATEGORY_DIAGNOSTIC = "DIAGNOSTIC";
    public static final String CATEGORY_OTHER = "OTHER";

    // ---------- 技术分级 ----------
    public static final int LEVEL_ONE = 1;
    public static final int LEVEL_TWO = 2;
    public static final int LEVEL_THREE = 3;

    // ---------- 准入类别 ----------
    public static final String ACCESS_NORMAL = "NORMAL";
    public static final String ACCESS_RESTRICTED = "RESTRICTED";
    public static final String ACCESS_PROHIBITED = "PROHIBITED";

    // ---------- 风险等级 ----------
    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";

    // ---------- 档案状态 ----------
    public static final int TECH_STATUS_PENDING = 0;
    public static final int TECH_STATUS_ADMITTED = 1;
    public static final int TECH_STATUS_SUSPENDED = 2;

    // ---------- 准入申请类型 ----------
    public static final String APPLY_NEW = "NEW";
    public static final String APPLY_ADD_DEPT = "ADD_DEPT";
    public static final String APPLY_CHANGE_LEVEL = "CHANGE_LEVEL";
    public static final String APPLY_RENEW = "RENEW";

    // ---------- 准入申请状态 ----------
    public static final int ADMISSION_STATUS_PENDING = 0;
    public static final int ADMISSION_STATUS_APPROVED = 1;
    public static final int ADMISSION_STATUS_REJECTED = 2;

    // ---------- 授权状态 ----------
    public static final int AUTH_STATUS_PENDING = 0;
    public static final int AUTH_STATUS_APPROVED = 1;
    public static final int AUTH_STATUS_REJECTED = 2;
    public static final int AUTH_STATUS_REVOKED = 3;

    /** 即将到期提醒窗口：90 天内到期即提醒（授权与准入有效期按年计，窗口比制度类更长） */
    public static final int EXPIRY_WARN_DAYS = 90;

    /** 新技术准入默认有效期：3 年 */
    public static final int DEFAULT_ADMISSION_YEARS = 3;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 技术分类编码 → 中文 */
    public static String categoryText(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case CATEGORY_SURGICAL -> "手术类";
            case CATEGORY_NON_SURGICAL -> "非手术类";
            case CATEGORY_TCM -> "中医技术";
            case CATEGORY_DIAGNOSTIC -> "诊疗技术";
            case CATEGORY_OTHER -> "其他";
            default -> category;
        };
    }

    /** 技术分类中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "手术类", "SURGICAL" -> CATEGORY_SURGICAL;
            case "非手术类", "NON_SURGICAL" -> CATEGORY_NON_SURGICAL;
            case "中医技术", "TCM" -> CATEGORY_TCM;
            case "诊疗技术", "DIAGNOSTIC" -> CATEGORY_DIAGNOSTIC;
            case "其他", "OTHER" -> CATEGORY_OTHER;
            default -> null;
        };
    }

    public static boolean isValidCategory(String category) {
        return CATEGORY_SURGICAL.equals(category) || CATEGORY_NON_SURGICAL.equals(category)
                || CATEGORY_TCM.equals(category) || CATEGORY_DIAGNOSTIC.equals(category)
                || CATEGORY_OTHER.equals(category);
    }

    /** 技术分级 → 中文 */
    public static String levelText(Integer level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case LEVEL_ONE -> "一级";
            case LEVEL_TWO -> "二级";
            case LEVEL_THREE -> "三级";
            default -> String.valueOf(level);
        };
    }

    /** 技术分级中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseLevel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "一级", "1" -> LEVEL_ONE;
            case "二级", "2" -> LEVEL_TWO;
            case "三级", "3" -> LEVEL_THREE;
            default -> null;
        };
    }

    public static boolean isValidLevel(Integer level) {
        return level != null
                && (level == LEVEL_ONE || level == LEVEL_TWO || level == LEVEL_THREE);
    }

    /** 准入类别编码 → 中文 */
    public static String accessTypeText(String accessType) {
        if (accessType == null) {
            return "";
        }
        return switch (accessType) {
            case ACCESS_NORMAL -> "常规技术";
            case ACCESS_RESTRICTED -> "限制类技术";
            case ACCESS_PROHIBITED -> "禁止类技术";
            default -> accessType;
        };
    }

    /** 准入类别中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseAccessType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "常规技术", "常规", "NORMAL" -> ACCESS_NORMAL;
            case "限制类技术", "限制类", "RESTRICTED" -> ACCESS_RESTRICTED;
            case "禁止类技术", "禁止类", "PROHIBITED" -> ACCESS_PROHIBITED;
            default -> null;
        };
    }

    public static boolean isValidAccessType(String accessType) {
        return ACCESS_NORMAL.equals(accessType) || ACCESS_RESTRICTED.equals(accessType)
                || ACCESS_PROHIBITED.equals(accessType);
    }

    /** 风险等级编码 → 中文 */
    public static String riskLevelText(String riskLevel) {
        if (riskLevel == null) {
            return "";
        }
        return switch (riskLevel) {
            case RISK_LOW -> "低";
            case RISK_MEDIUM -> "中";
            case RISK_HIGH -> "高";
            default -> riskLevel;
        };
    }

    /** 风险等级中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseRiskLevel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "低", "LOW" -> RISK_LOW;
            case "中", "MEDIUM" -> RISK_MEDIUM;
            case "高", "HIGH" -> RISK_HIGH;
            default -> null;
        };
    }

    public static boolean isValidRiskLevel(String riskLevel) {
        return riskLevel == null || RISK_LOW.equals(riskLevel)
                || RISK_MEDIUM.equals(riskLevel) || RISK_HIGH.equals(riskLevel);
    }

    /** 档案状态 → 中文 */
    public static String techStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case TECH_STATUS_PENDING -> "待准入";
            case TECH_STATUS_ADMITTED -> "已准入";
            case TECH_STATUS_SUSPENDED -> "已停用";
            default -> String.valueOf(status);
        };
    }

    /** 档案状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseTechStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "待准入" -> TECH_STATUS_PENDING;
            case "已准入" -> TECH_STATUS_ADMITTED;
            case "已停用" -> TECH_STATUS_SUSPENDED;
            default -> null;
        };
    }

    /** 准入申请类型编码 → 中文 */
    public static String applyTypeText(String applyType) {
        if (applyType == null) {
            return "";
        }
        return switch (applyType) {
            case APPLY_NEW -> "新技术准入";
            case APPLY_ADD_DEPT -> "新增开展科室";
            case APPLY_CHANGE_LEVEL -> "变更分级";
            case APPLY_RENEW -> "延续准入";
            default -> applyType;
        };
    }

    /** 准入申请类型中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseApplyType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "新技术准入", "NEW" -> APPLY_NEW;
            case "新增开展科室", "ADD_DEPT" -> APPLY_ADD_DEPT;
            case "变更分级", "CHANGE_LEVEL" -> APPLY_CHANGE_LEVEL;
            case "延续准入", "RENEW" -> APPLY_RENEW;
            default -> null;
        };
    }

    public static boolean isValidApplyType(String applyType) {
        return APPLY_NEW.equals(applyType) || APPLY_ADD_DEPT.equals(applyType)
                || APPLY_CHANGE_LEVEL.equals(applyType) || APPLY_RENEW.equals(applyType);
    }

    /** 准入申请状态 → 中文 */
    public static String admissionStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case ADMISSION_STATUS_PENDING -> "待审批";
            case ADMISSION_STATUS_APPROVED -> "已通过";
            case ADMISSION_STATUS_REJECTED -> "已驳回";
            default -> String.valueOf(status);
        };
    }

    /** 授权状态 → 中文 */
    public static String authStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case AUTH_STATUS_PENDING -> "待审批";
            case AUTH_STATUS_APPROVED -> "已通过";
            case AUTH_STATUS_REJECTED -> "已驳回";
            case AUTH_STATUS_REVOKED -> "已注销";
            default -> String.valueOf(status);
        };
    }

    /** 授权状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseAuthStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "待审批" -> AUTH_STATUS_PENDING;
            case "已通过" -> AUTH_STATUS_APPROVED;
            case "已驳回" -> AUTH_STATUS_REJECTED;
            case "已注销" -> AUTH_STATUS_REVOKED;
            default -> null;
        };
    }

    /** 是否已过期（已准入/已通过且有效期已过当天） */
    public static boolean isExpired(Integer status, LocalDate expiryDate) {
        if (expiryDate == null || status == null) {
            return false;
        }
        return (Integer.valueOf(TECH_STATUS_ADMITTED).equals(status)
                || Integer.valueOf(AUTH_STATUS_APPROVED).equals(status))
                && expiryDate.isBefore(LocalDate.now());
    }

    /** 是否即将到期（已准入/已通过、未过期、有效期在 90 天内） */
    public static boolean isExpiringSoon(Integer status, LocalDate expiryDate) {
        if (expiryDate == null || status == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return (Integer.valueOf(TECH_STATUS_ADMITTED).equals(status)
                || Integer.valueOf(AUTH_STATUS_APPROVED).equals(status))
                && !expiryDate.isBefore(today)
                && !expiryDate.isAfter(today.plusDays(EXPIRY_WARN_DAYS));
    }

    /** 日期格式化（yyyy-MM-dd），为空返回 "" */
    public static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }
}
