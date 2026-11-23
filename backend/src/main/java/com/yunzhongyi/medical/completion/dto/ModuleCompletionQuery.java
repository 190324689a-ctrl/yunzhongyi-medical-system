package com.yunzhongyi.medical.completion.dto;

import lombok.Data;

/**
 * 模块完成情况多条件组合查询参数
 */
@Data
public class ModuleCompletionQuery {

    /** 关键字：模块名称模糊匹配 */
    private String keyword;

    /** 分组：医师管理 / 质量管理 / 组织与制度 / 科研与统计 / 系统与移动 */
    private String category;

    /** 状态：0 待开发，1 进行中，2 已完成 */
    private Integer status;

    /** 模块编号 */
    private Integer moduleNo;

    private long page = 1;
    private long size = 10;
}
