package com.yunzhongyi.medical.mobile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.mobile.dto.MobileDeviceQuery;
import com.yunzhongyi.medical.mobile.entity.MobileDevice;
import com.yunzhongyi.medical.mobile.excel.MobileDeviceExcelUtil;
import com.yunzhongyi.medical.mobile.mapper.MobileDeviceMapper;
import com.yunzhongyi.medical.mobile.service.MobileDeviceService;
import com.yunzhongyi.medical.mobile.service.MobileTypeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动端设备登记服务实现。
 * <p>
 * 关键守卫：设备编码必填且全院唯一、平台须合法；已绑定的设备不可直接删除，
 * 须先解绑；重复绑定 / 重复解绑拒绝。
 */
@Service
@RequiredArgsConstructor
public class MobileDeviceServiceImpl
        extends ServiceImpl<MobileDeviceMapper, MobileDevice>
        implements MobileDeviceService {

    @Override
    public PageResult<MobileDevice> pageQuery(MobileDeviceQuery query) {
        Page<MobileDevice> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public MobileDevice detail(Long id) {
        MobileDevice d = this.getById(id);
        if (d == null) {
            throw new IllegalArgumentException("移动端设备不存在：" + id);
        }
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileDevice register(MobileDevice device) {
        validateDevice(device, null);
        if (device.getStatus() == null) {
            device.setStatus(MobileTypeUtil.DEVICE_BOUND);
        }
        if (device.getLastActiveTime() == null) {
            device.setLastActiveTime(LocalDateTime.now());
        }
        this.save(device);
        return device;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileDevice update(MobileDevice device) {
        MobileDevice old = detail(device.getId());
        validateDevice(device, device.getId());
        device.setStatus(old.getStatus());
        device.setLastActiveTime(old.getLastActiveTime());
        this.updateById(device);
        return device;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MobileDevice old = detail(id);
        if (Integer.valueOf(MobileTypeUtil.DEVICE_BOUND).equals(old.getStatus())) {
            throw new IllegalArgumentException("已绑定的设备不可直接删除，请先解绑");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileDevice bind(Long id) {
        MobileDevice d = detail(id);
        if (Integer.valueOf(MobileTypeUtil.DEVICE_BOUND).equals(d.getStatus())) {
            throw new IllegalArgumentException("设备已绑定，无需重复绑定");
        }
        d.setStatus(MobileTypeUtil.DEVICE_BOUND);
        d.setLastActiveTime(LocalDateTime.now());
        this.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MobileDevice unbind(Long id) {
        MobileDevice d = detail(id);
        if (Integer.valueOf(MobileTypeUtil.DEVICE_UNBOUND).equals(d.getStatus())) {
            throw new IllegalArgumentException("设备已解绑，无需重复解绑");
        }
        d.setStatus(MobileTypeUtil.DEVICE_UNBOUND);
        this.updateById(d);
        return d;
    }

    @Override
    public Map<String, Object> stats() {
        List<MobileDevice> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("bound", all.stream()
                .filter(d -> Integer.valueOf(MobileTypeUtil.DEVICE_BOUND).equals(d.getStatus())).count());
        r.put("unbound", all.stream()
                .filter(d -> Integer.valueOf(MobileTypeUtil.DEVICE_UNBOUND).equals(d.getStatus())).count());
        Map<String, Long> byPlatform = new LinkedHashMap<>();
        for (String platform : List.of(MobileTypeUtil.PLATFORM_IOS, MobileTypeUtil.PLATFORM_ANDROID)) {
            byPlatform.put(platform, all.stream().filter(d -> platform.equals(d.getPlatform())).count());
        }
        r.put("byPlatform", byPlatform);
        return r;
    }

    @Override
    public void exportExcel(MobileDeviceQuery query, OutputStream out) {
        MobileDeviceExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<MobileDevice> rows = MobileDeviceExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (MobileDevice row : rows) {
            try {
                validateDevice(row, null);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<MobileDevice>()
                    .eq(MobileDevice::getDeviceCode, row.getDeviceCode().trim()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            if (row.getStatus() == null) {
                row.setStatus(MobileTypeUtil.DEVICE_BOUND);
            }
            if (row.getLastActiveTime() == null) {
                row.setLastActiveTime(LocalDateTime.now());
            }
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
        MobileDeviceExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<MobileDevice> buildWrapper(MobileDeviceQuery query) {
        LambdaQueryWrapper<MobileDevice> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(MobileDevice::getDeviceCode, kw)
                    .or().like(MobileDevice::getUsername, kw)
                    .or().like(MobileDevice::getDisplayName, kw)
                    .or().like(MobileDevice::getDeviceModel, kw));
        }
        if (StringUtils.hasText(query.getPlatform())) {
            w.eq(MobileDevice::getPlatform, query.getPlatform());
        }
        if (query.getStatus() != null) {
            w.eq(MobileDevice::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getUsername())) {
            w.eq(MobileDevice::getUsername, query.getUsername());
        }
        w.orderByDesc(MobileDevice::getLastActiveTime).orderByDesc(MobileDevice::getId);
        return w;
    }

    private void validateDevice(MobileDevice d, Long excludeId) {
        if (!StringUtils.hasText(d.getDeviceCode())) {
            throw new IllegalArgumentException("设备编码不能为空");
        }
        if (!StringUtils.hasText(d.getUsername())) {
            throw new IllegalArgumentException("使用人不能为空");
        }
        if (d.getPlatform() != null && !MobileTypeUtil.PLATFORMS.contains(d.getPlatform())) {
            throw new IllegalArgumentException("设备平台不合法：" + d.getPlatform());
        }
        LambdaQueryWrapper<MobileDevice> w = new LambdaQueryWrapper<MobileDevice>()
                .eq(MobileDevice::getDeviceCode, d.getDeviceCode().trim());
        if (excludeId != null) {
            w.ne(MobileDevice::getId, excludeId);
        }
        if (this.count(w) > 0) {
            throw new IllegalArgumentException("设备编码已存在：" + d.getDeviceCode());
        }
    }
}
