package com.yunzhongyi.medical.statistics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.statistics.dto.QualityIndicatorQuery;
import com.yunzhongyi.medical.statistics.entity.QualityIndicator;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 三级医院医疗质量指标统计服务：第 25 项，指标 CRUD + 按国家 / 省 / 市标准
 * 统计（达标率）+ Excel 批量导入导出
 */
public interface QualityIndicatorService extends IService<QualityIndicator> {

    PageResult<QualityIndicator> pageQuery(QualityIndicatorQuery query);

    List<QualityIndicator> listByQuery(QualityIndicatorQuery query);

    /** 指标统计：总数、已填报数、达标数、达标率、按标准级别 / 按分类汇总 */
    Map<String, Object> stats();

    /** 按标准级别汇总统计（级别、指标数、已填报数、达标数、达标率） */
    List<Map<String, Object>> statsByLevel();

    /** 按指标分类汇总统计（分类、指标数、已填报数、达标数、达标率） */
    List<Map<String, Object>> statsByCategory();

    void exportExcel(QualityIndicatorQuery query, OutputStream out);

    /** 批量导入 Excel：同一指标编码重复跳过；返回成功导入条数 */
    int importExcel(InputStream in);
}
