package com.yunzhongyi.medical.radiation.dto;

import lombok.Data;

/**
 * 放射人员健康检查记录多条件组合查询参数
 */
@Data
public class RadiationHealthQuery {

    /** 关键字：人员姓名模糊匹配 */
    private String keyword;

    /** 放射人员 id（档案 id） */
    private Long personnelId;

    /** 体检类型：PRE_JOB 岗前体检 / ON_JOB 在岗期间体检 / OFF_JOB 离岗体检 */
    private String examType;

    /** 体检结论：FIT 合格 / REVIEW 复查 / UNFIT 不宜从事放射工作 */
    private String result;

    /** 只看剂量超标（年有效剂量 > 20 mSv）：true 时过滤 */
    private Boolean overLimit;

    private long page = 1;
    private long size = 10;
}
