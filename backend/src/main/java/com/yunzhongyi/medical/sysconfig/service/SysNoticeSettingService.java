package com.yunzhongyi.medical.sysconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.SysNoticeSettingQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysNoticeSetting;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 消息提醒设置服务：提醒规则配置的增删改、启用/停用、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：消息提醒设置。
 */
public interface SysNoticeSettingService extends IService<SysNoticeSetting> {

    /** 多条件组合分页查询 */
    PageResult<SysNoticeSetting> pageQuery(SysNoticeSettingQuery query);

    /** 详情 */
    SysNoticeSetting detail(Long id);

    /** 新增设置：编码唯一、渠道与接收范围合法 */
    SysNoticeSetting create(SysNoticeSetting setting);

    /** 修改设置 */
    SysNoticeSetting update(SysNoticeSetting setting);

    /** 启用 */
    SysNoticeSetting enable(Long id);

    /** 停用 */
    SysNoticeSetting disable(Long id);

    /** 全部启用的设置 */
    List<SysNoticeSetting> enabled();

    /** 提醒设置统计：总数、启用/停用、按渠道分组、按接收范围分组 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(SysNoticeSettingQuery query, OutputStream out);

    /** 批量导入 Excel：返回 {created, skipped, total}；编码重复或非法行跳过 */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
