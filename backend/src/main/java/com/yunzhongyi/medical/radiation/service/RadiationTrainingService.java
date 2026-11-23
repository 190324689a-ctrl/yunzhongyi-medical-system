package com.yunzhongyi.medical.radiation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.radiation.dto.RadiationTrainingQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationTraining;

import java.util.List;

/**
 * 放射人员培训管理服务
 */
public interface RadiationTrainingService extends IService<RadiationTraining> {

    /** 多条件组合分页查询 */
    PageResult<RadiationTraining> pageQuery(RadiationTrainingQuery query);

    /** 查某放射人员的全部培训记录（按培训日期降序） */
    List<RadiationTraining> listByPersonnel(Long personnelId);

    /** 详情 */
    RadiationTraining detail(Long id);
}
