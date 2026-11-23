package com.yunzhongyi.medical.quality.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.quality.dto.QualityTemplateQuery;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;

import java.util.List;

/**
 * 质量检查模板管理服务
 */
public interface QualityTemplateService extends IService<QualityTemplate> {

    /** 多条件组合分页查询 */
    PageResult<QualityTemplate> pageQuery(QualityTemplateQuery query);

    /** 全部启用的模板（新建检查计划时下拉用） */
    List<QualityTemplate> listEnabled();

    /** 启用模板 */
    QualityTemplate enable(Long id);

    /** 停用模板 */
    QualityTemplate disable(Long id);
}
