package com.yunzhongyi.medical;

import com.yunzhongyi.medical.techgrading.dto.ApproveRequest;
import com.yunzhongyi.medical.techgrading.dto.MedicalTechQuery;
import com.yunzhongyi.medical.techgrading.dto.RejectRequest;
import com.yunzhongyi.medical.techgrading.dto.TechAdmissionQuery;
import com.yunzhongyi.medical.techgrading.dto.TechAuthorizationQuery;
import com.yunzhongyi.medical.techgrading.entity.MedicalTech;
import com.yunzhongyi.medical.techgrading.entity.TechAdmission;
import com.yunzhongyi.medical.techgrading.entity.TechAuthorization;
import com.yunzhongyi.medical.techgrading.excel.MedicalTechExcelUtil;
import com.yunzhongyi.medical.techgrading.excel.TechAdmissionExcelUtil;
import com.yunzhongyi.medical.techgrading.excel.TechAuthorizationExcelUtil;
import com.yunzhongyi.medical.techgrading.service.MedicalTechService;
import com.yunzhongyi.medical.techgrading.service.TechAdmissionService;
import com.yunzhongyi.medical.techgrading.service.TechAuthorizationService;
import com.yunzhongyi.medical.techgrading.service.TechGradingTypeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 医疗技术分级管理模块验证（H2 内存库；生产默认 profile 是 MySQL）
 * 种子数据：6 项技术档案（一级 3 / 二级 1 / 三级 2，常规技术 4 / 限制类 1 / 禁止类 1，
 * 已准入 4 / 待准入 1 / 已停用 1，90 天内到期 1 项、已过期 1 项）、
 * 6 条准入申请（新技术准入 4 / 新增开展科室 1 / 延续准入 1，
 * 待审批 2 / 已通过 3 / 已驳回 1）、
 * 6 条医师授权（待审批 1 / 已通过 4 / 已驳回 1，90 天内到期 1 条、已过期 1 条）。
 */
@SpringBootTest
@ActiveProfiles("h2")
class TechGradingTests {

    @Autowired
    private MedicalTechService medicalTechService;

    @Autowired
    private TechAdmissionService techAdmissionService;

    @Autowired
    private TechAuthorizationService techAuthorizationService;

    @Test
    void contextLoads() {
        assertNotNull(medicalTechService);
        assertNotNull(techAdmissionService);
        assertNotNull(techAuthorizationService);
    }

    @Test
    void seedDataLoaded() {
        assertTrue(medicalTechService.count() >= 6);

        // 种子技术 1：中药穴位贴敷技术，一级，常规技术，中医内科，已准入
        MedicalTech t1 = medicalTechService.getById(1L);
        assertEquals("中药穴位贴敷技术", t1.getTechName());
        assertEquals("JS-2024-001", t1.getTechNo());
        assertEquals("TCM", t1.getCategory());
        assertEquals(1, t1.getTechLevel());
        assertEquals("NORMAL", t1.getAccessType());
        assertEquals("中医内科", t1.getDeptName());
        assertEquals("LOW", t1.getRiskLevel());
        assertEquals(0, t1.getEthicsRequired());
        assertEquals(1, t1.getStatus());
        assertEquals(LocalDate.of(2024, 1, 1), t1.getAdmissionDate());
        assertEquals(LocalDate.of(2027, 12, 31), t1.getExpiryDate());
        assertTrue(t1.getApprovalHistory().contains("准入通过"));

        // 种子技术 2：针刺治疗技术，一级，常规技术，针灸推拿科，已准入
        MedicalTech t2 = medicalTechService.getById(2L);
        assertEquals("针刺治疗技术", t2.getTechName());
        assertEquals(1, t2.getTechLevel());
        assertEquals("针灸推拿科", t2.getDeptName());
        assertEquals(1, t2.getStatus());

        // 种子技术 3：小针刀松解术，二级，骨伤科，2026-12-20 到期（90 天内到期）
        MedicalTech t3 = medicalTechService.getById(3L);
        assertEquals("小针刀松解术", t3.getTechName());
        assertEquals("SURGICAL", t3.getCategory());
        assertEquals(2, t3.getTechLevel());
        assertEquals(LocalDate.of(2026, 12, 20), t3.getExpiryDate());
        assertTrue(TechGradingTypeUtil.isExpiringSoon(t3.getStatus(), t3.getExpiryDate()));
        assertFalse(TechGradingTypeUtil.isExpired(t3.getStatus(), t3.getExpiryDate()));

        // 种子技术 4：介入栓塞治疗技术，三级，限制类技术，放射科，需伦理审查，有效期 2026-11-15 已过期
        MedicalTech t4 = medicalTechService.getById(4L);
        assertEquals("介入栓塞治疗技术", t4.getTechName());
        assertEquals(3, t4.getTechLevel());
        assertEquals("RESTRICTED", t4.getAccessType());
        assertEquals("HIGH", t4.getRiskLevel());
        assertEquals(1, t4.getEthicsRequired());
        assertTrue(TechGradingTypeUtil.isExpired(t4.getStatus(), t4.getExpiryDate()));

        // 种子技术 5：基因编辑治疗技术，三级，禁止类技术，待准入
        MedicalTech t5 = medicalTechService.getById(5L);
        assertEquals("基因编辑治疗技术", t5.getTechName());
        assertEquals("PROHIBITED", t5.getAccessType());
        assertEquals(0, t5.getStatus());
        assertNull(t5.getAdmissionDate());

        // 种子技术 6：中药熏蒸治疗技术，一级，已停用
        MedicalTech t6 = medicalTechService.getById(6L);
        assertEquals("中药熏蒸治疗技术", t6.getTechName());
        assertEquals(2, t6.getStatus());
        assertTrue(t6.getSuspendReason().contains("疗效证据不足"));

        // 准入申请种子：6 条，待审批 2 / 已通过 3 / 已驳回 1
        assertTrue(techAdmissionService.count() >= 6);
        TechAdmission a1 = techAdmissionService.getById(1L);
        assertEquals("中药穴位贴敷技术", a1.getTechName());
        assertEquals("NEW", a1.getApplyType());
        assertEquals("陈国强", a1.getApplicant());
        assertEquals(1, a1.getStatus());
        TechAdmission a4 = techAdmissionService.getById(4L);
        assertEquals("基因编辑治疗技术", a4.getTechName());
        assertEquals(0, a4.getStatus());
        TechAdmission a5 = techAdmissionService.getById(5L);
        assertEquals("ADD_DEPT", a5.getApplyType());
        assertEquals("妇科", a5.getNewDept());
        assertEquals(0, a5.getStatus());
        TechAdmission a6 = techAdmissionService.getById(6L);
        assertEquals("RENEW", a6.getApplyType());
        assertEquals(LocalDate.of(2026, 12, 31), a6.getNewExpiryDate());
        assertEquals(2, a6.getStatus());
        assertEquals(1, techAdmissionService.listByTech(1L).size());

        // 医师授权种子：6 条，待审批 1 / 已通过 4 / 已驳回 1
        assertTrue(techAuthorizationService.count() >= 6);
        TechAuthorization au1 = techAuthorizationService.getById(1L);
        assertEquals("陈国强", au1.getPhysicianName());
        assertEquals("中药穴位贴敷技术", au1.getTechName());
        assertEquals(1, au1.getTechLevel());
        assertEquals(1, au1.getAuthLevel());
        assertEquals(1, au1.getStatus());
        TechAuthorization au3 = techAuthorizationService.getById(3L);
        assertEquals("王建军", au3.getPhysicianName());
        assertEquals(2, au3.getAuthLevel());
        assertEquals(LocalDate.of(2026, 12, 20), au3.getEndDate());
        assertTrue(TechGradingTypeUtil.isExpiringSoon(au3.getStatus(), au3.getEndDate()));
        TechAuthorization au4 = techAuthorizationService.getById(4L);
        assertEquals("周建国", au4.getPhysicianName());
        assertEquals(3, au4.getAuthLevel());
        assertTrue(TechGradingTypeUtil.isExpired(au4.getStatus(), au4.getEndDate()));
        TechAuthorization au5 = techAuthorizationService.getById(5L);
        assertEquals("赵丽华", au5.getPhysicianName());
        assertEquals(0, au5.getStatus());
        assertEquals(2, techAuthorizationService.listByTech(1L).size());
        assertEquals(1, techAuthorizationService.listByPhysician(1L).size());
    }

