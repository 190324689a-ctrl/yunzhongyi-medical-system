package com.yunzhongyi.medical.briefing.service;

import java.util.List;

/**
 * 医疗工作简报类型工具：简报分类 / 发布状态编码与中文对照。
 * <p>
 * 简报分类 category：MONTHLY 月报 / QUARTERLY 季报 / ANNUAL 年报 /
 * SPECIAL 专项简报 / DYNAMIC 工作动态。
 * <p>
 * 发布状态 status：0 草稿，1 待审批，2 已发布，3 已驳回。
 */
public final class BriefingTypeUtil {

    private BriefingTypeUtil() {
    }

    public static final String CATEGORY_MONTHLY = "MONTHLY";
    public static final String CATEGORY_QUARTERLY = "QUARTERLY";
    public static final String CATEGORY_ANNUAL = "ANNUAL";
    public static final String CATEGORY_SPECIAL = "SPECIAL";
    public static final String CATEGORY_DYNAMIC = "DYNAMIC";

    public static final List<String> CATEGORIES = List.of(
            CATEGORY_MONTHLY, CATEGORY_QUARTERLY, CATEGORY_ANNUAL,
            CATEGORY_SPECIAL, CATEGORY_DYNAMIC);

    /** 发布状态：0 草稿，1 待审批，2 已发布，3 已驳回 */
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_PUBLISHED = 2;
    public static final int STATUS_REJECTED = 3;

    /** 简报分类编码 → 中文 */
    public static String categoryText(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case CATEGORY_MONTHLY -> "月报";
            case CATEGORY_QUARTERLY -> "季报";
            case CATEGORY_ANNUAL -> "年报";
            case CATEGORY_SPECIAL -> "专项简报";
            case CATEGORY_DYNAMIC -> "工作动态";
            default -> category;
        };
    }

    /** 简报分类中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "月报", "MONTHLY" -> CATEGORY_MONTHLY;
            case "季报", "QUARTERLY" -> CATEGORY_QUARTERLY;
            case "年报", "ANNUAL" -> CATEGORY_ANNUAL;
            case "专项简报", "SPECIAL" -> CATEGORY_SPECIAL;
            case "工作动态", "DYNAMIC" -> CATEGORY_DYNAMIC;
            default -> null;
        };
    }

    /** 发布状态 → 中文 */
    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PENDING -> "待审批";
            case STATUS_PUBLISHED -> "已发布";
            case STATUS_REJECTED -> "已驳回";
            default -> String.valueOf(status);
        };
    }

    /** 发布状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "草稿", "0" -> STATUS_DRAFT;
            case "待审批", "1" -> STATUS_PENDING;
            case "已发布", "2" -> STATUS_PUBLISHED;
            case "已驳回", "3" -> STATUS_REJECTED;
            default -> null;
        };
    }

    /** 模板启用状态 → 中文 */
    public static String templateStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return status == 1 ? "启用" : "停用";
    }

    /** 模板启用状态中文 → 编码（认不出时返回 null） */
    public static Integer parseTemplateStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "启用", "1" -> 1;
            case "停用", "0" -> 0;
            default -> null;
        };
    }
}
