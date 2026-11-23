package com.yunzhongyi.medical.notification.service;

import java.util.Set;

/**
 * 消息提醒模块常量与纯函数（第 29 项）。
 * <p>
 * 与第 28 项的提醒“设置”分工：28 号（sysconfig.SysNoticeSetting）只管提醒规则的
 * 配置（开/关、渠道、模板、接收范围）；本模块管提醒“中心/实际消息”：站内消息收发
 * 与已读、待办提醒、消息推送记录、短信提醒记录。推送目标支持按角色、按人员、
 * 全部人员三种接收范围，与 28 号的接收范围口径一致。
 * <p>
 * 短信网关与推送通道本机没有真实服务：发送调用为预留占位，明确返回失败/未启用、
 * 如实记录，绝不伪造发送成功。
 */
public final class NotificationTypeUtil {

    private NotificationTypeUtil() {
    }

    // ---------- 接收范围（与 28 号 SysConfigTypeUtil 口径一致） ----------
    public static final String RECEIVER_ALL = "ALL";
    public static final String RECEIVER_ROLE = "ROLE";
    public static final String RECEIVER_USER = "USER";
    public static final Set<String> RECEIVER_SCOPES = Set.of(RECEIVER_ALL, RECEIVER_ROLE, RECEIVER_USER);

    // ---------- 站内消息分类 ----------
    public static final String MSG_NOTICE = "NOTICE";
    public static final String MSG_ANNOUNCE = "ANNOUNCE";
    public static final String MSG_TODO = "TODO";
    public static final String MSG_SYSTEM = "SYSTEM";
    public static final Set<String> MESSAGE_CATEGORIES =
            Set.of(MSG_NOTICE, MSG_ANNOUNCE, MSG_TODO, MSG_SYSTEM);

    /** 已读状态：0 未读，1 已读 */
    public static final int READ_UNREAD = 0;
    public static final int READ_READ = 1;

    // ---------- 待办提醒类型 ----------
    public static final String TODO_APPROVAL = "APPROVAL";
    public static final String TODO_EXPIRE = "EXPIRE";
    public static final String TODO_RECTIFY = "RECTIFY";
    public static final String TODO_OTHER = "OTHER";
    public static final Set<String> TODO_TYPES =
            Set.of(TODO_APPROVAL, TODO_EXPIRE, TODO_RECTIFY, TODO_OTHER);

    /** 待办状态：0 待办，1 已完成，2 已逾期 */
    public static final int TODO_PENDING = 0;
    public static final int TODO_DONE = 1;
    public static final int TODO_OVERDUE = 2;

    // ---------- 待办来源 ----------
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_AUTO = "AUTO";
    public static final String SOURCE_SETTING = "SETTING";
    public static final Set<String> TODO_SOURCES =
            Set.of(SOURCE_MANUAL, SOURCE_AUTO, SOURCE_SETTING);

    // ---------- 消息推送渠道（站内消息与短信各有自己的表，不走这里） ----------
    public static final String PUSH_WECHAT = "WECHAT";
    public static final String PUSH_APP = "APP";
    public static final String PUSH_EMAIL = "EMAIL";
    public static final Set<String> PUSH_CHANNELS = Set.of(PUSH_WECHAT, PUSH_APP, PUSH_EMAIL);

    /** 发送状态：0 待发送，1 已发送，2 发送失败 */
    public static final int SEND_PENDING = 0;
    public static final int SEND_SENT = 1;
    public static final int SEND_FAILED = 2;

    /** 手机号校验：11 位大陆手机号 */
    public static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches(PHONE_PATTERN);
    }

    // ---------- 文本 ----------

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

    public static String messageCategoryText(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case MSG_NOTICE -> "通知";
            case MSG_ANNOUNCE -> "公告";
            case MSG_TODO -> "待办提醒";
            case MSG_SYSTEM -> "系统消息";
            default -> category;
        };
    }

    public static String parseMessageCategory(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "通知", MSG_NOTICE -> MSG_NOTICE;
            case "公告", MSG_ANNOUNCE -> MSG_ANNOUNCE;
            case "待办提醒", MSG_TODO -> MSG_TODO;
            case "系统消息", MSG_SYSTEM -> MSG_SYSTEM;
            default -> text.trim();
        };
    }

    public static String readStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case READ_UNREAD -> "未读";
            case READ_READ -> "已读";
            default -> "未知";
        };
    }

    public static Integer parseReadStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "未读", "0" -> READ_UNREAD;
            case "已读", "1" -> READ_READ;
            default -> null;
        };
    }

    public static String todoTypeText(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case TODO_APPROVAL -> "审批待办";
            case TODO_EXPIRE -> "到期提醒";
            case TODO_RECTIFY -> "整改待办";
            case TODO_OTHER -> "其他待办";
            default -> type;
        };
    }

    public static String parseTodoType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "审批待办", TODO_APPROVAL -> TODO_APPROVAL;
            case "到期提醒", TODO_EXPIRE -> TODO_EXPIRE;
            case "整改待办", TODO_RECTIFY -> TODO_RECTIFY;
            case "其他待办", TODO_OTHER -> TODO_OTHER;
            default -> text.trim();
        };
    }

    public static String todoStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case TODO_PENDING -> "待办";
            case TODO_DONE -> "已完成";
            case TODO_OVERDUE -> "已逾期";
            default -> "未知";
        };
    }

    public static Integer parseTodoStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "待办", "0" -> TODO_PENDING;
            case "已完成", "1" -> TODO_DONE;
            case "已逾期", "2" -> TODO_OVERDUE;
            default -> null;
        };
    }

    public static String todoSourceText(String source) {
        if (source == null) {
            return "";
        }
        return switch (source) {
            case SOURCE_MANUAL -> "手工创建";
            case SOURCE_AUTO -> "系统生成";
            case SOURCE_SETTING -> "按提醒设置生成";
            default -> source;
        };
    }

    public static String parseTodoSource(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "手工创建", SOURCE_MANUAL -> SOURCE_MANUAL;
            case "系统生成", SOURCE_AUTO -> SOURCE_AUTO;
            case "按提醒设置生成", SOURCE_SETTING -> SOURCE_SETTING;
            default -> text.trim();
        };
    }

    public static String pushChannelText(String channel) {
        if (channel == null) {
            return "";
        }
        return switch (channel) {
            case PUSH_WECHAT -> "微信";
            case PUSH_APP -> "移动端";
            case PUSH_EMAIL -> "邮件";
            default -> channel;
        };
    }

    public static String parsePushChannel(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "微信", PUSH_WECHAT -> PUSH_WECHAT;
            case "移动端", PUSH_APP -> PUSH_APP;
            case "邮件", PUSH_EMAIL -> PUSH_EMAIL;
            default -> text.trim();
        };
    }

    public static String sendStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case SEND_PENDING -> "待发送";
            case SEND_SENT -> "已发送";
            case SEND_FAILED -> "发送失败";
            default -> "未知";
        };
    }

    public static Integer parseSendStatus(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return switch (text.trim()) {
            case "待发送", "0" -> SEND_PENDING;
            case "已发送", "1" -> SEND_SENT;
            case "发送失败", "2" -> SEND_FAILED;
            default -> null;
        };
    }
}
