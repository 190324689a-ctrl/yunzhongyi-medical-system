package com.yunzhongyi.medical.completion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 医务系统模块完成情况：需求清单里一个模块的完成台账。
 * <p>
 * 覆盖需求（需求梳理.md 第 27 项）：医务系统各模块完成情况统计。
 * <p>
 * 一行对应需求清单里的一个模块（moduleNo 1-31，全院唯一），记录它的
 * 完成状态、进度、接口数、测试数与完成日期；统计接口直接对这张表现算。
 * <p>
 * 状态 status：0 待开发，1 进行中，2 已完成。已完成时 progress 强制为 100、
 * 完成日期为空时默认当天；未完成时完成日期清空（不存“计划完成日”）。
 * <p>
 * 接口数 / 测试数 interfaceCount / testCount 为空表示“计入其他模块”
 * （如第 14/15 项合入第 13 项质量检查管理、第 25/26 项测试计入第 24 项），
 * 统计合计时按 0 计，不虚增。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("module_completion")
public class ModuleCompletion extends BaseEntity {

    /** 模块编号：需求清单序号 1-31，全院唯一 */
    @NotNull(message = "模块编号不能为空")
    private Integer moduleNo;

    /** 模块名称，如 医师档案管理 */
    @NotBlank(message = "模块名称不能为空")
    private String moduleName;

    /** 分组：医师管理 / 质量管理 / 组织与制度 / 科研与统计 / 系统与移动 */
    @NotBlank(message = "分组不能为空")
    private String category;

    /** 状态：0 待开发，1 进行中，2 已完成 */
    private Integer status;

    /** 完成进度 0-100 */
    @Min(value = 0, message = "完成进度不能小于 0")
    @Max(value = 100, message = "完成进度不能大于 100")
    private Integer progress;

    /** 接口数（为空表示计入其他模块，统计时按 0 计） */
    private Integer interfaceCount;

    /** 测试数（为空表示计入其他模块，统计时按 0 计） */
    private Integer testCount;

    /** 完成日期（仅已完成模块有） */
    private LocalDate completedDate;

    /** 备注（如合入说明、HIS 预留占位） */
    private String remark;
}
