package com.yunzhongyi.medical.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.notification.dto.PushRecordQuery;
import com.yunzhongyi.medical.notification.entity.PushRecord;
import com.yunzhongyi.medical.notification.excel.PushRecordExcelUtil;
import com.yunzhongyi.medical.notification.mapper.PushRecordMapper;
import com.yunzhongyi.medical.notification.service.NotificationTypeUtil;
import com.yunzhongyi.medical.notification.service.PushRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息推送服务实现。
 * <p>
 * 推送通道本机没有真实服务：{@link #attemptSend(PushRecord)} 为预留占位，
 * 不发起任何真实调用，直接记为发送失败并写明“推送通道未对接”，绝不伪造发送成功。
 * 真实通道对接后，只需替换该方法内部实现，其余流转（重推守卫、统计、导出）不变。
 */
@Service
@RequiredArgsConstructor
public class PushRecordServiceImpl
        extends ServiceImpl<PushRecordMapper, PushRecord>
        implements PushRecordService {

    @Override
    public PageResult<PushRecord> pageQuery(PushRecordQuery query) {
        Page<PushRecord> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public PushRecord detail(Long id) {
        PushRecord p = this.getById(id);
        if (p == null) {
            throw new IllegalArgumentException("消息推送记录不存在：" + id);
        }
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> send(PushRecord record) {
        validatePush(record);
        applyDefaults(record);
        record.setSendStatus(NotificationTypeUtil.SEND_PENDING);
        record.setSendTime(null);
        record.setFailReason(null);
        this.save(record);
        return attemptSend(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> retry(Long id) {
        PushRecord p = detail(id);
        if (Integer.valueOf(NotificationTypeUtil.SEND_SENT).equals(p.getSendStatus())) {
            throw new IllegalArgumentException("已发送成功，不可重推");
        }
        return attemptSend(p);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PushRecord p = detail(id);
        if (Integer.valueOf(NotificationTypeUtil.SEND_SENT).equals(p.getSendStatus())) {
            throw new IllegalArgumentException("已发送成功的推送记录不可直接删除");
        }
        this.removeById(id);
    }

    @Override
    public Map<String, Object> stats() {
        List<PushRecord> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(p -> Integer.valueOf(NotificationTypeUtil.SEND_PENDING).equals(p.getSendStatus())).count());
        r.put("sent", all.stream()
                .filter(p -> Integer.valueOf(NotificationTypeUtil.SEND_SENT).equals(p.getSendStatus())).count());
        r.put("failed", all.stream()
                .filter(p -> Integer.valueOf(NotificationTypeUtil.SEND_FAILED).equals(p.getSendStatus())).count());
        Map<String, Long> byChannel = new LinkedHashMap<>();
        for (String ch : List.of(NotificationTypeUtil.PUSH_WECHAT, NotificationTypeUtil.PUSH_APP,
                NotificationTypeUtil.PUSH_EMAIL)) {
            byChannel.put(ch, all.stream().filter(p -> ch.equals(p.getChannel())).count());
        }
        r.put("byChannel", byChannel);
        return r;
    }

    @Override
    public void exportExcel(PushRecordQuery query, OutputStream out) {
        PushRecordExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    // ---------- 内部方法 ----------

    /**
     * 预留占位发送：本机未对接任何真实推送通道（微信 / 移动端 / 邮件），
     * 不发起真实调用，直接记为发送失败。
     */
    private Map<String, Object> attemptSend(PushRecord p) {
        p.setSendStatus(NotificationTypeUtil.SEND_FAILED);
        p.setSendTime(LocalDateTime.now());
        p.setFailReason("推送通道未对接（预留占位）："
                + NotificationTypeUtil.pushChannelText(p.getChannel()) + "通道未配置真实服务");
        this.updateById(p);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", p);
        result.put("success", Boolean.FALSE);
        result.put("message", p.getFailReason());
        return result;
    }

    private LambdaQueryWrapper<PushRecord> buildWrapper(PushRecordQuery query) {
        LambdaQueryWrapper<PushRecord> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(PushRecord::getTitle, kw)
                    .or().like(PushRecord::getContent, kw)
                    .or().like(PushRecord::getReceiverName, kw));
        }
        if (StringUtils.hasText(query.getChannel())) {
            w.eq(PushRecord::getChannel, query.getChannel());
        }
        if (query.getSendStatus() != null) {
            w.eq(PushRecord::getSendStatus, query.getSendStatus());
        }
        w.orderByDesc(PushRecord::getSendTime).orderByDesc(PushRecord::getId);
        return w;
    }

    private void applyDefaults(PushRecord p) {
        if (p.getChannel() == null) {
            p.setChannel(NotificationTypeUtil.PUSH_WECHAT);
        }
        if (p.getReceiverScope() == null) {
            p.setReceiverScope(NotificationTypeUtil.RECEIVER_ALL);
        }
        if (NotificationTypeUtil.RECEIVER_ALL.equals(p.getReceiverScope())) {
            p.setReceiverValue(null);
        }
    }

    private void validatePush(PushRecord p) {
        if (!StringUtils.hasText(p.getTitle())) {
            throw new IllegalArgumentException("推送标题不能为空");
        }
        if (p.getChannel() != null && !NotificationTypeUtil.PUSH_CHANNELS.contains(p.getChannel())) {
            throw new IllegalArgumentException("推送渠道不合法：" + p.getChannel());
        }
        if (p.getReceiverScope() != null && !NotificationTypeUtil.RECEIVER_SCOPES.contains(p.getReceiverScope())) {
            throw new IllegalArgumentException("接收范围不合法：" + p.getReceiverScope());
        }
        if (NotificationTypeUtil.RECEIVER_ROLE.equals(p.getReceiverScope())
                && !StringUtils.hasText(p.getReceiverValue())) {
            throw new IllegalArgumentException("按角色推送时须填写角色编码");
        }
        if (NotificationTypeUtil.RECEIVER_USER.equals(p.getReceiverScope())
                && !StringUtils.hasText(p.getReceiverValue())) {
            throw new IllegalArgumentException("按人员推送时须填写用户名");
        }
    }
}
