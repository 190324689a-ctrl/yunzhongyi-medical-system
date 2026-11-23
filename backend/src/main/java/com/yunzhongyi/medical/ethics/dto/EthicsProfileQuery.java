package com.yunzhongyi.medical.ethics.dto;

import lombok.Data;

/**
 * 医德医风档案多条件组合查询参数
 */
@Data
public class EthicsProfileQuery {

    /** 关键字：医师姓名 / 档案编号模糊匹配 */
    private String keyword;

    /** 科室 id */
    private Long deptId;

    /** 状态：0 在档，1 封存 */
    private Integer status;

    /** 最近考评等级：EXCELLENT 优秀 / QUALIFIED 合格 / BASIC 基本合格 / UNQUALIFIED 不合格 */
    private String lastGrade;

    private long page = 1;
    private long size = 10;
}
