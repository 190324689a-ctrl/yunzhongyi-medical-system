package com.yunzhongyi.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.assessment.dto.AssessmentTemplateQuery;
import com.yunzhongyi.medical.assessment.entity.AssessmentTemplate;
import com.yunzhongyi.medical.common.PageResult;

import java.util.List;

/**
 * 考核模板服务
 */
public interface AssessmentTemplateService extends IService<AssessmentTemplate> {

    /** 多条件组合分页查询 */
    PageResult<AssessmentTemplate> pageQuery(AssessmentTemplateQuery query);

    /** 全部启用的模板（新建考核记录时下拉用） */
    List<AssessmentTemplate> listEnabled();
}
