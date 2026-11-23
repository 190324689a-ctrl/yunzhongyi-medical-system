package com.yunzhongyi.medical.committee.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 委员会管理编码工具：委员会类别 / 委员会状态 / 决议状态对照。
 * <p>
 * 委员会类别 type：MEDICAL_QUALITY 医疗质量 / PHARMACY 药事 /
 * INFECTION 院感 / TRANSFUSION 输血 / MEDICAL_RECORD 病案 / OTHER 其他。
 * <p>
 * 委员会状态 status：1 在任，2 已换届，3 已解散。
 * <p>
 * 决议状态 status：0 待执行，1 执行中，2 已完成，3 已取消。
 */
public final class CommitteeTypeUtil {

    private CommitteeTypeUtil() {
    }

    // ---------- 委员会类别 ----------
    public static final String TYPE_MEDICAL_QUALITY = "MEDICAL_QUALITY";
    public static final String TYPE_PHARMACY = "PHARMACY";
    public static final String TYPE_INFECTION = "INFECTION";
    public static final String TYPE_TRANSFUSION = "TRANSFUSION";
    public static final String TYPE_MEDICAL_RECORD = "MEDICAL_RECORD";
    public static final String TYPE_OTHER = "OTHER";

    // ---------- 委员会状态 ----------
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_RENEWED = 2;
    public static final int STATUS_DISSOLVED = 3;

    // ---------- 决议状态：0 待执行，1 执行中，2 已完成，3 已取消 ----------
    public static final int RESOLUTION_PENDING = 0;
    public static final int RESOLUTION_IN_PROGRESS = 1;
    public static final int RESOLUTION_DONE = 2;
    public static final int RESOLUTION_CANCELLED = 3;

    /** 任期到期提醒窗口：90 天内到期即提醒 */
    public static final int EXPIRY_WARN_DAYS = 90;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 委员会类别编码 → 中文 */
    public static String typeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case TYPE_MEDICAL_QUALITY -> "医疗质量";
            case TYPE_PHARMACY -> "药事";
            case TYPE_INFECTION -> "院感";
            case TYPE_TRANSFUSION -> "输血";
            case TYPE_MEDICAL_RECORD -> "病案";
            case TYPE_OTHER -> "其他";
            default -> type;
        };
    }

    /** 委员会类别中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "医疗质量", "医疗质量管理", "MEDICAL_QUALITY" -> TYPE_MEDICAL_QUALITY;
            case "药事", "药事管理", "PHARMACY" -> TYPE_PHARMACY;
            case "院感", "医院感染", "INFECTION" -> TYPE_INFECTION;
            case "输血", "TRANSFUSION" -> TYPE_TRANSFUSION;
            case "病案", "病案管理", "MEDICAL_RECORD" -> TYPE_MEDICAL_RECORD;
            case "其他", "OTHER" -> TYPE_OTHER;
            default -> null;
        };
    }

    public static boolean isValidType(String type) {
        return TYPE_MEDICAL_QUALITY.equals(type) || TYPE_PHARMACY.equals(type)
                || TYPE_INFECTION.equals(type) || TYPE_TRANSFUSION.equals(type)
                || TYPE_MEDICAL_RECORD.equals(type) || TYPE_OTHER.equals(type);
    }

    /** 委员会状态 → 中文 */
    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_ACTIVE -> "在任";
            case STATUS_RENEWED -> "已换届";
            case STATUS_DISSOLVED -> "已解散";
            default -> String.valueOf(status);
        };
    }

    /** 委员会状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "在任" -> STATUS_ACTIVE;
            case "已换届" -> STATUS_RENEWED;
            case "已解散" -> STATUS_DISSOLVED;
            default -> null;
        };
    }

    /** 决议状态 → 中文 */
    public static String resolutionStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case RESOLUTION_PENDING -> "待执行";
            case RESOLUTION_IN_PROGRESS -> "执行中";
            case RESOLUTION_DONE -> "已完成";
            case RESOLUTION_CANCELLED -> "已取消";
            default -> String.valueOf(status);
        };
    }

    /** 决议是否逾期（待执行/执行中且决议期限已过当天） */
    public static boolean isResolutionOverdue(Integer status, LocalDate deadline) {
        if (deadline == null) {
            return false;
        }
        return (Integer.valueOf(RESOLUTION_PENDING).equals(status)
                || Integer.valueOf(RESOLUTION_IN_PROGRESS).equals(status))
                && deadline.isBefore(LocalDate.now());
    }

    /** 日期格式化（yyyy-MM-dd），为空返回 "" */
    public static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }
}
