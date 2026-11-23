package com.yunzhongyi.medical.sysconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.KnowledgeItemQuery;
import com.yunzhongyi.medical.sysconfig.entity.KnowledgeItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务：知识条目的增删改、启用/停用、查阅计数、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：支持 3000+ 条知识库。
 */
public interface KnowledgeItemService extends IService<KnowledgeItem> {

    /** 多条件组合分页查询（标题/正文/关键词模糊检索） */
    PageResult<KnowledgeItem> pageQuery(KnowledgeItemQuery query);

    /** 详情：打开一次查阅量 +1 */
    KnowledgeItem detail(Long id);

    /** 新增知识：编码唯一、分类合法 */
    KnowledgeItem create(KnowledgeItem item);

    /** 修改知识 */
    KnowledgeItem update(KnowledgeItem item);

    /** 启用 */
    KnowledgeItem enable(Long id);

    /** 停用 */
    KnowledgeItem disable(Long id);

    /** 全部启用的知识（检索用） */
    List<KnowledgeItem> enabled();

    /** 知识库统计：总数、启用/停用、按分类分组、总查阅量 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(KnowledgeItemQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；编码重复或非法行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
