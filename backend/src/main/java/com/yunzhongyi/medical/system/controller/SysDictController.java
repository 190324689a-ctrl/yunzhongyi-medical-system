package com.yunzhongyi.medical.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.common.Result;
import com.yunzhongyi.medical.system.entity.SysDictData;
import com.yunzhongyi.medical.system.entity.SysDictType;
import com.yunzhongyi.medical.system.excel.SysDictDataExcelUtil;
import com.yunzhongyi.medical.system.mapper.SysDictTypeMapper;
import com.yunzhongyi.medical.system.service.SysDictService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典管理接口。
 * <p>
 * 第 28 项补强：同一字典下数据值唯一、字典统计、批量导入导出。
 */
@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;
    private final SysDictTypeMapper dictTypeMapper;

    /** 按字典编码取数据项（前端下拉框） */
    @GetMapping("/{dictCode}")
    public Result<List<SysDictData>> listByCode(@PathVariable String dictCode) {
        return Result.ok(dictService.listByCode(dictCode));
    }

    /** 分页查询字典数据项 */
    @GetMapping
    public Result<PageResult<SysDictData>> page(@RequestParam(required = false) String dictCode,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        Page<SysDictData> p = dictService.page(new Page<>(page, size),
                new LambdaQueryWrapper<SysDictData>()
                        .eq(dictCode != null, SysDictData::getDictCode, dictCode)
                        .and(StringUtils.hasText(keyword), x -> x.like(SysDictData::getLabel, keyword)
                                .or().like(SysDictData::getDictValue, keyword))
                        .orderByAsc(SysDictData::getSortOrder));
        return Result.ok(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    /** 字典统计：字典类型数 / 数据项总数 / 启用数 / 各字典数据项数 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<SysDictType> types = dictTypeMapper.selectList(null);
        List<SysDictData> all = dictService.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("dictTypes", (long) types.size());
        r.put("total", (long) all.size());
        r.put("enabled", all.stream().filter(d -> Integer.valueOf(1).equals(d.getStatus())).count());
        r.put("disabled", all.stream().filter(d -> Integer.valueOf(0).equals(d.getStatus())).count());
        Map<String, Long> byDict = new LinkedHashMap<>();
        for (SysDictType type : types) {
            byDict.put(type.getName(), all.stream()
                    .filter(d -> type.getCode().equals(d.getDictCode())).count());
        }
        r.put("byDict", byDict);
        return Result.ok(r);
    }

    /** 全部字典类型（字典管理页下拉用） */
    @GetMapping("/types")
    public Result<List<SysDictType>> types() {
        return Result.ok(dictTypeMapper.selectList(
                new LambdaQueryWrapper<SysDictType>().orderByAsc(SysDictType::getId)));
    }

    @PostMapping
    public Result<SysDictData> create(@Valid @RequestBody SysDictData data) {
        if (!StringUtils.hasText(data.getDictCode()) || !StringUtils.hasText(data.getDictValue())) {
            return Result.fail(400, "字典编码与数据值不能为空");
        }
        if (dictService.count(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictCode, data.getDictCode())
                .eq(SysDictData::getDictValue, data.getDictValue())) > 0) {
            return Result.fail(400, "该字典下已存在相同数据值：" + data.getDictValue());
        }
        if (data.getStatus() == null) {
            data.setStatus(1);
        }
        dictService.save(data);
        return Result.ok(data);
    }

    @PutMapping("/{id}")
    public Result<SysDictData> update(@PathVariable Long id, @Valid @RequestBody SysDictData data) {
        SysDictData old = dictService.getById(id);
        if (old == null) {
            return Result.fail(400, "字典数据不存在：" + id);
        }
        if (StringUtils.hasText(data.getDictValue())
                && !data.getDictValue().equals(old.getDictValue())
                && dictService.count(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictCode,
                        StringUtils.hasText(data.getDictCode()) ? data.getDictCode() : old.getDictCode())
                .eq(SysDictData::getDictValue, data.getDictValue())) > 0) {
            return Result.fail(400, "该字典下已存在相同数据值：" + data.getDictValue());
        }
        data.setId(id);
        dictService.updateById(data);
        return Result.ok(data);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        dictService.removeById(id);
        return Result.ok();
    }

    /** 导出 Excel（按当前查询条件） */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String dictCode,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("字典数据.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        List<SysDictData> list = dictService.list(new LambdaQueryWrapper<SysDictData>()
                .eq(dictCode != null, SysDictData::getDictCode, dictCode)
                .orderByAsc(SysDictData::getSortOrder));
        SysDictDataExcelUtil.write(list, response.getOutputStream());
    }

    /** 批量导入 Excel（同一字典下数据值重复或非法行跳过） */
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
            List<SysDictData> rows = SysDictDataExcelUtil.read(file.getInputStream());
            int created = 0;
            int skipped = 0;
            for (SysDictData row : rows) {
                if (!StringUtils.hasText(row.getDictCode()) || !StringUtils.hasText(row.getDictValue())
                        || dictService.count(new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictCode, row.getDictCode())
                        .eq(SysDictData::getDictValue, row.getDictValue())) > 0) {
                    skipped++;
                    continue;
                }
                if (row.getStatus() == null) {
                    row.setStatus(1);
                }
                dictService.save(row);
                created++;
            }
            return Result.ok(Map.of("created", created, "skipped", skipped, "total", rows.size()));
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /** 导入模板下载（空表头，与导入格式一致） */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("字典数据导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        SysDictDataExcelUtil.writeTemplate(response.getOutputStream());
    }
}
