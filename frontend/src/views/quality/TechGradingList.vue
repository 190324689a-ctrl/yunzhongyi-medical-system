<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">技术档案总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.pending || 0 }}</div><div class="label">待准入</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ stats.admitted || 0 }}</div><div class="label">已准入</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.suspended || 0 }}</div><div class="label">已停用</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ stats.expiringSoon || 0 }}</div><div class="label">90天内到期</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ stats.expired || 0 }}</div><div class="label">已过期</div></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.admissionTotal || 0 }}</div><div class="label">准入申请总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ stats.admissionPending || 0 }}</div><div class="label">准入待审批</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ stats.admissionApproved || 0 }}</div><div class="label">准入已通过</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.authTotal || 0 }}</div><div class="label">医师授权总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ stats.authPending || 0 }}</div><div class="label">授权待审批</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ stats.authExpired || 0 }}</div><div class="label">授权已过期</div></div></el-card></el-col>
    </el-row>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ============ 技术档案 ============ -->
      <el-tab-pane label="技术档案" name="tech">
        <el-card>
          <el-form :inline="true" :model="techQuery" class="search-form">
            <el-form-item label="关键字">
              <el-input v-model="techQuery.keyword" placeholder="技术名称 / 技术编号" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item label="技术分类">
              <el-select v-model="techQuery.category" placeholder="全部分类" clearable style="width: 130px">
                <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="技术分级">
              <el-select v-model="techQuery.techLevel" placeholder="全部分级" clearable style="width: 110px">
                <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="准入类别">
              <el-select v-model="techQuery.accessType" placeholder="全部类别" clearable style="width: 130px">
                <el-option v-for="a in accessTypes" :key="a.value" :label="a.label" :value="a.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="风险等级">
              <el-select v-model="techQuery.riskLevel" placeholder="全部等级" clearable style="width: 110px">
                <el-option v-for="r in riskLevels" :key="r.value" :label="r.label" :value="r.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="档案状态">
              <el-select v-model="techQuery.status" placeholder="全部状态" clearable style="width: 110px">
                <el-option v-for="s in techStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="开展科室">
              <el-input v-model="techQuery.deptName" placeholder="如：骨伤科" clearable style="width: 130px" />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="techQuery.expiringSoon">只看90天内到期</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="techQuery.expired">只看已过期</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleTechSearch">查询</el-button>
              <el-button @click="handleTechReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div>
              <el-button type="primary" @click="handleTechAdd">新增技术档案</el-button>
            </div>
            <div>
              <el-button type="danger" :disabled="!techSelected.length" @click="handleTechBatchDelete">批量删除</el-button>
              <el-upload
                action="/api/medical-techs/import"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-success="handleTechImportSuccess"
                :on-error="handleImportError"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleTechExport">导出 Excel</el-button>
              <el-button @click="handleTechDownloadTemplate">下载模板</el-button>
            </div>
          </div>

          <el-table :data="techData" border stripe v-loading="techLoading" @selection-change="techSelected = $event" style="margin-top: 12px">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="techNo" label="技术编号" width="130" />
            <el-table-column prop="techName" label="技术名称" min-width="170" />
            <el-table-column prop="category" label="技术分类" width="100">
              <template #default="{ row }">{{ categoryText(row.category) }}</template>
            </el-table-column>
            <el-table-column prop="techLevel" label="技术分级" width="90">
              <template #default="{ row }">{{ levelText(row.techLevel) }}</template>
            </el-table-column>
            <el-table-column prop="accessType" label="准入类别" width="110">
              <template #default="{ row }">
                <el-tag :type="row.accessType === 'PROHIBITED' ? 'danger' : row.accessType === 'RESTRICTED' ? 'warning' : 'info'">{{ accessTypeText(row.accessType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deptName" label="开展科室" width="140" />
            <el-table-column prop="riskLevel" label="风险等级" width="90">
              <template #default="{ row }">{{ riskLevelText(row.riskLevel) }}</template>
            </el-table-column>
            <el-table-column prop="ethicsRequired" label="需伦理审查" width="100">
              <template #default="{ row }">{{ row.ethicsRequired === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column prop="admissionDate" label="准入日期" width="110" />
            <el-table-column prop="expiryDate" label="有效期" width="110">
              <template #default="{ row }">
                <span :class="{ expired: isTechExpired(row) }">{{ row.expiryDate }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="档案状态" width="90">
              <template #default="{ row }">
                <el-tag :type="techStatusTag(row.status)">{{ techStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleTechDetail(row)">详情</el-button>
                <el-button link type="primary" :disabled="row.status === 1" @click="handleTechEdit(row)">编辑</el-button>
                <el-button link type="warning" :disabled="row.status !== 1" @click="handleTechSuspend(row)">停用</el-button>
                <el-button link type="success" :disabled="row.status !== 2" @click="handleTechResume(row)">重新启用</el-button>
                <el-button link type="danger" :disabled="row.status === 1" @click="handleTechDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            style="margin-top: 16px; justify-content: flex-end"
            v-model:current-page="techQuery.page"
            v-model:page-size="techQuery.size"
            :total="techTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchTechList"
            @current-change="fetchTechList"
          />
        </el-card>

        <!-- 按分级 / 分类 / 科室统计 -->
        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="8">
            <el-card>
              <template #header><span>按技术分级统计</span></template>
              <el-table :data="levelStats" border stripe>
                <el-table-column prop="techLevelText" label="技术分级" />
                <el-table-column prop="count" label="技术数" width="90" />
                <el-table-column prop="admittedCount" label="已准入数" width="100" />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card>
              <template #header><span>按技术分类统计</span></template>
              <el-table :data="categoryStats" border stripe>
                <el-table-column prop="categoryText" label="技术分类" />
                <el-table-column prop="count" label="技术数" width="90" />
                <el-table-column prop="admittedCount" label="已准入数" width="100" />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card>
              <template #header><span>按开展科室统计</span></template>
              <el-table :data="deptStats" border stripe>
                <el-table-column prop="deptName" label="开展科室" />
                <el-table-column prop="count" label="技术数" width="90" />
                <el-table-column prop="admittedCount" label="已准入数" width="100" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>

        <!-- 已过期提醒 -->
        <el-card style="margin-top: 16px" v-if="techExpiredList.length">
          <template #header><span>已过期技术提醒（有效期已过，需延续准入或停用）</span></template>
          <el-table :data="techExpiredList" border stripe>
            <el-table-column prop="techNo" label="技术编号" width="130" />
            <el-table-column prop="techName" label="技术名称" min-width="170" />
            <el-table-column prop="deptName" label="开展科室" width="140" />
            <el-table-column prop="expiryDate" label="有效期" width="110">
              <template #default="{ row }"><span class="expired">{{ row.expiryDate }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleTechDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 准入申请 ============ -->
      <el-tab-pane label="准入申请" name="admission">
        <el-card>
          <el-form :inline="true" :model="admissionQuery" class="search-form">
            <el-form-item label="关键字">
              <el-input v-model="admissionQuery.keyword" placeholder="技术名称 / 技术编号 / 申请人" clearable style="width: 220px" />
            </el-form-item>
            <el-form-item label="申请类型">
              <el-select v-model="admissionQuery.applyType" placeholder="全部类型" clearable style="width: 150px">
                <el-option v-for="t in applyTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="申请科室">
              <el-input v-model="admissionQuery.applyDept" placeholder="如：骨伤科" clearable style="width: 130px" />
            </el-form-item>
            <el-form-item label="申请状态">
              <el-select v-model="admissionQuery.status" placeholder="全部状态" clearable style="width: 110px">
                <el-option v-for="s in admissionStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAdmissionSearch">查询</el-button>
              <el-button @click="handleAdmissionReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div>
              <el-button type="primary" @click="handleAdmissionAdd">新增准入申请</el-button>
            </div>
            <div>
              <el-button type="danger" :disabled="!admissionSelected.length" @click="handleAdmissionBatchDelete">批量删除</el-button>
              <el-upload
                action="/api/tech-admissions/import"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-success="handleAdmissionImportSuccess"
                :on-error="handleImportError"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleAdmissionExport">导出 Excel</el-button>
              <el-button @click="handleAdmissionDownloadTemplate">下载模板</el-button>
            </div>
          </div>

          <el-table :data="admissionData" border stripe v-loading="admissionLoading" @selection-change="admissionSelected = $event" style="margin-top: 12px">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="techNo" label="技术编号" width="130" />
            <el-table-column prop="techName" label="技术名称" min-width="170" />
            <el-table-column prop="applyType" label="申请类型" width="130">
              <template #default="{ row }">{{ applyTypeText(row.applyType) }}</template>
            </el-table-column>
            <el-table-column prop="applyDept" label="申请科室" width="120" />
            <el-table-column prop="applicant" label="申请人" width="90" />
            <el-table-column prop="applyDate" label="申请日期" width="110" />
            <el-table-column label="变更内容" min-width="180">
              <template #default="{ row }">
                <span v-if="row.applyType === 'CHANGE_LEVEL'">变更后分级：{{ levelText(row.newLevel) }}</span>
                <span v-else-if="row.applyType === 'ADD_DEPT'">新增科室：{{ row.newDept }}</span>
                <span v-else-if="row.applyType === 'RENEW'">延续后有效期：{{ row.newExpiryDate }}</span>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="申请状态" width="90">
              <template #default="{ row }">
                <el-tag :type="admissionStatusTag(row.status)">{{ admissionStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approver" label="审批人" width="90" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status !== 0" @click="handleAdmissionEdit(row)">编辑</el-button>
                <el-button link type="success" :disabled="row.status !== 0" @click="handleAdmissionApprove(row)">通过</el-button>
                <el-button link type="warning" :disabled="row.status !== 0" @click="handleAdmissionReject(row)">驳回</el-button>
                <el-button link type="danger" :disabled="row.status === 1" @click="handleAdmissionDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            style="margin-top: 16px; justify-content: flex-end"
            v-model:current-page="admissionQuery.page"
            v-model:page-size="admissionQuery.size"
            :total="admissionTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchAdmissionList"
            @current-change="fetchAdmissionList"
          />
        </el-card>
      </el-tab-pane>

      <!-- ============ 医师授权 ============ -->
      <el-tab-pane label="医师授权" name="auth">
        <el-card>
          <el-form :inline="true" :model="authQuery" class="search-form">
            <el-form-item label="关键字">
              <el-input v-model="authQuery.keyword" placeholder="医师姓名 / 技术名称 / 技术编号" clearable style="width: 220px" />
            </el-form-item>
            <el-form-item label="授权级别">
              <el-select v-model="authQuery.authLevel" placeholder="全部级别" clearable style="width: 110px">
                <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="授权状态">
              <el-select v-model="authQuery.status" placeholder="全部状态" clearable style="width: 110px">
                <el-option v-for="s in authStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="科室">
              <el-input v-model="authQuery.deptName" placeholder="如：骨伤科" clearable style="width: 130px" />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="authQuery.expiringSoon">只看90天内到期</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="authQuery.expired">只看已过期</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAuthSearch">查询</el-button>
              <el-button @click="handleAuthReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div>
              <el-button type="primary" @click="handleAuthAdd">新增授权申请</el-button>
            </div>
            <div>
              <el-button type="danger" :disabled="!authSelected.length" @click="handleAuthBatchDelete">批量删除</el-button>
              <el-upload
                action="/api/tech-authorizations/import"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-success="handleAuthImportSuccess"
                :on-error="handleImportError"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleAuthExport">导出 Excel</el-button>
              <el-button @click="handleAuthDownloadTemplate">下载模板</el-button>
            </div>
          </div>

          <el-table :data="authData" border stripe v-loading="authLoading" @selection-change="authSelected = $event" style="margin-top: 12px">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="physicianName" label="医师姓名" width="100" />
            <el-table-column prop="deptName" label="科室" width="110" />
            <el-table-column prop="techNo" label="技术编号" width="130" />
            <el-table-column prop="techName" label="技术名称" min-width="170" />
            <el-table-column prop="techLevel" label="技术分级" width="90">
              <template #default="{ row }">{{ levelText(row.techLevel) }}</template>
            </el-table-column>
            <el-table-column prop="authLevel" label="授权级别" width="90">
              <template #default="{ row }">{{ levelText(row.authLevel) }}</template>
            </el-table-column>
            <el-table-column prop="startDate" label="授权开始日期" width="120" />
            <el-table-column prop="endDate" label="授权结束日期" width="120">
              <template #default="{ row }">
                <span :class="{ expired: isAuthExpired(row) }">{{ row.endDate }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="授权状态" width="90">
              <template #default="{ row }">
                <el-tag :type="authStatusTag(row.status)">{{ authStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approver" label="审批人" width="90" />
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status !== 0" @click="handleAuthEdit(row)">编辑</el-button>
                <el-button link type="success" :disabled="row.status !== 0" @click="handleAuthApprove(row)">通过</el-button>
                <el-button link type="warning" :disabled="row.status !== 0" @click="handleAuthReject(row)">驳回</el-button>
                <el-button link type="warning" :disabled="row.status !== 1" @click="handleAuthRevoke(row)">注销</el-button>
                <el-button link type="danger" :disabled="row.status === 1" @click="handleAuthDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            style="margin-top: 16px; justify-content: flex-end"
            v-model:current-page="authQuery.page"
            v-model:page-size="authQuery.size"
            :total="authTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchAuthList"
            @current-change="fetchAuthList"
          />
        </el-card>

        <!-- 按科室统计 -->
        <el-card style="margin-top: 16px">
          <template #header><span>按科室统计授权</span></template>
          <el-table :data="authDeptStats" border stripe>
            <el-table-column prop="deptName" label="科室" />
            <el-table-column prop="count" label="授权数" width="120" />
            <el-table-column prop="approvedCount" label="已通过数" width="120" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 技术档案新增/编辑弹窗 -->
    <el-dialog v-model="techDialogVisible" :title="techDialogTitle" width="640px">
      <el-form ref="techFormRef" :model="techForm" :rules="techRules" label-width="110px">
        <el-form-item label="技术编号" prop="techNo">
          <el-input v-model="techForm.techNo" placeholder="如：JS-2024-001" />
        </el-form-item>
        <el-form-item label="技术名称" prop="techName">
          <el-input v-model="techForm.techName" placeholder="如：小针刀松解术" />
        </el-form-item>
        <el-form-item label="技术分类" prop="category">
          <el-select v-model="techForm.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="技术分级" prop="techLevel">
          <el-select v-model="techForm.techLevel" placeholder="请选择" style="width: 100%">
            <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="准入类别" prop="accessType">
          <el-select v-model="techForm.accessType" placeholder="请选择" style="width: 100%">
            <el-option v-for="a in accessTypes" :key="a.value" :label="a.label" :value="a.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开展科室">
          <el-input v-model="techForm.deptName" placeholder="如：骨伤科，多个科室用顿号分隔" />
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="techForm.riskLevel" placeholder="请选择" style="width: 100%">
            <el-option v-for="r in riskLevels" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="需要伦理审查">
          <el-switch v-model="techForm.ethicsBool" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="techForm.expiryDate" type="date" value-format="YYYY-MM-DD" placeholder="准入有效期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="技术简介">
          <el-input v-model="techForm.description" type="textarea" :rows="3" placeholder="适应症、禁忌症等说明" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="techForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="techDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTechSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 技术停用弹窗 -->
    <el-dialog v-model="suspendVisible" title="停用技术" width="480px">
      <el-form label-width="90px">
        <el-form-item label="技术名称"><span>{{ suspendRow.techName }}</span></el-form-item>
        <el-form-item label="审批人">
          <el-input v-model="suspendForm.approver" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item label="停用原因">
          <el-input v-model="suspendForm.reason" type="textarea" :rows="3" placeholder="必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="suspendVisible = false">取消</el-button>
        <el-button type="warning" @click="handleSuspendSubmit">停用</el-button>
      </template>
    </el-dialog>

    <!-- 技术详情弹窗 -->
    <el-dialog v-model="detailVisible" title="技术档案详情" width="800px">
      <el-tabs v-model="detailTab">
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="技术编号">{{ detail.techNo }}</el-descriptions-item>
            <el-descriptions-item label="技术名称">{{ detail.techName }}</el-descriptions-item>
            <el-descriptions-item label="技术分类">{{ categoryText(detail.category) }}</el-descriptions-item>
            <el-descriptions-item label="技术分级">{{ levelText(detail.techLevel) }}</el-descriptions-item>
            <el-descriptions-item label="准入类别">{{ accessTypeText(detail.accessType) }}</el-descriptions-item>
            <el-descriptions-item label="开展科室">{{ detail.deptName }}</el-descriptions-item>
            <el-descriptions-item label="风险等级">{{ riskLevelText(detail.riskLevel) }}</el-descriptions-item>
            <el-descriptions-item label="需伦理审查">{{ detail.ethicsRequired === 1 ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="档案状态">{{ techStatusText(detail.status) }}</el-descriptions-item>
            <el-descriptions-item label="准入日期">{{ detail.admissionDate }}</el-descriptions-item>
            <el-descriptions-item label="有效期">{{ detail.expiryDate }}</el-descriptions-item>
            <el-descriptions-item label="审批人">{{ detail.approver }}</el-descriptions-item>
            <el-descriptions-item label="技术简介" :span="2">{{ detail.description }}</el-descriptions-item>
            <el-descriptions-item label="停用原因" :span="2">{{ detail.suspendReason }}</el-descriptions-item>
            <el-descriptions-item label="审批记录" :span="2">{{ detail.approvalHistory }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="准入申请" name="admissions">
          <el-table :data="detailAdmissions" border stripe>
            <el-table-column prop="applyType" label="申请类型" width="130">
              <template #default="{ row }">{{ applyTypeText(row.applyType) }}</template>
            </el-table-column>
            <el-table-column prop="applicant" label="申请人" width="90" />
            <el-table-column prop="applyDate" label="申请日期" width="110" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }"><el-tag :type="admissionStatusTag(row.status)">{{ admissionStatusText(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="approvalHistory" label="审批记录" min-width="200" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="医师授权" name="auths">
          <el-table :data="detailAuths" border stripe>
            <el-table-column prop="physicianName" label="医师姓名" width="100" />
            <el-table-column prop="deptName" label="科室" width="110" />
            <el-table-column prop="authLevel" label="授权级别" width="90">
              <template #default="{ row }">{{ levelText(row.authLevel) }}</template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="110" />
            <el-table-column prop="endDate" label="结束日期" width="110" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }"><el-tag :type="authStatusTag(row.status)">{{ authStatusText(row.status) }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 准入申请新增/编辑弹窗 -->
    <el-dialog v-model="admissionDialogVisible" :title="admissionDialogTitle" width="640px">
      <el-form ref="admissionFormRef" :model="admissionForm" :rules="admissionRules" label-width="110px">
        <el-form-item label="医疗技术" prop="techId">
          <el-select v-model="admissionForm.techId" placeholder="请选择技术" style="width: 100%" @change="onAdmissionTechChange">
            <el-option v-for="t in techOptions" :key="t.id" :label="`${t.techName}（${t.techNo}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请类型" prop="applyType">
          <el-select v-model="admissionForm.applyType" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in applyTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请科室">
          <el-input v-model="admissionForm.applyDept" placeholder="如：骨伤科" />
        </el-form-item>
        <el-form-item label="申请人" prop="applicant">
          <el-input v-model="admissionForm.applicant" placeholder="如：王建军" />
        </el-form-item>
        <el-form-item label="申请日期">
          <el-date-picker v-model="admissionForm.applyDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="变更后分级" v-if="admissionForm.applyType === 'CHANGE_LEVEL'" prop="newLevel">
          <el-select v-model="admissionForm.newLevel" placeholder="请选择" style="width: 100%">
            <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="新增开展科室" v-if="admissionForm.applyType === 'ADD_DEPT'" prop="newDept">
          <el-input v-model="admissionForm.newDept" placeholder="如：妇科" />
        </el-form-item>
        <el-form-item label="延续后有效期" v-if="admissionForm.applyType === 'RENEW'" prop="newExpiryDate">
          <el-date-picker v-model="admissionForm.newExpiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="申请理由">
          <el-input v-model="admissionForm.reason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="admissionForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="admissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdmissionSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 准入审批弹窗（通过/驳回共用） -->
    <el-dialog v-model="admissionApprovalVisible" :title="admissionApprovalTitle" width="480px">
      <el-form label-width="90px">
        <el-form-item label="技术名称"><span>{{ admissionApprovalRow.techName }}</span></el-form-item>
        <el-form-item label="申请类型"><span>{{ applyTypeText(admissionApprovalRow.applyType) }}</span></el-form-item>
        <el-form-item label="审批人">
          <el-input v-model="admissionApprovalForm.approver" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item :label="admissionApprovalAction === 'approve' ? '审批意见' : '驳回原因'">
          <el-input v-model="admissionApprovalForm.opinion" type="textarea" :rows="3" :placeholder="admissionApprovalAction === 'approve' ? '如：同意' : '必填'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="admissionApprovalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdmissionApprovalSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 医师授权新增/编辑弹窗 -->
    <el-dialog v-model="authDialogVisible" :title="authDialogTitle" width="640px">
      <el-form ref="authFormRef" :model="authForm" :rules="authRules" label-width="110px">
        <el-form-item label="医师姓名" prop="physicianName">
          <el-input v-model="authForm.physicianName" placeholder="如：陈国强（按医师档案自动补全科室）" />
        </el-form-item>
        <el-form-item label="医疗技术" prop="techId">
          <el-select v-model="authForm.techId" placeholder="请选择技术" style="width: 100%" @change="onAuthTechChange">
            <el-option v-for="t in techOptions" :key="t.id" :label="`${t.techName}（${t.techNo}，${levelText(t.techLevel)}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权级别" prop="authLevel">
          <el-select v-model="authForm.authLevel" placeholder="请选择（不能高于技术分级）" style="width: 100%">
            <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权开始日期">
          <el-date-picker v-model="authForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="授权结束日期">
          <el-date-picker v-model="authForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="authForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="authDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAuthSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 授权审批/注销弹窗（通过/驳回/注销共用） -->
    <el-dialog v-model="authApprovalVisible" :title="authApprovalTitle" width="480px">
      <el-form label-width="90px">
        <el-form-item label="医师姓名"><span>{{ authApprovalRow.physicianName }}</span></el-form-item>
        <el-form-item label="技术名称"><span>{{ authApprovalRow.techName }}</span></el-form-item>
        <el-form-item label="审批人">
          <el-input v-model="authApprovalForm.approver" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item :label="authApprovalAction === 'approve' ? '审批意见' : authApprovalAction === 'reject' ? '驳回原因' : '注销原因'">
          <el-input v-model="authApprovalForm.opinion" type="textarea" :rows="3" :placeholder="authApprovalAction === 'approve' ? '如：同意' : '必填'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="authApprovalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAuthApprovalSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMedicalTechs, getMedicalTechStats, getMedicalTechStatsByLevel, getMedicalTechStatsByCategory,
  getMedicalTechStatsByDept, getMedicalTechById, createMedicalTech, updateMedicalTech,
  deleteMedicalTech, batchDeleteMedicalTechs, suspendMedicalTech, resumeMedicalTech,
  exportMedicalTechs, downloadMedicalTechTemplate,
  getTechAdmissions, getTechAdmissionStats, getTechAdmissionsByTech,
  createTechAdmission, updateTechAdmission, deleteTechAdmission, batchDeleteTechAdmissions,
  approveTechAdmission, rejectTechAdmission, exportTechAdmissions, downloadTechAdmissionTemplate,
  getTechAuthorizations, getTechAuthorizationStats, getTechAuthorizationStatsByDept,
  getTechAuthorizationsByTech, createTechAuthorization, updateTechAuthorization,
  deleteTechAuthorization, batchDeleteTechAuthorizations, approveTechAuthorization,
  rejectTechAuthorization, revokeTechAuthorization, exportTechAuthorizations,
  downloadTechAuthorizationTemplate
} from '../../api/techGrading'

const activeTab = ref('tech')

// ---------- 字典 ----------
const categories = [
  { value: 'SURGICAL', label: '手术类' },
  { value: 'NON_SURGICAL', label: '非手术类' },
  { value: 'TCM', label: '中医技术' },
  { value: 'DIAGNOSTIC', label: '诊疗技术' },
  { value: 'OTHER', label: '其他' }
]
const levels = [
  { value: 1, label: '一级' },
  { value: 2, label: '二级' },
  { value: 3, label: '三级' }
]
const accessTypes = [
  { value: 'NORMAL', label: '常规技术' },
  { value: 'RESTRICTED', label: '限制类技术' },
  { value: 'PROHIBITED', label: '禁止类技术' }
]
const riskLevels = [
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' }
]
const techStatuses = [
  { value: 0, label: '待准入' },
  { value: 1, label: '已准入' },
  { value: 2, label: '已停用' }
]
const applyTypes = [
  { value: 'NEW', label: '新技术准入' },
  { value: 'ADD_DEPT', label: '新增开展科室' },
  { value: 'CHANGE_LEVEL', label: '变更分级' },
  { value: 'RENEW', label: '延续准入' }
]
const admissionStatuses = [
  { value: 0, label: '待审批' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' }
]
const authStatuses = [
  { value: 0, label: '待审批' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' },
  { value: 3, label: '已注销' }
]

function categoryText(v) {
  return (categories.find((c) => c.value === v) || {}).label || v || ''
}
function levelText(v) {
  return (levels.find((c) => c.value === v) || {}).label || (v == null ? '' : String(v))
}
function accessTypeText(v) {
  return (accessTypes.find((c) => c.value === v) || {}).label || v || ''
}
function riskLevelText(v) {
  return (riskLevels.find((c) => c.value === v) || {}).label || v || ''
}
function techStatusText(v) {
  return (techStatuses.find((c) => c.value === v) || {}).label || ''
}
function techStatusTag(v) {
  return v === 1 ? 'success' : v === 2 ? 'info' : 'warning'
}
function applyTypeText(v) {
  return (applyTypes.find((c) => c.value === v) || {}).label || v || ''
}
function admissionStatusText(v) {
  return (admissionStatuses.find((c) => c.value === v) || {}).label || ''
}
function admissionStatusTag(v) {
  return v === 1 ? 'success' : v === 2 ? 'danger' : 'warning'
}
function authStatusText(v) {
  return (authStatuses.find((c) => c.value === v) || {}).label || ''
}
function authStatusTag(v) {
  return v === 1 ? 'success' : v === 2 ? 'danger' : v === 3 ? 'info' : 'warning'
}
function isTechExpired(row) {
  if (!row.expiryDate || row.status !== 1) return false
  return new Date(row.expiryDate) < new Date(new Date().toDateString())
}
function isAuthExpired(row) {
  if (!row.endDate || row.status !== 1) return false
  return new Date(row.endDate) < new Date(new Date().toDateString())
}

// ---------- 统计 ----------
const stats = ref({})
const levelStats = ref([])
const categoryStats = ref([])
const deptStats = ref([])
const authDeptStats = ref([])
const techExpiredList = ref([])

async function fetchStats() {
  try {
    stats.value = await getMedicalTechStats()
    levelStats.value = await getMedicalTechStatsByLevel()
    categoryStats.value = await getMedicalTechStatsByCategory()
    deptStats.value = await getMedicalTechStatsByDept()
    authDeptStats.value = await getTechAuthorizationStatsByDept()
    const d = await getMedicalTechs({ expired: true, page: 1, size: 100 })
    techExpiredList.value = d.records || d
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

// ---------- 技术档案 ----------
const techQuery = reactive({ keyword: '', category: '', techLevel: null, accessType: '', riskLevel: '', status: null, deptName: '', expiringSoon: false, expired: false, page: 1, size: 10 })
const techData = ref([])
const techTotal = ref(0)
const techLoading = ref(false)
const techSelected = ref([])
const techOptions = ref([])

async function fetchTechList() {
  techLoading.value = true
  try {
    const data = await getMedicalTechs({ ...techQuery })
    techData.value = data.records
    techTotal.value = data.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    techLoading.value = false
  }
}

async function fetchTechOptions() {
  try {
    const data = await getMedicalTechs({ page: 1, size: 10000 })
    techOptions.value = data.records || []
  } catch (e) {
    console.warn('技术下拉加载失败', e.message)
  }
}

function handleTechSearch() {
  techQuery.page = 1
  fetchTechList()
}

function handleTechReset() {
  Object.assign(techQuery, { keyword: '', category: '', techLevel: null, accessType: '', riskLevel: '', status: null, deptName: '', expiringSoon: false, expired: false, page: 1, size: 10 })
  fetchTechList()
}

function refreshAll() {
  fetchTechList()
  fetchAdmissionList()
  fetchAuthList()
  fetchStats()
  fetchTechOptions()
}

const techDialogVisible = ref(false)
const techDialogTitle = ref('新增技术档案')
const techFormRef = ref(null)
const techForm = reactive({
  id: null, techNo: '', techName: '', category: 'TCM', techLevel: 1, accessType: 'NORMAL',
  deptName: '', riskLevel: 'LOW', ethicsBool: false, expiryDate: '', description: '', remark: ''
})
const techRules = {
  techNo: [{ required: true, message: '请填写技术编号', trigger: 'blur' }],
  techName: [{ required: true, message: '请填写技术名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择技术分类', trigger: 'change' }],
  techLevel: [{ required: true, message: '请选择技术分级', trigger: 'change' }],
  accessType: [{ required: true, message: '请选择准入类别', trigger: 'change' }]
}

function resetTechForm() {
  Object.assign(techForm, {
    id: null, techNo: '', techName: '', category: 'TCM', techLevel: 1, accessType: 'NORMAL',
    deptName: '', riskLevel: 'LOW', ethicsBool: false, expiryDate: '', description: '', remark: ''
  })
}

function handleTechAdd() {
  resetTechForm()
  techDialogTitle.value = '新增技术档案'
  techDialogVisible.value = true
}

function handleTechEdit(row) {
  resetTechForm()
  Object.assign(techForm, row, { ethicsBool: row.ethicsRequired === 1 })
  techDialogTitle.value = '编辑技术档案'
  techDialogVisible.value = true
}

async function handleTechSubmit() {
  await techFormRef.value.validate()
  const payload = { ...techForm, ethicsRequired: techForm.ethicsBool ? 1 : 0 }
  delete payload.ethicsBool
  try {
    if (payload.id) {
      await updateMedicalTech(payload.id, payload)
      ElMessage.success('技术档案修改成功')
    } else {
      await createMedicalTech(payload)
      ElMessage.success('技术档案新增成功（待准入，需走准入审批）')
    }
    techDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleTechDelete(row) {
  await ElMessageBox.confirm(`确定删除技术「${row.techName}」（${row.techNo}）吗？同时删除其准入申请与授权记录。`, '提示', { type: 'warning' })
  try {
    await deleteMedicalTech(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleTechBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${techSelected.value.length} 项技术吗？已准入的技术不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteMedicalTechs(techSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

const suspendVisible = ref(false)
const suspendRow = ref({})
const suspendForm = reactive({ approver: '', reason: '' })

function handleTechSuspend(row) {
  suspendRow.value = row
  Object.assign(suspendForm, { approver: '', reason: '' })
  suspendVisible.value = true
}

async function handleSuspendSubmit() {
  if (!suspendForm.reason) {
    ElMessage.warning('请填写停用原因')
    return
  }
  try {
    await suspendMedicalTech(suspendRow.value.id, { ...suspendForm })
    ElMessage.success('技术已停用')
    suspendVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('停用失败：' + e.message)
  }
}

async function handleTechResume(row) {
  await ElMessageBox.confirm(`确定重新启用技术「${row.techName}」吗？`, '提示', { type: 'info' })
  try {
    await resumeMedicalTech(row.id)
    ElMessage.success('技术已重新启用')
    refreshAll()
  } catch (e) {
    ElMessage.error('重新启用失败：' + e.message)
  }
}

const detailVisible = ref(false)
const detailTab = ref('info')
const detail = ref({})
const detailAdmissions = ref([])
const detailAuths = ref([])

async function handleTechDetail(row) {
  try {
    detail.value = await getMedicalTechById(row.id)
    detailTab.value = 'info'
    detailAdmissions.value = await getTechAdmissionsByTech(row.id)
    detailAuths.value = await getTechAuthorizationsByTech(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function handleTechExport() {
  try {
    const res = await exportMedicalTechs({ ...techQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '医疗技术档案.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleTechDownloadTemplate() {
  const res = await downloadMedicalTechTemplate()
  downloadBlob(res.data, '医疗技术档案导入模板.xlsx')
}

function handleTechImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条（均为待准入，需走准入审批）`)
    refreshAll()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---------- 准入申请 ----------
const admissionQuery = reactive({ keyword: '', applyType: '', applyDept: '', status: null, page: 1, size: 10 })
const admissionData = ref([])
const admissionTotal = ref(0)
const admissionLoading = ref(false)
const admissionSelected = ref([])

async function fetchAdmissionList() {
  admissionLoading.value = true
  try {
    const data = await getTechAdmissions({ ...admissionQuery })
    admissionData.value = data.records
    admissionTotal.value = data.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    admissionLoading.value = false
  }
}

function handleAdmissionSearch() {
  admissionQuery.page = 1
  fetchAdmissionList()
}

function handleAdmissionReset() {
  Object.assign(admissionQuery, { keyword: '', applyType: '', applyDept: '', status: null, page: 1, size: 10 })
  fetchAdmissionList()
}

const admissionDialogVisible = ref(false)
const admissionDialogTitle = ref('新增准入申请')
const admissionFormRef = ref(null)
const admissionForm = reactive({
  id: null, techId: null, applyType: 'NEW', applyDept: '', applicant: '', applyDate: '',
  newLevel: null, newDept: '', newExpiryDate: '', reason: '', remark: ''
})
const admissionRules = {
  techId: [{ required: true, message: '请选择医疗技术', trigger: 'change' }],
  applyType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  applicant: [{ required: true, message: '请填写申请人', trigger: 'blur' }]
}

function resetAdmissionForm() {
  Object.assign(admissionForm, {
    id: null, techId: null, applyType: 'NEW', applyDept: '', applicant: '', applyDate: '',
    newLevel: null, newDept: '', newExpiryDate: '', reason: '', remark: ''
  })
}

function handleAdmissionAdd() {
  resetAdmissionForm()
  admissionDialogTitle.value = '新增准入申请'
  admissionDialogVisible.value = true
}

function handleAdmissionEdit(row) {
  resetAdmissionForm()
  Object.assign(admissionForm, row)
  admissionDialogTitle.value = '编辑准入申请'
  admissionDialogVisible.value = true
}

function onAdmissionTechChange() {
  // 技术由下拉选择，后端按档案补全编号与名称
}

async function handleAdmissionSubmit() {
  await admissionFormRef.value.validate()
  try {
    if (admissionForm.id) {
      await updateTechAdmission(admissionForm.id, { ...admissionForm })
      ElMessage.success('准入申请修改成功')
    } else {
      await createTechAdmission({ ...admissionForm })
      ElMessage.success('准入申请新增成功')
    }
    admissionDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleAdmissionDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.techName}」的${applyTypeText(row.applyType)}申请吗？`, '提示', { type: 'warning' })
  try {
    await deleteTechAdmission(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleAdmissionBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${admissionSelected.value.length} 条准入申请吗？已通过的申请不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteTechAdmissions(admissionSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

const admissionApprovalVisible = ref(false)
const admissionApprovalTitle = ref('审批通过')
const admissionApprovalAction = ref('approve')
const admissionApprovalRow = ref({})
const admissionApprovalForm = reactive({ approver: '', opinion: '' })

function handleAdmissionApprove(row) {
  admissionApprovalRow.value = row
  admissionApprovalAction.value = 'approve'
  admissionApprovalTitle.value = '审批通过'
  Object.assign(admissionApprovalForm, { approver: '', opinion: '' })
  admissionApprovalVisible.value = true
}

function handleAdmissionReject(row) {
  admissionApprovalRow.value = row
  admissionApprovalAction.value = 'reject'
  admissionApprovalTitle.value = '审批驳回'
  Object.assign(admissionApprovalForm, { approver: '', opinion: '' })
  admissionApprovalVisible.value = true
}

async function handleAdmissionApprovalSubmit() {
  if (!admissionApprovalForm.approver) {
    ElMessage.warning('请填写审批人')
    return
  }
  if (admissionApprovalAction.value === 'reject' && !admissionApprovalForm.opinion) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    if (admissionApprovalAction.value === 'approve') {
      await approveTechAdmission(admissionApprovalRow.value.id, { ...admissionApprovalForm })
      ElMessage.success('准入申请已通过，技术档案已同步')
    } else {
      await rejectTechAdmission(admissionApprovalRow.value.id, { approver: admissionApprovalForm.approver, reason: admissionApprovalForm.opinion })
      ElMessage.success('准入申请已驳回')
    }
    admissionApprovalVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

async function handleAdmissionExport() {
  try {
    const res = await exportTechAdmissions({ ...admissionQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '技术准入申请.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleAdmissionDownloadTemplate() {
  const res = await downloadTechAdmissionTemplate()
  downloadBlob(res.data, '技术准入申请导入模板.xlsx')
}

function handleAdmissionImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条（均为待审批）`)
    refreshAll()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

// ---------- 医师授权 ----------
const authQuery = reactive({ keyword: '', authLevel: null, status: null, deptName: '', expiringSoon: false, expired: false, page: 1, size: 10 })
const authData = ref([])
const authTotal = ref(0)
const authLoading = ref(false)
const authSelected = ref([])

async function fetchAuthList() {
  authLoading.value = true
  try {
    const data = await getTechAuthorizations({ ...authQuery })
    authData.value = data.records
    authTotal.value = data.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    authLoading.value = false
  }
}

function handleAuthSearch() {
  authQuery.page = 1
  fetchAuthList()
}

function handleAuthReset() {
  Object.assign(authQuery, { keyword: '', authLevel: null, status: null, deptName: '', expiringSoon: false, expired: false, page: 1, size: 10 })
  fetchAuthList()
}

const authDialogVisible = ref(false)
const authDialogTitle = ref('新增授权申请')
const authFormRef = ref(null)
const authForm = reactive({
  id: null, physicianName: '', techId: null, authLevel: 1, startDate: '', endDate: '', remark: ''
})
const authRules = {
  physicianName: [{ required: true, message: '请填写医师姓名', trigger: 'blur' }],
  techId: [{ required: true, message: '请选择医疗技术', trigger: 'change' }],
  authLevel: [{ required: true, message: '请选择授权级别', trigger: 'change' }]
}

function resetAuthForm() {
  Object.assign(authForm, {
    id: null, physicianName: '', techId: null, authLevel: 1, startDate: '', endDate: '', remark: ''
  })
}

function handleAuthAdd() {
  resetAuthForm()
  authDialogTitle.value = '新增授权申请'
  authDialogVisible.value = true
}

function handleAuthEdit(row) {
  resetAuthForm()
  Object.assign(authForm, row)
  authDialogTitle.value = '编辑授权申请'
  authDialogVisible.value = true
}

function onAuthTechChange() {
  // 技术由下拉选择，后端按档案补全编号、名称与技术分级
}

async function handleAuthSubmit() {
  await authFormRef.value.validate()
  try {
    if (authForm.id) {
      await updateTechAuthorization(authForm.id, { ...authForm })
      ElMessage.success('授权申请修改成功')
    } else {
      await createTechAuthorization({ ...authForm })
      ElMessage.success('授权申请新增成功')
    }
    authDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleAuthDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.physicianName}」的「${row.techName}」授权申请吗？`, '提示', { type: 'warning' })
  try {
    await deleteTechAuthorization(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleAuthBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${authSelected.value.length} 条授权吗？已通过的授权不能删除，请先注销。`, '提示', { type: 'warning' })
  try {
    await batchDeleteTechAuthorizations(authSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

const authApprovalVisible = ref(false)
const authApprovalTitle = ref('审批通过')
const authApprovalAction = ref('approve')
const authApprovalRow = ref({})
const authApprovalForm = reactive({ approver: '', opinion: '' })

function openAuthApproval(row, action, title) {
  authApprovalRow.value = row
  authApprovalAction.value = action
  authApprovalTitle.value = title
  Object.assign(authApprovalForm, { approver: '', opinion: '' })
  authApprovalVisible.value = true
}

function handleAuthApprove(row) {
  openAuthApproval(row, 'approve', '审批通过')
}

function handleAuthReject(row) {
  openAuthApproval(row, 'reject', '审批驳回')
}

function handleAuthRevoke(row) {
  openAuthApproval(row, 'revoke', '注销授权')
}

async function handleAuthApprovalSubmit() {
  if (!authApprovalForm.approver) {
    ElMessage.warning('请填写审批人')
    return
  }
  if (authApprovalAction.value !== 'approve' && !authApprovalForm.opinion) {
    ElMessage.warning(authApprovalAction.value === 'reject' ? '请填写驳回原因' : '请填写注销原因')
    return
  }
  try {
    if (authApprovalAction.value === 'approve') {
      await approveTechAuthorization(authApprovalRow.value.id, { ...authApprovalForm })
      ElMessage.success('授权申请已通过')
    } else if (authApprovalAction.value === 'reject') {
      await rejectTechAuthorization(authApprovalRow.value.id, { approver: authApprovalForm.approver, reason: authApprovalForm.opinion })
      ElMessage.success('授权申请已驳回')
    } else {
      await revokeTechAuthorization(authApprovalRow.value.id, { approver: authApprovalForm.approver, reason: authApprovalForm.opinion })
      ElMessage.success('授权已注销')
    }
    authApprovalVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}

async function handleAuthExport() {
  try {
    const res = await exportTechAuthorizations({ ...authQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '医师技术授权.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleAuthDownloadTemplate() {
  const res = await downloadTechAuthorizationTemplate()
  downloadBlob(res.data, '医师技术授权导入模板.xlsx')
}

function handleAuthImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条（均为待审批）`)
    refreshAll()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.search-form {
  margin-bottom: -12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
}
.stat {
  text-align: center;
}
.stat .num {
  font-size: 28px;
  font-weight: bold;
}
.stat .num.warn { color: #e6a23c; }
.stat .num.ok { color: #67c23a; }
.stat .num.bad { color: #f56c6c; }
.stat .label {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.expired {
  color: #f56c6c;
  font-weight: bold;
}
</style>
