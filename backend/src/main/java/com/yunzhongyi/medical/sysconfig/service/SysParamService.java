package com.yunzhongyi.medical.sysconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.SysParamQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysParam;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 系统参数服务：参数键值对的增删改、启用/停用、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：参数设置，支持自定义。
 */
public interface SysParamService extends IService<SysParam> {

    /** 多条件组合分页查询 */
    PageResult<SysParam> pageQuery(SysParamQuery query);

    /** 详情 */
    SysParam detail(Long id);

    /** 按参数键取启用参数（业务读取用） */
    SysParam getByKey(String paramKey);

    /** 新增参数：键唯一、分组与值类型合法、值须符合值类型 */
    SysParam create(SysParam param);

    /** 修改参数：内置参数的键不可改 */
    SysParam update(SysParam param);

    /** 启用 */
    SysParam enable(Long id);

    /** 停用 */
    SysParam disable(Long id);

    /** 全部启用的参数（下拉/配置读取用） */
    List<SysParam> enabled();

    /** 参数统计：总数、启用/停用、按分组统计、内置/自定义数 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(SysParamQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；键重复或非法行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
