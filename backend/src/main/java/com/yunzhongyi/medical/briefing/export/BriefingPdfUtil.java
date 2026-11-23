package com.yunzhongyi.medical.briefing.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.briefing.service.BriefingTypeUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 医疗工作简报 PDF 导出（OpenPDF，真实 PDF 文件，非占位）。
 * <p>
 * 中文字体：本机 Noto CJK 字体集（.ttc）按下标逐个试读，取 PostScript 名含
 * “CJKsc”的简体中文字体并嵌入（IDENTITY_H）；正文用常规体，标题与栏目
 * 小标题用粗体。若本机找不到可用的中文字体，明确抛错、不生成假 PDF，
 * 调用方应提示用户改用 Word 导出。
 */
@Slf4j
public final class BriefingPdfUtil {

    private BriefingPdfUtil() {
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 本机中文字体候选（Noto CJK 字体集，含简/繁/日/韩五个子字体） */
    private static final List<String> REGULAR_CANDIDATES = List.of(
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc");
    private static final List<String> BOLD_CANDIDATES = List.of(
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
            "/usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc");

    private static volatile BaseFont cachedRegular;
    private static volatile BaseFont cachedBold;

    public static void write(Briefing briefing, OutputStream out) {
        BaseFont regular = resolveCjkFont(REGULAR_CANDIDATES, false);
        BaseFont bold = resolveCjkFont(BOLD_CANDIDATES, true);
        Font titleFont = new Font(bold, 18, Font.BOLD);
        Font metaFont = new Font(regular, 10, Font.NORMAL);
        Font bodyFont = new Font(regular, 11, Font.NORMAL);
        Font sectionFont = new Font(bold, 12, Font.BOLD);

        Document doc = new Document(PageSize.A4, 54, 54, 54, 54);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Paragraph title = new Paragraph(briefing.getTitle() == null ? "" : briefing.getTitle(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            doc.add(title);

            Paragraph meta = new Paragraph(buildMetaLine(briefing), metaFont);
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(16);
            doc.add(meta);

            String content = briefing.getContent() == null ? "" : briefing.getContent();
            for (String line : content.split("\\r?\\n")) {
                if (line.isBlank()) {
                    continue;
                }
                String trimmed = line.trim();
                Paragraph p = new Paragraph(trimmed,
                        BriefingWordUtil.isSectionHeader(trimmed) ? sectionFont : bodyFont);
                p.setLeading(18);
                p.setSpacingAfter(4);
                doc.add(p);
            }
        } catch (Exception e) {
            throw new RuntimeException("导出 PDF 失败: " + e.getMessage(), e);
        } finally {
            doc.close();
        }
    }

    private static String buildMetaLine(Briefing b) {
        StringBuilder sb = new StringBuilder();
        sb.append("期号：").append(b.getIssueNo() == null ? "" : b.getIssueNo());
        sb.append("　分类：").append(BriefingTypeUtil.categoryText(b.getCategory()));
        sb.append("　统计周期：").append(b.getPeriod() == null ? "" : b.getPeriod());
        sb.append("　状态：").append(BriefingTypeUtil.statusText(b.getStatus()));
        if (b.getAuthorName() != null && !b.getAuthorName().isBlank()) {
            sb.append("　撰稿人：").append(b.getAuthorName());
        }
        if (b.getPublishDate() != null) {
            sb.append("　发布日期：").append(b.getPublishDate().format(DATE_FORMAT));
        }
        return sb.toString();
    }

    /**
     * 从 .ttc 字体集里找出简体中文字体（PostScript 名含 CJKsc，如
     * NotoSansCJKsc-Regular）。找不到时抛错，不生成无中文的假 PDF。
     */
    static BaseFont resolveCjkFont(List<String> candidates, boolean bold) {
        BaseFont cached = bold ? cachedBold : cachedRegular;
        if (cached != null) {
            return cached;
        }
        synchronized (BriefingPdfUtil.class) {
            cached = bold ? cachedBold : cachedRegular;
            if (cached != null) {
                return cached;
            }
            for (String path : candidates) {
                for (int index = 0; index < 5; index++) {
                    try {
                        BaseFont font = BaseFont.createFont(path + "," + index,
                                BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        String psName = font.getPostscriptFontName();
                        if (psName != null && psName.contains("CJKsc")) {
                            log.info("PDF 中文字体：{} 下标 {}（{}）", path, index, psName);
                            if (bold) {
                                cachedBold = font;
                            } else {
                                cachedRegular = font;
                            }
                            return font;
                        }
                    } catch (Exception e) {
                        // 该下标不是可用字体，继续试下一个
                        log.debug("PDF 字体候选不可用：{} 下标 {}：{}", path, index, e.getMessage());
                    }
                }
            }
            // 粗体找不到时退回常规体；常规体找不到则明确报错
            if (bold) {
                return resolveCjkFont(REGULAR_CANDIDATES, false);
            }
            throw new IllegalStateException("本机未找到可用的简体中文字体，无法生成 PDF，请改用 Word 导出");
        }
    }
}
