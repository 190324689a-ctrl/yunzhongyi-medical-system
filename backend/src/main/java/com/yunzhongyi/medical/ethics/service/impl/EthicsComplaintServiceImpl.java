package com.yunzhongyi.medical.ethics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.EthicsComplaintQuery;
import com.yunzhongyi.medical.ethics.dto.FeedbackRequest;
import com.yunzhongyi.medical.ethics.dto.HandleRequest;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsComplaint;
import com.yunzhongyi.medical.ethics.excel.EthicsComplaintExcelUtil;
import com.yunzhongyi.medical.ethics.mapper.EthicsComplaintMapper;
import com.yunzhongyi.medical.ethics.service.EthicsComplaintService;
import com.yunzhongyi.medical.ethics.service.EthicsTypeUtil;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.mapper.PhysicianMapper;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医德医风投诉服务实现：受理 / 调查 / 处理 / 反馈流转 + 统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EthicsComplaintServiceImpl
        extends ServiceImpl<EthicsComplaintMapper, EthicsComplaint>
        implements EthicsComplaintService {

    private final PhysicianMapper physicianMapper;

    @Override
    public PageResult<EthicsComplaint> pageQuery(EthicsComplaintQuery query) {
        Page<EthicsComplaint> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<EthicsComplaint> listByQuery(EthicsComplaintQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<EthicsComplaint> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_PENDING).equals(c.getStatus())).count());
        r.put("investigating", all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_INVESTIGATING).equals(c.getStatus())).count());
        r.put("handled", all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_HANDLED).equals(c.getStatus())).count());
        r.put("feedback", all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_FEEDBACK).equals(c.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_REJECTED).equals(c.getStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(EthicsTypeUtil.COMPLAINT_ATTITUDE, EthicsTypeUtil.COMPLAINT_QUALITY,
                EthicsTypeUtil.COMPLAINT_CHARGE, EthicsTypeUtil.COMPLAINT_INTEGRITY,
                EthicsTypeUtil.COMPLAINT_OTHER)) {
            byType.put(t, all.stream().filter(c -> t.equals(c.getComplaintType())).count());
        }
        r.put("byType", byType);
        r.put("byDept", statsByDept());
        // 逾期：未到已反馈、未不予受理，且处理期限已过当天
        r.put("overdue", all.stream()
                .filter(c -> EthicsTypeUtil.isComplaintOverdue(c.getStatus(), c.getDeadlineDate())).count());
        // 反馈率：已反馈 / 总数
        long feedbackCount = all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_FEEDBACK).equals(c.getStatus())).count();
        r.put("feedbackRate", rate(feedbackCount, all.size()));
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<EthicsComplaint>> grouped = new LinkedHashMap<>();
        for (EthicsComplaint c : this.list()) {
            String dept = StringUtils.hasText(c.getDeptName()) ? c.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("pendingCount", list.stream()
                    .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_PENDING).equals(c.getStatus())).count());
            row.put("investigatingCount", list.stream()
                    .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_INVESTIGATING).equals(c.getStatus())).count());
            row.put("feedbackCount", list.stream()
                    .filter(c -> Integer.valueOf(EthicsTypeUtil.COMPLAINT_FEEDBACK).equals(c.getStatus())).count());
            row.put("overdueCount", list.stream()
                    .filter(c -> EthicsTypeUtil.isComplaintOverdue(c.getStatus(), c.getDeadlineDate())).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public EthicsComplaint detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EthicsComplaint accept(Long id, String handler) {
        EthicsComplaint c = getOrThrow(id);
        if (!Integer.valueOf(EthicsTypeUtil.COMPLAINT_PENDING).equals(c.getStatus())) {
            throw new IllegalStateException("只有待受理的投诉才能受理（当前状态："
                    + EthicsTypeUtil.complaintStatusText(c.getStatus()) + "）");
        }
        c.setStatus(EthicsTypeUtil.COMPLAINT_INVESTIGATING);
        if (StringUtils.hasText(handler)) {
            c.setHandler(handler.trim());
        }
        c.setApprovalHistory(appendHistory(c.getApprovalHistory(),
                "医务科 " + (c.getHandler() == null ? "" : c.getHandler()) + "：受理，进入调查"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待受理/调查中能修改”守卫
        super.updateById(c);
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EthicsComplaint handle(Long id, HandleRequest request) {
        EthicsComplaint c = getOrThrow(id);
        if (!Integer.valueOf(EthicsTypeUtil.COMPLAINT_INVESTIGATING).equals(c.getStatus())) {
            throw new IllegalStateException("只有调查中的投诉才能处理（当前状态："
                    + EthicsTypeUtil.complaintStatusText(c.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getHandleResult())) {
            throw new IllegalArgumentException("处理结果不能为空");
        }
        if (StringUtils.hasText(request.getHandler())) {
            c.setHandler(request.getHandler().trim());
        }
        c.setHandleResult(request.getHandleResult().trim());
        c.setHandleDate(LocalDate.now());
        c.setStatus(EthicsTypeUtil.COMPLAINT_HANDLED);
        c.setApprovalHistory(appendHistory(c.getApprovalHistory(),
                "医务科 " + (c.getHandler() == null ? "" : c.getHandler()) + "：已处理（" + c.getHandleResult() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待受理/调查中能修改”守卫
        super.updateById(c);
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EthicsComplaint feedback(Long id, FeedbackRequest request) {
        EthicsComplaint c = getOrThrow(id);
        if (!Integer.valueOf(EthicsTypeUtil.COMPLAINT_HANDLED).equals(c.getStatus())) {
            throw new IllegalStateException("只有已处理的投诉才能反馈（当前状态："
                    + EthicsTypeUtil.complaintStatusText(c.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getFeedbackContent())) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }
        if (StringUtils.hasText(request.getHandler())) {
            c.setHandler(request.getHandler().trim());
        }
        c.setFeedbackContent(request.getFeedbackContent().trim());
        c.setFeedbackDate(LocalDate.now());
        c.setStatus(EthicsTypeUtil.COMPLAINT_FEEDBACK);
        c.setApprovalHistory(appendHistory(c.getApprovalHistory(),
                "医务科 " + (c.getHandler() == null ? "" : c.getHandler()) + "：已向投诉人反馈"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待受理/调查中能修改”守卫
        super.updateById(c);
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EthicsComplaint reject(Long id, RejectRequest request) {
        EthicsComplaint c = getOrThrow(id);
        if (!Integer.valueOf(EthicsTypeUtil.COMPLAINT_PENDING).equals(c.getStatus())) {
            throw new IllegalStateException("只有待受理的投诉才能不予受理（当前状态："
                    + EthicsTypeUtil.complaintStatusText(c.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("不予受理原因不能为空");
        }
        if (StringUtils.hasText(request.getApprover())) {
            c.setHandler(request.getApprover().trim());
        }
        c.setRejectReason(request.getReason().trim());
        c.setStatus(EthicsTypeUtil.COMPLAINT_REJECTED);
        c.setApprovalHistory(appendHistory(c.getApprovalHistory(),
                "医务科 " + (c.getHandler() == null ? "" : c.getHandler()) + "：不予受理（" + c.getRejectReason() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待受理/调查中能修改”守卫
        super.updateById(c);
        return c;
    }

    @Override
    public void exportExcel(EthicsComplaintQuery query, OutputStream out) {
        EthicsComplaintExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<EthicsComplaint> list = EthicsComplaintExcelUtil.read(in);
        int ok = 0;
        for (EthicsComplaint c : list) {
            if (!StringUtils.hasText(c.getComplaintNo())) {
                log.warn("导入跳过：投诉编号为空的行");
                continue;
            }
            if (!StringUtils.hasText(c.getComplainantName())) {
                log.warn("导入跳过：投诉人姓名为空的行（投诉 {}）", c.getComplaintNo());
                continue;
            }
            if (!StringUtils.hasText(c.getComplaintType()) || !EthicsTypeUtil.isValidComplaintType(c.getComplaintType())) {
                log.warn("导入跳过：投诉类型为空或不正确（投诉 {}）", c.getComplaintNo());
                continue;
            }
            // 同一投诉编号已有记录，跳过
            if (this.count(new LambdaQueryWrapper<EthicsComplaint>()
                    .eq(EthicsComplaint::getComplaintNo, c.getComplaintNo())) > 0) {
                log.warn("导入跳过：重复投诉编号 {}", c.getComplaintNo());
                continue;
            }
            // 导入的均为待受理，需走受理/处理/反馈流转
            c.setStatus(EthicsTypeUtil.COMPLAINT_PENDING);
            c.setHandler(null);
            c.setHandleResult(null);
            c.setHandleDate(null);
            c.setFeedbackContent(null);
            c.setFeedbackDate(null);
            c.setRejectReason(null);
            c.setApprovalHistory(null);
            if (c.getComplaintDate() == null) {
                c.setComplaintDate(LocalDate.now());
            }
            if (c.getDeadlineDate() == null) {
                c.setDeadlineDate(c.getComplaintDate().plusDays(EthicsTypeUtil.HANDLE_DEADLINE_DAYS));
            }
            // 按被投诉医师姓名查医师档案补全医师 id / 科室；查不到的不阻断，留空待核实
            if (c.getPhysicianId() == null && StringUtils.hasText(c.getPhysicianName())) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, c.getPhysicianName()).last("LIMIT 1"));
                if (physician != null) {
                    c.setPhysicianId(physician.getId());
                    if (!StringUtils.hasText(c.getDeptName())) {
                        c.setDeptName(physician.getDeptName());
                    }
                }
            }
            try {
                this.save(c);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", c.getComplaintNo(), e.getMessage());
            }
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<EthicsComplaint> buildWrapper(EthicsComplaintQuery q) {
        LambdaQueryWrapper<EthicsComplaint> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(EthicsComplaint::getComplainantName, q.getKeyword())
                .or().like(EthicsComplaint::getPhysicianName, q.getKeyword())
                .or().like(EthicsComplaint::getComplaintNo, q.getKeyword()));
        qw.eq(StringUtils.hasText(q.getComplaintType()), EthicsComplaint::getComplaintType, q.getComplaintType());
        qw.eq(q.getStatus() != null, EthicsComplaint::getStatus, q.getStatus());
        if (Boolean.TRUE.equals(q.getOverdue())) {
            // 逾期：未到已反馈、未不予受理，且处理期限已过当天
            qw.in(EthicsComplaint::getStatus, List.of(
                    EthicsTypeUtil.COMPLAINT_PENDING,
                    EthicsTypeUtil.COMPLAINT_INVESTIGATING,
                    EthicsTypeUtil.COMPLAINT_HANDLED));
            qw.lt(EthicsComplaint::getDeadlineDate, LocalDate.now());
        }
        qw.orderByDesc(EthicsComplaint::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验投诉类型与处理期限 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EthicsComplaint entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(EthicsComplaint entity) {
        // 只有待受理/调查中的投诉能修改：已处理/已反馈/不予受理的投诉不可再改
        EthicsComplaint old = getOrThrow(entity.getId());
        if (!Integer.valueOf(EthicsTypeUtil.COMPLAINT_PENDING).equals(old.getStatus())
                && !Integer.valueOf(EthicsTypeUtil.COMPLAINT_INVESTIGATING).equals(old.getStatus())) {
            throw new IllegalStateException("只有待受理/调查中的投诉才能修改（当前状态："
                    + EthicsTypeUtil.complaintStatusText(old.getStatus()) + "）");
        }
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(EthicsComplaint c, boolean isCreate) {
        if (!StringUtils.hasText(c.getComplaintNo())) {
            throw new IllegalArgumentException("投诉编号不能为空");
        }
        if (!StringUtils.hasText(c.getComplaintType()) || !EthicsTypeUtil.isValidComplaintType(c.getComplaintType())) {
            throw new IllegalArgumentException("投诉类型不正确");
        }
        if (c.getComplaintDate() != null && c.getDeadlineDate() != null
                && c.getDeadlineDate().isBefore(c.getComplaintDate())) {
            throw new IllegalArgumentException("处理期限不能早于投诉日期");
        }
        if (isCreate) {
            if (c.getStatus() == null) {
                c.setStatus(EthicsTypeUtil.COMPLAINT_PENDING);
            }
            if (c.getComplaintDate() == null) {
                c.setComplaintDate(LocalDate.now());
            }
            if (c.getDeadlineDate() == null) {
                c.setDeadlineDate(c.getComplaintDate().plusDays(EthicsTypeUtil.HANDLE_DEADLINE_DAYS));
            }
            // 同一投诉编号已有记录，拒绝重复登记
            if (this.count(new LambdaQueryWrapper<EthicsComplaint>()
                    .eq(EthicsComplaint::getComplaintNo, c.getComplaintNo())) > 0) {
                throw new IllegalArgumentException("投诉编号 " + c.getComplaintNo() + " 已有记录，请勿重复登记");
            }
            // 按被投诉医师姓名查医师档案补全医师 id / 科室；查不到的不阻断，留空待核实
            if (c.getPhysicianId() == null && StringUtils.hasText(c.getPhysicianName())) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, c.getPhysicianName()).last("LIMIT 1"));
                if (physician != null) {
                    c.setPhysicianId(physician.getId());
                    if (!StringUtils.hasText(c.getDeptName())) {
                        c.setDeptName(physician.getDeptName());
                    }
                }
            }
        }
    }

    private BigDecimal rate(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private String appendHistory(String history, String entry) {
        if (!StringUtils.hasText(history)) {
            return entry;
        }
        return history + "；" + entry;
    }

    private EthicsComplaint getOrThrow(Long id) {
        EthicsComplaint c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("医德医风投诉不存在：" + id);
        }
        return c;
    }
}
