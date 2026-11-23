package com.yunzhongyi.medical.coresystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.coresystem.dto.CoreSystemReadingQuery;
import com.yunzhongyi.medical.coresystem.entity.CoreSystem;
import com.yunzhongyi.medical.coresystem.entity.CoreSystemReading;
import com.yunzhongyi.medical.coresystem.mapper.CoreSystemMapper;
import com.yunzhongyi.medical.coresystem.mapper.CoreSystemReadingMapper;
import com.yunzhongyi.medical.coresystem.service.CoreSystemReadingService;
import com.yunzhongyi.medical.coresystem.service.CoreSystemTypeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 核心制度查阅与学习记录服务实现
 * <p>
 * 制度存在性与发布状态直接读 CoreSystemMapper，不依赖 CoreSystemService，
 * 避免两个服务互相注入形成循环依赖。
 */
@Service
@RequiredArgsConstructor
public class CoreSystemReadingServiceImpl
        extends ServiceImpl<CoreSystemReadingMapper, CoreSystemReading>
        implements CoreSystemReadingService {

    private final CoreSystemMapper coreSystemMapper;

    @Override
    public PageResult<CoreSystemReading> pageQuery(CoreSystemReadingQuery query) {
        Page<CoreSystemReading> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<CoreSystemReading> listByCoreSystem(Long coreSystemId) {
        return this.list(new LambdaQueryWrapper<CoreSystemReading>()
                .eq(CoreSystemReading::getCoreSystemId, coreSystemId)
                .orderByDesc(CoreSystemReading::getReadDate)
                .orderByDesc(CoreSystemReading::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoreSystemReading create(CoreSystemReading reading) {
        if (reading.getCoreSystemId() == null) {
            throw new IllegalArgumentException("制度不能为空");
        }
        CoreSystem coreSystem = coreSystemMapper.selectById(reading.getCoreSystemId());
        if (coreSystem == null) {
            throw new IllegalArgumentException("制度不存在：" + reading.getCoreSystemId());
        }
        if (!Integer.valueOf(CoreSystemTypeUtil.STATUS_PUBLISHED).equals(coreSystem.getStatus())) {
            throw new IllegalStateException("只有已发布的制度才能登记查阅学习（当前状态："
                    + CoreSystemTypeUtil.statusText(coreSystem.getStatus()) + "）");
        }
        if (!StringUtils.hasText(reading.getReaderName())) {
            throw new IllegalArgumentException("查阅人姓名不能为空");
        }
        reading.setReaderName(reading.getReaderName().trim());
        if (reading.getReadDate() == null) {
            reading.setReadDate(LocalDate.now());
        }
        if (reading.getLearned() == null) {
            reading.setLearned(0);
        }
        if (!Integer.valueOf(0).equals(reading.getLearned())
                && !Integer.valueOf(1).equals(reading.getLearned())) {
            throw new IllegalArgumentException("是否已学习不正确（0 已查阅 / 1 已学习）");
        }
        reading.setCoreSystemTitle(coreSystem.getTitle());
        // 同一人同一制度同一天只登记一次
        if (this.count(new LambdaQueryWrapper<CoreSystemReading>()
                .eq(CoreSystemReading::getCoreSystemId, reading.getCoreSystemId())
                .eq(CoreSystemReading::getReaderName, reading.getReaderName())
                .eq(CoreSystemReading::getReadDate, reading.getReadDate())) > 0) {
            throw new IllegalArgumentException(reading.getReaderName() + " 已于 "
                    + reading.getReadDate() + " 登记过该制度的查阅学习，请勿重复登记");
        }
        this.save(reading);
        return reading;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoreSystemReading markLearned(Long id) {
        CoreSystemReading r = getOrThrow(id);
        if (Integer.valueOf(1).equals(r.getLearned())) {
            throw new IllegalStateException("该记录已标记为已学习");
        }
        r.setLearned(1);
        super.updateById(r);
        return r;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReading(Long id) {
        getOrThrow(id);
        return super.removeById(id);
    }

    private LambdaQueryWrapper<CoreSystemReading> buildWrapper(CoreSystemReadingQuery q) {
        LambdaQueryWrapper<CoreSystemReading> qw = new LambdaQueryWrapper<>();
        qw.eq(q.getCoreSystemId() != null, CoreSystemReading::getCoreSystemId, q.getCoreSystemId());
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(CoreSystemReading::getReaderName, q.getKeyword())
                .or().like(CoreSystemReading::getDeptName, q.getKeyword()));
        qw.eq(q.getLearned() != null, CoreSystemReading::getLearned, q.getLearned());
        qw.orderByDesc(CoreSystemReading::getReadDate);
        qw.orderByDesc(CoreSystemReading::getId);
        return qw;
    }

    private CoreSystemReading getOrThrow(Long id) {
        CoreSystemReading r = this.getById(id);
        if (r == null) {
            throw new IllegalArgumentException("查阅学习记录不存在：" + id);
        }
        return r;
    }
}
