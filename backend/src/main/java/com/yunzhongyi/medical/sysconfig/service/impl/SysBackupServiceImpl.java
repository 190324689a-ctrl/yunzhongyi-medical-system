package com.yunzhongyi.medical.sysconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.sysconfig.dto.SysBackupQuery;
import com.yunzhongyi.medical.sysconfig.entity.SysBackup;
import com.yunzhongyi.medical.sysconfig.excel.SysBackupExcelUtil;
import com.yunzhongyi.medical.sysconfig.mapper.SysBackupMapper;
import com.yunzhongyi.medical.sysconfig.service.SysBackupService;
import com.yunzhongyi.medical.sysconfig.service.SysConfigTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据备份服务实现。
 * <p>
 * 关键守卫：备份类型须合法；执行备份时枚举全库业务表、逐表 COUNT 求和作为记录条数，
 * 生成备份文件名并登记为成功（文件大小按记录数估算，仅作展示）；只有成功的备份能恢复；
 * 已恢复过的备份不可直接删除（留作审计）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysBackupServiceImpl
        extends ServiceImpl<SysBackupMapper, SysBackup>
        implements SysBackupService {

    private final SqlSessionFactory sqlSessionFactory;

    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public PageResult<SysBackup> pageQuery(SysBackupQuery query) {
        Page<SysBackup> p = this.page(new Page<>(query.getPage(), query.getSize()),
                buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public SysBackup detail(Long id) {
        SysBackup b = this.getById(id);
        if (b == null) {
            throw new IllegalArgumentException("数据备份不存在：" + id);
        }
        return b;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysBackup create(SysBackup backup) {
        validateBackup(backup);
        if (backup.getBackupType() == null) {
            backup.setBackupType(SysConfigTypeUtil.BACKUP_FULL);
        }
        if (backup.getBackupScope() == null) {
            backup.setBackupScope("ALL");
        }
        if (!StringUtils.hasText(backup.getCreatedBy())) {
            backup.setCreatedBy("admin");
        }
        backup.setStatus(SysConfigTypeUtil.BACKUP_RUNNING);
        backup.setRestoreCount(0);
        this.save(backup);
        // 统计全库各表行数之和作为备份记录条数，生成备份文件名
        long total = countAllTableRows();
        backup.setRecordCount(total);
        backup.setFileName("medical-backup-" + LocalDateTime.now().format(FILE_FMT) + ".zip");
        backup.setFileSize(SysConfigTypeUtil.estimateFileSize(total));
        backup.setStatus(SysConfigTypeUtil.BACKUP_SUCCESS);
        this.updateById(backup);
        return backup;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysBackup restore(Long id) {
        SysBackup b = detail(id);
        if (!Integer.valueOf(SysConfigTypeUtil.BACKUP_SUCCESS).equals(b.getStatus())) {
            throw new IllegalArgumentException("只有备份成功的记录才能恢复");
        }
        b.setRestoreCount(b.getRestoreCount() == null ? 1 : b.getRestoreCount() + 1);
        b.setLastRestoreTime(LocalDateTime.now());
        this.updateById(b);
        return b;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        SysBackup b = detail((Long) id);
        if (b.getRestoreCount() != null && b.getRestoreCount() > 0) {
            throw new IllegalArgumentException("该备份已被恢复过（" + b.getRestoreCount() + " 次），不可直接删除");
        }
        return super.removeById(id);
    }

    @Override
    public Map<String, Object> stats() {
        List<SysBackup> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("success", all.stream()
                .filter(b -> Integer.valueOf(SysConfigTypeUtil.BACKUP_SUCCESS).equals(b.getStatus())).count());
        r.put("failed", all.stream()
                .filter(b -> Integer.valueOf(SysConfigTypeUtil.BACKUP_FAILED).equals(b.getStatus())).count());
        r.put("running", all.stream()
                .filter(b -> Integer.valueOf(SysConfigTypeUtil.BACKUP_RUNNING).equals(b.getStatus())).count());
        r.put("restored", all.stream()
                .filter(b -> b.getRestoreCount() != null && b.getRestoreCount() > 0).count());
        r.put("totalRestoreCount", all.stream()
                .mapToLong(b -> b.getRestoreCount() == null ? 0L : b.getRestoreCount()).sum());
        r.put("totalRecords", all.stream()
                .mapToLong(b -> b.getRecordCount() == null ? 0L : b.getRecordCount()).sum());
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : List.of(SysConfigTypeUtil.BACKUP_FULL,
                SysConfigTypeUtil.BACKUP_INCREMENTAL, SysConfigTypeUtil.BACKUP_CONFIG)) {
            byType.put(type, all.stream().filter(b -> type.equals(b.getBackupType())).count());
        }
        r.put("byType", byType);
        return r;
    }

    @Override
    public void exportExcel(SysBackupQuery query, OutputStream out) {
        SysBackupExcelUtil.write(this.list(buildWrapper(query)), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importExcel(InputStream in) {
        List<SysBackup> rows = SysBackupExcelUtil.read(in);
        int created = 0;
        int skipped = 0;
        for (SysBackup row : rows) {
            try {
                validateBackup(row);
            } catch (IllegalArgumentException e) {
                skipped++;
                continue;
            }
            if (row.getBackupType() == null) {
                row.setBackupType(SysConfigTypeUtil.BACKUP_FULL);
            }
            if (row.getStatus() == null) {
                row.setStatus(SysConfigTypeUtil.BACKUP_SUCCESS);
            }
            if (row.getRestoreCount() == null) {
                row.setRestoreCount(0);
            }
            this.save(row);
            created++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", rows.size());
        return result;
    }

    @Override
    public void writeImportTemplate(OutputStream out) {
        SysBackupExcelUtil.writeTemplate(out);
    }

    // ---------- 内部方法 ----------

    private LambdaQueryWrapper<SysBackup> buildWrapper(SysBackupQuery query) {
        LambdaQueryWrapper<SysBackup> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            w.like(SysBackup::getBackupName, query.getKeyword().trim());
        }
        if (StringUtils.hasText(query.getBackupType())) {
            w.eq(SysBackup::getBackupType, query.getBackupType());
        }
        if (query.getStatus() != null) {
            w.eq(SysBackup::getStatus, query.getStatus());
        }
        if (Boolean.TRUE.equals(query.getRestoredOnly())) {
            w.gt(SysBackup::getRestoreCount, 0);
        }
        w.orderByDesc(SysBackup::getId);
        return w;
    }

    private void validateBackup(SysBackup b) {
        if (!StringUtils.hasText(b.getBackupName())) {
            throw new IllegalArgumentException("备份名称不能为空");
        }
        if (b.getBackupType() != null && !SysConfigTypeUtil.BACKUP_TYPES.contains(b.getBackupType())) {
            throw new IllegalArgumentException("备份类型不合法：" + b.getBackupType());
        }
    }

    /**
     * 枚举当前库全部业务表并逐表 COUNT 求和；某张表统计失败时记日志并跳过，
     * 不阻断备份登记。
     */
    private long countAllTableRows() {
        long total = 0L;
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Connection conn = session.getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            try (Statement st = conn.createStatement()) {
                for (String table : tables) {
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        if (rs.next()) {
                            total += rs.getLong(1);
                        }
                    } catch (Exception e) {
                        log.warn("备份统计表 {} 行数失败，已跳过: {}", table, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("备份统计全库行数失败，记录条数记为 0: {}", e.getMessage());
        }
        return total;
    }
}
