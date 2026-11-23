package com.yunzhongyi.medical.sysconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.SysInterfaceQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysInterface;
import com.yunzhongyi.medical.sysconfig.excel.SysInterfaceExcelUtil;
import com.yunzhongyi.medical.sysconfig.mapper.SysInterfaceMapper;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
import com.yunzhongyi.medical.sysconfig.service.SysInterfaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统接口服务实现。
 * <p>
 * 关键守卫：接口编码全院唯一；接口类型与鉴权方式须合法；地址须为 http/https 开头；
 * 超时时间 1-60 秒；只有启用的接口能做连通性测试；测试真实发起 HTTP 请求，
 * 不伪造成功，测试结果与时间回写。
 */
@Service
@RequiredArgsConstructor
public class SysInterfaceServiceImpl
        extends ServiceImpl<SysInterfaceMapper, SysInterface>
        implements SysInterfaceService {

    @Override
    public PageResult<SysInterface> pageQuery(SysInterfaceQuery query) {
        Page<SysInterface> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        p.getRecords().forEach(i -> i.setAuthToken(null));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public SysInterface detail(Long id) {
        SysInterface s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("系统接口不存在：" + id);
        }
        s.setAuthToken(null);
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysInterface create(SysInterface iface) {
        validateInterface(iface, null);
        applyDefaults(iface);
        this.save(iface);
        iface.setAuthToken(null);
        return iface;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysInterface update(SysInterface iface) {
        SysInterface old = this.getById(iface.getId());
        if (old == null) {
            throw new IllegalArgumentException("系统接口不存在：" + iface.getId());
        }
        validateInterface(iface, iface.getId());
        if (!old.getInterfaceCode().equals(iface.getInterfaceCode())) {
            throw new IllegalArgumentException("接口编码不可修改");
        }
        // 不传凭证时沿用原凭证
        if (!StringUtils.hasText(iface.getAuthToken())) {
            iface.setAuthToken(old.getAuthToken());
        }
        applyDefaults(iface);
        this.updateById(iface);
        iface.setAuthToken(null);
        return iface;
    }

    @Override
    public SysInterface enable(Long id) {
        SysInterface s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("系统接口不存在：" + id);
        }
        s.setStatus(SysConfigTypeUtil.STATUS_ENABLED);
        this.updateById(s);
        s.setAuthToken(null);
        return s;
    }

    @Override
    public SysInterface disable(Long id) {
        SysInterface s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("系统接口不存在：" + id);
        }
        s.setStatus(SysConfigTypeUtil.STATUS_DISABLED);
        this.updateById(s);
        s.setAuthToken(null);
        return s;
    }

    @Override
    public List<SysInterface> enabled() {
        List<SysInterface> list = this.list(new LambdaQueryWrapper<SysInterface>()
                .eq(SysInterface::getStatus, SysConfigTypeUtil.STATUS_ENABLED)
                .orderByAsc(SysInterface::getInterfaceCode));
        list.forEach(i -> i.setAuthToken(null));
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> testConnection(Long id) {
        SysInterface s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("系统接口不存在：" + id);
        }
        if (!Integer.valueOf(SysConfigTypeUtil.STATUS_ENABLED).equals(s.getStatus())) {
            throw new IllegalArgumentException("只有启用的接口才能测试连通性");
        }
        int timeoutMs = (s.getTimeoutSeconds() == null ? 5 : s.getTimeoutSeconds()) * 1000;
        boolean success = false;
        String message;
        Integer httpStatus = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(s.getUrl()).openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            httpStatus = conn.getResponseCode();
            if (httpStatus == null || httpStatus == -1) {
                // 未收到响应（如连接被重置、沙箱禁出站 TCP）：如实记为失败，不伪造成功
                success = false;
                message = "连接失败: 未收到响应";
            } else {
                success = httpStatus >= 200 && httpStatus < 400;
                message = "HTTP " + httpStatus;
            }
            conn.disconnect();
        } catch (Exception e) {
            message = "连接失败: " + e.getMessage();
        }
        s.setLastTestTime(LocalDateTime.now());
        s.setLastTestResult(success ? SysConfigTypeUtil.TEST_SUCCESS : SysConfigTypeUtil.TEST_FAIL);
        s.setLastTestMsg(message);
        this.updateById(s);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", success);
        r.put("httpStatus", httpStatus);
        r.put("message", message);
        r.put("testTime", s.getLastTestTime());
        return r;
    }

    @Override
    public Map<String, Object> stats() {
        List<SysInterface> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("enabled", all.stream()
                .filter(s -> Integer.valueOf(SysConfigTypeUtil.STATUS_ENABLED).equals(s.getStatus())).count());
        r.put("disabled", all.stream()
                .filter(s -> Integer.valueOf(SysConfigTypeUtil.STATUS_DISABLED).equals(s.getStatus())).count());
        r.put("testSuccess", all.stream()
                .filter(s -> SysConfigTypeUtil.TEST_SUCCESS.equals(s.getLastTestResult())).count());
        r.put("testFail", all.stream()
                .filter(s -> SysConfigTypeUtil.TEST_FAIL.equals(s.getLastTestResult())).count());
        r.put("untested", all.stream()
                .filter(s -> s.getLastTestResult() == null
                        || SysConfigTypeUtil.TEST_UNTESTED.equals(s.getLastTestResult())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of(SysConfigTypeUtil.IF_HIS, SysConfigTypeUtil.IF_LIS,
                SysConfigTypeUtil.IF_PACS, SysConfigTypeUtil.IF_INSURANCE, SysConfigTypeUtil.IF_OTHER)) {
            byType.put(type, all.stream().filter(s -> type.equals(s.getType())).count());
        }
        r.put("byType", byType);
        return r;
    }

    @Override
    public void exportExcel(SysInterfaceQuery query, OutputStream out) {
        List<SysInterface> list = this.list(buildWrapper(query));
        list.forEach(i -> i.setAuthToken(null));
        SysInterfaceExcelUtil.write(list, out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<SysInterface> rows = SysInterfaceExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (SysInterface row : rows) {
            try {
                validateInterface(row, null);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<SysInterface>()
                    .eq(SysInterface::getInterfaceCode, row.getInterfaceCode()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            applyDefaults(row);
            if (row.getLastTestResult() == null) {
                row.setLastTestResult(SysConfigTypeUtil.TEST_UNTESTED);
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
        SysInterfaceExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<SysInterface> buildWrapper(SysInterfaceQuery query) {
        LambdaQueryWrapper<SysInterface> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(SysInterface::getInterfaceName, kw)
                    .or().like(SysInterface::getInterfaceCode, kw));
        }
        if (StringUtils.hasText(query.getType())) {
            w.eq(SysInterface::getType, query.getType());
        }
        if (query.getStatus() != null) {
            w.eq(SysInterface::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getLastTestResult())) {
            w.eq(SysInterface::getLastTestResult, query.getLastTestResult());
        }
        w.orderByAsc(SysInterface::getInterfaceCode);
        return w;
    }

    private void applyDefaults(SysInterface s) {
        if (s.getType() == null) {
            s.setType(SysConfigTypeUtil.IF_OTHER);
        }
        if (s.getAuthType() == null) {
            s.setAuthType(SysConfigTypeUtil.AUTH_NONE);
        }
        if (s.getStatus() == null) {
            s.setStatus(SysConfigTypeUtil.STATUS_ENABLED);
        }
        if (s.getTimeoutSeconds() == null) {
            s.setTimeoutSeconds(5);
        }
        if (s.getLastTestResult() == null) {
            s.setLastTestResult(SysConfigTypeUtil.TEST_UNTESTED);
        }
    }

    private void validateInterface(SysInterface s, Long excludeId) {
        if (!StringUtils.hasText(s.getInterfaceCode())) {
            throw new IllegalArgumentException("接口编码不能为空");
        }
        if (!StringUtils.hasText(s.getInterfaceName())) {
            throw new IllegalArgumentException("接口名称不能为空");
        }
        if (!StringUtils.hasText(s.getUrl())) {
            throw new IllegalArgumentException("接口地址不能为空");
        }
        if (!s.getUrl().startsWith("http://") && !s.getUrl().startsWith("https://")) {
            throw new IllegalArgumentException("接口地址须为 http/https 开头");
        }
        if (s.getType() != null && !SysConfigTypeUtil.INTERFACE_TYPES.contains(s.getType())) {
            throw new IllegalArgumentException("接口类型不合法：" + s.getType());
        }
        if (s.getAuthType() != null && !SysConfigTypeUtil.AUTH_TYPES.contains(s.getAuthType())) {
            throw new IllegalArgumentException("鉴权方式不合法：" + s.getAuthType());
        }
        if (s.getTimeoutSeconds() != null && (s.getTimeoutSeconds() < 1 || s.getTimeoutSeconds() > 60)) {
            throw new IllegalArgumentException("超时时间须在 1-60 秒之间");
        }
        long dup = this.count(new LambdaQueryWrapper<SysInterface>()
                .eq(SysInterface::getInterfaceCode, s.getInterfaceCode())
                .ne(excludeId != null, SysInterface::getId, excludeId));
        if (dup > 0) {
            throw new IllegalArgumentException("接口编码已存在：" + s.getInterfaceCode());
        }
    }
}
