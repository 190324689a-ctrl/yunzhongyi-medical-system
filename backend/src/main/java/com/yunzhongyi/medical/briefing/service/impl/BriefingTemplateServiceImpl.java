package com.yunzhongyi.medical.briefing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunzhongyi.medical.briefing.dto.BriefingTemplateQuery;
import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.briefing.entity.BriefingTemplate;
import com.yunzhongyi.medical.briefing.mapper.BriefingMapper;
import com.yunzhongyi.medical.briefing.mapper.BriefingTemplateMapper;
import com.yunzhongyi.medical.briefing.service.BriefingTemplateService;
import com.yunzhongyi.medical.briefing.service.BriefingTypeUtil;
import com.yunzhongyi.medical.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 简报模板服务实现。
 * <p>
 * 关键守卫：名称必填、分类须合法、栏目须为非空 JSON 数组且每项为非空字符串；
 * 同名模板不允许重复；已被简报引用的模板不允许删除；停用的模板不能用来
 * 新建/自动生成简报（由简报服务校验）。
 */
@Service
@RequiredArgsConstructor
public class BriefingTemplateServiceImpl
        extends ServiceImpl<BriefingTemplateMapper, BriefingTemplate>
        implements BriefingTemplateService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BriefingMapper briefingMapper;

    @Override
    public PageResult<BriefingTemplate> pageQuery(BriefingTemplateQuery query) {
        Page<BriefingTemplate> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public BriefingTemplate detail(Long id) {
        BriefingTemplate t = this.getById(id);
        if (t == null) {
            throw new IllegalArgumentException("简报模板不存在：" + id);
        }
        return t;
    }

    @Override
    public List<BriefingTemplate> enabledList() {
        return this.list(new LambdaQueryWrapper<BriefingTemplate>()
                .eq(BriefingTemplate::getStatus, 1)
                .orderByAsc(BriefingTemplate::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BriefingTemplate create(BriefingTemplate template) {
        validateTemplate(template);
        applyDefaults(template);
        long dup = this.count(new LambdaQueryWrapper<BriefingTemplate>()
                .eq(BriefingTemplate::getName, template.getName().trim()));
        if (dup > 0) {
            throw new IllegalArgumentException("模板名称已存在：" + template.getName().trim());
        }
        template.setName(template.getName().trim());
        this.save(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BriefingTemplate update(BriefingTemplate template) {
        BriefingTemplate old = detail(template.getId());
        validateTemplate(template);
        applyDefaults(template);
        long dup = this.count(new LambdaQueryWrapper<BriefingTemplate>()
                .eq(BriefingTemplate::getName, template.getName().trim())
                .ne(BriefingTemplate::getId, template.getId()));
        if (dup > 0) {
            throw new IllegalArgumentException("模板名称已存在：" + template.getName().trim());
        }
        template.setName(template.getName().trim());
        template.setStatus(old.getStatus());
        this.updateById(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        detail(id);
        long used = briefingMapper.selectCount(new LambdaQueryWrapper<Briefing>()
                .eq(Briefing::getTemplateId, id));
        if (used > 0) {
            throw new IllegalArgumentException("该模板已被 " + used + " 期简报引用，不能直接删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BriefingTemplate enable(Long id) {
        BriefingTemplate t = detail(id);
        t.setStatus(1);
        this.updateById(t);
        return t;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BriefingTemplate disable(Long id) {
        BriefingTemplate t = detail(id);
        t.setStatus(0);
        this.updateById(t);
        return t;
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<BriefingTemplate> buildWrapper(BriefingTemplateQuery query) {
        LambdaQueryWrapper<BriefingTemplate> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            w.like(BriefingTemplate::getName, query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getCategory())) {
            w.eq(BriefingTemplate::getCategory, query.getCategory());
        }
        if (query.getStatus() != null) {
            w.eq(BriefingTemplate::getStatus, query.getStatus());
        }
        w.orderByAsc(BriefingTemplate::getId);
        return w;
    }

    private void applyDefaults(BriefingTemplate t) {
        if (t.getCategory() == null) {
            t.setCategory(BriefingTypeUtil.CATEGORY_MONTHLY);
        }
        if (t.getStatus() == null) {
            t.setStatus(1);
        }
        if (!StringUtils.hasText(t.getSections())) {
            t.setSections("[\"工作概况\",\"医疗质量\",\"重点工作\",\"下月计划\"]");
        }
    }

    private void validateTemplate(BriefingTemplate t) {
        if (!StringUtils.hasText(t.getName())) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        if (t.getCategory() != null && !BriefingTypeUtil.CATEGORIES.contains(t.getCategory())) {
            throw new IllegalArgumentException("简报分类不合法：" + t.getCategory());
        }
        if (StringUtils.hasText(t.getSections())) {
            parseSections(t.getSections());
        }
    }

    /** 解析栏目 JSON 数组：须为非空数组且每项为非空字符串 */
    static List<String> parseSections(String sectionsJson) {
        List<String> sections;
        try {
            sections = MAPPER.readValue(sectionsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("栏目配置须为 JSON 数组，如 [\"工作概况\",\"医疗质量\"]");
        }
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("栏目配置不能为空，至少填写一个栏目");
        }
        for (String s : sections) {
            if (s == null || s.isBlank()) {
                throw new IllegalArgumentException("栏目名称不能为空");
            }
        }
        return sections;
    }
}
