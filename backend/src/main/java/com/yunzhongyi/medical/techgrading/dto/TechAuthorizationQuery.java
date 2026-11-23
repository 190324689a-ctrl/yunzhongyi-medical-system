package com.yunzhongyi.medical.techgrading.dto;

import lombok.Data;

/**
 * 医师医疗技术授权多条件组合查询参数
 */
@Data
public class TechAuthorizationQuery {

    /** 关键字：医师姓名 / 技术名称 / 技术编号模糊匹配 */
    private String keyword;

    /** 医疗技术档案 id */
    private Long techId;

    /** 医师档案 id */
    private Long physicianId;

    /** 授权级别：1 一级 / 2 二级 / 3 三级 */
    private Integer authLevel;

    /** 授权状态：0 待审批，1 已通过，2 已驳回，3 已注销 */
    private Integer status;

    /** 科室关键字（模糊匹配） */
    private String deptName;

    /** 只看 90 天内到期（已通过、未过期） */
    private Boolean expiringSoon;

    /** 只看已过期（已通过且授权有效期已过当天） */
    private Boolean expired;

    private long page = 1;
    private long size = 10;
}
