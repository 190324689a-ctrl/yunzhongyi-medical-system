package com.yunzhongyi.medical.newtech.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.newtech.entity.NewTechRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新技术、新项目进展记录 Mapper
 */
@Mapper
public interface NewTechRecordMapper extends BaseMapper<NewTechRecord> {
}
