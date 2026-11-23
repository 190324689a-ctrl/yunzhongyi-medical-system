package com.yunzhongyi.medical.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.statistics.entity.WorkloadRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作量登记 Mapper
 */
@Mapper
public interface WorkloadRecordMapper extends BaseMapper<WorkloadRecord> {
}
