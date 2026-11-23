package com.yunzhongyi.medical.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunzhongyi.medical.authorization.entity.Authorization;
import org.apache.ibatis.annotations.Mapper;

/**
 * 重点授权申请 Mapper
 */
@Mapper
public interface AuthorizationMapper extends BaseMapper<Authorization> {
}
