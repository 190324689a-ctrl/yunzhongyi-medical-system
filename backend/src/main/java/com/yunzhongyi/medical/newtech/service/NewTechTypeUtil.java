package com.yunzhongyi.medical.newtech.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 新技术、新项目类型 / 伦理审查状态 / 审批状态工具：编码与中文对照。
 * <p>
 * 项目类型 projectType：NEW_TECH 新技术 / NEW_PROJECT 新项目。
 * <p>
 * 审批状态 status：0 待审批，1 已立项，2 已验收，3 已驳回，4 已推广
 * （一级审批：医务科；验收/推广由进展记录驱动状态流转）。
 * <p>
 * 伦理审查状态 ethicsStatus：0 不需审查，1 待审查，2 已通过，3 未通过。
 * 需伦理审查的项目，伦理未通过前不能审批立项。
 * <p>
 * 进展记录类型 recordType：PROGRESS 进展记录 / ACCEPTANCE 验收记录 /
 * PROMOTION 推广记录。
 */
public final class NewTechTypeUtil {

    private NewTechTypeUtil() {
    }

    public static final String TYPE_NEW_TECH = "NEW_TECH";
    public static final String TYPE_NEW_PROJECT = "NEW_PROJECT";

    public static final String RECORD_PROGRESS = "PROGRESS";
    public static final String RECORD_ACCEPTANCE = "ACCEPTANCE";
    public static final String RECORD_PROMOTION = "PROMOTION";

    /** 审批状态：0 待审批，1 已立项，2 已验收，3 已驳回，4 已推广 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_ACCEPTED = 2;
    public static final int STATUS_REJECTED = 3;
    public static final int STATUS_PROMOTED = 4;

    /** 伦理审查状态：0 不需审查，1 待审查，2 已通过，3 未通过 */
    public static final int ETHICS_NOT_REQUIRED = 0;
    public static final int ETHICS_PENDING = 1;
    public static final int ETHICS_PASSED = 2;
    public static final int ETHICS_FAILED = 3;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 项目类型编码 → 中文 */
    public static String typeText(String projectType) {
        if (projectType == null) {
            return "";
        }
        return switch (projectType) {
            case TYPE_NEW_TECH -> "新技术";
            case TYPE_NEW_PROJECT -> "新项目";
            default -> projectType;
        };
    }

    /** 项目类型中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "新技术", "NEW_TECH" -> TYPE_NEW_TECH;
            case "新项目", "NEW_PROJECT" -> TYPE_NEW_PROJECT;
            default -> null;
        };
    }

    /** 是否为合法项目类型编码 */
    public static boolean isValidType(String projectType) {
        return TYPE_NEW_TECH.equals(projectType) || TYPE_NEW_PROJECT.equals(projectType);
    }

    /** 审批状态 → 中文 */
    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_PENDING -> "待审批";
            case STATUS_APPROVED -> "已立项";
            case STATUS_ACCEPTED -> "已验收";
            case STATUS_REJECTED -> "已驳回";
            case STATUS_PROMOTED -> "已推广";
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
            case "已立项", "通过", "已通过" -> STATUS_APPROVED;
            case "已验收", "验收通过" -> STATUS_ACCEPTED;
            case "已驳回", "驳回" -> STATUS_REJECTED;
            case "已推广" -> STATUS_PROMOTED;
            default -> null;
        };
    }

    /** 伦理审查状态 → 中文 */
    public static String ethicsText(Integer ethicsStatus) {
        if (ethicsStatus == null) {
            return "";
        }
        return switch (ethicsStatus) {
            case ETHICS_NOT_REQUIRED -> "不需审查";
            case ETHICS_PENDING -> "待审查";
            case ETHICS_PASSED -> "已通过";
            case ETHICS_FAILED -> "未通过";
            default -> String.valueOf(ethicsStatus);
        };
    }

    /** 伦理审查状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseEthicsStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "不需审查", "不需", "无需审查" -> ETHICS_NOT_REQUIRED;
            case "待审查", "待伦理审查" -> ETHICS_PENDING;
            case "已通过", "通过", "伦理通过" -> ETHICS_PASSED;
            case "未通过", "不通过", "伦理未通过" -> ETHICS_FAILED;
            default -> null;
        };
    }

    /** 是否需伦理审查中文 → Boolean（导入 Excel 用，认不出时返回 null） */
    public static Boolean parseEthicsRequired(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "是", "需审查", "需要", "Y", "YES", "true" -> Boolean.TRUE;
            case "否", "不需审查", "不需要", "N", "NO", "false" -> Boolean.FALSE;
            default -> null;
        };
    }

    /** 进展记录类型编码 → 中文 */
    public static String recordTypeText(String recordType) {
        if (recordType == null) {
            return "";
        }
        return switch (recordType) {
            case RECORD_PROGRESS -> "进展记录";
            case RECORD_ACCEPTANCE -> "验收记录";
            case RECORD_PROMOTION -> "推广记录";
            default -> recordType;
        };
    }

    /** 进展记录类型中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseRecordType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "进展记录", "进展", "PROGRESS" -> RECORD_PROGRESS;
            case "验收记录", "验收", "ACCEPTANCE" -> RECORD_ACCEPTANCE;
            case "推广记录", "推广", "PROMOTION" -> RECORD_PROMOTION;
            default -> null;
        };
    }

    /** 是否为合法进展记录类型编码 */
    public static boolean isValidRecordType(String recordType) {
        return RECORD_PROGRESS.equals(recordType) || RECORD_ACCEPTANCE.equals(recordType)
                || RECORD_PROMOTION.equals(recordType);
    }

    /** 日期格式化（yyyy-MM-dd），为空返回 "" */
    public static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    /** 预算格式化（万元），为空返回 "" */
    public static String fmtBudget(BigDecimal budget) {
        return budget == null ? "" : budget.stripTrailingZeros().toPlainString();
    }
}
