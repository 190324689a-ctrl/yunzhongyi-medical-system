package com.yunzhongyi.medical.radiation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 放射人员健康检查记录（职业健康监护）
 * <p>
 * 覆盖需求（需求梳理.md 第 8 项）：放射人员健康管理。
 * <p>
 * 体检类型 examType：PRE_JOB 岗前体检 / ON_JOB 在岗期间体检 /
 * OFF_JOB 离岗体检（见 RadiationTypeUtil）。
 * <p>
 * 体检结论 result：FIT 合格 / REVIEW 复查 / UNFIT 不宜从事放射工作。
 * <p>
 * 个人受照剂量 dose：本次体检周期内的个人年有效剂量（mSv）。
 * 国家职业照射年有效剂量限值 20 mSv（见 RadiationTypeUtil.DOSE_LIMIT_MSV）：
 * dose &gt; 20 即超标，统计里单独计数并提醒复查调岗。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("radiation_health")
public class RadiationHealth extends BaseEntity {

    /** 放射人员 id（档案 id） */
    private Long personnelId;

    /** 人员姓名（冗余） */
    @NotBlank(message = "人员姓名不能为空")
    private String personnelName;

    /** 体检类型：PRE_JOB 岗前体检 / ON_JOB 在岗期间体检 / OFF_JOB 离岗体检 */
    @NotBlank(message = "体检类型不能为空")
    private String examType;

    /** 体检日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate examDate;

    /** 体检机构 */
    private String examOrg;

    /** 个人年受照剂量（mSv） */
    private BigDecimal dose;

    /** 体检结论：FIT 合格 / REVIEW 复查 / UNFIT 不宜从事放射工作 */
    private String result;

    /** 体检结论说明 */
    private String conclusion;

    /** 下次体检日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextExamDate;

    /** 备注 */
    private String remark;
}
