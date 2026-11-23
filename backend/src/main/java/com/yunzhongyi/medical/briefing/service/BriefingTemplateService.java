package com.yunzhongyi.medical.briefing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.briefing.dto.BriefingTemplateQuery;
import com.yunzhongyi.medical.briefing.entity.BriefingTemplate;
import com.yunzhongyi.medical.common.PageResult;

import java.util.List;

public interface BriefingTemplateService extends IService<BriefingTemplate> {

    PageResult<BriefingTemplate> pageQuery(BriefingTemplateQuery query);

    BriefingTemplate detail(Long id);

    List<BriefingTemplate> enabledList();

    BriefingTemplate create(BriefingTemplate template);

    BriefingTemplate update(BriefingTemplate template);

    void delete(Long id);

    BriefingTemplate enable(Long id);

    BriefingTemplate disable(Long id);
}
