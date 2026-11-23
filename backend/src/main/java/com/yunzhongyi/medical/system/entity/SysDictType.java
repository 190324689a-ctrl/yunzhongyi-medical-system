package com.yunzhongyi.medical.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    /** 字典编码，如 title（职称）、education（学历） */
    private String code;
    /** 字典名称 */
    private String name;
    /** 状态：1 启用，0 停用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
