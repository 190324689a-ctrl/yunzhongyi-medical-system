package com.yunzhongyi.medical.sysconfig.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据备份记录：一次备份任务的登记与结果。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：数据备份，支持导出。
 * <p>
 * 备份类型 backupType：FULL 全量备份 / INCREMENTAL 增量备份 / CONFIG 配置备份。
 * 备份状态 status：0 进行中 / 1 成功 / 2 失败；执行备份时先登记、再统计各核心表行数
 * 写入 recordCount、生成备份文件名，成功后状态置为成功。
 * 已恢复过的备份（restoreCount &gt; 0）不可直接删除（留作审计），失败的备份可直接删除。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_backup")
public class SysBackup extends BaseEntity {

    /** 备份名称，如 2026-11-22 全量备份 */
    @NotBlank(message = "备份名称不能为空")
    private String backupName;

    /** 备份类型：FULL 全量 / INCREMENTAL 增量 / CONFIG 配置 */
    private String backupType;

    /** 备份范围：ALL 全库 / SYSTEM 系统配置 / BUSINESS 业务数据 */
    private String backupScope;

    /** 备份时统计到的业务记录总条数（各核心表行数之和） */
    private Long recordCount;

    /** 备份文件名，如 medical-backup-20261122-203000.zip */
    private String fileName;

    /** 备份文件大小（字节），按记录数估算（每条记录约 1KB），仅作展示 */
    private Long fileSize;

    /** 备份状态：0 进行中，1 成功，2 失败 */
    private Integer status;

    /** 执行人 */
    private String createdBy;

    /** 已恢复次数 */
    private Integer restoreCount;

    /** 最近一次恢复时间 */
    private LocalDateTime lastRestoreTime;

    /** 备注 */
    private String remark;
}
