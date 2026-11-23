package com.yunzhongyi.medical.mobile.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 医务管理移动端类型与文案常量 */
public final class MobileTypeUtil {

    private MobileTypeUtil() {
    }

    // ---------- 设备平台 ----------
    public static final String PLATFORM_IOS = "IOS";
    public static final String PLATFORM_ANDROID = "ANDROID";
    public static final Set<String> PLATFORMS = Set.of(PLATFORM_IOS, PLATFORM_ANDROID);

    public static String platformText(String platform) {
        if (PLATFORM_IOS.equals(platform)) {
            return "iOS";
        }
        if (PLATFORM_ANDROID.equals(platform)) {
            return "Android";
        }
        return platform == null ? "" : platform;
    }

    // ---------- 设备状态 ----------
    public static final int DEVICE_BOUND = 1;
    public static final int DEVICE_UNBOUND = 0;

    public static String deviceStatusText(Integer status) {
        if (Integer.valueOf(DEVICE_BOUND).equals(status)) {
            return "已绑定";
        }
        if (Integer.valueOf(DEVICE_UNBOUND).equals(status)) {
            return "已解绑";
        }
        return "";
    }

    // ---------- 业务模块 ----------
    public static final String MODULE_LEAVE = "LEAVE";
    public static final String MODULE_AUTH = "AUTH";
    public static final String MODULE_SCHEDULE = "SCHEDULE";
    public static final String MODULE_MDT = "MDT";
    public static final String MODULE_NEWTECH = "NEWTECH";
    public static final String MODULE_PERSONNEL = "PERSONNEL";
    public static final String MODULE_MEDGRADING = "MEDGRADING";
    public static final String MODULE_BRIEFING = "BRIEFING";
    public static final List<String> MODULE_TYPES = List.of(
            MODULE_LEAVE, MODULE_AUTH, MODULE_SCHEDULE, MODULE_MDT,
            MODULE_NEWTECH, MODULE_PERSONNEL, MODULE_MEDGRADING, MODULE_BRIEFING);

    private static final Map<String, String> MODULE_TEXT = new LinkedHashMap<>();

    static {
        MODULE_TEXT.put(MODULE_LEAVE, "休假执业");
        MODULE_TEXT.put(MODULE_AUTH, "重点授权");
        MODULE_TEXT.put(MODULE_SCHEDULE, "排班调班");
        MODULE_TEXT.put(MODULE_MDT, "多学科会诊");
        MODULE_TEXT.put(MODULE_NEWTECH, "新技术新项目");
        MODULE_TEXT.put(MODULE_PERSONNEL, "医疗人员动态");
        MODULE_TEXT.put(MODULE_MEDGRADING, "医疗分级");
        MODULE_TEXT.put(MODULE_BRIEFING, "医疗工作简报");
    }

    public static String moduleText(String moduleType) {
        return MODULE_TEXT.getOrDefault(moduleType, moduleType == null ? "" : moduleType);
    }

    // ---------- 审批操作 / 结果 ----------
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final Set<String> ACTIONS = Set.of(ACTION_APPROVE, ACTION_REJECT);

    public static String actionText(String action) {
        if (ACTION_APPROVE.equals(action)) {
            return "通过";
        }
        if (ACTION_REJECT.equals(action)) {
            return "驳回";
        }
        return action == null ? "" : action;
    }

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAIL = "FAIL";

    public static String resultText(String result) {
        if (RESULT_SUCCESS.equals(result)) {
            return "成功";
        }
        if (RESULT_FAIL.equals(result)) {
            return "失败";
        }
        return result == null ? "" : result;
    }

    // ---------- 休假申请类型 ----------
    public static String leaveApplyTypeText(String applyType) {
        return switch (applyType == null ? "" : applyType) {
            case "LEAVE" -> "休假";
            case "PRACTICE" -> "执业";
            case "TRAINING" -> "进修";
            case "OUT_TRAINING" -> "外出进修";
            case "SURGERY_AUTH" -> "手术分级授权";
            default -> applyType;
        };
    }

    // ---------- 重点授权类型 ----------
    public static String authTypeText(String authType) {
        return switch (authType == null ? "" : authType) {
            case "SURGERY" -> "手术分级授权";
            case "ANESTHESIA" -> "麻醉药品处方权";
            case "ANTITUMOR" -> "抗肿瘤处方权";
            case "PSYCHOTROPIC" -> "精神药品处方权";
            default -> authType;
        };
    }

    // ---------- 两级审批状态文案（休假执业 / 重点授权共用） ----------
    public static String twoLevelStatusText(Integer status) {
        if (Integer.valueOf(0).equals(status)) {
            return "待科主任审批";
        }
        if (Integer.valueOf(1).equals(status)) {
            return "待医务科审批";
        }
        if (Integer.valueOf(2).equals(status)) {
            return "已通过";
        }
        if (Integer.valueOf(3).equals(status)) {
            return "已驳回";
        }
        return "";
    }
}
