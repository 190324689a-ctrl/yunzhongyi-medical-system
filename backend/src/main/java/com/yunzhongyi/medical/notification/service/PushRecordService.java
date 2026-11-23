package com.yunzhongyi.medical.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.notification.dto.PushRecordQuery;
import com.yunzhongyi.medical.notification.entity.PushRecord;

import java.io.OutputStream;
import java.util.Map;

/**
 * 消息推送服务：推送发送（预留占位）、重推、统计、导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 29 项）：消息推送。
 * 推送通道本机没有真实服务：发送与重推均为预留占位调用，明确返回失败、
 * 如实记录（发送失败，失败原因写明通道未对接），绝不伪造发送成功。
 */
public interface PushRecordService extends IService<PushRecord> {

    /** 多条件组合分页查询 */
    PageResult<PushRecord> pageQuery(PushRecordQuery query);

    /** 详情 */
    PushRecord detail(Long id);

    /**
     * 发送推送：先建记录再尝试发送；本机无真实推送通道，调用返回
     * {record, success=false, message}，记录记为发送失败并写明失败原因。
     */
    Map<String, Object> send(PushRecord record);

    /**
     * 重推：已发送成功不可重推；待发送与发送失败可重推，重推仍走预留占位、
     * 再次失败如实记录。
     */
    Map<String, Object> retry(Long id);

    /** 删除推送记录：已发送成功的记录不可直接删除 */
    void delete(Long id);

    /** 推送记录统计：总数、待发送/已发送/发送失败、按渠道分组 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(PushRecordQuery query, OutputStream out);
}
