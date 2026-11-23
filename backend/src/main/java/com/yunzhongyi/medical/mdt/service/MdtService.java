package com.yunzhongyi.medical.mdt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.mdt.dto.MdtQuery;
import com.yunzhongyi.medical.mdt.entity.MdtApplication;
import com.yunzhongyi.medical.mdt.entity.MdtRecord;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * MDT 多学科会诊管理服务
 */
public interface MdtService extends IService<MdtApplication> {

    /** 多条件组合分页查询 */
    PageResult<MdtApplication> pageQuery(MdtQuery query);

    /** 按同样条件查全部 */
    List<MdtApplication> listByQuery(MdtQuery query);

    /**
     * MDT 统计：总数 / 待审批数 / 已通过数 / 已驳回数 / 按会诊类型分组 /
     * 按科室汇总 / 按月趋势
     */
    Map<String, Object> stats();

    /** 按科室汇总统计（同 stats 中的 byDept，单独提供给前端表格） */
    List<Map<String, Object>> statsByDept();

    /** 按月趋势统计（申请月份 → 申请数 / 通过数，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 详情：申请 + 全部会诊记录 */
    Map<String, Object> detail(Long id);

    /** 一级审批通过（医务科/科主任）：只有待审批的申请能通过，审批记录留痕 */
    MdtApplication approve(Long id, String approver, String opinion);

    /** 一级审批驳回（医务科/科主任）：只有待审批的申请能驳回，审批记录留痕 */
    MdtApplication reject(Long id, String approver, String opinion);

    /** 查某申请的全部会诊记录（按会诊时间升序） */
    List<MdtRecord> listRecords(Long mdtId);

    /** 添加会诊记录：只有审批通过的申请才能记录，可多次补充 */
    MdtRecord addRecord(Long mdtId, MdtRecord record);

    /** 删除一条会诊记录 */
    boolean removeRecord(Long mdtId, Long recordId);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(MdtQuery query, OutputStream out);

    /** 批量导入 Excel，返回成功导入条数 */
    int importExcel(InputStream in);
}
