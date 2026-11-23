package com.yunzhongyi.medical;

import com.yunzhongyi.medical.ethics.dto.ApproveRequest;
import com.yunzhongyi.medical.ethics.dto.EthicsCommendationQuery;
import com.yunzhongyi.medical.ethics.dto.EthicsComplaintQuery;
import com.yunzhongyi.medical.ethics.dto.EthicsEvaluationQuery;
import com.yunzhongyi.medical.ethics.dto.EthicsProfileQuery;
import com.yunzhongyi.medical.ethics.dto.FeedbackRequest;
import com.yunzhongyi.medical.ethics.dto.HandleRequest;
import com.yunzhongyi.medical.ethics.dto.RejectRequest;
import com.yunzhongyi.medical.ethics.entity.EthicsCommendation;
import com.yunzhongyi.medical.ethics.entity.EthicsComplaint;
import com.yunzhongyi.medical.ethics.entity.EthicsEvaluation;
import com.yunzhongyi.medical.ethics.entity.EthicsProfile;
import com.yunzhongyi.medical.ethics.entity.EthicsTemplate;
import com.yunzhongyi.medical.ethics.excel.EthicsCommendationExcelUtil;
import com.yunzhongyi.medical.ethics.excel.EthicsComplaintExcelUtil;
import com.yunzhongyi.medical.ethics.excel.EthicsEvaluationExcelUtil;
import com.yunzhongyi.medical.ethics.excel.EthicsProfileExcelUtil;
import com.yunzhongyi.medical.ethics.service.EthicsCommendationService;
import com.yunzhongyi.medical.ethics.service.EthicsComplaintService;
import com.yunzhongyi.medical.ethics.service.EthicsEvaluationService;
import com.yunzhongyi.medical.ethics.service.EthicsProfileService;
import com.yunzhongyi.medical.ethics.service.EthicsTemplateService;
import com.yunzhongyi.medical.ethics.service.EthicsTypeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 医德医风管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 份档案（中医内科/针灸推拿科/骨伤科/妇科/儿科/急诊科各 1 人，全部在档；
 * 最近考评等级优秀 1 / 合格 2 / 基本合格 1 / 不合格 1，刘志强待审批故为空）、
 * 2 个考评模板（年度/季度，权重之和均为 100）、
 * 6 条考评记录（优秀 2 / 合格 2 / 基本合格 1 / 不合格 1，平均分 80.33，优秀率 33.33%；
 * 已通过 5 条、待审批 1 条）、
 * 6 条投诉（服务态度 2 / 医疗质量 1 / 收费问题 1 / 廉洁行医 1 / 其他 1；
 * 待受理 1 / 调查中 2 / 已处理 1 / 已反馈 1 / 不予受理 1；逾期 1 条，反馈率 16.67%）、
 * 6 条表彰（锦旗 2 / 表扬信 1 / 通报表扬 2 / 荣誉称号 1；院级 4 / 市级 1 / 省级 1；
 * 已通过 4 条、待审批 1 条、已驳回 1 条）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class EthicsTests {

    @Autowired
    private EthicsProfileService profileService;

    @Autowired
    private EthicsTemplateService templateService;

    @Autowired
    private EthicsEvaluationService evaluationService;

    @Autowired
    private EthicsComplaintService complaintService;

    @Autowired
    private EthicsCommendationService commendationService;

    @Test
    void contextLoads() {
        assertNotNull(profileService);
        assertNotNull(templateService);
        assertNotNull(evaluationService);
        assertNotNull(complaintService);
        assertNotNull(commendationService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(profileService.count() >= 6);
        assertTrue(templateService.count() >= 2);
        assertTrue(evaluationService.count() >= 6);
        assertTrue(complaintService.count() >= 6);
        assertTrue(commendationService.count() >= 6);

        // 种子档案 1：陈国强，中医内科，主任医师，在档，最近考评优秀（2026）
        EthicsProfile p1 = profileService.getById(1L);
        assertEquals("YD-2024-001", p1.getArchiveNo());
        assertEquals("陈国强", p1.getPhysicianName());
        assertEquals("中医内科", p1.getDeptName());
        assertEquals("主任医师", p1.getTitle());
        assertEquals(LocalDate.of(2024, 1, 1), p1.getEstablishDate());
        assertEquals(0, p1.getStatus());
        assertEquals("EXCELLENT", p1.getLastGrade());
        assertEquals(2026, p1.getLastEvaluateYear());

        // 种子档案 6：刘志强，2026 年度考评待审批，档案里最近考评结果为空
        EthicsProfile p6 = profileService.getById(6L);
        assertEquals("YD-2024-006", p6.getArchiveNo());
        assertEquals("刘志强", p6.getPhysicianName());
        assertEquals("急诊科", p6.getDeptName());
        assertNull(p6.getLastGrade());
        assertNull(p6.getLastEvaluateYear());

        // 种子模板 1：医德医风年度考评模板，权重 50/25/25
        EthicsTemplate t1 = templateService.getById(1L);
        assertEquals("医德医风年度考评模板", t1.getTemplateName());
        assertEquals(1, t1.getStatus());
        assertTrue(t1.getScoreItems().contains("医德医风"));
        assertEquals(BigDecimal.valueOf(100), EthicsTypeUtil.sumWeights(t1.getScoreItems()));

        // 种子考评 1：陈国强 95 分优秀，已通过
        EthicsEvaluation e1 = evaluationService.getById(1L);
        assertEquals("陈国强", e1.getPhysicianName());
        assertEquals("医德医风年度考评模板", e1.getTemplateName());
        assertEquals(2026, e1.getAssessYear());
        assertEquals(new BigDecimal("95.00"), e1.getTotalScore());
        assertEquals("EXCELLENT", e1.getGrade());
        assertEquals("优秀", e1.getResult());
        assertEquals(1, e1.getStatus());
        assertTrue(e1.getApprovalHistory().contains("通过"));

        // 种子考评 5：孙小红 55 分不合格，需整改
        EthicsEvaluation e5 = evaluationService.getById(5L);
        assertEquals("孙小红", e5.getPhysicianName());
        assertEquals(new BigDecimal("55.00"), e5.getTotalScore());
        assertEquals("UNQUALIFIED", e5.getGrade());
        assertEquals("不合格，需整改", e5.getResult());

        // 种子考评 6：刘志强 90 分优秀，待审批
        EthicsEvaluation e6 = evaluationService.getById(6L);
        assertEquals("刘志强", e6.getPhysicianName());
        assertEquals(new BigDecimal("90.00"), e6.getTotalScore());
        assertEquals("EXCELLENT", e6.getGrade());
        assertEquals(0, e6.getStatus());
        assertNull(e6.getApprover());

        // 种子投诉 1：张先生投诉陈国强服务态度，已反馈
        EthicsComplaint c1 = complaintService.getById(1L);
        assertEquals("YD-TS-2026-001", c1.getComplaintNo());
        assertEquals("张先生", c1.getComplainantName());
        assertEquals("ATTITUDE", c1.getComplaintType());
        assertEquals(3, c1.getStatus());
        assertEquals(LocalDate.of(2026, 11, 13), c1.getFeedbackDate());
        assertTrue(c1.getApprovalHistory().contains("已向投诉人反馈"));

        // 种子投诉 4：刘女士投诉赵丽华廉洁行医，待受理
        EthicsComplaint c4 = complaintService.getById(4L);
        assertEquals("YD-TS-2026-004", c4.getComplaintNo());
        assertEquals("INTEGRITY", c4.getComplaintType());
        assertEquals(0, c4.getStatus());
        assertEquals(LocalDate.of(2026, 11, 27), c4.getDeadlineDate());

        // 种子投诉 5：陈先生投诉孙小红，处理期限 2026-11-04 已逾期
        EthicsComplaint c5 = complaintService.getById(5L);
        assertEquals("YD-TS-2026-005", c5.getComplaintNo());
        assertEquals(1, c5.getStatus());
        assertTrue(EthicsTypeUtil.isComplaintOverdue(c5.getStatus(), c5.getDeadlineDate()));

        // 种子投诉 6：赵女士投诉刘志强，不予受理
        EthicsComplaint c6 = complaintService.getById(6L);
        assertEquals(4, c6.getStatus());
        assertTrue(c6.getRejectReason().contains("分诊"));

        // 种子表彰 1：陈国强锦旗，院级，已通过
        EthicsCommendation m1 = commendationService.getById(1L);
        assertEquals("陈国强", m1.getPhysicianName());
        assertEquals("BANNER", m1.getCommendType());
        assertEquals("HOSPITAL", m1.getCommendLevel());
        assertEquals("妙手仁心 德艺双馨", m1.getTitle());
        assertEquals(1, m1.getStatus());

        // 种子表彰 5：赵丽华锦旗，待审批
        EthicsCommendation m5 = commendationService.getById(5L);
        assertEquals("赵丽华", m5.getPhysicianName());
        assertEquals(0, m5.getStatus());

        // 种子表彰 6：孙小红通报表扬，已驳回
        EthicsCommendation m6 = commendationService.getById(6L);
        assertEquals("孙小红", m6.getPhysicianName());
        assertEquals(2, m6.getStatus());
        assertTrue(m6.getApprovalHistory().contains("驳回"));
    }

    @Test
    void profilePageQuery() {
        EthicsProfileQuery q = new EthicsProfileQuery();
        q.setKeyword("陈国强");
        assertEquals(1, profileService.pageQuery(q).getTotal());

        EthicsProfileQuery q2 = new EthicsProfileQuery();
        q2.setKeyword("YD-2024-003");
        assertEquals(1, profileService.pageQuery(q2).getTotal()); // 王建军

        EthicsProfileQuery q3 = new EthicsProfileQuery();
        q3.setDeptId(1L);
        assertEquals(1, profileService.pageQuery(q3).getTotal()); // 中医内科：陈国强

        EthicsProfileQuery q4 = new EthicsProfileQuery();
        q4.setStatus(0);
        assertEquals(6, profileService.pageQuery(q4).getTotal()); // 全部在档

        EthicsProfileQuery q5 = new EthicsProfileQuery();
        q5.setLastGrade("EXCELLENT");
        assertEquals(1, profileService.pageQuery(q5).getTotal()); // 只有陈国强（刘志强待审批）
    }

    @Test
    void profileSaveValidationAndDefaults() {
        // 同一医师已有档案，拒绝重复建档
        EthicsProfile dupPhysician = newProfile(1L, "陈国强", "YD-2026-TEST-001");
        assertThrows(IllegalArgumentException.class, () -> profileService.save(dupPhysician));

        // 同一档案编号已有记录，拒绝重复建档
        EthicsProfile dupNo = newProfile(10L, "张伟", "YD-2024-001");
        assertThrows(IllegalArgumentException.class, () -> profileService.save(dupNo));

        // 医师档案中不存在的人，拒绝建档
        EthicsProfile unknown = newProfile(null, "不存在的人", "YD-2026-TEST-002");
        assertThrows(IllegalArgumentException.class, () -> profileService.save(unknown));

        // 合法建档：默认在档、建档日期默认当天，科室/职称按医师档案补全
        EthicsProfile p = newProfile(10L, "张伟", "YD-2026-TEST-001");
        p.setStatus(null);
        p.setEstablishDate(null);
        p.setDeptName(null);
        p.setTitle(null);
        assertTrue(profileService.save(p));
        try {
            assertNotNull(p.getId());
            assertEquals(0, p.getStatus());
            assertEquals(LocalDate.now(), p.getEstablishDate());
            assertEquals("骨伤科", p.getDeptName());
            assertEquals("住院医师", p.getTitle());
        } finally {
            profileService.removeById(p.getId());
            assertNull(profileService.getById(p.getId()));
        }
    }

    @Test
    void templateValidation() {
        // 权重之和不是 100，拒绝保存
        EthicsTemplate bad = new EthicsTemplate();
        bad.setTemplateName("测试模板");
        bad.setScoreItems("[{\"name\":\"医德医风\",\"weight\":50},{\"name\":\"服务态度\",\"weight\":40}]");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(bad));

        // 评分项为空，拒绝保存
        EthicsTemplate empty = new EthicsTemplate();
        empty.setTemplateName("测试模板");
        empty.setScoreItems("");
        assertThrows(IllegalArgumentException.class, () -> templateService.save(empty));

        // 合法模板：默认启用
        EthicsTemplate t = new EthicsTemplate();
        t.setTemplateName("测试模板");
        t.setScoreItems("[{\"name\":\"医德医风\",\"weight\":60},{\"name\":\"廉洁行医\",\"weight\":40}]");
        assertTrue(templateService.save(t));
        try {
            assertNotNull(t.getId());
            assertEquals(1, t.getStatus());
            assertTrue(templateService.listEnabled().stream().anyMatch(x -> x.getId().equals(t.getId())));
        } finally {
            templateService.removeById(t.getId());
            assertNull(templateService.getById(t.getId()));
        }
    }

    @Test
    void evaluationAutoScore() {
        // 无评分项得分，拒绝保存
        EthicsEvaluation noScore = newEvaluation(10L, "张伟", null);
        assertThrows(IllegalArgumentException.class, () -> evaluationService.save(noScore));

        // 单项得分超过模板权重，拒绝保存
        EthicsEvaluation overWeight = newEvaluation(10L, "张伟",
                "{\"医德医风\":51,\"服务态度\":24,\"廉洁行医\":24}");
        assertThrows(IllegalArgumentException.class, () -> evaluationService.save(overWeight));

        // 模板外的评分项，拒绝保存
        EthicsEvaluation unknownItem = newEvaluation(10L, "张伟",
                "{\"医德医风\":40,\"不存在项\":20}");
        assertThrows(IllegalArgumentException.class, () -> evaluationService.save(unknownItem));

        // 合法录入：总分与等级自动算，默认待审批
        EthicsEvaluation e = newEvaluation(10L, "张伟",
                "{\"医德医风\":47,\"服务态度\":24,\"廉洁行医\":24}");
        assertTrue(evaluationService.save(e));
        try {
            assertNotNull(e.getId());
            assertEquals(new BigDecimal("95.00"), e.getTotalScore());
            assertEquals("EXCELLENT", e.getGrade());
            assertEquals("优秀", e.getResult());
            assertEquals(0, e.getStatus());
            assertEquals(2026, e.getAssessYear());
            assertEquals("医德医风年度考评模板", e.getTemplateName());
        } finally {
            evaluationService.removeById(e.getId());
            assertNull(evaluationService.getById(e.getId()));
        }
    }

    @Test
    void evaluationApproveFlow() {
        // 审批通过需要审批人
        ApproveRequest noApprover = new ApproveRequest();
        assertThrows(IllegalArgumentException.class, () -> evaluationService.approve(6L, noApprover));

        // 审批通过种子考评 6（刘志强 90 分优秀）：档案同步最近考评结果
        ApproveRequest req = new ApproveRequest();
        req.setApprover("吴文斌");
        req.setOpinion("同意");
        EthicsEvaluation approved = evaluationService.approve(6L, req);
        try {
            assertEquals(1, approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertTrue(approved.getApprovalHistory().contains("通过"));
            EthicsProfile p6 = profileService.getById(6L);
            assertEquals("EXCELLENT", p6.getLastGrade());
            assertEquals(2026, p6.getLastEvaluateYear());

            // 已通过的考评不能再次审批，也不能修改、不能删除
            assertThrows(IllegalStateException.class, () -> evaluationService.approve(6L, req));
            RejectRequest rej = new RejectRequest();
            rej.setApprover("吴文斌");
            rej.setReason("测试");
            assertThrows(IllegalStateException.class, () -> evaluationService.reject(6L, rej));
            EthicsEvaluation edit = evaluationService.getById(6L);
            edit.setAssessor("改名后的考评人");
            assertThrows(IllegalStateException.class, () -> evaluationService.updateById(edit));
            assertThrows(IllegalStateException.class, () -> evaluationService.removeById(6L));
        } finally {
            // 恢复种子：考评改回待审批，档案最近考评结果清空
            evaluationService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EthicsEvaluation>()
                            .eq(EthicsEvaluation::getId, 6L)
                            .set(EthicsEvaluation::getStatus, 0)
                            .set(EthicsEvaluation::getApprover, null)
                            .set(EthicsEvaluation::getApprovalHistory, null));
            profileService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EthicsProfile>()
                            .eq(EthicsProfile::getId, 6L)
                            .set(EthicsProfile::getLastGrade, null)
                            .set(EthicsProfile::getLastEvaluateYear, null));
            assertEquals(0, evaluationService.getById(6L).getStatus());
            assertNull(profileService.getById(6L).getLastGrade());
        }

        // 驳回：新建一条待审批考评再驳回
        EthicsEvaluation e = newEvaluation(10L, "张伟",
                "{\"医德医风\":40,\"服务态度\":20,\"廉洁行医\":20}");
        assertTrue(evaluationService.save(e));
        try {
            RejectRequest noReason = new RejectRequest();
            noReason.setApprover("吴文斌");
            assertThrows(IllegalArgumentException.class, () -> evaluationService.reject(e.getId(), noReason));

            RejectRequest rej = new RejectRequest();
            rej.setApprover("吴文斌");
            rej.setReason("评分依据不足，请补充佐证材料");
            EthicsEvaluation rejected = evaluationService.reject(e.getId(), rej);
            assertEquals(2, rejected.getStatus());
            assertTrue(rejected.getApprovalHistory().contains("驳回"));
            // 已驳回的考评不能再审批
            assertThrows(IllegalStateException.class, () -> evaluationService.approve(e.getId(), req));
        } finally {
            evaluationService.removeById(e.getId());
            assertNull(evaluationService.getById(e.getId()));
        }
    }

    @Test
    void evaluationStats() {
        Map<String, Object> s = evaluationService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(1L, s.get("pending")); // 刘志强
        assertEquals(5L, s.get("passed"));
        assertEquals(0L, s.get("rejected"));
        assertEquals(new BigDecimal("80.33"), s.get("avgScore")); // (95+88+82+72+55+90)/6
        assertEquals(2L, s.get("excellentCount"));
        assertEquals(new BigDecimal("33.33"), s.get("excellentRate"));
        Map<?, ?> byGrade = (Map<?, ?>) s.get("byGrade");
        assertEquals(2L, byGrade.get("EXCELLENT"));
        assertEquals(2L, byGrade.get("QUALIFIED"));
        assertEquals(1L, byGrade.get("BASIC"));
        assertEquals(1L, byGrade.get("UNQUALIFIED"));

        // 按科室汇总：中医内科 1 人、平均分 95.00
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        Map<String, Object> internal = byDept.stream()
                .filter(d -> "中医内科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(1L, internal.get("count"));
        assertEquals(new BigDecimal("95.00"), internal.get("avgScore"));
        assertEquals(1L, internal.get("excellentCount"));
    }

    @Test
    void complaintFlow() {
        // 已反馈的投诉不能再受理
        assertThrows(IllegalStateException.class, () -> complaintService.accept(1L, "吴文斌"));

        // 完整流转：种子投诉 4（刘女士，待受理）受理 → 处理 → 反馈
        EthicsComplaint accepted = complaintService.accept(4L, "李雅琴");
        try {
            assertEquals(1, accepted.getStatus());
            assertEquals("李雅琴", accepted.getHandler());
            assertTrue(accepted.getApprovalHistory().contains("受理"));

            // 调查中的投诉不能直接反馈（须先处理）
            FeedbackRequest early = new FeedbackRequest();
            early.setFeedbackContent("测试");
            assertThrows(IllegalStateException.class, () -> complaintService.feedback(4L, early));

            // 处理时结果必填
            HandleRequest noResult = new HandleRequest();
            noResult.setHandler("李雅琴");
            assertThrows(IllegalArgumentException.class, () -> complaintService.handle(4L, noResult));

            HandleRequest handle = new HandleRequest();
            handle.setHandler("李雅琴");
            handle.setHandleResult("经核查未发现收受红包行为，已向投诉人说明核查结果");
            EthicsComplaint handled = complaintService.handle(4L, handle);
            assertEquals(2, handled.getStatus());
            assertEquals(LocalDate.now(), handled.getHandleDate());
            assertTrue(handled.getApprovalHistory().contains("已处理"));

            // 反馈时内容必填
            FeedbackRequest noContent = new FeedbackRequest();
            assertThrows(IllegalArgumentException.class, () -> complaintService.feedback(4L, noContent));

            FeedbackRequest feedback = new FeedbackRequest();
            feedback.setHandler("李雅琴");
            feedback.setFeedbackContent("已电话反馈核查结果，投诉人表示理解");
            EthicsComplaint done = complaintService.feedback(4L, feedback);
            assertEquals(3, done.getStatus());
            assertEquals(LocalDate.now(), done.getFeedbackDate());
            assertTrue(done.getApprovalHistory().contains("已向投诉人反馈"));

            // 已反馈的投诉不能再流转、不能修改
            assertThrows(IllegalStateException.class, () -> complaintService.accept(4L, "李雅琴"));
            EthicsComplaint edit = complaintService.getById(4L);
            edit.setContent("改名后的投诉内容");
            assertThrows(IllegalStateException.class, () -> complaintService.updateById(edit));
        } finally {
            // 恢复种子：改回待受理，并把流转信息显式清掉
            complaintService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EthicsComplaint>()
                            .eq(EthicsComplaint::getId, 4L)
                            .set(EthicsComplaint::getStatus, 0)
                            .set(EthicsComplaint::getHandler, null)
                            .set(EthicsComplaint::getHandleResult, null)
                            .set(EthicsComplaint::getHandleDate, null)
                            .set(EthicsComplaint::getFeedbackContent, null)
                            .set(EthicsComplaint::getFeedbackDate, null)
                            .set(EthicsComplaint::getApprovalHistory, null));
            assertEquals(0, complaintService.getById(4L).getStatus());
            assertNull(complaintService.getById(4L).getHandleResult());
        }

        // 不予受理：新建一条待受理投诉再驳回
        EthicsComplaint c = newComplaint("YD-TS-TEST-001", "测试投诉人", "陈国强", "ATTITUDE");
        assertTrue(complaintService.save(c));
        try {
            RejectRequest noReason = new RejectRequest();
            noReason.setApprover("吴文斌");
            assertThrows(IllegalArgumentException.class, () -> complaintService.reject(c.getId(), noReason));

            RejectRequest rej = new RejectRequest();
            rej.setApprover("吴文斌");
            rej.setReason("经核实为挂号流程问题，不属于医德医风投诉范畴");
            EthicsComplaint rejected = complaintService.reject(c.getId(), rej);
            assertEquals(4, rejected.getStatus());
            assertTrue(rejected.getRejectReason().contains("挂号流程"));
            // 不予受理的投诉不能再受理
            assertThrows(IllegalStateException.class, () -> complaintService.accept(c.getId(), "吴文斌"));
        } finally {
            complaintService.removeById(c.getId());
            assertNull(complaintService.getById(c.getId()));
        }
    }

    @Test
    void complaintStats() {
        Map<String, Object> s = complaintService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(1L, s.get("pending")); // 刘女士
        assertEquals(2L, s.get("investigating")); // 王先生、陈先生
        assertEquals(1L, s.get("handled")); // 李女士
        assertEquals(1L, s.get("feedback")); // 张先生
        assertEquals(1L, s.get("rejected")); // 赵女士
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(2L, byType.get("ATTITUDE"));
        assertEquals(1L, byType.get("QUALITY"));
        assertEquals(1L, byType.get("CHARGE"));
        assertEquals(1L, byType.get("INTEGRITY"));
        assertEquals(1L, byType.get("OTHER"));

        // 逾期 1 条（陈先生，处理期限 2026-11-04）；反馈率 16.67%
        assertEquals(1L, s.get("overdue"));
        assertEquals(new BigDecimal("16.67"), s.get("feedbackRate"));

        // 只看逾期：1 条
        EthicsComplaintQuery q = new EthicsComplaintQuery();
        q.setOverdue(true);
        assertEquals(1, complaintService.pageQuery(q).getTotal());

        // 按科室汇总：儿科 1 条、逾期 1 条
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        Map<String, Object> pediatrics = byDept.stream()
                .filter(d -> "儿科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(1L, pediatrics.get("count"));
        assertEquals(1L, pediatrics.get("overdueCount"));
    }

    @Test
    void commendationApproveFlow() {
        // 审批通过需要审批人
        ApproveRequest noApprover = new ApproveRequest();
        assertThrows(IllegalArgumentException.class, () -> commendationService.approve(5L, noApprover));

        // 审批通过种子表彰 5（赵丽华锦旗）
        ApproveRequest req = new ApproveRequest();
        req.setApprover("吴文斌");
        req.setOpinion("同意");
        EthicsCommendation approved = commendationService.approve(5L, req);
        try {
            assertEquals(1, approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertTrue(approved.getApprovalHistory().contains("通过"));

            // 已通过的表彰不能再次审批、不能修改、不能删除
            assertThrows(IllegalStateException.class, () -> commendationService.approve(5L, req));
            EthicsCommendation edit = commendationService.getById(5L);
            edit.setTitle("改名后的表彰");
            assertThrows(IllegalStateException.class, () -> commendationService.updateById(edit));
            assertThrows(IllegalStateException.class, () -> commendationService.removeById(5L));
        } finally {
            // 恢复种子：改回待审批，并把审批信息显式清掉
            commendationService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EthicsCommendation>()
                            .eq(EthicsCommendation::getId, 5L)
                            .set(EthicsCommendation::getStatus, 0)
                            .set(EthicsCommendation::getApprover, null)
                            .set(EthicsCommendation::getApprovalHistory, null));
            assertEquals(0, commendationService.getById(5L).getStatus());
            assertNull(commendationService.getById(5L).getApprover());
        }

        // 驳回：新建一条待审批表彰再驳回
        EthicsCommendation c = newCommendation(10L, "张伟", "NOTICE", "HOSPITAL", "测试表彰");
        assertTrue(commendationService.save(c));
        try {
            RejectRequest rej = new RejectRequest();
            rej.setApprover("吴文斌");
            rej.setReason("事迹材料不完整，请补充后重新申报");
            EthicsCommendation rejected = commendationService.reject(c.getId(), rej);
            assertEquals(2, rejected.getStatus());
            assertTrue(rejected.getApprovalHistory().contains("驳回"));
            // 已驳回的表彰不能再审批
            assertThrows(IllegalStateException.class, () -> commendationService.approve(c.getId(), req));
        } finally {
            commendationService.removeById(c.getId());
            assertNull(commendationService.getById(c.getId()));
        }
    }

    @Test
    void commendationStats() {
        Map<String, Object> s = commendationService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(1L, s.get("pending")); // 赵丽华
        assertEquals(4L, s.get("passed"));
        assertEquals(1L, s.get("rejected")); // 孙小红
        Map<?, ?> byType = (Map<?, ?>) s.get("byType");
        assertEquals(2L, byType.get("BANNER"));
        assertEquals(1L, byType.get("LETTER"));
        assertEquals(2L, byType.get("NOTICE"));
        assertEquals(1L, byType.get("HONOR"));
        assertEquals(0L, byType.get("OTHER"));
        Map<?, ?> byLevel = (Map<?, ?>) s.get("byLevel");
        assertEquals(4L, byLevel.get("HOSPITAL"));
        assertEquals(1L, byLevel.get("CITY"));
        assertEquals(1L, byLevel.get("PROVINCE"));
        assertEquals(0L, byLevel.get("NATIONAL"));

        // 按科室汇总：中医内科 2 条、已通过 2 条
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        Map<String, Object> internal = byDept.stream()
                .filter(d -> "中医内科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(2L, internal.get("count"));
        assertEquals(2L, internal.get("passedCount"));
    }

    @Test
    void profileStats() {
        Map<String, Object> s = profileService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(6L, s.get("active"));
        assertEquals(0L, s.get("archived"));
        assertEquals(4L, s.get("commendationTotal")); // 已通过表彰 4 条
        assertEquals(6L, s.get("complaintTotal")); // 投诉总数 6 条
        // 考评平均分只统计已通过的考评：(95+88+82+72+55)/5 = 78.40，优秀 1 人、优秀率 20.00%
        assertEquals(new BigDecimal("78.40"), s.get("evaluationAvg"));
        assertEquals(1L, s.get("excellentCount"));
        assertEquals(new BigDecimal("20.00"), s.get("excellentRate"));
        assertEquals(1L, s.get("overdueComplaints")); // 陈先生投诉已逾期

        // 按科室汇总：6 个科室各 1 份档案；中医内科最近考评优秀 1 人
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDept = (List<Map<String, Object>>) s.get("byDept");
        assertEquals(6, byDept.size());
        Map<String, Object> internal = byDept.stream()
                .filter(d -> "中医内科".equals(d.get("deptName"))).findFirst().orElseThrow();
        assertEquals(1L, internal.get("count"));
        assertEquals(1L, internal.get("excellentCount"));
    }

    @Test
    void evaluationExcelRoundTrip() {
        EthicsEvaluationQuery q = new EthicsEvaluationQuery();
        q.setPage(1);
        q.setSize(100);
        List<EthicsEvaluation> all = evaluationService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EthicsEvaluationExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<EthicsEvaluation> back = EthicsEvaluationExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        EthicsEvaluation first = all.stream()
                .filter(e -> "陈国强".equals(e.getPhysicianName()))
                .findFirst().orElseThrow();
        EthicsEvaluation firstBack = back.stream()
                .filter(e -> "陈国强".equals(e.getPhysicianName()))
                .findFirst().orElseThrow();
        assertEquals(first.getTemplateName(), firstBack.getTemplateName());
        assertEquals(first.getAssessYear(), firstBack.getAssessYear());
        // 各项得分导出再读回：每项得分一致
        assertEquals(EthicsTypeUtil.parseItemScores(first.getItemScores()),
                EthicsTypeUtil.parseItemScores(firstBack.getItemScores()));
        assertEquals(0, first.getTotalScore().compareTo(firstBack.getTotalScore()));
        assertEquals(first.getGrade(), firstBack.getGrade());
        assertEquals(first.getResult(), firstBack.getResult());
        assertEquals(first.getAssessor(), firstBack.getAssessor());
        assertEquals(first.getAssessDate(), firstBack.getAssessDate());
        assertEquals(first.getStatus(), firstBack.getStatus());
    }

    @Test
    void complaintExcelRoundTrip() {
        EthicsComplaintQuery q = new EthicsComplaintQuery();
        q.setPage(1);
        q.setSize(100);
        List<EthicsComplaint> all = complaintService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EthicsComplaintExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<EthicsComplaint> back = EthicsComplaintExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        EthicsComplaint first = all.stream()
                .filter(c -> "YD-TS-2026-001".equals(c.getComplaintNo()))
                .findFirst().orElseThrow();
        EthicsComplaint firstBack = back.stream()
                .filter(c -> "YD-TS-2026-001".equals(c.getComplaintNo()))
                .findFirst().orElseThrow();
        assertEquals(first.getComplainantName(), firstBack.getComplainantName());
        assertEquals(first.getPhysicianName(), firstBack.getPhysicianName());
        assertEquals(first.getComplaintType(), firstBack.getComplaintType());
        assertEquals(first.getComplaintDate(), firstBack.getComplaintDate());
        assertEquals(first.getDeadlineDate(), firstBack.getDeadlineDate());
        assertEquals(first.getContent(), firstBack.getContent());
        assertEquals(first.getStatus(), firstBack.getStatus());
        assertEquals(first.getHandleResult(), firstBack.getHandleResult());
        assertEquals(first.getFeedbackContent(), firstBack.getFeedbackContent());
    }

    @Test
    void profileExcelRoundTrip() {
        EthicsProfileQuery q = new EthicsProfileQuery();
        q.setPage(1);
        q.setSize(100);
        List<EthicsProfile> all = profileService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EthicsProfileExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<EthicsProfile> back = EthicsProfileExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        EthicsProfile first = all.stream()
                .filter(p -> "YD-2024-001".equals(p.getArchiveNo()))
                .findFirst().orElseThrow();
        EthicsProfile firstBack = back.stream()
                .filter(p -> "YD-2024-001".equals(p.getArchiveNo()))
                .findFirst().orElseThrow();
        assertEquals(first.getPhysicianName(), firstBack.getPhysicianName());
        assertEquals(first.getDeptName(), firstBack.getDeptName());
        assertEquals(first.getTitle(), firstBack.getTitle());
        assertEquals(first.getEstablishDate(), firstBack.getEstablishDate());
        assertEquals(first.getStatus(), firstBack.getStatus());
        assertEquals(first.getLastGrade(), firstBack.getLastGrade());
        assertEquals(first.getLastEvaluateYear(), firstBack.getLastEvaluateYear());
    }

    @Test
    void commendationExcelRoundTrip() {
        EthicsCommendationQuery q = new EthicsCommendationQuery();
        q.setPage(1);
        q.setSize(100);
        List<EthicsCommendation> all = commendationService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EthicsCommendationExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<EthicsCommendation> back = EthicsCommendationExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        EthicsCommendation first = all.stream()
                .filter(c -> "妙手仁心 德艺双馨".equals(c.getTitle()))
                .findFirst().orElseThrow();
        EthicsCommendation firstBack = back.stream()
                .filter(c -> "妙手仁心 德艺双馨".equals(c.getTitle()))
                .findFirst().orElseThrow();
        assertEquals(first.getPhysicianName(), firstBack.getPhysicianName());
        assertEquals(first.getCommendType(), firstBack.getCommendType());
        assertEquals(first.getCommendLevel(), firstBack.getCommendLevel());
        assertEquals(first.getGrantedBy(), firstBack.getGrantedBy());
        assertEquals(first.getCommendDate(), firstBack.getCommendDate());
        assertEquals(first.getStatus(), firstBack.getStatus());
    }

    @Test
    void importViaServiceRoundTrip() throws Exception {
        // 新建一条待受理投诉 → 导出全部 → 删掉这条 → 再导回同一投诉，应只补回这一条
        // （导入的均为待受理，种子里的已反馈/已处理/调查中/不予受理行按“同一投诉编号重复”规则跳过）
        EthicsComplaint fresh = newComplaint("YD-TS-2026-100", "导入测试投诉人", "陈国强", "ATTITUDE");
        assertTrue(complaintService.save(fresh));

        EthicsComplaintQuery q = new EthicsComplaintQuery();
        q.setPage(1);
        q.setSize(100);
        List<EthicsComplaint> all = complaintService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EthicsComplaintExcelUtil.write(all, out);

        assertTrue(complaintService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, complaintService.listByQuery(q).size());

        int imported = complaintService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因重复而跳过，只补回导入测试投诉这一条
        assertEquals(all.size(), complaintService.listByQuery(q).size());
        EthicsComplaint restored = complaintService.listByQuery(q).stream()
                .filter(c -> "YD-TS-2026-100".equals(c.getComplaintNo()))
                .findFirst().orElseThrow();
        assertEquals("ATTITUDE", restored.getComplaintType());
        assertEquals(0, restored.getStatus()); // 导入的投诉均为待受理
        assertTrue(complaintService.removeById(restored.getId()));
        assertEquals(all.size() - 1, complaintService.listByQuery(q).size());
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> profileService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> evaluationService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> complaintService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> commendationService.detail(999999L));
    }

    private EthicsProfile newProfile(Long physicianId, String physicianName, String archiveNo) {
        EthicsProfile p = new EthicsProfile();
        p.setArchiveNo(archiveNo);
        p.setPhysicianId(physicianId);
        p.setPhysicianName(physicianName);
        p.setDeptName("骨伤科");
        p.setTitle("住院医师");
        return p;
    }

    private EthicsEvaluation newEvaluation(Long physicianId, String physicianName, String itemScores) {
        EthicsEvaluation e = new EthicsEvaluation();
        e.setPhysicianId(physicianId);
        e.setPhysicianName(physicianName);
        e.setDeptId(3L);
        e.setDeptName("骨伤科");
        e.setTemplateId(1L);
        e.setTemplateName("医德医风年度考评模板");
        e.setAssessYear(2026);
        e.setCycle("年度");
        e.setItemScores(itemScores);
        e.setAssessor("吴文斌");
        e.setAssessDate(LocalDate.of(2026, 11, 22));
        return e;
    }

    private EthicsComplaint newComplaint(String complaintNo, String complainantName,
                                         String physicianName, String complaintType) {
        EthicsComplaint c = new EthicsComplaint();
        c.setComplaintNo(complaintNo);
        c.setComplainantName(complainantName);
        c.setComplainantPhone("13800000100");
        c.setPhysicianName(physicianName);
        c.setComplaintType(complaintType);
        c.setComplaintDate(LocalDate.of(2026, 11, 22));
        c.setContent("测试投诉内容");
        return c;
    }

    private EthicsCommendation newCommendation(Long physicianId, String physicianName,
                                               String commendType, String commendLevel, String title) {
        EthicsCommendation c = new EthicsCommendation();
        c.setPhysicianId(physicianId);
        c.setPhysicianName(physicianName);
        c.setDeptId(3L);
        c.setDeptName("骨伤科");
        c.setCommendType(commendType);
        c.setCommendLevel(commendLevel);
        c.setTitle(title);
        c.setContent("测试表彰事迹");
        c.setGrantedBy("测试授予方");
        c.setCommendDate(LocalDate.of(2026, 11, 22));
        return c;
    }
}
