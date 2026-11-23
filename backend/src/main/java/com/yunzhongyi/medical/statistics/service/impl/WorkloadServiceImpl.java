package com.yunzhongyi.medical.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.statistics.dto.WorkloadQuery;
import com.yunzhongyi.medical.statistics.entity.WorkloadRecord;
import com.yunzhongyi.medical.statistics.excel.WorkloadExcelUtil;
import com.yunzhongyi.medical.statistics.mapper.WorkloadRecordMapper;
import com.yunzhongyi.medical.statistics.service.WorkloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作统计服务实现：工作量登记 CRUD + 按科室 / 按人员 / 按时间统计 +
 * Excel 批量导入导出
 */
@Slf4j
@Service
public class WorkloadServiceImpl
        extends ServiceImpl<WorkloadRecordMapper, WorkloadRecord>
        implements WorkloadService {

    @Override
    public PageResult<WorkloadRecord> pageQuery(WorkloadQuery query) {
        Page<WorkloadRecord> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<WorkloadRecord> listByQuery(WorkloadQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<WorkloadRecord> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalOutpatient", sumOutpatient(all));
        r.put("totalInpatient", sumInpatient(all));
        r.put("totalSurgery", sumSurgery(all));
        r.put("totalWorkload", sumTotal(all));
        r.put("recordCount", (long) all.size());
        r.put("physicianCount", all.stream().map(WorkloadRecord::getPhysicianName)
                .filter(StringUtils::hasText).distinct().count());
        r.put("deptCount", all.stream().map(WorkloadRecord::getDeptName)
                .filter(StringUtils::hasText).distinct().count());
        r.put("byDept", statsByDept());
        r.put("byPhysician", statsByPhysician());
        r.put("byMonth", statsByMonth());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<WorkloadRecord>> grouped = new LinkedHashMap<>();
        for (WorkloadRecord w : this.list()) {
            String dept = StringUtils.hasText(w.getDeptName()) ? w.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(w);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("outpatientCount", sumOutpatient(list));
            row.put("inpatientCount", sumInpatient(list));
            row.put("surgeryCount", sumSurgery(list));
            row.put("totalWorkload", sumTotal(list));
            row.put("physicianCount", list.stream().map(WorkloadRecord::getPhysicianName)
                    .filter(StringUtils::hasText).distinct().count());
            row.put("recordCount", (long) list.size());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("totalWorkload"), (Long) a.get("totalWorkload")));
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByPhysician() {
        Map<String, List<WorkloadRecord>> grouped = new LinkedHashMap<>();
        for (WorkloadRecord w : this.list()) {
            String name = StringUtils.hasText(w.getPhysicianName()) ? w.getPhysicianName() : "未知";
            grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(w);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((name, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("physicianName", name);
            row.put("deptName", list.get(0).getDeptName());
            row.put("outpatientCount", sumOutpatient(list));
            row.put("inpatientCount", sumInpatient(list));
            row.put("surgeryCount", sumSurgery(list));
            row.put("totalWorkload", sumTotal(list));
            row.put("recordCount", (long) list.size());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("totalWorkload"), (Long) a.get("totalWorkload")));
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        Map<String, List<WorkloadRecord>> grouped = new LinkedHashMap<>();
        for (WorkloadRecord w : this.list()) {
            if (!StringUtils.hasText(w.getStatMonth())) {
                continue;
            }
            grouped.computeIfAbsent(w.getStatMonth(), k -> new ArrayList<>()).add(w);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.keySet().stream().sorted().forEach(month -> {
            List<WorkloadRecord> list = grouped.get(month);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month);
            row.put("outpatientCount", sumOutpatient(list));
            row.put("inpatientCount", sumInpatient(list));
            row.put("surgeryCount", sumSurgery(list));
            row.put("totalWorkload", sumTotal(list));
            row.put("recordCount", (long) list.size());
            result.add(row);
        });
        return result;
    }

    @Override
    public void exportExcel(WorkloadQuery query, OutputStream out) {
        WorkloadExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<WorkloadRecord> list = WorkloadExcelUtil.read(in);
        int ok = 0;
        for (WorkloadRecord w : list) {
            if (!StringUtils.hasText(w.getPhysicianName())) {
                log.warn("导入跳过：医师姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(w.getStatMonth())
                    || !w.getStatMonth().matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
                log.warn("导入跳过：统计月份为空或格式不正确（医师 {}）", w.getPhysicianName());
                continue;
            }
            normalizeCounts(w);
            // 同一医师同一统计月份视为重复，跳过
            LambdaQueryWrapper<WorkloadRecord> qw = new LambdaQueryWrapper<WorkloadRecord>()
                    .eq(WorkloadRecord::getPhysicianName, w.getPhysicianName())
                    .eq(WorkloadRecord::getStatMonth, w.getStatMonth());
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复工作量登记 {} {}", w.getPhysicianName(), w.getStatMonth());
                continue;
            }
            this.save(w);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<WorkloadRecord> buildWrapper(WorkloadQuery q) {
        LambdaQueryWrapper<WorkloadRecord> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.and(w -> w.like(WorkloadRecord::getPhysicianName, q.getKeyword())
                    .or().like(WorkloadRecord::getDeptName, q.getKeyword()));
        }
        qw.eq(q.getDeptId() != null, WorkloadRecord::getDeptId, q.getDeptId());
        qw.eq(q.getPhysicianId() != null, WorkloadRecord::getPhysicianId, q.getPhysicianId());
        qw.eq(StringUtils.hasText(q.getStatMonth()), WorkloadRecord::getStatMonth, q.getStatMonth());
        qw.ge(StringUtils.hasText(q.getStartMonth()), WorkloadRecord::getStatMonth, q.getStartMonth());
        qw.le(StringUtils.hasText(q.getEndMonth()), WorkloadRecord::getStatMonth, q.getEndMonth());
        qw.orderByDesc(WorkloadRecord::getStatMonth).orderByDesc(WorkloadRecord::getId);
        return qw;
    }

    /** 新建时补默认值并校验重复；更新时只校验本条之外的重复 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WorkloadRecord entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WorkloadRecord entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(WorkloadRecord w, boolean isCreate) {
        if (w.getOutpatientCount() != null && w.getOutpatientCount() < 0
                || w.getInpatientCount() != null && w.getInpatientCount() < 0
                || w.getSurgeryCount() != null && w.getSurgeryCount() < 0) {
            throw new IllegalArgumentException("工作量不能为负数");
        }
        normalizeCounts(w);
        LambdaQueryWrapper<WorkloadRecord> qw = new LambdaQueryWrapper<WorkloadRecord>()
                .eq(WorkloadRecord::getPhysicianName, w.getPhysicianName())
                .eq(WorkloadRecord::getStatMonth, w.getStatMonth());
        if (!isCreate) {
            qw.ne(WorkloadRecord::getId, w.getId());
        }
        if (this.count(qw) > 0) {
            throw new IllegalArgumentException("该医师 " + w.getStatMonth() + " 的工作量已登记，请修改原记录");
        }
    }

    private void normalizeCounts(WorkloadRecord w) {
        if (w.getOutpatientCount() == null) {
            w.setOutpatientCount(0);
        }
        if (w.getInpatientCount() == null) {
            w.setInpatientCount(0);
        }
        if (w.getSurgeryCount() == null) {
            w.setSurgeryCount(0);
        }
    }

    private static long sumOutpatient(List<WorkloadRecord> list) {
        return list.stream().mapToLong(w -> w.getOutpatientCount() == null ? 0 : w.getOutpatientCount()).sum();
    }

    private static long sumInpatient(List<WorkloadRecord> list) {
        return list.stream().mapToLong(w -> w.getInpatientCount() == null ? 0 : w.getInpatientCount()).sum();
    }

    private static long sumSurgery(List<WorkloadRecord> list) {
        return list.stream().mapToLong(w -> w.getSurgeryCount() == null ? 0 : w.getSurgeryCount()).sum();
    }

    private static long sumTotal(List<WorkloadRecord> list) {
        return sumOutpatient(list) + sumInpatient(list) + sumSurgery(list);
    }
}
