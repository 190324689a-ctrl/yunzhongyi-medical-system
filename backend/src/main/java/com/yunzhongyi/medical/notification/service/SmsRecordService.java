package com.yunzhongyi.medical.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.notification.dto.SmsRecordQuery;
import com.yunzhongyi.medical.notification.entity.SmsRecord;

import java.io.OutputStream;
import java.util.Map;

/**
 * 短信提醒服务：短信发送（预留占位）、重发、统计、导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 29 项）：短信提醒。
 * 短信网关本机没有真实服务：发送与重发均为预留占位调用，明确返回失败、
 * 如实记录（发送失败，失败原因写明网关未对接），绝不伪造发送成功。
 */
public interface SmsRecordService extends IService<SmsRecord> {

    /** 多条件组合分页查询 */
    PageResult<SmsRecord> pageQuery(SmsRecordQuery query);

    /** 详情 */
    SmsRecord detail(Long id);

    /**
     * 发送短信：手机号须合法，先建记录再尝试发送；本机无真实短信网关，调用返回
     * {record, success=false, message}，记录记为发送失败并写明失败原因。
     */
    Map<String, Object> send(SmsRecord record);

    /**
     * 重发：已发送成功不可重发；待发送与发送失败可重发，重发仍走预留占位、
     * 再次失败如实记录。
     */
    Map<String, Object> retry(Long id);

    /** 删除短信记录：已发送成功的记录不可直接删除 */
    void delete(Long id);

    /** 短信记录统计：总数、待发送/已发送/发送失败 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(SmsRecordQuery query, OutputStream out);
}
