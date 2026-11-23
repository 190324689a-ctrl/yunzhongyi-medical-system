package com.yunzhongyi.medical.quality.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 质量检查类型 / 严重程度 / 计划状态 / 整改状态 / 等级工具：编码与中文对照、
 * 评分项 JSON 解析、总分与等级自动计算。
 * <p>
 * 检查类型 inspectType：SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查。
 * <p>
 * 计划状态 status：0 待执行，1 进行中，2 已完成，3 已取消。
 * <p>
 * 严重程度 severity：MAJOR 严重 / MINOR 一般 / MILD 轻微。
 * <p>
 * 整改状态 status：0 待整改，1 已整改（待复查），2 已复查通过；
 * 逾期不单独存状态，按“待整改/已整改且整改期限已过当天”动态判断。
 * <p>
 * 等级规则（总分满分 100）：≥90 优秀 / ≥80 合格 / ≥60 基本合格 / &lt;60 不合格。
 */
@Slf4j
public final class QualityTypeUtil {

    private QualityTypeUtil() {
    }

    public static final String TYPE_SELF = "SELF";
    public static final String TYPE_HOSPITAL = "HOSPITAL";
    public static final String TYPE_SPECIAL = "SPECIAL";

    /** 计划状态：0 待执行，1 进行中，2 已完成，3 已取消 */
    public static final int PLAN_PENDING = 0;
    public static final int PLAN_IN_PROGRESS = 1;
    public static final int PLAN_COMPLETED = 2;
    public static final int PLAN_CANCELLED = 3;

    public static final String SEVERITY_MAJOR = "MAJOR";
    public static final String SEVERITY_MINOR = "MINOR";
    public static final String SEVERITY_MILD = "MILD";

    /** 整改状态：0 待整改，1 已整改（待复查），2 已复查通过 */
    public static final int RECTIFY_PENDING = 0;
    public static final int RECTIFY_DONE = 1;
    public static final int RECTIFY_REVIEWED = 2;

    public static final String GRADE_EXCELLENT = "EXCELLENT";
    public static final String GRADE_QUALIFIED = "QUALIFIED";
    public static final String GRADE_BASIC = "BASIC";
    public static final String GRADE_UNQUALIFIED = "UNQUALIFIED";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 检查类型编码 → 中文 */
    public static String typeText(String inspectType) {
        if (inspectType == null) {
            return "";
        }
        return switch (inspectType) {
            case TYPE_SELF -> "科室自查";
            case TYPE_HOSPITAL -> "院级检查";
            case TYPE_SPECIAL -> "专项检查";
            default -> inspectType;
        };
    }

    /** 检查类型中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "科室自查", "自查", TYPE_SELF -> TYPE_SELF;
            case "院级检查", "院级", TYPE_HOSPITAL -> TYPE_HOSPITAL;
            case "专项检查", "专项", TYPE_SPECIAL -> TYPE_SPECIAL;
            default -> null;
        };
    }

    /** 是否为合法检查类型编码 */
    public static boolean isValidType(String inspectType) {
        return TYPE_SELF.equals(inspectType) || TYPE_HOSPITAL.equals(inspectType)
                || TYPE_SPECIAL.equals(inspectType);
    }

