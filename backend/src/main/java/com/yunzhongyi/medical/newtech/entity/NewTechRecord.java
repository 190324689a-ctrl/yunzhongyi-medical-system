package com.yunzhongyi.medical.newtech.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 新技术、新项目进展记录：进展 / 验收 / 推广记录，可多次补充。
 * <p>
 * 记录类型 recordType：PROGRESS 进展记录 / ACCEPTANCE 验收记录 /
 * PROMOTION 推广记录。添加验收记录后项目自动变为已验收，添加推广记录后
 * 项目自动变为已推广（见 NewTechServiceImpl.addRecord）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("newtech_record")
public class NewTechRecord extends BaseEntity {

    /** 所属新技术、新项目 id（服务端从路径参数补值，不由客户端传） */
    private Long projectId;

    /** 记录类型：PROGRESS 进展 / ACCEPTANCE 验收 / PROMOTION 推广 */
    @NotBlank(message = "记录类型不能为空")
    private String recordType;

    /** 记录日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    /** 记录内容（进展情况 / 验收情况 / 推广情况） */
    @NotBlank(message = "记录内容不能为空")
    private String content;

    /** 结论（验收结论 / 推广效果等） */
    private String conclusion;

    /** 记录人 */
    private String recorder;

    /** 备注 */
    private String remark;
}
