package com.yunzhongyi.medical.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.inspection.dto.InspectionItemQuery;
import com.yunzhongyi.medical.inspection.entity.InspectionItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 检查项目管理服务
 */
public interface InspectionItemService extends IService<InspectionItem> {

    /** 多条件组合分页查询 */
    PageResult<InspectionItem> pageQuery(InspectionItemQuery query);

    /** 按同样条件查全部 */
    List<InspectionItem> listByQuery(InspectionItemQuery query);

    /**
     * 检查项目统计：项目总数 / 已启用 / 已停用 / 按项目分类分组 /
     * 按检查方式分组 / 按适用检查类型分组 / 已组装进模板数 / 未组装进模板数
     */
    Map<String, Object> stats();

    /** 按项目分类汇总统计 */
    List<Map<String, Object>> statsByCategory();

    /** 详情 */
    InspectionItem detail(Long id);

    /** 启用：只有停用的项目能启用 */
    InspectionItem enable(Long id);

    /** 停用：只有启用的项目能停用；已被模板引用的项目不可停用 */
    InspectionItem disable(Long id);

    /** 删除：已被模板引用的项目不可删除 */
    @Override
    boolean removeById(java.io.Serializable id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(InspectionItemQuery query, OutputStream out);

    /** 批量导入 Excel（导入的项目默认启用），返回成功导入条数 */
    int importExcel(InputStream in);
}
