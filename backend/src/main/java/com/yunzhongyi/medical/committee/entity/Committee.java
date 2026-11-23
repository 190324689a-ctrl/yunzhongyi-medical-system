package com.yunzhongyi.medical.committee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 委员会
 * <p>
 * 覆盖需求（需求梳理.md 第 9 项）：各类委员会（医疗质量、药事、院感、输血、
 * 病案等）管理，支持委员会换届、会议记录、决议管理。
 * <p>
 * 委员会类别 type：MEDICAL_QUALITY 医疗质量 / PHARMACY 药事 /
 * INFECTION 院感 / TRANSFUSION 输血 / MEDICAL_RECORD 病案 / OTHER 其他
 * （见 CommitteeTypeUtil）。
 * <p>
 * 状态 status：1 在任，2 已换届，3 已解散。
 * 换届：对在任的委员会换届时生成一条新记录（届次 termNo +1、新任期、
 * 新一届主任委员/副主任委员/秘书），原记录状态改为已换届，历史会议与
 * 决议保留在原记录名下；解散后该委员会不再新增会议。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("committee")
public class Committee extends BaseEntity {

    /** 委员会名称，如“医疗质量管理委员会” */
    @NotBlank(message = "委员会名称不能为空")
    private String name;

    /** 委员会类别：MEDICAL_QUALITY / PHARMACY / INFECTION / TRANSFUSION / MEDICAL_RECORD / OTHER */
    @NotBlank(message = "委员会类别不能为空")
    private String type;

    /** 主任委员 */
    private String chairperson;

    /** 副主任委员 */
    private String viceChairperson;

    /** 秘书 */
    private String secretary;

    /** 委员人数 */
    private Integer memberCount;

    /** 届次（第几届） */
    private Integer termNo;

    /** 任期开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate termStartDate;

    /** 任期结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate termEndDate;

    /** 状态：1 在任，2 已换届，3 已解散 */
    private Integer status;

    /** 备注 */
    private String remark;
}
