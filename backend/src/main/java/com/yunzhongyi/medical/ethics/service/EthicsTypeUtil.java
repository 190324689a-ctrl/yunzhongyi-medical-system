package com.yunzhongyi.medical.ethics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医德医风管理编码工具：档案状态 / 考评等级 / 投诉类型与流转状态 /
 * 表彰类型与级别对照、评分项 JSON 解析、逾期判定。
 * <p>
 * 考评等级规则（总分满分 100）：≥90 优秀 / ≥80 合格 / ≥60 基本合格 / &lt;60 不合格，
 * 与医师考核管理（模块 4）一致。
 * <p>
 * 投诉处理期限：默认投诉日期 + 15 天（HANDLE_DEADLINE_DAYS）。
 */
@Slf4j
public final class EthicsTypeUtil {

    private EthicsTypeUtil() {
    }

    // ---------- 医德档案状态 ----------
    public static final int PROFILE_ACTIVE = 0;
    public static final int PROFILE_ARCHIVED = 1;

    // ---------- 考评 / 表彰审批状态 ----------
    public static final int APPROVAL_PENDING = 0;
    public static final int APPROVAL_PASSED = 1;
    public static final int APPROVAL_REJECTED = 2;

    // ---------- 投诉流转状态 ----------
    public static final int COMPLAINT_PENDING = 0;
    public static final int COMPLAINT_INVESTIGATING = 1;
    public static final int COMPLAINT_HANDLED = 2;
    public static final int COMPLAINT_FEEDBACK = 3;
    public static final int COMPLAINT_REJECTED = 4;

    // ---------- 考评等级 ----------
    public static final String GRADE_EXCELLENT = "EXCELLENT";
    public static final String GRADE_QUALIFIED = "QUALIFIED";
    public static final String GRADE_BASIC = "BASIC";
    public static final String GRADE_UNQUALIFIED = "UNQUALIFIED";

    // ---------- 投诉类型 ----------
    public static final String COMPLAINT_ATTITUDE = "ATTITUDE";
    public static final String COMPLAINT_QUALITY = "QUALITY";
    public static final String COMPLAINT_CHARGE = "CHARGE";
    public static final String COMPLAINT_INTEGRITY = "INTEGRITY";
    public static final String COMPLAINT_OTHER = "OTHER";

    // ---------- 表彰类型 ----------
    public static final String COMMEND_BANNER = "BANNER";
    public static final String COMMEND_LETTER = "LETTER";
    public static final String COMMEND_NOTICE = "NOTICE";
    public static final String COMMEND_HONOR = "HONOR";
    public static final String COMMEND_OTHER = "OTHER";

    // ---------- 表彰级别 ----------
    public static final String LEVEL_HOSPITAL = "HOSPITAL";
    public static final String LEVEL_CITY = "CITY";
    public static final String LEVEL_PROVINCE = "PROVINCE";
    public static final String LEVEL_NATIONAL = "NATIONAL";

