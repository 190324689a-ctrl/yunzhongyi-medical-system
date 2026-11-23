package com.yunzhongyi.medical.radiation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 放射人员培训记录
 * <p>
 * 覆盖需求（需求梳理.md 第 8 项）：放射人员培训管理。
 * <p>
 * 培训类型 trainingType：PRE_JOB 岗前培训 / RETRAINING 复训 /
 * PROTECTION 防护知识培训 / EQUIPMENT 新设备培训（见 RadiationTypeUtil）。
 * <p>
 * 培训结果 result：PASS 合格 / FAIL 不合格。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("radiation_training")
public class RadiationTraining extends BaseEntity {

    /** 放射人员 id（档案 id） */
    private Long personnelId;

    /** 人员姓名（冗余） */
    @NotBlank(message = "人员姓名不能为空")
    private String personnelName;

    /** 培训名称 */
    @NotBlank(message = "培训名称不能为空")
    private String trainingName;

    /** 培训类型：PRE_JOB 岗前培训 / RETRAINING 复训 / PROTECTION 防护知识培训 / EQUIPMENT 新设备培训 */
    @NotBlank(message = "培训类型不能为空")
    private String trainingType;

    /** 培训机构 */
    private String org;

    /** 培训日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate trainingDate;

    /** 培训学时 */
    private Integer hours;

    /** 培训结果：PASS 合格 / FAIL 不合格 */
    private String result;

    /** 结业证书编号 */
    private String certificateNo;

    /** 备注 */
    private String remark;
}
