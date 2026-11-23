package com.yunzhongyi.medical.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.system.entity.SysOperLog;
import com.yunzhongyi.medical.system.excel.SysOperLogExcelUtil;
import com.yunzhongyi.medical.system.mapper.SysOperLogMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志查询接口（底座）。
 * <p>
 * 第 28 项补强：日志统计（按模块/结果分组）、导出 Excel；日志只读，不提供导入。
 */
@RestController
@RequestMapping("/api/oper-logs")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogMapper operLogMapper;

    @GetMapping
    public Result<PageResult<SysOperLog>> page(@RequestParam(required = false) String module,
                                               @RequestParam(required = false) String username,
                                               @RequestParam(required = false) Integer result,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size) {
        Page<SysOperLog> p = operLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysOperLog>()
                        .eq(StringUtils.hasText(module), SysOperLog::getModule, module)
                        .like(StringUtils.hasText(username), SysOperLog::getUsername, username)
                        .eq(result != null, SysOperLog::getResult, result)
                        .orderByDesc(SysOperLog::getId));
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 日志统计：总数 / 成功 / 失败 / 按模块分组 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<SysOperLog> all = operLogMapper.selectList(null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("success", all.stream().filter(l -> Integer.valueOf(1).equals(l.getResult())).count());
        r.put("failed", all.stream().filter(l -> Integer.valueOf(0).equals(l.getResult())).count());
        Map<String, Long> byModule = new LinkedHashMap<>();
        for (SysOperLog log : all) {
            String key = log.getModule() == null ? "未分类" : log.getModule();
            byModule.merge(key, 1L, Long::sum);
        }
        r.put("byModule", byModule);
        return Result.ok(r);
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String module,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) Integer result,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("操作日志.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        List<SysOperLog> list = operLogMapper.selectList(new LambdaQueryWrapper<SysOperLog>()
                .eq(StringUtils.hasText(module), SysOperLog::getModule, module)
                .like(StringUtils.hasText(username), SysOperLog::getUsername, username)
                .eq(result != null, SysOperLog::getResult, result)
                .orderByDesc(SysOperLog::getId));
        SysOperLogExcelUtil.write(list, response.getOutputStream());
    }
}
