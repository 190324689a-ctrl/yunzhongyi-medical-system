package com.yunzhongyi.medical.radiation.dto;

import lombok.Data;

/**
 * 放射人员培训记录多条件组合查询参数
 */
@Data
public class RadiationTrainingQuery {

    /** 关键字：人员姓名 / 培训名称模糊匹配 */
    private String keyword;

    /** 放射人员 id（档案 id） */
    private Long personnelId;

    /** 培训类型：PRE_JOB 岗前培训 / RETRAINING 复训 / PROTECTION 防护知识培训 / EQUIPMENT 新设备培训 */
    private String trainingType;

    /** 培训结果：PASS 合格 / FAIL 不合格 */
    private String result;

    private long page = 1;
    private long size = 10;
}
