package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 医德医风表彰多条件组合查询参数
 */
@Data
public class EthicsCommendationQuery {

    /** 关键字：受表彰医师姓名 / 表彰标题模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 表彰类型：BANNER 锦旗 / LETTER 表扬信 / NOTICE 通报表扬 / HONOR 荣誉称号 / OTHER 其他 */
    private String commendType;

    /** 表彰级别：HOSPITAL 院级 / CITY 市级 / PROVINCE 省级 / NATIONAL 国家级 */
    private String commendLevel;

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
