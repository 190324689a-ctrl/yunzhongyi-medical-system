package com.yunzhongyi.medical.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.schedule.dto.ScheduleTemplateQuery;
import com.yunzhongyi.medical.schedule.entity.ScheduleTemplate;
import com.yunzhongyi.medical.schedule.mapper.ScheduleTemplateMapper;
import com.yunzhongyi.medical.schedule.service.ScheduleShiftUtil;
import com.yunzhongyi.medical.schedule.service.ScheduleTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 排班模板服务实现：自定义模板 + 班次组合校验
 */
@Service
@RequiredArgsConstructor
public class ScheduleTemplateServiceImpl
        extends ServiceImpl<ScheduleTemplateMapper, ScheduleTemplate>
        implements ScheduleTemplateService {

    @Override
    public PageResult<ScheduleTemplate> pageQuery(ScheduleTemplateQuery query) {
        LambdaQueryWrapper<ScheduleTemplate> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            qw.like(ScheduleTemplate::getTemplateName, query.getKeyword());
        }
        qw.eq(query.getDeptId() != null, ScheduleTemplate::getDeptId, query.getDeptId());
        qw.eq(StringUtils.hasText(query.getScheduleType()), ScheduleTemplate::getScheduleType, query.getScheduleType());
        qw.eq(query.getStatus() != null, ScheduleTemplate::getStatus, query.getStatus());
        qw.orderByDesc(ScheduleTemplate::getId);
        Page<ScheduleTemplate> p = this.page(new Page<>(query.getPage(), query.getSize()), qw);
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<ScheduleTemplate> listEnabled() {
        return this.list(new LambdaQueryWrapper<ScheduleTemplate>()
                .eq(ScheduleTemplate::getStatus, 1)
                .orderByDesc(ScheduleTemplate::getId));
    }

    /** 新建默认启用；保存/更新时校验班次组合 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ScheduleTemplate entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        validateShiftPattern(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(ScheduleTemplate entity) {
        validateShiftPattern(entity);
        return super.updateById(entity);
    }

    private void validateShiftPattern(ScheduleTemplate t) {
        if (!StringUtils.hasText(t.getScheduleType())
                || !ScheduleShiftUtil.isValidType(t.getScheduleType())) {
            throw new IllegalArgumentException("排班类型不正确");
        }
        if (!StringUtils.hasText(t.getShiftPattern())) {
            throw new IllegalArgumentException("班次组合不能为空，至少包含一个星期几的班次");
        }
        List<Map<String, Object>> items = ScheduleShiftUtil.parseShiftPattern(t.getShiftPattern());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("班次组合格式不正确，应为 JSON 数组，如 [{\"dayOfWeek\":1,\"shiftCode\":\"DAY\"}]");
        }
        Set<Integer> seenDays = new HashSet<>();
        for (Map<String, Object> item : items) {
            Object dow = item.get("dayOfWeek");
            Object shift = item.get("shiftCode");
            if (dow == null || shift == null) {
                throw new IllegalArgumentException("班次组合每项都必须包含 dayOfWeek（1-7）和 shiftCode");
            }
            int day;
            try {
                day = Integer.parseInt(String.valueOf(dow));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("班次组合 dayOfWeek 必须是 1-7 的数字");
            }
            if (day < 1 || day > 7) {
                throw new IllegalArgumentException("班次组合 dayOfWeek 必须在 1-7 之间（1=周一，7=周日）");
            }
            if (!seenDays.add(day)) {
                throw new IllegalArgumentException("班次组合中星期 " + day + " 重复出现");
            }
            if (!ScheduleShiftUtil.isValidShift(String.valueOf(shift))) {
                throw new IllegalArgumentException("班次组合中班次「" + shift + "」不正确");
            }
        }
    }
}
