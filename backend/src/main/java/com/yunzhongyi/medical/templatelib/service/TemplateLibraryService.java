package com.yunzhongyi.medical.templatelib.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.quality.entity.QualityTemplate;
import com.yunzhongyi.medical.templatelib.entity.TemplateVersion;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 检查模板管理（模板库）服务：版本管理、复制、使用情况、统计、导入导出。
 * <p>
 * 覆盖需求（需求梳理.md 第 21 项）：检查模板管理。
 * 模板本身的增删改与启用/停用在质量检查管理模块（/api/quality-templates），
 * 本模块补齐模板的版本、复制、使用跟踪、统计与批量导入导出。
 */
public interface TemplateLibraryService extends IService<TemplateVersion> {

    /** 新建版本：校验检查项（权重之和须为 100），快照新内容为当前版本并回写模板 */
    TemplateVersion createVersion(Long templateId, String checkItems, String changeRemark);

    /** 从历史版本恢复：以该版本快照内容新建一版（历史只追加不改写） */
    TemplateVersion restoreVersion(Long versionId, String changeRemark);

    /** 某模板的版本历史（按版本号倒序） */
    List<TemplateVersion> listVersions(Long templateId);

    /** 某模板的当前版本（无版本记录时返回 null） */
    TemplateVersion currentVersion(Long templateId);

    /** 复制模板：同类型同检查项的新模板，默认停用；同名同类型已存在时拒绝 */
    QualityTemplate copyTemplate(Long templateId, String newName);

    /** 某模板的使用情况：计划数（按状态）、记录数、最近一次计划日期、是否有进行中计划 */
    Map<String, Object> usage(Long templateId);

    /** 模板库统计：总数、启用/停用、按检查类型分组、版本数、最常用模板、从未使用数等 */
    Map<String, Object> stats();

    /** 导出全部模板 Excel */
    void exportTemplates(OutputStream out);

    /** 导入模板 Excel：返回 {created, skipped, total}；同名同类型跳过，非法行跳过 */
    Map<String, Object> importTemplates(InputStream in);

    /** 导入模板下载（空表头，与导入格式一致） */
    void writeImportTemplate(OutputStream out);
}
