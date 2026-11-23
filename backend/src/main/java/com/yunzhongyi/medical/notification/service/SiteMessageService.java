package com.yunzhongyi.medical.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.notification.dto.SiteMessageQuery;
import com.yunzhongyi.medical.notification.entity.SiteMessage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 站内消息服务：消息收发、已读流转、未读计数、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 29 项）：站内消息。
 */
public interface SiteMessageService extends IService<SiteMessage> {

    /** 多条件组合分页查询 */
    PageResult<SiteMessage> pageQuery(SiteMessageQuery query);

    /** 详情 */
    SiteMessage detail(Long id);

    /** 发送站内消息：标题必填、分类与接收范围合法，默认未读、发送时间为当前时间 */
    SiteMessage create(SiteMessage message);

    /** 修改消息：已读消息不可修改 */
    SiteMessage update(SiteMessage message);

    /** 删除消息：不存在拒绝 */
    void delete(Long id);

    /** 标已读：已读消息不可重复标记 */
    SiteMessage markRead(Long id);

    /** 全部标已读：把某用户可见的未读消息全部标已读，返回标已读条数 */
    long markAllRead(String username, String roleCode);

    /** 未读数：某用户可见（全部人员 / 其角色 / 其本人）的未读消息数 */
    long unreadCount(String username, String roleCode);

    /** 站内消息统计：总数、未读/已读、按分类分组、按接收范围分组 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(SiteMessageQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；标题与内容均已存在的行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
