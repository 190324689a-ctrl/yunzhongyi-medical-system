package com.yunzhongyi.medical.physician.dto;

import lombok.Data;

/**
 * 医师档案多条件组合查询参数
 */
@Data
public class PhysicianQuery {

    /** 关键字：姓名 / 执业证号 / 手机号 模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 职称 */
    private String title;

    /** 最高学历 */
    private String education;

    /** 人员类别 */
    private String staffType;

    /** 手术分级（0-4） */
    private Integer surgeryLevel;

    /** 手术分级授权：0 未授权，1 已授权 */
    private Integer surgeryAuth;

    /** 抗肿瘤处方权 */
    private Integer antitumorRx;

    /** 麻醉药品处方权 */
    private Integer anesthesiaRx;

    /** 精神药品处方权 */
    private Integer psychotropicRx;

    /** 在职状态 */
    private Integer status;

    private long page = 1;
    private long size = 10;
}
