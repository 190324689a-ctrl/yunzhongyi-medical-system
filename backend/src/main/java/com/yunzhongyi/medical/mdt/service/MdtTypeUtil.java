package com.yunzhongyi.medical.mdt.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MDT 会诊类型 / 紧急程度 / 审批状态工具：编码与中文对照。
 * <p>
 * 会诊类型 mdtType：IN_HOSPITAL 院内会诊 / OUT_HOSPITAL 院外会诊 /
 * REMOTE 远程会诊 / EMERGENCY 急会诊。
 * <p>
 * 紧急程度 urgency：NORMAL 一般 / URGENT 紧急。
 * <p>
 * 审批状态 status：0 待审批，1 已通过，2 已驳回（一级审批：医务科/科主任）。
 * 已结束的申请（已通过/已驳回）不能再审批，也不能再修改。
 */
public final class MdtTypeUtil {

    private MdtTypeUtil() {
    }

    public static final String TYPE_IN_HOSPITAL = "IN_HOSPITAL";
    public static final String TYPE_OUT_HOSPITAL = "OUT_HOSPITAL";
    public static final String TYPE_REMOTE = "REMOTE";
    public static final String TYPE_EMERGENCY = "EMERGENCY";

    public static final String URGENCY_NORMAL = "NORMAL";
    public static final String URGENCY_URGENT = "URGENT";

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 会诊类型编码 → 中文 */
    public static String typeText(String mdtType) {
        if (mdtType == null) {
            return "";
        }
        return switch (mdtType) {
            case TYPE_IN_HOSPITAL -> "院内会诊";
            case TYPE_OUT_HOSPITAL -> "院外会诊";
            case TYPE_REMOTE -> "远程会诊";
            case TYPE_EMERGENCY -> "急会诊";
            default -> mdtType;
        };
    }

    /** 会诊类型中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "院内会诊", "院内", "IN_HOSPITAL" -> TYPE_IN_HOSPITAL;
            case "院外会诊", "院外", "OUT_HOSPITAL" -> TYPE_OUT_HOSPITAL;
            case "远程会诊", "远程", "REMOTE" -> TYPE_REMOTE;
            case "急会诊", "急诊会诊", "EMERGENCY" -> TYPE_EMERGENCY;
            default -> null;
        };
    }

    /** 是否为合法会诊类型编码 */
    public static boolean isValidType(String mdtType) {
        return TYPE_IN_HOSPITAL.equals(mdtType) || TYPE_OUT_HOSPITAL.equals(mdtType)
                || TYPE_REMOTE.equals(mdtType) || TYPE_EMERGENCY.equals(mdtType);
    }

    /** 紧急程度编码 → 中文 */
    public static String urgencyText(String urgency) {
        if (urgency == null) {
            return "";
        }
        return switch (urgency) {
            case URGENCY_NORMAL -> "一般";
            case URGENCY_URGENT -> "紧急";
            default -> urgency;
        };
    }

    /** 紧急程度中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseUrgency(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "一般", "普通", "NORMAL" -> URGENCY_NORMAL;
            case "紧急", "特急", "URGENT" -> URGENCY_URGENT;
            default -> null;
        };
    }

    /** 是否为合法紧急程度编码 */
    public static boolean isValidUrgency(String urgency) {
        return URGENCY_NORMAL.equals(urgency) || URGENCY_URGENT.equals(urgency);
    }

    /** 审批状态 → 中文 */
    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_PENDING -> "待审批";
            case STATUS_APPROVED -> "已通过";
            case STATUS_REJECTED -> "已驳回";
            default -> String.valueOf(status);
        };
    }

    /** 审批状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "待审批", "待医务科审批", "待科主任审批" -> STATUS_PENDING;
            case "已通过", "通过" -> STATUS_APPROVED;
            case "已驳回", "驳回" -> STATUS_REJECTED;
            default -> null;
        };
    }

    /** 期望/实际会诊时间格式化（yyyy-MM-dd HH:mm），为空返回 "" */
    public static String fmtDateTime(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME_FMT);
    }
}
