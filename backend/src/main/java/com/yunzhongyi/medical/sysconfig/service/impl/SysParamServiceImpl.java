package com.yunzhongyi.medical.sysconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.SysParamQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysParam;
import com.yunzhongyi.medical.sysconfig.excel.SysParamExcelUtil;
import com.yunzhongyi.medical.sysconfig.mapper.SysParamMapper;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
import com.yunzhongyi.medical.sysconfig.service.SysParamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统参数服务实现。
 * <p>
 * 关键守卫：参数键全院唯一；分组与值类型须合法；参数值须符合值类型
 * （NUMBER 须为数字、BOOLEAN 只能是 true/false）；内置参数的键不可改、不可删除。
 */
@Service
@RequiredArgsConstructor
public class SysParamServiceImpl
        extends ServiceImpl<SysParamMapper, SysParam>
        implements SysParamService {

    @Override
    public PageResult<SysParam> pageQuery(SysParamQuery query) {
        Page<SysParam> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public SysParam detail(Long id) {
        SysParam p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("系统参数不存在：" + id);
        }
        return p;
    }

    @Override
    public SysParam getByKey(String paramKey) {
        SysParam p = this.getOne(new LambdaQueryWrapper<SysParam>()
                .eq(SysParam::getParamKey, paramKey));
        if (p == null) {
            throw new IllegalArgumentException("系统参数不存在：" + paramKey);
        }
        if (!Integer.valueOf(SysConfigTypeUtil.STATUS_ENABLED).equals(p.getStatus())) {
            throw new IllegalArgumentException("系统参数已停用：" + paramKey);
        }
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysParam create(SysParam param) {
        validateParam(param, null);
        applyDefaults(param);
        this.save(param);
        return param;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysParam update(SysParam param) {
        SysParam old = detail(param.getId());
        validateParam(param, param.getId());
        if (Integer.valueOf(1).equals(old.getIsSystem())
                && !old.getParamKey().equals(param.getParamKey())) {
            throw new IllegalArgumentException("内置参数的参数键不可修改");
        }
        if (Integer.valueOf(1).equals(old.getIsSystem())) {
            param.setIsSystem(1);
        }
        applyDefaults(param);
        this.updateById(param);
        return param;
    }

    @Override
    public SysParam enable(Long id) {
        SysParam p = detail(id);
        p.setStatus(SysConfigTypeUtil.STATUS_ENABLED);
        this.updateById(p);
        return p;
    }

    @Override
    public SysParam disable(Long id) {
        SysParam p = detail(id);
        p.setStatus(SysConfigTypeUtil.STATUS_DISABLED);
        this.updateById(p);
        return p;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        SysParam p = detail((Long) id);
        if (Integer.valueOf(1).equals(p.getIsSystem())) {
            throw new IllegalArgumentException("内置参数不可删除");
        }
        return super.removeById(id);
    }

    @Override
    public List<SysParam> enabled() {
        return this.list(new LambdaQueryWrapper<SysParam>()
                .eq(SysParam::getStatus, SysConfigTypeUtil.STATUS_ENABLED)
                .orderByAsc(SysParam::getParamKey));
    }

    @Override
    public Map<String, Object> stats() {
        List<SysParam> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream()
                .filter(p -> Integer.valueOf(SysConfigTypeUtil.STATUS_ENABLED).equals(p.getStatus())).count());
        r.put("disabled", all.stream()
                .filter(p -> Integer.valueOf(SysConfigTypeUtil.STATUS_DISABLED).equals(p.getStatus())).count());
        r.put("system", all.stream()
                .filter(p -> Integer.valueOf(1).equals(p.getIsSystem())).count());
        r.put("custom", all.stream()
                .filter(p -> !Integer.valueOf(1).equals(p.getIsSystem())).count());
        Map<String, Long> byGroup = new LinkedHashMap<>();
        for (String group : List.of(SysConfigTypeUtil.GROUP_SYSTEM,
                SysConfigTypeUtil.GROUP_BUSINESS, SysConfigTypeUtil.GROUP_NOTIFY)) {
            byGroup.put(group, all.stream().filter(p -> group.equals(p.getParamGroup())).count());
        }
        r.put("byGroup", byGroup);
        return r;
    }

    @Override
    public void exportExcel(SysParamQuery query, OutputStream out) {
        SysParamExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<SysParam> rows = SysParamExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (SysParam row : rows) {
            try {
                validateParam(row, null);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<SysParam>()
                    .eq(SysParam::getParamKey, row.getParamKey()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            applyDefaults(row);
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
        SysParamExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<SysParam> buildWrapper(SysParamQuery query) {
        LambdaQueryWrapper<SysParam> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(SysParam::getParamName, kw)
                    .or().like(SysParam::getParamKey, kw));
        }
        if (StringUtils.hasText(query.getParamGroup())) {
            w.eq(SysParam::getParamGroup, query.getParamGroup());
        }
        if (StringUtils.hasText(query.getValueType())) {
            w.eq(SysParam::getValueType, query.getValueType());
        }
        if (query.getStatus() != null) {
            w.eq(SysParam::getStatus, query.getStatus());
        }
        w.orderByAsc(SysParam::getParamGroup).orderByAsc(SysParam::getParamKey);
        return w;
    }

    private void applyDefaults(SysParam p) {
        if (p.getParamGroup() == null) {
            p.setParamGroup(SysConfigTypeUtil.GROUP_BUSINESS);
        }
        if (p.getValueType() == null) {
            p.setValueType(SysConfigTypeUtil.VALUE_STRING);
        }
        if (p.getStatus() == null) {
            p.setStatus(SysConfigTypeUtil.STATUS_ENABLED);
        }
        if (p.getIsSystem() == null) {
            p.setIsSystem(0);
        }
    }

    private void validateParam(SysParam p, Long excludeId) {
        if (!StringUtils.hasText(p.getParamKey())) {
            throw new IllegalArgumentException("参数键不能为空");
        }
        if (!StringUtils.hasText(p.getParamName())) {
            throw new IllegalArgumentException("参数名称不能为空");
        }
        if (p.getParamGroup() != null && !SysConfigTypeUtil.PARAM_GROUPS.contains(p.getParamGroup())) {
            throw new IllegalArgumentException("参数分组不合法：" + p.getParamGroup());
        }
        if (p.getValueType() != null && !SysConfigTypeUtil.VALUE_TYPES.contains(p.getValueType())) {
            throw new IllegalArgumentException("值类型不合法：" + p.getValueType());
        }
        if (!SysConfigTypeUtil.valueMatchesType(p.getParamValue(), p.getValueType())) {
            throw new IllegalArgumentException("参数值与值类型不符：" + p.getParamValue());
        }
        long dup = this.count(new LambdaQueryWrapper<SysParam>()
                .eq(SysParam::getParamKey, p.getParamKey())
                .ne(excludeId != null, SysParam::getId, excludeId));
        if (dup > 0) {
            throw new IllegalArgumentException("参数键已存在：" + p.getParamKey());
        }
    }
}
