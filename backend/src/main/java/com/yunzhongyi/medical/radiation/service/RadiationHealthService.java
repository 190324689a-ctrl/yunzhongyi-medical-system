package com.yunzhongyi.medical.radiation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.radiation.dto.RadiationHealthQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationHealth;

import java.util.List;

/**
 * 放射人员健康管理服务
 */
public interface RadiationHealthService extends IService<RadiationHealth> {

    /** 多条件组合分页查询 */
    PageResult<RadiationHealth> pageQuery(RadiationHealthQuery query);

    /** 查某放射人员的全部体检记录（按体检日期降序） */
    List<RadiationHealth> listByPersonnel(Long personnelId);

    /** 详情 */
    RadiationHealth detail(Long id);
}
