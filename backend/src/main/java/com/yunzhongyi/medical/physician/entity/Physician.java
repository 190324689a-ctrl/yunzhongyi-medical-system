package com.yunzhongyi.medical.physician.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医师档案
 * <p>
 * 覆盖需求：基本信息（执业、学历、职称、聘期、证书、证件照、进修、人员类别、最高学历）、
 * 手术分级授权、抗肿瘤处方权、麻醉药品处方权、精神药品处方权。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("physician")
public class Physician extends BaseEntity {

    /** 姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 性别：M 男，F 女 */
    private String gender;

    /** 身份证号 */
    @Pattern(regexp = "(^$)|(^[0-9]{17}[0-9Xx]$)", message = "身份证号格式不正确")
    private String idCard;

    /** 手机号 */
    @Pattern(regexp = "(^$)|(^1[3-9]\\d{9}$)", message = "手机号格式不正确")
    private String phone;

    /** 所属科室 id */
    private Long deptId;

    /** 科室名称（冗余，便于列表展示与导出） */
    private String deptName;

    /** 职称：主任医师 / 副主任医师 / 主治医师 / 住院医师 */
    private String title;

    /** 最高学历：博士 / 硕士 / 本科 / 大专 */
    private String education;

    /** 毕业院校 */
    private String graduateSchool;

    /** 专业 */
    private String major;

    /** 执业证号 */
    private String practiceNo;

    /** 执业范围 */
    private String practiceScope;

    /** 执业类别：临床 / 中医 / 口腔 / 公共卫生 */
    private String practiceCategory;

    /** 人员类别：编制 / 合同 / 返聘 / 进修 / 规培 */
    private String staffType;

    /** 聘期开始 */
    private LocalDate hireStartDate;

    /** 聘期结束 */
    private LocalDate hireEndDate;

    /** 证书信息（多个证书以分号分隔，如：执业医师证；主治医师证） */
    private String certificates;

    /** 证件照 URL */
    private String photoUrl;

    /** 进修经历 */
    private String training;

    /** 手术分级：1 一级，2 二级，3 三级，4 四级；0 未授权 */
    private Integer surgeryLevel;

    /** 手术分级授权状态：0 未授权，1 已授权 */
    private Integer surgeryAuth;

    /** 抗肿瘤处方权：0 无，1 有 */
    private Integer antitumorRx;

    /** 麻醉药品处方权：0 无，1 有 */
    private Integer anesthesiaRx;

    /** 精神药品处方权：0 无，1 有 */
    private Integer psychotropicRx;

    /** 在职状态：1 在职，2 休假，3 进修，4 离职，5 退休 */
    private Integer status;

    /** 备注 */
    private String remark;
}