    /** 计划状态 → 中文 */
    public static String planStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PLAN_PENDING -> "待执行";
            case PLAN_IN_PROGRESS -> "进行中";
            case PLAN_COMPLETED -> "已完成";
            case PLAN_CANCELLED -> "已取消";
            default -> String.valueOf(status);
        };
    }

    /** 计划状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parsePlanStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "待执行", "待检查" -> PLAN_PENDING;
            case "进行中", "检查中" -> PLAN_IN_PROGRESS;
            case "已完成", "完成" -> PLAN_COMPLETED;
            case "已取消", "取消" -> PLAN_CANCELLED;
            default -> null;
        };
    }

    /** 严重程度编码 → 中文 */
    public static String severityText(String severity) {
        if (severity == null) {
            return "";
        }
        return switch (severity) {
            case SEVERITY_MAJOR -> "严重";
            case SEVERITY_MINOR -> "一般";
            case SEVERITY_MILD -> "轻微";
            default -> severity;
        };
    }

    /** 严重程度中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseSeverity(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "严重", SEVERITY_MAJOR -> SEVERITY_MAJOR;
            case "一般", SEVERITY_MINOR -> SEVERITY_MINOR;
            case "轻微", SEVERITY_MILD -> SEVERITY_MILD;
            default -> null;
        };
    }

    /** 是否为合法严重程度编码 */
    public static boolean isValidSeverity(String severity) {
        return SEVERITY_MAJOR.equals(severity) || SEVERITY_MINOR.equals(severity)
                || SEVERITY_MILD.equals(severity);
    }

    /** 整改状态 → 中文 */
    public static String rectifyStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case RECTIFY_PENDING -> "待整改";
            case RECTIFY_DONE -> "已整改";
            case RECTIFY_REVIEWED -> "已复查通过";
            default -> String.valueOf(status);
        };
    }

    /** 总分 → 等级 */
    public static String gradeOf(BigDecimal totalScore) {
        if (totalScore == null) {
            return null;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return GRADE_EXCELLENT;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return GRADE_QUALIFIED;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return GRADE_BASIC;
        }
        return GRADE_UNQUALIFIED;
    }

    /** 等级编码 → 中文 */
    public static String gradeText(String grade) {
        if (grade == null) {
            return "";
        }
        return switch (grade) {
            case GRADE_EXCELLENT -> "优秀";
            case GRADE_QUALIFIED -> "合格";
            case GRADE_BASIC -> "基本合格";
            case GRADE_UNQUALIFIED -> "不合格";
            default -> grade;
        };
    }

    /** 等级中文 / 编码 → 编码 */
    public static String parseGrade(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "优秀", GRADE_EXCELLENT -> GRADE_EXCELLENT;
            case "合格", GRADE_QUALIFIED -> GRADE_QUALIFIED;
            case "基本合格", GRADE_BASIC -> GRADE_BASIC;
            case "不合格", GRADE_UNQUALIFIED -> GRADE_UNQUALIFIED;
            default -> null;
        };
    }

    /** 是否逾期：待整改/已整改且整改期限已过当天（已复查通过的不算逾期） */
    public static boolean isOverdue(Integer status, LocalDate deadline) {
        if (deadline == null || status == null) {
            return false;
        }
        if (Integer.valueOf(RECTIFY_REVIEWED).equals(status)) {
            return false;
        }
        return deadline.isBefore(LocalDate.now());
    }

    /** 模板检查项 JSON → 检查项列表 [{name, weight, standard}]；解析失败返回空列表 */
    public static List<Map<String, Object>> parseCheckItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("检查模板检查项 JSON 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** 模板检查项权重之和 */
    public static BigDecimal sumWeights(String json) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : parseCheckItems(json)) {
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

    /** 检查项权重 Map（检查项名 → 权重），供打分校验用 */
    public static Map<String, BigDecimal> weightMap(String json) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map<String, Object> item : parseCheckItems(json)) {
            Object name = item.get("name");
            Object w = item.get("weight");
            if (name != null && w != null) {
                try {
                    result.put(String.valueOf(name), new BigDecimal(String.valueOf(w)));
                } catch (NumberFormatException e) {
                    log.warn("检查项权重解析失败，忽略该项 {}={}", name, w);
                }
            }
        }
        return result;
    }

    /** 各项得分 JSON → 有序 Map（键：检查项名，值：得分）；解析失败返回空 Map */
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
                    log.warn("检查得分解析失败，忽略该项 {}={}", k, v);
                }
            });
        } catch (Exception e) {
            log.warn("检查各项得分 JSON 解析失败: {}", e.getMessage());
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

    /** 各项得分 JSON → 展示文本，如“病历书写规范 27；核心制度执行 26” */
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

    /** 平均分（保留两位小数），无记录时返回 0 */
    public static BigDecimal avgScore(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal s : scores) {
            sum = sum.add(s);
        }
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }
}
