package com.yunzhongyi.medical.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.schedule.dto.ScheduleChangeRequest;
import com.yunzhongyi.medical.schedule.dto.ScheduleGenerateRequest;
import com.yunzhongyi.medical.schedule.dto.ScheduleQuery;
import com.yunzhongyi.medical.schedule.entity.Schedule;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 排班管理服务
 */
public interface ScheduleService extends IService<Schedule> {

    /** 多条件组合分页查询 */
    PageResult<Schedule> pageQuery(ScheduleQuery query);

    /** 按同样条件查全部 */
    List<Schedule> listByQuery(ScheduleQuery query);

    /**
     * 排班统计：总数 / 待审批调班代班数 / 按排班类型分组 / 按班次分组 /
     * 按科室汇总 / 按人员汇总
     */
    Map<String, Object> stats();

    /** 按科室汇总统计（同 stats 中的 byDept，单独提供给前端表格） */
    List<Map<String, Object>> statsByDept();

    /** 按人员汇总统计（排班天数、夜班天数） */
    List<Map<String, Object>> statsByPhysician();

    /**
     * 按模板批量生成排班：返回 {created 新建数, skipped 冲突跳过数, restDays 休息跳过数, total 应排天数}。
     * 同一医师同一天已有排班时跳过该天（不覆盖、不报错）。
     */
    Map<String, Object> generateByTemplate(ScheduleGenerateRequest request);

    /** 申请调班（换班次/换日期）：进入待科主任审批 */
    Schedule applySwap(Long id, ScheduleChangeRequest request);

    /** 申请代班（换人）：进入待科主任审批 */
    Schedule applySubstitute(Long id, ScheduleChangeRequest request);

    /** 一级审批通过（科主任）：调班改日期/班次，代班改人员；仍校验不冲突 */
    Schedule approveChange(Long id, String approver, String opinion);

    /** 一级审批驳回（科主任）：原排班不变 */
    Schedule rejectChange(Long id, String approver, String opinion);

    /**
     * 同步单条排班到 HIS（预留占位）。
     * 未配置 HIS 对接时抛 UnsupportedOperationException，绝不伪造同步成功。
     */
    Map<String, Object> syncToHis(Long id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(ScheduleQuery query, OutputStream out);

    /** 批量导入 Excel，返回成功导入条数 */
    int importExcel(InputStream in);
}
