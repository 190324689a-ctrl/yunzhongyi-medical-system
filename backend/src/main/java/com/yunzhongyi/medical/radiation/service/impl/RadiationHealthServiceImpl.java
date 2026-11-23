package com.yunzhongyi.medical.radiation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.radiation.dto.RadiationHealthQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationHealth;
import com.yunzhongyi.medical.radiation.mapper.RadiationHealthMapper;
import com.yunzhongyi.medical.radiation.service.RadiationHealthService;
import com.yunzhongyi.medical.radiation.service.RadiationTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 放射人员健康管理服务实现：体检记录 CRUD
 */
@Slf4j
@Service
public class RadiationHealthServiceImpl
        extends ServiceImpl<RadiationHealthMapper, RadiationHealth>
        implements RadiationHealthService {

    @Override
    public PageResult<RadiationHealth> pageQuery(RadiationHealthQuery query) {
        Page<RadiationHealth> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<RadiationHealth> listByPersonnel(Long personnelId) {
        return this.list(new LambdaQueryWrapper<RadiationHealth>()
                .eq(RadiationHealth::getPersonnelId, personnelId)
                .orderByDesc(RadiationHealth::getExamDate)
                .orderByDesc(RadiationHealth::getId));
    }

    @Override
    public RadiationHealth detail(Long id) {
        RadiationHealth h = this.getById(id);
        if (h == null) {
            throw new IllegalArgumentException("体检记录不存在：" + id);
        }
        return h;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<RadiationHealth> buildWrapper(RadiationHealthQuery q) {
        LambdaQueryWrapper<RadiationHealth> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(q.getKeyword()), RadiationHealth::getPersonnelName, q.getKeyword());
        qw.eq(q.getPersonnelId() != null, RadiationHealth::getPersonnelId, q.getPersonnelId());
        qw.eq(StringUtils.hasText(q.getExamType()), RadiationHealth::getExamType, q.getExamType());
        qw.eq(StringUtils.hasText(q.getResult()), RadiationHealth::getResult, q.getResult());
        if (Boolean.TRUE.equals(q.getOverLimit())) {
            // 年有效剂量 > 20 mSv 即超标
            qw.gt(RadiationHealth::getDose, RadiationTypeUtil.DOSE_LIMIT_MSV);
        }
        qw.orderByDesc(RadiationHealth::getExamDate).orderByDesc(RadiationHealth::getId);
        return qw;
    }

    /** 新建/更新时校验：体检类型、体检结论、剂量、重复登记 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(RadiationHealth entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(RadiationHealth entity) {
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(RadiationHealth h, boolean isCreate) {
        if (!StringUtils.hasText(h.getExamType()) || !RadiationTypeUtil.isValidExamType(h.getExamType())) {
            throw new IllegalArgumentException("体检类型不正确");
        }
        if (h.getExamDate() == null) {
            throw new IllegalArgumentException("体检日期不能为空");
        }
        if (StringUtils.hasText(h.getResult()) && !RadiationTypeUtil.isValidExamResult(h.getResult())) {
            throw new IllegalArgumentException("体检结论不正确");
        }
        if (h.getDose() != null && h.getDose().signum() < 0) {
            throw new IllegalArgumentException("个人受照剂量不能为负");
        }
        if (h.getNextExamDate() != null && h.getExamDate() != null
                && h.getNextExamDate().isBefore(h.getExamDate())) {
            throw new IllegalArgumentException("下次体检日期不能早于本次体检日期");
        }
        if (isCreate) {
            if (!StringUtils.hasText(h.getResult())) {
                h.setResult(RadiationTypeUtil.EXAM_FIT);
            }
            // 同一人员同一体检类型同一体检日期视为重复，拒绝登记
            LambdaQueryWrapper<RadiationHealth> qw = new LambdaQueryWrapper<RadiationHealth>()
                    .eq(RadiationHealth::getPersonnelId, h.getPersonnelId())
                    .eq(RadiationHealth::getExamType, h.getExamType())
                    .eq(RadiationHealth::getExamDate, h.getExamDate());
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("该人员同一体检类型同一日期已有记录，请勿重复登记");
            }
            // 剂量超标时记日志提醒复查调岗
            if (RadiationTypeUtil.isDoseOverLimit(h.getDose())) {
                log.warn("放射人员 {} 体检个人受照剂量 {} mSv 超过年有效剂量限值 20 mSv，请安排复查调岗",
                        h.getPersonnelName(), h.getDose());
            }
        }
    }
}
