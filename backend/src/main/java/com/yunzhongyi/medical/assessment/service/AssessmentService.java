package com.yunzhongyi.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.assessment.dto.AssessmentQuery;
import com.yunzhongyi.medical.assessment.entity.Assessment;
import com.yunzhongyi.medical.common.PageResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 医师考核服务
 */
public interface AssessmentService extends IService<Assessment> {

    /** 多条件组合分页查询 */
    PageResult<Assessment> pageQuery(AssessmentQuery query);

    /** 按同样条件查全部 */
    List<Assessment> listByQuery(AssessmentQuery query);

    /**
     * 评分统计：总数 / 平均分 / 各等级人数与优秀率 /
     * 按等级分组 / 按科室汇总（人数、平均分、优秀率）
     */
    Map<String, Object> stats();

    /** 按科室汇总统计（同 stats 中的 byDept，单独提供给前端表格） */
    List<Map<String, Object>> statsByDept();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(AssessmentQuery query, OutputStream out);

    /** 批量导入 Excel，返回成功导入条数 */
    int importExcel(InputStream in);
}
