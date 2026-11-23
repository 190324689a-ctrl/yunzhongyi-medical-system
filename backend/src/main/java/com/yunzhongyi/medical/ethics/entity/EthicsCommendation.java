package com.yunzhongyi.medical.ethics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医德医风表彰
 * <p>
 * 覆盖需求（需求梳理.md 第 12 项）：医德医风表彰。
 * <p>
 * 表彰类型 commendType：BANNER 锦旗 / LETTER 表扬信 /
 * NOTICE 通报表扬 / HONOR 荣誉称号 / OTHER 其他。
 * <p>
 * 表彰级别 commendLevel：HOSPITAL 院级 / CITY 市级 /
 * PROVINCE 省级 / NATIONAL 国家级。
 * <p>
 * 审批 status：0 待审批，1 已通过，2 已驳回；一级审批（医务科）。
 * 已通过的表彰是正式记录，不能直接删除；已结束的表彰不能再审批、再修改。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ethics_commendation")
public class EthicsCommendation extends BaseEntity {

    /** 受表彰医师 id */
    @NotNull(message = "受表彰医师不能为空")
    private Long physicianId;

    /** 受表彰医师姓名（冗余，便于列表展示与查询） */
    @NotBlank(message = "受表彰医师姓名不能为空")
    private String physicianName;

    /** 所属科室 id */
    private Long deptId;

    /** 科室名称（冗余） */
    private String deptName;

    /** 表彰类型：BANNER 锦旗 / LETTER 表扬信 / NOTICE 通报表扬 / HONOR 荣誉称号 / OTHER 其他 */
    @NotBlank(message = "表彰类型不能为空")
    private String commendType;

    /** 表彰级别：HOSPITAL 院级 / CITY 市级 / PROVINCE 省级 / NATIONAL 国家级 */
    @NotBlank(message = "表彰级别不能为空")
    private String commendLevel;

    /** 表彰标题，如“妙手仁心 德艺双馨” */
    @NotBlank(message = "表彰标题不能为空")
    private String title;

    /** 表彰事迹内容 */
    private String content;

    /** 授予方，如“患者张某家属”“医院党委” */
    private String grantedBy;

    /** 表彰日期（默认当天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate commendDate;

    /** 审批状态：0 待审批，1 已通过，2 已驳回 */
    private Integer status;

    /** 审批人 */
    private String approver;

    /** 审批记录（通过/驳回留痕） */
    private String approvalHistory;

    /** 备注 */
    private String remark;
}
