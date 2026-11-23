package com.yunzhongyi.medical.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志（审计）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    /** 操作人 */
    private String username;
    /** 模块，如 physician */
    private String module;
    /** 操作，如 create / update / delete / export / import */
    private String operation;
    /** 请求方法/接口 */
    private String method;
    /** 请求参数摘要 */
    private String params;
    /** 结果：1 成功，0 失败 */
    private Integer result;
    /** 耗时毫秒 */
    private Long costMs;
    /** 错误信息 */
    private String errorMsg;
}
