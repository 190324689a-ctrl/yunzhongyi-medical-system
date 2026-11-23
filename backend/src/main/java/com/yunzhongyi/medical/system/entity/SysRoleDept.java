package com.yunzhongyi.medical.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-科室关联（登录鉴权增强：角色可绑定一个或多个科室，表示该角色的
 * 医院科室归属 / 数据范围；登录后可见页面仍由角色-权限决定）。
 */
@Data
@TableName("sys_role_dept")
public class SysRoleDept implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色 id */
    private Long roleId;

    /** 科室 id */
    private Long deptId;
}
