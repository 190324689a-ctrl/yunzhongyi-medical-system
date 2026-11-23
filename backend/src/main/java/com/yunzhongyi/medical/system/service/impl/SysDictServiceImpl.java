package com.yunzhongyi.medical.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.system.entity.SysDictData;
import com.yunzhongyi.medical.system.mapper.SysDictDataMapper;
import com.yunzhongyi.medical.system.service.SysDictService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典服务实现
 */
@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictService {

    @Override
    public List<SysDictData> listByCode(String dictCode) {
        return this.list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictCode, dictCode)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSortOrder).orderByAsc(SysDictData::getId));
    }
}
