package com.yunzhongyi.medical.statistics.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.statistics.service.StatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 统计汇总接口：第 24/25/26 项的聚合口径
 * <p>
 * 所有数字都从库内已有业务数据现算，不另造业务数据；排班人次与诊疗量是两个
 * 口径，分开返回、不混算。
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /** 三组统计一次取全：工作统计 + 三级医院指标统计 + 医疗质量统计 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statisticsService.overview());
    }

    /** 工作统计（24）：工作量登记汇总 + 排班人次（另一口径，单独列示） */
    @GetMapping("/work")
    public Result<Map<String, Object>> work() {
        return Result.ok(statisticsService.workOverview());
    }

    /** 三级医院医疗质量指标统计（25）：指标表汇总 */
    @GetMapping("/tertiary")
    public Result<Map<String, Object>> tertiary() {
        return Result.ok(statisticsService.tertiaryOverview());
    }

    /** 医疗质量统计（26）：医疗质量 / 医疗安全 / 患者安全三组实时聚合指标 */
    @GetMapping("/quality")
    public Result<Map<String, Object>> quality() {
        return Result.ok(statisticsService.qualityOverview());
    }

    /** 导出统计报表 Excel：工作统计（按科室）、三级医院指标、医疗质量统计三个 sheet */
    @GetMapping("/export")
    public void exportReport(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医疗统计报表.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        statisticsService.exportReport(response.getOutputStream());
    }
}
