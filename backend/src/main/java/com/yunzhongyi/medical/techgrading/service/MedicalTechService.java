package com.yunzhongyi.medical.techgrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.techgrading.dto.MedicalTechQuery;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.entity.MedicalTech;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 医疗技术档案管理服务
 */
public interface MedicalTechService extends IService<MedicalTech> {

    /** 多条件组合分页查询 */
    PageResult<MedicalTech> pageQuery(MedicalTechQuery query);

    /** 按同样条件查全部 */
    List<MedicalTech> listByQuery(MedicalTechQuery query);

    /**
     * 技术档案统计：档案总数 / 待准入 / 已准入 / 已停用 / 按技术分级分组 /
     * 按准入类别分组 / 按技术分类分组 / 按开展科室汇总 / 90 天内到期数 /
     * 已过期数 / 准入申请统计 / 医师授权统计
     */
    Map<String, Object> stats();

    /** 按技术分级汇总统计 */
    List<Map<String, Object>> statsByLevel();

    /** 按技术分类汇总统计 */
    List<Map<String, Object>> statsByCategory();

    /** 按开展科室汇总统计（技术数、已准入数） */
    List<Map<String, Object>> statsByDept();

    /** 详情 */
    MedicalTech detail(Long id);

    /** 停用：只有已准入的档案能停用，停用原因必填，审批记录留痕 */
    MedicalTech suspend(Long id, RejectRequest request);

    /** 重新启用：只有已停用的档案能重新启用，审批记录留痕 */
    MedicalTech resume(Long id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(MedicalTechQuery query, OutputStream out);

    /** 批量导入 Excel（导入的均为待准入，需走准入审批流程），返回成功导入条数 */
    int importExcel(InputStream in);
}
