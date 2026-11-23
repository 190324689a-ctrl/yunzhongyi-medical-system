package com.yunzhongyi.medical.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.document.dto.AbolishRequest;
import com.yunzhongyi.medical.document.dto.DocumentQuery;
import com.yunzhongyi.medical.document.dto.PublishRequest;
import com.yunzhongyi.medical.document.entity.Document;
import com.yunzhongyi.medical.document.excel.DocumentExcelUtil;
import com.yunzhongyi.medical.document.mapper.DocumentMapper;
import com.yunzhongyi.medical.document.service.DocumentService;
import com.yunzhongyi.medical.document.service.DocumentTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件制度管理服务实现：文件 CRUD + 发布/作废/新建版本 + 统计 + Excel 批量导入导出 + 文本下载
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl
        extends ServiceImpl<DocumentMapper, Document>
        implements DocumentService {

    @Override
    public PageResult<Document> pageQuery(DocumentQuery query) {
        Page<Document> p = this.page(new Page<>(query.getPage(), query.getSize()), buildWrapper(query));
        return PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords());
    }

    @Override
    public List<Document> listByQuery(DocumentQuery query) {
        return this.list(buildWrapper(query));
    }

    @Override
    public Map<String, Object> stats() {
        List<Document> all = this.list();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", (long) all.size());
        r.put("draft", all.stream()
                .filter(d -> Integer.valueOf(DocumentTypeUtil.STATUS_DRAFT).equals(d.getStatus())).count());
        r.put("published", all.stream()
                .filter(d -> Integer.valueOf(DocumentTypeUtil.STATUS_PUBLISHED).equals(d.getStatus())).count());
        r.put("abolished", all.stream()
                .filter(d -> Integer.valueOf(DocumentTypeUtil.STATUS_ABOLISHED).equals(d.getStatus())).count());
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (String c : List.of(DocumentTypeUtil.CATEGORY_SYSTEM, DocumentTypeUtil.CATEGORY_STANDARD,
                DocumentTypeUtil.CATEGORY_PROCESS, DocumentTypeUtil.CATEGORY_FILE,
                DocumentTypeUtil.CATEGORY_OTHER)) {
            byCategory.put(c, all.stream().filter(d -> c.equals(d.getCategory())).count());
        }
        r.put("byCategory", byCategory);
        r.put("byCategoryList", statsByCategory());
        r.put("byMonth", statsByMonth());

        // 30 天内到期：已发布、未过期、失效日期不晚于今天 + 30 天
        r.put("expiringSoon", all.stream()
                .filter(d -> DocumentTypeUtil.isExpiringSoon(d.getStatus(), d.getExpiryDate())).count());
        // 已过期：已发布且失效日期已过当天
        r.put("expired", all.stream()
                .filter(d -> DocumentTypeUtil.isExpired(d.getStatus(), d.getExpiryDate())).count());
        return r;
    }

    @Override
    public List<Map<String, Object>> statsByCategory() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String c : List.of(DocumentTypeUtil.CATEGORY_SYSTEM, DocumentTypeUtil.CATEGORY_STANDARD,
                DocumentTypeUtil.CATEGORY_PROCESS, DocumentTypeUtil.CATEGORY_FILE,
                DocumentTypeUtil.CATEGORY_OTHER)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", c);
            row.put("categoryText", DocumentTypeUtil.categoryText(c));
            row.put("count", this.count(new LambdaQueryWrapper<Document>()
                    .eq(Document::getCategory, c)));
            row.put("publishedCount", this.count(new LambdaQueryWrapper<Document>()
                    .eq(Document::getCategory, c)
                    .eq(Document::getStatus, DocumentTypeUtil.STATUS_PUBLISHED)));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> statsByMonth() {
        Map<String, Long> grouped = new LinkedHashMap<>();
        for (Document d : this.list()) {
            if (d.getPublishDate() == null) {
                continue;
            }
            String month = d.getPublishDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            grouped.merge(month, 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        grouped.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", e.getKey());
            row.put("count", e.getValue());
            result.add(row);
        });
        return result;
    }

    @Override
    public Document detail(Long id) {
        return getOrThrow(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document publish(Long id, PublishRequest request) {
        Document d = getOrThrow(id);
        if (!Integer.valueOf(DocumentTypeUtil.STATUS_DRAFT).equals(d.getStatus())) {
            throw new IllegalStateException("只有草稿才能发布（当前状态："
                    + DocumentTypeUtil.statusText(d.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getApprover())) {
            throw new IllegalArgumentException("发布审批人不能为空");
        }
        d.setApprover(request.getApprover().trim());
        LocalDate today = LocalDate.now();
        d.setPublishDate(today);
        if (d.getEffectiveDate() == null) {
            d.setEffectiveDate(today);
        }
        if (d.getExpiryDate() != null && d.getExpiryDate().isBefore(d.getEffectiveDate())) {
            throw new IllegalArgumentException("失效日期不能早于生效日期");
        }
        d.setStatus(DocumentTypeUtil.STATUS_PUBLISHED);
        String opinion = StringUtils.hasText(request.getOpinion()) ? request.getOpinion().trim() : "同意";
        d.setApprovalHistory(appendHistory(d.getApprovalHistory(),
                "医务科 " + d.getApprover() + "：发布（" + opinion + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有草稿能修改”
        // 守卫（发布中的记录正是要从草稿改为已发布，守卫会误判）
        super.updateById(d);
        // 新版本发布时，同一文件编号的其他已发布版本自动作废
        List<Document> others = this.list(new LambdaQueryWrapper<Document>()
                .eq(Document::getDocNo, d.getDocNo())
                .eq(Document::getStatus, DocumentTypeUtil.STATUS_PUBLISHED)
                .ne(Document::getId, d.getId()));
        for (Document old : others) {
            old.setStatus(DocumentTypeUtil.STATUS_ABOLISHED);
            old.setAbolishReason("已被新版本 " + d.getVersion() + " 替代");
            old.setApprovalHistory(appendHistory(old.getApprovalHistory(),
                    "系统：因新版本 " + d.getVersion() + " 发布自动作废"));
            super.updateById(old);
        }
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document abolish(Long id, AbolishRequest request) {
        Document d = getOrThrow(id);
        if (!Integer.valueOf(DocumentTypeUtil.STATUS_PUBLISHED).equals(d.getStatus())) {
            throw new IllegalStateException("只有已发布的文件才能作废（当前状态："
                    + DocumentTypeUtil.statusText(d.getStatus()) + "）");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("作废原因不能为空");
        }
        d.setStatus(DocumentTypeUtil.STATUS_ABOLISHED);
        d.setAbolishReason(request.getReason().trim());
        d.setApprovalHistory(appendHistory(d.getApprovalHistory(),
                "医务科 " + (d.getApprover() == null ? "" : d.getApprover()) + "：作废（" + d.getAbolishReason() + "）"));
        // 直接调 super.updateById，跳过本类 updateById 的“只有草稿能修改”守卫
        super.updateById(d);
        return d;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document newVersion(Long id) {
        Document old = getOrThrow(id);
        if (!Integer.valueOf(DocumentTypeUtil.STATUS_PUBLISHED).equals(old.getStatus())) {
            throw new IllegalStateException("只有已发布的文件才能新建版本（当前状态："
                    + DocumentTypeUtil.statusText(old.getStatus()) + "）");
        }
        String nextVersion = DocumentTypeUtil.nextVersion(old.getVersion());
        if (this.count(new LambdaQueryWrapper<Document>()
                .eq(Document::getDocNo, old.getDocNo())
                .eq(Document::getVersion, nextVersion)) > 0) {
            throw new IllegalArgumentException("该文件编号已有 " + nextVersion + " 版本，请先处理该版本");
        }
        Document next = new Document();
        next.setTitle(old.getTitle());
        next.setDocNo(old.getDocNo());
        next.setCategory(old.getCategory());
        next.setVersion(nextVersion);
        next.setContent(old.getContent());
        next.setDepartment(old.getDepartment());
        next.setDrafter(old.getDrafter());
        next.setApprover(null);
        next.setPublishDate(null);
        next.setEffectiveDate(null);
        next.setExpiryDate(null);
        next.setStatus(DocumentTypeUtil.STATUS_DRAFT);
        next.setAbolishReason(null);
        next.setApprovalHistory(null);
        next.setRemark(old.getRemark());
        normalize(next, true);
        super.save(next);
        return next;
    }

    @Override
    public String downloadText(Long id) {
        Document d = getOrThrow(id);
        StringBuilder sb = new StringBuilder();
        sb.append(d.getTitle()).append("\n");
        sb.append("文件编号：").append(d.getDocNo()).append("\n");
        sb.append("分类：").append(DocumentTypeUtil.categoryText(d.getCategory())).append("\n");
        sb.append("版本：").append(d.getVersion() == null ? "" : d.getVersion()).append("\n");
        sb.append("归口科室：").append(d.getDepartment() == null ? "" : d.getDepartment()).append("\n");
        sb.append("状态：").append(DocumentTypeUtil.statusText(d.getStatus())).append("\n");
        sb.append("发布日期：").append(DocumentTypeUtil.fmtDate(d.getPublishDate())).append("\n");
        sb.append("生效日期：").append(DocumentTypeUtil.fmtDate(d.getEffectiveDate())).append("\n");
        sb.append("失效日期：").append(DocumentTypeUtil.fmtDate(d.getExpiryDate())).append("\n");
        sb.append("\n");
        sb.append(d.getContent() == null ? "" : d.getContent()).append("\n");
        return sb.toString();
    }

    @Override
    public void exportExcel(DocumentQuery query, OutputStream out) {
        DocumentExcelUtil.write(listByQuery(query), out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(InputStream in) {
        List<Document> list = DocumentExcelUtil.read(in);
        int ok = 0;
        for (Document d : list) {
            if (!StringUtils.hasText(d.getTitle()) || !StringUtils.hasText(d.getDocNo())) {
                log.warn("导入跳过：文件标题或文件编号为空的行");
                continue;
            }
            if (!StringUtils.hasText(d.getCategory()) || !DocumentTypeUtil.isValidCategory(d.getCategory())) {
                log.warn("导入跳过：文件分类为空或不正确（文件 {}）", d.getTitle());
                continue;
            }
            // 导入的均为草稿，需走发布审批流程；正文不进 Excel，导入后在页面补录
            d.setStatus(DocumentTypeUtil.STATUS_DRAFT);
            if (!StringUtils.hasText(d.getVersion())) {
                d.setVersion("V1.0");
            }
            d.setApprover(null);
            d.setPublishDate(null);
            d.setApprovalHistory(null);
            d.setAbolishReason(null);
            // 同一文件编号同一版本视为重复，跳过
            if (this.count(new LambdaQueryWrapper<Document>()
                    .eq(Document::getDocNo, d.getDocNo())
                    .eq(Document::getVersion, d.getVersion())) > 0) {
                log.warn("导入跳过：重复文件 {} {}", d.getDocNo(), d.getVersion());
                continue;
            }
            try {
                this.save(d);
                ok++;
            } catch (IllegalArgumentException e) {
                log.warn("导入跳过：{}（{}）", d.getTitle(), e.getMessage());
            }
        }
        return ok;
    }

    /** 删除：已发布的文件请先作废，草稿/已作废可直接删除 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        Document d = getOrThrow((Long) id);
        if (Integer.valueOf(DocumentTypeUtil.STATUS_PUBLISHED).equals(d.getStatus())) {
            throw new IllegalStateException("已发布的文件不能直接删除，请先作废");
        }
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(java.util.Collection<?> idList) {
        for (Object id : idList) {
            removeById((java.io.Serializable) id);
        }
        return true;
    }

    /** 多条件组合查询条件构造 */
    private LambdaQueryWrapper<Document> buildWrapper(DocumentQuery q) {
        LambdaQueryWrapper<Document> qw = new LambdaQueryWrapper<>();
        qw.and(StringUtils.hasText(q.getKeyword()), w -> w
                .like(Document::getTitle, q.getKeyword())
                .or().like(Document::getDocNo, q.getKeyword()));
        qw.eq(StringUtils.hasText(q.getCategory()), Document::getCategory, q.getCategory());
        qw.eq(q.getStatus() != null, Document::getStatus, q.getStatus());
        if (Boolean.TRUE.equals(q.getExpiringSoon())) {
            // 30 天内到期：只看已发布的，失效日期在今天 ~ 今天 + 30 天之间
            qw.eq(Document::getStatus, DocumentTypeUtil.STATUS_PUBLISHED);
            qw.ge(Document::getExpiryDate, LocalDate.now());
            qw.le(Document::getExpiryDate, LocalDate.now().plusDays(DocumentTypeUtil.EXPIRY_WARN_DAYS));
        }
        if (Boolean.TRUE.equals(q.getExpired())) {
            // 已过期：只看已发布的，失效日期已过当天
            qw.eq(Document::getStatus, DocumentTypeUtil.STATUS_PUBLISHED);
            qw.lt(Document::getExpiryDate, LocalDate.now());
        }
        qw.orderByDesc(Document::getId);
        return qw;
    }

    /** 新建时补默认值并校验；更新时校验分类与日期 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Document entity) {
        normalize(entity, true);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Document entity) {
        // 只有草稿能修改：已发布的文件改版走新建版本，已作废的文件不可再改
        Document old = getOrThrow(entity.getId());
        if (!Integer.valueOf(DocumentTypeUtil.STATUS_DRAFT).equals(old.getStatus())) {
            throw new IllegalStateException("只有草稿才能修改（当前状态："
                    + DocumentTypeUtil.statusText(old.getStatus()) + "），已发布的文件请新建版本");
        }
        normalize(entity, false);
        return super.updateById(entity);
    }

    private void normalize(Document d, boolean isCreate) {
        if (!StringUtils.hasText(d.getCategory()) || !DocumentTypeUtil.isValidCategory(d.getCategory())) {
            throw new IllegalArgumentException("文件分类不正确");
        }
        if (d.getEffectiveDate() != null && d.getExpiryDate() != null
                && d.getExpiryDate().isBefore(d.getEffectiveDate())) {
            throw new IllegalArgumentException("失效日期不能早于生效日期");
        }
        if (d.getStatus() != null
                && !Integer.valueOf(DocumentTypeUtil.STATUS_DRAFT).equals(d.getStatus())
                && !Integer.valueOf(DocumentTypeUtil.STATUS_PUBLISHED).equals(d.getStatus())
                && !Integer.valueOf(DocumentTypeUtil.STATUS_ABOLISHED).equals(d.getStatus())) {
            throw new IllegalArgumentException("文件状态不正确");
        }
        if (isCreate) {
            if (d.getStatus() == null) {
                d.setStatus(DocumentTypeUtil.STATUS_DRAFT);
            }
            if (!StringUtils.hasText(d.getVersion())) {
                d.setVersion("V1.0");
            }
            // 同一文件编号同一版本已有记录，拒绝重复建档
            LambdaQueryWrapper<Document> qw = new LambdaQueryWrapper<Document>()
                    .eq(Document::getDocNo, d.getDocNo())
                    .eq(Document::getVersion, d.getVersion());
            if (this.count(qw) > 0) {
                throw new IllegalArgumentException("文件编号 " + d.getDocNo() + " 的 " + d.getVersion()
                        + " 版本已有记录，请勿重复建档");
            }
        }
    }

    private String appendHistory(String history, String entry) {
        if (!StringUtils.hasText(history)) {
            return entry;
        }
        return history + "；" + entry;
    }

    private Document getOrThrow(Long id) {
        Document d = this.getById(id);
        if (d == null) {
            throw new IllegalArgumentException("文件不存在：" + id);
        }
        return d;
    }
}
