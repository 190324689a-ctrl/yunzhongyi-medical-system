package com.yunzhongyi.medical.mdt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.mdt.dto.MdtQuery;
import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import com.yunzhongyi.medical.mdt.entity.MdtRecord;
import com.yunzhongyi.medical.mdt.excel.MdtExcelUtil;
import com.yunzhongyi.medical.mdt.mapper.MdtApplicationMapper;
import com.yunzhongyi.medical.mdt.mapper.MdtRecordMapper;
import com.yunzhongyi.medical.mdt.service.MdtService;
import com.yunzhongyi.medical.mdt.service.MdtTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MDT 多学科会诊管理服务实现：申请 CRUD + 一级审批（医务科/科主任）+
 * 会诊记录（可多次补充）+ 统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdtServiceImpl
        extends ServiceImpl<MdtApplicationMapper, MdtApplication>
        implements MdtService {

    private final MdtRecordMapper recordMapper;

    @Override
    public PageResult<MdtApplication> pageQuery(MdtQuery query) {
        Page<MdtApplication> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<MdtApplication> listByQuery(MdtQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<MdtApplication> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(a -> Integer.valueOf(MdtTypeUtil.STATUS_PENDING).equals(a.getStatus())).count());
        r.put("approved", all.stream()
                .filter(a -> Integer.valueOf(MdtTypeUtil.STATUS_APPROVED).equals(a.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(a -> Integer.valueOf(MdtTypeUtil.STATUS_REJECTED).equals(a.getStatus())).count());
        r.put("urgentCount", all.stream()
                .filter(a -> MdtTypeUtil.URGENCY_URGENT.equals(a.getUrgency())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(MdtTypeUtil.TYPE_IN_HOSPITAL, MdtTypeUtil.TYPE_OUT_HOSPITAL,
                MdtTypeUtil.TYPE_REMOTE, MdtTypeUtil.TYPE_EMERGENCY)) {
            byType.put(t, all.stream().filter(a -> t.equals(a.getMdtType())).count());
        }
        r.put("byType", byType);
        r.put("byDept", statsByDept());
        r.put("byMonth", statsByMonth());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<MdtApplication>> grouped = new LinkedHashMap<>();
        for (MdtApplication a : this.list()) {
            String dept = StringUtils.hasText(a.getDeptName()) ? a.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(a);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("pendingCount", list.stream()
                    .filter(a -> Integer.valueOf(MdtTypeUtil.STATUS_PENDING).equals(a.getStatus())).count());
            row.put("approvedCount", list.stream()
                    .filter(a -> Integer.valueOf(MdtTypeUtil.STATUS_APPROVED).equals(a.getStatus())).count());
            row.put("urgentCount", list.stream()
                    .filter(a -> MdtTypeUtil.URGENCY_URGENT.equals(a.getUrgency())).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        Map<YearMonth, List<MdtApplication>> grouped = new LinkedHashMap<>();
        for (MdtApplication a : this.list()) {
            if (a.getApplyDate() == null) {
                continue;
            }
            grouped.computeIfAbsent(YearMonth.from(a.getApplyDate()), k -> new ArrayList<>()).add(a);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.keySet().stream().sorted().forEach(month -> {
            List<MdtApplication> list = grouped.get(month);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            row.put("count", (long) list.size());
            row.put("approvedCount", list.stream()
                    .filter(a -> Integer.valueOf(MdtTypeUtil.STATUS_APPROVED).equals(a.getStatus())).count());
            row.put("pendingCount", list.stream()
                    .filter(a -> Integer.valueOf(MdtTypeUtil.STATUS_PENDING).equals(a.getStatus())).count());
            result.add(row);
        });
        return result;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        MdtApplication app = getOrThrow(id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("application", app);
        r.put("records", listRecords(id));
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MdtApplication approve(Long id, String approver, String opinion) {
        MdtApplication a = getOrThrow(id);
        if (!Integer.valueOf(MdtTypeUtil.STATUS_PENDING).equals(a.getStatus())) {
            throw new IllegalStateException("该 MDT 申请已结束（" + MdtTypeUtil.statusText(a.getStatus())
                    + "），不能再审批");
        }
        appendHistory(a, "医务科", approver, "通过", opinion);
        a.setStatus(MdtTypeUtil.STATUS_APPROVED);
        a.setApprover(approver);
        a.setApprovalOpinion(opinion);
        // 直接调 super.updateById：审批已校验待审批状态；走本类 updateById 的
        // normalize 会在同一事务内因 MyBatis 一级缓存拿到已被改过的同一对象而误判
        super.updateById(a);
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MdtApplication reject(Long id, String approver, String opinion) {
        MdtApplication a = getOrThrow(id);
        if (!Integer.valueOf(MdtTypeUtil.STATUS_PENDING).equals(a.getStatus())) {
            throw new IllegalStateException("该 MDT 申请已结束（" + MdtTypeUtil.statusText(a.getStatus())
                    + "），不能再审批");
        }
        appendHistory(a, "医务科", approver, "驳回", opinion);
        a.setStatus(MdtTypeUtil.STATUS_REJECTED);
        a.setApprover(approver);
        a.setApprovalOpinion(opinion);
        // 同 approve：直接调 super.updateById，跳过 normalize 的修改守卫
        super.updateById(a);
        return a;
    }

    @Override
    public List<MdtRecord> listRecords(Long mdtId) {
        getOrThrow(mdtId);
        return recordMapper.selectList(new LambdaQueryWrapper<MdtRecord>()
                .eq(MdtRecord::getMdtId, mdtId)
                .orderByAsc(MdtRecord::getMeetingTime).orderByAsc(MdtRecord::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MdtRecord addRecord(Long mdtId, MdtRecord record) {
        MdtApplication a = getOrThrow(mdtId);
        if (!Integer.valueOf(MdtTypeUtil.STATUS_APPROVED).equals(a.getStatus())) {
            throw new IllegalStateException("只有审批通过的 MDT 申请才能添加会诊记录，当前状态："
                    + MdtTypeUtil.statusText(a.getStatus()));
        }
        if (!StringUtils.hasText(record.getAttendees())) {
            throw new IllegalArgumentException("实际参会专家不能为空");
        }
        if (record.getMeetingTime() == null) {
            throw new IllegalArgumentException("会诊时间不能为空");
        }
        if (!StringUtils.hasText(record.getOpinion())) {
            throw new IllegalArgumentException("会诊意见/结论不能为空");
        }
        record.setMdtId(mdtId);
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }
        recordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRecord(Long mdtId, Long recordId) {
        getOrThrow(mdtId);
        MdtRecord record = recordMapper.selectById(recordId);
        if (record == null || !mdtId.equals(record.getMdtId())) {
            throw new IllegalArgumentException("会诊记录不存在：" + recordId);
        }
        return recordMapper.deleteById(recordId) > 0;
    }

    @Override
    public void exportExcel(MdtQuery query, OutputStream out) {
        MdtExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<MdtApplication> list = MdtExcelUtil.read(in);
        int ok = 0;
        for (MdtApplication a : list) {
            if (!StringUtils.hasText(a.getPatientName())) {
                log.warn("导入跳过：患者姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(a.getMdtType()) || !MdtTypeUtil.isValidType(a.getMdtType())) {
                log.warn("导入跳过：会诊类型为空或不正确（患者 {}）", a.getPatientName());
                continue;
            }
            if (a.getExpectedTime() == null) {
                log.warn("导入跳过：期望会诊时间为空（患者 {}）", a.getPatientName());
                continue;
            }
            if (!StringUtils.hasText(a.getUrgency()) || !MdtTypeUtil.isValidUrgency(a.getUrgency())) {
                a.setUrgency(MdtTypeUtil.URGENCY_NORMAL);
            }
            if (!StringUtils.hasText(a.getApplyPhysicianName())) {
                log.warn("导入跳过：申请医师为空（患者 {}）", a.getPatientName());
                continue;
            }
            // 同一患者同一住院号同一期望会诊时间视为重复，跳过
            LambdaQueryWrapper<MdtApplication> qw = new LambdaQueryWrapper<MdtApplication>()
                    .eq(MdtApplication::getPatientName, a.getPatientName())
                    .eq(MdtApplication::getExpectedTime, a.getExpectedTime());
            if (StringUtils.hasText(a.getInpatientNo())) {
                qw.eq(MdtApplication::getInpatientNo, a.getInpatientNo());
            }
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复 MDT 申请 {} {}", a.getPatientName(), a.getExpectedTime());
                continue;
            }
            this.save(a);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<MdtApplication> buildWrapper(MdtQuery q) {
        LambdaQueryWrapper<MdtApplication> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.and(w -> w.like(MdtApplication::getPatientName, q.getKeyword())
                    .or().like(MdtApplication::getInpatientNo, q.getKeyword())
                    .or().like(MdtApplication::getApplyPhysicianName, q.getKeyword()));
        }
        qw.eq(q.getDeptId() != null, MdtApplication::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getMdtType()), MdtApplication::getMdtType, q.getMdtType());
        qw.eq(q.getStatus() != null, MdtApplication::getStatus, q.getStatus());
        qw.eq(StringUtils.hasText(q.getUrgency()), MdtApplication::getUrgency, q.getUrgency());
        qw.eq(q.getApplyPhysicianId() != null, MdtApplication::getApplyPhysicianId, q.getApplyPhysicianId());
        // 注意：LambdaQueryWrapper 的条件值参数是立即求值的，null 日期上直接调
        // atStartOfDay() 会 NPE，所以先算好再传
        LocalDateTime startDateTime = q.getStartDate() == null ? null : q.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = q.getEndDate() == null ? null : q.getEndDate().atTime(23, 59, 59);
        qw.ge(startDateTime != null, MdtApplication::getExpectedTime, startDateTime);
        qw.le(endDateTime != null, MdtApplication::getExpectedTime, endDateTime);
        qw.orderByDesc(MdtApplication::getApplyDate).orderByDesc(MdtApplication::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时只允许改待审批的申请 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(MdtApplication entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(MdtApplication entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(MdtApplication a, boolean isCreate) {
        if (!StringUtils.hasText(a.getMdtType()) || !MdtTypeUtil.isValidType(a.getMdtType())) {
            throw new IllegalArgumentException("会诊类型不正确");
        }
        if (!StringUtils.hasText(a.getUrgency()) || !MdtTypeUtil.isValidUrgency(a.getUrgency())) {
            throw new IllegalArgumentException("紧急程度不正确");
        }
        if (a.getExpectedTime() == null) {
            throw new IllegalArgumentException("期望会诊时间不能为空");
        }
        if (isCreate) {
            if (a.getStatus() == null) {
                a.setStatus(MdtTypeUtil.STATUS_PENDING);
            }
            if (a.getApplyDate() == null) {
                a.setApplyDate(LocalDate.now());
            }
        } else {
            // 更新只允许改待审批的申请（审批流转不走这里，见 approve/reject）。
            // 注意：这里用 selectOne 而不用 getById——同一事务内 getById 可能因
            // MyBatis 一级缓存返回调用方已改过的同一对象，导致状态判断失真。
            MdtApplication existing = this.baseMapper.selectOne(new LambdaQueryWrapper<MdtApplication>()
                    .eq(MdtApplication::getId, a.getId()));
            if (existing == null) {
                throw new IllegalArgumentException("MDT 申请不存在：" + a.getId());
            }
            if (!Integer.valueOf(MdtTypeUtil.STATUS_PENDING).equals(existing.getStatus())) {
                throw new IllegalStateException("已结束的 MDT 申请（"
                        + MdtTypeUtil.statusText(existing.getStatus()) + "）不能再修改");
            }
        }
    }

    private MdtApplication getOrThrow(Long id) {
        MdtApplication a = this.getById(id);
        if (a == null) {
            throw new IllegalArgumentException("MDT 申请不存在：" + id);
        }
        return a;
    }

    private void appendHistory(MdtApplication a, String step, String approver, String action, String opinion) {
        String who = StringUtils.hasText(approver) && !approver.equals(step) ? " " + approver : "";
        String line = step + who + "：" + action
                + (StringUtils.hasText(opinion) ? "（" + opinion + "）" : "");
        a.setApprovalHistory(StringUtils.hasText(a.getApprovalHistory())
                ? a.getApprovalHistory() + "；" + line : line);
    }
}
