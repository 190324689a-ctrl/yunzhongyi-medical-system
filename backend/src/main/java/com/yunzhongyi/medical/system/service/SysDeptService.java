package com.yunzhongyi.medical.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.system.entity.SysDept;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 科室服务
 */
public interface SysDeptService extends IService<SysDept> {

    /** 分页查询科室 */
    PageResult<SysDept> page(String keyword, String type, Integer status, long page, long size);

    /** 全部启用科室（下拉框用） */
    List<SysDept> listEnabled();

    /** 新增科室：科室编码唯一 */
    SysDept create(SysDept dept);

    /** 修改科室：科室编码唯一（改编码时须无下级科室） */
    SysDept update(SysDept dept);

    /** 启用 */
    SysDept enable(Long id);

    /** 停用：有下级科室的不可停用 */
    SysDept disable(Long id);

    /** 科室统计：总数、启用/停用、按类型分组 */
    Map<String, Object> stats();

    /** 科室树：按 parentId 嵌套（顶级 parentId 为 0），节点为科室全部字段 + children */
    List<Map<String, Object>> tree();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(String keyword, String type, Integer status, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；编码重复或非法行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
