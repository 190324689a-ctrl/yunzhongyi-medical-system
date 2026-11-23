package com.yunzhongyi.medical.sysconfig.service;

import java.util.List;
import java.util.Set;

/**
 * 医务系统设置模块常量与纯函数（第 28 项）。
 */
public final class SysConfigTypeUtil {

    private SysConfigTypeUtil() {
    }

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    // ---------- 系统参数 ----------
    public static final String GROUP_SYSTEM = "SYSTEM";
    public static final String GROUP_BUSINESS = "BUSINESS";
    public static final String GROUP_NOTIFY = "NOTIFY";
    public static final Set<String> PARAM_GROUPS = Set.of(GROUP_SYSTEM, GROUP_BUSINESS, GROUP_NOTIFY);

    public static final String VALUE_STRING = "STRING";
    public static final String VALUE_NUMBER = "NUMBER";
    public static final String VALUE_BOOLEAN = "BOOLEAN";
    public static final Set<String> VALUE_TYPES = Set.of(VALUE_STRING, VALUE_NUMBER, VALUE_BOOLEAN);

    // ---------- 数据备份 ----------
    public static final String BACKUP_FULL = "FULL";
    public static final String BACKUP_INCREMENTAL = "INCREMENTAL";
    public static final String BACKUP_CONFIG = "CONFIG";
    public static final Set<String> BACKUP_TYPES = Set.of(BACKUP_FULL, BACKUP_INCREMENTAL, BACKUP_CONFIG);

    public static final int BACKUP_RUNNING = 0;
    public static final int BACKUP_SUCCESS = 1;
    public static final int BACKUP_FAILED = 2;

    // ---------- 系统接口 ----------
    public static final String IF_HIS = "HIS";
    public static final String IF_LIS = "LIS";
    public static final String IF_PACS = "PACS";
    public static final String IF_INSURANCE = "INSURANCE";
    public static final String IF_OTHER = "OTHER";
    public static final Set<String> INTERFACE_TYPES = Set.of(IF_HIS, IF_LIS, IF_PACS, IF_INSURANCE, IF_OTHER);

    public static final String AUTH_NONE = "NONE";
    public static final String AUTH_TOKEN = "TOKEN";
    public static final String AUTH_BASIC = "BASIC";
    public static final Set<String> AUTH_TYPES = Set.of(AUTH_NONE, AUTH_TOKEN, AUTH_BASIC);

    public static final String TEST_SUCCESS = "SUCCESS";
    public static final String TEST_FAIL = "FAIL";
    public static final String TEST_UNTESTED = "UNTESTED";

    // ---------- 消息提醒设置 ----------
    public static final String CHANNEL_SITE = "SITE";
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_WECHAT = "WECHAT";
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final Set<String> CHANNELS = Set.of(CHANNEL_SITE, CHANNEL_SMS, CHANNEL_WECHAT, CHANNEL_EMAIL);

    public static final String RECEIVER_ALL = "ALL";
    public static final String RECEIVER_ROLE = "ROLE";
    public static final String RECEIVER_USER = "USER";
    public static final Set<String> RECEIVER_SCOPES = Set.of(RECEIVER_ALL, RECEIVER_ROLE, RECEIVER_USER);

    // ---------- 知识库 ----------
    public static final Set<String> KNOWLEDGE_CATEGORIES = Set.of("制度规范", "操作指南", "常见问题", "法律法规", "培训资料", "其他");

    // ---------- 文本 ----------
    public static String paramGroupText(String group) {
        if (group == null) {
            return "";
        }
        return switch (group) {
            case GROUP_SYSTEM -> "系统参数";
            case GROUP_BUSINESS -> "业务参数";
            case GROUP_NOTIFY -> "消息提醒参数";
            default -> group;
        };
    }

