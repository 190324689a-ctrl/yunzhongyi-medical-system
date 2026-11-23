package com.yunzhongyi.medical.templatelib.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.quality.entity.QualityPlan;
import com.yunzhongyi.medical.quality.entity.QualityRecord;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.quality.mapper.QualityPlanMapper;
import com.yunzhongyi.medical.quality.mapper.QualityRecordMapper;
import com.yunzhongyi.medical.quality.mapper.QualityTemplateMapper;
import com.yunzhongyi.medical.quality.service.QualityTemplateService;
import com.yunzhongyi.medical.quality.service.QualityTypeUtil;
import com.yunzhongyi.medical.templatelib.entity.TemplateVersion;
import com.yunzhongyi.medical.templatelib.excel.TemplateLibraryExcelUtil;
import com.yunzhongyi.medical.templatelib.mapper.TemplateVersionMapper;
import com.yunzhongyi.medical.templatelib.service.TemplateLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查模板管理（模板库）服务实现。
 * <p>
 * 关键守卫：检查记录逐项打分时按模板当前检查项权重校验上限，因此模板有
 * 待执行 / 进行中计划时不允许改版（新建版本 / 恢复历史版本），避免进行中的
 * 检查按新权重打分；复制、统计、导入导出不受此限。
 */
@Service
@RequiredArgsConstructor
public class TemplateLibraryServiceImpl
        extends ServiceImpl<TemplateVersionMapper, TemplateVersion>
        implements TemplateLibraryService {

    private final QualityTemplateMapper templateMapper;
    private final QualityTemplateService templateService;
    private final QualityPlanMapper planMapper;
    private final QualityRecordMapper recordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateVersion createVersion(Long templateId, String checkItems, String changeRemark) {
        QualityTemplate template = getTemplateOrThrow(templateId);
        validateCheckItems(checkItems);
        assertNoActivePlan(templateId, "新建版本");
        return saveNewVersion(template, checkItems, changeRemark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TemplateVersion restoreVersion(Long versionId, String changeRemark) {
        TemplateVersion version = this.getById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("模板版本不存在：" + versionId);
        }
        QualityTemplate template = getTemplateOrThrow(version.getTemplateId());
        validateCheckItems(version.getCheckItems());
        assertNoActivePlan(template.getId(), "恢复历史版本");
        String remark = StringUtils.hasText(changeRemark)
                ? changeRemark
                : "从 " + version.getVersionNo() + " 恢复";
        return saveNewVersion(template, version.getCheckItems(), remark);
    }

    @Override
    public List<TemplateVersion> listVersions(Long templateId) {
        getTemplateOrThrow(templateId);
        return this.list(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, templateId)
                .orderByDesc(TemplateVersion::getId));
    }

    @Override
    public TemplateVersion currentVersion(Long templateId) {
        return this.getOne(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, templateId)
                .eq(TemplateVersion::getStatus, 1)
                .orderByDesc(TemplateVersion::getId)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QualityTemplate copyTemplate(Long templateId, String newName) {
        QualityTemplate source = getTemplateOrThrow(templateId);
        if (!StringUtils.hasText(newName)) {
            throw new IllegalArgumentException("新模板名称不能为空");
        }
        String name = newName.trim();
        long dup = templateService.count(new LambdaQueryWrapper<QualityTemplate>()
                .eq(QualityTemplate::getTemplateName, name)
                .eq(QualityTemplate::getInspectType, source.getInspectType()));
        if (dup > 0) {
            throw new IllegalArgumentException("同检查类型下已存在同名模板：" + name);
        }
        QualityTemplate copy = new QualityTemplate();
        copy.setTemplateName(name);
        copy.setInspectType(source.getInspectType());
        copy.setCheckItems(source.getCheckItems());
        copy.setStatus(0);
        copy.setRemark("复制自「" + source.getTemplateName() + "」");
        templateService.save(copy);
        return copy;
    }

    @Override
    public Map<String, Object> usage(Long templateId) {
        QualityTemplate template = getTemplateOrThrow(templateId);
        List<QualityPlan> plans = planMapper.selectList(new LambdaQueryWrapper<QualityPlan>()
                .eq(QualityPlan::getTemplateId, templateId)
                .orderByDesc(QualityPlan::getPlanDate));
        long recordCount = recordMapper.selectCount(new LambdaQueryWrapper<QualityRecord>()
                .eq(QualityRecord::getTemplateId, templateId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getTemplateName());
        result.put("inspectType", template.getInspectType());
        result.put("planTotal", plans.size());
        result.put("planPending", plans.stream().filter(p -> Integer.valueOf(0).equals(p.getStatus())).count());
        result.put("planInProgress", plans.stream().filter(p -> Integer.valueOf(1).equals(p.getStatus())).count());
        result.put("planCompleted", plans.stream().filter(p -> Integer.valueOf(2).equals(p.getStatus())).count());
        result.put("planCancelled", plans.stream().filter(p -> Integer.valueOf(3).equals(p.getStatus())).count());
        result.put("hasActivePlan", plans.stream().anyMatch(p -> Integer.valueOf(0).equals(p.getStatus())
                || Integer.valueOf(1).equals(p.getStatus())));
        result.put("recordTotal", recordCount);
        result.put("lastPlanDate", plans.isEmpty() ? null : plans.get(0).getPlanDate());
        return result;
    }

    @Override
    public Map<String, Object> stats() {
        List<QualityTemplate> templates = templateService.list();
        List<TemplateVersion> versions = this.list();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateTotal", templates.size());
        result.put("enabled", templates.stream().filter(t -> Integer.valueOf(1).equals(t.getStatus())).count());
        result.put("disabled", templates.stream().filter(t -> Integer.valueOf(0).equals(t.getStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("SELF", templates.stream().filter(t -> "SELF".equals(t.getInspectType())).count());
        byType.put("HOSPITAL", templates.stream().filter(t -> "HOSPITAL".equals(t.getInspectType())).count());
        byType.put("SPECIAL", templates.stream().filter(t -> "SPECIAL".equals(t.getInspectType())).count());
        result.put("byType", byType);
        result.put("versionTotal", versions.size());
        long withVersions = templates.stream()
                .filter(t -> versions.stream().anyMatch(v -> v.getTemplateId().equals(t.getId())))
                .count();
        result.put("templatesWithVersions", withVersions);
        result.put("templatesWithoutVersions", templates.size() - withVersions);
        // 平均每模板检查项数
        int itemTotal = 0;
        for (QualityTemplate t : templates) {
            itemTotal += QualityTypeUtil.parseCheckItems(t.getCheckItems()).size();
        }
        BigDecimal avgItems = templates.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(itemTotal).divide(BigDecimal.valueOf(templates.size()), 2, RoundingMode.HALF_UP);
        result.put("avgItems", avgItems);
        // 最常用模板（按计划数）与从未使用数
        String mostUsedName = null;
        long mostUsedPlans = 0;
        long neverUsed = 0;
        for (QualityTemplate t : templates) {
            long planCount = planMapper.selectCount(new LambdaQueryWrapper<QualityPlan>()
                    .eq(QualityPlan::getTemplateId, t.getId()));
            if (planCount == 0) {
                neverUsed++;
            }
            if (planCount > mostUsedPlans) {
                mostUsedPlans = planCount;
                mostUsedName = t.getTemplateName();
            }
        }
        result.put("mostUsedTemplate", mostUsedName);
        result.put("mostUsedPlanCount", mostUsedPlans);
        result.put("neverUsed", neverUsed);
        return result;
    }

    @Override
    public void exportTemplates(OutputStream out) {
        TemplateLibraryExcelUtil.write(templateService.list(
                new LambdaQueryWrapper<QualityTemplate>().orderByDesc(QualityTemplate::getId)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importTemplates(InputStream in) {
        List<QualityTemplate> rows = TemplateLibraryExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (QualityTemplate row : rows) {
            try {
                validateTemplateRow(row);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = templateService.count(new LambdaQueryWrapper<QualityTemplate>()
                    .eq(QualityTemplate::getTemplateName, row.getTemplateName())
                    .eq(QualityTemplate::getInspectType, row.getInspectType()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            if (row.getStatus() == null) {
                row.setStatus(1);
            }
            templateService.save(row);
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
        TemplateLibraryExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    /** 追加一个新版本：旧当前版本改为历史，模板检查项回写为新内容 */
    private TemplateVersion saveNewVersion(QualityTemplate template, String checkItems, String changeRemark) {
        String nextNo = nextVersionNo(template.getId());
        List<TemplateVersion> current = this.list(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, template.getId())
                .eq(TemplateVersion::getStatus, 1));
        for (TemplateVersion old : current) {
            old.setStatus(0);
            this.updateById(old);
        }
        TemplateVersion version = new TemplateVersion();
        version.setTemplateId(template.getId());
        version.setTemplateName(template.getTemplateName());
        version.setVersionNo(nextNo);
        version.setCheckItems(checkItems);
        version.setChangeRemark(changeRemark);
        version.setStatus(1);
        this.save(version);
        template.setCheckItems(checkItems);
        templateService.updateById(template);
        return version;
    }

    /** 下一个版本号：已有最大版本号 +1，无版本记录时为 V1.0 */
    private String nextVersionNo(Long templateId) {
        List<TemplateVersion> existing = this.list(new LambdaQueryWrapper<TemplateVersion>()
                .eq(TemplateVersion::getTemplateId, templateId));
        int max = 0;
        for (TemplateVersion v : existing) {
            String no = v.getVersionNo();
            if (no != null && no.matches("V\\d+\\.0")) {
                try {
                    max = Math.max(max, Integer.parseInt(no.substring(1, no.indexOf('.'))));
                } catch (NumberFormatException ignored) {
                    // 非标准版本号忽略
                }
            }
        }
        return "V" + (max + 1) + ".0";
    }

    /** 检查项校验：与模板保存同一口径（至少一项、有名称、正数权重、权重之和为 100） */
    private void validateCheckItems(String checkItems) {
        List<Map<String, Object>> items = QualityTypeUtil.parseCheckItems(checkItems);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("检查项不能为空，至少填写一项");
        }
        for (Map<String, Object> item : items) {
            Object name = item.get("name");
            Object weight = item.get("weight");
            if (name == null || String.valueOf(name).isBlank()) {
                throw new IllegalArgumentException("检查项名称不能为空");
            }
            if (weight == null) {
                throw new IllegalArgumentException("检查项「" + name + "」权重不能为空");
            }
            try {
                BigDecimal w = new BigDecimal(String.valueOf(weight));
                if (w.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("检查项「" + name + "」权重必须大于 0");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("检查项「" + name + "」权重不是数字");
            }
        }
        BigDecimal sum = QualityTypeUtil.sumWeights(checkItems);
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("检查项权重之和必须为 100，当前为 "
                    + sum.stripTrailingZeros().toPlainString());
        }
    }

    /** 导入行校验：名称、检查类型、检查项权重之和 */
    private void validateTemplateRow(QualityTemplate row) {
        if (!StringUtils.hasText(row.getTemplateName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        if (!QualityTypeUtil.isValidType(row.getInspectType())) {
            throw new IllegalArgumentException("检查类型不正确：" + row.getInspectType());
        }
        validateCheckItems(row.getCheckItems());
    }

    /** 有待执行 / 进行中计划时不允许改版 */
    private void assertNoActivePlan(Long templateId, String action) {
        long active = planMapper.selectCount(new LambdaQueryWrapper<QualityPlan>()
                .eq(QualityPlan::getTemplateId, templateId)
                .in(QualityPlan::getStatus, QualityTypeUtil.PLAN_PENDING, QualityTypeUtil.PLAN_IN_PROGRESS));
        if (active > 0) {
            throw new IllegalArgumentException("该模板有 " + active + " 个进行中的检查计划，不能" + action
                    + "（请先完成或取消相关计划）");
        }
    }

    private QualityTemplate getTemplateOrThrow(Long templateId) {
        QualityTemplate t = templateMapper.selectById(templateId);
        if (t == null) {
            throw new IllegalArgumentException("检查模板不存在：" + templateId);
        }
        return t;
    }
}
