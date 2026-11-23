package com.yunzhongyi.medical.mobile.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yunzhongyi.medical.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 移动端审批操作记录：在手机上对各业务待审批事项执行通过 / 驳回后留痕。
 * <p>
 * 覆盖需求（需求梳理.md 第 31 项）：移动端审批。
 * 本表只记录“在移动端执行了审批”这一事实，审批本身复用各业务模块
 * 已有的审批逻辑（休假执业 / 重点授权 / 排班调班 / MDT / 新技术新项目 /
 * 医疗人员动态 / 医疗分级 / 医疗工作简报），不在此另造审批流。
 * 业务模块 moduleType：LEAVE 休假执业 / AUTH 重点授权 / SCHEDULE 排班调班 /
 * MDT 多学科会诊 / NEWTECH 新技术新项目 / PERSONNEL 医疗人员动态 /
 * MEDGRADING 医疗分级 / BRIEFING 医疗工作简报。
 * 操作 action：APPROVE 通过 / REJECT 驳回；结果 result：SUCCESS 成功 / FAIL 失败；
 * 审批失败时同样记一条 FAIL 记录（写明失败原因），再把业务模块返回的错误抛给调用方。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mobile_approval_log")
public class MobileApprovalLog extends BaseEntity {

    /** 业务模块：LEAVE / AUTH / SCHEDULE / MDT / NEWTECH / PERSONNEL / MEDGRADING / BRIEFING */
    private String moduleType;

    /** 关联业务 id */
    private Long relatedId;

    /** 关联业务标题（审批时快照，如“孙小红休假申请”） */
    private String relatedTitle;

    /** 操作：APPROVE 通过 / REJECT 驳回 */
    private String action;

    /** 审批人（用户名） */
    private String operator;

    /** 执行审批的设备编码 */
    private String deviceCode;

    /** 审批意见（驳回时为驳回原因） */
    private String opinion;

    /** 结果：SUCCESS 成功 / FAIL 失败 */
    private String result;

    /** 失败原因（成功时为空） */
    private String failReason;

    /** 操作时间 */
    private LocalDateTime actionTime;

    /** 备注 */
    private String remark;
}
