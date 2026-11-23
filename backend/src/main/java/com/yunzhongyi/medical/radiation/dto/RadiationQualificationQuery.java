package com.yunzhongyi.medical.radiation.dto;

import lombok.Data;

/**
 * 放射人员资质多条件组合查询参数
 */
@Data
public class RadiationQualificationQuery {

    /** 关键字：人员姓名 / 证书名称 / 证书编号模糊匹配 */
    private String keyword;

    /** 放射人员 id（档案 id） */
    private Long personnelId;

    /** 证书类型：WORKER_CERT / EQUIPMENT / PROTECTION / PRACTICE */
    private String certType;

    /** 审核状态：0 待审核，1 已通过，2 已驳回 */
    private Integer status;

    /** 只看 90 天内到期（含已过期）：true 时按有效期过滤 */
    private Boolean expiringSoon;

    private long page = 1;
    private long size = 10;
}
