package com.yunzhongyi.medical.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.notification.dto.SmsRecordQuery;
import com.yunzhongyi.medical.notification.entity.SmsRecord;
import com.yunzhongyi.medical.notification.excel.SmsRecordExcelUtil;
import com.yunzhongyi.medical.notification.mapper.SmsRecordMapper;
import com.yunzhongyi.medical.notification.service.NotificationTypeUtil;
import com.yunzhongyi.medical.notification.service.SmsRecordService;
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
 * 短信提醒服务实现。
 * <p>
 * 短信网关本机没有真实服务：{@link #attemptSend(SmsRecord)} 为预留占位，
 * 不发起任何真实调用，直接记为发送失败并写明“短信网关未对接”，绝不伪造发送成功。
 * 真实网关对接后，只需替换该方法内部实现，其余流转（重发守卫、统计、导出）不变。
 */
@Service
@RequiredArgsConstructor
public class SmsRecordServiceImpl
        extends ServiceImpl<SmsRecordMapper, SmsRecord>
        implements SmsRecordService {

    @Override
    public PageResult<SmsRecord> pageQuery(SmsRecordQuery query) {
        Page<SmsRecord> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public SmsRecord detail(Long id) {
        SmsRecord s = this.getById(id);
        if (s == null) {
            throw new IllegalArgumentException("短信提醒记录不存在：" + id);
        }
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> send(SmsRecord record) {
        validateSms(record);
        record.setSendStatus(NotificationTypeUtil.SEND_PENDING);
        record.setSendTime(null);
        record.setFailReason(null);
        this.save(record);
        return attemptSend(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> retry(Long id) {
        SmsRecord s = detail(id);
        if (Integer.valueOf(NotificationTypeUtil.SEND_SENT).equals(s.getSendStatus())) {
            throw new IllegalArgumentException("已发送成功，不可重发");
        }
        return attemptSend(s);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SmsRecord s = detail(id);
        if (Integer.valueOf(NotificationTypeUtil.SEND_SENT).equals(s.getSendStatus())) {
            throw new IllegalArgumentException("已发送成功的短信记录不可直接删除");
        }
        this.removeById(id);
    }

    @Override
    public Map<String, Object> stats() {
        List<SmsRecord> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(s -> Integer.valueOf(NotificationTypeUtil.SEND_PENDING).equals(s.getSendStatus())).count());
        r.put("sent", all.stream()
                .filter(s -> Integer.valueOf(NotificationTypeUtil.SEND_SENT).equals(s.getSendStatus())).count());
        r.put("failed", all.stream()
                .filter(s -> Integer.valueOf(NotificationTypeUtil.SEND_FAILED).equals(s.getSendStatus())).count());
        return r;
    }

    @Override
    public void exportExcel(SmsRecordQuery query, OutputStream out) {
        SmsRecordExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    // ---------- 内部方法 ----------

    /**
     * 预留占位发送：本机未对接任何真实短信网关，不发起真实调用，直接记为发送失败。
     */
    private Map<String, Object> attemptSend(SmsRecord s) {
        s.setSendStatus(NotificationTypeUtil.SEND_FAILED);
        s.setSendTime(LocalDateTime.now());
        s.setFailReason("短信网关未对接（预留占位）：短信网关未配置真实服务");
        this.updateById(s);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record", s);
        result.put("success", Boolean.FALSE);
        result.put("message", s.getFailReason());
        return result;
    }

    private LambdaQueryWrapper<SmsRecord> buildWrapper(SmsRecordQuery query) {
        LambdaQueryWrapper<SmsRecord> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            w.and(x -> x.like(SmsRecord::getPhone, kw)
                    .or().like(SmsRecord::getContent, kw)
                    .or().like(SmsRecord::getReceiverName, kw));
        }
        if (query.getSendStatus() != null) {
            w.eq(SmsRecord::getSendStatus, query.getSendStatus());
        }
        w.orderByDesc(SmsRecord::getSendTime).orderByDesc(SmsRecord::getId);
        return w;
    }

    private void validateSms(SmsRecord s) {
        if (!StringUtils.hasText(s.getPhone())) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!NotificationTypeUtil.isValidPhone(s.getPhone().trim())) {
            throw new IllegalArgumentException("手机号格式不合法：" + s.getPhone());
        }
        if (!StringUtils.hasText(s.getContent())) {
            throw new IllegalArgumentException("短信内容不能为空");
        }
    }
}
