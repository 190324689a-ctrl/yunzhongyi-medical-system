package com.yunzhongyi.medical.personnel.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 医疗人员动态变动类型 / 审批状态工具：编码与中文对照。
 * <p>
 * 变动类型 changeType：TRANSFER 调动 / RESIGN 离职 / RETIRE 退休 /
 * JOIN 入职 / REHIRE 返聘。
 * <p>
 * 审批状态 status：0 待审批，1 已通过，2 已驳回
 * （一级审批：医务科；审批通过时同步医师档案的科室/在职状态）。
 */
public final class PersonnelChangeTypeUtil {

    private PersonnelChangeTypeUtil() {
    }

    public static final String TYPE_TRANSFER = "TRANSFER";
    public static final String TYPE_RESIGN = "RESIGN";
    public static final String TYPE_RETIRE = "RETIRE";
    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_REHIRE = "REHIRE";

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;

    /** 医师档案在职状态（见 Physician.status）：1 在职，4 离职，5 退休 */
    public static final int PHYSICIAN_ACTIVE = 1;
    public static final int PHYSICIAN_RESIGNED = 4;
    public static final int PHYSICIAN_RETIRED = 5;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 变动类型编码 → 中文 */
    public static String typeText(String changeType) {
        if (changeType == null) {
            return "";
        }
        return switch (changeType) {
            case TYPE_TRANSFER -> "调动";
            case TYPE_RESIGN -> "离职";
            case TYPE_RETIRE -> "退休";
            case TYPE_JOIN -> "入职";
            case TYPE_REHIRE -> "返聘";
            default -> changeType;
        };
    }

    /** 变动类型中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "调动", "TRANSFER" -> TYPE_TRANSFER;
            case "离职", "RESIGN" -> TYPE_RESIGN;
            case "退休", "RETIRE" -> TYPE_RETIRE;
            case "入职", "JOIN" -> TYPE_JOIN;
            case "返聘", "REHIRE" -> TYPE_REHIRE;
            default -> null;
        };
    }

    /** 是否为合法变动类型编码 */
    public static boolean isValidType(String changeType) {
        return TYPE_TRANSFER.equals(changeType) || TYPE_RESIGN.equals(changeType)
                || TYPE_RETIRE.equals(changeType) || TYPE_JOIN.equals(changeType)
                || TYPE_REHIRE.equals(changeType);
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
            case "待审批" -> STATUS_PENDING;
            case "已通过", "通过" -> STATUS_APPROVED;
            case "已驳回", "驳回" -> STATUS_REJECTED;
            default -> null;
        };
    }

    /** 日期格式化（yyyy-MM-dd），为空返回 "" */
    public static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }
}
