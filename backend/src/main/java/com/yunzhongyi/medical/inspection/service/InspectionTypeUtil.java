package com.yunzhongyi.medical.inspection.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 检查维护编码工具：项目分类 / 检查方式 / 适用检查类型 /
 * 标准级别 / 项目状态 / 标准状态对照、过期判定、模板 JSON 组装。
 * <p>
 * 项目分类自模块 20 起由检查分类库（inspection_category）维护，
 * 10 个既有分类编码仍保留在这里作兜底校验（见 InspectionItemServiceImpl#isUsableCategory），
 * 分类的新增/改名/启停走 InspectionCategoryService。
 * <p>
 * 项目分类 category：MEDICAL_RECORD 病历质量 / CORE_SYSTEM 核心制度 /
 * INFECTION 医院感染 / PHARMACY 药事管理 / NURSING 护理质量 /
 * SAFETY 医疗安全 / SERVICE 服务质量 / TCM 中医特色 /
 * MGMT 科室管理 / OTHER 其他。
 * <p>
 * 检查方式 checkMethod：ON_SITE 现场查看 / RECORD_REVIEW 抽查病历 /
 * INTERVIEW 访谈 / DOC_REVIEW 资料查阅。
 * <p>
 * 适用检查类型 applicableType：ALL 通用 / SELF 科室自查 /
 * HOSPITAL 院级检查 / SPECIAL 专项检查（与 QualityTypeUtil 的检查类型一致）。
 * <p>
 * 标准级别 level：NATIONAL 国家 / PROVINCE 省 / CITY 市 / HOSPITAL 院内。
 * <p>
 * 项目状态 status：1 启用，0 停用。
 * <p>
 * 标准状态 status：1 现行，0 已废止。
 */
public final class InspectionTypeUtil {

    private InspectionTypeUtil() {
    }

    // ---------- 项目分类 ----------
    public static final String CATEGORY_MEDICAL_RECORD = "MEDICAL_RECORD";
    public static final String CATEGORY_CORE_SYSTEM = "CORE_SYSTEM";
    public static final String CATEGORY_INFECTION = "INFECTION";
    public static final String CATEGORY_PHARMACY = "PHARMACY";
    public static final String CATEGORY_NURSING = "NURSING";
    public static final String CATEGORY_SAFETY = "SAFETY";
    public static final String CATEGORY_SERVICE = "SERVICE";
    public static final String CATEGORY_TCM = "TCM";
    public static final String CATEGORY_MGMT = "MGMT";
    public static final String CATEGORY_OTHER = "OTHER";

    // ---------- 检查方式 ----------
    public static final String METHOD_ON_SITE = "ON_SITE";
    public static final String METHOD_RECORD_REVIEW = "RECORD_REVIEW";
    public static final String METHOD_INTERVIEW = "INTERVIEW";
    public static final String METHOD_DOC_REVIEW = "DOC_REVIEW";

    // ---------- 适用检查类型 ----------
    public static final String APPLY_ALL = "ALL";
    public static final String APPLY_SELF = "SELF";
    public static final String APPLY_HOSPITAL = "HOSPITAL";
    public static final String APPLY_SPECIAL = "SPECIAL";

    // ---------- 标准级别 ----------
    public static final String LEVEL_NATIONAL = "NATIONAL";
    public static final String LEVEL_PROVINCE = "PROVINCE";
    public static final String LEVEL_CITY = "CITY";
    public static final String LEVEL_HOSPITAL = "HOSPITAL";

    // ---------- 项目状态 ----------
    public static final int ITEM_STATUS_ENABLED = 1;
    public static final int ITEM_STATUS_DISABLED = 0;

    // ---------- 标准状态 ----------
    public static final int STANDARD_STATUS_CURRENT = 1;
    public static final int STANDARD_STATUS_ABOLISHED = 0;

