package com.yunzhongyi.medical.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统权限（菜单 / 按钮）
 * type: 1 目录，2 菜单，3 按钮
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    /** 父级 id，0 为顶级 */
    private Long parentId;
    /** 名称 */
    private String name;
    /** 权限标识，如 physician:list */
    private String code;
    /** 类型：1 目录，2 菜单，3 按钮 */
    private Integer type;
    /** 前端路由路径 */
    private String path;
    /** 前端组件路径 */
    private String component;
    /** 排序 */
    private Integer sortOrder;
    /** 状态：1 启用，0 停用 */
    private Integer status;
}
