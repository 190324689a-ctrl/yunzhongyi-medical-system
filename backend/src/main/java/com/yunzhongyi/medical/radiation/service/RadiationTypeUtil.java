package com.yunzhongyi.medical.radiation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 放射人员管理编码工具：放射类别 / 在岗状态 / 证书类型 / 培训类型 /
 * 体检类型 / 审核状态对照，以及个人受照剂量限值。
 * <p>
 * 放射类别 workType：DIAGNOSIS 诊断 / INTERVENTION 介入 /
 * NUCLEAR_MED 核医学 / RADIOTHERAPY 放疗。
 * <p>
 * 在岗状态 status：1 在岗，2 停岗，3 离岗。
 * <p>
 * 证书类型 certType：WORKER_CERT 放射工作人员证 / EQUIPMENT 大型设备上岗证 /
 * PROTECTION 放射防护培训合格证 / PRACTICE 执业医师证。
 * 资质审核状态：0 待审核，1 已通过，2 已驳回（一级审核：医务科）。
 * <p>
 * 培训类型 trainingType：PRE_JOB 岗前培训 / RETRAINING 复训 /
 * PROTECTION 防护知识培训 / EQUIPMENT 新设备培训。
 * 培训结果：PASS 合格 / FAIL 不合格。
 * <p>
 * 体检类型 examType：PRE_JOB 岗前体检 / ON_JOB 在岗期间体检 / OFF_JOB 离岗体检。
 * 体检结论：FIT 合格 / REVIEW 复查 / UNFIT 不宜从事放射工作。
 * <p>
 * 个人受照剂量限值：国家职业照射年有效剂量限值 20 mSv，
 * dose &gt; 20 mSv 即超标。
 */
public final class RadiationTypeUtil {

    private RadiationTypeUtil() {
    }

    // ---------- 放射类别 ----------
    public static final String WORK_DIAGNOSIS = "DIAGNOSIS";
    public static final String WORK_INTERVENTION = "INTERVENTION";
    public static final String WORK_NUCLEAR_MED = "NUCLEAR_MED";
    public static final String WORK_RADIOTHERAPY = "RADIOTHERAPY";

    // ---------- 在岗状态 ----------
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_SUSPENDED = 2;
    public static final int STATUS_LEFT = 3;

    // ---------- 证书类型 ----------
    public static final String CERT_WORKER_CERT = "WORKER_CERT";
    public static final String CERT_EQUIPMENT = "EQUIPMENT";
    public static final String CERT_PROTECTION = "PROTECTION";
    public static final String CERT_PRACTICE = "PRACTICE";

    // ---------- 资质审核状态：0 待审核，1 已通过，2 已驳回 ----------
    public static final int APPROVE_PENDING = 0;
    public static final int APPROVE_APPROVED = 1;
    public static final int APPROVE_REJECTED = 2;

    // ---------- 培训类型 ----------
    public static final String TRAINING_PRE_JOB = "PRE_JOB";
    public static final String TRAINING_RETRAINING = "RETRAINING";
    public static final String TRAINING_PROTECTION = "PROTECTION";
    public static final String TRAINING_EQUIPMENT = "EQUIPMENT";

    // ---------- 培训结果 ----------
    public static final String TRAINING_PASS = "PASS";
    public static final String TRAINING_FAIL = "FAIL";

    // ---------- 体检类型 ----------
    public static final String EXAM_PRE_JOB = "PRE_JOB";
    public static final String EXAM_ON_JOB = "ON_JOB";
    public static final String EXAM_OFF_JOB = "OFF_JOB";

    // ---------- 体检结论 ----------
    public static final String EXAM_FIT = "FIT";
    public static final String EXAM_REVIEW = "REVIEW";
    public static final String EXAM_UNFIT = "UNFIT";

    /** 个人受照年有效剂量限值（mSv）：国家职业照射限值 20 mSv/年 */
    public static final BigDecimal DOSE_LIMIT_MSV = new BigDecimal("20");

