package com.yunzhongyi.medical.newtech.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.newtech.dto.NewTechQuery;
import com.yunzhongyi.medical.newtech.entity.NewTechProject;
import com.yunzhongyi.medical.newtech.entity.NewTechRecord;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 新技术、新项目管理服务
 */
public interface NewTechService extends IService<NewTechProject> {

    /** 多条件组合分页查询 */
    PageResult<NewTechProject> pageQuery(NewTechQuery query);

    /** 按同样条件查全部 */
    List<NewTechProject> listByQuery(NewTechQuery query);

    /**
     * 新技术、新项目统计：总数 / 待审批数 / 已立项数 / 已验收数 / 已推广数 /
     * 已驳回数 / 待伦理审查数 / 按项目类型分组 / 按科室汇总 / 按月趋势
     */
    Map<String, Object> stats();

    /** 按科室汇总统计（同 stats 中的 byDept，单独提供给前端表格） */
    List<Map<String, Object>> statsByDept();

    /** 按月趋势统计（申报月份 → 申报数 / 立项数，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 详情：项目 + 全部进展记录 */
    Map<String, Object> detail(Long id);

    /** 一级审批通过（医务科）：只有待审批的项目能通过，审批记录留痕；需伦理审查的项目须伦理通过后才能立项 */
    NewTechProject approve(Long id, String approver, String opinion);

    /** 一级审批驳回（医务科）：只有待审批的项目能驳回，审批记录留痕 */
    NewTechProject reject(Long id, String approver, String opinion);

    /** 伦理审查：只有需伦理审查且待审查的项目能审查，审查结果留痕 */
    NewTechProject ethicsReview(Long id, Boolean passed, String reviewer, String opinion);

    /** 查某项目的全部进展记录（按记录日期升序） */
    List<NewTechRecord> listRecords(Long projectId);

    /**
     * 添加进展记录：进展记录只允许已立项/已验收的项目；验收记录只允许已立项的项目，
     * 添加后项目自动变为已验收；推广记录只允许已验收的项目，添加后项目自动变为已推广
     */
    NewTechRecord addRecord(Long projectId, NewTechRecord record);

    /** 删除一条进展记录 */
    boolean removeRecord(Long projectId, Long recordId);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(NewTechQuery query, OutputStream out);

    /** 批量导入 Excel，返回成功导入条数 */
    int importExcel(InputStream in);
}
