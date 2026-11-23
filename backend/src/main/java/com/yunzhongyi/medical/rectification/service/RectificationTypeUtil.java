package com.yunzhongyi.medical.rectification.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 检查整改汇总类型工具：汇总范围编码与中文对照、整改完成率计算、
 * 统计周期格式校验。
 * <p>
 * 汇总范围 scopeType：ALL 全院 / DEPT 按科室 / TYPE 按检查类型。
 * 检查类型编码沿用 quality.service.QualityTypeUtil
 *（SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查）。
 */
public final class RectificationTypeUtil {

    private RectificationTypeUtil() {
    }

    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_DEPT = "DEPT";
    public static final String SCOPE_TYPE = "TYPE";

    /** 汇总范围编码 → 中文 */
    public static String scopeText(String scopeType) {
        if (scopeType == null) {
            return "";
        }
        return switch (scopeType) {
            case SCOPE_ALL -> "全院";
            case SCOPE_DEPT -> "按科室";
            case SCOPE_TYPE -> "按检查类型";
            default -> scopeType;
        };
    }

    /** 汇总范围中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseScope(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "全院", SCOPE_ALL -> SCOPE_ALL;
            case "按科室", "科室", SCOPE_DEPT -> SCOPE_DEPT;
            case "按检查类型", "检查类型", SCOPE_TYPE -> SCOPE_TYPE;
            default -> null;
        };
    }

    /** 是否为合法汇总范围编码 */
    public static boolean isValidScope(String scopeType) {
        return SCOPE_ALL.equals(scopeType) || SCOPE_DEPT.equals(scopeType) || SCOPE_TYPE.equals(scopeType);
    }

    /** 统计周期是否符合 yyyy-MM 格式（如 2026-11） */
    public static boolean isValidPeriod(String statPeriod) {
        if (statPeriod == null) {
            return false;
        }
        return statPeriod.matches("\\d{4}-(0[1-9]|1[0-2])");
    }

    /** 整改完成率（%，保留两位小数）= 已复查通过 / 整改任务总数 × 100，无任务时为 0 */
    public static BigDecimal rectificationRate(long reviewed, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(reviewed * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    /** 数字格式化：null 显示空字符串 */
    public static String fmtNumber(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    /** 数字格式化：null 显示空字符串 */
    public static String fmtNumber(Integer v) {
        return v == null ? "" : String.valueOf(v);
    }
}