    /** 投诉处理期限：投诉日期 + 15 天 */
    public static final int HANDLE_DEADLINE_DAYS = 15;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---------- 档案状态 ----------
    public static String profileStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PROFILE_ACTIVE -> "在档";
            case PROFILE_ARCHIVED -> "封存";
            default -> String.valueOf(status);
        };
    }

    // ---------- 审批状态 ----------
    public static String approvalStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case APPROVAL_PENDING -> "待审批";
            case APPROVAL_PASSED -> "已通过";
            case APPROVAL_REJECTED -> "已驳回";
            default -> String.valueOf(status);
        };
    }

    // ---------- 投诉流转状态 ----------
    public static String complaintStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case COMPLAINT_PENDING -> "待受理";
            case COMPLAINT_INVESTIGATING -> "调查中";
            case COMPLAINT_HANDLED -> "已处理";
            case COMPLAINT_FEEDBACK -> "已反馈";
            case COMPLAINT_REJECTED -> "不予受理";
            default -> String.valueOf(status);
        };
    }

    public static Integer parseComplaintStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "待受理" -> COMPLAINT_PENDING;
            case "调查中" -> COMPLAINT_INVESTIGATING;
            case "已处理" -> COMPLAINT_HANDLED;
            case "已反馈" -> COMPLAINT_FEEDBACK;
            case "不予受理" -> COMPLAINT_REJECTED;
            default -> null;
        };
    }

    // ---------- 考评等级 ----------
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

    // ---------- 投诉类型 ----------
    public static String complaintTypeText(String v) {
        if (v == null) {
            return "";
        }
        return switch (v) {
            case COMPLAINT_ATTITUDE -> "服务态度";
            case COMPLAINT_QUALITY -> "医疗质量";
            case COMPLAINT_CHARGE -> "收费问题";
            case COMPLAINT_INTEGRITY -> "廉洁行医";
            case COMPLAINT_OTHER -> "其他";
            default -> v;
        };
    }

    public static String parseComplaintType(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "服务态度", COMPLAINT_ATTITUDE -> COMPLAINT_ATTITUDE;
            case "医疗质量", COMPLAINT_QUALITY -> COMPLAINT_QUALITY;
            case "收费问题", COMPLAINT_CHARGE -> COMPLAINT_CHARGE;
            case "廉洁行医", COMPLAINT_INTEGRITY -> COMPLAINT_INTEGRITY;
            case "其他", COMPLAINT_OTHER -> COMPLAINT_OTHER;
            default -> null;
        };
    }

    public static boolean isValidComplaintType(String v) {
        return COMPLAINT_ATTITUDE.equals(v) || COMPLAINT_QUALITY.equals(v)
                || COMPLAINT_CHARGE.equals(v) || COMPLAINT_INTEGRITY.equals(v)
                || COMPLAINT_OTHER.equals(v);
    }

    // ---------- 表彰类型 ----------
    public static String commendTypeText(String v) {
        if (v == null) {
            return "";
        }
        return switch (v) {
            case COMMEND_BANNER -> "锦旗";
            case COMMEND_LETTER -> "表扬信";
            case COMMEND_NOTICE -> "通报表扬";
            case COMMEND_HONOR -> "荣誉称号";
            case COMMEND_OTHER -> "其他";
            default -> v;
        };
    }

    public static String parseCommendType(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "锦旗", COMMEND_BANNER -> COMMEND_BANNER;
            case "表扬信", COMMEND_LETTER -> COMMEND_LETTER;
            case "通报表扬", COMMEND_NOTICE -> COMMEND_NOTICE;
            case "荣誉称号", COMMEND_HONOR -> COMMEND_HONOR;
            case "其他", COMMEND_OTHER -> COMMEND_OTHER;
            default -> null;
        };
    }

    public static boolean isValidCommendType(String v) {
        return COMMEND_BANNER.equals(v) || COMMEND_LETTER.equals(v)
                || COMMEND_NOTICE.equals(v) || COMMEND_HONOR.equals(v)
                || COMMEND_OTHER.equals(v);
    }

    // ---------- 表彰级别 ----------
    public static String commendLevelText(String v) {
        if (v == null) {
            return "";
        }
        return switch (v) {
            case LEVEL_HOSPITAL -> "院级";
            case LEVEL_CITY -> "市级";
            case LEVEL_PROVINCE -> "省级";
            case LEVEL_NATIONAL -> "国家级";
            default -> v;
        };
    }

    public static String parseCommendLevel(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return switch (s.trim()) {
            case "院级", LEVEL_HOSPITAL -> LEVEL_HOSPITAL;
            case "市级", LEVEL_CITY -> LEVEL_CITY;
            case "省级", LEVEL_PROVINCE -> LEVEL_PROVINCE;
            case "国家级", LEVEL_NATIONAL -> LEVEL_NATIONAL;
            default -> null;
        };
    }

    public static boolean isValidCommendLevel(String v) {
        return LEVEL_HOSPITAL.equals(v) || LEVEL_CITY.equals(v)
                || LEVEL_PROVINCE.equals(v) || LEVEL_NATIONAL.equals(v);
    }

    // ---------- 逾期判定 ----------
    /** 是否逾期（未到已反馈、未不予受理，且处理期限已过当天） */
    public static boolean isComplaintOverdue(Integer status, LocalDate deadlineDate) {
        if (deadlineDate == null) {
            return false;
        }
        return (Integer.valueOf(COMPLAINT_PENDING).equals(status)
                || Integer.valueOf(COMPLAINT_INVESTIGATING).equals(status)
                || Integer.valueOf(COMPLAINT_HANDLED).equals(status))
                && deadlineDate.isBefore(LocalDate.now());
    }

    // ---------- 评分项 JSON ----------
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
                    log.warn("医德考评得分解析失败，忽略该项 {}={}", k, v);
                }
            });
        } catch (Exception e) {
            log.warn("医德考评各项得分 JSON 解析失败: {}", e.getMessage());
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
            log.warn("医德考评模板评分项 JSON 解析失败: {}", e.getMessage());
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

    /** 各项得分 JSON → 展示文本，如“医德医风 47；服务态度 24” */
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

    /** 日期格式化（yyyy-MM-dd），为空返回 "" */
    public static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }
}
