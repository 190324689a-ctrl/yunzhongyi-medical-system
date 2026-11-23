package com.yunzhongyi.medical.committee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.committee.dto.CommitteeQuery;
import com.yunzhongyi.medical.committee.dto.CommitteeRenewRequest;
import com.yunzhongyi.medical.committee.entity.Committee;
import com.yunzhongyi.medical.committee.entity.CommitteeMeeting;
import com.yunzhongyi.medical.committee.entity.CommitteeResolution;
import com.yunzhongyi.medical.committee.excel.CommitteeExcelUtil;
import com.yunzhongyi.medical.committee.mapper.CommitteeMapper;
import com.yunzhongyi.medical.committee.mapper.CommitteeMeetingMapper;
import com.yunzhongyi.medical.committee.mapper.CommitteeResolutionMapper;
import com.yunzhongyi.medical.committee.service.CommitteeService;
import com.yunzhongyi.medical.committee.service.CommitteeTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 委员会管理服务实现：委员会 CRUD + 换届/解散 + 三表聚合统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitteeServiceImpl
        extends ServiceImpl<CommitteeMapper, Committee>
        implements CommitteeService {

    private final CommitteeMeetingMapper meetingMapper;
    private final CommitteeResolutionMapper resolutionMapper;

    @Override
    public PageResult<Committee> pageQuery(CommitteeQuery query) {
        Page<Committee> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<Committee> listByQuery(CommitteeQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<Committee> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("active", all.stream()
                .filter(c -> Integer.valueOf(CommitteeTypeUtil.STATUS_ACTIVE).equals(c.getStatus())).count());
        r.put("renewed", all.stream()
                .filter(c -> Integer.valueOf(CommitteeTypeUtil.STATUS_RENEWED).equals(c.getStatus())).count());
        r.put("dissolved", all.stream()
                .filter(c -> Integer.valueOf(CommitteeTypeUtil.STATUS_DISSOLVED).equals(c.getStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(CommitteeTypeUtil.TYPE_MEDICAL_QUALITY, CommitteeTypeUtil.TYPE_PHARMACY,
                CommitteeTypeUtil.TYPE_INFECTION, CommitteeTypeUtil.TYPE_TRANSFUSION,
                CommitteeTypeUtil.TYPE_MEDICAL_RECORD, CommitteeTypeUtil.TYPE_OTHER)) {
            byType.put(t, all.stream().filter(c -> t.equals(c.getType())).count());
        }
        r.put("byType", byType);
        r.put("byTypeList", statsByType());
        r.put("byMonth", statsByMonth());

        // 任期 90 天内到期：在任且任期结束日期不晚于今天 + 90 天（含已过期）
        LocalDate today = LocalDate.now();
        LocalDate warnDate = today.plusDays(CommitteeTypeUtil.EXPIRY_WARN_DAYS);
        r.put("expiringSoon", all.stream()
                .filter(c -> Integer.valueOf(CommitteeTypeUtil.STATUS_ACTIVE).equals(c.getStatus()))
                .filter(c -> c.getTermEndDate() != null && !c.getTermEndDate().isAfter(warnDate)).count());

        // 会议统计
        List<CommitteeMeeting> meetings = meetingMapper.selectList(null);
        r.put("meetingTotal", (long) meetings.size());

        // 决议统计
        List<CommitteeResolution> resolutions = resolutionMapper.selectList(null);
        r.put("resolutionTotal", (long) resolutions.size());
        r.put("resolutionPending", resolutions.stream()
                .filter(x -> Integer.valueOf(CommitteeTypeUtil.RESOLUTION_PENDING).equals(x.getStatus())).count());
        r.put("resolutionInProgress", resolutions.stream()
                .filter(x -> Integer.valueOf(CommitteeTypeUtil.RESOLUTION_IN_PROGRESS).equals(x.getStatus())).count());
        r.put("resolutionDone", resolutions.stream()
                .filter(x -> Integer.valueOf(CommitteeTypeUtil.RESOLUTION_DONE).equals(x.getStatus())).count());
        r.put("resolutionCancelled", resolutions.stream()
                .filter(x -> Integer.valueOf(CommitteeTypeUtil.RESOLUTION_CANCELLED).equals(x.getStatus())).count());
        long effective = resolutions.stream()
                .filter(x -> !Integer.valueOf(CommitteeTypeUtil.RESOLUTION_CANCELLED).equals(x.getStatus())).count();
        long done = resolutions.stream()
                .filter(x -> Integer.valueOf(CommitteeTypeUtil.RESOLUTION_DONE).equals(x.getStatus())).count();
        BigDecimal completionRate = effective == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(done * 100).divide(BigDecimal.valueOf(effective), 2, RoundingMode.HALF_UP);
        r.put("completionRate", completionRate);
        r.put("overdueResolutionCount", resolutions.stream()
                .filter(x -> CommitteeTypeUtil.isResolutionOverdue(x.getStatus(), x.getDeadline())).count());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByType() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String t : List.of(CommitteeTypeUtil.TYPE_MEDICAL_QUALITY, CommitteeTypeUtil.TYPE_PHARMACY,
                CommitteeTypeUtil.TYPE_INFECTION, CommitteeTypeUtil.TYPE_TRANSFUSION,
                CommitteeTypeUtil.TYPE_MEDICAL_RECORD, CommitteeTypeUtil.TYPE_OTHER)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", t);
            row.put("typeText", CommitteeTypeUtil.typeText(t));
            row.put("count", this.count(new LambdaQueryWrapper<Committee>()
                    .eq(Committee::getType, t)));
            row.put("activeCount", this.count(new LambdaQueryWrapper<Committee>()
                    .eq(Committee::getType, t)
                    .eq(Committee::getStatus, CommitteeTypeUtil.STATUS_ACTIVE)));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        Map<String, Long> grouped = new LinkedHashMap<>();
        for (CommitteeMeeting m : meetingMapper.selectList(null)) {
            if (m.getMeetingDate() == null) {
                continue;
            }
            String month = m.getMeetingDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            grouped.merge(month, 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", e.getKey());
            row.put("count", e.getValue());
            result.add(row);
        });
        return result;
    }

    @Override
    public Committee detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Committee renew(Long id, CommitteeRenewRequest request) {
        Committee old = getOrThrow(id);
        if (!Integer.valueOf(CommitteeTypeUtil.STATUS_ACTIVE).equals(old.getStatus())) {
            throw new IllegalStateException("该委员会已结束（"
                    + CommitteeTypeUtil.statusText(old.getStatus()) + "），不能再换届");
        }
        Committee next = new Committee();
        next.setName(old.getName());
        next.setType(old.getType());
        next.setChairperson(StringUtils.hasText(request.getChairperson()) ? request.getChairperson() : old.getChairperson());
        next.setViceChairperson(StringUtils.hasText(request.getViceChairperson()) ? request.getViceChairperson() : old.getViceChairperson());
        next.setSecretary(StringUtils.hasText(request.getSecretary()) ? request.getSecretary() : old.getSecretary());
        next.setMemberCount(request.getMemberCount() != null ? request.getMemberCount() : old.getMemberCount());
        next.setTermNo(old.getTermNo() == null ? 2 : old.getTermNo() + 1);
        next.setTermStartDate(request.getTermStartDate() != null ? request.getTermStartDate() : old.getTermEndDate());
        next.setTermEndDate(request.getTermEndDate());
        next.setStatus(CommitteeTypeUtil.STATUS_ACTIVE);
        next.setRemark(old.getRemark());
        normalize(next, true);
        super.save(next);
        // 原记录改为已换届；直接调 super.updateById，跳过本类 updateById 的
        // normalize 修改守卫（换届后原记录不再是“在任”，守卫会误判）
        old.setStatus(CommitteeTypeUtil.STATUS_RENEWED);
        super.updateById(old);
        return next;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Committee dissolve(Long id) {
        Committee c = getOrThrow(id);
        if (!Integer.valueOf(CommitteeTypeUtil.STATUS_ACTIVE).equals(c.getStatus())) {
            throw new IllegalStateException("该委员会已结束（"
                    + CommitteeTypeUtil.statusText(c.getStatus()) + "），不能再解散");
        }
        c.setStatus(CommitteeTypeUtil.STATUS_DISSOLVED);
        super.updateById(c);
        return c;
    }

    @Override
    public void exportExcel(CommitteeQuery query, OutputStream out) {
        CommitteeExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<Committee> list = CommitteeExcelUtil.read(in);
        int ok = 0;
        for (Committee c : list) {
            if (!StringUtils.hasText(c.getName())) {
                log.warn("导入跳过：委员会名称为空的行");
                continue;
            }
            if (!StringUtils.hasText(c.getType()) || !CommitteeTypeUtil.isValidType(c.getType())) {
                log.warn("导入跳过：委员会类别为空或不正确（委员会 {}）", c.getName());
                continue;
            }
            if (c.getStatus() == null) {
                c.setStatus(CommitteeTypeUtil.STATUS_ACTIVE);
            }
            if (c.getTermNo() == null) {
                c.setTermNo(1);
            }
            // 同一委员会名称同一届次视为重复，跳过
            if (this.count(new LambdaQueryWrapper<Committee>()
                    .eq(Committee::getName, c.getName())
                    .eq(Committee::getTermNo, c.getTermNo())) > 0) {
                log.warn("导入跳过：重复委员会 {} 第 {} 届", c.getName(), c.getTermNo());
                continue;
            }
            try {
                this.save(c);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", c.getName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除委员会时一并删除其会议与决议（逻辑删除） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Committee entity) {
        return removeById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        Long committeeId = (Long) id;
        List<CommitteeMeeting> meetings = meetingMapper.selectList(
                new LambdaQueryWrapper<CommitteeMeeting>().eq(CommitteeMeeting::getCommitteeId, committeeId));
        for (CommitteeMeeting m : meetings) {
            resolutionMapper.delete(new LambdaQueryWrapper<CommitteeResolution>()
                    .eq(CommitteeResolution::getMeetingId, m.getId()));
        }
        meetingMapper.delete(new LambdaQueryWrapper<CommitteeMeeting>()
                .eq(CommitteeMeeting::getCommitteeId, committeeId));
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(java.util.Collection<?> idList) {
        for (Object id : idList) {
            removeById((java.io.Serializable) id);
        }
        return true;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<Committee> buildWrapper(CommitteeQuery q) {
        LambdaQueryWrapper<Committee> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(Committee::getName, q.getKeyword())
                .or().like(Committee::getChairperson, q.getKeyword()));
        qw.eq(StringUtils.hasText(q.getType()), Committee::getType, q.getType());
        qw.eq(q.getStatus() != null, Committee::getStatus, q.getStatus());
        if (Boolean.TRUE.equals(q.getExpiringSoon())) {
            // 任期 90 天内到期（含已过期）：只看在任的委员会，任期结束不晚于今天 + 90 天
            qw.eq(Committee::getStatus, CommitteeTypeUtil.STATUS_ACTIVE);
            qw.le(Committee::getTermEndDate,
                    LocalDate.now().plusDays(CommitteeTypeUtil.EXPIRY_WARN_DAYS));
        }
        qw.orderByDesc(Committee::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验类别与任期 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Committee entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Committee entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(Committee c, boolean isCreate) {
        if (!StringUtils.hasText(c.getType()) || !CommitteeTypeUtil.isValidType(c.getType())) {
            throw new IllegalArgumentException("委员会类别不正确");
        }
        if (c.getTermStartDate() != null && c.getTermEndDate() != null
                && c.getTermEndDate().isBefore(c.getTermStartDate())) {
            throw new IllegalArgumentException("任期结束日期不能早于任期开始日期");
        }
        if (c.getMemberCount() != null && c.getMemberCount() < 0) {
            throw new IllegalArgumentException("委员人数不能为负");
        }
        if (c.getStatus() != null
                && !Integer.valueOf(CommitteeTypeUtil.STATUS_ACTIVE).equals(c.getStatus())
                && !Integer.valueOf(CommitteeTypeUtil.STATUS_RENEWED).equals(c.getStatus())
                && !Integer.valueOf(CommitteeTypeUtil.STATUS_DISSOLVED).equals(c.getStatus())) {
            throw new IllegalArgumentException("委员会状态不正确");
        }
        if (isCreate) {
            if (c.getStatus() == null) {
                c.setStatus(CommitteeTypeUtil.STATUS_ACTIVE);
            }
            if (c.getTermNo() == null) {
                c.setTermNo(1);
            }
            // 同一委员会名称同一届次已有记录，拒绝重复建档
            LambdaQueryWrapper<Committee> qw = new LambdaQueryWrapper<Committee>()
                    .eq(Committee::getName, c.getName())
                    .eq(Committee::getTermNo, c.getTermNo());
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("该委员会第 " + c.getTermNo() + " 届已有记录，请勿重复建档");
            }
        }
    }

    private Committee getOrThrow(Long id) {
        Committee c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("委员会不存在：" + id);
        }
        return c;
    }
}