    /** 资质到期提醒窗口：90 天内到期即提醒 */
    public static final int EXPIRY_WARN_DAYS = 90;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 放射类别编码 → 中文 */
    public static String workTypeText(String workType) {
        if (workType == null) {
            return "";
        }
        return switch (workType) {
            case WORK_DIAGNOSIS -> "放射诊断";
            case WORK_INTERVENTION -> "介入放射";
            case WORK_NUCLEAR_MED -> "核医学";
            case WORK_RADIOTHERAPY -> "放射治疗";
            default -> workType;
        };
    }

    /** 放射类别中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseWorkType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "放射诊断", "诊断", "DIAGNOSIS" -> WORK_DIAGNOSIS;
            case "介入放射", "介入", "INTERVENTION" -> WORK_INTERVENTION;
            case "核医学", "NUCLEAR_MED" -> WORK_NUCLEAR_MED;
            case "放射治疗", "放疗", "RADIOTHERAPY" -> WORK_RADIOTHERAPY;
            default -> null;
        };
    }

    public static boolean isValidWorkType(String workType) {
        return WORK_DIAGNOSIS.equals(workType) || WORK_INTERVENTION.equals(workType)
                || WORK_NUCLEAR_MED.equals(workType) || WORK_RADIOTHERAPY.equals(workType);
    }

    /** 在岗状态 → 中文 */
    public static String statusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case STATUS_ACTIVE -> "在岗";
            case STATUS_SUSPENDED -> "停岗";
            case STATUS_LEFT -> "离岗";
            default -> String.valueOf(status);
        };
    }

    /** 在岗状态中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static Integer parseStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "在岗" -> STATUS_ACTIVE;
            case "停岗" -> STATUS_SUSPENDED;
            case "离岗" -> STATUS_LEFT;
            default -> null;
        };
    }

    /** 证书类型编码 → 中文 */
    public static String certTypeText(String certType) {
        if (certType == null) {
            return "";
        }
        return switch (certType) {
            case CERT_WORKER_CERT -> "放射工作人员证";
            case CERT_EQUIPMENT -> "大型设备上岗证";
            case CERT_PROTECTION -> "放射防护培训合格证";
            case CERT_PRACTICE -> "执业医师证";
            default -> certType;
        };
    }

    /** 证书类型中文 → 编码（认不出时返回 null） */
    public static String parseCertType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "放射工作人员证", "WORKER_CERT" -> CERT_WORKER_CERT;
            case "大型设备上岗证", "EQUIPMENT" -> CERT_EQUIPMENT;
            case "放射防护培训合格证", "PROTECTION" -> CERT_PROTECTION;
            case "执业医师证", "PRACTICE" -> CERT_PRACTICE;
            default -> null;
        };
    }

    public static boolean isValidCertType(String certType) {
        return CERT_WORKER_CERT.equals(certType) || CERT_EQUIPMENT.equals(certType)
                || CERT_PROTECTION.equals(certType) || CERT_PRACTICE.equals(certType);
    }

    /** 资质审核状态 → 中文 */
    public static String approveStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case APPROVE_PENDING -> "待审核";
            case APPROVE_APPROVED -> "已通过";
            case APPROVE_REJECTED -> "已驳回";
            default -> String.valueOf(status);
        };
    }

    /** 资质审核状态中文 → 编码（认不出时返回 null） */
    public static Integer parseApproveStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "待审核" -> APPROVE_PENDING;
            case "已通过", "通过" -> APPROVE_APPROVED;
            case "已驳回", "驳回" -> APPROVE_REJECTED;
            default -> null;
        };
    }

    /** 培训类型编码 → 中文 */
    public static String trainingTypeText(String trainingType) {
        if (trainingType == null) {
            return "";
        }
        return switch (trainingType) {
            case TRAINING_PRE_JOB -> "岗前培训";
            case TRAINING_RETRAINING -> "复训";
            case TRAINING_PROTECTION -> "防护知识培训";
            case TRAINING_EQUIPMENT -> "新设备培训";
            default -> trainingType;
        };
    }

    /** 培训类型中文 → 编码（认不出时返回 null） */
    public static String parseTrainingType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "岗前培训", "PRE_JOB" -> TRAINING_PRE_JOB;
            case "复训", "RETRAINING" -> TRAINING_RETRAINING;
            case "防护知识培训", "PROTECTION" -> TRAINING_PROTECTION;
            case "新设备培训", "EQUIPMENT" -> TRAINING_EQUIPMENT;
            default -> null;
        };
    }

    public static boolean isValidTrainingType(String trainingType) {
        return TRAINING_PRE_JOB.equals(trainingType) || TRAINING_RETRAINING.equals(trainingType)
                || TRAINING_PROTECTION.equals(trainingType) || TRAINING_EQUIPMENT.equals(trainingType);
    }

    /** 培训结果 → 中文 */
    public static String trainingResultText(String result) {
        if (result == null) {
            return "";
        }
        return switch (result) {
            case TRAINING_PASS -> "合格";
            case TRAINING_FAIL -> "不合格";
            default -> result;
        };
    }

    /** 培训结果中文 → 编码（认不出时返回 null） */
    public static String parseTrainingResult(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "合格", "PASS" -> TRAINING_PASS;
            case "不合格", "FAIL" -> TRAINING_FAIL;
            default -> null;
        };
    }

    public static boolean isValidTrainingResult(String result) {
        return TRAINING_PASS.equals(result) || TRAINING_FAIL.equals(result);
    }

    /** 体检类型编码 → 中文 */
    public static String examTypeText(String examType) {
        if (examType == null) {
            return "";
        }
        return switch (examType) {
            case EXAM_PRE_JOB -> "岗前体检";
            case EXAM_ON_JOB -> "在岗期间体检";
            case EXAM_OFF_JOB -> "离岗体检";
            default -> examType;
        };
    }

    /** 体检类型中文 → 编码（认不出时返回 null） */
    public static String parseExamType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "岗前体检", "PRE_JOB" -> EXAM_PRE_JOB;
            case "在岗期间体检", "ON_JOB" -> EXAM_ON_JOB;
            case "离岗体检", "OFF_JOB" -> EXAM_OFF_JOB;
            default -> null;
        };
    }

    public static boolean isValidExamType(String examType) {
        return EXAM_PRE_JOB.equals(examType) || EXAM_ON_JOB.equals(examType)
                || EXAM_OFF_JOB.equals(examType);
    }

    /** 体检结论 → 中文 */
    public static String examResultText(String result) {
        if (result == null) {
            return "";
        }
        return switch (result) {
            case EXAM_FIT -> "合格";
            case EXAM_REVIEW -> "复查";
            case EXAM_UNFIT -> "不宜从事放射工作";
            default -> result;
        };
    }

    /** 体检结论中文 → 编码（认不出时返回 null） */
    public static String parseExamResult(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "合格", "FIT" -> EXAM_FIT;
            case "复查", "REVIEW" -> EXAM_REVIEW;
            case "不宜从事放射工作", "UNFIT" -> EXAM_UNFIT;
            default -> null;
        };
    }

    public static boolean isValidExamResult(String result) {
        return EXAM_FIT.equals(result) || EXAM_REVIEW.equals(result) || EXAM_UNFIT.equals(result);
    }

    /** 个人受照剂量是否超标（年有效剂量 > 20 mSv） */
    public static boolean isDoseOverLimit(BigDecimal dose) {
        return dose != null && dose.compareTo(DOSE_LIMIT_MSV) > 0;
    }

    /** 日期格式化（yyyy-MM-dd），为空返回 "" */
    public static String fmtDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }
}
