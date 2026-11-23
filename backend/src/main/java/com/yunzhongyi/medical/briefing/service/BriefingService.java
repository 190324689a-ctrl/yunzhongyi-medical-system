package com.yunzhongyi.medical.briefing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.briefing.dto.BriefingQuery;
import com.yunzhongyi.medical.briefing.dto.GenerateBriefingRequest;
import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.common.PageResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface BriefingService extends IService<Briefing> {

    PageResult<Briefing> pageQuery(BriefingQuery query);

    Map<String, Object> stats();

    Map<String, Object> statsByCategory();

    Map<String, Object> statsByMonth();

    Briefing detail(Long id);

    Briefing create(Briefing briefing);

    Briefing update(Briefing briefing);

    void delete(Long id);

    void deleteBatch(java.util.List<Long> ids);

    /** 提交审批：草稿 / 已驳回 → 待审批 */
    Briefing submit(Long id);

    /** 审批通过：待审批 → 已发布，发布日期默认为当天 */
    Briefing approve(Long id, String approver, String opinion);

    /** 审批驳回：待审批 → 已驳回，驳回原因必填 */
    Briefing reject(Long id, String approver, String reason);

    /** 按模板自动生成简报（草稿）：从各业务模块聚合本期数据写正文 */
    Briefing generate(GenerateBriefingRequest request);

    /** 导出 Word（.docx，Apache POI） */
    void exportWord(Long id, OutputStream out);

    /** 导出 PDF（OpenPDF + 嵌入中文字体，真实 PDF 文件） */
    void exportPdf(Long id, OutputStream out);

    /** 下载正文纯文本（.txt） */
    void downloadText(Long id, OutputStream out);

    void exportExcel(BriefingQuery query, OutputStream out);

    Map<String, Object> importExcel(InputStream in);

    void writeImportTemplate(OutputStream out);
}
