package com.yunzhongyi.medical.authorization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.authorization.dto.AuthorizationQuery;
import com.yunzhongyi.medical.authorization.entity.Authorization;
import com.yunzhongyi.medical.authorization.excel.AuthorizationExcelUtil;
import com.yunzhongyi.medical.authorization.mapper.AuthorizationMapper;
import com.yunzhongyi.medical.authorization.service.AuthorizationService;
import com.yunzhongyi.medical.common.PageResult;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 重点授权服务实现：在线申请 + 多级审批（科主任 → 医务科）+ 统计 + Excel 批量导入导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl
        extends ServiceImpl<AuthorizationMapper, Authorization>
        implements AuthorizationService {

    /** Excel 里只有申请人姓名没有医师 id，导入时按姓名查医师档案补全 */
    private final PhysicianMapper physicianMapper;

    @Override
    public PageResult<Authorization> pageQuery(AuthorizationQuery query) {
        Page<Authorization> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<Authorization> listByQuery(AuthorizationQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", this.count());
        r.put("pendingDept", this.count(new LambdaQueryWrapper<Authorization>().eq(Authorization::getStatus, 0)));
        r.put("pendingMedical", this.count(new LambdaQueryWrapper<Authorization>().eq(Authorization::getStatus, 1)));
        r.put("pending", this.count(new LambdaQueryWrapper<Authorization>().in(Authorization::getStatus, List.of(0, 1))));
        r.put("approved", this.count(new LambdaQueryWrapper<Authorization>().eq(Authorization::getStatus, 2)));
        r.put("rejected", this.count(new LambdaQueryWrapper<Authorization>().eq(Authorization::getStatus, 3)));
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of("SURGERY", "ANESTHESIA", "ANTITUMOR", "PSYCHOTROPIC")) {
            byType.put(type, this.count(new LambdaQueryWrapper<Authorization>().eq(Authorization::getAuthType, type)));
        }
        r.put("byType", byType);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Authorization approve(Long id, String approver, String opinion) {
        Authorization a = getOrThrow(id);
        if (a.getStatus() == null || a.getStatus() == 2 || a.getStatus() == 3) {
            throw new IllegalStateException("该申请已结束审批，不能再通过");
        }
        String step = a.getStatus() == 0 ? "科主任" : "医务科";
        appendHistory(a, step, approver, "通过", opinion);
        a.setStatus(a.getStatus() == 0 ? 1 : 2);
        a.setCurrentApprover(a.getStatus() == 1 ? "医务科" : null);
        a.setApprovalOpinion(opinion);
        this.updateById(a);
        return a;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Authorization reject(Long id, String approver, String opinion) {
        Authorization a = getOrThrow(id);
        if (a.getStatus() == null || a.getStatus() == 2 || a.getStatus() == 3) {
            throw new IllegalStateException("该申请已结束审批，不能再驳回");
        }
        String step = a.getStatus() == 0 ? "科主任" : "医务科";
        appendHistory(a, step, approver, "驳回", opinion);
        a.setStatus(3);
        a.setCurrentApprover(null);
        a.setApprovalOpinion(opinion);
        this.updateById(a);
        return a;
    }

    @Override
    public void exportExcel(AuthorizationQuery query, OutputStream out) {
        AuthorizationExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<Authorization> list = AuthorizationExcelUtil.read(in);
        int ok = 0;
        for (Authorization a : list) {
            if (!StringUtils.hasText(a.getPhysicianName())) {
                log.warn("导入跳过：申请人姓名为空的行");
                continue;
            }
            if (!StringUtils.hasText(a.getAuthType())) {
                log.warn("导入跳过：授权类型为空的行（申请人 {}）", a.getPhysicianName());
                continue;
            }
            // 同一医师同一授权类型视为重复，跳过
            LambdaQueryWrapper<Authorization> qw = new LambdaQueryWrapper<Authorization>()
                    .eq(Authorization::getPhysicianName, a.getPhysicianName())
                    .eq(Authorization::getAuthType, a.getAuthType());
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复授权 {} {}", a.getPhysicianName(), a.getAuthType());
                continue;
            }
            // Excel 里没有医师 id，按姓名查医师档案补全（申请人、科室）；查不到的跳过
            if (a.getPhysicianId() == null) {
                Physician physician = physicianMapper.selectOne(new LambdaQueryWrapper<Physician>()
                        .eq(Physician::getName, a.getPhysicianName()).last("LIMIT 1"));
                if (physician == null) {
                    log.warn("导入跳过：申请人 {} 在医师档案中不存在", a.getPhysicianName());
                    continue;
                }
                a.setPhysicianId(physician.getId());
                if (a.getDeptId() == null) {
                    a.setDeptId(physician.getDeptId());
                }
                if (!StringUtils.hasText(a.getDeptName())) {
                    a.setDeptName(physician.getDeptName());
                }
            }
            if (a.getStatus() == null) {
                a.setStatus(0);
            }
            if (a.getApplyDate() == null) {
                a.setApplyDate(LocalDate.now());
            }
            if (!StringUtils.hasText(a.getCurrentApprover())) {
                a.setCurrentApprover("科主任");
            }
            this.save(a);
            ok++;
        }
        return ok;
    }

    private Authorization getOrThrow(Long id) {
        Authorization a = this.getById(id);
        if (a == null) {
            throw new IllegalArgumentException("申请不存在：" + id);
        }
        return a;
    }

    private void appendHistory(Authorization a, String step, String approver, String action, String opinion) {
        // 前端审批人默认就填“科主任 / 医务科”，与环节名相同时不重复写
        String who = StringUtils.hasText(approver) && !approver.equals(step) ? " " + approver : "";
        String line = step + who + "：" + action
                + (StringUtils.hasText(opinion) ? "（" + opinion + "）" : "");
        a.setApprovalHistory(StringUtils.hasText(a.getApprovalHistory())
                ? a.getApprovalHistory() + "；" + line : line);
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<Authorization> buildWrapper(AuthorizationQuery q) {
        LambdaQueryWrapper<Authorization> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.like(Authorization::getPhysicianName, q.getKeyword());
        }
        qw.eq(q.getDeptId() != null, Authorization::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getAuthType()), Authorization::getAuthType, q.getAuthType());
        qw.eq(q.getStatus() != null, Authorization::getStatus, q.getStatus());
        qw.orderByDesc(Authorization::getId);
        return qw;
    }

    /** 新建时补默认值并校验有效期；更新时同样校验 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Authorization entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Authorization entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(Authorization a, boolean isCreate) {
        if (a.getValidFrom() != null && a.getValidTo() != null && a.getValidTo().isBefore(a.getValidFrom())) {
            throw new IllegalArgumentException("授权有效期结束日期不能早于开始日期");
        }
        if (isCreate) {
            if (a.getStatus() == null) {
                a.setStatus(0);
            }
            if (a.getApplyDate() == null) {
                a.setApplyDate(LocalDate.now());
            }
            if (!StringUtils.hasText(a.getCurrentApprover())) {
                a.setCurrentApprover("科主任");
            }
        }
    }
}
