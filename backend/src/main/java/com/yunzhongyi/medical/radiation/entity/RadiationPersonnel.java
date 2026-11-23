package com.yunzhongyi.medical.radiation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 放射人员档案
 * <p>
 * 覆盖需求（需求梳理.md 第 8 项）：放射人员档案管理。
 * <p>
 * 放射类别 workType：DIAGNOSIS 放射诊断 / INTERVENTION 介入放射 /
 * NUCLEAR_MED 核医学 / RADIOTHERAPY 放射治疗（见 RadiationTypeUtil）。
 * <p>
 * 在岗状态 status：1 在岗，2 停岗，3 离岗。档案与医师档案通过
 * physicianId 关联（放射科人员多已建档；外聘/新入职人员可为空，
 * 建档后在档案里补填）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("radiation_personnel")
public class RadiationPersonnel extends BaseEntity {

    /** 医师档案 id（已建档时关联；未建档可为空） */
    private Long physicianId;

    /** 姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 性别：M 男 / F 女 */
    private String gender;

    /** 身份证号 */
    private String idCard;

    /** 联系电话 */
    private String phone;

    /** 科室 id（放射科为主，也可为开展介入诊疗的临床科室） */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 职称 */
    private String title;

    /** 放射类别：DIAGNOSIS 诊断 / INTERVENTION 介入 / NUCLEAR_MED 核医学 / RADIOTHERAPY 放疗 */
    @NotBlank(message = "放射类别不能为空")
    private String workType;

    /** 放射工作人员证号 */
    private String licenseNo;

    /** 放射诊疗许可证号 */
    private String permitNo;

    /** 放射诊疗许可证有效期至 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate permitExpiry;

    /** 从事放射工作日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;

    /** 在岗状态：1 在岗，2 停岗，3 离岗 */
    private Integer status;

    /** 备注 */
    private String remark;
}
