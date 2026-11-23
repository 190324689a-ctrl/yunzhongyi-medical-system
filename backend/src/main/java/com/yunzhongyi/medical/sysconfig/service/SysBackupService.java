package com.yunzhongyi.medical.sysconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.SysBackupQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysBackup;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 数据备份服务：备份执行、恢复、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：数据备份。
 */
public interface SysBackupService extends IService<SysBackup> {

    /** 多条件组合分页查询 */
    PageResult<SysBackup> pageQuery(SysBackupQuery query);

    /** 详情 */
    SysBackup detail(Long id);

    /** 执行备份：统计各核心表行数总和，生成备份文件名，登记为成功 */
    SysBackup create(SysBackup backup);

    /** 恢复备份：只有成功的备份能恢复，恢复次数 +1 并记录恢复时间 */
    SysBackup restore(Long id);

    /** 删除：已恢复过的备份不可直接删除 */
    @Override
    boolean removeById(java.io.Serializable id);

    /** 备份统计：总数、成功/失败/进行中、已恢复数、总记录数、按类型分组 */
    Map<String, Object> stats();

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(SysBackupQuery query, OutputStream out);

    /** 批量导入 Excel：导入的是历史备份记录，返回 {created, skipped, total} */
    Map<String, Object> importExcel(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
