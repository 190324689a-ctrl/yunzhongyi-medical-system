package com.yunzhongyi.medical.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.system.entity.SysDictData;

import java.util.List;

/**
 * 字典服务
 */
public interface SysDictService extends IService<SysDictData> {

    /** 按字典编码取启用数据项（下拉框用） */
    List<SysDictData> listByCode(String dictCode);
}
