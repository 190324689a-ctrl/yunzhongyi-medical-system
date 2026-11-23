package com.yunzhongyi.medical.committee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.committee.dto.CommitteeResolutionQuery;
import com.yunzhongyi.medical.committee.entity.CommitteeResolution;

import java.util.List;

/**
 * 委员会决议服务
 */
public interface CommitteeResolutionService extends IService<CommitteeResolution> {

    /** 多条件组合分页查询 */
    PageResult<CommitteeResolution> pageQuery(CommitteeResolutionQuery query);

    /** 查某会议的全部决议（按决议期限升序） */
    List<CommitteeResolution> listByMeeting(Long meetingId);

    /** 详情 */
    CommitteeResolution detail(Long id);

    /** 开始执行：待执行 → 执行中 */
    CommitteeResolution start(Long id);

    /** 完成：执行中 → 已完成（完成日期默认为当天） */
    CommitteeResolution complete(Long id);

    /** 取消：待执行/执行中 → 已取消 */
    CommitteeResolution cancel(Long id);
}
