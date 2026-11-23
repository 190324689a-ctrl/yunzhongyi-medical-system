package com.yunzhongyi.medical.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.notification.dto.SiteMessageQuery;
import com.yunzhongyi.medical.notification.entity.SiteMessage;
import com.yunzhongyi.medical.notification.excel.SiteMessageExcelUtil;
import com.yunzhongyi.medical.notification.mapper.SiteMessageMapper;
import com.yunzhongyi.medical.notification.service.NotificationTypeUtil;
import com.yunzhongyi.medical.notification.service.SiteMessageService;
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
 * 站内消息服务实现。
 * <p>
 * 关键守卫：标题必填、分类与接收范围须合法；按角色接收须填角色编码，
 * 按人员接收须填用户名，全部人员时接收对象强制清空；已读消息不可修改、
 * 不可重复标已读。
 */
@Service
@RequiredArgsConstructor
public class SiteMessageServiceImpl
        extends ServiceImpl<SiteMessageMapper, SiteMessage>
        implements SiteMessageService {

    @Override
    public PageResult<SiteMessage> pageQuery(SiteMessageQuery query) {
        Page<SiteMessage> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public SiteMessage detail(Long id) {
        SiteMessage m = this.getById(id);
        if (m == null) {
            throw new IllegalArgumentException("站内消息不存在：" + id);
        }
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SiteMessage create(SiteMessage message) {
        validateMessage(message);
        applyDefaults(message);
        message.setReadStatus(NotificationTypeUtil.READ_UNREAD);
        message.setReadTime(null);
        if (message.getSendTime() == null) {
            message.setSendTime(LocalDateTime.now());
        }
        this.save(message);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SiteMessage update(SiteMessage message) {
        SiteMessage old = detail(message.getId());
        if (Integer.valueOf(NotificationTypeUtil.READ_READ).equals(old.getReadStatus())) {
            throw new IllegalArgumentException("已读消息不可修改");
        }
        validateMessage(message);
        applyDefaults(message);
        message.setReadStatus(old.getReadStatus());
        message.setReadTime(old.getReadTime());
        message.setSendTime(old.getSendTime());
        this.updateById(message);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        detail(id);
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SiteMessage markRead(Long id) {
        SiteMessage m = detail(id);
        if (Integer.valueOf(NotificationTypeUtil.READ_READ).equals(m.getReadStatus())) {
            throw new IllegalArgumentException("消息已读，无需重复标记");
        }
        m.setReadStatus(NotificationTypeUtil.READ_READ);
        m.setReadTime(LocalDateTime.now());
        this.updateById(m);
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long markAllRead(String username, String roleCode) {
        List<SiteMessage> unread = this.list(visibleWrapper(username, roleCode)
                .eq(SiteMessage::getReadStatus, NotificationTypeUtil.READ_UNREAD));
        LocalDateTime now = LocalDateTime.now();
        for (SiteMessage m : unread) {
            m.setReadStatus(NotificationTypeUtil.READ_READ);
            m.setReadTime(now);
            this.updateById(m);
        }
        return unread.size();
    }

    @Override
    public long unreadCount(String username, String roleCode) {
        return this.count(visibleWrapper(username, roleCode)
                .eq(SiteMessage::getReadStatus, NotificationTypeUtil.READ_UNREAD));
    }

    @Override
    public Map<String, Object> stats() {
        List<SiteMessage> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("unread", all.stream()
                .filter(m -> Integer.valueOf(NotificationTypeUtil.READ_UNREAD).equals(m.getReadStatus())).count());
        r.put("read", all.stream()
                .filter(m -> Integer.valueOf(NotificationTypeUtil.READ_READ).equals(m.getReadStatus())).count());
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String c : List.of(NotificationTypeUtil.MSG_NOTICE, NotificationTypeUtil.MSG_ANNOUNCE,
                NotificationTypeUtil.MSG_TODO, NotificationTypeUtil.MSG_SYSTEM)) {
            byCategory.put(c, all.stream().filter(m -> c.equals(m.getCategory())).count());
        }
        r.put("byCategory", byCategory);
        Map<String, Long> byReceiver = new LinkedHashMap<>();
        for (String scope : List.of(NotificationTypeUtil.RECEIVER_ALL, NotificationTypeUtil.RECEIVER_ROLE,
                NotificationTypeUtil.RECEIVER_USER)) {
            byReceiver.put(scope, all.stream().filter(m -> scope.equals(m.getReceiverScope())).count());
        }
        r.put("byReceiverScope", byReceiver);
        return r;
    }

    @Override
    public void exportExcel(SiteMessageQuery query, OutputStream out) {
        SiteMessageExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<SiteMessage> rows = SiteMessageExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (SiteMessage row : rows) {
            try {
                validateMessage(row);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            long dup = this.count(new LambdaQueryWrapper<SiteMessage>()
                    .eq(SiteMessage::getTitle, row.getTitle())
                    .eq(SiteMessage::getContent, row.getContent() == null ? "" : row.getContent()));
            if (dup > 0) {
                skipped++;
                continue;
            }
            applyDefaults(row);
            row.setReadStatus(NotificationTypeUtil.READ_UNREAD);
            row.setReadTime(null);
            if (row.getSendTime() == null) {
                row.setSendTime(LocalDateTime.now());
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
        SiteMessageExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    /**
     * 某用户可见的消息：全部人员，或接收范围为其角色，或接收范围为其本人。
     * 未传用户名与角色编码时视为管理员视角，返回全部消息。
     */
    private LambdaQueryWrapper<SiteMessage> visibleWrapper(String username, String roleCode) {
        LambdaQueryWrapper<SiteMessage> w = new LambdaQueryWrapper<>();
        if (!StringUtils.hasText(username) && !StringUtils.hasText(roleCode)) {
            return w;
        }
        w.and(x -> x.eq(SiteMessage::getReceiverScope, NotificationTypeUtil.RECEIVER_ALL)
                .or(StringUtils.hasText(roleCode), q -> q.eq(SiteMessage::getReceiverScope,
                                NotificationTypeUtil.RECEIVER_ROLE)
                        .eq(SiteMessage::getReceiverValue, roleCode))
                .or(StringUtils.hasText(username), q -> q.eq(SiteMessage::getReceiverScope,
                                NotificationTypeUtil.RECEIVER_USER)
                        .eq(SiteMessage::getReceiverValue, username)));
        return w;
    }

    private LambdaQueryWrapper<SiteMessage> buildWrapper(SiteMessageQuery query) {
        LambdaQueryWrapper<SiteMessage> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(SiteMessage::getTitle, kw)
                    .or().like(SiteMessage::getContent, kw)
                    .or().like(SiteMessage::getSenderName, kw));
        }
        if (StringUtils.hasText(query.getCategory())) {
            w.eq(SiteMessage::getCategory, query.getCategory());
        }
        if (query.getReadStatus() != null) {
            w.eq(SiteMessage::getReadStatus, query.getReadStatus());
        }
        if (StringUtils.hasText(query.getReceiverScope())) {
            w.eq(SiteMessage::getReceiverScope, query.getReceiverScope());
        }
        w.orderByDesc(SiteMessage::getSendTime).orderByDesc(SiteMessage::getId);
        return w;
    }

    private void applyDefaults(SiteMessage m) {
        if (m.getCategory() == null) {
            m.setCategory(NotificationTypeUtil.MSG_NOTICE);
        }
        if (m.getReceiverScope() == null) {
            m.setReceiverScope(NotificationTypeUtil.RECEIVER_ALL);
        }
        if (NotificationTypeUtil.RECEIVER_ALL.equals(m.getReceiverScope())) {
            m.setReceiverValue(null);
        }
    }

    private void validateMessage(SiteMessage m) {
        if (!StringUtils.hasText(m.getTitle())) {
            throw new IllegalArgumentException("消息标题不能为空");
        }
        if (m.getCategory() != null && !NotificationTypeUtil.MESSAGE_CATEGORIES.contains(m.getCategory())) {
            throw new IllegalArgumentException("消息分类不合法：" + m.getCategory());
        }
        if (m.getReceiverScope() != null && !NotificationTypeUtil.RECEIVER_SCOPES.contains(m.getReceiverScope())) {
            throw new IllegalArgumentException("接收范围不合法：" + m.getReceiverScope());
        }
        if (NotificationTypeUtil.RECEIVER_ROLE.equals(m.getReceiverScope())
                && !StringUtils.hasText(m.getReceiverValue())) {
            throw new IllegalArgumentException("按角色接收时须填写角色编码");
        }
        if (NotificationTypeUtil.RECEIVER_USER.equals(m.getReceiverScope())
                && !StringUtils.hasText(m.getReceiverValue())) {
            throw new IllegalArgumentException("按人员接收时须填写用户名");
        }
    }
}
