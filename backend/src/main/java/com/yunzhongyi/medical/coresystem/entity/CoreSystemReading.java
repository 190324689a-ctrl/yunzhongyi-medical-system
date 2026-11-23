package com.yunzhongyi.medical.coresystem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 核心制度查阅与学习记录
 * <p>
 * 一条记录 = 一名人员对一项核心制度的一次查阅/学习登记。
 * learned：0 已查阅（仅阅读过正文），1 已学习（完成学习并确认）。
 * <p>
 * 只有已发布的制度才能登记查阅学习；同一人同一制度同一天只登记一次，
 * 重复登记拒绝（见 CoreSystemReadingServiceImpl）。
 * coreSystemTitle 为制度名称冗余字段，查阅列表展示用，避免每次联表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("core_system_reading")
public class CoreSystemReading extends BaseEntity {

    /** 制度 id */
    private Long coreSystemId;

    /** 制度名称（冗余，展示用） */
    private String coreSystemTitle;

    /** 查阅人姓名 */
    @NotBlank(message = "查阅人姓名不能为空")
    private String readerName;

    /** 查阅人科室 */
    private String deptName;

    /** 查阅日期（默认当天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate readDate;

    /** 是否已学习：0 已查阅，1 已学习 */
    private Integer learned;

    /** 备注 */
    private String remark;
}
