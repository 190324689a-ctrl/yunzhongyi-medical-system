package com.yunzhongyi.medical.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.inspection.dto.InspectionCategoryQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionCategory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 检查分类管理服务
 */
public interface InspectionCategoryService extends IService<InspectionCategory> {

    /** 多条件组合分页查询 */
    PageResult<InspectionCategory> pageQuery(InspectionCategoryQuery query);

    /** 按同样条件查全部 */
    List<InspectionCategory> listByQuery(InspectionCategoryQuery query);

    /**
     * 检查分类统计：分类总数 / 已启用 / 已停用 / 一级 / 二级 /
     * 按适用检查类型分组 / 归属检查项目总数 / 有项目归属的分类数 /
     * 无项目归属的分类数 / 已组装进模板的项目数 / 涉及模板数
     */
    Map<String, Object> stats();

    /** 按分类汇总统计（每个分类的项目数、已启用项目数、涉及模板数、下级数） */
    List<Map<String, Object>> statsByCategory();

    /** 分类树（一级分类带下级，children 嵌套） */
    List<Map<String, Object>> tree();

    /** 全部启用的分类（新建检查项目时下拉用） */
    List<InspectionCategory> enabled();

    /** 详情 */
    InspectionCategory detail(Long id);

    /** 启用：只有停用的分类能启用；父分类停用时子分类不可启用 */
    InspectionCategory enable(Long id);

    /** 停用：只有启用的分类能停用；已有检查项目归属、或有启用下级的分类不可停用 */
    InspectionCategory disable(Long id);

    /** 删除：已有下级、或已有检查项目归属的分类不可删除 */
    @Override
    boolean removeById(java.io.Serializable id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(InspectionCategoryQuery query, OutputStream out);

    /** 批量导入 Excel（导入的分类默认启用），返回成功导入条数 */
    int importExcel(InputStream in);
}