    /** 即将到期提醒窗口：30 天内到期即提醒（与文件制度、政策法规、核心制度一致） */
    public static final int EXPIRY_WARN_DAYS = 30;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 项目分类编码 → 中文 */
    public static String categoryText(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case CATEGORY_MEDICAL_RECORD -> "病历质量";
            case CATEGORY_CORE_SYSTEM -> "核心制度";
            case CATEGORY_INFECTION -> "医院感染";
            case CATEGORY_PHARMACY -> "药事管理";
            case CATEGORY_NURSING -> "护理质量";
            case CATEGORY_SAFETY -> "医疗安全";
            case CATEGORY_SERVICE -> "服务质量";
            case CATEGORY_TCM -> "中医特色";
            case CATEGORY_MGMT -> "科室管理";
            case CATEGORY_OTHER -> "其他";
            default -> category;
        };
    }

    /** 项目分类中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "病历质量", "MEDICAL_RECORD" -> CATEGORY_MEDICAL_RECORD;
            case "核心制度", "CORE_SYSTEM" -> CATEGORY_CORE_SYSTEM;
            case "医院感染", "INFECTION" -> CATEGORY_INFECTION;
            case "药事管理", "PHARMACY" -> CATEGORY_PHARMACY;
            case "护理质量", "NURSING" -> CATEGORY_NURSING;
            case "医疗安全", "SAFETY" -> CATEGORY_SAFETY;
            case "服务质量", "SERVICE" -> CATEGORY_SERVICE;
            case "中医特色", "TCM" -> CATEGORY_TCM;
            case "科室管理", "MGMT" -> CATEGORY_MGMT;
            case "其他", "OTHER" -> CATEGORY_OTHER;
            default -> null;
        };
    }

    public static boolean isValidCategory(String category) {
        return CATEGORY_MEDICAL_RECORD.equals(category) || CATEGORY_CORE_SYSTEM.equals(category)
                || CATEGORY_INFECTION.equals(category) || CATEGORY_PHARMACY.equals(category)
                || CATEGORY_NURSING.equals(category) || CATEGORY_SAFETY.equals(category)
                || CATEGORY_SERVICE.equals(category) || CATEGORY_TCM.equals(category)
                || CATEGORY_MGMT.equals(category) || CATEGORY_OTHER.equals(category);
    }

    /** 检查方式编码 → 中文 */
    public static String checkMethodText(String method) {
        if (method == null) {
            return "";
        }
        return switch (method) {
            case METHOD_ON_SITE -> "现场查看";
            case METHOD_RECORD_REVIEW -> "抽查病历";
            case METHOD_INTERVIEW -> "访谈";
            case METHOD_DOC_REVIEW -> "资料查阅";
            default -> method;
        };
    }

    /** 检查方式中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseCheckMethod(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "现场查看", "ON_SITE" -> METHOD_ON_SITE;
            case "抽查病历", "RECORD_REVIEW" -> METHOD_RECORD_REVIEW;
            case "访谈", "INTERVIEW" -> METHOD_INTERVIEW;
            case "资料查阅", "DOC_REVIEW" -> METHOD_DOC_REVIEW;
            default -> null;
        };
    }

    public static boolean isValidCheckMethod(String method) {
        return method == null || METHOD_ON_SITE.equals(method) || METHOD_RECORD_REVIEW.equals(method)
                || METHOD_INTERVIEW.equals(method) || METHOD_DOC_REVIEW.equals(method);
    }

    /** 适用检查类型编码 → 中文 */
    public static String applicableTypeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case APPLY_ALL -> "通用";
            case APPLY_SELF -> "科室自查";
            case APPLY_HOSPITAL -> "院级检查";
            case APPLY_SPECIAL -> "专项检查";
            default -> type;
        };
    }

    /** 适用检查类型中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseApplicableType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "通用", "ALL" -> APPLY_ALL;
            case "科室自查", "SELF" -> APPLY_SELF;
            case "院级检查", "HOSPITAL" -> APPLY_HOSPITAL;
            case "专项检查", "SPECIAL" -> APPLY_SPECIAL;
            default -> null;
        };
    }

    public static boolean isValidApplicableType(String type) {
        return type == null || APPLY_ALL.equals(type) || APPLY_SELF.equals(type)
                || APPLY_HOSPITAL.equals(type) || APPLY_SPECIAL.equals(type);
    }

    /** 标准级别编码 → 中文 */
    public static String levelText(String level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case LEVEL_NATIONAL -> "国家";
            case LEVEL_PROVINCE -> "省";
            case LEVEL_CITY -> "市";
            case LEVEL_HOSPITAL -> "院内";
            default -> level;
        };
    }

    /** 标准级别中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseLevel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "国家", "NATIONAL" -> LEVEL_NATIONAL;
            case "省", "PROVINCE" -> LEVEL_PROVINCE;
            case "市", "CITY" -> LEVEL_CITY;
            case "院内", "HOSPITAL" -> LEVEL_HOSPITAL;
            default -> null;
        };
    }

    public static boolean isValidLevel(String level) {
        return LEVEL_NATIONAL.equals(level) || LEVEL_PROVINCE.equals(level)
                || LEVEL_CITY.equals(level) || LEVEL_HOSPITAL.equals(level);
    }

    /** 项目状态 → 中文 */
    public static String itemStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case ITEM_STATUS_ENABLED -> "启用";
            case ITEM_STATUS_DISABLED -> "停用";
            default -> String.valueOf(status);
        };
    }

    /** 项目状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseItemStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "启用" -> ITEM_STATUS_ENABLED;
            case "停用" -> ITEM_STATUS_DISABLED;
            default -> null;
        };
    }

    /** 标准状态 → 中文 */
    public static String standardStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STANDARD_STATUS_CURRENT -> "现行";
            case STANDARD_STATUS_ABOLISHED -> "已废止";
            default -> String.valueOf(status);
        };
    }

    /** 标准状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseStandardStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "现行" -> STANDARD_STATUS_CURRENT;
            case "已废止" -> STANDARD_STATUS_ABOLISHED;
            default -> null;
        };
    }

    /** 是否已过期（现行且有效期已过当天） */
    public static boolean isExpired(Integer status, LocalDate expiryDate) {
        if (expiryDate == null || status == null) {
            return false;
        }
        return Integer.valueOf(STANDARD_STATUS_CURRENT).equals(status)
                && expiryDate.isBefore(LocalDate.now());
    }

    /** 是否即将到期（现行、未过期、有效期在 30 天内） */
    public static boolean isExpiringSoon(Integer status, LocalDate expiryDate) {
        if (expiryDate == null || status == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return Integer.valueOf(STANDARD_STATUS_CURRENT).equals(status)
                && !expiryDate.isBefore(today)
                && !expiryDate.isAfter(today.plusDays(EXPIRY_WARN_DAYS));
    }

    /** 日期格式化（yyyy-MM-dd），为空返回 "" */
    public static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }
}
