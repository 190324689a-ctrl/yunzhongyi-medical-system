package com.yunzhongyi.medical.committee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.committee.dto.CommitteeResolutionQuery;
import com.yunzhongyi.medical.committee.entity.CommitteeMeeting;
import com.yunzhongyi.medical.committee.entity.CommitteeResolution;
import com.yunzhongyi.medical.committee.mapper.CommitteeMeetingMapper;
import com.yunzhongyi.medical.committee.mapper.CommitteeResolutionMapper;
import com.yunzhongyi.medical.committee.service.CommitteeResolutionService;
import com.yunzhongyi.medical.committee.service.CommitteeTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 委员会决议服务实现：决议 CRUD + 执行流转（开始/完成/取消）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitteeResolutionServiceImpl
        extends ServiceImpl<CommitteeResolutionMapper, CommitteeResolution>
        implements CommitteeResolutionService {

    private final CommitteeMeetingMapper meetingMapper;

    @Override
    public PageResult<CommitteeResolution> pageQuery(CommitteeResolutionQuery query) {
        Page<CommitteeResolution> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<CommitteeResolution> listByMeeting(Long meetingId) {
        return this.list(new LambdaQueryWrapper<CommitteeResolution>()
                .eq(CommitteeResolution::getMeetingId, meetingId)
                .orderByAsc(CommitteeResolution::getDeadline)
                .orderByDesc(CommitteeResolution::getId));
    }

    @Override
    public CommitteeResolution detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommitteeResolution start(Long id) {
        CommitteeResolution r = getOrThrow(id);
        if (!Integer.valueOf(CommitteeTypeUtil.RESOLUTION_PENDING).equals(r.getStatus())) {
            throw new IllegalStateException("该决议已结束执行（"
                    + CommitteeTypeUtil.resolutionStatusText(r.getStatus()) + "），不能开始执行");
        }
        r.setStatus(CommitteeTypeUtil.RESOLUTION_IN_PROGRESS);
        // 直接调 super.updateById：流转已校验待执行状态；走本类 updateById 的
        // normalize 会在同一事务内因 MyBatis 一级缓存拿到已被改过的同一对象而误判
        super.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommitteeResolution complete(Long id) {
        CommitteeResolution r = getOrThrow(id);
        if (!Integer.valueOf(CommitteeTypeUtil.RESOLUTION_IN_PROGRESS).equals(r.getStatus())) {
            throw new IllegalStateException("只有执行中的决议才能完成（当前："
                    + CommitteeTypeUtil.resolutionStatusText(r.getStatus()) + "）");
        }
        r.setStatus(CommitteeTypeUtil.RESOLUTION_DONE);
        if (r.getCompleteDate() == null) {
            r.setCompleteDate(LocalDate.now());
        }
        super.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommitteeResolution cancel(Long id) {
        CommitteeResolution r = getOrThrow(id);
        if (Integer.valueOf(CommitteeTypeUtil.RESOLUTION_DONE).equals(r.getStatus())
                || Integer.valueOf(CommitteeTypeUtil.RESOLUTION_CANCELLED).equals(r.getStatus())) {
            throw new IllegalStateException("该决议已结束（"
                    + CommitteeTypeUtil.resolutionStatusText(r.getStatus()) + "），不能再取消");
        }
        r.setStatus(CommitteeTypeUtil.RESOLUTION_CANCELLED);
        super.updateById(r);
        return r;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<CommitteeResolution> buildWrapper(CommitteeResolutionQuery q) {
        LambdaQueryWrapper<CommitteeResolution> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(CommitteeResolution::getTitle, q.getKeyword())
                .or().like(CommitteeResolution::getResponsiblePerson, q.getKeyword()));
        qw.eq(q.getMeetingId() != null, CommitteeResolution::getMeetingId, q.getMeetingId());
        qw.eq(q.getCommitteeId() != null, CommitteeResolution::getCommitteeId, q.getCommitteeId());
        qw.eq(q.getStatus() != null, CommitteeResolution::getStatus, q.getStatus());
        if (Boolean.TRUE.equals(q.getOverdue())) {
            // 逾期：待执行/执行中且决议期限已过当天
            qw.in(CommitteeResolution::getStatus,
                    CommitteeTypeUtil.RESOLUTION_PENDING, CommitteeTypeUtil.RESOLUTION_IN_PROGRESS);
            qw.lt(CommitteeResolution::getDeadline, LocalDate.now());
        }
        qw.orderByAsc(CommitteeResolution::getDeadline).orderByDesc(CommitteeResolution::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时只允许改未结束的决议 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(CommitteeResolution entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(CommitteeResolution entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(CommitteeResolution r, boolean isCreate) {
        if (r.getStatus() != null
                && !Integer.valueOf(CommitteeTypeUtil.RESOLUTION_PENDING).equals(r.getStatus())
                && !Integer.valueOf(CommitteeTypeUtil.RESOLUTION_IN_PROGRESS).equals(r.getStatus())
                && !Integer.valueOf(CommitteeTypeUtil.RESOLUTION_DONE).equals(r.getStatus())
                && !Integer.valueOf(CommitteeTypeUtil.RESOLUTION_CANCELLED).equals(r.getStatus())) {
            throw new IllegalArgumentException("决议状态不正确");
        }
        CommitteeMeeting m = meetingMapper.selectById(r.getMeetingId());
        if (m == null) {
            throw new IllegalArgumentException("会议不存在：" + r.getMeetingId());
        }
        if (isCreate) {
            if (r.getStatus() == null) {
                r.setStatus(CommitteeTypeUtil.RESOLUTION_PENDING);
            }
            if (r.getCommitteeId() == null) {
                r.setCommitteeId(m.getCommitteeId());
            }
            // 同一会议同一决议事项已有记录，拒绝重复登记
            LambdaQueryWrapper<CommitteeResolution> qw = new LambdaQueryWrapper<CommitteeResolution>()
                    .eq(CommitteeResolution::getMeetingId, r.getMeetingId())
                    .eq(CommitteeResolution::getTitle, r.getTitle());
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("该会议已有同名决议，请勿重复登记");
            }
        } else {
            // 更新只允许改未结束的决议（流转不走这里）。
            // 注意：这里用 selectOne 而不用 getById——同一事务内 getById 可能因
            // MyBatis 一级缓存返回调用方已改过的同一对象，导致状态判断失真。
            CommitteeResolution existing = this.baseMapper.selectOne(
                    new LambdaQueryWrapper<CommitteeResolution>().eq(CommitteeResolution::getId, r.getId()));
            if (existing == null) {
                throw new IllegalArgumentException("决议不存在：" + r.getId());
            }
            if (Integer.valueOf(CommitteeTypeUtil.RESOLUTION_DONE).equals(existing.getStatus())
                    || Integer.valueOf(CommitteeTypeUtil.RESOLUTION_CANCELLED).equals(existing.getStatus())) {
                throw new IllegalStateException("已结束的决议（"
                        + CommitteeTypeUtil.resolutionStatusText(existing.getStatus()) + "）不能再修改");
            }
        }
    }

    private CommitteeResolution getOrThrow(Long id) {
        CommitteeResolution r = this.getById(id);
        if (r == null) {
            throw new IllegalArgumentException("决议不存在：" + id);
        }
        return r;
    }
}
