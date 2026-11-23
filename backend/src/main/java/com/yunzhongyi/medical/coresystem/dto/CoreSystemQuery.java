package com.yunzhongyi.medical.coresystem.dto;

import lombok.Data;

/**
 * 核心制度多条件组合查询参数
 */
@Data
public class CoreSystemQuery {

    /** 关键字：制度名称 / 制度编号 / 制定部门模糊匹配 */
    private String keyword;

    /** 类别：MEDICAL 医疗 / NURSING 护理 / PHARMACY 药事 / MANAGEMENT 管理 / SAFETY 安全 / OTHER 其他 */
    private String category;

    /** 适用层级：HOSPITAL 院级 / DEPT 科室级 */
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
