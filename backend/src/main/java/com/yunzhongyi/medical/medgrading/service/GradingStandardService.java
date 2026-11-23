package com.yunzhongyi.medical.medgrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.medgrading.dto.GradingStandardQuery;
import com.yunzhongyi.medical.medgrading.entity.GradingStandard;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 医疗分级标准服务：分数线方案的增删改、启用/停用、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 22 项）：医疗分级、医疗质量分级管理。
 */
public interface GradingStandardService extends IService<GradingStandard> {

    /** 多条件组合分页查询 */
    PageResult<GradingStandard> pageQuery(GradingStandardQuery query);

    /** 详情 */
    GradingStandard detail(Long id);

    /** 新增标准：编码唯一、范围合法、分数线须满足 passMin &lt; goodMin &lt; excellentMin */
    GradingStandard create(GradingStandard standard);

    /** 修改标准：只有未被评定引用过的标准能改编码；分数线变更不追溯已有评定 */
    GradingStandard update(GradingStandard standard);

    /** 启用：只有停用的标准能启用 */
    GradingStandard enable(Long id);

    /** 停用：有待审批评定的标准不可停用 */
    GradingStandard disable(Long id);

    /** 全部启用的标准（新建评定时下拉用） */
    List<GradingStandard> enabled();

    /** 分级标准统计：总数、启用/停用、按分级范围分组、被评定引用数 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(GradingStandardQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；编码重复或非法行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
