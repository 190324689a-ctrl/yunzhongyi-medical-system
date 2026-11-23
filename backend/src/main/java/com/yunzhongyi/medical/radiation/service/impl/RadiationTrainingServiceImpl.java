package com.yunzhongyi.medical.radiation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.radiation.dto.RadiationTrainingQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationTraining;
import com.yunzhongyi.medical.radiation.mapper.RadiationTrainingMapper;
import com.yunzhongyi.medical.radiation.service.RadiationTrainingService;
import com.yunzhongyi.medical.radiation.service.RadiationTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 放射人员培训管理服务实现：培训记录 CRUD
 */
@Slf4j
@Service
public class RadiationTrainingServiceImpl
        extends ServiceImpl<RadiationTrainingMapper, RadiationTraining>
        implements RadiationTrainingService {

    @Override
    public PageResult<RadiationTraining> pageQuery(RadiationTrainingQuery query) {
        Page<RadiationTraining> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<RadiationTraining> listByPersonnel(Long personnelId) {
        return this.list(new LambdaQueryWrapper<RadiationTraining>()
                .eq(RadiationTraining::getPersonnelId, personnelId)
                .orderByDesc(RadiationTraining::getTrainingDate)
                .orderByDesc(RadiationTraining::getId));
    }

    @Override
    public RadiationTraining detail(Long id) {
        RadiationTraining t = this.getById(id);
        if (t == null) {
            throw new IllegalArgumentException("培训记录不存在：" + id);
        }
        return t;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<RadiationTraining> buildWrapper(RadiationTrainingQuery q) {
        LambdaQueryWrapper<RadiationTraining> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(RadiationTraining::getPersonnelName, q.getKeyword())
                .or().like(RadiationTraining::getTrainingName, q.getKeyword()));
        qw.eq(q.getPersonnelId() != null, RadiationTraining::getPersonnelId, q.getPersonnelId());
        qw.eq(StringUtils.hasText(q.getTrainingType()), RadiationTraining::getTrainingType, q.getTrainingType());
        qw.eq(StringUtils.hasText(q.getResult()), RadiationTraining::getResult, q.getResult());
        qw.orderByDesc(RadiationTraining::getTrainingDate).orderByDesc(RadiationTraining::getId);
        return qw;
    }

    /** 新建/更新时校验：培训类型、培训结果、学时、重复登记 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(RadiationTraining entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(RadiationTraining entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(RadiationTraining t, boolean isCreate) {
        if (!StringUtils.hasText(t.getTrainingType()) || !RadiationTypeUtil.isValidTrainingType(t.getTrainingType())) {
            throw new IllegalArgumentException("培训类型不正确");
        }
        if (t.getTrainingDate() == null) {
            throw new IllegalArgumentException("培训日期不能为空");
        }
        if (StringUtils.hasText(t.getResult()) && !RadiationTypeUtil.isValidTrainingResult(t.getResult())) {
            throw new IllegalArgumentException("培训结果不正确");
        }
        if (t.getHours() != null && t.getHours() < 0) {
            throw new IllegalArgumentException("培训学时不能为负");
        }
        if (isCreate) {
            if (!StringUtils.hasText(t.getResult())) {
                t.setResult(RadiationTypeUtil.TRAINING_PASS);
            }
            // 同一人员同一培训名称同一培训日期视为重复，拒绝登记
            LambdaQueryWrapper<RadiationTraining> qw = new LambdaQueryWrapper<RadiationTraining>()
                    .eq(RadiationTraining::getPersonnelId, t.getPersonnelId())
                    .eq(RadiationTraining::getTrainingName, t.getTrainingName())
                    .eq(RadiationTraining::getTrainingDate, t.getTrainingDate());
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("该人员同一培训同一日期已有记录，请勿重复登记");
            }
        }
    }
}
