package com.yunzhongyi.medical.completion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 医务系统完成统计编码工具：完成状态 / 分组 / 完成率。
 * <p>
 * 覆盖需求（需求梳理.md 第 27 项）：医务系统各模块完成情况统计。
 * <p>
 * 状态 status：0 待开发 / 1 进行中 / 2 已完成。
 * <p>
 * 分组 category：医师管理（1-8 号模块）/ 组织与制度（9-12）/
 * 质量管理（13-22）/ 科研与统计（23-27）/ 系统与移动（28-31）。
 * <p>
 * 完成率 = 已完成模块数 / 模块总数 × 100，保留两位小数。
 */
public final class CompletionTypeUtil {

    private CompletionTypeUtil() {
    }

    // ---------- 状态 ----------

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETED = 2;

    public static boolean isValidStatus(Integer status) {
        return status != null && (status == STATUS_PENDING
                || status == STATUS_IN_PROGRESS || status == STATUS_COMPLETED);
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_PENDING -> "待开发";
            case STATUS_IN_PROGRESS -> "进行中";
            case STATUS_COMPLETED -> "已完成";
            default -> String.valueOf(status);
        };
    }

    /** 状态中文 / 编码 → 编码，认不出时返回 null */
    public static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "待开发", "0" -> STATUS_PENDING;
            case "进行中", "1" -> STATUS_IN_PROGRESS;
            case "已完成", "2" -> STATUS_COMPLETED;
            default -> null;
        };
    }

    // ---------- 分组 ----------

    public static final String CATEGORY_PHYSICIAN = "医师管理";
    public static final String CATEGORY_QUALITY = "质量管理";
    public static final String CATEGORY_ORG = "组织与制度";
    public static final String CATEGORY_RESEARCH = "科研与统计";
    public static final String CATEGORY_SYSTEM = "系统与移动";

    public static final List<String> CATEGORIES = List.of(
            CATEGORY_PHYSICIAN, CATEGORY_ORG, CATEGORY_QUALITY,
            CATEGORY_RESEARCH, CATEGORY_SYSTEM);

    public static boolean isValidCategory(String category) {
        return category != null && CATEGORIES.contains(category);
    }

    /** 分组中文 → 编码（分组本身就是中文，非法时返回 null） */
    public static String parseCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return CATEGORIES.contains(t) ? t : null;
    }

    // ---------- 完成率 ----------

    /** 完成率 = 已完成 / 总数 × 100，保留两位小数；总数为 0 时返回 0.00 */
    public static BigDecimal completionRate(long completed, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(completed * 100)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /** 平均进度 = 进度之和 / 模块数，保留两位小数；无模块时返回 0.00 */
    public static BigDecimal avgProgress(long progressSum, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(progressSum)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
