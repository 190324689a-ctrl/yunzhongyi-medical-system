package com.yunzhongyi.medical.physician.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.physician.entity.Physician;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医师档案 Mapper
 */
@Mapper
public interface PhysicianMapper extends BaseMapper<Physician> {
}
