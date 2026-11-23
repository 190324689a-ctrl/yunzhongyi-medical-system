package com.yunzhongyi.medical.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.schedule.dto.ScheduleTemplateQuery;
import com.yunzhongyi.medical.schedule.entity.ScheduleTemplate;

import java.util.List;

/**
 * 排班模板服务
 */
public interface ScheduleTemplateService extends IService<ScheduleTemplate> {

    /** 多条件组合分页查询 */
    PageResult<ScheduleTemplate> pageQuery(ScheduleTemplateQuery query);

    /** 全部启用的模板（批量生成排班时下拉用） */
    List<ScheduleTemplate> listEnabled();
}
