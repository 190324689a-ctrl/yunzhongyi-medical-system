package com.yunzhongyi.medical.mdt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.mdt.entity.MdtRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * MDT 会诊记录 Mapper
 */
@Mapper
public interface MdtRecordMapper extends BaseMapper<MdtRecord> {
}
