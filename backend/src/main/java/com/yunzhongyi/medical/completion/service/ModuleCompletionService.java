package com.yunzhongyi.medical.completion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.completion.dto.ModuleCompletionQuery;
import com.yunzhongyi.medical.completion.entity.ModuleCompletion;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 医务系统完成统计服务：模块完成台账的增删改、标记完成/重新开始、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 27 项）：医务系统各模块完成情况统计。
 */
public interface ModuleCompletionService extends IService<ModuleCompletion> {

    /** 多条件组合分页查询 */
    PageResult<ModuleCompletion> pageQuery(ModuleCompletionQuery query);

    /** 详情 */
    ModuleCompletion detail(Long id);

    /** 新增模块台账：模块编号全院唯一、分组合法、进度 0-100 */
    ModuleCompletion create(ModuleCompletion record);

    /** 修改模块台账：模块编号不可与已有记录重复 */
    ModuleCompletion update(ModuleCompletion record);

    /** 标记完成：只有未完成的模块能标记完成，进度置 100、完成日期默认当天 */
    ModuleCompletion complete(Long id);

    /** 重新开始：只有已完成的模块能重新开始，回到进行中、进度置 50、完成日期清空 */
    ModuleCompletion reopen(Long id);

    /** 完成统计总览：模块总数、已完成/进行中/待开发数、完成率、接口总数、测试总数、平均进度 */
    Map<String, Object> stats();

    /** 按分组汇总：各分组模块数、已完成/进行中/待开发数、完成率、接口数、测试数、平均进度 */
    List<Map<String, Object>> statsByCategory();

    /** 按完成月份趋势：每月完成模块数（按完成月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(ModuleCompletionQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；模块编号重复或非法行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
