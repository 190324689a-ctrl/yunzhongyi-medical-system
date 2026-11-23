package com.yunzhongyi.medical.coresystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.coresystem.dto.AbolishRequest;
import com.yunzhongyi.medical.coresystem.dto.CoreSystemQuery;
import com.yunzhongyi.medical.coresystem.dto.PublishRequest;
import com.yunzhongyi.medical.coresystem.entity.CoreSystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 核心制度管理服务
 */
public interface CoreSystemService extends IService<CoreSystem> {

    /** 多条件组合分页查询 */
    PageResult<CoreSystem> pageQuery(CoreSystemQuery query);

    /** 按同样条件查全部 */
    List<CoreSystem> listByQuery(CoreSystemQuery query);

    /**
     * 制度统计：制度总数 / 草稿 / 已发布 / 已废止 / 按类别分组 /
     * 按层级分组 / 30 天内到期数 / 已过期数 / 按发布月份分组 /
     * 查阅总人次 / 已学习人次 / 有查阅记录的制度数
     */
    Map<String, Object> stats();

    /** 按制度类别汇总统计 */
    List<Map<String, Object>> statsByCategory();

    /** 按适用层级汇总统计 */
    List<Map<String, Object>> statsByLevel();

    /** 按月发布趋势统计（发布月份、发布数，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 详情 */
    CoreSystem detail(Long id);

    /**
     * 发布（一级审批）：只有草稿能发布。审批人必填；发布日期默认为当天，
     * 生效日期为空时默认当天；审批记录留痕。发布新版本时，同一编号的
     * 其他已发布版本自动废止（废止原因“已被新版本 Vx.x 替代”）。
     */
    CoreSystem publish(Long id, PublishRequest request);

    /** 废止：只有已发布的制度能废止，废止原因必填 */
    CoreSystem abolish(Long id, AbolishRequest request);

    /**
     * 新建版本：只有已发布的制度能新建版本。生成一条草稿记录（制度
     * 编号相同、版本号 +1、正文与适用范围、归口科室沿用原制度），发布日期/
     * 生效日期/失效日期/审批信息清空，由新版本发布时重填。
     */
    CoreSystem newVersion(Long id);

    /** 下载文本：按正文生成 .txt 文件内容（在线阅读的正文即制度本身，不引入真实文件存储） */
    String downloadText(Long id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(CoreSystemQuery query, OutputStream out);

    /** 批量导入 Excel（导入的均为草稿，需走发布审批流程），返回成功导入条数 */
    int importExcel(InputStream in);
}
