package com.yunzhongyi.medical.sysconfig.dto;

import lombok.Data;

/** 数据备份多条件组合查询参数 */
@Data
public class SysBackupQuery {
    /** 关键字：备份名称模糊匹配 */
    private String keyword;
    /** 备份类型：FULL / INCREMENTAL / CONFIG */
    private String backupType;
    /** 备份状态：0 进行中，1 成功，2 失败 */
    private Integer status;
    /** 只看已恢复过的备份 */
    private Boolean restoredOnly;
    private long page = 1;
    private long size = 10;
}
