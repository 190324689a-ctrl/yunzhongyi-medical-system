package com.yunzhongyi.medical.policy.dto;

import lombok.Data;

/**
 * 政策法规多条件组合查询参数
 */
@Data
public class PolicyQuery {

    /** 关键字：法规名称 / 文号 / 发布机关模糊匹配 */
    private String keyword;

    /** 分类：LAW 法律 / REGULATION 行政法规 / RULE 部门规章 / LOCAL 地方性法规 / NORMATIVE 规范性文件 / STANDARD 行业标准 / OTHER 其他 */
    private String category;

    /** 发布层级：NATIONAL 国家 / PROVINCIAL 省 / MUNICIPAL 市 / HOSPITAL 院内 */
    private String level;

    /** 状态：0 草稿，1 已发布，2 已废止 */
    private Integer status;

    /** 适用科室关键字（模糊匹配，如“中医内科”） */
    private String applicableDept;

    /** 只看 30 天内到期（已发布、未过期） */
    private Boolean expiringSoon;

    /** 只看已过期（已发布且失效日期已过当天） */
    private Boolean expired;

    private long page = 1;
    private long size = 10;
}
