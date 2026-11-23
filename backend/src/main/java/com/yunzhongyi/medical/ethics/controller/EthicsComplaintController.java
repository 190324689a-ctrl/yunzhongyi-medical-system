package com.yunzhongyi.medical.ethics.controller;

import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.ethics.dto.EthicsComplaintQuery;
import com.yunzhongyi.medical.ethics.dto.FeedbackRequest;
import com.yunzhongyi.medical.ethics.dto.HandleRequest;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsComplaint;
import com.yunzhongyi.medical.ethics.service.EthicsComplaintService;
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
 * 医德医风投诉接口：投诉登记、受理 / 调查 / 处理 / 反馈流转、统计、批量导入导出
 */
@RestController
@RequestMapping("/api/ethics-complaints")
@RequiredArgsConstructor
public class EthicsComplaintController {

    private final EthicsComplaintService complaintService;

    /** 多条件组合分页查询 */
    @GetMapping
    public Result<?> page(EthicsComplaintQuery query) {
        return Result.ok(complaintService.pageQuery(query));
    }

    /** 投诉统计：总数、按流转状态分组、按投诉类型分组、按科室汇总、逾期数、反馈率 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(complaintService.stats());
    }

    /** 按科室汇总统计 */
    @GetMapping("/stats/by-dept")
    public Result<List<Map<String, Object>>> statsByDept() {
        return Result.ok(complaintService.statsByDept());
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<EthicsComplaint> get(@PathVariable Long id) {
        try {
            return Result.ok(complaintService.detail(id));
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 新增投诉登记：默认待受理，处理期限默认为投诉日期 + 15 天 */
    @PostMapping
    public Result<EthicsComplaint> create(@Valid @RequestBody EthicsComplaint complaint) {
        try {
            complaintService.save(complaint);
            return Result.ok(complaint);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 修改（只有待受理/调查中的投诉能修改） */
    @PutMapping("/{id}")
    public Result<EthicsComplaint> update(@PathVariable Long id, @Valid @RequestBody EthicsComplaint complaint) {
        try {
            complaint.setId(id);
            complaintService.updateById(complaint);
            return Result.ok(complaint);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            complaintService.removeById(id);
            return Result.ok();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody List<Long> ids) {
        try {
            complaintService.removeByIds(ids);
            return Result.ok(Map.of("deleted", ids.size()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 受理：待受理 → 调查中 */
    @PostMapping("/{id}/accept")
    public Result<EthicsComplaint> accept(@PathVariable Long id,
                                          @RequestBody(required = false) Map<String, String> body) {
        try {
            return Result.ok(complaintService.accept(id, body == null ? null : body.get("handler")));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 处理：调查中 → 已处理，处理结果必填 */
    @PostMapping("/{id}/handle")
    public Result<EthicsComplaint> handle(@PathVariable Long id,
                                          @RequestBody(required = false) HandleRequest request) {
        try {
            return Result.ok(complaintService.handle(id, request == null ? new HandleRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 反馈：已处理 → 已反馈，反馈内容必填 */
    @PostMapping("/{id}/feedback")
    public Result<EthicsComplaint> feedback(@PathVariable Long id,
                                            @RequestBody(required = false) FeedbackRequest request) {
        try {
            return Result.ok(complaintService.feedback(id, request == null ? new FeedbackRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 不予受理：待受理 → 不予受理，原因必填 */
    @PostMapping("/{id}/reject")
    public Result<EthicsComplaint> reject(@PathVariable Long id,
                                          @RequestBody(required = false) RejectRequest request) {
        try {
            return Result.ok(complaintService.reject(id, request == null ? new RejectRequest() : request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(EthicsComplaintQuery query, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("医德医风投诉.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        complaintService.exportExcel(query, response.getOutputStream());
    }

    /** 批量导入 Excel（同一投诉编号重复跳过；按姓名查医师档案补全） */
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
            int count = complaintService.importExcel(file.getInputStream());
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
                + URLEncoder.encode("医德医风投诉导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        complaintService.exportExcel(new EthicsComplaintQuery(), response.getOutputStream());
    }
}
