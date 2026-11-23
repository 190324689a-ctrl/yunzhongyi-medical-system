package com.yunzhongyi.medical.statistics.service;

import com.yunzhongyi.medical.statistics.entity.QualityIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 统计模块类型 / 标准级别 / 达标判定工具：编码与中文对照。
 * <p>
 * 标准级别 standardLevel：NATIONAL 国家 / PROVINCE 省 / CITY 市。
 * <p>
 * 指标分类 category：QUALITY 医疗质量 / SAFETY 医疗安全 /
 * PATIENT_SAFETY 患者安全 / EFFICIENCY 工作效率（库里存中文，前端下拉
 * 与导入都认中文与编码）。
 * <p>
 * 比较方向 direction：HIGHER_BETTER 越高越好 / LOWER_BETTER 越低越好。
 */
public final class StatisticsTypeUtil {

    private StatisticsTypeUtil() {
    }

    public static final String LEVEL_NATIONAL = "NATIONAL";
    public static final String LEVEL_PROVINCE = "PROVINCE";
    public static final String LEVEL_CITY = "CITY";

    public static final String CATEGORY_QUALITY = "医疗质量";
    public static final String CATEGORY_SAFETY = "医疗安全";
    public static final String CATEGORY_PATIENT_SAFETY = "患者安全";
    public static final String CATEGORY_EFFICIENCY = "工作效率";

    public static final String DIRECTION_HIGHER = "HIGHER_BETTER";
    public static final String DIRECTION_LOWER = "LOWER_BETTER";

    /** 标准级别编码 → 中文 */
    public static String levelText(String level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case LEVEL_NATIONAL -> "国家";
            case LEVEL_PROVINCE -> "省";
            case LEVEL_CITY -> "市";
            default -> level;
        };
    }

    /** 标准级别中文 / 编码 → 编码（非法返回 null） */
    public static String parseLevel(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        return switch (t) {
            case LEVEL_NATIONAL, "国家", "国家标准" -> LEVEL_NATIONAL;
            case LEVEL_PROVINCE, "省", "省级", "省标准" -> LEVEL_PROVINCE;
            case LEVEL_CITY, "市", "市级", "市标准" -> LEVEL_CITY;
            default -> null;
        };
    }

    public static boolean isValidLevel(String level) {
        return LEVEL_NATIONAL.equals(level) || LEVEL_PROVINCE.equals(level) || LEVEL_CITY.equals(level);
    }

    /** 指标分类中文 / 编码 → 中文（非法返回 null） */
    public static String parseCategory(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        return switch (t) {
            case CATEGORY_QUALITY, "QUALITY" -> CATEGORY_QUALITY;
            case CATEGORY_SAFETY, "SAFETY" -> CATEGORY_SAFETY;
            case CATEGORY_PATIENT_SAFETY, "PATIENT_SAFETY" -> CATEGORY_PATIENT_SAFETY;
            case CATEGORY_EFFICIENCY, "EFFICIENCY" -> CATEGORY_EFFICIENCY;
            default -> null;
        };
    }

    public static boolean isValidCategory(String category) {
        return CATEGORY_QUALITY.equals(category) || CATEGORY_SAFETY.equals(category)
                || CATEGORY_PATIENT_SAFETY.equals(category) || CATEGORY_EFFICIENCY.equals(category);
    }

    /** 比较方向中文 / 编码 → 编码（非法返回 null） */
    public static String parseDirection(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        return switch (t) {
            case DIRECTION_HIGHER, "越高越好", "高者为优" -> DIRECTION_HIGHER;
            case DIRECTION_LOWER, "越低越好", "低者为优" -> DIRECTION_LOWER;
            default -> null;
        };
    }

    /** 比较方向编码 → 中文 */
    public static String directionText(String direction) {
        if (direction == null) {
            return "";
        }
        return switch (direction) {
            case DIRECTION_HIGHER -> "越高越好";
            case DIRECTION_LOWER -> "越低越好";
            default -> direction;
        };
    }

    /**
     * 判定一个指标是否达标：实际值与目标值任一为空时返回 null（未填报）。
     * 越高越好：实际值 ≥ 目标值；越低越好：实际值 ≤ 目标值。
     */
    public static Boolean evaluate(QualityIndicator indicator) {
        if (indicator == null || indicator.getActualValue() == null || indicator.getTargetValue() == null) {
            return null;
        }
        int cmp = indicator.getActualValue().compareTo(indicator.getTargetValue());
        if (DIRECTION_LOWER.equals(indicator.getDirection())) {
            return cmp <= 0;
        }
        return cmp >= 0;
    }

    /** 达标情况中文：达标 / 未达标 / 未填报 */
    public static String complianceText(QualityIndicator indicator) {
        Boolean ok = evaluate(indicator);
        if (ok == null) {
            return "未填报";
        }
        return ok ? "达标" : "未达标";
    }

    /** 达标率（百分比，保留 2 位小数）：达标数 / 已填报数；已填报数为 0 时返回 0 */
    public static BigDecimal rate(long compliant, long reported) {
        if (reported <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(compliant * 100.0 / reported).setScale(2, RoundingMode.HALF_UP);
    }

    /** 数值格式化：去掉多余的 0（如 33.30 → 33.3，80.00 → 80） */
    public static String fmtNumber(BigDecimal v) {
        if (v == null) {
            return "";
        }
        return v.stripTrailingZeros().toPlainString();
    }
}