    @Test
    void pageQueryWithFilters() {
        // 技术档案查询
        MedicalTechQuery q = new MedicalTechQuery();
        q.setCategory("TCM");
        assertEquals(3, medicalTechService.pageQuery(q).getTotal()); // 穴位贴敷、针刺、熏蒸

        MedicalTechQuery q2 = new MedicalTechQuery();
        q2.setTechLevel(1);
        assertEquals(3, medicalTechService.pageQuery(q2).getTotal());

        MedicalTechQuery q3 = new MedicalTechQuery();
        q3.setAccessType("RESTRICTED");
        assertEquals(1, medicalTechService.pageQuery(q3).getTotal()); // 介入栓塞

        MedicalTechQuery q4 = new MedicalTechQuery();
        q4.setStatus(1);
        assertEquals(4, medicalTechService.pageQuery(q4).getTotal()); // 已准入 4 项

        MedicalTechQuery q5 = new MedicalTechQuery();
        q5.setKeyword("JS-2024");
        assertEquals(4, medicalTechService.pageQuery(q5).getTotal());

        MedicalTechQuery q6 = new MedicalTechQuery();
        q6.setDeptName("骨伤科");
        assertEquals(1, medicalTechService.pageQuery(q6).getTotal()); // 小针刀

        MedicalTechQuery q7 = new MedicalTechQuery();
        q7.setExpiringSoon(true);
        assertEquals(1, medicalTechService.pageQuery(q7).getTotal()); // 小针刀

        MedicalTechQuery q8 = new MedicalTechQuery();
        q8.setExpired(true);
        assertEquals(1, medicalTechService.pageQuery(q8).getTotal()); // 介入栓塞

        // 准入申请查询
        TechAdmissionQuery aq = new TechAdmissionQuery();
        aq.setApplyType("NEW");
        assertEquals(4, techAdmissionService.pageQuery(aq).getTotal());

        TechAdmissionQuery aq2 = new TechAdmissionQuery();
        aq2.setStatus(0);
        assertEquals(2, techAdmissionService.pageQuery(aq2).getTotal()); // 基因编辑、新增开展科室

        TechAdmissionQuery aq3 = new TechAdmissionQuery();
        aq3.setKeyword("吴文斌");
        assertEquals(1, techAdmissionService.pageQuery(aq3).getTotal());

        // 医师授权查询
        TechAuthorizationQuery uq = new TechAuthorizationQuery();
        uq.setAuthLevel(3);
        assertEquals(1, techAuthorizationService.pageQuery(uq).getTotal()); // 周建国

        TechAuthorizationQuery uq2 = new TechAuthorizationQuery();
        uq2.setStatus(1);
        assertEquals(4, techAuthorizationService.pageQuery(uq2).getTotal()); // 已通过 4 条

        TechAuthorizationQuery uq3 = new TechAuthorizationQuery();
        uq3.setKeyword("陈国强");
        assertEquals(1, techAuthorizationService.pageQuery(uq3).getTotal());

        TechAuthorizationQuery uq4 = new TechAuthorizationQuery();
        uq4.setExpiringSoon(true);
        assertEquals(1, techAuthorizationService.pageQuery(uq4).getTotal()); // 王建军

        TechAuthorizationQuery uq5 = new TechAuthorizationQuery();
        uq5.setExpired(true);
        assertEquals(1, techAuthorizationService.pageQuery(uq5).getTotal()); // 周建国
    }

