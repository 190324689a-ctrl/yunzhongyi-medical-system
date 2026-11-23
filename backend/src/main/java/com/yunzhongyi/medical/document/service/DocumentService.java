package com.yunzhongyi.medical.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.document.dto.AbolishRequest;
import com.yunzhongyi.medical.document.dto.DocumentQuery;
import com.yunzhongyi.medical.document.dto.PublishRequest;
import com.yunzhongyi.medical.document.entity.Document;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 文件制度管理服务
 */
public interface DocumentService extends IService<Document> {

    /** 多条件组合分页查询 */
    PageResult<Document> pageQuery(DocumentQuery query);

    /** 按同样条件查全部 */
    List<Document> listByQuery(DocumentQuery query);

    /**
     * 文件统计：文件总数 / 草稿 / 已发布 / 已作废 / 按分类分组 /
     * 30 天内到期数 / 已过期数 / 按发布月份分组
     */
    Map<String, Object> stats();

    /** 按文件分类汇总统计 */
    List<Map<String, Object>> statsByCategory();

    /** 按月发布趋势统计（发布月份、发布数，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 详情 */
    Document detail(Long id);

    /**
     * 发布（一级审批）：只有草稿能发布。审批人必填；发布日期默认为当天，
     * 生效日期为空时默认当天；审批记录留痕。发布新版本时，同一文件编号的
     * 其他已发布版本自动作废（作废原因“已被新版本 Vx.x 替代”）。
     */
    Document publish(Long id, PublishRequest request);

    /** 作废：只有已发布的文件能作废，作废原因必填 */
    Document abolish(Long id, AbolishRequest request);

    /**
     * 新建版本：只有已发布的文件能新建版本。生成一条草稿记录（文件编号
     * 相同、版本号 +1、正文与归口科室沿用原文件），发布日期/生效日期/
     * 失效日期/审批信息清空，由新版本发布时重填。
     */
    Document newVersion(Long id);

    /** 下载文本：按正文生成 .txt 文件内容（在线阅读的正文即文件本身，不引入真实文件存储） */
    String downloadText(Long id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(DocumentQuery query, OutputStream out);

    /** 批量导入 Excel（导入的均为草稿，需走发布审批流程），返回成功导入条数 */
    int importExcel(InputStream in);
}
