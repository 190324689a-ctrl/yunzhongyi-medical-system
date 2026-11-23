package com.yunzhongyi.medical.sysconfig.dto;

import lombok.Data;

/** 系统接口多条件组合查询参数 */
@Data
public class SysInterfaceQuery {
    /** 关键字：接口名称 / 接口编码模糊匹配 */
    private String keyword;
    /** 接口类型：HIS / LIS / PACS / INSURANCE / OTHER */
    private String type;
    /** 状态：1 启用，0 停用 */
    private Integer status;
    /** 最近一次测试结果：SUCCESS / FAIL / UNTESTED */
    private String lastTestResult;
    private long page = 1;
    private long size = 10;
}
