package com.yunzhongyi.medical.schedule.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 排班班次工具：班次/排班类型/调班代班状态的中英文对照、班次默认时间段、
 * 排班模板班次组合（shiftPattern）JSON 解析。
 * <p>
 * 班次默认时间段：白班 08:00–17:30 / 上午班 08:00–12:00 /
 * 下午班 14:00–17:30 / 夜班 17:30–次日 08:00（结束时间存 08:00）/ 休息无时间段。
 */
@Slf4j
public final class ScheduleShiftUtil {

    private ScheduleShiftUtil() {
    }

    public static final String SHIFT_DAY = "DAY";
    public static final String SHIFT_MORNING = "MORNING";
    public static final String SHIFT_AFTERNOON = "AFTERNOON";
    public static final String SHIFT_NIGHT = "NIGHT";
    public static final String SHIFT_REST = "REST";

    public static final String TYPE_DEPT = "DEPT";
    public static final String TYPE_OUTPATIENT = "OUTPATIENT";
    public static final String TYPE_INPATIENT = "INPATIENT";
    public static final String TYPE_EMERGENCY = "EMERGENCY";

    public static final String CHANGE_SWAP = "SWAP";
    public static final String CHANGE_SUBSTITUTE = "SUBSTITUTE";

    /** 调班/代班状态：0 无申请，1 待科主任审批，2 已通过，3 已驳回 */
    public static final int CHANGE_NONE = 0;
    public static final int CHANGE_PENDING = 1;
    public static final int CHANGE_APPROVED = 2;
    public static final int CHANGE_REJECTED = 3;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 班次编码 → 中文 */
    public static String shiftText(String shiftCode) {
        if (shiftCode == null) {
            return "";
        }
        return switch (shiftCode) {
            case SHIFT_DAY -> "白班";
            case SHIFT_MORNING -> "上午班";
            case SHIFT_AFTERNOON -> "下午班";
            case SHIFT_NIGHT -> "夜班";
            case SHIFT_REST -> "休息";
            default -> shiftCode;
        };
    }

    /** 班次中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseShift(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "白班", "DAY" -> SHIFT_DAY;
            case "上午班", "上午", "MORNING" -> SHIFT_MORNING;
            case "下午班", "下午", "AFTERNOON" -> SHIFT_AFTERNOON;
            case "夜班", "晚班", "NIGHT" -> SHIFT_NIGHT;
            case "休息", "休", "REST" -> SHIFT_REST;
            default -> null;
        };
    }

    /** 排班类型编码 → 中文 */
    public static String typeText(String scheduleType) {
        if (scheduleType == null) {
            return "";
        }
        return switch (scheduleType) {
            case TYPE_DEPT -> "科室排班";
            case TYPE_OUTPATIENT -> "门诊排班";
            case TYPE_INPATIENT -> "住院排班";
            case TYPE_EMERGENCY -> "急诊排班";
            default -> scheduleType;
        };
    }

    /** 排班类型中文 → 编码（导入 Excel 用，认不出时返回 null） */
    public static String parseType(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        return switch (t) {
            case "科室排班", "科室", "DEPT" -> TYPE_DEPT;
            case "门诊排班", "门诊", "OUTPATIENT" -> TYPE_OUTPATIENT;
            case "住院排班", "住院", "INPATIENT" -> TYPE_INPATIENT;
            case "急诊排班", "急诊", "EMERGENCY" -> TYPE_EMERGENCY;
            default -> null;
        };
    }

    /** 调班/代班类型编码 → 中文 */
    public static String changeTypeText(String changeType) {
        if (changeType == null) {
            return "";
        }
        return switch (changeType) {
            case CHANGE_SWAP -> "调班";
            case CHANGE_SUBSTITUTE -> "代班";
            default -> changeType;
        };
    }

    /** 调班/代班状态 → 中文 */
    public static String changeStatusText(Integer changeStatus) {
        if (changeStatus == null) {
            return "";
        }
        return switch (changeStatus) {
            case CHANGE_NONE -> "无申请";
            case CHANGE_PENDING -> "待科主任审批";
            case CHANGE_APPROVED -> "已通过";
            case CHANGE_REJECTED -> "已驳回";
            default -> String.valueOf(changeStatus);
        };
    }

    /** 班次默认开始时间（休息返回 null） */
    public static LocalTime defaultStartTime(String shiftCode) {
        if (shiftCode == null) {
            return null;
        }
        return switch (shiftCode) {
            case SHIFT_DAY, SHIFT_MORNING -> LocalTime.of(8, 0);
            case SHIFT_AFTERNOON -> LocalTime.of(14, 0);
            case SHIFT_NIGHT -> LocalTime.of(17, 30);
            default -> null;
        };
    }

    /** 班次默认结束时间（休息返回 null；夜班结束时间为次日 08:00，存 08:00） */
    public static LocalTime defaultEndTime(String shiftCode) {
        if (shiftCode == null) {
            return null;
        }
        return switch (shiftCode) {
            case SHIFT_DAY -> LocalTime.of(17, 30);
            case SHIFT_MORNING -> LocalTime.of(12, 0);
            case SHIFT_AFTERNOON -> LocalTime.of(17, 30);
            case SHIFT_NIGHT -> LocalTime.of(8, 0);
            default -> null;
        };
    }

    /** 是否为合法班次编码 */
    public static boolean isValidShift(String shiftCode) {
        return SHIFT_DAY.equals(shiftCode) || SHIFT_MORNING.equals(shiftCode)
                || SHIFT_AFTERNOON.equals(shiftCode) || SHIFT_NIGHT.equals(shiftCode)
                || SHIFT_REST.equals(shiftCode);
    }

    /** 是否为合法排班类型编码 */
    public static boolean isValidType(String scheduleType) {
        return TYPE_DEPT.equals(scheduleType) || TYPE_OUTPATIENT.equals(scheduleType)
                || TYPE_INPATIENT.equals(scheduleType) || TYPE_EMERGENCY.equals(scheduleType);
    }

    /** 解析模板班次组合 JSON：[{"dayOfWeek":1,"shiftCode":"DAY"}, ...]，解析失败返回空列表 */
    public static List<Map<String, Object>> parseShiftPattern(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("排班模板班次组合解析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 查模板在某星期几（1=周一 … 7=周日）的班次；未列出返回 null（当天不排班）。
     */
    public static String shiftForDayOfWeek(String shiftPatternJson, int dayOfWeek) {
        for (Map<String, Object> item : parseShiftPattern(shiftPatternJson)) {
            Object dow = item.get("dayOfWeek");
            if (dow == null) {
                continue;
            }
            try {
                if (Integer.parseInt(String.valueOf(dow)) == dayOfWeek) {
                    return String.valueOf(item.get("shiftCode"));
                }
            } catch (NumberFormatException ignored) {
                // 非数字星期几忽略
            }
        }
        return null;
    }

    /** 班次组合展示文本，如“周一 白班；周二 白班；周六 上午班；周日 休息” */
    public static String shiftPatternText(String json) {
        String[] week = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<String> parts = new ArrayList<>();
        for (Map<String, Object> item : parseShiftPattern(json)) {
            Object dow = item.get("dayOfWeek");
            Object shift = item.get("shiftCode");
            if (dow == null || shift == null) {
                continue;
            }
            try {
                int d = Integer.parseInt(String.valueOf(dow));
                if (d >= 1 && d <= 7) {
                    parts.add(week[d] + " " + shiftText(String.valueOf(shift)));
                }
            } catch (NumberFormatException ignored) {
                // 非数字星期几忽略
            }
        }
        return String.join("；", parts);
    }
}
