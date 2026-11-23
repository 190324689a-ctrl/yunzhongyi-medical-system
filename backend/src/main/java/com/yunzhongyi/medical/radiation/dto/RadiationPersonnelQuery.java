package com.yunzhongyi.medical.radiation.dto;

import lombok.Data;

/**
 * 放射人员档案多条件组合查询参数
 */
@Data
public class RadiationPersonnelQuery {

    /** 关键字：姓名 / 放射工作人员证号模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 放射类别：DIAGNOSIS 诊断 / INTERVENTION 介入 / NUCLEAR_MED 核医学 / RADIOTHERAPY 放疗 */
    private String workType;

    /** 在岗状态：1 在岗，2 停岗，3 离岗 */
    private Integer status;

    /** 人员 id（医师档案 id） */
    private Long physicianId;

    private long page = 1;
    private long size = 10;
}
