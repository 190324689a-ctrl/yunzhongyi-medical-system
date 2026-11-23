package com.yunzhongyi.medical.committee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.committee.dto.CommitteeMeetingQuery;
import com.yunzhongyi.medical.committee.entity.CommitteeMeeting;

import java.util.List;

/**
 * 委员会会议记录服务
 */
public interface CommitteeMeetingService extends IService<CommitteeMeeting> {

    /** 多条件组合分页查询 */
    PageResult<CommitteeMeeting> pageQuery(CommitteeMeetingQuery query);

    /** 查某委员会的全部会议（按会议日期降序） */
    List<CommitteeMeeting> listByCommittee(Long committeeId);

    /** 详情 */
    CommitteeMeeting detail(Long id);
}
