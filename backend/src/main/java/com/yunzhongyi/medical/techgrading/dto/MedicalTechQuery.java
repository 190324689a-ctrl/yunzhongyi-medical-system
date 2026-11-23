package com.yunzhongyi.medical.techgrading.dto;

import lombok.Data;

/**
 * 医疗技术档案多条件组合查询参数
 */
@Data
public class MedicalTechQuery {

    /** 关键字：技术名称 / 技术编号模糊匹配 */
    private String keyword;

    /** 技术分类：SURGICAL 手术类 / NON_SURGICAL 非手术类 / TCM 中医技术 / DIAGNOSTIC 诊疗技术 / OTHER 其他 */
    private String category;

    /** 技术分级：1 一级 / 2 二级 / 3 三级 */
    private Integer techLevel;

    /** 准入类别：NORMAL 常规技术 / RESTRICTED 限制类技术 / PROHIBITED 禁止类技术 */
    private String accessType;

    /** 风险等级：LOW 低 / MEDIUM 中 / HIGH 高 */
    private String riskLevel;

    /** 档案状态：0 待准入，1 已准入，2 已停用 */
    private Integer status;

    /** 开展科室关键字（模糊匹配，如“骨伤科”） */
    private String deptName;

    /** 只看 90 天内到期（已准入、未过期） */
    private Boolean expiringSoon;

    /** 只看已过期（已准入且有效期已过当天） */
    private Boolean expired;

    private long page = 1;
    private long size = 10;
}
