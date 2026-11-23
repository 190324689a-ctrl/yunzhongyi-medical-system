package com.yunzhongyi.medical.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.inspection.dto.InspectionItemQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionCategory;
import com.yunzhongyi.medical.inspection.entity.InspectionItem;
import com.yunzhongyi.medical.inspection.entity.InspectionStandard;
import com.yunzhongyi.medical.inspection.entity.InspectionTemplateItem;
import com.yunzhongyi.medical.inspection.excel.InspectionItemExcelUtil;
import com.yunzhongyi.medical.inspection.mapper.InspectionCategoryMapper;
import com.yunzhongyi.medical.inspection.mapper.InspectionItemMapper;
import com.yunzhongyi.medical.inspection.mapper.InspectionStandardMapper;
import com.yunzhongyi.medical.inspection.mapper.InspectionTemplateItemMapper;
import com.yunzhongyi.medical.inspection.service.InspectionItemService;
import com.yunzhongyi.medical.inspection.service.InspectionTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查项目管理服务实现：项目 CRUD + 启用/停用 + 统计 + Excel 批量导入导出
 * <p>
 * 引用守卫直接读 InspectionTemplateItemMapper，不依赖模板配置服务，
 * 避免互相注入形成循环依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionItemServiceImpl
        extends ServiceImpl<InspectionItemMapper, InspectionItem>
        implements InspectionItemService {

    private final InspectionStandardMapper inspectionStandardMapper;
    private final InspectionTemplateItemMapper inspectionTemplateItemMapper;
    private final InspectionCategoryMapper inspectionCategoryMapper;

    @Override
    public PageResult<InspectionItem> pageQuery(InspectionItemQuery query) {
        Page<InspectionItem> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<InspectionItem> listByQuery(InspectionItemQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<InspectionItem> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream()
                .filter(p -> Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(p.getStatus())).count());
        r.put("disabled", all.stream()
                .filter(p -> Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_DISABLED).equals(p.getStatus())).count());
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String c : List.of(InspectionTypeUtil.CATEGORY_MEDICAL_RECORD,
                InspectionTypeUtil.CATEGORY_CORE_SYSTEM, InspectionTypeUtil.CATEGORY_INFECTION,
                InspectionTypeUtil.CATEGORY_PHARMACY, InspectionTypeUtil.CATEGORY_NURSING,
                InspectionTypeUtil.CATEGORY_SAFETY, InspectionTypeUtil.CATEGORY_SERVICE,
                InspectionTypeUtil.CATEGORY_TCM, InspectionTypeUtil.CATEGORY_MGMT,
                InspectionTypeUtil.CATEGORY_OTHER)) {
            byCategory.put(c, all.stream().filter(p -> c.equals(p.getCategory())).count());
        }
        r.put("byCategory", byCategory);
        Map<String, Long> byMethod = new LinkedHashMap<>();
        for (String m : List.of(InspectionTypeUtil.METHOD_ON_SITE,
                InspectionTypeUtil.METHOD_RECORD_REVIEW, InspectionTypeUtil.METHOD_INTERVIEW,
                InspectionTypeUtil.METHOD_DOC_REVIEW)) {
            byMethod.put(m, all.stream().filter(p -> m.equals(p.getCheckMethod())).count());
        }
        r.put("byMethod", byMethod);
        Map<String, Long> byApplicableType = new LinkedHashMap<>();
        for (String t : List.of(InspectionTypeUtil.APPLY_ALL, InspectionTypeUtil.APPLY_SELF,
                InspectionTypeUtil.APPLY_HOSPITAL, InspectionTypeUtil.APPLY_SPECIAL)) {
            byApplicableType.put(t, all.stream().filter(p -> t.equals(p.getApplicableType())).count());
        }
        r.put("byApplicableType", byApplicableType);
        r.put("byCategoryList", statsByCategory());
        long linked = linkedItemIds().size();
        r.put("linked", linked);
        r.put("unlinked", all.size() - linked);
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByCategory() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String c : List.of(InspectionTypeUtil.CATEGORY_MEDICAL_RECORD,
                InspectionTypeUtil.CATEGORY_CORE_SYSTEM, InspectionTypeUtil.CATEGORY_INFECTION,
                InspectionTypeUtil.CATEGORY_PHARMACY, InspectionTypeUtil.CATEGORY_NURSING,
                InspectionTypeUtil.CATEGORY_SAFETY, InspectionTypeUtil.CATEGORY_SERVICE,
                InspectionTypeUtil.CATEGORY_TCM, InspectionTypeUtil.CATEGORY_MGMT,
                InspectionTypeUtil.CATEGORY_OTHER)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", c);
            row.put("categoryText", InspectionTypeUtil.categoryText(c));
            row.put("count", this.count(new LambdaQueryWrapper<InspectionItem>()
                    .eq(InspectionItem::getCategory, c)));
            row.put("enabledCount", this.count(new LambdaQueryWrapper<InspectionItem>()
                    .eq(InspectionItem::getCategory, c)
                    .eq(InspectionItem::getStatus, InspectionTypeUtil.ITEM_STATUS_ENABLED)));
            result.add(row);
        }
        return result;
    }

    @Override
    public InspectionItem detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionItem enable(Long id) {
        InspectionItem p = getOrThrow(id);
        if (!Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_DISABLED).equals(p.getStatus())) {
            throw new IllegalStateException("只有停用的项目才能启用（当前状态："
                    + InspectionTypeUtil.itemStatusText(p.getStatus()) + "）");
        }
        p.setStatus(InspectionTypeUtil.ITEM_STATUS_ENABLED);
        super.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionItem disable(Long id) {
        InspectionItem p = getOrThrow(id);
        if (!Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(p.getStatus())) {
            throw new IllegalStateException("只有启用的项目才能停用（当前状态："
                    + InspectionTypeUtil.itemStatusText(p.getStatus()) + "）");
        }
        if (isLinked(id)) {
            throw new IllegalStateException("该项目已被检查模板引用，请先从模板里移除再停用");
        }
        p.setStatus(InspectionTypeUtil.ITEM_STATUS_DISABLED);
        super.updateById(p);
        return p;
    }

    @Override
    public void exportExcel(InspectionItemQuery query, OutputStream out) {
        InspectionItemExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<InspectionItem> list = InspectionItemExcelUtil.read(in);
        int ok = 0;
        for (InspectionItem p : list) {
            if (!StringUtils.hasText(p.getItemName()) || !StringUtils.hasText(p.getItemNo())) {
                log.warn("导入跳过：项目名称或项目编号为空的行");
                continue;
            }
            if (!StringUtils.hasText(p.getCategory()) || !isUsableCategory(p.getCategory())) {
                log.warn("导入跳过：项目分类为空、不正确或已停用（项目 {}）", p.getItemName());
                continue;
            }
            if (!InspectionTypeUtil.isValidCheckMethod(p.getCheckMethod())) {
                log.warn("导入跳过：检查方式不正确（项目 {}），已置空", p.getItemName());
                p.setCheckMethod(null);
            }
            if (!InspectionTypeUtil.isValidApplicableType(p.getApplicableType())) {
                log.warn("导入跳过：适用检查类型不正确（项目 {}），默认通用", p.getItemName());
                p.setApplicableType(InspectionTypeUtil.APPLY_ALL);
            }
            if (p.getMaxScore() == null || p.getMaxScore() <= 0) {
                p.setMaxScore(100);
            }
            if (p.getWeight() == null || p.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                p.setWeight(BigDecimal.valueOf(10));
            }
            if (p.getStatus() == null) {
                p.setStatus(InspectionTypeUtil.ITEM_STATUS_ENABLED);
            }
            // 依据标准编号关联标准库：找不到则置空关联，不阻断导入
            if (StringUtils.hasText(p.getStandardNo())) {
                InspectionStandard s = inspectionStandardMapper.selectOne(
                        new LambdaQueryWrapper<InspectionStandard>()
                                .eq(InspectionStandard::getStandardNo, p.getStandardNo().trim()));
                if (s == null) {
                    log.warn("导入：依据标准编号 {} 不存在，已置空（项目 {}）", p.getStandardNo(), p.getItemName());
                    p.setStandardId(null);
                    p.setStandardNo(null);
                } else {
                    p.setStandardId(s.getId());
                    p.setStandardNo(s.getStandardNo());
                }
            }
            // 同一项目编号视为重复，跳过
            if (this.count(new LambdaQueryWrapper<InspectionItem>()
                    .eq(InspectionItem::getItemNo, p.getItemNo())) > 0) {
                log.warn("导入跳过：重复项目编号 {}", p.getItemNo());
                continue;
            }
            try {
                this.save(p);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", p.getItemName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除：已被模板引用的项目不能直接删除 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        InspectionItem p = getOrThrow((Long) id);
        if (isLinked(p.getId())) {
            throw new IllegalStateException("该项目已被检查模板引用，请先从模板里移除再删除");
        }
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
    private LambdaQueryWrapper<InspectionItem> buildWrapper(InspectionItemQuery q) {
        LambdaQueryWrapper<InspectionItem> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(InspectionItem::getItemName, q.getKeyword())
                .or().like(InspectionItem::getItemNo, q.getKeyword()));
        qw.eq(StringUtils.hasText(q.getCategory()), InspectionItem::getCategory, q.getCategory());
        qw.eq(StringUtils.hasText(q.getCheckMethod()), InspectionItem::getCheckMethod, q.getCheckMethod());
        qw.eq(q.getStandardId() != null, InspectionItem::getStandardId, q.getStandardId());
        qw.eq(StringUtils.hasText(q.getApplicableType()), InspectionItem::getApplicableType, q.getApplicableType());
        qw.eq(q.getStatus() != null, InspectionItem::getStatus, q.getStatus());
        if (Boolean.TRUE.equals(q.getLinked())) {
            qw.inSql(InspectionItem::getId,
                    "SELECT item_id FROM inspection_template_item WHERE deleted = 0");
        }
        if (Boolean.TRUE.equals(q.getUnlinked())) {
            qw.notInSql(InspectionItem::getId,
                    "SELECT item_id FROM inspection_template_item WHERE deleted = 0");
        }
        qw.orderByAsc(InspectionItem::getSortOrder).orderByDesc(InspectionItem::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验分类、方式、类型、满分与权重 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(InspectionItem entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(InspectionItem entity) {
        getOrThrow(entity.getId());
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(InspectionItem p, boolean isCreate) {
        if (!StringUtils.hasText(p.getCategory())) {
            throw new IllegalArgumentException("项目分类不能为空");
        }
        if (!isUsableCategory(p.getCategory())) {
            // 分类库里有该编码但已停用时，点明原因；否则按原口径报分类不正确
            InspectionCategory cat = inspectionCategoryMapper.selectOne(
                    new LambdaQueryWrapper<InspectionCategory>()
                            .eq(InspectionCategory::getCategoryCode, p.getCategory().trim()));
            if (cat != null) {
                throw new IllegalArgumentException("检查分类“" + cat.getCategoryName() + "”已停用，请先启用分类或更换分类");
            }
            throw new IllegalArgumentException("项目分类不正确");
        }
        if (!InspectionTypeUtil.isValidCheckMethod(p.getCheckMethod())) {
            throw new IllegalArgumentException("检查方式不正确");
        }
        if (!InspectionTypeUtil.isValidApplicableType(p.getApplicableType())) {
            throw new IllegalArgumentException("适用检查类型不正确");
        }
        if (p.getMaxScore() != null && p.getMaxScore() <= 0) {
            throw new IllegalArgumentException("满分必须大于 0");
        }
        if (p.getWeight() != null && p.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("默认权重必须大于 0");
        }
        if (p.getStatus() != null
                && !Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(p.getStatus())
                && !Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_DISABLED).equals(p.getStatus())) {
            throw new IllegalArgumentException("项目状态不正确");
        }
        // 依据标准存在时补全标准编号；不存在则拒绝
        if (p.getStandardId() != null) {
            InspectionStandard s = inspectionStandardMapper.selectById(p.getStandardId());
            if (s == null) {
                throw new IllegalArgumentException("依据标准不存在：" + p.getStandardId());
            }
            p.setStandardNo(s.getStandardNo());
        }
        if (isCreate) {
            if (p.getMaxScore() == null) {
                p.setMaxScore(100);
            }
            if (p.getWeight() == null) {
                p.setWeight(BigDecimal.valueOf(10));
            }
            if (!StringUtils.hasText(p.getApplicableType())) {
                p.setApplicableType(InspectionTypeUtil.APPLY_ALL);
            }
            if (p.getStatus() == null) {
                p.setStatus(InspectionTypeUtil.ITEM_STATUS_ENABLED);
            }
            // 同一项目编号已有记录，拒绝重复建档
            if (this.count(new LambdaQueryWrapper<InspectionItem>()
                    .eq(InspectionItem::getItemNo, p.getItemNo())) > 0) {
                throw new IllegalArgumentException("项目编号 " + p.getItemNo() + " 已有记录，请勿重复建档");
            }
        } else if (p.getItemNo() != null) {
            // 更新时改编号：新编号已被其他项目占用则拒绝
            InspectionItem dup = this.getOne(new LambdaQueryWrapper<InspectionItem>()
                    .eq(InspectionItem::getItemNo, p.getItemNo())
                    .ne(InspectionItem::getId, p.getId()));
            if (dup != null) {
                throw new IllegalArgumentException("项目编号 " + p.getItemNo() + " 已有记录，请勿重复建档");
            }
        }
    }

    /**
     * 分类是否可用：分类库里有该编码的，以库为准（须启用）；
     * 库里没有的，沿用模块 19 的 10 个既有分类编码（见 InspectionTypeUtil）。
     * 这样既有项目不受影响，新建的二级分类（如运行病历）也能直接归项目。
     */
    private boolean isUsableCategory(String code) {
        if (code == null) {
            return false;
        }
        InspectionCategory cat = inspectionCategoryMapper.selectOne(
                new LambdaQueryWrapper<InspectionCategory>()
                        .eq(InspectionCategory::getCategoryCode, code.trim()));
        if (cat != null) {
            return Integer.valueOf(InspectionTypeUtil.ITEM_STATUS_ENABLED).equals(cat.getStatus());
        }
        return InspectionTypeUtil.isValidCategory(code);
    }

    /** 该项目是否已被任一模板引用（未删除的配置记录） */
    private boolean isLinked(Long itemId) {
        return linkedItemIds().contains(itemId);
    }

    private java.util.Set<Long> linkedItemIds() {
        return new java.util.HashSet<>(inspectionTemplateItemMapper.selectList(
                        new LambdaQueryWrapper<InspectionTemplateItem>()
                                .select(InspectionTemplateItem::getItemId))
                .stream()
                .map(InspectionTemplateItem::getItemId)
                .toList());
    }

    private InspectionItem getOrThrow(Long id) {
        InspectionItem p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("检查项目不存在：" + id);
        }
        return p;
    }
}
