package com.yunzhongyi.medical.sysconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.SysNoticeSettingQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysNoticeSetting;
import com.yunzhongyi.medical.sysconfig.excel.SysNoticeSettingExcelUtil;
import com.yunzhongyi.medical.sysconfig.mapper.SysNoticeSettingMapper;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
import com.yunzhongyi.medical.sysconfig.service.SysNoticeSettingService;
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
 * 消息提醒设置服务实现。
 * <p>
 * 关键守卫：设置编码全院唯一；提醒渠道与接收范围须合法；按角色接收时须填角色编码，
 * 按人员接收时须填用户名，全部人员时接收对象须为空。
 */
@Service
@RequiredArgsConstructor
public class SysNoticeSettingServiceImpl
        extends ServiceImpl<SysNoticeSettingMapper, SysNoticeSetting>
        implements SysNoticeSettingService {

    @Override
    public PageResult<SysNoticeSetting> pageQuery(SysNoticeSettingQuery query) {
        Page<SysNoticeSetting> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public SysNoticeSetting detail(Long id) {
        SysNoticeSetting s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("消息提醒设置不存在：" + id);
        }
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNoticeSetting create(SysNoticeSetting setting) {
        validateSetting(setting, null);
        applyDefaults(setting);
        this.save(setting);
        return setting;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysNoticeSetting update(SysNoticeSetting setting) {
        detail(setting.getId());
        validateSetting(setting, setting.getId());
        applyDefaults(setting);
        this.updateById(setting);
        return setting;
    }

    @Override
    public SysNoticeSetting enable(Long id) {
        SysNoticeSetting s = detail(id);
        s.setEnabled(SysConfigTypeUtil.STATUS_ENABLED);
        this.updateById(s);
        return s;
    }

    @Override
    public SysNoticeSetting disable(Long id) {
        SysNoticeSetting s = detail(id);
        s.setEnabled(SysConfigTypeUtil.STATUS_DISABLED);
        this.updateById(s);
        return s;
    }

    @Override
    public List<SysNoticeSetting> enabled() {
        return this.list(new LambdaQueryWrapper<SysNoticeSetting>()
                .eq(SysNoticeSetting::getEnabled, SysConfigTypeUtil.STATUS_ENABLED)
                .orderByAsc(SysNoticeSetting::getSettingKey));
    }

    @Override
    public Map<String, Object> stats() {
        List<SysNoticeSetting> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream()
                .filter(s -> Integer.valueOf(SysConfigTypeUtil.STATUS_ENABLED).equals(s.getEnabled())).count());
        r.put("disabled", all.stream()
                .filter(s -> Integer.valueOf(SysConfigTypeUtil.STATUS_DISABLED).equals(s.getEnabled())).count());
        Map<String, Long> byChannel = new LinkedHashMap<>();
        for (String ch : List.of(SysConfigTypeUtil.CHANNEL_SITE, SysConfigTypeUtil.CHANNEL_SMS,
                SysConfigTypeUtil.CHANNEL_WECHAT, SysConfigTypeUtil.CHANNEL_EMAIL)) {
            byChannel.put(ch, all.stream().filter(s -> ch.equals(s.getChannel())).count());
        }
        r.put("byChannel", byChannel);
        Map<String, Long> byReceiver = new LinkedHashMap<>();
        for (String scope : List.of(SysConfigTypeUtil.RECEIVER_ALL, SysConfigTypeUtil.RECEIVER_ROLE,
                SysConfigTypeUtil.RECEIVER_USER)) {
            byReceiver.put(scope, all.stream().filter(s -> scope.equals(s.getReceiverScope())).count());
        }
        r.put("byReceiverScope", byReceiver);
        return r;
    }

    @Override
    public void exportExcel(SysNoticeSettingQuery query, OutputStream out) {
        SysNoticeSettingExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<SysNoticeSetting> rows = SysNoticeSettingExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (SysNoticeSetting row : rows) {
            try {
                validateSetting(row, null);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<SysNoticeSetting>()
                    .eq(SysNoticeSetting::getSettingKey, row.getSettingKey()));
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
        SysNoticeSettingExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<SysNoticeSetting> buildWrapper(SysNoticeSettingQuery query) {
        LambdaQueryWrapper<SysNoticeSetting> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(SysNoticeSetting::getSettingName, kw)
                    .or().like(SysNoticeSetting::getSettingKey, kw)
                    .or().like(SysNoticeSetting::getTriggerEvent, kw));
        }
        if (StringUtils.hasText(query.getChannel())) {
            w.eq(SysNoticeSetting::getChannel, query.getChannel());
        }
        if (query.getEnabled() != null) {
            w.eq(SysNoticeSetting::getEnabled, query.getEnabled());
        }
        w.orderByAsc(SysNoticeSetting::getSettingKey);
        return w;
    }

    private void applyDefaults(SysNoticeSetting s) {
        if (s.getChannel() == null) {
            s.setChannel(SysConfigTypeUtil.CHANNEL_SITE);
        }
        if (s.getReceiverScope() == null) {
            s.setReceiverScope(SysConfigTypeUtil.RECEIVER_ALL);
        }
        if (SysConfigTypeUtil.RECEIVER_ALL.equals(s.getReceiverScope())) {
            s.setReceiverValue(null);
        }
        if (s.getEnabled() == null) {
            s.setEnabled(SysConfigTypeUtil.STATUS_ENABLED);
        }
    }

    private void validateSetting(SysNoticeSetting s, Long excludeId) {
        if (!StringUtils.hasText(s.getSettingKey())) {
            throw new IllegalArgumentException("设置编码不能为空");
        }
        if (!StringUtils.hasText(s.getSettingName())) {
            throw new IllegalArgumentException("设置名称不能为空");
        }
        if (s.getChannel() != null && !SysConfigTypeUtil.CHANNELS.contains(s.getChannel())) {
            throw new IllegalArgumentException("提醒渠道不合法：" + s.getChannel());
        }
        if (s.getReceiverScope() != null && !SysConfigTypeUtil.RECEIVER_SCOPES.contains(s.getReceiverScope())) {
            throw new IllegalArgumentException("接收范围不合法：" + s.getReceiverScope());
        }
        if (SysConfigTypeUtil.RECEIVER_ROLE.equals(s.getReceiverScope())
                && !StringUtils.hasText(s.getReceiverValue())) {
            throw new IllegalArgumentException("按角色接收时须填写角色编码");
        }
        if (SysConfigTypeUtil.RECEIVER_USER.equals(s.getReceiverScope())
                && !StringUtils.hasText(s.getReceiverValue())) {
            throw new IllegalArgumentException("按人员接收时须填写用户名");
        }
        long dup = this.count(new LambdaQueryWrapper<SysNoticeSetting>()
                .eq(SysNoticeSetting::getSettingKey, s.getSettingKey())
                .ne(excludeId != null, SysNoticeSetting::getId, excludeId));
        if (dup > 0) {
            throw new IllegalArgumentException("设置编码已存在：" + s.getSettingKey());
        }
    }
}
