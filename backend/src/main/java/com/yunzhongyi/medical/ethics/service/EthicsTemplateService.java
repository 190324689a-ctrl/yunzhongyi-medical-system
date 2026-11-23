package com.yunzhongyi.medical.ethics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.ethics.dto.EthicsTemplateQuery;
import com.yunzhongyi.medical.ethics.entity.EthicsTemplate;

import java.util.List;

/** 医德考评模板服务：自定义模板 + 权重合计 100 校验 */
public interface EthicsTemplateService extends IService<EthicsTemplate> {

    PageResult<EthicsTemplate> pageQuery(EthicsTemplateQuery query);

    /** 全部启用的模板（新建医德考评记录时下拉用） */
    List<EthicsTemplate> listEnabled();
}
