package com.yunzhongyi.medical.committee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.committee.dto.CommitteeMeetingQuery;
import com.yunzhongyi.medical.committee.entity.Committee;
import com.yunzhongyi.medical.committee.entity.CommitteeMeeting;
import com.yunzhongyi.medical.committee.entity.CommitteeResolution;
import com.yunzhongyi.medical.committee.mapper.CommitteeMapper;
import com.yunzhongyi.medical.committee.mapper.CommitteeMeetingMapper;
import com.yunzhongyi.medical.committee.mapper.CommitteeResolutionMapper;
import com.yunzhongyi.medical.committee.service.CommitteeMeetingService;
import com.yunzhongyi.medical.committee.service.CommitteeTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 委员会会议记录服务实现：会议 CRUD；只有在任的委员会才能新增会议
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitteeMeetingServiceImpl
        extends ServiceImpl<CommitteeMeetingMapper, CommitteeMeeting>
        implements CommitteeMeetingService {

    private final CommitteeMapper committeeMapper;
    private final CommitteeResolutionMapper resolutionMapper;

    @Override
    public PageResult<CommitteeMeeting> pageQuery(CommitteeMeetingQuery query) {
        Page<CommitteeMeeting> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<CommitteeMeeting> listByCommittee(Long committeeId) {
        return this.list(new LambdaQueryWrapper<CommitteeMeeting>()
                .eq(CommitteeMeeting::getCommitteeId, committeeId)
                .orderByDesc(CommitteeMeeting::getMeetingDate)
                .orderByDesc(CommitteeMeeting::getId));
    }

    @Override
    public CommitteeMeeting detail(Long id) {
        return getOrThrow(id);
    }

    /** 删除会议时一并删除其决议（逻辑删除） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        resolutionMapper.delete(new LambdaQueryWrapper<CommitteeResolution>()
                .eq(CommitteeResolution::getMeetingId, (Long) id));
        return super.removeById(id);
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<CommitteeMeeting> buildWrapper(CommitteeMeetingQuery q) {
        LambdaQueryWrapper<CommitteeMeeting> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(CommitteeMeeting::getTitle, q.getKeyword())
                .or().like(CommitteeMeeting::getHost, q.getKeyword())
                .or().like(CommitteeMeeting::getCommitteeName, q.getKeyword()));
        qw.eq(q.getCommitteeId() != null, CommitteeMeeting::getCommitteeId, q.getCommitteeId());
        if (StringUtils.hasText(q.getDateFrom())) {
            qw.ge(CommitteeMeeting::getMeetingDate, LocalDate.parse(q.getDateFrom()));
        }
        if (StringUtils.hasText(q.getDateTo())) {
            qw.le(CommitteeMeeting::getMeetingDate, LocalDate.parse(q.getDateTo()));
        }
        qw.orderByDesc(CommitteeMeeting::getMeetingDate).orderByDesc(CommitteeMeeting::getId);
        return qw;
    }

    /** 新建时校验委员会在任并补委员会名称；更新时校验 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(CommitteeMeeting entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(CommitteeMeeting entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(CommitteeMeeting m, boolean isCreate) {
        if (m.getMeetingDate() == null) {
            throw new IllegalArgumentException("会议日期不能为空");
        }
        if (m.getAttendeeCount() != null && m.getAttendeeCount() < 0) {
            throw new IllegalArgumentException("参会人数不能为负");
        }
        Committee c = committeeMapper.selectById(m.getCommitteeId());
        if (c == null) {
            throw new IllegalArgumentException("委员会不存在：" + m.getCommitteeId());
        }
        if (isCreate) {
            // 只有在任的委员会才能新增会议
            if (!Integer.valueOf(CommitteeTypeUtil.STATUS_ACTIVE).equals(c.getStatus())) {
                throw new IllegalStateException("该委员会已结束（"
                        + CommitteeTypeUtil.statusText(c.getStatus()) + "），不能再新增会议");
            }
            if (!StringUtils.hasText(m.getCommitteeName())) {
                m.setCommitteeName(c.getName());
            }
            // 同一委员会同一天同一主题已有会议记录，拒绝重复登记
            LambdaQueryWrapper<CommitteeMeeting> qw = new LambdaQueryWrapper<CommitteeMeeting>()
                    .eq(CommitteeMeeting::getCommitteeId, m.getCommitteeId())
                    .eq(CommitteeMeeting::getMeetingDate, m.getMeetingDate())
                    .eq(CommitteeMeeting::getTitle, m.getTitle());
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("该委员会当天已有同主题会议记录，请勿重复登记");
            }
        }
    }

    private CommitteeMeeting getOrThrow(Long id) {
        CommitteeMeeting m = this.getById(id);
        if (m == null) {
            throw new IllegalArgumentException("会议记录不存在：" + id);
        }
        return m;
    }
}
