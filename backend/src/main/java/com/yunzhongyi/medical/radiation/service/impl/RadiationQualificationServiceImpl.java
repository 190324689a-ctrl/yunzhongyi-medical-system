package com.yunzhongyi.medical.radiation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.radiation.dto.RadiationQualificationQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationQualification;
import com.yunzhongyi.medical.radiation.mapper.RadiationQualificationMapper;
import com.yunzhongyi.medical.radiation.service.RadiationQualificationService;
import com.yunzhongyi.medical.radiation.service.RadiationTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 放射人员资质管理服务实现：资质 CRUD + 一级审核（医务科）
 */
@Slf4j
@Service
public class RadiationQualificationServiceImpl
        extends ServiceImpl<RadiationQualificationMapper, RadiationQualification>
        implements RadiationQualificationService {

    @Override
    public PageResult<RadiationQualification> pageQuery(RadiationQualificationQuery query) {
        Page<RadiationQualification> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<RadiationQualification> listByPersonnel(Long personnelId) {
        return this.list(new LambdaQueryWrapper<RadiationQualification>()
                .eq(RadiationQualification::getPersonnelId, personnelId)
                .orderByAsc(RadiationQualification::getExpiryDate)
                .orderByDesc(RadiationQualification::getId));
    }

    @Override
    public RadiationQualification detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RadiationQualification approve(Long id, String approver, String opinion) {
        RadiationQualification q = getOrThrow(id);
        if (!Integer.valueOf(RadiationTypeUtil.APPROVE_PENDING).equals(q.getStatus())) {
            throw new IllegalStateException("该资质已结束（" + RadiationTypeUtil.approveStatusText(q.getStatus())
                    + "），不能再审核");
        }
        appendHistory(q, "医务科", approver, "通过", opinion);
        q.setStatus(RadiationTypeUtil.APPROVE_APPROVED);
        q.setApprover(approver);
        q.setApprovalOpinion(opinion);
        // 直接调 super.updateById：审核已校验待审核状态；走本类 updateById 的
        // normalize 会在同一事务内因 MyBatis 一级缓存拿到已被改过的同一对象而误判
        super.updateById(q);
        return q;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RadiationQualification reject(Long id, String approver, String opinion) {
        RadiationQualification q = getOrThrow(id);
        if (!Integer.valueOf(RadiationTypeUtil.APPROVE_PENDING).equals(q.getStatus())) {
            throw new IllegalStateException("该资质已结束（" + RadiationTypeUtil.approveStatusText(q.getStatus())
                    + "），不能再审核");
        }
        appendHistory(q, "医务科", approver, "驳回", opinion);
        q.setStatus(RadiationTypeUtil.APPROVE_REJECTED);
        q.setApprover(approver);
        q.setApprovalOpinion(opinion);
        // 同 approve：直接调 super.updateById，跳过 normalize 的修改守卫
        super.updateById(q);
        return q;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<RadiationQualification> buildWrapper(RadiationQualificationQuery q) {
        LambdaQueryWrapper<RadiationQualification> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(RadiationQualification::getPersonnelName, q.getKeyword())
                .or().like(RadiationQualification::getCertName, q.getKeyword())
                .or().like(RadiationQualification::getCertNo, q.getKeyword()));
        qw.eq(q.getPersonnelId() != null, RadiationQualification::getPersonnelId, q.getPersonnelId());
        qw.eq(StringUtils.hasText(q.getCertType()), RadiationQualification::getCertType, q.getCertType());
        qw.eq(q.getStatus() != null, RadiationQualification::getStatus, q.getStatus());
        if (Boolean.TRUE.equals(q.getExpiringSoon())) {
            // 90 天内到期（含已过期）：只看已通过的证书，有效期不晚于今天 + 90 天
            qw.eq(RadiationQualification::getStatus, RadiationTypeUtil.APPROVE_APPROVED);
            qw.le(RadiationQualification::getExpiryDate,
                    LocalDate.now().plusDays(RadiationTypeUtil.EXPIRY_WARN_DAYS));
        }
        qw.orderByAsc(RadiationQualification::getExpiryDate).orderByDesc(RadiationQualification::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时只允许改待审核的资质 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(RadiationQualification entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(RadiationQualification entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(RadiationQualification q, boolean isCreate) {
        if (!StringUtils.hasText(q.getCertType()) || !RadiationTypeUtil.isValidCertType(q.getCertType())) {
            throw new IllegalArgumentException("证书类型不正确");
        }
        if (q.getIssueDate() != null && q.getExpiryDate() != null
                && q.getExpiryDate().isBefore(q.getIssueDate())) {
            throw new IllegalArgumentException("证书有效期不能早于发证日期");
        }
        if (isCreate) {
            if (q.getStatus() == null) {
                q.setStatus(RadiationTypeUtil.APPROVE_PENDING);
            }
            // 同一人员同一证书类型同一证书编号已有待审核记录，拒绝重复登记
            if (StringUtils.hasText(q.getCertNo())) {
                LambdaQueryWrapper<RadiationQualification> qw = new LambdaQueryWrapper<RadiationQualification>()
                        .eq(RadiationQualification::getPersonnelId, q.getPersonnelId())
                        .eq(RadiationQualification::getCertType, q.getCertType())
                        .eq(RadiationQualification::getCertNo, q.getCertNo())
                        .eq(RadiationQualification::getStatus, RadiationTypeUtil.APPROVE_PENDING);
                if (this.count(qw) > 0) {
                    throw new IllegalArgumentException("该人员同一证书已有待审核记录，请勿重复登记");
                }
            }
        } else {
            // 更新只允许改待审核的资质（审核流转不走这里）。
            // 注意：这里用 selectOne 而不用 getById——同一事务内 getById 可能因
            // MyBatis 一级缓存返回调用方已改过的同一对象，导致状态判断失真。
            RadiationQualification existing = this.baseMapper.selectOne(new LambdaQueryWrapper<RadiationQualification>()
                    .eq(RadiationQualification::getId, q.getId()));
            if (existing == null) {
                throw new IllegalArgumentException("资质不存在：" + q.getId());
            }
            if (!Integer.valueOf(RadiationTypeUtil.APPROVE_PENDING).equals(existing.getStatus())) {
                throw new IllegalStateException("已结束审核的资质（"
                        + RadiationTypeUtil.approveStatusText(existing.getStatus()) + "）不能再修改");
            }
        }
    }

    private RadiationQualification getOrThrow(Long id) {
        RadiationQualification q = this.getById(id);
        if (q == null) {
            throw new IllegalArgumentException("资质不存在：" + id);
        }
        return q;
    }

    private void appendHistory(RadiationQualification q, String step, String approver, String action, String opinion) {
        String who = StringUtils.hasText(approver) && !approver.equals(step) ? " " + approver : "";
        String line = step + who + "：" + action
                + (StringUtils.hasText(opinion) ? "（" + opinion + "）" : "");
        q.setApprovalHistory(StringUtils.hasText(q.getApprovalHistory())
                ? q.getApprovalHistory() + "；" + line : line);
    }
}
