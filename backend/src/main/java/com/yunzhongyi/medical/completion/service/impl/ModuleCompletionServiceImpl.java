package com.yunzhongyi.medical.completion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.completion.dto.ModuleCompletionQuery;
import com.yunzhongyi.medical.completion.entity.ModuleCompletion;
import com.yunzhongyi.medical.completion.excel.ModuleCompletionExcelUtil;
import com.yunzhongyi.medical.completion.mapper.ModuleCompletionMapper;
import com.yunzhongyi.medical.completion.service.CompletionTypeUtil;
import com.yunzhongyi.medical.completion.service.ModuleCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医务系统完成统计服务实现。
 * <p>
 * 关键守卫：模块编号全院唯一；进度 0-100；已完成时进度强制 100、
 * 完成日期为空默认当天，未完成时完成日期清空；只有未完成的模块能
 * 标记完成，只有已完成的模块能重新开始。统计全部对台账现算，
 * 接口数 / 测试数为空（计入其他模块）时按 0 计。
 */
@Service
@RequiredArgsConstructor
public class ModuleCompletionServiceImpl
        extends ServiceImpl<ModuleCompletionMapper, ModuleCompletion>
        implements ModuleCompletionService {

    @Override
    public PageResult<ModuleCompletion> pageQuery(ModuleCompletionQuery query) {
        Page<ModuleCompletion> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public ModuleCompletion detail(Long id) {
        ModuleCompletion r = this.getById(id);
        if (r == null) {
            throw new IllegalArgumentException("模块完成记录不存在：" + id);
        }
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuleCompletion create(ModuleCompletion record) {
        validate(record, null);
        applyStatusRules(record);
        this.save(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuleCompletion update(ModuleCompletion record) {
        detail(record.getId());
        validate(record, record.getId());
        applyStatusRules(record);
        this.updateById(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuleCompletion complete(Long id) {
        ModuleCompletion r = detail(id);
        if (Integer.valueOf(CompletionTypeUtil.STATUS_COMPLETED).equals(r.getStatus())) {
            throw new IllegalArgumentException("该模块已完成，无需重复标记");
        }
        r.setStatus(CompletionTypeUtil.STATUS_COMPLETED);
        r.setProgress(100);
        r.setCompletedDate(LocalDate.now());
        this.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModuleCompletion reopen(Long id) {
        ModuleCompletion r = detail(id);
        if (!Integer.valueOf(CompletionTypeUtil.STATUS_COMPLETED).equals(r.getStatus())) {
            throw new IllegalArgumentException("只有已完成的模块能重新开始");
        }
        r.setStatus(CompletionTypeUtil.STATUS_IN_PROGRESS);
        r.setProgress(50);
        r.setCompletedDate(null);
        this.updateById(r);
        return r;
    }

    @Override
    public Map<String, Object> stats() {
        List<ModuleCompletion> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        long completed = countByStatus(all, CompletionTypeUtil.STATUS_COMPLETED);
        long inProgress = countByStatus(all, CompletionTypeUtil.STATUS_IN_PROGRESS);
        long pending = countByStatus(all, CompletionTypeUtil.STATUS_PENDING);
        r.put("completed", completed);
        r.put("inProgress", inProgress);
        r.put("pending", pending);
        r.put("completionRate", CompletionTypeUtil.completionRate(completed, all.size()));
        r.put("totalInterfaces", sumInterfaces(all));
        r.put("totalTests", sumTests(all));
        r.put("avgProgress", CompletionTypeUtil.avgProgress(sumProgress(all), all.size()));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("completed", completed);
        byStatus.put("inProgress", inProgress);
        byStatus.put("pending", pending);
        r.put("byStatus", byStatus);
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByCategory() {
        List<ModuleCompletion> all = this.list();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String category : CompletionTypeUtil.CATEGORIES) {
            List<ModuleCompletion> group = all.stream()
                    .filter(r -> category.equals(r.getCategory())).toList();
            if (group.isEmpty()) {
                continue;
            }
            long completed = countByStatus(group, CompletionTypeUtil.STATUS_COMPLETED);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", category);
            row.put("total", (long) group.size());
            row.put("completed", completed);
            row.put("inProgress", countByStatus(group, CompletionTypeUtil.STATUS_IN_PROGRESS));
            row.put("pending", countByStatus(group, CompletionTypeUtil.STATUS_PENDING));
            row.put("completionRate",
                    CompletionTypeUtil.completionRate(completed, group.size()));
            row.put("totalInterfaces", sumInterfaces(group));
            row.put("totalTests", sumTests(group));
            row.put("avgProgress",
                    CompletionTypeUtil.avgProgress(sumProgress(group), group.size()));
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        List<ModuleCompletion> all = this.list();
        Map<String, Long> byMonth = new LinkedHashMap<>();
        all.stream()
                .filter(r -> Integer.valueOf(CompletionTypeUtil.STATUS_COMPLETED)
                        .equals(r.getStatus()) && r.getCompletedDate() != null)
                .sorted((a, b) -> a.getCompletedDate().compareTo(b.getCompletedDate()))
                .forEach(r -> {
                    String month = r.getCompletedDate().toString().substring(0, 7);
                    byMonth.merge(month, 1L, Long::sum);
                });
        List<Map<String, Object>> rows = new ArrayList<>();
        byMonth.forEach((month, count) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("completedCount", count);
            rows.add(row);
        });
        return rows;
    }

    @Override
    public void exportExcel(ModuleCompletionQuery query, OutputStream out) {
        ModuleCompletionExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<ModuleCompletion> rows = ModuleCompletionExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (ModuleCompletion row : rows) {
            try {
                validate(row, null);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            applyStatusRules(row);
            this.save(row);
            created++;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("created", created);
        r.put("skipped", skipped);
        r.put("total", rows.size());
        return r;
    }

    @Override
    public void writeImportTemplate(OutputStream out) {
        ModuleCompletionExcelUtil.writeTemplate(out);
    }

    // ---------- 校验与规则 ----------

    private void validate(ModuleCompletion record, Long selfId) {
        if (record.getModuleNo() == null) {
            throw new IllegalArgumentException("模块编号不能为空");
        }
        if (!StringUtils.hasText(record.getModuleName())) {
            throw new IllegalArgumentException("模块名称不能为空");
        }
        if (!CompletionTypeUtil.isValidCategory(record.getCategory())) {
            throw new IllegalArgumentException("分组不合法：" + record.getCategory());
        }
        if (record.getStatus() != null
                && !CompletionTypeUtil.isValidStatus(record.getStatus())) {
            throw new IllegalArgumentException("状态不合法：" + record.getStatus());
        }
        if (record.getProgress() != null
                && (record.getProgress() < 0 || record.getProgress() > 100)) {
            throw new IllegalArgumentException("完成进度须在 0-100 之间");
        }
        if (record.getInterfaceCount() != null && record.getInterfaceCount() < 0) {
            throw new IllegalArgumentException("接口数不能为负");
        }
        if (record.getTestCount() != null && record.getTestCount() < 0) {
            throw new IllegalArgumentException("测试数不能为负");
        }
        long dup = this.count(new LambdaQueryWrapper<ModuleCompletion>()
                .eq(ModuleCompletion::getModuleNo, record.getModuleNo())
                .ne(selfId != null, ModuleCompletion::getId, selfId));
        if (dup > 0) {
            throw new IllegalArgumentException("模块编号已存在：" + record.getModuleNo());
        }
    }

    /**
     * 状态联动规则：已完成 → 进度 100、完成日期为空默认当天；
     * 未完成 → 完成日期清空；状态为空默认待开发、进度为空默认 0。
     */
    private void applyStatusRules(ModuleCompletion record) {
        if (record.getStatus() == null) {
            record.setStatus(CompletionTypeUtil.STATUS_PENDING);
        }
        if (record.getProgress() == null) {
            record.setProgress(CompletionTypeUtil.STATUS_COMPLETED == record.getStatus() ? 100 : 0);
        }
        if (Integer.valueOf(CompletionTypeUtil.STATUS_COMPLETED).equals(record.getStatus())) {
            record.setProgress(100);
            if (record.getCompletedDate() == null) {
                record.setCompletedDate(LocalDate.now());
            }
        } else {
            record.setCompletedDate(null);
        }
    }

    private LambdaQueryWrapper<ModuleCompletion> buildWrapper(ModuleCompletionQuery query) {
        LambdaQueryWrapper<ModuleCompletion> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            w.like(ModuleCompletion::getModuleName, query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getCategory())) {
            w.eq(ModuleCompletion::getCategory, query.getCategory());
        }
        if (query.getStatus() != null) {
            w.eq(ModuleCompletion::getStatus, query.getStatus());
        }
        if (query.getModuleNo() != null) {
            w.eq(ModuleCompletion::getModuleNo, query.getModuleNo());
        }
        w.orderByAsc(ModuleCompletion::getModuleNo);
        return w;
    }

    private long countByStatus(List<ModuleCompletion> list, int status) {
        return list.stream().filter(r -> Integer.valueOf(status).equals(r.getStatus())).count();
    }

    private long sumInterfaces(List<ModuleCompletion> list) {
        return list.stream().mapToLong(r -> r.getInterfaceCount() == null
                ? 0 : r.getInterfaceCount()).sum();
    }

    private long sumTests(List<ModuleCompletion> list) {
        return list.stream().mapToLong(r -> r.getTestCount() == null
                ? 0 : r.getTestCount()).sum();
    }

    private long sumProgress(List<ModuleCompletion> list) {
        return list.stream().mapToLong(r -> r.getProgress() == null
                ? 0 : r.getProgress()).sum();
    }
}
