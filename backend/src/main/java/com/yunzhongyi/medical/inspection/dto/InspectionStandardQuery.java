package com.yunzhongyi.medical.inspection.dto;

import lombok.Data;

/**
 * 检查标准多条件组合查询参数
 */
@Data
public class InspectionStandardQuery {

    /** 关键字：标准名称 / 标准编号 / 依据文件模糊匹配 */
    private String keyword;

    /** 标准级别：NATIONAL 国家 / PROVINCE 省 / CITY 市 / HOSPITAL 院内 */
    private String level;

    /** 状态：1 现行，0 已废止 */
    private Integer status;

    /** 只看 30 天内到期（现行、未过期） */
    private Boolean expiringSoon;

    /** 只看已过期（现行且有效期已过当天） */
    private Boolean expired;

    private long page = 1;
    private long size = 10;
}
