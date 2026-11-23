package com.yunzhongyi.medical.sysconfig.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数设置：键值对形式的全局配置。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：参数设置，支持自定义。
 * <p>
 * 参数分组 paramGroup：SYSTEM 系统参数 / BUSINESS 业务参数 / NOTIFY 消息提醒参数。
 * 值类型 valueType：STRING 文本 / NUMBER 数字 / BOOLEAN 开关；NUMBER 类型的值须能解析为数字，
 * BOOLEAN 类型的值只能是 true/false。
 * 状态 status：1 启用，0 停用；只有启用的参数才会被业务读取。
 * 内置参数 isSystem=1：参数键不可改、不可删除，只能改值（防止误删系统关键配置）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_param")
public class SysParam extends BaseEntity {

    /** 参数键（唯一），如 sys.name / his.enabled */
    @NotBlank(message = "参数键不能为空")
    private String paramKey;

    /** 参数名称，如 系统名称 */
    @NotBlank(message = "参数名称不能为空")
    private String paramName;

    /** 参数值 */
    private String paramValue;

    /** 参数分组：SYSTEM / BUSINESS / NOTIFY */
    private String paramGroup;

    /** 值类型：STRING / NUMBER / BOOLEAN */
    private String valueType;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 是否内置参数：1 内置（键不可改、不可删），0 自定义 */
    private Integer isSystem;

    /** 备注 */
    private String remark;
}
