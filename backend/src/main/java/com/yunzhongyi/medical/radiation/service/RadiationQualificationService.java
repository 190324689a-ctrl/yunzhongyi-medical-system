package com.yunzhongyi.medical.radiation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.radiation.dto.RadiationQualificationQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationQualification;

import java.util.List;

/**
 * 放射人员资质管理服务
 */
public interface RadiationQualificationService extends IService<RadiationQualification> {

    /** 多条件组合分页查询 */
    PageResult<RadiationQualification> pageQuery(RadiationQualificationQuery query);

    /** 查某放射人员的全部资质（按有效期升序，到期在前） */
    List<RadiationQualification> listByPersonnel(Long personnelId);

    /** 详情 */
    RadiationQualification detail(Long id);

    /**
     * 一级审核通过（医务科）：只有待审核的资质能通过，审核记录留痕
     */
    RadiationQualification approve(Long id, String approver, String opinion);

    /** 一级审核驳回（医务科）：只有待审核的资质能驳回，审核记录留痕 */
    RadiationQualification reject(Long id, String approver, String opinion);
}
