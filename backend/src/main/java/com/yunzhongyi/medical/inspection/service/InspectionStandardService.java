package com.yunzhongyi.medical.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.inspection.dto.InspectionStandardQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionStandard;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 检查标准管理服务
 */
public interface InspectionStandardService extends IService<InspectionStandard> {

    /** 多条件组合分页查询 */
    PageResult<InspectionStandard> pageQuery(InspectionStandardQuery query);

    /** 按同样条件查全部 */
    List<InspectionStandard> listByQuery(InspectionStandardQuery query);

    /**
     * 检查标准统计：标准总数 / 现行 / 已废止 / 按标准级别分组 /
     * 30 天内到期数 / 已过期数 / 被检查项目引用数
     */
    Map<String, Object> stats();

    /** 按标准级别汇总统计 */
    List<Map<String, Object>> statsByLevel();

    /** 详情 */
    InspectionStandard detail(Long id);

    /** 废止：只有现行的标准能废止，废止原因必填 */
    InspectionStandard abolish(Long id, String reason);

    /** 恢复现行：只有已废止的标准能恢复 */
    InspectionStandard restore(Long id);

    /** 删除：已被检查项目引用的标准不可删除 */
    @Override
    boolean removeById(java.io.Serializable id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(InspectionStandardQuery query, OutputStream out);

    /** 批量导入 Excel（导入的标准默认现行），返回成功导入条数 */
    int importExcel(InputStream in);
}
