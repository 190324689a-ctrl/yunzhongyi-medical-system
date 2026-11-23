package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 医德医风投诉多条件组合查询参数
 */
@Data
public class EthicsComplaintQuery {

    /** 关键字：投诉人 / 被投诉医师 / 投诉编号模糊匹配 */
    private String keyword;

    /** 投诉类型：ATTITUDE 服务态度 / QUALITY 医疗质量 / CHARGE 收费问题 / INTEGRITY 廉洁行医 / OTHER 其他 */
    private String complaintType;

    /** 流转状态：0 待受理，1 调查中，2 已处理，3 已反馈，4 不予受理 */
    private Integer status;

    /** 只看逾期（未到已反馈、未不予受理，且处理期限已过当天） */
    private Boolean overdue;

    private long page = 1;
    private long size = 10;
}
