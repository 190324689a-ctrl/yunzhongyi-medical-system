package com.yunzhongyi.medical.sysconfig.dto;

import lombok.Data;

/** 系统参数多条件组合查询参数 */
@Data
public class SysParamQuery {
    /** 关键字：参数名称 / 参数键模糊匹配 */
    private String keyword;
    /** 参数分组：SYSTEM / BUSINESS / NOTIFY */
    private String paramGroup;
    /** 值类型：STRING / NUMBER / BOOLEAN */
    private String valueType;
    /** 状态：1 启用，0 停用 */
    private Integer status;
    private long page = 1;
    private long size = 10;
}
