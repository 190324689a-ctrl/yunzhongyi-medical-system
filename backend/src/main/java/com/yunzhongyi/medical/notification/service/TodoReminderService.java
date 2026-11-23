package com.yunzhongyi.medical.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.notification.dto.GenerateTodoRequest;
import com.yunzhongyi.medical.notification.dto.TodoReminderQuery;
import com.yunzhongyi.medical.notification.entity.TodoReminder;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 待办提醒服务：待办创建、完成流转、逾期扫描、按 28 号提醒设置生成、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 29 项）：待办提醒。
 */
public interface TodoReminderService extends IService<TodoReminder> {

    /** 多条件组合分页查询 */
    PageResult<TodoReminder> pageQuery(TodoReminderQuery query);

    /** 详情 */
    TodoReminder detail(Long id);

    /** 新建待办：标题必填、类型与来源合法，默认待办 */
    TodoReminder create(TodoReminder todo);

    /** 修改待办：已完成的待办不可修改 */
    TodoReminder update(TodoReminder todo);

    /** 删除待办：已完成的待办不可直接删除 */
    void delete(Long id);

    /** 完成待办：待办与已逾期可完成，已完成不可重复完成 */
    TodoReminder complete(Long id);

    /** 逾期扫描：待办且截止日期早于当天的改为已逾期，返回改动条数 */
    long overdueScan();

    /** 按 28 号提醒设置生成待办：设置须存在且启用，责任人必填 */
    TodoReminder generateFromSetting(GenerateTodoRequest request);

    /** 待办提醒统计：总数、待办/已完成/已逾期、按类型分组、按来源分组、今日到期数 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(TodoReminderQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；标题与内容均已存在的行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
