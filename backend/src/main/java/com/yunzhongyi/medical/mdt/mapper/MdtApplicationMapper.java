package com.yunzhongyi.medical.mdt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * MDT 会诊申请 Mapper
 */
@Mapper
public interface MdtApplicationMapper extends BaseMapper<MdtApplication> {
}