    @Test
    void saveValidationAndDefaults() {
        // 技术分类非法，拒绝保存
        MedicalTech badCategory = newTech("测试技术", "JS-TEST-001");
        badCategory.setCategory("XXX");
        assertThrows(IllegalArgumentException.class, () -> medicalTechService.save(badCategory));

        // 技术分级非法，拒绝保存
        MedicalTech badLevel = newTech("测试技术", "JS-TEST-001");
        badLevel.setTechLevel(4);
        assertThrows(IllegalArgumentException.class, () -> medicalTechService.save(badLevel));

        // 准入类别非法，拒绝保存
        MedicalTech badAccess = newTech("测试技术", "JS-TEST-001");
        badAccess.setAccessType("XXX");
        assertThrows(IllegalArgumentException.class, () -> medicalTechService.save(badAccess));

        // 有效期早于准入日期，拒绝保存
        MedicalTech badDate = newTech("测试技术", "JS-TEST-001");
        badDate.setAdmissionDate(LocalDate.of(2026, 1, 1));
        badDate.setExpiryDate(LocalDate.of(2025, 12, 31));
        assertThrows(IllegalArgumentException.class, () -> medicalTechService.save(badDate));

        // 合法建档：默认待准入、风险低、不需伦理审查
        MedicalTech p = newTech("测试技术", "JS-TEST-001");
        p.setRiskLevel(null);
        p.setEthicsRequired(null);
        assertTrue(medicalTechService.save(p));
        try {
            assertNotNull(p.getId());
            assertEquals(TechGradingTypeUtil.TECH_STATUS_PENDING, p.getStatus());
            assertEquals("LOW", p.getRiskLevel());
            assertEquals(0, p.getEthicsRequired());

            // 同一技术编号已有记录，拒绝重复建档
            MedicalTech dup = newTech("测试技术", "JS-TEST-001");
            assertThrows(IllegalArgumentException.class, () -> medicalTechService.save(dup));
        } finally {
            medicalTechService.removeById(p.getId());
        }
    }

    @Test
    void updateGuard() {
        // 已准入的技术不能直接修改（分级/科室/有效期变更走准入申请）
        MedicalTech admitted = medicalTechService.getById(1L);
        admitted.setTechName("改名后的技术");
        assertThrows(IllegalStateException.class, () -> medicalTechService.updateById(admitted));

        // 待准入的技术可以修改
        MedicalTech pending = medicalTechService.getById(5L);
        String oldName = pending.getTechName();
        pending.setTechName("基因编辑治疗技术（修订）");
        assertTrue(medicalTechService.updateById(pending));
        try {
            assertEquals("基因编辑治疗技术（修订）", medicalTechService.getById(5L).getTechName());
        } finally {
            MedicalTech restore = new MedicalTech();
            restore.setId(5L);
            restore.setTechName(oldName);
            medicalTechService.getBaseMapper().updateById(restore);
        }
    }

    @Test
    void suspendResumeFlow() {
        // 停用（种子 id=1 中药穴位贴敷技术）需要停用原因
        RejectRequest noReason = new RejectRequest();
        assertThrows(IllegalArgumentException.class, () -> medicalTechService.suspend(1L, noReason));

        RejectRequest req = new RejectRequest();
        req.setApprover("吴文斌");
        req.setReason("暂停开展，待评估");
        MedicalTech suspended = medicalTechService.suspend(1L, req);
        try {
            assertEquals(2, suspended.getStatus());
            assertEquals("暂停开展，待评估", suspended.getSuspendReason());
            assertTrue(suspended.getApprovalHistory().contains("停用"));

            // 已停用的技术不能再次停用
            assertThrows(IllegalStateException.class, () -> medicalTechService.suspend(1L, req));

            // 重新启用
            MedicalTech resumed = medicalTechService.resume(1L);
            assertEquals(1, resumed.getStatus());
            assertNull(resumed.getSuspendReason());
            assertTrue(resumed.getApprovalHistory().contains("重新启用"));

            // 已准入的技术不能重新启用
            assertThrows(IllegalStateException.class, () -> medicalTechService.resume(1L));
        } finally {
            // 恢复种子：状态改回已准入，并把停用信息与审批记录显式恢复
            medicalTechService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MedicalTech>()
                            .eq(MedicalTech::getId, 1L)
                            .set(MedicalTech::getStatus, 1)
                            .set(MedicalTech::getSuspendReason, null)
                            .set(MedicalTech::getApprovalHistory, "医务科 吴文斌：准入通过（同意）"));
            assertEquals(1, medicalTechService.getById(1L).getStatus());
            assertNull(medicalTechService.getById(1L).getSuspendReason());
        }

