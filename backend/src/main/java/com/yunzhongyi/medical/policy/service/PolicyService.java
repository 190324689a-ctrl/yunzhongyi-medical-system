package com.yunzhongyi.medical.policy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunzhongyi.medical.common.PageResult;
import com.yunzhongyi.medical.policy.dto.AbolishRequest;
import com.yunzhongyi.medical.policy.dto.PolicyQuery;
import com.yunzhongyi.medical.policy.dto.PublishRequest;
import com.yunzhongyi.medical.policy.entity.Policy;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 政策法规管理服务
 */
public interface PolicyService extends IService<Policy> {

    /** 多条件组合分页查询 */
    PageResult<Policy> pageQuery(PolicyQuery query);

    /** 按同样条件查全部 */
    List<Policy> listByQuery(PolicyQuery query);

    /**
     * 法规统计：法规总数 / 草稿 / 已发布 / 已废止 / 按分类分组 /
     * 按层级分组 / 30 天内到期数 / 已过期数 / 按发布月份分组 /
     * 查阅总人次 / 已学习人次 / 有查阅记录的法规数
     */
    Map<String, Object> stats();

    /** 按法规分类汇总统计 */
    List<Map<String, Object>> statsByCategory();

    /** 按发布层级汇总统计 */
    List<Map<String, Object>> statsByLevel();

    /** 按月发布趋势统计（发布月份、发布数，按月份升序） */
    List<Map<String, Object>> statsByMonth();

    /** 详情 */
    Policy detail(Long id);

    /**
     * 发布（一级审批）：只有草稿能发布。审批人必填；发布日期默认为当天，
     * 生效日期为空时默认当天；审批记录留痕。发布新版本时，同一文号的
     * 其他已发布版本自动废止（废止原因“已被新版本 Vx.x 替代”）。
     */
    Policy publish(Long id, PublishRequest request);

    /** 废止：只有已发布的法规能废止，废止原因必填 */
    Policy abolish(Long id, AbolishRequest request);

    /**
     * 新建版本：只有已发布的法规能新建版本。生成一条草稿记录（文号
     * 相同、版本号 +1、正文与适用范围、归口科室沿用原法规），发布日期/
     * 生效日期/失效日期/审批信息清空，由新版本发布时重填。
     */
    Policy newVersion(Long id);

    /** 下载文本：按正文生成 .txt 文件内容（在线阅读的正文即法规本身，不引入真实文件存储） */
    String downloadText(Long id);

    /** 导出 Excel（按当前查询条件） */
    void exportExcel(PolicyQuery query, OutputStream out);

    /** 批量导入 Excel（导入的均为草稿，需走发布审批流程），返回成功导入条数 */
    int importExcel(InputStream in);
}
