package com.yunzhongyi.medical.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.schedule.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排班记录 Mapper
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
}
