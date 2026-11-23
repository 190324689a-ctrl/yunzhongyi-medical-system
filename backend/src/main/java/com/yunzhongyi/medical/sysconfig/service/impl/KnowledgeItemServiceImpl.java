package com.yunzhongyi.medical.sysconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.KnowledgeItemQuery;
import com.yunzhongyi.medical.sysconfig.entity.KnowledgeItem;
import com.yunzhongyi.medical.sysconfig.excel.KnowledgeItemExcelUtil;
import com.yunzhongyi.medical.sysconfig.mapper.KnowledgeItemMapper;
import com.yunzhongyi.medical.sysconfig.service.KnowledgeItemService;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
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
 * 知识库服务实现。
 * <p>
 * 关键守卫：知识编码全院唯一；知识分类须合法；打开详情时查阅量 +1；
 * 导入的知识查阅量从 0 开始计（不沿用导出文件里的旧数字）。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeItemServiceImpl
        extends ServiceImpl<KnowledgeItemMapper, KnowledgeItem>
        implements KnowledgeItemService {

    @Override
    public PageResult<KnowledgeItem> pageQuery(KnowledgeItemQuery query) {
        Page<KnowledgeItem> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public KnowledgeItem detail(Long id) {
        KnowledgeItem k = this.getById(id);
        if (k == null) {
            throw new IllegalArgumentException("知识库条目不存在：" + id);
        }
        k.setViewCount(k.getViewCount() == null ? 1L : k.getViewCount() + 1);
        this.updateById(k);
        return k;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeItem create(KnowledgeItem item) {
        validateItem(item, null);
        applyDefaults(item);
        this.save(item);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeItem update(KnowledgeItem item) {
        KnowledgeItem old = this.getById(item.getId());
        if (old == null) {
            throw new IllegalArgumentException("知识库条目不存在：" + item.getId());
        }
        validateItem(item, item.getId());
        // 查阅量不随编辑改写，沿用原值
        item.setViewCount(old.getViewCount());
        applyDefaults(item);
        this.updateById(item);
        return item;
    }

    @Override
    public KnowledgeItem enable(Long id) {
        KnowledgeItem k = this.getById(id);
        if (k == null) {
            throw new IllegalArgumentException("知识库条目不存在：" + id);
        }
        k.setStatus(SysConfigTypeUtil.STATUS_ENABLED);
        this.updateById(k);
        return k;
    }

    @Override
    public KnowledgeItem disable(Long id) {
        KnowledgeItem k = this.getById(id);
        if (k == null) {
            throw new IllegalArgumentException("知识库条目不存在：" + id);
        }
        k.setStatus(SysConfigTypeUtil.STATUS_DISABLED);
        this.updateById(k);
        return k;
    }

    @Override
    public List<KnowledgeItem> enabled() {
        return this.list(new LambdaQueryWrapper<KnowledgeItem>()
                .eq(KnowledgeItem::getStatus, SysConfigTypeUtil.STATUS_ENABLED)
                .orderByAsc(KnowledgeItem::getSortOrder).orderByAsc(KnowledgeItem::getId));
    }

    @Override
    public Map<String, Object> stats() {
        List<KnowledgeItem> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream()
                .filter(k -> Integer.valueOf(SysConfigTypeUtil.STATUS_ENABLED).equals(k.getStatus())).count());
        r.put("disabled", all.stream()
                .filter(k -> Integer.valueOf(SysConfigTypeUtil.STATUS_DISABLED).equals(k.getStatus())).count());
        r.put("totalViews", all.stream()
                .mapToLong(k -> k.getViewCount() == null ? 0L : k.getViewCount()).sum());
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String cat : SysConfigTypeUtil.KNOWLEDGE_CATEGORIES) {
            byCategory.put(cat, all.stream().filter(k -> cat.equals(k.getCategory())).count());
        }
        r.put("byCategory", byCategory);
        return r;
    }

    @Override
    public void exportExcel(KnowledgeItemQuery query, OutputStream out) {
        KnowledgeItemExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<KnowledgeItem> rows = KnowledgeItemExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (KnowledgeItem row : rows) {
            try {
                validateItem(row, null);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<KnowledgeItem>()
                    .eq(KnowledgeItem::getKbCode, row.getKbCode()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            row.setViewCount(0L);
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
        KnowledgeItemExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<KnowledgeItem> buildWrapper(KnowledgeItemQuery query) {
        LambdaQueryWrapper<KnowledgeItem> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(KnowledgeItem::getTitle, kw)
                    .or().like(KnowledgeItem::getContent, kw)
                    .or().like(KnowledgeItem::getKeywords, kw));
        }
        if (StringUtils.hasText(query.getCategory())) {
            w.eq(KnowledgeItem::getCategory, query.getCategory());
        }
        if (query.getStatus() != null) {
            w.eq(KnowledgeItem::getStatus, query.getStatus());
        }
        w.orderByAsc(KnowledgeItem::getSortOrder).orderByDesc(KnowledgeItem::getId);
        return w;
    }

    private void applyDefaults(KnowledgeItem k) {
        if (k.getCategory() == null) {
            k.setCategory("其他");
        }
        if (k.getStatus() == null) {
            k.setStatus(SysConfigTypeUtil.STATUS_ENABLED);
        }
        if (k.getViewCount() == null) {
            k.setViewCount(0L);
        }
        if (k.getSortOrder() == null) {
            k.setSortOrder(0);
        }
    }

    private void validateItem(KnowledgeItem k, Long excludeId) {
        if (!StringUtils.hasText(k.getKbCode())) {
            throw new IllegalArgumentException("知识编码不能为空");
        }
        if (!StringUtils.hasText(k.getTitle())) {
            throw new IllegalArgumentException("知识标题不能为空");
        }
        if (k.getCategory() != null && !SysConfigTypeUtil.KNOWLEDGE_CATEGORIES.contains(k.getCategory())) {
            throw new IllegalArgumentException("知识分类不合法：" + k.getCategory());
        }
        long dup = this.count(new LambdaQueryWrapper<KnowledgeItem>()
                .eq(KnowledgeItem::getKbCode, k.getKbCode())
                .ne(excludeId != null, KnowledgeItem::getId, excludeId));
        if (dup > 0) {
            throw new IllegalArgumentException("知识编码已存在：" + k.getKbCode());
        }
    }
}
