package com.yunzhongyi.medical.committee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 委员会换届请求：新一届的任期与人员（为空的字段沿用上一届）
 */
@Data
public class CommitteeRenewRequest {

    /** 新一届主任委员 */
    private String chairperson;

    /** 新一届副主任委员 */
    private String viceChairperson;

    /** 新一届秘书 */
    private String secretary;

    /** 新一届委员人数 */
    private Integer memberCount;

    /** 新一届任期开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate termStartDate;

    /** 新一届任期结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate termEndDate;
}
