package com.yunzhongyi.medical.personnel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.personnel.dto.PersonnelChangeQuery;
import com.yunzhongyi.medical.personnel.entity.PersonnelChange;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 医疗人员动态管理服务
 */
public interface PersonnelChangeService extends IService<PersonnelChange> {

    /** 多条件组合分页查询 */
    PageResult<PersonnelChange> pageQuery(PersonnelChangeQuery query);

    /** 按同样条件查全部 */
    List<PersonnelChange> listByQuery(PersonnelChangeQuery query);

    /**
     * 医疗人员动态统计：总数 / 待审批数 / 已通过数 / 已驳回数 /
     * 按变动类型分组 / 按相关科室汇总 / 按月趋势
     */
    Map<String, Object> stats();

    /** 按相关科室汇总统计（同 stats 中的 byDept，单独提供给前端表格） */
    List<Map<String, Object>> statsByDept();

    /** 按月趋势统计（生效月份 → 变动数 / 已通过数 / 待审批数，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 详情 */
    PersonnelChange detail(Long id);

    /**
     * 一级审批通过（医务科）：只有待审批的变动能通过，审批记录留痕；
     * 通过时同步医师档案（调动改科室、离职/退休改在职状态、入职/返聘改回在职）
     */
    PersonnelChange approve(Long id, String approver, String opinion);

    /** 一级审批驳回（医务科）：只有待审批的变动能驳回，审批记录留痕 */
    PersonnelChange reject(Long id, String approver, String opinion);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(PersonnelChangeQuery query, OutputStream out);

    /** 批量导入 Excel，返回成功导入条数 */
    int importExcel(InputStream in);
}
