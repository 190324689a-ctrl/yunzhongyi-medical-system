package com.yunzhongyi.medical.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 科室
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /** 父级 id，0 为顶级 */
    private Long parentId;
    /** 科室名称 */
    private String name;
    /** 科室编码 */
    private String code;
    /** 科室类型：clinical 临床 / medical-technical 医技 / admin 行政 / outpatient 门诊 */
    private String type;
    /** 科主任 */
    private String director;
    /** 联系电话 */
    private String phone;
    /** 排序 */
    private Integer sortOrder;
    /** 状态：1 启用，0 停用 */
    private Integer status;
}
