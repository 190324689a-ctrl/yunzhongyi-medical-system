package com.yunzhongyi.medical.radiation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.radiation.dto.RadiationPersonnelQuery;
import com.yunzhongyi.medical.radiation.entity.RadiationPersonnel;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 放射人员档案管理服务
 */
public interface RadiationPersonnelService extends IService<RadiationPersonnel> {

    /** 多条件组合分页查询 */
    PageResult<RadiationPersonnel> pageQuery(RadiationPersonnelQuery query);

    /** 按同样条件查全部 */
    List<RadiationPersonnel> listByQuery(RadiationPersonnelQuery query);

    /**
     * 放射人员统计：档案总数 / 在岗 / 停岗 / 离岗 / 按放射类别分组 /
     * 按科室汇总 / 资质待审核 / 已通过 / 已驳回 / 90 天内到期 / 已过期 /
     * 培训总数 / 培训合格数 / 体检总数 / 平均剂量 / 剂量超标数 /
     * 复查数 / 不宜从事放射工作数 / 体检逾期人数
     */
    Map<String, Object> stats();

    /** 按科室汇总统计（档案数 / 在岗数 / 停岗数 / 离岗数） */
    List<Map<String, Object>> statsByDept();

    /** 按放射类别汇总统计 */
    List<Map<String, Object>> statsByWorkType();

    /** 详情 */
    RadiationPersonnel detail(Long id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(RadiationPersonnelQuery query, OutputStream out);

    /** 批量导入 Excel，返回成功导入条数 */
    int importExcel(InputStream in);
}
