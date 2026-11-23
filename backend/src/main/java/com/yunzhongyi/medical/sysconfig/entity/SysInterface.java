package com.yunzhongyi.medical.sysconfig.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统接口配置：与 HIS / LIS / PACS / 医保等外部系统的对接配置。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：系统接口。
 * <p>
 * 接口类型 type：HIS / LIS / PACS / INSURANCE 医保 / OTHER 其他。
 * 鉴权方式 authType：NONE 无鉴权 / TOKEN 令牌 / BASIC 账号密码。
 * 状态 status：1 启用，0 停用；只有启用的接口才允许连通性测试。
 * 连通性测试 testConnection：真实发起 HTTP 请求（超时按 timeoutSeconds 计），
 * 不伪造成功；测试结果与时间回写 lastTestResult / lastTestTime / lastTestMsg。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_interface")
public class SysInterface extends BaseEntity {

    /** 接口编码（唯一），如 HIS-001 */
    @NotBlank(message = "接口编码不能为空")
    private String interfaceCode;

    /** 接口名称，如 HIS 挂号接口 */
    @NotBlank(message = "接口名称不能为空")
    private String interfaceName;

    /** 接口类型：HIS / LIS / PACS / INSURANCE / OTHER */
    private String type;

    /** 接口地址（须为 http/https 开头） */
    @NotBlank(message = "接口地址不能为空")
    private String url;

    /** 鉴权方式：NONE / TOKEN / BASIC */
    private String authType;

    /** 鉴权凭证（Token 或账号密码占位；列表不回显） */
    private String authToken;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 超时时间（秒，默认 5，1-60） */
    private Integer timeoutSeconds;

    /** 最近一次测试时间 */
    private LocalDateTime lastTestTime;

    /** 最近一次测试结果：SUCCESS 成功 / FAIL 失败 / UNTESTED 未测试 */
    private String lastTestResult;

    /** 最近一次测试说明（如 HTTP 200 / 连接超时） */
    private String lastTestMsg;

    /** 备注 */
    private String remark;
}
