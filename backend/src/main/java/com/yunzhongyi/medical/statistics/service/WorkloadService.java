package com.yunzhongyi.medical.statistics.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.statistics.dto.WorkloadQuery;
import com.yunzhongyi.medical.statistics.entity.WorkloadRecord;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 工作统计服务：第 24 项，工作量登记 CRUD + 按科室 / 按人员 / 按时间统计 +
 * Excel 批量导入导出
 */
public interface WorkloadService extends IService<WorkloadRecord> {

    PageResult<WorkloadRecord> pageQuery(WorkloadQuery query);

    List<WorkloadRecord> listByQuery(WorkloadQuery query);

    /** 工作统计：总量（门诊 / 住院 / 手术 / 合计）、记录数、按科室 / 按人员 / 按月汇总 */
    Map<String, Object> stats();

    /** 按科室汇总统计（科室、门诊量、住院量、手术量、合计、填报人数） */
    List<Map<String, Object>> statsByDept();

    /** 按人员汇总统计（医师、科室、门诊量、住院量、手术量、合计） */
    List<Map<String, Object>> statsByPhysician();

    /** 按月趋势统计（月份、门诊量、住院量、手术量、合计，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    void exportExcel(WorkloadQuery query, OutputStream out);

    /** 批量导入 Excel：同一医师同一统计月份重复跳过；返回成功导入条数 */
    int importExcel(InputStream in);
}
