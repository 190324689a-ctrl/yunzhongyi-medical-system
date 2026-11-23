package com.yunzhongyi.medical.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 登录账号 */
    private String username;
    /** 密码（PBKDF2 哈希存储；种子里的演示账号仍为明文占位，首次登录成功后自动迁移为哈希） */
    private String password;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 所属科室 id */
    private Long deptId;
    /** 状态：1 启用，0 停用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
