package com.yunzhongyi.medical.schedule.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 调班 / 代班申请请求
 * <p>
 * 调班（SWAP）：填 targetDate + targetShiftCode（目标班次时间段可不填，按班次默认时间补全）。
 * 代班（SUBSTITUTE）：填 targetPhysicianId（目标医师姓名由服务端按医师档案补全）。
 */
@Data
public class ScheduleChangeRequest {

    /** 调班目标日期 */
    private LocalDate targetDate;

    /** 调班目标班次：DAY / MORNING / AFTERNOON / NIGHT / REST */
    private String targetShiftCode;

    /** 调班目标班次开始时间（可不填，按班次默认时间补全） */
    private LocalTime targetStartTime;

    /** 调班目标班次结束时间（可不填，按班次默认时间补全） */
    private LocalTime targetEndTime;

    /** 代班目标医师 id */
    private Long targetPhysicianId;

    /** 调班/代班事由 */
    private String changeReason;
}
