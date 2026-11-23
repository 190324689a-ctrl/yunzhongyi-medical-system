package com.yunzhongyi.medical.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.inspection.dto.InspectionCategoryQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionCategory;
import com.yunzhongyi.medical.inspection.entity.InspectionItem;
import com.yunzhongyi.medical.inspection.entity.InspectionTemplateItem;
import com.yunzhongyi.medical.inspection.excel.InspectionCategoryExcelUtil;
import com.yunzhongyi.medical.inspection.mapper.InspectionCategoryMapper;
import com.yunzhongyi.medical.inspection.mapper.InspectionItemMapper;
import com.yunzhongyi.medical.inspection.mapper.InspectionTemplateItemMapper;
import com.yunzhongyi.medical.inspection.service.InspectionCategoryService;
import com.yunzhongyi.medical.inspection.service.InspectionTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 检查分类管理服务实现：分类 CRUD + 启用/停用 + 统计 + 分类树 + Excel 批量导入导出
 * <p>
 * 关联守卫直接读 InspectionItemMapper / InspectionTemplateItemMapper，
 * 不依赖检查项目服务，避免互相注入形成循环依赖。
 * <p>
 * 口径与模块 19 检查维护一致：检查项目按分类编码归类
 * （InspectionItem.category 存分类编码），模板通过
 * InspectionTemplateItem 组装项目、间接引用分类；分类下有项目
 * （尤其是有项目已被模板引用）时，不可删除、不可停用、不可改编码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionCategoryServiceImpl
        extends ServiceImpl<InspectionCategoryMapper, InspectionCategory>
        implements InspectionCategoryService {

    private final InspectionItemMapper inspectionItemMapper;
    private final InspectionTemplateItemMapper inspectionTemplateItemMapper;

    @Override
    public PageResult<InspectionCategory> pageQuery(InspectionCategoryQuery query) {
        Page<InspectionCategory> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        enrichParentInfo(p.getRecords());
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<InspectionCategory> listByQuery(InspectionCategoryQuery query) {
        List<InspectionCategory> list = this.list(buildWrapper(query));
        enrichParentInfo(list);
        return list;
    }

    @Override
    public Map<String, Object> stats() {
        List<InspectionCategory> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream()
                .filter(c -> Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(c.getStatus())).count());
        r.put("disabled", all.stream()
                .filter(c -> Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_DISABLED).equals(c.getStatus())).count());
        r.put("level1", all.stream().filter(c -> Integer.valueOf(1).equals(c.getLevel())).count());
        r.put("level2", all.stream().filter(c -> Integer.valueOf(2).equals(c.getLevel())).count());
        Map<String, Long> byApplicableType = new LinkedHashMap<>();
        for (String t : List.of(InspectionTypeUtil.APPLY_ALL, InspectionTypeUtil.APPLY_SELF,
                InspectionTypeUtil.APPLY_HOSPITAL, InspectionTypeUtil.APPLY_SPECIAL)) {
            byApplicableType.put(t, all.stream().filter(c -> t.equals(c.getApplicableType())).count());
        }
        r.put("byApplicableType", byApplicableType);
        // 归属检查项目数：按分类编码汇总
        List<InspectionItem> items = inspectionItemMapper.selectList(new LambdaQueryWrapper<>());
        r.put("itemTotal", (long) items.size());
        Set<String> codesWithItems = new HashSet<>();
        for (InspectionItem item : items) {
            codesWithItems.add(item.getCategory());
        }
        r.put("categoriesWithItems", all.stream()
                .filter(c -> codesWithItems.contains(c.getCategoryCode())).count());
        r.put("categoriesWithoutItems", all.stream()
                .filter(c -> !codesWithItems.contains(c.getCategoryCode())).count());
        // 已组装进模板的项目数 / 涉及模板数（去重）
        List<InspectionTemplateItem> links = inspectionTemplateItemMapper.selectList(new LambdaQueryWrapper<>());
        r.put("linkedItems", (long) links.stream().map(InspectionTemplateItem::getItemId).distinct().count());
        r.put("templateCount", (long) links.stream().map(InspectionTemplateItem::getTemplateId).distinct().count());
        r.put("byCategoryList", statsByCategory());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByCategory() {
        List<InspectionCategory> all = this.list();
        List<InspectionItem> items = inspectionItemMapper.selectList(new LambdaQueryWrapper<>());
        List<InspectionTemplateItem> links = inspectionTemplateItemMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Set<Long>> itemIdToTemplateIds = new HashMap<>();
        for (InspectionTemplateItem link : links) {
            itemIdToTemplateIds.computeIfAbsent(link.getItemId(), k -> new HashSet<>())
                    .add(link.getTemplateId());
        }
        Map<String, List<InspectionItem>> itemsByCategory = new HashMap<>();
        for (InspectionItem item : items) {
            itemsByCategory.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (InspectionCategory c : all) {
            List<InspectionItem> mine = itemsByCategory.getOrDefault(c.getCategoryCode(), List.of());
            Set<Long> templateIds = new HashSet<>();
            for (InspectionItem item : mine) {
                templateIds.addAll(itemIdToTemplateIds.getOrDefault(item.getId(), Set.of()));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("categoryCode", c.getCategoryCode());
            row.put("categoryName", c.getCategoryName());
            row.put("parentId", c.getParentId());
            row.put("level", c.getLevel());
            row.put("applicableType", c.getApplicableType());
            row.put("applicableTypeText", InspectionTypeUtil.applicableTypeText(c.getApplicableType()));
            row.put("status", c.getStatus());
            row.put("itemCount", (long) mine.size());
            row.put("enabledItemCount", mine.stream()
                    .filter(i -> Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(i.getStatus()))
                    .count());
            row.put("templateCount", (long) templateIds.size());
            row.put("childCount", this.count(new LambdaQueryWrapper<InspectionCategory>()
                    .eq(InspectionCategory::getParentId, c.getId())));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> tree() {
        List<InspectionCategory> all = this.list(new LambdaQueryWrapper<InspectionCategory>()
                .orderByAsc(InspectionCategory::getSortOrder)
                .orderByAsc(InspectionCategory::getId));
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (InspectionCategory c : all) {
            byId.put(c.getId(), toNode(c));
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (InspectionCategory c : all) {
            Map<String, Object> node = byId.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(node);
            } else {
                Map<String, Object> parent = byId.get(c.getParentId());
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children =
                            (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    // 上级已被删除的孤儿分类，仍挂在根上展示，不丢数据
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    @Override
    public List<InspectionCategory> enabled() {
        List<InspectionCategory> list = this.list(new LambdaQueryWrapper<InspectionCategory>()
                .eq(InspectionCategory::getStatus, InspectionTypeUtil.ITEM_STATUS_ENABLED)
                .orderByAsc(InspectionCategory::getLevel)
                .orderByAsc(InspectionCategory::getSortOrder)
                .orderByAsc(InspectionCategory::getId));
        enrichParentInfo(list);
        return list;
    }

    @Override
    public InspectionCategory detail(Long id) {
        InspectionCategory c = getOrThrow(id);
        enrichParentInfo(List.of(c));
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionCategory enable(Long id) {
        InspectionCategory c = getOrThrow(id);
        if (!Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_DISABLED).equals(c.getStatus())) {
            throw new IllegalStateException("只有停用的分类才能启用（当前状态："
                    + InspectionTypeUtil.itemStatusText(c.getStatus()) + "）");
        }
        if (c.getParentId() != null) {
            InspectionCategory parent = this.getById(c.getParentId());
            if (parent != null
                    && Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_DISABLED).equals(parent.getStatus())) {
                throw new IllegalStateException("上级分类“" + parent.getCategoryName()
                        + "”已停用，请先启用上级分类");
            }
        }
        c.setStatus(InspectionTypeUtil.ITEM_STATUS_ENABLED);
        super.updateById(c);
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionCategory disable(Long id) {
        InspectionCategory c = getOrThrow(id);
        if (!Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(c.getStatus())) {
            throw new IllegalStateException("只有启用的分类才能停用（当前状态："
                    + InspectionTypeUtil.itemStatusText(c.getStatus()) + "）");
        }
        assertNoItems(c, "停用");
        long enabledChildren = this.count(new LambdaQueryWrapper<InspectionCategory>()
                .eq(InspectionCategory::getParentId, id)
                .eq(InspectionCategory::getStatus, InspectionTypeUtil.ITEM_STATUS_ENABLED));
        if (enabledChildren > 0) {
            throw new IllegalStateException("该分类下还有 " + enabledChildren
                    + " 个启用的下级分类，请先停用下级分类");
        }
        c.setStatus(InspectionTypeUtil.ITEM_STATUS_DISABLED);
        super.updateById(c);
        return c;
    }

    @Override
    public void exportExcel(InspectionCategoryQuery query, OutputStream out) {
        InspectionCategoryExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<InspectionCategory> list = InspectionCategoryExcelUtil.read(in);
        int ok = 0;
        for (InspectionCategory c : list) {
            if (!StringUtils.hasText(c.getCategoryName()) || !StringUtils.hasText(c.getCategoryCode())) {
                log.warn("导入跳过：分类名称或分类编码为空的行");
                continue;
            }
            // 上级按编码解析：填了上级编码但查不到，跳过该行（不挂错上级）
            if (StringUtils.hasText(c.getParentCode())) {
                InspectionCategory parent = this.getOne(new LambdaQueryWrapper<InspectionCategory>()
                        .eq(InspectionCategory::getCategoryCode, c.getParentCode().trim()));
                if (parent == null) {
                    log.warn("导入跳过：上级分类编码 {} 不存在（分类 {}）", c.getParentCode(), c.getCategoryName());
                    continue;
                }
                if (!Integer.valueOf(1).equals(parent.getLevel())) {
                    log.warn("导入跳过：上级分类 {} 不是一级分类（分类 {}）", c.getParentCode(), c.getCategoryName());
                    continue;
                }
                c.setParentId(parent.getId());
            }
            if (!InspectionTypeUtil.isValidApplicableType(c.getApplicableType())) {
                log.warn("导入：适用检查类型不正确（分类 {}），默认通用", c.getCategoryName());
                c.setApplicableType(InspectionTypeUtil.APPLY_ALL);
            }
            if (c.getStatus() == null) {
                c.setStatus(InspectionTypeUtil.ITEM_STATUS_ENABLED);
            }
            // 同一分类编码视为重复，跳过
            if (this.count(new LambdaQueryWrapper<InspectionCategory>()
                    .eq(InspectionCategory::getCategoryCode, c.getCategoryCode())) > 0) {
                log.warn("导入跳过：重复分类编码 {}", c.getCategoryCode());
                continue;
            }
            try {
                this.save(c);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", c.getCategoryName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除：已有下级、或已有检查项目归属的分类不能直接删除 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        InspectionCategory c = getOrThrow((Long) id);
        long children = this.count(new LambdaQueryWrapper<InspectionCategory>()
                .eq(InspectionCategory::getParentId, c.getId()));
        if (children > 0) {
            throw new IllegalStateException("该分类下还有 " + children + " 个下级分类，请先删除下级分类");
        }
        assertNoItems(c, "删除");
        return super.removeById(id);
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
    private LambdaQueryWrapper<InspectionCategory> buildWrapper(InspectionCategoryQuery q) {
        LambdaQueryWrapper<InspectionCategory> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(InspectionCategory::getCategoryName, q.getKeyword())
                .or().like(InspectionCategory::getCategoryCode, q.getKeyword()));
        qw.eq(q.getParentId() != null, InspectionCategory::getParentId, q.getParentId());
        qw.eq(q.getLevel() != null, InspectionCategory::getLevel, q.getLevel());
        qw.eq(StringUtils.hasText(q.getApplicableType()), InspectionCategory::getApplicableType, q.getApplicableType());
        qw.eq(q.getStatus() != null, InspectionCategory::getStatus, q.getStatus());
        if (Boolean.TRUE.equals(q.getWithItems())) {
            qw.inSql(InspectionCategory::getCategoryCode,
                    "SELECT DISTINCT category FROM inspection_item WHERE deleted = 0");
        }
        if (Boolean.TRUE.equals(q.getWithoutItems())) {
            qw.notInSql(InspectionCategory::getCategoryCode,
                    "SELECT DISTINCT category FROM inspection_item WHERE deleted = 0");
        }
        qw.orderByAsc(InspectionCategory::getLevel)
                .orderByAsc(InspectionCategory::getSortOrder)
                .orderByDesc(InspectionCategory::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验编码、名称、上级与层级 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(InspectionCategory entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(InspectionCategory entity) {
        InspectionCategory old = getOrThrow(entity.getId());
        normalize(entity, false);
        // 改编码守卫：已有检查项目归属该分类时，编码不可改（项目按编码归类，改了就断链）
        if (entity.getCategoryCode() != null && !entity.getCategoryCode().equals(old.getCategoryCode())) {
            long used = inspectionItemMapper.selectCount(new LambdaQueryWrapper<InspectionItem>()
                    .eq(InspectionItem::getCategory, old.getCategoryCode()));
            if (used > 0) {
                throw new IllegalStateException("该分类已有 " + used
                        + " 个检查项目归属，分类编码不可修改（项目按编码归类）");
            }
        }
        return super.updateById(entity);
    }

    private void normalize(InspectionCategory c, boolean isCreate) {
        if (!StringUtils.hasText(c.getCategoryCode())) {
            throw new IllegalArgumentException("分类编码不能为空");
        }
        if (!StringUtils.hasText(c.getCategoryName())) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        c.setCategoryCode(c.getCategoryCode().trim());
        c.setCategoryName(c.getCategoryName().trim());
        if (!InspectionTypeUtil.isValidApplicableType(c.getApplicableType())) {
            throw new IllegalArgumentException("适用检查类型不正确");
        }
        if (c.getStatus() != null
                && !Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(c.getStatus())
                && !Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_DISABLED).equals(c.getStatus())) {
            throw new IllegalArgumentException("分类状态不正确");
        }
        // 上级校验：存在、一级、不是自己、不是自己的下级（不成环）
        InspectionCategory parent = null;
        if (c.getParentId() != null) {
            if (!isCreate && c.getParentId().equals(c.getId())) {
                throw new IllegalArgumentException("上级分类不能是自己");
            }
            parent = this.getById(c.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("上级分类不存在：" + c.getParentId());
            }
            if (!Integer.valueOf(1).equals(parent.getLevel())) {
                throw new IllegalArgumentException("上级分类必须是 一级分类，不建三级分类");
            }
            if (!isCreate && isDescendantOf(c.getId(), c.getParentId())) {
                throw new IllegalArgumentException("上级分类不能是自己的下级分类");
            }
        }
        // 层级由上级决定，不接受前端传入
        c.setLevel(c.getParentId() == null ? 1 : 2);
        if (isCreate) {
            if (!StringUtils.hasText(c.getApplicableType())) {
                c.setApplicableType(InspectionTypeUtil.APPLY_ALL);
            }
            if (c.getStatus() == null) {
                c.setStatus(InspectionTypeUtil.ITEM_STATUS_ENABLED);
            }
            // 同一分类编码已有记录，拒绝重复建档
            if (this.count(new LambdaQueryWrapper<InspectionCategory>()
                    .eq(InspectionCategory::getCategoryCode, c.getCategoryCode())) > 0) {
                throw new IllegalArgumentException("分类编码 " + c.getCategoryCode() + " 已有记录，请勿重复建档");
            }
            assertNameUnique(c, null);
        } else {
            // 更新时改编码：新编码已被其他分类占用则拒绝
            InspectionCategory dup = this.getOne(new LambdaQueryWrapper<InspectionCategory>()
                    .eq(InspectionCategory::getCategoryCode, c.getCategoryCode())
                    .ne(InspectionCategory::getId, c.getId()));
            if (dup != null) {
                throw new IllegalArgumentException("分类编码 " + c.getCategoryCode() + " 已有记录，请勿重复建档");
            }
            // 已有下级的分类不可改为二级（下级会悬空）
            if (c.getParentId() != null) {
                InspectionCategory old = getOrThrow(c.getId());
                if (old.getParentId() == null) {
                    long children = this.count(new LambdaQueryWrapper<InspectionCategory>()
                            .eq(InspectionCategory::getParentId, c.getId()));
                    if (children > 0) {
                        throw new IllegalStateException("该分类已有 " + children
                                + " 个下级分类，不可改为二级分类");
                    }
                }
            }
            assertNameUnique(c, c.getId());
        }
    }

    /** 同一上级下分类名称不可重复（parentId 相同才算同级；顶级之间 parentId 都为空） */
    private void assertNameUnique(InspectionCategory c, Long excludeId) {
        LambdaQueryWrapper<InspectionCategory> qw = new LambdaQueryWrapper<InspectionCategory>()
                .eq(InspectionCategory::getCategoryName, c.getCategoryName());
        if (c.getParentId() == null) {
            qw.isNull(InspectionCategory::getParentId);
        } else {
            qw.eq(InspectionCategory::getParentId, c.getParentId());
        }
        if (excludeId != null) {
            qw.ne(InspectionCategory::getId, excludeId);
        }
        if (this.count(qw) > 0) {
            throw new IllegalArgumentException("同一上级下已存在同名分类“" + c.getCategoryName() + "”");
        }
    }

    /** candidateId 是否是 categoryId 的下级（沿 parentId 链向上找） */
    private boolean isDescendantOf(Long categoryId, Long candidateId) {
        Long cur = candidateId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            if (cur.equals(categoryId)) {
                return true;
            }
            InspectionCategory c = this.getById(cur);
            cur = c == null ? null : c.getParentId();
        }
        return false;
    }

    /**
     * 归属守卫：分类下已有检查项目时不可删除/停用；
     * 其中有项目已被检查模板引用时，报错里点明模板引用（先从模板里移除项目）。
     */
    private void assertNoItems(InspectionCategory c, String action) {
        List<InspectionItem> items = inspectionItemMapper.selectList(new LambdaQueryWrapper<InspectionItem>()
                .eq(InspectionItem::getCategory, c.getCategoryCode()));
        if (items.isEmpty()) {
            return;
        }
        Set<Long> linkedItemIds = new HashSet<>(inspectionTemplateItemMapper.selectList(
                        new LambdaQueryWrapper<InspectionTemplateItem>()
                                .select(InspectionTemplateItem::getItemId))
                .stream().map(InspectionTemplateItem::getItemId).toList());
        long linked = items.stream().filter(i -> linkedItemIds.contains(i.getId())).count();
        if (linked > 0) {
            throw new IllegalStateException("该分类下有 " + linked
                    + " 个检查项目已被检查模板引用，请先从模板里移除项目再" + action);
        }
        throw new IllegalStateException("该分类下已有 " + items.size()
                + " 个检查项目归属，请先把项目改归其他分类再" + action);
    }

    /** 补上级分类编码/名称（展示与导出用） */
    private void enrichParentInfo(List<InspectionCategory> list) {
        if (list.isEmpty()) {
            return;
        }
        Map<Long, InspectionCategory> parents = new HashMap<>();
        for (InspectionCategory c : list) {
            if (c.getParentId() != null && !parents.containsKey(c.getParentId())) {
                parents.put(c.getParentId(), this.getById(c.getParentId()));
            }
        }
        for (InspectionCategory c : list) {
            if (c.getParentId() != null) {
                InspectionCategory p = parents.get(c.getParentId());
                if (p != null) {
                    c.setParentCode(p.getCategoryCode());
                    c.setParentName(p.getCategoryName());
                }
            }
        }
    }

    private Map<String, Object> toNode(InspectionCategory c) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", c.getId());
        node.put("categoryCode", c.getCategoryCode());
        node.put("categoryName", c.getCategoryName());
        node.put("parentId", c.getParentId());
        node.put("level", c.getLevel());
        node.put("applicableType", c.getApplicableType());
        node.put("applicableTypeText", InspectionTypeUtil.applicableTypeText(c.getApplicableType()));
        node.put("description", c.getDescription());
        node.put("status", c.getStatus());
        node.put("sortOrder", c.getSortOrder());
        node.put("remark", c.getRemark());
        node.put("children", new ArrayList<Map<String, Object>>());
        return node;
    }

    private InspectionCategory getOrThrow(Long id) {
        InspectionCategory c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("检查分类不存在：" + id);
        }
        return c;
    }
}
