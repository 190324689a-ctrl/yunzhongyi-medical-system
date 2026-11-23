package com.yunzhongyi.medical.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.inspection.dto.InspectionTemplateItemQuery;
import com.yunzhongyi.medical.inspection.dto.TemplateItemConfigRequest;
import com.yunzhongyi.medical.inspection.entity.InspectionItem;
import com.yunzhongyi.medical.inspection.entity.InspectionTemplateItem;
import com.yunzhongyi.medical.inspection.excel.InspectionTemplateItemExcelUtil;
import com.yunzhongyi.medical.inspection.mapper.InspectionItemMapper;
import com.yunzhongyi.medical.inspection.mapper.InspectionTemplateItemMapper;
import com.yunzhongyi.medical.inspection.service.InspectionTemplateItemService;
import com.yunzhongyi.medical.inspection.service.InspectionTypeUtil;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.mapper.QualityTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模板检查项配置服务实现：模板检查项整模板替换 + 单项删除 + 统计 +
 * Excel 批量导入导出；配置保存后同步回写质量检查模板的 checkItems JSON。
 * <p>
 * 直接读 QualityTemplateMapper 与 InspectionItemMapper，不依赖另外两个服务，
 * 避免互相注入形成循环依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionTemplateItemServiceImpl
        extends ServiceImpl<InspectionTemplateItemMapper, InspectionTemplateItem>
        implements InspectionTemplateItemService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final QualityTemplateMapper qualityTemplateMapper;
    private final InspectionItemMapper inspectionItemMapper;

    @Override
    public PageResult<InspectionTemplateItem> pageQuery(InspectionTemplateItemQuery query) {
        Page<InspectionTemplateItem> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<InspectionTemplateItem> listByTemplate(Long templateId) {
        return this.list(new LambdaQueryWrapper<InspectionTemplateItem>()
                .eq(InspectionTemplateItem::getTemplateId, templateId)
                .orderByAsc(InspectionTemplateItem::getSortOrder)
                .orderByAsc(InspectionTemplateItem::getId));
    }

    @Override
    public Map<String, Object> stats() {
        List<InspectionTemplateItem> all = this.list();
        Set<Long> templateIds = new HashSet<>();
        for (InspectionTemplateItem link : all) {
            templateIds.add(link.getTemplateId());
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("templatesConfigured", (long) templateIds.size());
        r.put("totalLinks", (long) all.size());
        r.put("avgItemsPerTemplate", templateIds.isEmpty() ? 0
                : BigDecimal.valueOf(all.size())
                        .divide(BigDecimal.valueOf(templateIds.size()), 2, java.math.RoundingMode.HALF_UP));
        long templateTotal = qualityTemplateMapper.selectCount(null);
        r.put("templateTotal", templateTotal);
        r.put("uncoveredTemplates", templateTotal - templateIds.size());
        Set<Long> linkedItemIds = new HashSet<>();
        for (InspectionTemplateItem link : all) {
            linkedItemIds.add(link.getItemId());
        }
        long itemTotal = inspectionItemMapper.selectCount(null);
        r.put("itemTotal", itemTotal);
        r.put("linkedItems", (long) linkedItemIds.size());
        r.put("unlinkedItems", itemTotal - linkedItemIds.size());
        return r;
    }

    @Override
    public InspectionTemplateItem detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<InspectionTemplateItem> configure(TemplateItemConfigRequest request) {
        QualityTemplate template = qualityTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new IllegalArgumentException("检查模板不存在：" + request.getTemplateId());
        }
        List<TemplateItemConfigRequest.TemplateItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("检查项不能为空，至少选择一项");
        }
        // 校验：项目存在且已启用、同一模板内项目不重复、每项权重合法、权重之和为 100
        Set<Long> seen = new HashSet<>();
        List<InspectionItem> resolved = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (TemplateItemConfigRequest.TemplateItem cfg : items) {
            if (cfg.getItemId() == null) {
                throw new IllegalArgumentException("检查项目不能为空");
            }
            if (!seen.add(cfg.getItemId())) {
                throw new IllegalArgumentException("同一模板内检查项目不能重复");
            }
            InspectionItem item = inspectionItemMapper.selectById(cfg.getItemId());
            if (item == null) {
                throw new IllegalArgumentException("检查项目不存在：" + cfg.getItemId());
            }
            if (!Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(item.getStatus())) {
                throw new IllegalArgumentException("检查项目「" + item.getItemName() + "」已停用，不能组装进模板");
            }
            BigDecimal weight = cfg.getWeight() != null ? cfg.getWeight() : item.getWeight();
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("检查项目「" + item.getItemName() + "」权重必须大于 0");
            }
            cfg.setWeight(weight);
            sum = sum.add(weight);
            resolved.add(item);
        }
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("检查项权重之和必须为 100，当前为 "
                    + sum.stripTrailingZeros().toPlainString());
        }
        // 整模板替换：先删旧配置，再插新配置
        this.baseMapper.delete(new LambdaQueryWrapper<InspectionTemplateItem>()
                .eq(InspectionTemplateItem::getTemplateId, template.getId()));
        List<InspectionTemplateItem> saved = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            TemplateItemConfigRequest.TemplateItem cfg = items.get(i);
            InspectionItem item = resolved.get(i);
            InspectionTemplateItem link = new InspectionTemplateItem();
            link.setTemplateId(template.getId());
            link.setTemplateName(template.getTemplateName());
            link.setItemId(item.getId());
            link.setItemNo(item.getItemNo());
            link.setItemName(item.getItemName());
            link.setWeight(cfg.getWeight());
            link.setSortOrder(cfg.getSortOrder() != null ? cfg.getSortOrder() : i + 1);
            this.save(link);
            saved.add(link);
        }
        saved.sort((a, b) -> {
            int c = Integer.compare(
                    a.getSortOrder() == null ? 0 : a.getSortOrder(),
                    b.getSortOrder() == null ? 0 : b.getSortOrder());
            return c != 0 ? c : Long.compare(a.getId(), b.getId());
        });
        syncTemplateJson(template, saved, resolved);
        return saved;
    }

    @Override
    public void exportExcel(Long templateId, OutputStream out) {
        List<InspectionTemplateItem> list = templateId == null
                ? this.list(new LambdaQueryWrapper<InspectionTemplateItem>()
                        .orderByAsc(InspectionTemplateItem::getTemplateId)
                        .orderByAsc(InspectionTemplateItem::getSortOrder))
                : listByTemplate(templateId);
        InspectionTemplateItemExcelUtil.write(list, out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(Long templateId, InputStream in) {
        QualityTemplate template = qualityTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("检查模板不存在：" + templateId);
        }
        List<InspectionTemplateItem> rows = InspectionTemplateItemExcelUtil.read(in);
        TemplateItemConfigRequest request = new TemplateItemConfigRequest();
        request.setTemplateId(templateId);
        List<TemplateItemConfigRequest.TemplateItem> items = new ArrayList<>();
        Set<String> seenNo = new HashSet<>();
        for (InspectionTemplateItem row : rows) {
            if (!StringUtils.hasText(row.getItemNo())) {
                log.warn("导入跳过：项目编号为空的行");
                continue;
            }
            if (!seenNo.add(row.getItemNo().trim())) {
                log.warn("导入跳过：重复项目编号 {}", row.getItemNo());
                continue;
            }
            InspectionItem item = inspectionItemMapper.selectOne(new LambdaQueryWrapper<InspectionItem>()
                    .eq(InspectionItem::getItemNo, row.getItemNo().trim()));
            if (item == null) {
                log.warn("导入跳过：项目编号 {} 在检查项目库中不存在", row.getItemNo());
                continue;
            }
            if (!Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(item.getStatus())) {
                log.warn("导入跳过：检查项目「{}」已停用", item.getItemName());
                continue;
            }
            TemplateItemConfigRequest.TemplateItem cfg = new TemplateItemConfigRequest.TemplateItem();
            cfg.setItemId(item.getId());
            cfg.setWeight(row.getWeight() != null ? row.getWeight() : item.getWeight());
            cfg.setSortOrder(row.getSortOrder());
            items.add(cfg);
        }
        request.setItems(items);
        List<InspectionTemplateItem> saved = configure(request);
        return saved.size();
    }

    /** 删除单项：一个模板至少保留一项；删除后同步回写模板 JSON */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        InspectionTemplateItem link = getOrThrow((Long) id);
        List<InspectionTemplateItem> rest = listByTemplate(link.getTemplateId());
        if (rest.size() <= 1) {
            throw new IllegalStateException("一个模板至少保留一项，不能删除最后一项");
        }
        boolean ok = super.removeById(id);
        if (ok) {
            List<InspectionTemplateItem> remaining = listByTemplate(link.getTemplateId());
            QualityTemplate template = qualityTemplateMapper.selectById(link.getTemplateId());
            if (template != null) {
                syncTemplateJson(template, remaining, loadItems(remaining));
            }
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(java.util.Collection<?> idList) {
        for (Object id : idList) {
            removeById((java.io.Serializable) id);
        }
        return true;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<InspectionTemplateItem> buildWrapper(InspectionTemplateItemQuery q) {
        LambdaQueryWrapper<InspectionTemplateItem> qw = new LambdaQueryWrapper<>();
        qw.eq(q.getTemplateId() != null, InspectionTemplateItem::getTemplateId, q.getTemplateId());
        qw.eq(q.getItemId() != null, InspectionTemplateItem::getItemId, q.getItemId());
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(InspectionTemplateItem::getTemplateName, q.getKeyword())
                .or().like(InspectionTemplateItem::getItemName, q.getKeyword())
                .or().like(InspectionTemplateItem::getItemNo, q.getKeyword()));
        qw.orderByAsc(InspectionTemplateItem::getTemplateId)
                .orderByAsc(InspectionTemplateItem::getSortOrder)
                .orderByAsc(InspectionTemplateItem::getId);
        return qw;
    }

    /**
     * 同步回写模板 checkItems JSON：name 取项目名称、weight 取配置权重、
     * standard 取项目评分标准文字（与模板手写 JSON 格式一致）。
     */
    private void syncTemplateJson(QualityTemplate template,
                                  List<InspectionTemplateItem> links,
                                  List<InspectionItem> items) {
        Map<Long, InspectionItem> byId = new LinkedHashMap<>();
        for (InspectionItem item : items) {
            byId.put(item.getId(), item);
        }
        ArrayNode arr = MAPPER.createArrayNode();
        for (InspectionTemplateItem link : links) {
            InspectionItem item = byId.get(link.getItemId());
            ObjectNode node = MAPPER.createObjectNode();
            node.put("name", link.getItemName());
            node.put("weight", link.getWeight());
            String standard = item != null && StringUtils.hasText(item.getStandardText())
                    ? item.getStandardText() : "";
            node.put("standard", standard);
            arr.add(node);
        }
        try {
            template.setCheckItems(MAPPER.writeValueAsString(arr));
        } catch (Exception e) {
            throw new RuntimeException("同步模板检查项 JSON 失败: " + e.getMessage(), e);
        }
        qualityTemplateMapper.updateById(template);
    }

    private List<InspectionItem> loadItems(List<InspectionTemplateItem> links) {
        List<InspectionItem> items = new ArrayList<>();
        for (InspectionTemplateItem link : links) {
            InspectionItem item = inspectionItemMapper.selectById(link.getItemId());
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private InspectionTemplateItem getOrThrow(Long id) {
        InspectionTemplateItem p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("模板检查项配置不存在：" + id);
        }
        return p;
    }
}
