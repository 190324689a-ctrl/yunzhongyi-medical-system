package com.yunzhongyi.medical.physician.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.physician.dto.PhysicianQuery;
import com.yunzhongyi.medical.physician.entity.Physician;
import com.yunzhongyi.medical.physician.excel.PhysicianExcelUtil;
import com.yunzhongyi.medical.physician.mapper.PhysicianMapper;
import com.yunzhongyi.medical.physician.service.PhysicianService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 医师档案服务实现
 */
@Slf4j
@Service
public class PhysicianServiceImpl extends ServiceImpl<PhysicianMapper, Physician> implements PhysicianService {

    @Override
    public PageResult<Physician> pageQuery(PhysicianQuery query) {
        Page<Physician> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<Physician> listByQuery(PhysicianQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public void exportExcel(PhysicianQuery query, OutputStream out) {
        PhysicianExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<Physician> list = PhysicianExcelUtil.read(in);
        int ok = 0;
        for (Physician p : list) {
            if (!StringUtils.hasText(p.getName())) {
                log.warn("导入跳过：姓名为空的行");
                continue;
            }
            // 同名同科室视为重复，跳过
            LambdaQueryWrapper<Physician> qw = new LambdaQueryWrapper<Physician>()
                    .eq(Physician::getName, p.getName())
                    .eq(p.getDeptId() != null, Physician::getDeptId, p.getDeptId());
            if (this.count(qw) > 0) {
                log.warn("导入跳过：重复医师 {}", p.getName());
                continue;
            }
            if (p.getStatus() == null) {
                p.setStatus(1);
            }
            this.save(p);
            ok++;
        }
        return ok;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<Physician> buildWrapper(PhysicianQuery q) {
        LambdaQueryWrapper<Physician> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            qw.and(w -> w.like(Physician::getName, q.getKeyword())
                    .or().like(Physician::getPracticeNo, q.getKeyword())
                    .or().like(Physician::getPhone, q.getKeyword()));
        }
        qw.eq(q.getDeptId() != null, Physician::getDeptId, q.getDeptId());
        qw.eq(StringUtils.hasText(q.getTitle()), Physician::getTitle, q.getTitle());
        qw.eq(StringUtils.hasText(q.getEducation()), Physician::getEducation, q.getEducation());
        qw.eq(StringUtils.hasText(q.getStaffType()), Physician::getStaffType, q.getStaffType());
        qw.eq(q.getSurgeryLevel() != null, Physician::getSurgeryLevel, q.getSurgeryLevel());
        qw.eq(q.getSurgeryAuth() != null, Physician::getSurgeryAuth, q.getSurgeryAuth());
        qw.eq(q.getAntitumorRx() != null, Physician::getAntitumorRx, q.getAntitumorRx());
        qw.eq(q.getAnesthesiaRx() != null, Physician::getAnesthesiaRx, q.getAnesthesiaRx());
        qw.eq(q.getPsychotropicRx() != null, Physician::getPsychotropicRx, q.getPsychotropicRx());
        qw.eq(q.getStatus() != null, Physician::getStatus, q.getStatus());
        qw.orderByDesc(Physician::getId);
        return qw;
    }
}
