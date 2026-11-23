package com.yunzhongyi.medical.sysconfig.dto;

import lombok.Data;

/** 知识库多条件组合查询参数 */
@Data
public class KnowledgeItemQuery {
    /** 关键字：标题 / 正文 / 关键词模糊匹配 */
    private String keyword;
    /** 知识分类：制度规范 / 操作指南 / 常见问题 / 法律法规 / 培训资料 / 其他 */
    private String category;
    /** 状态：1 启用，0 停用 */
    private Integer status;
    private long page = 1;
    private long size = 10;
}
