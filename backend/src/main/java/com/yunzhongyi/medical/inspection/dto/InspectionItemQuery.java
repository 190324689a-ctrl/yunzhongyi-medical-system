package com.yunzhongyi.medical.inspection.dto;

import lombok.Data;

/**
 * 检查项目多条件组合查询参数
 */
@Data
public class InspectionItemQuery {

    /** 关键字：项目名称 / 项目编号模糊匹配 */
    private String keyword;

    /** 项目分类：MEDICAL_RECORD 病历质量 / CORE_SYSTEM 核心制度 / INFECTION 医院感染 / PHARMACY 药事管理 / NURSING 护理质量 / SAFETY 医疗安全 / SERVICE 服务质量 / TCM 中医特色 / MGMT 科室管理 / OTHER 其他 */
    private String category;

    /** 检查方式：ON_SITE 现场查看 / RECORD_REVIEW 抽查病历 / INTERVIEW 访谈 / DOC_REVIEW 资料查阅 */
    private String checkMethod;

    /** 依据标准 id */
    private Long standardId;

    /** 适用检查类型：ALL 通用 / SELF 科室自查 / HOSPITAL 院级检查 / SPECIAL 专项检查 */
    private String applicableType;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 只看已被模板引用（已组装进至少一个模板） */
    private Boolean linked;

    /** 只看未被任何模板引用 */
    private Boolean unlinked;

    private long page = 1;
    private long size = 10;
}