    public static String parseParamGroup(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "系统参数", GROUP_SYSTEM -> GROUP_SYSTEM;
            case "业务参数", GROUP_BUSINESS -> GROUP_BUSINESS;
            case "消息提醒参数", GROUP_NOTIFY -> GROUP_NOTIFY;
            default -> text.trim();
        };
    }

    public static String valueTypeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case VALUE_STRING -> "文本";
            case VALUE_NUMBER -> "数字";
            case VALUE_BOOLEAN -> "开关";
            default -> type;
        };
    }

    public static String parseValueType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "文本", VALUE_STRING -> VALUE_STRING;
            case "数字", VALUE_NUMBER -> VALUE_NUMBER;
            case "开关", VALUE_BOOLEAN -> VALUE_BOOLEAN;
            default -> text.trim();
        };
    }

    public static String backupTypeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case BACKUP_FULL -> "全量备份";
            case BACKUP_INCREMENTAL -> "增量备份";
            case BACKUP_CONFIG -> "配置备份";
            default -> type;
        };
    }

    public static String parseBackupType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "全量备份", BACKUP_FULL -> BACKUP_FULL;
            case "增量备份", BACKUP_INCREMENTAL -> BACKUP_INCREMENTAL;
            case "配置备份", BACKUP_CONFIG -> BACKUP_CONFIG;
            default -> text.trim();
        };
    }

    public static String backupStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case BACKUP_RUNNING -> "进行中";
            case BACKUP_SUCCESS -> "成功";
            case BACKUP_FAILED -> "失败";
            default -> "未知";
        };
    }

    public static Integer parseBackupStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "进行中", "0" -> BACKUP_RUNNING;
            case "成功", "1" -> BACKUP_SUCCESS;
            case "失败", "2" -> BACKUP_FAILED;
            default -> null;
        };
    }

    public static String interfaceTypeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case IF_HIS -> "HIS";
            case IF_LIS -> "LIS";
            case IF_PACS -> "PACS";
            case IF_INSURANCE -> "医保";
            case IF_OTHER -> "其他";
            default -> type;
        };
    }

    public static String parseInterfaceType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case IF_HIS -> IF_HIS;
            case IF_LIS -> IF_LIS;
            case IF_PACS -> IF_PACS;
            case "医保", IF_INSURANCE -> IF_INSURANCE;
            case "其他", IF_OTHER -> IF_OTHER;
            default -> text.trim();
        };
    }

    public static String authTypeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case AUTH_NONE -> "无鉴权";
            case AUTH_TOKEN -> "令牌";
            case AUTH_BASIC -> "账号密码";
            default -> type;
        };
    }

    public static String parseAuthType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "无鉴权", AUTH_NONE -> AUTH_NONE;
            case "令牌", AUTH_TOKEN -> AUTH_TOKEN;
            case "账号密码", AUTH_BASIC -> AUTH_BASIC;
            default -> text.trim();
        };
    }

    public static String testResultText(String result) {
        if (result == null) {
            return "";
        }
        return switch (result) {
            case TEST_SUCCESS -> "成功";
            case TEST_FAIL -> "失败";
            case TEST_UNTESTED -> "未测试";
            default -> result;
        };
    }

    public static String parseTestResult(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "成功", TEST_SUCCESS -> TEST_SUCCESS;
            case "失败", TEST_FAIL -> TEST_FAIL;
            case "未测试", TEST_UNTESTED -> TEST_UNTESTED;
            default -> text.trim();
        };
    }

    public static String channelText(String channel) {
        if (channel == null) {
            return "";
        }
        return switch (channel) {
            case CHANNEL_SITE -> "站内消息";
            case CHANNEL_SMS -> "短信";
            case CHANNEL_WECHAT -> "微信";
            case CHANNEL_EMAIL -> "邮件";
            default -> channel;
        };
    }

    public static String parseChannel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "站内消息", CHANNEL_SITE -> CHANNEL_SITE;
            case "短信", CHANNEL_SMS -> CHANNEL_SMS;
            case "微信", CHANNEL_WECHAT -> CHANNEL_WECHAT;
            case "邮件", CHANNEL_EMAIL -> CHANNEL_EMAIL;
            default -> text.trim();
        };
    }

    public static String receiverScopeText(String scope) {
        if (scope == null) {
            return "";
        }
        return switch (scope) {
            case RECEIVER_ALL -> "全部人员";
            case RECEIVER_ROLE -> "按角色";
            case RECEIVER_USER -> "按人员";
            default -> scope;
        };
    }

    public static String parseReceiverScope(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "全部人员", RECEIVER_ALL -> RECEIVER_ALL;
            case "按角色", RECEIVER_ROLE -> RECEIVER_ROLE;
            case "按人员", RECEIVER_USER -> RECEIVER_USER;
            default -> text.trim();
        };
    }

    /** 校验参数值是否符合值类型：NUMBER 须可解析为数字，BOOLEAN 只能是 true/false */
    public static boolean valueMatchesType(String value, String valueType) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (VALUE_NUMBER.equals(valueType)) {
            try {
                new java.math.BigDecimal(value.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (VALUE_BOOLEAN.equals(valueType)) {
            String v = value.trim().toLowerCase();
            return "true".equals(v) || "false".equals(v);
        }
        return true;
    }

    /** 备份文件大小估算：每条记录约 1KB（仅作展示，不代表真实文件） */
    public static long estimateFileSize(long recordCount) {
        return recordCount * 1024L;
    }
}
