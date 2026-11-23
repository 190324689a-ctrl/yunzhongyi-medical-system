package com.yunzhongyi.medical.document.dto;

import lombok.Data;

/**
 * 文件制度多条件组合查询参数
 */
@Data
public class DocumentQuery {

    /** 关键字：文件标题 / 文件编号模糊匹配 */
    private String keyword;

    /** 分类：SYSTEM 制度 / STANDARD 规范 / PROCESS 流程 / FILE 文件 / OTHER 其他 */
    private String category;

    /** 状态：0 草稿，1 已发布，2 已作废 */
    private Integer status;

    /** 只看 30 天内到期（已发布、未过期） */
    private Boolean expiringSoon;

    /** 只看已过期（已发布且失效日期已过当天） */
    private Boolean expired;

    private long page = 1;
    private long size = 10;
}
