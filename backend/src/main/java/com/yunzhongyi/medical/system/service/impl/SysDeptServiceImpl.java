package com.yunzhongyi.medical.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.system.entity.SysDept;
import com.yunzhongyi.medical.system.excel.SysDeptExcelUtil;
import com.yunzhongyi.medical.system.mapper.SysDeptMapper;
import com.yunzhongyi.medical.system.service.SysDeptService;
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
 * 科室服务实现。
 * <p>
 * 第 28 项补强：科室编码全院唯一；启用/停用（有下级科室的不可停用）；
 * 删除时有下级科室的不可删除；批量导入导出。
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    @Override
    public PageResult<SysDept> page(String keyword, String type, Integer status, long page, long size) {
        LambdaQueryWrapper<SysDept> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(keyword), SysDept::getName, keyword)
          .or().like(StringUtils.hasText(keyword), SysDept::getCode, keyword);
        qw.eq(StringUtils.hasText(type), SysDept::getType, type);
        qw.eq(status != null, SysDept::getStatus, status);
        qw.orderByAsc(SysDept::getSortOrder).orderByDesc(SysDept::getId);
        Page<SysDept> p = this.page(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<SysDept> listEnabled() {
        return this.list(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, 1)
                .orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDept create(SysDept dept) {
        validateDept(dept, null);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        this.save(dept);
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDept update(SysDept dept) {
        SysDept old = this.getById(dept.getId());
        if (old == null) {
            throw new IllegalArgumentException("科室不存在：" + dept.getId());
        }
        validateDept(dept, dept.getId());
        if (StringUtils.hasText(dept.getCode()) && !dept.getCode().equals(old.getCode())
                && this.count(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, dept.getId())) > 0) {
            throw new IllegalArgumentException("该科室已有下级科室，编码不可修改");
        }
        this.updateById(dept);
        return dept;
    }

    @Override
    public SysDept enable(Long id) {
        SysDept d = this.getById(id);
        if (d == null) {
            throw new IllegalArgumentException("科室不存在：" + id);
        }
        d.setStatus(1);
        this.updateById(d);
        return d;
    }

    @Override
    public SysDept disable(Long id) {
        SysDept d = this.getById(id);
        if (d == null) {
            throw new IllegalArgumentException("科室不存在：" + id);
        }
        long children = this.count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new IllegalArgumentException("该科室有 " + children + " 个下级科室，不能停用");
        }
        d.setStatus(0);
        this.updateById(d);
        return d;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        long children = this.count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new IllegalArgumentException("该科室有 " + children + " 个下级科室，不能直接删除");
        }
        return super.removeById(id);
    }

    @Override
    public Map<String, Object> stats() {
        List<SysDept> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream().filter(d -> Integer.valueOf(1).equals(d.getStatus())).count());
        r.put("disabled", all.stream().filter(d -> Integer.valueOf(0).equals(d.getStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of("clinical", "medical-technical", "admin", "outpatient")) {
            byType.put(type, all.stream().filter(d -> type.equals(d.getType())).count());
        }
        r.put("byType", byType);
        return r;
    }

    @Override
    public List<Map<String, Object>> tree() {
        List<SysDept> all = this.list(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getId));
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (SysDept d : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", d.getId());
            node.put("parentId", d.getParentId());
            node.put("name", d.getName());
            node.put("code", d.getCode());
            node.put("type", d.getType());
            node.put("director", d.getDirector());
            node.put("phone", d.getPhone());
            node.put("sortOrder", d.getSortOrder());
            node.put("status", d.getStatus());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(d.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SysDept d : all) {
            Map<String, Object> node = nodes.get(d.getId());
            Long parentId = d.getParentId();
            if (parentId == null || parentId == 0 || !nodes.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodes.get(parentId).get("children");
                children.add(node);
            }
        }
        return roots;
    }

    @Override
    public void exportExcel(String keyword, String type, Integer status, OutputStream out) {
        LambdaQueryWrapper<SysDept> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(keyword), SysDept::getName, keyword)
          .or().like(StringUtils.hasText(keyword), SysDept::getCode, keyword);
        qw.eq(StringUtils.hasText(type), SysDept::getType, type);
        qw.eq(status != null, SysDept::getStatus, status);
        qw.orderByAsc(SysDept::getSortOrder).orderByDesc(SysDept::getId);
        SysDeptExcelUtil.write(this.list(qw), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<SysDept> rows = SysDeptExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (SysDept row : rows) {
            try {
                validateDept(row, null);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            if (row.getParentId() == null) {
                row.setParentId(0L);
            }
            if (row.getStatus() == null) {
                row.setStatus(1);
            }
            if (row.getSortOrder() == null) {
                row.setSortOrder(0);
            }
            this.save(row);
            created++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", rows.size());
        return result;
    }

    @Override
    public void writeImportTemplate(OutputStream out) {
        SysDeptExcelUtil.writeTemplate(out);
    }

    private void validateDept(SysDept dept, Long excludeId) {
        if (!StringUtils.hasText(dept.getName())) {
            throw new IllegalArgumentException("科室名称不能为空");
        }
        if (dept.getParentId() != null && excludeId != null && dept.getParentId().equals(excludeId)) {
            throw new IllegalArgumentException("上级科室不能是自己");
        }
        if (StringUtils.hasText(dept.getCode())) {
            long dup = this.count(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getCode, dept.getCode())
                    .ne(excludeId != null, SysDept::getId, excludeId));
            if (dup > 0) {
                throw new IllegalArgumentException("科室编码已存在：" + dept.getCode());
            }
        }
        if (StringUtils.hasText(dept.getType())
                && !List.of("clinical", "medical-technical", "admin", "outpatient")
                .contains(dept.getType())) {
            throw new IllegalArgumentException("科室类型不合法：" + dept.getType());
        }
    }
}
