package com.yunzhongyi.medical.sysconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.SysInterfaceQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysInterface;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 系统接口服务：外部系统对接配置的增删改、启用/停用、连通性测试、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：系统接口。
 */
public interface SysInterfaceService extends IService<SysInterface> {

    /** 多条件组合分页查询（鉴权凭证不回显） */
    PageResult<SysInterface> pageQuery(SysInterfaceQuery query);

    /** 详情（鉴权凭证不回显） */
    SysInterface detail(Long id);

    /** 新增接口：编码唯一、类型与鉴权方式合法、地址须为 http/https 开头 */
    SysInterface create(SysInterface iface);

    /** 修改接口：编码不可改（已有测试记录按编码关联） */
    SysInterface update(SysInterface iface);

    /** 启用 */
    SysInterface enable(Long id);

    /** 停用 */
    SysInterface disable(Long id);

    /** 全部启用的接口 */
    List<SysInterface> enabled();

    /**
     * 连通性测试：只有启用的接口能测试；真实发起 HTTP 请求并回写测试结果与时间，
     * 不伪造成功；返回 {success, httpStatus, message}。
     */
    Map<String, Object> testConnection(Long id);

    /** 接口统计：总数、启用/停用、按类型分组、测试成功/失败/未测试数 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件，鉴权凭证不导出） */
    void exportExcel(SysInterfaceQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；编码重复或非法行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
