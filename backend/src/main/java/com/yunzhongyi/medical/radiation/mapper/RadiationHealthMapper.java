package com.yunzhongyi.medical.radiation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.radiation.entity.RadiationHealth;
import org.apache.ibatis.annotations.Mapper;

/** 放射人员健康检查记录 Mapper */
@Mapper
public interface RadiationHealthMapper extends BaseMapper<RadiationHealth> {
}
