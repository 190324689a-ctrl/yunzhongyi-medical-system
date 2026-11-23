package com.yunzhongyi.medical.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.system.entity.SysOperLog;

/**
 * 操作日志服务
 */
public interface SysOperLogService extends IService<SysOperLog> {

    /** 记录一条操作日志（失败也不影响主流程） */
    void record(String username, String module, String operation, String method, String params, boolean success, long costMs, String errorMsg);
}
