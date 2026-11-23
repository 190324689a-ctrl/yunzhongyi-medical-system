package com.yunzhongyi.medical.committee.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.committee.dto.CommitteeQuery;
import com.yunzhongyi.medical.committee.dto.CommitteeRenewRequest;
import com.yunzhongyi.medical.committee.entity.Committee;
import com.yunzhongyi.medical.committee.service.CommitteeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 委员会管理接口：委员会查询/增删改、换届、解散、统计、批量导入导出；
 * 会议记录、决议见 CommitteeMeetingController / CommitteeResolutionController
 */
@RestController
@RequestMapping("/api/committees")
@RequiredArgsConstructor
public class CommitteeController {

    private final CommitteeService committeeService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(CommitteeQuery query) {
        return Result.ok(committeeService.pageQuery(query));
    }

    /** 委员会统计：总数 / 在任 / 已换届 / 已解散 / 按类别分组 / 任期 90 天内到期 / 会议 / 决议聚合统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(committeeService.stats());
    }

    /** 按委员会类别汇总统计 */
    @GetMapping("/stats/by-type")
    public Result<List<Map<String, Object>>> statsByType() {
        return Result.ok(committeeService.statsByType());
    }

    /** 按月会议趋势统计 */
    @GetMapping("/stats/by-month")
    public Result<List<Map<String, Object>>> statsByMonth() {
        return Result.ok(committeeService.statsByMonth());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<Committee> get(@PathVariable Long id) {
        try {
            return Result.ok(committeeService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增委员会（默认在任、第 1 届） */
    @PostMapping
    public Result<Committee> create(@Valid @RequestBody Committee committee) {
        try {
            committeeService.save(committee);
            return Result.ok(committee);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改 */
    @PutMapping("/{id}")
    public Result<Committee> update(@PathVariable Long id, @Valid @RequestBody Committee committee) {
        try {
            committee.setId(id);
            committeeService.updateById(committee);
            return Result.ok(committee);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除（同时删除该委员会的会议与决议） */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        committeeService.removeById(id);
        return Result.ok();
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        committeeService.removeByIds(ids);
        return Result.ok(Map.of("deleted", ids.size()));
    }

    /** 换届：生成新一届记录（届次 +1），原记录改为已换届 */
    @PostMapping("/{id}/renew")
    public Result<Committee> renew(@PathVariable Long id,
                                   @RequestBody(required = false) CommitteeRenewRequest request) {
        try {
            return Result.ok(committeeService.renew(id, request == null ? new CommitteeRenewRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 解散：只有在任的委员会能解散 */
    @PostMapping("/{id}/dissolve")
    public Result<Committee> dissolve(@PathVariable Long id) {
        try {
            return Result.ok(committeeService.dissolve(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(CommitteeQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("委员会.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        committeeService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel */
    @PostMapping("/import")
    public Result<?> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "请上传 Excel 文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.endsWith(".xlsx") || name.endsWith(".xls"))) {
            return Result.fail(400, "只支持 .xlsx / .xls 文件");
        }
        try {
            int count = committeeService.importExcel(file.getInputStream());
            return Result.ok(Map.of("imported", count));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("委员会导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        committeeService.exportExcel(new CommitteeQuery(), response.getOutputStream());
    }
}
