package com.yunzhongyi.medical.ethics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.ApproveRequest;
import com.yunzhongyi.medical.ethics.dto.EthicsCommendationQuery;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsCommendation;
import com.yunzhongyi.medical.ethics.excel.EthicsCommendationExcelUtil;
import com.yunzhongyi.medical.ethics.mapper.EthicsCommendationMapper;
import com.yunzhongyi.medical.ethics.service.EthicsCommendationService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医德医风表彰服务实现：表彰登记 + 一级审批 + 统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EthicsCommendationServiceImpl
        extends ServiceImpl<EthicsCommendationMapper, EthicsCommendation>
        implements EthicsCommendationService {

    private final PhysicianMapper physicianMapper;

    @Override
    public PageResult<EthicsCommendation> pageQuery(EthicsCommendationQuery query) {
        Page<EthicsCommendation> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<EthicsCommendation> listByQuery(EthicsCommendationQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<EthicsCommendation> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("pending", all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.APPROVAL_PENDING).equals(c.getStatus())).count());
        r.put("passed", all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.APPROVAL_PASSED).equals(c.getStatus())).count());
        r.put("rejected", all.stream()
                .filter(c -> Integer.valueOf(EthicsTypeUtil.APPROVAL_REJECTED).equals(c.getStatus())).count());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String t : List.of(EthicsTypeUtil.COMMEND_BANNER, EthicsTypeUtil.COMMEND_LETTER,
                EthicsTypeUtil.COMMEND_NOTICE, EthicsTypeUtil.COMMEND_HONOR,
                EthicsTypeUtil.COMMEND_OTHER)) {
            byType.put(t, all.stream().filter(c -> t.equals(c.getCommendType())).count());
        }
        r.put("byType", byType);
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (String l : List.of(EthicsTypeUtil.LEVEL_HOSPITAL, EthicsTypeUtil.LEVEL_CITY,
                EthicsTypeUtil.LEVEL_PROVINCE, EthicsTypeUtil.LEVEL_NATIONAL)) {
            byLevel.put(l, all.stream().filter(c -> l.equals(c.getCommendLevel())).count());
        }
        r.put("byLevel", byLevel);
        r.put("byDept", statsByDept());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByDept() {
        Map<String, List<EthicsCommendation>> grouped = new LinkedHashMap<>();
        for (EthicsCommendation c : this.list()) {
            String dept = StringUtils.hasText(c.getDeptName()) ? c.getDeptName() : "未分科室";
            grouped.computeIfAbsent(dept, k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((dept, list) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deptName", dept);
            row.put("count", (long) list.size());
            row.put("passedCount", list.stream()
                    .filter(c -> Integer.valueOf(EthicsTypeUtil.APPROVAL_PASSED).equals(c.getStatus())).count());
            result.add(row);
        });
        result.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return result;
    }

    @Override
    public EthicsCommendation detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EthicsCommendation approve(Long id, ApproveRequest request) {
        EthicsCommendation c = getOrThrow(id);
        if (!Integer.valueOf(EthicsTypeUtil.APPROVAL_PENDING).equals(c.getStatus())) {
            throw new IllegalStateException("只有待审批的表彰才能通过（当前状态："
                    + EthicsTypeUtil.approvalStatusText(c.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getApprover())) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        c.setApprover(request.getApprover().trim());
        c.setStatus(EthicsTypeUtil.APPROVAL_PASSED);
        String opinion = StringUtils.hasText(request.getOpinion()) ? request.getOpinion().trim() : "同意";
        c.setApprovalHistory(appendHistory(c.getApprovalHistory(),
                "医务科 " + c.getApprover() + "：通过（" + opinion + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”守卫
        super.updateById(c);
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EthicsCommendation reject(Long id, RejectRequest request) {
        EthicsCommendation c = getOrThrow(id);
        if (!Integer.valueOf(EthicsTypeUtil.APPROVAL_PENDING).equals(c.getStatus())) {
            throw new IllegalStateException("只有待审批的表彰才能驳回（当前状态："
                    + EthicsTypeUtil.approvalStatusText(c.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getApprover())) {
            throw new IllegalArgumentException("审批人不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }
        c.setApprover(request.getApprover().trim());
        c.setStatus(EthicsTypeUtil.APPROVAL_REJECTED);
        c.setApprovalHistory(appendHistory(c.getApprovalHistory(),
                "医务科 " + c.getApprover() + "：驳回（" + request.getReason().trim() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有待审批能修改”守卫
        super.updateById(c);
        return c;
    }

    /** 删除：已通过的表彰是正式记录，不能直接删除 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        EthicsCommendation c = getOrThrow((Long) id);
        if (Integer.valueOf(EthicsTypeUtil.APPROVAL_PASSED).equals(c.getStatus())) {
            throw new IllegalStateException("已通过的表彰不能直接删除");
        }
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

    @Override
    public void exportExcel(EthicsCommendationQuery query, OutputStream out) {
        EthicsCommendationExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<EthicsCommendation> list = EthicsCommendationExcelUtil.read(in);
        int ok = 0;
        for (EthicsCommendation c : list) {
            if (!StringUtils.hasText(c.getPhysicianName())) {
                log.warn("导入跳过：受表彰医师姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(c.getTitle())) {
                log.warn("导入跳过：表彰标题为空的行（医师 {}）", c.getPhysicianName());
                continue;
            }
            if (!StringUtils.hasText(c.getCommendType()) || !EthicsTypeUtil.isValidCommendType(c.getCommendType())) {
                log.warn("导入跳过：表彰类型为空或不正确（医师 {}）", c.getPhysicianName());
                continue;
            }
            if (!StringUtils.hasText(c.getCommendLevel()) || !EthicsTypeUtil.isValidCommendLevel(c.getCommendLevel())) {
                log.warn("导入跳过：表彰级别为空或不正确（医师 {}），默认院级", c.getPhysicianName());
                c.setCommendLevel(EthicsTypeUtil.LEVEL_HOSPITAL);
            }
            // 同一医师同一表彰标题同一表彰日期视为重复，跳过
            LambdaQueryWrapper<EthicsCommendation> qw = new LambdaQueryWrapper<EthicsCommendation>()
                    .eq(EthicsCommendation::getPhysicianName, c.getPhysicianName())
                    .eq(EthicsCommendation::getTitle, c.getTitle());
            if (c.getCommendDate() != null) {
                qw.eq(EthicsCommendation::getCommendDate, c.getCommendDate());
            }
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复表彰 {} {}", c.getPhysicianName(), c.getTitle());
                continue;
            }
            // 按姓名查医师档案补全医师 id / 科室；查不到的跳过
            if (c.getPhysicianId() == null) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, c.getPhysicianName()).last("LIMIT 1"));
                if (physician == null) {
                    log.warn("导入跳过：医师 {} 在医师档案中不存在", c.getPhysicianName());
                    continue;
                }
                c.setPhysicianId(physician.getId());
                if (c.getDeptId() == null) {
                    c.setDeptId(physician.getDeptId());
                }
                if (!StringUtils.hasText(c.getDeptName())) {
                    c.setDeptName(physician.getDeptName());
                }
            }
            // 导入的均为待审批，需走审批流程
            c.setStatus(EthicsTypeUtil.APPROVAL_PENDING);
            c.setApprover(null);
            c.setApprovalHistory(null);
            if (c.getCommendDate() == null) {
                c.setCommendDate(LocalDate.now());
            }
            try {
                this.save(c);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", c.getPhysicianName(), e.getMessage());
            }
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<EthicsCommendation> buildWrapper(EthicsCommendationQuery q) {
        LambdaQueryWrapper<EthicsCommendation> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(EthicsCommendation::getPhysicianName, q.getKeyword())
                .or().like(EthicsCommendation::getTitle, q.getKeyword()));
        qw.eq(q.getDeptId() != null, EthicsCommendation::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getCommendType()), EthicsCommendation::getCommendType, q.getCommendType());
        qw.eq(StringUtils.hasText(q.getCommendLevel()), EthicsCommendation::getCommendLevel, q.getCommendLevel());
        qw.eq(q.getStatus() != null, EthicsCommendation::getStatus, q.getStatus());
        qw.orderByDesc(EthicsCommendation::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验表彰类型与级别 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EthicsCommendation entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(EthicsCommendation entity) {
        // 只有待审批的表彰能修改：已通过/已驳回的表彰不可再改
        EthicsCommendation old = getOrThrow(entity.getId());
        if (!Integer.valueOf(EthicsTypeUtil.APPROVAL_PENDING).equals(old.getStatus())) {
            throw new IllegalStateException("只有待审批的表彰才能修改（当前状态："
                    + EthicsTypeUtil.approvalStatusText(old.getStatus()) + "）");
        }
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(EthicsCommendation c, boolean isCreate) {
        if (c.getPhysicianId() == null && !StringUtils.hasText(c.getPhysicianName())) {
            throw new IllegalArgumentException("受表彰医师不能为空");
        }
        if (!StringUtils.hasText(c.getCommendType()) || !EthicsTypeUtil.isValidCommendType(c.getCommendType())) {
            throw new IllegalArgumentException("表彰类型不正确");
        }
        if (!StringUtils.hasText(c.getCommendLevel()) || !EthicsTypeUtil.isValidCommendLevel(c.getCommendLevel())) {
            throw new IllegalArgumentException("表彰级别不正确");
        }
        if (isCreate) {
            if (c.getStatus() == null) {
                c.setStatus(EthicsTypeUtil.APPROVAL_PENDING);
            }
            if (c.getCommendDate() == null) {
                c.setCommendDate(LocalDate.now());
            }
            // 按姓名查医师档案补全医师 id / 科室
            if (c.getPhysicianId() == null && StringUtils.hasText(c.getPhysicianName())) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, c.getPhysicianName()).last("LIMIT 1"));
                if (physician != null) {
                    c.setPhysicianId(physician.getId());
                    if (c.getDeptId() == null) {
                        c.setDeptId(physician.getDeptId());
                    }
                    if (!StringUtils.hasText(c.getDeptName())) {
                        c.setDeptName(physician.getDeptName());
                    }
                }
            }
        }
    }

    private String appendHistory(String history, String entry) {
        if (!StringUtils.hasText(history)) {
            return entry;
        }
        return history + "；" + entry;
    }

    private EthicsCommendation getOrThrow(Long id) {
        EthicsCommendation c = this.getById(id);
        if (c == null) {
            throw new IllegalArgumentException("医德医风表彰不存在：" + id);
        }
        return c;
    }
}
