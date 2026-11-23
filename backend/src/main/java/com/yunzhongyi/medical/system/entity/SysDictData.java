package com.yunzhongyi.medical.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据项
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    /** 所属字典类型编码 */
    private String dictCode;
    /** 数据值（存库） */
    private String dictValue;
    /** 显示标签 */
    private String label;
    /** 排序 */
    private Integer sortOrder;
    /** 状态：1 启用，0 停用 */
    private Integer status;
}