        // 待准入的技术不能停用
        RejectRequest req2 = new RejectRequest();
        req2.setReason("测试");
        assertThrows(IllegalStateException.class, () -> medicalTechService.suspend(5L, req2));
    }

    @Test
    void deleteGuardAndCascade() {
        // 已准入的技术不能直接删除
        assertThrows(IllegalStateException.class, () -> medicalTechService.removeById(1L));

        // 待准入的技术可以删除，且同时删除其准入申请
        MedicalTech p = newTech("待删技术", "JS-TEST-002");
        assertTrue(medicalTechService.save(p));
        TechAdmission a = newAdmission(p.getId(), "NEW");
        assertTrue(techAdmissionService.save(a));
        assertTrue(medicalTechService.removeById(p.getId()));
        assertNull(medicalTechService.getById(p.getId()));
        assertNull(techAdmissionService.getById(a.getId()));
    }

    @Test
    void admissionApproveNewFlow() {
        // 新建一条待准入技术 + 新技术准入申请 → 审批通过 → 档案变为已准入、准入日期为当天、有效期默认 + 3 年
        MedicalTech tech = newTech("测试新技术", "JS-TEST-003");
        tech.setExpiryDate(null); // 有效期为空时，准入通过默认 + 3 年
        assertTrue(medicalTechService.save(tech));
        TechAdmission a = newAdmission(tech.getId(), "NEW");
        assertTrue(techAdmissionService.save(a));
        try {
            ApproveRequest noApprover = new ApproveRequest();
            assertThrows(IllegalArgumentException.class, () -> techAdmissionService.approve(a.getId(), noApprover));

            ApproveRequest req = new ApproveRequest();
            req.setApprover("吴文斌");
            req.setOpinion("同意准入");
            TechAdmission approved = techAdmissionService.approve(a.getId(), req);
            assertEquals(1, approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertTrue(approved.getApprovalHistory().contains("通过"));

            MedicalTech admitted = medicalTechService.getById(tech.getId());
            assertEquals(1, admitted.getStatus());
            assertEquals(LocalDate.now(), admitted.getAdmissionDate());
            assertEquals(LocalDate.now().plusYears(3), admitted.getExpiryDate());
            assertTrue(admitted.getApprovalHistory().contains("准入通过"));

            // 已通过的申请不能再次审批，也不能删除
            assertThrows(IllegalStateException.class, () -> techAdmissionService.approve(a.getId(), req));
            assertThrows(IllegalStateException.class, () -> techAdmissionService.removeById(a.getId()));
        } finally {
            // 恢复：删掉申请、把技术改回待准入并清空准入信息、再删技术
            techAdmissionService.getBaseMapper().deleteById(a.getId());
            medicalTechService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MedicalTech>()
                            .eq(MedicalTech::getId, tech.getId())
                            .set(MedicalTech::getStatus, 0)
                            .set(MedicalTech::getAdmissionDate, null)
                            .set(MedicalTech::getExpiryDate, null)
                            .set(MedicalTech::getApprover, null)
                            .set(MedicalTech::getApprovalHistory, null));
            assertTrue(medicalTechService.removeById(tech.getId()));
        }
    }

    @Test
    void admissionApproveAddDeptFlow() {
        // 种子技术 1（中药穴位贴敷技术，已准入）新增开展科室“骨伤科” → 审批通过 → 开展科室追加
        TechAdmission a = newAdmission(1L, "ADD_DEPT");
        a.setNewDept("骨伤科");
        assertTrue(techAdmissionService.save(a));
        try {
            ApproveRequest req = new ApproveRequest();
            req.setApprover("吴文斌");
            techAdmissionService.approve(a.getId(), req);
            assertEquals("中医内科、骨伤科", medicalTechService.getById(1L).getDeptName());
        } finally {
            techAdmissionService.getBaseMapper().deleteById(a.getId());
            medicalTechService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MedicalTech>()
                            .eq(MedicalTech::getId, 1L)
                            .set(MedicalTech::getDeptName, "中医内科"));
            assertEquals("中医内科", medicalTechService.getById(1L).getDeptName());
        }
    }

    @Test
    void admissionApproveChangeLevelFlow() {
        // 种子技术 1（一级）变更分级为二级 → 审批通过 → 档案分级改为二级
        TechAdmission a = newAdmission(1L, "CHANGE_LEVEL");
        a.setNewLevel(2);
        assertTrue(techAdmissionService.save(a));
        try {
            ApproveRequest req = new ApproveRequest();
            req.setApprover("吴文斌");
            techAdmissionService.approve(a.getId(), req);
            assertEquals(2, medicalTechService.getById(1L).getTechLevel());
        } finally {
            techAdmissionService.getBaseMapper().deleteById(a.getId());
            medicalTechService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MedicalTech>()
                            .eq(MedicalTech::getId, 1L)
                            .set(MedicalTech::getTechLevel, 1));
            assertEquals(1, medicalTechService.getById(1L).getTechLevel());
        }
    }

    @Test
    void admissionApproveRenewFlow() {
        // 种子技术 3（小针刀，有效期 2026-12-20）延续准入至 2029-12-31 → 审批通过 → 档案有效期更新
        TechAdmission a = newAdmission(3L, "RENEW");
        a.setNewExpiryDate(LocalDate.of(2029, 12, 31));
        assertTrue(techAdmissionService.save(a));
        try {
            ApproveRequest req = new ApproveRequest();
            req.setApprover("吴文斌");
            techAdmissionService.approve(a.getId(), req);
            assertEquals(LocalDate.of(2029, 12, 31), medicalTechService.getById(3L).getExpiryDate());
        } finally {
            techAdmissionService.getBaseMapper().deleteById(a.getId());
            medicalTechService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MedicalTech>()
                            .eq(MedicalTech::getId, 3L)
                            .set(MedicalTech::getExpiryDate, LocalDate.of(2026, 12, 20)));
            assertEquals(LocalDate.of(2026, 12, 20), medicalTechService.getById(3L).getExpiryDate());
        }
    }

    @Test
    void admissionValidation() {
        // 变更分级不填变更后分级，拒绝
        TechAdmission noLevel = newAdmission(1L, "CHANGE_LEVEL");
        assertThrows(IllegalArgumentException.class, () -> techAdmissionService.save(noLevel));

        // 新增开展科室不填科室，拒绝
        TechAdmission noDept = newAdmission(1L, "ADD_DEPT");
        assertThrows(IllegalArgumentException.class, () -> techAdmissionService.save(noDept));

        // 延续准入不填新有效期，拒绝
        TechAdmission noExpiry = newAdmission(1L, "RENEW");
        assertThrows(IllegalArgumentException.class, () -> techAdmissionService.save(noExpiry));

        // 延续后有效期早于当天，拒绝
        TechAdmission pastExpiry = newAdmission(1L, "RENEW");
        pastExpiry.setNewExpiryDate(LocalDate.of(2020, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> techAdmissionService.save(pastExpiry));

        // 关联的技术不存在，拒绝
        TechAdmission missingTech = newAdmission(999999L, "NEW");
        assertThrows(IllegalArgumentException.class, () -> techAdmissionService.save(missingTech));

        // 同一技术同一申请类型已有待审批记录，拒绝重复申请（种子 id=5 是技术 2 的待审批 ADD_DEPT）
        TechAdmission dup = newAdmission(2L, "ADD_DEPT");
        dup.setNewDept("妇科");
        assertThrows(IllegalArgumentException.class, () -> techAdmissionService.save(dup));

        // 禁止类技术的新技术准入审批通过时拒绝（种子 id=4 是技术 5 的待审批 NEW 申请）
        ApproveRequest req = new ApproveRequest();
        req.setApprover("吴文斌");
        assertThrows(IllegalStateException.class, () -> techAdmissionService.approve(4L, req));

        // 已驳回的申请不能再审批
        RejectRequest rejectReq = new RejectRequest();
        rejectReq.setApprover("吴文斌");
        rejectReq.setReason("测试驳回");
        TechAdmission rejected = techAdmissionService.reject(5L, rejectReq);
        try {
            assertEquals(2, rejected.getStatus());
            assertThrows(IllegalStateException.class, () -> techAdmissionService.approve(5L, req));
            assertThrows(IllegalStateException.class, () -> techAdmissionService.reject(5L, rejectReq));
        } finally {
            // 恢复种子 id=5：改回待审批，清空审批信息
            techAdmissionService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TechAdmission>()
                            .eq(TechAdmission::getId, 5L)
                            .set(TechAdmission::getStatus, 0)
                            .set(TechAdmission::getApprover, null)
                            .set(TechAdmission::getApprovalHistory, null));
            assertEquals(0, techAdmissionService.getById(5L).getStatus());
        }
    }

    @Test
    void authValidationAndDefaults() {
        // 授权级别高于技术分级，拒绝（技术 1 为一级，申请二级授权）
        TechAuthorization overLevel = newAuth("孙小红", 5L, 1L, 2);
        assertThrows(IllegalArgumentException.class, () -> techAuthorizationService.save(overLevel));

        // 技术尚未准入，拒绝授权（技术 5 待准入）
        TechAuthorization notAdmitted = newAuth("孙小红", 5L, 5L, 1);
        assertThrows(IllegalStateException.class, () -> techAuthorizationService.save(notAdmitted));

        // 技术已停用，拒绝授权（技术 6 已停用）
        TechAuthorization suspended = newAuth("孙小红", 5L, 6L, 1);
        assertThrows(IllegalStateException.class, () -> techAuthorizationService.save(suspended));

        // 授权结束日期早于开始日期，拒绝
        TechAuthorization badDate = newAuth("孙小红", 5L, 2L, 1);
        badDate.setEndDate(LocalDate.of(2025, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> techAuthorizationService.save(badDate));

        // 同一医师同一技术已有已通过授权，拒绝重复申请（陈国强已有技术 1 的已通过授权）
        TechAuthorization dup = newAuth("陈国强", 1L, 1L, 1);
        assertThrows(IllegalArgumentException.class, () -> techAuthorizationService.save(dup));

        // 禁止类技术不得授权（临时建一条已准入的禁止类技术）
        MedicalTech prohibitedTech = newTech("测试禁止技术", "JS-TEST-004");
        prohibitedTech.setAccessType("PROHIBITED");
        prohibitedTech.setStatus(TechGradingTypeUtil.TECH_STATUS_ADMITTED);
        assertTrue(medicalTechService.save(prohibitedTech));
        try {
            TechAuthorization prohibited = newAuth("孙小红", 5L, prohibitedTech.getId(), 1);
            assertThrows(IllegalStateException.class, () -> techAuthorizationService.save(prohibited));
        } finally {
            medicalTechService.getBaseMapper().deleteById(prohibitedTech.getId());
        }

        // 合法申请：默认待审批、开始日期默认为当天、科室按医师档案补全
        TechAuthorization a = newAuth("孙小红", 5L, 2L, 1);
        a.setDeptName(null);
        assertTrue(techAuthorizationService.save(a));
        try {
            assertNotNull(a.getId());
            assertEquals(0, a.getStatus());
            assertEquals(LocalDate.now(), a.getStartDate());
            assertEquals("儿科", a.getDeptName());
            assertEquals("JS-2024-002", a.getTechNo());
            assertEquals(1, a.getTechLevel());
        } finally {
            techAuthorizationService.removeById(a.getId());
        }
    }

    @Test
    void authApproveRejectRevokeFlow() {
        // 审批通过（种子 id=5 赵丽华的技术 1 一级授权申请）需要审批人
        ApproveRequest noApprover = new ApproveRequest();
        assertThrows(IllegalArgumentException.class, () -> techAuthorizationService.approve(5L, noApprover));

        ApproveRequest req = new ApproveRequest();
        req.setApprover("吴文斌");
        req.setOpinion("同意");
        TechAuthorization approved = techAuthorizationService.approve(5L, req);
        try {
            assertEquals(1, approved.getStatus());
            assertEquals("吴文斌", approved.getApprover());
            assertTrue(approved.getApprovalHistory().contains("通过"));

            // 已通过的授权不能再次审批，也不能直接删除
            assertThrows(IllegalStateException.class, () -> techAuthorizationService.approve(5L, req));
            assertThrows(IllegalStateException.class, () -> techAuthorizationService.removeById(5L));

            // 注销需要注销原因
            RejectRequest noReason = new RejectRequest();
            assertThrows(IllegalArgumentException.class, () -> techAuthorizationService.revoke(5L, noReason));

            RejectRequest revokeReq = new RejectRequest();
            revokeReq.setApprover("吴文斌");
            revokeReq.setReason("医师调离该科室");
            TechAuthorization revoked = techAuthorizationService.revoke(5L, revokeReq);
            assertEquals(3, revoked.getStatus());
            assertEquals("医师调离该科室", revoked.getRevokeReason());
            assertTrue(revoked.getApprovalHistory().contains("注销"));

            // 已注销的授权不能再次注销，也不能审批
            assertThrows(IllegalStateException.class, () -> techAuthorizationService.revoke(5L, revokeReq));
            assertThrows(IllegalStateException.class, () -> techAuthorizationService.approve(5L, req));
        } finally {
            // 恢复种子 id=5：改回待审批，清空审批与注销信息
            techAuthorizationService.getBaseMapper().update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TechAuthorization>()
                            .eq(TechAuthorization::getId, 5L)
                            .set(TechAuthorization::getStatus, 0)
                            .set(TechAuthorization::getApprover, null)
                            .set(TechAuthorization::getApprovalHistory, null)
                            .set(TechAuthorization::getRevokeReason, null));
            assertEquals(0, techAuthorizationService.getById(5L).getStatus());
            assertNull(techAuthorizationService.getById(5L).getRevokeReason());
        }

        // 已驳回的授权不能审批（种子 id=6）
        assertThrows(IllegalStateException.class, () -> techAuthorizationService.approve(6L, req));
        RejectRequest rejectReq = new RejectRequest();
        rejectReq.setReason("测试");
        assertThrows(IllegalStateException.class, () -> techAuthorizationService.reject(6L, rejectReq));
    }

    @Test
    void authDeleteGuard() {
        // 已通过的授权不能直接删除（种子 id=1）
        assertThrows(IllegalStateException.class, () -> techAuthorizationService.removeById(1L));

        // 待审批的授权可以删除；已驳回的授权也可以删除（用临时记录，不动种子）
        TechAuthorization pending = newAuth("孙小红", 5L, 2L, 1);
        assertTrue(techAuthorizationService.save(pending));
        assertTrue(techAuthorizationService.removeById(pending.getId()));
        assertNull(techAuthorizationService.getById(pending.getId()));

        TechAuthorization toReject = newAuth("孙小红", 5L, 2L, 1);
        assertTrue(techAuthorizationService.save(toReject));
        RejectRequest rejectReq = new RejectRequest();
        rejectReq.setApprover("吴文斌");
        rejectReq.setReason("测试驳回");
        TechAuthorization rejected = techAuthorizationService.reject(toReject.getId(), rejectReq);
        assertEquals(2, rejected.getStatus());
        assertTrue(techAuthorizationService.removeById(toReject.getId()));
        assertNull(techAuthorizationService.getById(toReject.getId()));
    }

    @Test
    void statsCounts() {
        Map<String, Object> s = medicalTechService.stats();
        assertEquals(6L, s.get("total"));
        assertEquals(1L, s.get("pending")); // 基因编辑
        assertEquals(4L, s.get("admitted")); // 穴位贴敷、针刺、小针刀、介入栓塞
        assertEquals(1L, s.get("suspended")); // 中药熏蒸
        Map<?, ?> byLevel = (Map<?, ?>) s.get("byLevel");
        assertEquals(3L, byLevel.get("1"));
        assertEquals(1L, byLevel.get("2"));
        assertEquals(2L, byLevel.get("3"));
        Map<?, ?> byAccessType = (Map<?, ?>) s.get("byAccessType");
        assertEquals(4L, byAccessType.get("NORMAL"));
        assertEquals(1L, byAccessType.get("RESTRICTED"));
        assertEquals(1L, byAccessType.get("PROHIBITED"));
        Map<?, ?> byCategory = (Map<?, ?>) s.get("byCategory");
        assertEquals(3L, byCategory.get("TCM"));
        assertEquals(2L, byCategory.get("SURGICAL"));
        assertEquals(0L, byCategory.get("NON_SURGICAL"));
        assertEquals(0L, byCategory.get("DIAGNOSTIC"));
        assertEquals(1L, byCategory.get("OTHER"));

        // 90 天内到期 1 项（小针刀，2026-12-20）；已过期 1 项（介入栓塞，2026-11-15）
        assertEquals(1L, s.get("expiringSoon"));
        assertEquals(1L, s.get("expired"));

        // 准入申请统计：总数 6、待审批 2、已通过 3、已驳回 1
        assertEquals(6L, s.get("admissionTotal"));
        assertEquals(2L, s.get("admissionPending"));
        assertEquals(3L, s.get("admissionApproved"));
        assertEquals(1L, s.get("admissionRejected"));

        // 医师授权统计：总数 6、待审批 1、已通过 4、90 天内到期 1、已过期 1
        assertEquals(6L, s.get("authTotal"));
        assertEquals(1L, s.get("authPending"));
        assertEquals(4L, s.get("authApproved"));
        assertEquals(1L, s.get("authExpiringSoon"));
        assertEquals(1L, s.get("authExpired"));

        // 按技术分级汇总：一级 3 项、已准入 2 项（中药熏蒸已停用）；二级 1 项、已准入 1 项；三级 2 项、已准入 1 项
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byLevelList = (List<Map<String, Object>>) s.get("byLevelList");
        Map<String, Object> level1 = byLevelList.stream()
                .filter(p -> Integer.valueOf(1).equals(p.get("techLevel"))).findFirst().orElseThrow();
        assertEquals(3L, level1.get("count"));
        assertEquals(2L, level1.get("admittedCount"));
        Map<String, Object> level3 = byLevelList.stream()
                .filter(p -> Integer.valueOf(3).equals(p.get("techLevel"))).findFirst().orElseThrow();
        assertEquals(2L, level3.get("count"));
        assertEquals(1L, level3.get("admittedCount"));

        // 按技术分类汇总：中医技术 3 项、已准入 2 项（中药熏蒸已停用）；手术类 2 项、已准入 2 项；其他 1 项、已准入 0 项
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byCategoryList = (List<Map<String, Object>>) s.get("byCategoryList");
        Map<String, Object> tcm = byCategoryList.stream()
                .filter(p -> "TCM".equals(p.get("category"))).findFirst().orElseThrow();
        assertEquals(3L, tcm.get("count"));
        assertEquals(2L, tcm.get("admittedCount"));
        Map<String, Object> other = byCategoryList.stream()
                .filter(p -> "OTHER".equals(p.get("category"))).findFirst().orElseThrow();
        assertEquals(1L, other.get("count"));
        assertEquals(0L, other.get("admittedCount"));

        // 按开展科室汇总：5 个科室各 1 项；妇科（中药熏蒸）已准入 0 项
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDeptList = (List<Map<String, Object>>) s.get("byDeptList");
        assertEquals(5, byDeptList.size());
        Map<String, Object> fuke = byDeptList.stream()
                .filter(p -> "妇科".equals(p.get("deptName"))).findFirst().orElseThrow();
        assertEquals(1L, fuke.get("count"));
        assertEquals(0L, fuke.get("admittedCount"));

        // 准入申请统计
        Map<String, Object> as = techAdmissionService.stats();
        assertEquals(6L, as.get("total"));
        assertEquals(2L, as.get("pending"));
        assertEquals(3L, as.get("approved"));
        assertEquals(1L, as.get("rejected"));
        Map<?, ?> byType = (Map<?, ?>) as.get("byType");
        assertEquals(4L, byType.get("NEW"));
        assertEquals(1L, byType.get("ADD_DEPT"));
        assertEquals(0L, byType.get("CHANGE_LEVEL"));
        assertEquals(1L, byType.get("RENEW"));

        // 医师授权统计
        Map<String, Object> us = techAuthorizationService.stats();
        assertEquals(6L, us.get("total"));
        assertEquals(1L, us.get("pending"));
        assertEquals(4L, us.get("approved"));
        assertEquals(1L, us.get("rejected"));
        assertEquals(0L, us.get("revoked"));
        Map<?, ?> authByLevel = (Map<?, ?>) us.get("byLevel");
        assertEquals(3L, authByLevel.get("1"));
        assertEquals(2L, authByLevel.get("2"));
        assertEquals(1L, authByLevel.get("3"));
        assertEquals(1L, us.get("expiringSoon"));
        assertEquals(1L, us.get("expired"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> authByDept = (List<Map<String, Object>>) us.get("byDeptList");
        assertEquals(6, authByDept.size());
    }

    @Test
    void detailNotFound() {
        assertThrows(IllegalArgumentException.class, () -> medicalTechService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> techAdmissionService.detail(999999L));
        assertThrows(IllegalArgumentException.class, () -> techAuthorizationService.detail(999999L));
    }

    @Test
    void excelRoundTrip() {
        // 技术档案导出再读回：关键字段一致
        MedicalTechQuery q = new MedicalTechQuery();
        q.setPage(1);
        q.setSize(100);
        List<MedicalTech> all = medicalTechService.listByQuery(q);
        assertTrue(all.size() >= 6);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MedicalTechExcelUtil.write(all, out);
        assertTrue(out.size() > 0);

        List<MedicalTech> back = MedicalTechExcelUtil.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(all.size(), back.size());
        MedicalTech first = all.stream()
                .filter(p -> "JS-2024-001".equals(p.getTechNo()))
                .findFirst().orElseThrow();
        MedicalTech firstBack = back.stream()
                .filter(p -> "JS-2024-001".equals(p.getTechNo()))
                .findFirst().orElseThrow();
        assertEquals(first.getTechName(), firstBack.getTechName());
        assertEquals(first.getCategory(), firstBack.getCategory());
        assertEquals(first.getTechLevel(), firstBack.getTechLevel());
        assertEquals(first.getAccessType(), firstBack.getAccessType());
        assertEquals(first.getDeptName(), firstBack.getDeptName());
        assertEquals(first.getRiskLevel(), firstBack.getRiskLevel());
        assertEquals(first.getEthicsRequired(), firstBack.getEthicsRequired());
        assertEquals(first.getStatus(), firstBack.getStatus());
        assertEquals(first.getAdmissionDate(), firstBack.getAdmissionDate());
        assertEquals(first.getExpiryDate(), firstBack.getExpiryDate());
        assertEquals(first.getApprover(), firstBack.getApprover());

        // 准入申请导出再读回
        TechAdmissionQuery aq = new TechAdmissionQuery();
        aq.setPage(1);
        aq.setSize(100);
        List<TechAdmission> allA = techAdmissionService.listByQuery(aq);
        assertTrue(allA.size() >= 6);
        ByteArrayOutputStream outA = new ByteArrayOutputStream();
        TechAdmissionExcelUtil.write(allA, outA);
        assertTrue(outA.size() > 0);
        List<TechAdmission> backA = TechAdmissionExcelUtil.read(new ByteArrayInputStream(outA.toByteArray()));
        assertEquals(allA.size(), backA.size());
        TechAdmission a5 = allA.stream().filter(p -> Long.valueOf(5L).equals(p.getId())).findFirst().orElseThrow();
        TechAdmission a5Back = backA.stream().filter(p -> "针刺治疗技术".equals(p.getTechName())
                && "ADD_DEPT".equals(p.getApplyType())).findFirst().orElseThrow();
        assertEquals(a5.getApplicant(), a5Back.getApplicant());
        assertEquals(a5.getNewDept(), a5Back.getNewDept());
        assertEquals(a5.getApplyDate(), a5Back.getApplyDate());
        assertEquals(a5.getStatus(), a5Back.getStatus());

        // 医师授权导出再读回
        TechAuthorizationQuery uq = new TechAuthorizationQuery();
        uq.setPage(1);
        uq.setSize(100);
        List<TechAuthorization> allU = techAuthorizationService.listByQuery(uq);
        assertTrue(allU.size() >= 6);
        ByteArrayOutputStream outU = new ByteArrayOutputStream();
        TechAuthorizationExcelUtil.write(allU, outU);
        assertTrue(outU.size() > 0);
        List<TechAuthorization> backU = TechAuthorizationExcelUtil.read(new ByteArrayInputStream(outU.toByteArray()));
        assertEquals(allU.size(), backU.size());
        TechAuthorization u1 = allU.stream().filter(p -> Long.valueOf(1L).equals(p.getId())).findFirst().orElseThrow();
        TechAuthorization u1Back = backU.stream().filter(p -> "陈国强".equals(p.getPhysicianName())).findFirst().orElseThrow();
        assertEquals(u1.getTechName(), u1Back.getTechName());
        assertEquals(u1.getTechLevel(), u1Back.getTechLevel());
        assertEquals(u1.getAuthLevel(), u1Back.getAuthLevel());
        assertEquals(u1.getStartDate(), u1Back.getStartDate());
        assertEquals(u1.getEndDate(), u1Back.getEndDate());
        assertEquals(u1.getStatus(), u1Back.getStatus());
    }

    @Test
    void importViaServiceRoundTrip() {
        // 技术档案：新建一条待准入技术 → 导出全部 → 删掉这条 → 再导回，应只补回这一条
        MedicalTech fresh = newTech("导入测试技术", "JS-TEST-005");
        assertTrue(medicalTechService.save(fresh));

        MedicalTechQuery q = new MedicalTechQuery();
        q.setPage(1);
        q.setSize(100);
        List<MedicalTech> all = medicalTechService.listByQuery(q);
        assertTrue(all.size() >= 7);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MedicalTechExcelUtil.write(all, out);

        assertTrue(medicalTechService.removeById(fresh.getId()));
        assertEquals(all.size() - 1, medicalTechService.listByQuery(q).size());

        int imported = medicalTechService.importExcel(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, imported); // 6 条种子因技术编号重复而跳过，只补回导入测试技术这一条
        assertEquals(all.size(), medicalTechService.listByQuery(q).size());
        MedicalTech restored = medicalTechService.listByQuery(q).stream()
                .filter(p -> "JS-TEST-005".equals(p.getTechNo()))
                .findFirst().orElseThrow();
        assertEquals("TCM", restored.getCategory());
        assertEquals(1, restored.getTechLevel());
        assertEquals(0, restored.getStatus()); // 导入的技术均为待准入
        assertTrue(medicalTechService.removeById(restored.getId()));
        assertEquals(all.size() - 1, medicalTechService.listByQuery(q).size());

        // 准入申请：新建一条技术 6 的变更分级申请 → 导出全部 → 删掉这条 → 再导回，应只补回这一条
        TechAdmission freshA = newAdmission(6L, "CHANGE_LEVEL");
        freshA.setNewLevel(2);
        assertTrue(techAdmissionService.save(freshA));

        TechAdmissionQuery aq = new TechAdmissionQuery();
        aq.setPage(1);
        aq.setSize(100);
        List<TechAdmission> allA = techAdmissionService.listByQuery(aq);
        assertTrue(allA.size() >= 7);

        ByteArrayOutputStream outA = new ByteArrayOutputStream();
        TechAdmissionExcelUtil.write(allA, outA);

        assertTrue(techAdmissionService.removeById(freshA.getId()));
        assertEquals(allA.size() - 1, techAdmissionService.listByQuery(aq).size());

        int importedA = techAdmissionService.importExcel(new ByteArrayInputStream(outA.toByteArray()));
        assertEquals(1, importedA); // 6 条种子因“同一技术同一类型同一申请日期”重复而跳过
        assertEquals(allA.size(), techAdmissionService.listByQuery(aq).size());
        TechAdmission restoredA = techAdmissionService.listByQuery(aq).stream()
                .filter(p -> Long.valueOf(6L).equals(p.getTechId()) && "CHANGE_LEVEL".equals(p.getApplyType()))
                .findFirst().orElseThrow();
        assertEquals(2, restoredA.getNewLevel());
        assertEquals(0, restoredA.getStatus()); // 导入的申请均为待审批
        assertTrue(techAdmissionService.removeById(restoredA.getId()));
        assertEquals(allA.size() - 1, techAdmissionService.listByQuery(aq).size());

        // 医师授权：新建一条孙小红的技术 2 一级授权申请 → 导出全部 → 删掉这条 → 再导回，应只补回这一条
        TechAuthorization freshU = newAuth("孙小红", 5L, 2L, 1);
        assertTrue(techAuthorizationService.save(freshU));

        TechAuthorizationQuery uq = new TechAuthorizationQuery();
        uq.setPage(1);
        uq.setSize(100);
        List<TechAuthorization> allU = techAuthorizationService.listByQuery(uq);
        assertTrue(allU.size() >= 7);

        ByteArrayOutputStream outU = new ByteArrayOutputStream();
        TechAuthorizationExcelUtil.write(allU, outU);

        assertTrue(techAuthorizationService.removeById(freshU.getId()));
        assertEquals(allU.size() - 1, techAuthorizationService.listByQuery(uq).size());

        int importedU = techAuthorizationService.importExcel(new ByteArrayInputStream(outU.toByteArray()));
        assertEquals(1, importedU); // 6 条种子因“同一医师同一技术同一开始日期”重复而跳过
        assertEquals(allU.size(), techAuthorizationService.listByQuery(uq).size());
        TechAuthorization restoredU = techAuthorizationService.listByQuery(uq).stream()
                .filter(p -> "孙小红".equals(p.getPhysicianName()) && Long.valueOf(2L).equals(p.getTechId()))
                .findFirst().orElseThrow();
        assertEquals(1, restoredU.getAuthLevel());
        assertEquals(0, restoredU.getStatus()); // 导入的授权均为待审批
        assertTrue(techAuthorizationService.removeById(restoredU.getId()));
        assertEquals(allU.size() - 1, techAuthorizationService.listByQuery(uq).size());
    }

    private MedicalTech newTech(String techName, String techNo) {
        MedicalTech p = new MedicalTech();
        p.setTechNo(techNo);
        p.setTechName(techName);
        p.setCategory("TCM");
        p.setTechLevel(1);
        p.setAccessType("NORMAL");
        p.setDeptName("中医内科");
        p.setRiskLevel("LOW");
        p.setEthicsRequired(0);
        p.setDescription("测试技术简介。");
        p.setAdmissionDate(LocalDate.of(2026, 1, 1));
        p.setExpiryDate(LocalDate.of(2027, 12, 31));
        return p;
    }

    private TechAdmission newAdmission(Long techId, String applyType) {
        TechAdmission a = new TechAdmission();
        a.setTechId(techId);
        a.setApplyType(applyType);
        a.setApplyDept("医务科");
        a.setApplicant("吴文斌");
        a.setApplyDate(LocalDate.now());
        a.setReason("测试申请。");
        return a;
    }

    private TechAuthorization newAuth(String physicianName, Long physicianId, Long techId, int authLevel) {
        TechAuthorization a = new TechAuthorization();
        a.setPhysicianName(physicianName);
        a.setPhysicianId(physicianId);
        a.setTechId(techId);
        a.setAuthLevel(authLevel);
        a.setStartDate(LocalDate.now());
        a.setEndDate(LocalDate.now().plusYears(1));
        return a;
    }
}
