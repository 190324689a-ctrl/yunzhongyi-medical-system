package com.yunzhongyi.medical.assessment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 考核评分工具：等级自动计算 + 评分项 JSON 解析。
 * <p>
 * 等级规则（总分满分 100）：≥90 优秀 / ≥80 合格 / ≥60 基本合格 / &lt;60 不合格。
 */
@Slf4j
public final class AssessmentGradeUtil {

    private AssessmentGradeUtil() {
    }

    public static final String EXCELLENT = "EXCELLENT";
    public static final String QUALIFIED = "QUALIFIED";
    public static final String BASIC = "BASIC";
    public static final String UNQUALIFIED = "UNQUALIFIED";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 总分 → 等级 */
    public static String gradeOf(BigDecimal totalScore) {
        if (totalScore == null) {
            return null;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return EXCELLENT;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return QUALIFIED;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return BASIC;
        }
        return UNQUALIFIED;
    }

    /** 等级编码 → 中文 */
    public static String gradeText(String grade) {
        if (grade == null) {
            return "";
        }
        return switch (grade) {
            case EXCELLENT -> "优秀";
            case QUALIFIED -> "合格";
            case BASIC -> "基本合格";
            case UNQUALIFIED -> "不合格";
            default -> grade;
        };
    }

    /** 等级中文 / 编码 → 编码 */
    public static String parseGrade(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "优秀", EXCELLENT -> EXCELLENT;
            case "合格", QUALIFIED -> QUALIFIED;
            case "基本合格", BASIC -> BASIC;
            case "不合格", UNQUALIFIED -> UNQUALIFIED;
            default -> null;
        };
    }

    /** 考核类型编码 → 中文 */
    public static String typeText(String v) {
        if (v == null) {
            return "";
        }
        return switch (v) {
            case "PERIODIC" -> "定期考核";
            case "ETHICS" -> "医德考评";
            case "QUALITY" -> "医疗质量考核";
            default -> v;
        };
    }

    /** 考核类型中文 / 编码 → 编码 */
    public static String parseType(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "定期考核", "PERIODIC" -> "PERIODIC";
            case "医德考评", "ETHICS" -> "ETHICS";
            case "医疗质量考核", "QUALITY" -> "QUALITY";
            default -> null;
        };
    }

    /** 各项得分 JSON → 有序 Map（键：评分项名，值：得分）；解析失败返回空 Map */
    public static Map<String, BigDecimal> parseItemScores(String json) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            Map<String, Object> raw = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            raw.forEach((k, v) -> {
                try {
                    result.put(k, new BigDecimal(String.valueOf(v)));
                } catch (NumberFormatException e) {
                    log.warn("考核得分解析失败，忽略该项 {}={}", k, v);
                }
            });
        } catch (Exception e) {
            log.warn("考核各项得分 JSON 解析失败: {}", e.getMessage());
        }
        return result;
    }

    /** 各项得分求和 */
    public static BigDecimal sumItemScores(String json) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : parseItemScores(json).values()) {
            total = total.add(v);
        }
        return total;
    }

    /** 模板评分项 JSON → 评分项列表 [{name, weight}]；解析失败返回空列表 */
    public static List<Map<String, Object>> parseScoreItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("考核模板评分项 JSON 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** 模板评分项权重之和 */
    public static BigDecimal sumWeights(String json) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : parseScoreItems(json)) {
            Object w = item.get("weight");
            if (w != null) {
                try {
                    total = total.add(new BigDecimal(String.valueOf(w)));
                } catch (NumberFormatException ignored) {
                    // 权重非法时校验会另外报错
                }
            }
        }
        return total;
    }

    /** 各项得分 JSON → 展示文本，如“医德医风 28；业务能力 28” */
    public static String itemScoresText(String json) {
        StringBuilder sb = new StringBuilder();
        parseItemScores(json).forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(k).append(" ").append(v.stripTrailingZeros().toPlainString());
        });
        return sb.toString();
    }
}
