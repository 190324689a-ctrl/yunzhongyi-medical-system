package com.yunzhongyi.medical.inspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.inspection.dto.InspectionTemplateItemQuery;
import com.yunzhongyi.medical.inspection.dto.TemplateItemConfigRequest;
import com.yunzhongyi.medical.inspection.entity.InspectionTemplateItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 模板检查项配置服务：把检查项目库里的项目组装进质量检查模板，
 * 配置保存时同步回写模板的 checkItems JSON。
 */
public interface InspectionTemplateItemService extends IService<InspectionTemplateItem> {

    /** 多条件组合分页查询 */
    PageResult<InspectionTemplateItem> pageQuery(InspectionTemplateItemQuery query);

    /** 查某模板的全部检查项（按排序号升序） */
    List<InspectionTemplateItem> listByTemplate(Long templateId);

    /**
     * 模板配置统计：已配置模板数 / 配置记录总数 / 平均每模板检查项数 /
     * 未配置检查项的模板数 / 未组装进任何模板的项目数
     */
    Map<String, Object> stats();

    /** 详情 */
    InspectionTemplateItem detail(Long id);

    /**
     * 配置模板检查项（整模板替换）：校验模板存在、项目存在且已启用、
     * 同一模板内项目不重复、每项权重大于 0、权重之和为 100；
     * 保存后同步回写模板 checkItems JSON。返回保存后的检查项列表。
     */
    List<InspectionTemplateItem> configure(TemplateItemConfigRequest request);

    /** 删除单项：一个模板至少保留一项，不能删除最后一项；删除后同步回写模板 JSON */
    @Override
    boolean removeById(java.io.Serializable id);

    /** 导出指定模板的检查项 Excel */
    void exportExcel(Long templateId, OutputStream out);

    /** 批量导入指定模板的检查项 Excel（整模板替换，权重之和须为 100），返回成功导入条数 */
    int importExcel(Long templateId, InputStream in);
}
