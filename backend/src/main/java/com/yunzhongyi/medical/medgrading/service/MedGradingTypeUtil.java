package com.yunzhongyi.medical.medgrading.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * 医疗分级管理编码工具：分级范围 / 等级判定 / 评定周期校验。
 * <p>
 * 覆盖需求（需求梳理.md 第 22 项）：医疗分级、医疗质量分级管理。
 * <p>
 * 分级范围 scope：QUALITY 医疗质量分级 / SERVICE 医疗服务分级 /
 * COMPREHENSIVE 综合分级。
 * <p>
 * 等级 grade：A 甲级 / B 乙级 / C 丙级 / D 丁级，按分级标准的分数线自动算：
 * score &gt;= excellentMin 为甲级，&gt;= goodMin 为乙级，&gt;= passMin 为丙级，否则丁级。
 * <p>
 * 评定周期 period：yyyy-MM，如 2026-11。
 */
public final class MedGradingTypeUtil {

    private MedGradingTypeUtil() {
    }

    // ---------- 分级范围 ----------
    public static final String SCOPE_QUALITY = "QUALITY";
    public static final String SCOPE_SERVICE = "SERVICE";
    public static final String SCOPE_COMPREHENSIVE = "COMPREHENSIVE";

    public static boolean isValidScope(String scope) {
        return SCOPE_QUALITY.equals(scope) || SCOPE_SERVICE.equals(scope)
                || SCOPE_COMPREHENSIVE.equals(scope);
    }

    public static String scopeText(String scope) {
        if (scope == null) {
            return "";
        }
        return switch (scope) {
            case SCOPE_QUALITY -> "医疗质量分级";
            case SCOPE_SERVICE -> "医疗服务分级";
            case SCOPE_COMPREHENSIVE -> "综合分级";
            default -> scope;
        };
    }

    /** 分级范围中文 / 编码 → 编码，认不出时返回 null */
    public static String parseScope(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "医疗质量分级", "QUALITY" -> SCOPE_QUALITY;
            case "医疗服务分级", "SERVICE" -> SCOPE_SERVICE;
            case "综合分级", "COMPREHENSIVE" -> SCOPE_COMPREHENSIVE;
            default -> null;
        };
    }

    // ---------- 等级 ----------
    public static final String GRADE_A = "A";
    public static final String GRADE_B = "B";
    public static final String GRADE_C = "C";
    public static final String GRADE_D = "D";

    public static String gradeText(String grade) {
        if (grade == null) {
            return "";
        }
        return switch (grade) {
            case GRADE_A -> "甲级";
            case GRADE_B -> "乙级";
            case GRADE_C -> "丙级";
            case GRADE_D -> "丁级";
            default -> grade;
        };
    }

    /**
     * 按分数线自动算等级：&gt;= excellentMin 甲级，&gt;= goodMin 乙级，
     * &gt;= passMin 丙级，否则丁级。分数线须满足 excellentMin &gt; goodMin &gt; passMin。
     */
    public static String calcGrade(BigDecimal score, BigDecimal excellentMin,
                                   BigDecimal goodMin, BigDecimal passMin) {
        if (score == null) {
            throw new IllegalArgumentException("评分不能为空");
        }
        if (excellentMin == null || goodMin == null || passMin == null) {
            throw new IllegalArgumentException("分级标准分数线不完整");
        }
        if (score.compareTo(excellentMin) >= 0) {
            return GRADE_A;
        }
        if (score.compareTo(goodMin) >= 0) {
            return GRADE_B;
        }
        if (score.compareTo(passMin) >= 0) {
            return GRADE_C;
        }
        return GRADE_D;
    }

    /** 评定状态：0 待审批，1 已通过，2 已驳回 */
    public static final int ASSESS_PENDING = 0;
    public static final int ASSESS_APPROVED = 1;
    public static final int ASSESS_REJECTED = 2;

    public static String assessStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case ASSESS_PENDING -> "待审批";
            case ASSESS_APPROVED -> "已通过";
            case ASSESS_REJECTED -> "已驳回";
            default -> String.valueOf(status);
        };
    }

    /** 标准状态：1 启用，0 停用 */
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    // ---------- 评定周期 ----------
    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    public static boolean isValidPeriod(String period) {
        return period != null && PERIOD_PATTERN.matcher(period.trim()).matches();
    }
}
