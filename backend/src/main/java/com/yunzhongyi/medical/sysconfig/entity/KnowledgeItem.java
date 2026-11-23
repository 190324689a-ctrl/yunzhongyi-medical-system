package com.yunzhongyi.medical.sysconfig.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库条目：医务系统内置知识库的一条知识。
 * <p>
 * 覆盖需求（需求梳理.md 第 28 项）：支持 3000+ 条知识库。
 * 3000+ 是容量口径：分页查询 + 关键字全文检索（标题/正文/关键词）+ 按分类筛选，
 * 单表可承载远超 3000 条；种子数据先写入 8 条代表性知识，验证增删改查与导入导出。
 * <p>
 * 知识分类 category：制度规范 / 操作指南 / 常见问题 / 法律法规 / 培训资料 / 其他。
 * 状态 status：1 启用（可检索），0 停用（不出现在检索结果里）。
 * 查阅量 viewCount：每次打开详情自动 +1。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_knowledge")
public class KnowledgeItem extends BaseEntity {

    /** 知识编码（唯一），如 KB-001 */
    @NotBlank(message = "知识编码不能为空")
    private String kbCode;

    /** 知识标题 */
    @NotBlank(message = "知识标题不能为空")
    private String title;

    /** 知识分类：制度规范 / 操作指南 / 常见问题 / 法律法规 / 培训资料 / 其他 */
    private String category;

    /** 知识正文 */
    private String content;

    /** 关键词（逗号分隔，用于检索） */
    private String keywords;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 查阅量 */
    private Long viewCount;

    /** 排序值（越小越靠前） */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}
