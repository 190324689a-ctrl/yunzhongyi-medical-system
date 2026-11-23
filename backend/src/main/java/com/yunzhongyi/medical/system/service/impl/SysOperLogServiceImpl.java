package com.yunzhongyi.medical.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.system.entity.SysOperLog;
import com.yunzhongyi.medical.system.mapper.SysOperLogMapper;
import com.yunzhongyi.medical.system.service.SysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Override
    public void record(String username, String module, String operation, String method,
                       String params, boolean success, long costMs, String errorMsg) {
        try {
            SysOperLog log = new SysOperLog();
            log.setUsername(username);
            log.setModule(module);
            log.setOperation(operation);
            log.setMethod(method);
            log.setParams(params != null && params.length() > 2000 ? params.substring(0, 2000) : params);
            log.setResult(success ? 1 : 0);
            log.setCostMs(costMs);
            log.setErrorMsg(errorMsg);
            this.save(log);
        } catch (Exception e) {
            log.warn("写操作日志失败: {}", e.getMessage());
        }
    }
}
