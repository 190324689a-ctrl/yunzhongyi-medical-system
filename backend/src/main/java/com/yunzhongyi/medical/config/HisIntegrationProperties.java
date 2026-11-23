package com.yunzhongyi.medical.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HIS 对接配置（预留占位，未真实对接）。
 * <p>
 * 需求（需求梳理.md 第 5 项）要求排班“支持与 HIS 对接”，但目前没有真实 HIS 系统
 * 可连：本配置默认关闭（his.enabled=false），base-url 为空；排班同步接口在未配置时
 * 明确返回“未配置”而不伪造同步成功。待医院提供真实 HIS 接口地址与鉴权方式后，
 * 再按其规范实现同步调用。
 */
@Data
@Component
@ConfigurationProperties(prefix = "his")
public class HisIntegrationProperties {

    /** 是否启用 HIS 对接（默认 false，未真实对接） */
    private boolean enabled = false;

    /** HIS 接口地址（未配置） */
    private String baseUrl = "";

    /** HIS 接口应用标识（未配置） */
    private String appKey = "";

    /** 同步超时时间（毫秒） */
    private int timeoutMs = 5000;
}
