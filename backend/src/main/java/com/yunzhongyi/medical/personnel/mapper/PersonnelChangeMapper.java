package com.yunzhongyi.medical.personnel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.personnel.entity.PersonnelChange;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医疗人员动态（人员变动）Mapper
 */
@Mapper
public interface PersonnelChangeMapper extends BaseMapper<PersonnelChange> {
}
