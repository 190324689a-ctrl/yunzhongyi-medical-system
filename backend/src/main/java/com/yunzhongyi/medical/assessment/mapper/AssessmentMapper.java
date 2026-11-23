package com.yunzhongyi.medical.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.assessment.entity.Assessment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医师考核记录 Mapper
 */
@Mapper
public interface AssessmentMapper extends BaseMapper<Assessment> {
}
