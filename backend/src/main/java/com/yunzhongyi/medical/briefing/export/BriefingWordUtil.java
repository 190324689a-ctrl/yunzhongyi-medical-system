package com.yunzhongyi.medical.briefing.export;

import com.yunzhongyi.medical.briefing.entity.Briefing;
import com.yunzhongyi.medical.briefing.service.BriefingTypeUtil;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

/**
 * 医疗工作简报 Word 导出（Apache POI XWPF，真实 .docx 文件）。
 * <p>
 * 版式：标题居中加粗 16 号；第二行写期号 / 分类 / 统计周期 / 撰稿人 /
 * 发布日期；正文按行分段，栏目小标题（以“：”结尾或“、”开头的序号行）加粗。
 */
public final class BriefingWordUtil {

    private BriefingWordUtil() {
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void write(Briefing briefing, OutputStream out) {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText(briefing.getTitle() == null ? "" : briefing.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            XWPFParagraph meta = doc.createParagraph();
            meta.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun metaRun = meta.createRun();
            metaRun.setText(buildMetaLine(briefing));
            metaRun.setFontSize(10);

            String content = briefing.getContent() == null ? "" : briefing.getContent();
            for (String line : content.split("\\r?\\n")) {
                if (line.isBlank()) {
                    continue;
                }
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setText(line.trim());
                run.setFontSize(11);
                if (isSectionHeader(line.trim())) {
                    run.setBold(true);
                    run.setFontSize(12);
                }
            }
            doc.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出 Word 失败: " + e.getMessage(), e);
        }
    }

    /** 正文纯文本：标题 + 基本信息 + 正文，供 .txt 下载用 */
    public static String toPlainText(Briefing briefing) {
        StringBuilder sb = new StringBuilder();
        sb.append(briefing.getTitle() == null ? "" : briefing.getTitle()).append("\n");
        sb.append(buildMetaLine(briefing)).append("\n\n");
        sb.append(briefing.getContent() == null ? "" : briefing.getContent()).append("\n");
        return sb.toString();
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

    /** 栏目小标题：以中文冒号结尾的行，或“一、二、”开头的序号行 */
    static boolean isSectionHeader(String line) {
        if (line.endsWith("：") || line.endsWith(":")) {
            return true;
        }
        return line.matches("^[一二三四五六七八九十]+、.*") || line.matches("^\\d+[.、].*");
    }
}
