package com.yunzhongyi.medical.techgrading.dto;

import lombok.Data;

/**
 * 医疗技术准入申请多条件组合查询参数
 */
@Data
public class TechAdmissionQuery {

    /** 关键字：技术名称 / 技术编号 / 申请人模糊匹配 */
    private String keyword;

    /** 医疗技术档案 id */
    private Long techId;

    /** 申请类型：NEW 新技术准入 / ADD_DEPT 新增开展科室 / CHANGE_LEVEL 变更分级 / RENEW 延续准入 */
    private String applyType;

    /** 申请科室关键字（模糊匹配） */
    private String applyDept;

    /** 申请状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
