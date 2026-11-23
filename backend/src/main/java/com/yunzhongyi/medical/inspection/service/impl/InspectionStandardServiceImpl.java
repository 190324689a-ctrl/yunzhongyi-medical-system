package com.yunzhongyi.medical.inspection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.inspection.dto.InspectionStandardQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionItem;
import com.yunzhongyi.medical.inspection.entity.InspectionStandard;
import com.yunzhongyi.medical.inspection.excel.InspectionStandardExcelUtil;
import com.yunzhongyi.medical.inspection.mapper.InspectionItemMapper;
import com.yunzhongyi.medical.inspection.mapper.InspectionStandardMapper;
import com.yunzhongyi.medical.inspection.service.InspectionStandardService;
import com.yunzhongyi.medical.inspection.service.InspectionTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 检查标准管理服务实现：标准 CRUD + 废止/恢复现行 + 统计 + Excel 批量导入导出
 * <p>
 * 引用守卫直接读 InspectionItemMapper，不依赖检查项目服务，
 * 避免互相注入形成循环依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionStandardServiceImpl
        extends ServiceImpl<InspectionStandardMapper, InspectionStandard>
        implements InspectionStandardService {

    private final InspectionItemMapper inspectionItemMapper;

    @Override
    public PageResult<InspectionStandard> pageQuery(InspectionStandardQuery query) {
        Page<InspectionStandard> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<InspectionStandard> listByQuery(InspectionStandardQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<InspectionStandard> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("current", all.stream()
                .filter(p -> Integer.valueOf(InspectionTypeUtil.STANDARD_STATUS_CURRENT).equals(p.getStatus())).count());
        r.put("abolished", all.stream()
                .filter(p -> Integer.valueOf(InspectionTypeUtil.STANDARD_STATUS_ABOLISHED).equals(p.getStatus())).count());
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (String l : List.of(InspectionTypeUtil.LEVEL_NATIONAL, InspectionTypeUtil.LEVEL_PROVINCE,
                InspectionTypeUtil.LEVEL_CITY, InspectionTypeUtil.LEVEL_HOSPITAL)) {
            byLevel.put(l, all.stream().filter(p -> l.equals(p.getLevel())).count());
        }
        r.put("byLevel", byLevel);
        r.put("byLevelList", statsByLevel());
        r.put("expiringSoon", all.stream()
                .filter(p -> InspectionTypeUtil.isExpiringSoon(p.getStatus(), p.getExpiryDate())).count());
        r.put("expired", all.stream()
                .filter(p -> InspectionTypeUtil.isExpired(p.getStatus(), p.getExpiryDate())).count());
        // 被检查项目引用的标准数（去重）
        long referenced = inspectionItemMapper.selectList(new LambdaQueryWrapper<InspectionItem>()
                        .select(InspectionItem::getStandardId)
                        .isNotNull(InspectionItem::getStandardId))
                .stream().map(InspectionItem::getStandardId).distinct().count();
        r.put("referenced", referenced);
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByLevel() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String l : List.of(InspectionTypeUtil.LEVEL_NATIONAL, InspectionTypeUtil.LEVEL_PROVINCE,
                InspectionTypeUtil.LEVEL_CITY, InspectionTypeUtil.LEVEL_HOSPITAL)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("level", l);
            row.put("levelText", InspectionTypeUtil.levelText(l));
            row.put("count", this.count(new LambdaQueryWrapper<InspectionStandard>()
                    .eq(InspectionStandard::getLevel, l)));
            row.put("currentCount", this.count(new LambdaQueryWrapper<InspectionStandard>()
                    .eq(InspectionStandard::getLevel, l)
                    .eq(InspectionStandard::getStatus, InspectionTypeUtil.STANDARD_STATUS_CURRENT)));
            result.add(row);
        }
        return result;
    }

    @Override
    public InspectionStandard detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionStandard abolish(Long id, String reason) {
        InspectionStandard p = getOrThrow(id);
        if (!Integer.valueOf(InspectionTypeUtil.STANDARD_STATUS_CURRENT).equals(p.getStatus())) {
            throw new IllegalStateException("只有现行的标准才能废止（当前状态："
                    + InspectionTypeUtil.standardStatusText(p.getStatus()) + "）");
        }
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("废止原因不能为空");
        }
        p.setStatus(InspectionTypeUtil.STANDARD_STATUS_ABOLISHED);
        p.setAbolishReason(reason.trim());
        super.updateById(p);
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionStandard restore(Long id) {
        InspectionStandard p = getOrThrow(id);
        if (!Integer.valueOf(InspectionTypeUtil.STANDARD_STATUS_ABOLISHED).equals(p.getStatus())) {
            throw new IllegalStateException("只有已废止的标准才能恢复现行（当前状态："
                    + InspectionTypeUtil.standardStatusText(p.getStatus()) + "）");
        }
        p.setStatus(InspectionTypeUtil.STANDARD_STATUS_CURRENT);
        p.setAbolishReason(null);
        super.updateById(p);
        return p;
    }

    @Override
    public void exportExcel(InspectionStandardQuery query, OutputStream out) {
        InspectionStandardExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<InspectionStandard> list = InspectionStandardExcelUtil.read(in);
        int ok = 0;
        for (InspectionStandard p : list) {
            if (!StringUtils.hasText(p.getStandardName()) || !StringUtils.hasText(p.getStandardNo())) {
                log.warn("导入跳过：标准名称或标准编号为空的行");
                continue;
            }
            if (!StringUtils.hasText(p.getLevel()) || !InspectionTypeUtil.isValidLevel(p.getLevel())) {
                log.warn("导入跳过：标准级别为空或不正确（标准 {}）", p.getStandardName());
                continue;
            }
            if (p.getEffectiveDate() != null && p.getExpiryDate() != null
                    && p.getExpiryDate().isBefore(p.getEffectiveDate())) {
                log.warn("导入跳过：有效期早于生效日期（标准 {}）", p.getStandardName());
                continue;
            }
            if (p.getStatus() == null) {
                p.setStatus(InspectionTypeUtil.STANDARD_STATUS_CURRENT);
            }
            // 同一标准编号视为重复，跳过
            if (this.count(new LambdaQueryWrapper<InspectionStandard>()
                    .eq(InspectionStandard::getStandardNo, p.getStandardNo())) > 0) {
                log.warn("导入跳过：重复标准编号 {}", p.getStandardNo());
                continue;
            }
            try {
                this.save(p);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", p.getStandardName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除：已被检查项目引用的标准不能直接删除 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        InspectionStandard p = getOrThrow((Long) id);
        long used = inspectionItemMapper.selectCount(new LambdaQueryWrapper<InspectionItem>()
                .eq(InspectionItem::getStandardId, p.getId()));
        if (used > 0) {
            throw new IllegalStateException("该标准已被 " + used + " 个检查项目引用，请先解除项目关联再删除");
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
    private LambdaQueryWrapper<InspectionStandard> buildWrapper(InspectionStandardQuery q) {
        LambdaQueryWrapper<InspectionStandard> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(InspectionStandard::getStandardName, q.getKeyword())
                .or().like(InspectionStandard::getStandardNo, q.getKeyword())
                .or().like(InspectionStandard::getSourceDoc, q.getKeyword()));
        qw.eq(StringUtils.hasText(q.getLevel()), InspectionStandard::getLevel, q.getLevel());
        qw.eq(q.getStatus() != null, InspectionStandard::getStatus, q.getStatus());
        if (Boolean.TRUE.equals(q.getExpiringSoon())) {
            // 30 天内到期：只看现行的，有效期在今天 ~ 今天 + 30 天之间
            qw.eq(InspectionStandard::getStatus, InspectionTypeUtil.STANDARD_STATUS_CURRENT);
            qw.ge(InspectionStandard::getExpiryDate, LocalDate.now());
            qw.le(InspectionStandard::getExpiryDate, LocalDate.now().plusDays(InspectionTypeUtil.EXPIRY_WARN_DAYS));
        }
        if (Boolean.TRUE.equals(q.getExpired())) {
            // 已过期：只看现行的，有效期已过当天
            qw.eq(InspectionStandard::getStatus, InspectionTypeUtil.STANDARD_STATUS_CURRENT);
            qw.lt(InspectionStandard::getExpiryDate, LocalDate.now());
        }
        qw.orderByDesc(InspectionStandard::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验级别与日期 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(InspectionStandard entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(InspectionStandard entity) {
        getOrThrow(entity.getId());
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(InspectionStandard p, boolean isCreate) {
        if (!StringUtils.hasText(p.getLevel()) || !InspectionTypeUtil.isValidLevel(p.getLevel())) {
            throw new IllegalArgumentException("标准级别不正确");
        }
        if (p.getEffectiveDate() != null && p.getExpiryDate() != null
                && p.getExpiryDate().isBefore(p.getEffectiveDate())) {
            throw new IllegalArgumentException("有效期不能早于生效日期");
        }
        if (p.getStatus() != null
                && !Integer.valueOf(InspectionTypeUtil.STANDARD_STATUS_CURRENT).equals(p.getStatus())
                && !Integer.valueOf(InspectionTypeUtil.STANDARD_STATUS_ABOLISHED).equals(p.getStatus())) {
            throw new IllegalArgumentException("标准状态不正确");
        }
        if (isCreate) {
            if (p.getStatus() == null) {
                p.setStatus(InspectionTypeUtil.STANDARD_STATUS_CURRENT);
            }
            // 同一标准编号已有记录，拒绝重复建档
            if (this.count(new LambdaQueryWrapper<InspectionStandard>()
                    .eq(InspectionStandard::getStandardNo, p.getStandardNo())) > 0) {
                throw new IllegalArgumentException("标准编号 " + p.getStandardNo() + " 已有记录，请勿重复建档");
            }
        } else if (p.getStandardNo() != null) {
            // 更新时改编号：新编号已被其他标准占用则拒绝
            InspectionStandard dup = this.getOne(new LambdaQueryWrapper<InspectionStandard>()
                    .eq(InspectionStandard::getStandardNo, p.getStandardNo())
                    .ne(InspectionStandard::getId, p.getId()));
            if (dup != null) {
                throw new IllegalArgumentException("标准编号 " + p.getStandardNo() + " 已有记录，请勿重复建档");
            }
        }
    }

    private InspectionStandard getOrThrow(Long id) {
        InspectionStandard p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("检查标准不存在：" + id);
        }
        return p;
    }
}
