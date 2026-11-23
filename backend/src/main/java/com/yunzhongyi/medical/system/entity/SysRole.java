package com.yunzhongyi.medical.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色编码，如 ADMIN / DOCTOR / NURSE */
    private String code;
    /** 角色名称 */
    private String name;
    /** 状态：1 启用，0 停用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
