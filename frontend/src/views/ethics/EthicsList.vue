<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ profileStats.total || 0 }}</div><div class="label">医德档案总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ profileStats.active || 0 }}</div><div class="label">在档</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ profileStats.commendationTotal || 0 }}</div><div class="label">已通过表彰（次）</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ profileStats.complaintTotal || 0 }}</div><div class="label">投诉总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ evalStats.avgScore ?? 0 }}</div><div class="label">考评平均分</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ profileStats.overdueComplaints || 0 }}</div><div class="label">逾期未反馈投诉</div></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ evalStats.pending || 0 }}</div><div class="label">待审批考评</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ complaintStats.pending || 0 }}</div><div class="label">待受理投诉</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ commendStats.pending || 0 }}</div><div class="label">待审批表彰</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ complaintStats.feedbackRate ?? 0 }}%</div><div class="label">投诉反馈率</div></div></el-card></el-col>
    </el-row>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ============ 医德档案 ============ -->
      <el-tab-pane label="医德档案" name="profile">
        <el-form :inline="true" :model="profileQuery" class="search-form">
          <el-form-item label="关键字">
            <el-input v-model="profileQuery.keyword" placeholder="医师姓名 / 档案编号" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="科室">
            <el-select v-model="profileQuery.deptId" placeholder="全部科室" clearable style="width: 140px">
              <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="profileQuery.status" placeholder="全部状态" clearable style="width: 110px">
              <el-option v-for="s in profileStatuses" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="最近考评等级">
            <el-select v-model="profileQuery.lastGrade" placeholder="全部等级" clearable style="width: 120px">
              <el-option v-for="g in grades" :key="g.value" :label="g.label" :value="g.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleProfileSearch">查询</el-button>
            <el-button @click="handleProfileReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="toolbar">
          <div><el-button type="primary" @click="handleProfileAdd">新增档案</el-button></div>
          <div>
            <el-button type="danger" :disabled="!profileSelected.length" @click="handleProfileBatchDelete">批量删除</el-button>
            <el-upload action="/api/ethics-profiles/import" :show-file-list="false" accept=".xlsx,.xls"
              :on-success="handleImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
              <el-button type="success">批量导入</el-button>
            </el-upload>
            <el-button type="warning" @click="handleProfileExport">导出 Excel</el-button>
            <el-button @click="handleProfileTemplate">下载模板</el-button>
          </div>
        </div>

        <el-table :data="profileData" border stripe v-loading="profileLoading" @selection-change="profileSelected = $event" style="margin-top: 12px">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="archiveNo" label="档案编号" width="130" />
          <el-table-column prop="physicianName" label="医师姓名" width="100" />
          <el-table-column prop="deptName" label="科室" width="110" />
          <el-table-column prop="title" label="职称" width="100" />
          <el-table-column prop="establishDate" label="建档日期" width="110" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">{{ profileStatusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="lastGrade" label="最近考评等级" width="110">
            <template #default="{ row }"><el-tag v-if="row.lastGrade" :type="gradeTag(row.lastGrade)">{{ gradeText(row.lastGrade) }}</el-tag><span v-else>—</span></template>
          </el-table-column>
          <el-table-column prop="lastEvaluateYear" label="最近考评年度" width="110" />
          <el-table-column prop="remark" label="备注" min-width="160" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleProfileEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleProfileDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="profileQuery.page"
          v-model:page-size="profileQuery.size" :total="profileTotal" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" @size-change="fetchProfiles" @current-change="fetchProfiles" />

        <el-card style="margin-top: 16px">
          <template #header><span>按科室统计</span></template>
          <el-table :data="profileDeptStats" border stripe>
            <el-table-column prop="deptName" label="科室" />
            <el-table-column prop="count" label="档案数" />
            <el-table-column prop="activeCount" label="在档数" />
            <el-table-column prop="excellentCount" label="最近考评优秀人数" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 医德考评 ============ -->
      <el-tab-pane label="医德考评" name="evaluation">
        <el-form :inline="true" :model="evalQuery" class="search-form">
          <el-form-item label="关键字">
            <el-input v-model="evalQuery.keyword" placeholder="被考评医师姓名" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="科室">
            <el-select v-model="evalQuery.deptId" placeholder="全部科室" clearable style="width: 140px">
              <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="考评模板">
            <el-select v-model="evalQuery.templateId" placeholder="全部模板" clearable style="width: 170px">
              <el-option v-for="t in enabledTemplates" :key="t.id" :label="t.templateName" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="考评年度">
            <el-input-number v-model="evalQuery.assessYear" :min="2000" :max="2100" placeholder="年度" style="width: 120px" />
          </el-form-item>
          <el-form-item label="等级">
            <el-select v-model="evalQuery.grade" placeholder="全部等级" clearable style="width: 120px">
              <el-option v-for="g in grades" :key="g.value" :label="g.label" :value="g.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="审批状态">
            <el-select v-model="evalQuery.status" placeholder="全部状态" clearable style="width: 110px">
              <el-option v-for="s in approvalStatuses" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleEvalSearch">查询</el-button>
            <el-button @click="handleEvalReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="toolbar">
          <div>
            <el-button type="primary" @click="handleEvalAdd">新增考评</el-button>
            <el-button @click="handleTemplateManage">模板管理</el-button>
          </div>
          <div>
            <el-button type="danger" :disabled="!evalSelected.length" @click="handleEvalBatchDelete">批量删除</el-button>
            <el-upload action="/api/ethics-evaluations/import" :show-file-list="false" accept=".xlsx,.xls"
              :on-success="handleImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
              <el-button type="success">批量导入</el-button>
            </el-upload>
            <el-button type="warning" @click="handleEvalExport">导出 Excel</el-button>
            <el-button @click="handleEvalTemplate">下载模板</el-button>
          </div>
        </div>

        <el-table :data="evalData" border stripe v-loading="evalLoading" @selection-change="evalSelected = $event" style="margin-top: 12px">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="physicianName" label="被考评医师" width="100" />
          <el-table-column prop="deptName" label="科室" width="110" />
          <el-table-column prop="templateName" label="考评模板" width="170" />
          <el-table-column prop="assessYear" label="年度" width="70" />
          <el-table-column prop="cycle" label="周期" width="90" />
          <el-table-column prop="itemScores" label="各项得分" min-width="180">
            <template #default="{ row }">{{ scoreItemsText(row.itemScores) }}</template>
          </el-table-column>
          <el-table-column prop="totalScore" label="总分" width="70" />
          <el-table-column prop="grade" label="等级" width="90">
            <template #default="{ row }"><el-tag :type="gradeTag(row.grade)">{{ gradeText(row.grade) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="result" label="考评结果" width="110" />
          <el-table-column prop="assessor" label="考评人" width="90" />
          <el-table-column prop="assessDate" label="考评日期" width="110" />
          <el-table-column prop="status" label="审批状态" width="90">
            <template #default="{ row }"><el-tag :type="approvalTag(row.status)">{{ approvalStatusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :disabled="row.status !== 0" @click="handleEvalEdit(row)">编辑</el-button>
              <el-button link type="success" :disabled="row.status !== 0" @click="handleEvalApprove(row)">通过</el-button>
              <el-button link type="warning" :disabled="row.status !== 0" @click="handleEvalReject(row)">驳回</el-button>
              <el-button link type="danger" :disabled="row.status === 1" @click="handleEvalDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="evalQuery.page"
          v-model:page-size="evalQuery.size" :total="evalTotal" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" @size-change="fetchEvaluations" @current-change="fetchEvaluations" />

        <el-card style="margin-top: 16px">
          <template #header><span>按科室评分统计</span></template>
          <el-table :data="evalDeptStats" border stripe>
            <el-table-column prop="deptName" label="科室" />
            <el-table-column prop="count" label="考评人数" />
            <el-table-column prop="avgScore" label="平均分" />
            <el-table-column prop="excellentCount" label="优秀人数" />
            <el-table-column prop="excellentRate" label="优秀率（%）" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 医德医风投诉 ============ -->
      <el-tab-pane label="医德医风投诉" name="complaint">
        <el-form :inline="true" :model="complaintQuery" class="search-form">
          <el-form-item label="关键字">
            <el-input v-model="complaintQuery.keyword" placeholder="投诉人 / 被投诉医师 / 投诉编号" clearable style="width: 220px" />
          </el-form-item>
          <el-form-item label="投诉类型">
            <el-select v-model="complaintQuery.complaintType" placeholder="全部类型" clearable style="width: 120px">
              <el-option v-for="t in complaintTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="流转状态">
            <el-select v-model="complaintQuery.status" placeholder="全部状态" clearable style="width: 120px">
              <el-option v-for="s in complaintStatuses" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="complaintQuery.overdue">只看逾期</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleComplaintSearch">查询</el-button>
            <el-button @click="handleComplaintReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="toolbar">
          <div><el-button type="primary" @click="handleComplaintAdd">新增投诉</el-button></div>
          <div>
            <el-button type="danger" :disabled="!complaintSelected.length" @click="handleComplaintBatchDelete">批量删除</el-button>
            <el-upload action="/api/ethics-complaints/import" :show-file-list="false" accept=".xlsx,.xls"
              :on-success="handleImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
              <el-button type="success">批量导入</el-button>
            </el-upload>
            <el-button type="warning" @click="handleComplaintExport">导出 Excel</el-button>
            <el-button @click="handleComplaintTemplate">下载模板</el-button>
          </div>
        </div>

        <el-table :data="complaintData" border stripe v-loading="complaintLoading" @selection-change="complaintSelected = $event" style="margin-top: 12px">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="complaintNo" label="投诉编号" width="150" />
          <el-table-column prop="complainantName" label="投诉人" width="90" />
          <el-table-column prop="physicianName" label="被投诉医师" width="100" />
          <el-table-column prop="deptName" label="科室" width="110" />
          <el-table-column prop="complaintType" label="投诉类型" width="100">
            <template #default="{ row }">{{ complaintTypeText(row.complaintType) }}</template>
          </el-table-column>
          <el-table-column prop="complaintDate" label="投诉日期" width="110" />
          <el-table-column prop="deadlineDate" label="处理期限" width="110">
            <template #default="{ row }"><span :class="{ overdue: isOverdue(row) }">{{ row.deadlineDate }}</span></template>
          </el-table-column>
          <el-table-column prop="status" label="流转状态" width="100">
            <template #default="{ row }"><el-tag :type="complaintTag(row.status)">{{ complaintStatusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="handler" label="处理人" width="90" />
          <el-table-column label="操作" width="330" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleComplaintDetail(row)">详情</el-button>
              <el-button link type="primary" :disabled="!(row.status === 0 || row.status === 1)" @click="handleComplaintEdit(row)">编辑</el-button>
              <el-button link type="success" :disabled="row.status !== 0" @click="handleComplaintAccept(row)">受理</el-button>
              <el-button link type="primary" :disabled="row.status !== 1" @click="handleComplaintHandle(row)">处理</el-button>
              <el-button link type="success" :disabled="row.status !== 2" @click="handleComplaintFeedback(row)">反馈</el-button>
              <el-button link type="warning" :disabled="row.status !== 0" @click="handleComplaintReject(row)">不予受理</el-button>
              <el-button link type="danger" @click="handleComplaintDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="complaintQuery.page"
          v-model:page-size="complaintQuery.size" :total="complaintTotal" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" @size-change="fetchComplaints" @current-change="fetchComplaints" />

        <!-- 逾期投诉提醒 -->
        <el-card style="margin-top: 16px" v-if="overdueList.length">
          <template #header><span>逾期未反馈投诉提醒</span></template>
          <el-table :data="overdueList" border stripe>
            <el-table-column prop="complaintNo" label="投诉编号" width="150" />
            <el-table-column prop="complainantName" label="投诉人" width="90" />
            <el-table-column prop="physicianName" label="被投诉医师" width="100" />
            <el-table-column prop="complaintType" label="投诉类型" width="100">
              <template #default="{ row }">{{ complaintTypeText(row.complaintType) }}</template>
            </el-table-column>
            <el-table-column prop="deadlineDate" label="处理期限" width="120">
              <template #default="{ row }"><span class="overdue">{{ row.deadlineDate }}</span></template>
            </el-table-column>
            <el-table-column prop="status" label="流转状态" width="100">
              <template #default="{ row }"><el-tag :type="complaintTag(row.status)">{{ complaintStatusText(row.status) }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card style="margin-top: 16px">
          <template #header><span>按科室投诉统计</span></template>
          <el-table :data="complaintDeptStats" border stripe>
            <el-table-column prop="deptName" label="科室" />
            <el-table-column prop="count" label="投诉数" />
            <el-table-column prop="pendingCount" label="待受理" />
            <el-table-column prop="investigatingCount" label="调查中" />
            <el-table-column prop="feedbackCount" label="已反馈" />
            <el-table-column prop="overdueCount" label="逾期" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ============ 医德医风表彰 ============ -->
      <el-tab-pane label="医德医风表彰" name="commendation">
        <el-form :inline="true" :model="commendQuery" class="search-form">
          <el-form-item label="关键字">
            <el-input v-model="commendQuery.keyword" placeholder="受表彰医师 / 表彰标题" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="科室">
            <el-select v-model="commendQuery.deptId" placeholder="全部科室" clearable style="width: 140px">
              <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="表彰类型">
            <el-select v-model="commendQuery.commendType" placeholder="全部类型" clearable style="width: 120px">
              <el-option v-for="t in commendTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="表彰级别">
            <el-select v-model="commendQuery.commendLevel" placeholder="全部级别" clearable style="width: 110px">
              <el-option v-for="l in commendLevels" :key="l.value" :label="l.label" :value="l.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="审批状态">
            <el-select v-model="commendQuery.status" placeholder="全部状态" clearable style="width: 110px">
              <el-option v-for="s in approvalStatuses" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleCommendSearch">查询</el-button>
            <el-button @click="handleCommendReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="toolbar">
          <div><el-button type="primary" @click="handleCommendAdd">新增表彰</el-button></div>
          <div>
            <el-button type="danger" :disabled="!commendSelected.length" @click="handleCommendBatchDelete">批量删除</el-button>
            <el-upload action="/api/ethics-commendations/import" :show-file-list="false" accept=".xlsx,.xls"
              :on-success="handleImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
              <el-button type="success">批量导入</el-button>
            </el-upload>
            <el-button type="warning" @click="handleCommendExport">导出 Excel</el-button>
            <el-button @click="handleCommendTemplate">下载模板</el-button>
          </div>
        </div>

        <el-table :data="commendData" border stripe v-loading="commendLoading" @selection-change="commendSelected = $event" style="margin-top: 12px">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="physicianName" label="受表彰医师" width="100" />
          <el-table-column prop="deptName" label="科室" width="110" />
          <el-table-column prop="commendType" label="表彰类型" width="100">
            <template #default="{ row }">{{ commendTypeText(row.commendType) }}</template>
          </el-table-column>
          <el-table-column prop="commendLevel" label="表彰级别" width="90">
            <template #default="{ row }">{{ commendLevelText(row.commendLevel) }}</template>
          </el-table-column>
          <el-table-column prop="title" label="表彰标题" min-width="180" />
          <el-table-column prop="grantedBy" label="授予方" width="120" />
          <el-table-column prop="commendDate" label="表彰日期" width="110" />
          <el-table-column prop="status" label="审批状态" width="90">
            <template #default="{ row }"><el-tag :type="approvalTag(row.status)">{{ approvalStatusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :disabled="row.status !== 0" @click="handleCommendEdit(row)">编辑</el-button>
              <el-button link type="success" :disabled="row.status !== 0" @click="handleCommendApprove(row)">通过</el-button>
              <el-button link type="warning" :disabled="row.status !== 0" @click="handleCommendReject(row)">驳回</el-button>
              <el-button link type="danger" :disabled="row.status === 1" @click="handleCommendDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="commendQuery.page"
          v-model:page-size="commendQuery.size" :total="commendTotal" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper" @size-change="fetchCommendations" @current-change="fetchCommendations" />

        <el-card style="margin-top: 16px">
          <template #header><span>按科室表彰统计</span></template>
          <el-table :data="commendDeptStats" border stripe>
            <el-table-column prop="deptName" label="科室" />
            <el-table-column prop="count" label="表彰数" />
            <el-table-column prop="passedCount" label="已通过" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增 / 编辑档案弹窗 -->
    <el-dialog v-model="profileDialogVisible" :title="profileDialogTitle" width="640px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="110px">
        <el-form-item label="档案编号" prop="archiveNo">
          <el-input v-model="profileForm.archiveNo" placeholder="如：YD-2024-001" />
        </el-form-item>
        <el-form-item label="医师" prop="physicianId">
          <el-select v-model="profileForm.physicianId" placeholder="请选择医师" style="width: 100%" @change="onProfilePhysicianChange">
            <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室">
          <el-input v-model="profileForm.deptName" placeholder="选择医师后自动补全" />
        </el-form-item>
        <el-form-item label="职称">
          <el-input v-model="profileForm.title" />
        </el-form-item>
        <el-form-item label="建档日期">
          <el-date-picker v-model="profileForm.establishDate" type="date" value-format="YYYY-MM-DD" placeholder="建档日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="profileForm.status" style="width: 100%">
            <el-option v-for="s in profileStatuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="profileForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleProfileSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑考评弹窗 -->
    <el-dialog v-model="evalDialogVisible" :title="evalDialogTitle" width="760px">
      <el-form ref="evalFormRef" :model="evalForm" :rules="evalRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="被考评医师" prop="physicianId">
              <el-select v-model="evalForm.physicianId" placeholder="请选择医师" style="width: 100%" @change="onEvalPhysicianChange">
                <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考评模板" prop="templateId">
              <el-select v-model="evalForm.templateId" placeholder="请选择模板" style="width: 100%" @change="onEvalTemplateChange">
                <el-option v-for="t in enabledTemplates" :key="t.id" :label="t.templateName" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考评年度" prop="assessYear">
              <el-input-number v-model="evalForm.assessYear" :min="2000" :max="2100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考评周期">
              <el-input v-model="evalForm.cycle" placeholder="如：年度 / 2026年第三季度" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考评人">
              <el-input v-model="evalForm.assessor" placeholder="考评人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考评日期">
              <el-date-picker v-model="evalForm.assessDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">评分录入（总分 100 分，等级由总分自动计算）</el-divider>
        <el-row :gutter="16">
          <el-col :span="12" v-for="item in evalScoreItems" :key="item.name">
            <el-form-item :label="`${item.name}（满分${item.weight}）`">
              <el-input-number v-model="evalScores[item.name]" :min="0" :max="item.weight" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert v-if="evalScoreItems.length" :title="`当前总分 ${evalComputedTotal} 分，等级：${evalComputedGradeText}`" type="info" :closable="false" show-icon />
        <el-form-item label="备注" style="margin-top: 16px"><el-input v-model="evalForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEvalSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 考评模板管理弹窗 -->
    <el-dialog title="医德考评模板管理" v-model="templateVisible" width="860px">
      <div class="toolbar" style="margin-bottom: 12px">
        <el-button type="primary" @click="handleTemplateAdd">新增模板</el-button>
      </div>
      <el-table :data="templates" border stripe>
        <el-table-column prop="templateName" label="模板名称" width="200" />
        <el-table-column label="评分项（维度：权重）">
          <template #default="{ row }">{{ templateItemsText(row.scoreItems) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTemplateEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="handleTemplateToggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" @click="handleTemplateDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 模板新增 / 编辑弹窗 -->
    <el-dialog v-model="templateFormVisible" :title="templateFormTitle" width="640px">
      <el-form :model="templateForm" label-width="110px">
        <el-form-item label="模板名称">
          <el-input v-model="templateForm.templateName" placeholder="如：医德医风年度考评模板" />
        </el-form-item>
        <el-form-item label="评分项">
          <div v-for="(item, idx) in templateItems" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px">
            <el-input v-model="item.name" placeholder="评分维度，如：医德医风" style="flex: 1" />
            <el-input-number v-model="item.weight" :min="1" :max="100" placeholder="权重" style="width: 130px" />
            <el-button type="danger" link @click="templateItems.splice(idx, 1)">删除</el-button>
          </div>
          <el-button @click="templateItems.push({ name: '', weight: null })">添加评分项</el-button>
          <span style="margin-left: 12px; color: #909399">权重合计：{{ templateWeightSum }}（须为 100）</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="templateForm.status" style="width: 100%">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="templateForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateFormVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTemplateSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 审批通过弹窗（考评 / 表彰共用） -->
    <el-dialog v-model="approveVisible" :title="approveTitle" width="500px">
      <p>{{ approveTip }}</p>
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="审批人">
          <el-input v-model="approveForm.approver" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.opinion" placeholder="默认：同意" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApproveSubmit">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 审批驳回弹窗（考评 / 表彰共用） -->
    <el-dialog v-model="rejectVisible" :title="rejectTitle" width="500px">
      <p>{{ rejectTip }}</p>
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="审批人">
          <el-input v-model="rejectForm.approver" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRejectSubmit">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑投诉弹窗 -->
    <el-dialog v-model="complaintDialogVisible" :title="complaintDialogTitle" width="640px">
      <el-form ref="complaintFormRef" :model="complaintForm" :rules="complaintRules" label-width="110px">
        <el-form-item label="投诉编号" prop="complaintNo">
          <el-input v-model="complaintForm.complaintNo" placeholder="如：YD-TS-2026-001" />
        </el-form-item>
        <el-form-item label="投诉人" prop="complainantName">
          <el-input v-model="complaintForm.complainantName" placeholder="投诉人姓名" />
        </el-form-item>
        <el-form-item label="投诉人电话">
          <el-input v-model="complaintForm.complainantPhone" placeholder="投诉人电话" />
        </el-form-item>
        <el-form-item label="被投诉医师">
          <el-select v-model="complaintForm.physicianId" placeholder="请选择医师（不明时可留空）" clearable style="width: 100%" @change="onComplaintPhysicianChange">
            <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="投诉类型" prop="complaintType">
          <el-select v-model="complaintForm.complaintType" placeholder="请选择投诉类型" style="width: 100%">
            <el-option v-for="t in complaintTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="投诉日期">
          <el-date-picker v-model="complaintForm.complaintDate" type="date" value-format="YYYY-MM-DD" placeholder="投诉日期" style="width: 48%" />
          <span style="margin: 0 8px">处理期限</span>
          <el-date-picker v-model="complaintForm.deadlineDate" type="date" value-format="YYYY-MM-DD" placeholder="默认投诉日期+15天" style="width: 48%" />
        </el-form-item>
        <el-form-item label="投诉内容">
          <el-input v-model="complaintForm.content" type="textarea" :rows="4" placeholder="投诉内容" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="complaintForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleComplaintSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 投诉详情弹窗 -->
    <el-dialog v-model="complaintDetailVisible" :title="`投诉详情：${complaintDetail.complaintNo || ''}`" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="投诉编号">{{ complaintDetail.complaintNo }}</el-descriptions-item>
        <el-descriptions-item label="投诉人">{{ complaintDetail.complainantName }}（{{ complaintDetail.complainantPhone }}）</el-descriptions-item>
        <el-descriptions-item label="被投诉医师">{{ complaintDetail.physicianName }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ complaintDetail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="投诉类型">{{ complaintTypeText(complaintDetail.complaintType) }}</el-descriptions-item>
        <el-descriptions-item label="流转状态"><el-tag :type="complaintTag(complaintDetail.status)">{{ complaintStatusText(complaintDetail.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="投诉日期">{{ complaintDetail.complaintDate }}</el-descriptions-item>
        <el-descriptions-item label="处理期限"><span :class="{ overdue: isOverdue(complaintDetail) }">{{ complaintDetail.deadlineDate }}</span></el-descriptions-item>
        <el-descriptions-item label="投诉内容" :span="2">{{ complaintDetail.content }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ complaintDetail.handler }}</el-descriptions-item>
        <el-descriptions-item label="处理日期">{{ complaintDetail.handleDate }}</el-descriptions-item>
        <el-descriptions-item label="处理结果" :span="2">{{ complaintDetail.handleResult }}</el-descriptions-item>
        <el-descriptions-item label="反馈日期">{{ complaintDetail.feedbackDate }}</el-descriptions-item>
        <el-descriptions-item label="反馈内容" :span="2">{{ complaintDetail.feedbackContent }}</el-descriptions-item>
        <el-descriptions-item label="不予受理原因" :span="2">{{ complaintDetail.rejectReason }}</el-descriptions-item>
        <el-descriptions-item label="流转记录" :span="2">{{ complaintDetail.approvalHistory }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ complaintDetail.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 投诉受理弹窗 -->
    <el-dialog v-model="acceptVisible" title="受理投诉" width="500px">
      <p>受理投诉「{{ acceptRow.complaintNo }}」：受理后进入调查。</p>
      <el-form :model="acceptForm" label-width="100px">
        <el-form-item label="受理人">
          <el-input v-model="acceptForm.handler" placeholder="如：吴文斌" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="acceptVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAcceptSubmit">确认受理</el-button>
      </template>
    </el-dialog>

    <!-- 投诉处理弹窗 -->
    <el-dialog v-model="handleVisible" title="处理投诉" width="500px">
      <p>处理投诉「{{ handleRow.complaintNo }}」：处理后待向投诉人反馈。</p>
      <el-form :model="handleForm" label-width="100px">
        <el-form-item label="处理人">
          <el-input v-model="handleForm.handler" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="handleForm.handleResult" type="textarea" :rows="3" placeholder="请填写处理结果" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="handleHandleSubmit">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 投诉反馈弹窗 -->
    <el-dialog v-model="feedbackVisible" title="反馈投诉" width="500px">
      <p>向投诉人反馈投诉「{{ feedbackRow.complaintNo }}」的处理结果。</p>
      <el-form :model="feedbackForm" label-width="100px">
        <el-form-item label="反馈人">
          <el-input v-model="feedbackForm.handler" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item label="反馈内容">
          <el-input v-model="feedbackForm.feedbackContent" type="textarea" :rows="3" placeholder="请填写反馈内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="handleFeedbackSubmit">确认反馈</el-button>
      </template>
    </el-dialog>

    <!-- 投诉不予受理弹窗 -->
    <el-dialog v-model="complaintRejectVisible" title="不予受理" width="500px">
      <p>投诉「{{ complaintRejectRow.complaintNo }}」不予受理：请填写原因。</p>
      <el-form :model="complaintRejectForm" label-width="100px">
        <el-form-item label="处理人">
          <el-input v-model="complaintRejectForm.approver" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item label="不予受理原因">
          <el-input v-model="complaintRejectForm.reason" type="textarea" :rows="3" placeholder="请填写不予受理原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintRejectVisible = false">取消</el-button>
        <el-button type="primary" @click="handleComplaintRejectSubmit">确认不予受理</el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑表彰弹窗 -->
    <el-dialog v-model="commendDialogVisible" :title="commendDialogTitle" width="640px">
      <el-form ref="commendFormRef" :model="commendForm" :rules="commendRules" label-width="110px">
        <el-form-item label="受表彰医师" prop="physicianId">
          <el-select v-model="commendForm.physicianId" placeholder="请选择医师" style="width: 100%" @change="onCommendPhysicianChange">
            <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="表彰类型" prop="commendType">
          <el-select v-model="commendForm.commendType" placeholder="请选择表彰类型" style="width: 100%">
            <el-option v-for="t in commendTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="表彰级别" prop="commendLevel">
          <el-select v-model="commendForm.commendLevel" placeholder="请选择表彰级别" style="width: 100%">
            <el-option v-for="l in commendLevels" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="表彰标题" prop="title">
          <el-input v-model="commendForm.title" placeholder="如：妙手仁心 德艺双馨" />
        </el-form-item>
        <el-form-item label="表彰事迹">
          <el-input v-model="commendForm.content" type="textarea" :rows="3" placeholder="表彰事迹内容" />
        </el-form-item>
        <el-form-item label="授予方">
          <el-input v-model="commendForm.grantedBy" placeholder="如：患者张某家属 / 医院党委" />
        </el-form-item>
        <el-form-item label="表彰日期">
          <el-date-picker v-model="commendForm.commendDate" type="date" value-format="YYYY-MM-DD" placeholder="表彰日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="commendForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCommendSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEthicsProfiles, getEthicsProfileStats, getEthicsProfileStatsByDept,
  createEthicsProfile, updateEthicsProfile, deleteEthicsProfile,
  batchDeleteEthicsProfiles, exportEthicsProfiles, downloadEthicsProfileTemplate,
  getEthicsTemplates, getEnabledEthicsTemplates,
  createEthicsTemplate, updateEthicsTemplate, deleteEthicsTemplate,
  getEthicsEvaluations, getEthicsEvaluationStats, getEthicsEvaluationStatsByDept,
  createEthicsEvaluation, updateEthicsEvaluation, deleteEthicsEvaluation,
  batchDeleteEthicsEvaluations, approveEthicsEvaluation, rejectEthicsEvaluation,
  exportEthicsEvaluations, downloadEthicsEvaluationTemplate,
  getEthicsComplaints, getEthicsComplaintStats, getEthicsComplaintStatsByDept, getEthicsComplaintById,
  createEthicsComplaint, updateEthicsComplaint, deleteEthicsComplaint,
  batchDeleteEthicsComplaints, acceptEthicsComplaint, handleEthicsComplaint,
  feedbackEthicsComplaint, rejectEthicsComplaint,
  exportEthicsComplaints, downloadEthicsComplaintTemplate,
  getEthicsCommendations, getEthicsCommendationStats, getEthicsCommendationStatsByDept,
  createEthicsCommendation, updateEthicsCommendation, deleteEthicsCommendation,
  batchDeleteEthicsCommendations, approveEthicsCommendation, rejectEthicsCommendation,
  exportEthicsCommendations, downloadEthicsCommendationTemplate
} from '../../api/ethics'
import { getEnabledDepts } from '../../api/system'
import { getPhysicians } from '../../api/physician'

// ---------- 常量 ----------
const grades = [
  { value: 'EXCELLENT', label: '优秀' },
  { value: 'QUALIFIED', label: '合格' },
  { value: 'BASIC', label: '基本合格' },
  { value: 'UNQUALIFIED', label: '不合格' }
]
const gradeText = (v) => grades.find((g) => g.value === v)?.label || v || ''
const gradeTag = (v) => ({ EXCELLENT: 'success', QUALIFIED: '', BASIC: 'warning', UNQUALIFIED: 'danger' }[v] || 'info')
const profileStatuses = [
  { value: 0, label: '在档' },
  { value: 1, label: '封存' }
]
const profileStatusText = (v) => profileStatuses.find((s) => s.value === v)?.label || ''
const approvalStatuses = [
  { value: 0, label: '待审批' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' }
]
const approvalStatusText = (v) => approvalStatuses.find((s) => s.value === v)?.label || ''
const approvalTag = (v) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[v] || 'info')
const complaintTypes = [
  { value: 'ATTITUDE', label: '服务态度' },
  { value: 'QUALITY', label: '医疗质量' },
  { value: 'CHARGE', label: '收费问题' },
  { value: 'INTEGRITY', label: '廉洁行医' },
  { value: 'OTHER', label: '其他' }
]
const complaintTypeText = (v) => complaintTypes.find((t) => t.value === v)?.label || v || ''
const complaintStatuses = [
  { value: 0, label: '待受理' },
  { value: 1, label: '调查中' },
  { value: 2, label: '已处理' },
  { value: 3, label: '已反馈' },
  { value: 4, label: '不予受理' }
]
const complaintStatusText = (v) => complaintStatuses.find((s) => s.value === v)?.label || ''
const complaintTag = (v) => ({ 0: 'warning', 1: '', 2: 'success', 3: 'success', 4: 'info' }[v] || 'info')
const commendTypes = [
  { value: 'BANNER', label: '锦旗' },
  { value: 'LETTER', label: '表扬信' },
  { value: 'NOTICE', label: '通报表扬' },
  { value: 'HONOR', label: '荣誉称号' },
  { value: 'OTHER', label: '其他' }
]
const commendTypeText = (v) => commendTypes.find((t) => t.value === v)?.label || v || ''
const commendLevels = [
  { value: 'HOSPITAL', label: '院级' },
  { value: 'CITY', label: '市级' },
  { value: 'PROVINCE', label: '省级' },
  { value: 'NATIONAL', label: '国家级' }
]
const commendLevelText = (v) => commendLevels.find((l) => l.value === v)?.label || v || ''
const isOverdue = (row) => {
  if (!row.deadlineDate || row.status === 3 || row.status === 4) return false
  return row.deadlineDate < new Date().toISOString().slice(0, 10)
}
const scoreItemsText = (json) => {
  if (!json) return ''
  try {
    const obj = JSON.parse(json)
    return Object.entries(obj).map(([k, v]) => `${k} ${v}`).join('；')
  } catch (e) {
    return ''
  }
}
const parseScoreItems = (json) => {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}
const templateItemsText = (json) => parseScoreItems(json).map((i) => `${i.name}：${i.weight}`).join('；')

// ---------- 公共 ----------
const activeTab = ref('profile')
const depts = ref([])
const physicians = ref([])
const enabledTemplates = ref([])
const profileStats = ref({})
const evalStats = ref({})
const complaintStats = ref({})
const commendStats = ref({})

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function handleImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
    refreshAll()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

async function fetchStats() {
  try {
    profileStats.value = await getEthicsProfileStats()
    evalStats.value = await getEthicsEvaluationStats()
    complaintStats.value = await getEthicsComplaintStats()
    commendStats.value = await getEthicsCommendationStats()
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

function refreshAll() {
  fetchProfiles()
  fetchEvaluations()
  fetchComplaints()
  fetchCommendations()
  fetchStats()
}

// ---------- 医德档案 ----------
const profileQuery = reactive({ keyword: '', deptId: null, status: null, lastGrade: '', page: 1, size: 10 })
const profileData = ref([])
const profileTotal = ref(0)
const profileLoading = ref(false)
const profileSelected = ref([])
const profileDeptStats = ref([])

const profileDialogVisible = ref(false)
const profileDialogTitle = ref('新增档案')
const profileFormRef = ref(null)
const profileForm = reactive({
  id: null, archiveNo: '', physicianId: null, physicianName: '', deptId: null,
  deptName: '', title: '', establishDate: '', status: 0, remark: ''
})
const profileRules = {
  archiveNo: [{ required: true, message: '请填写档案编号', trigger: 'blur' }],
  physicianId: [{ required: true, message: '请选择医师', trigger: 'change' }]
}

async function fetchProfiles() {
  profileLoading.value = true
  try {
    const data = await getEthicsProfiles({ ...profileQuery })
    profileData.value = data.records
    profileTotal.value = data.total
    profileDeptStats.value = await getEthicsProfileStatsByDept()
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    profileLoading.value = false
  }
}

function handleProfileSearch() {
  profileQuery.page = 1
  fetchProfiles()
}

function handleProfileReset() {
  Object.assign(profileQuery, { keyword: '', deptId: null, status: null, lastGrade: '', page: 1, size: 10 })
  fetchProfiles()
}

function resetProfileForm() {
  Object.assign(profileForm, {
    id: null, archiveNo: '', physicianId: null, physicianName: '', deptId: null,
    deptName: '', title: '', establishDate: '', status: 0, remark: ''
  })
}

function onProfilePhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    profileForm.physicianName = p.name
    profileForm.deptId = p.deptId
    profileForm.deptName = p.deptName
    profileForm.title = p.title
  }
}

function handleProfileAdd() {
  resetProfileForm()
  profileDialogTitle.value = '新增档案'
  profileDialogVisible.value = true
}

function handleProfileEdit(row) {
  resetProfileForm()
  Object.assign(profileForm, row)
  profileDialogTitle.value = '编辑档案'
  profileDialogVisible.value = true
}

async function handleProfileSubmit() {
  await profileFormRef.value.validate()
  try {
    if (profileForm.id) {
      await updateEthicsProfile(profileForm.id, { ...profileForm })
      ElMessage.success('档案修改成功')
    } else {
      await createEthicsProfile({ ...profileForm })
      ElMessage.success('档案新增成功')
    }
    profileDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleProfileDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.physicianName}」的医德档案（${row.archiveNo}）吗？`, '提示', { type: 'warning' })
  try {
    await deleteEthicsProfile(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleProfileBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${profileSelected.value.length} 份档案吗？`, '提示', { type: 'warning' })
  try {
    await batchDeleteEthicsProfiles(profileSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

async function handleProfileExport() {
  try {
    const res = await exportEthicsProfiles({ ...profileQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '医德医风档案.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleProfileTemplate() {
  const res = await downloadEthicsProfileTemplate()
  downloadBlob(res.data, '医德医风档案导入模板.xlsx')
}

// ---------- 医德考评 ----------
const evalQuery = reactive({ keyword: '', deptId: null, templateId: null, assessYear: null, grade: '', status: null, page: 1, size: 10 })
const evalData = ref([])
const evalTotal = ref(0)
const evalLoading = ref(false)
const evalSelected = ref([])
const evalDeptStats = ref([])

const evalDialogVisible = ref(false)
const evalDialogTitle = ref('新增考评')
const evalFormRef = ref(null)
const evalForm = reactive({
  id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
  templateId: null, templateName: '', assessYear: new Date().getFullYear(), cycle: '年度',
  assessor: '', assessDate: '', remark: ''
})
const evalRules = {
  physicianId: [{ required: true, message: '请选择被考评医师', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择考评模板', trigger: 'change' }],
  assessYear: [{ required: true, message: '请填写考评年度', trigger: 'blur' }]
}
const evalScoreItems = ref([])
const evalScores = reactive({})

const evalComputedTotal = computed(() => {
  let total = 0
  for (const item of evalScoreItems.value) {
    total += Number(evalScores[item.name] || 0)
  }
  return total
})
const evalComputedGradeText = computed(() => {
  const t = evalComputedTotal.value
  return t >= 90 ? '优秀' : t >= 80 ? '合格' : t >= 60 ? '基本合格' : '不合格'
})

async function fetchEvaluations() {
  evalLoading.value = true
  try {
    const data = await getEthicsEvaluations({ ...evalQuery })
    evalData.value = data.records
    evalTotal.value = data.total
    evalDeptStats.value = await getEthicsEvaluationStatsByDept()
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    evalLoading.value = false
  }
}

function handleEvalSearch() {
  evalQuery.page = 1
  fetchEvaluations()
}

function handleEvalReset() {
  Object.assign(evalQuery, { keyword: '', deptId: null, templateId: null, assessYear: null, grade: '', status: null, page: 1, size: 10 })
  fetchEvaluations()
}

function resetEvalForm() {
  Object.assign(evalForm, {
    id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
    templateId: null, templateName: '', assessYear: new Date().getFullYear(), cycle: '年度',
    assessor: '', assessDate: '', remark: ''
  })
  evalScoreItems.value = []
  Object.keys(evalScores).forEach((k) => delete evalScores[k])
}

function onEvalPhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    evalForm.physicianName = p.name
    evalForm.deptId = p.deptId
    evalForm.deptName = p.deptName
  }
}

function fillEvalScoresFromTemplate(templateId, itemScoresJson) {
  const t = enabledTemplates.value.find((x) => x.id === templateId)
  evalScoreItems.value = t ? parseScoreItems(t.scoreItems) : []
  Object.keys(evalScores).forEach((k) => delete evalScores[k])
  const saved = {}
  try {
    Object.assign(saved, JSON.parse(itemScoresJson || '{}'))
  } catch (e) {
    // 旧数据格式异常时按空处理
  }
  evalScoreItems.value.forEach((item) => {
    evalScores[item.name] = saved[item.name] ?? 0
  })
}

function onEvalTemplateChange(templateId) {
  const t = enabledTemplates.value.find((x) => x.id === templateId)
  if (t) {
    evalForm.templateName = t.templateName
  }
  fillEvalScoresFromTemplate(templateId, null)
}

function handleEvalAdd() {
  resetEvalForm()
  evalDialogTitle.value = '新增考评'
  evalDialogVisible.value = true
}

function handleEvalEdit(row) {
  resetEvalForm()
  Object.assign(evalForm, row)
  fillEvalScoresFromTemplate(row.templateId, row.itemScores)
  evalDialogTitle.value = '编辑考评'
  evalDialogVisible.value = true
}

async function handleEvalSubmit() {
  await evalFormRef.value.validate()
  if (!evalScoreItems.value.length) {
    ElMessage.warning('请先选择考评模板')
    return
  }
  const scores = {}
  evalScoreItems.value.forEach((item) => {
    scores[item.name] = Number(evalScores[item.name] || 0)
  })
  const payload = { ...evalForm, itemScores: JSON.stringify(scores) }
  try {
    if (evalForm.id) {
      await updateEthicsEvaluation(evalForm.id, payload)
      ElMessage.success('考评修改成功')
    } else {
      await createEthicsEvaluation(payload)
      ElMessage.success('考评新增成功')
    }
    evalDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleEvalDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.physicianName}」${row.assessYear} 年的医德考评记录吗？`, '提示', { type: 'warning' })
  try {
    await deleteEthicsEvaluation(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleEvalBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${evalSelected.value.length} 条考评记录吗？已通过的考评不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteEthicsEvaluations(evalSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

async function handleEvalExport() {
  try {
    const res = await exportEthicsEvaluations({ ...evalQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '医德考评.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleEvalTemplate() {
  const res = await downloadEthicsEvaluationTemplate()
  downloadBlob(res.data, '医德考评导入模板.xlsx')
}

// ---------- 考评模板管理 ----------
const templateVisible = ref(false)
const templates = ref([])
const templateFormVisible = ref(false)
const templateFormTitle = ref('新增模板')
const templateForm = reactive({ id: null, templateName: '', status: 1, remark: '' })
const templateItems = ref([{ name: '', weight: null }])
const templateWeightSum = computed(() => templateItems.value.reduce((s, i) => s + Number(i.weight || 0), 0))

async function fetchTemplates() {
  templates.value = await getEthicsTemplates({ page: 1, size: 100 }).then((d) => d.records)
  enabledTemplates.value = await getEnabledEthicsTemplates()
}

async function handleTemplateManage() {
  await fetchTemplates()
  templateVisible.value = true
}

function resetTemplateForm() {
  Object.assign(templateForm, { id: null, templateName: '', status: 1, remark: '' })
  templateItems.value = [{ name: '', weight: null }]
}

function handleTemplateAdd() {
  resetTemplateForm()
  templateFormTitle.value = '新增模板'
  templateFormVisible.value = true
}

function handleTemplateEdit(row) {
  resetTemplateForm()
  Object.assign(templateForm, row)
  templateItems.value = parseScoreItems(row.scoreItems)
  templateFormTitle.value = '编辑模板'
  templateFormVisible.value = true
}

async function handleTemplateToggle(row) {
  try {
    await updateEthicsTemplate(row.id, { ...row, status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '模板已停用' : '模板已启用')
    fetchTemplates()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}

async function handleTemplateDelete(row) {
  await ElMessageBox.confirm(`确定删除模板「${row.templateName}」吗？`, '提示', { type: 'warning' })
  try {
    await deleteEthicsTemplate(row.id)
    ElMessage.success('删除成功')
    fetchTemplates()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleTemplateSubmit() {
  if (!templateForm.templateName) {
    ElMessage.warning('请填写模板名称')
    return
  }
  if (templateWeightSum.value !== 100) {
    ElMessage.warning(`评分项权重之和必须为 100，当前为 ${templateWeightSum.value}`)
    return
  }
  const payload = { ...templateForm, scoreItems: JSON.stringify(templateItems.value) }
  try {
    if (templateForm.id) {
      await updateEthicsTemplate(templateForm.id, payload)
      ElMessage.success('模板修改成功')
    } else {
      await createEthicsTemplate(payload)
      ElMessage.success('模板新增成功')
    }
    templateFormVisible.value = false
    fetchTemplates()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

// ---------- 审批通过 / 驳回（考评 / 表彰共用） ----------
const approveVisible = ref(false)
const approveKind = ref('evaluation')
const approveRow = ref({})
const approveForm = reactive({ approver: '', opinion: '' })
const approveTitle = computed(() => (approveKind.value === 'evaluation' ? '通过医德考评' : '通过表彰'))
const approveTip = computed(() => {
  if (approveKind.value === 'evaluation') {
    return `通过「${approveRow.value.physicianName}」${approveRow.value.assessYear} 年的医德考评（${approveRow.value.totalScore} 分，${gradeText(approveRow.value.grade)}）：通过后同步更新其医德档案的最近考评结果。`
  }
  return `通过表彰「${approveRow.value.title}」（${approveRow.value.physicianName}）：通过后为正式表彰记录。`
})

const rejectVisible = ref(false)
const rejectKind = ref('evaluation')
const rejectRow = ref({})
const rejectForm = reactive({ approver: '', reason: '' })
const rejectTitle = computed(() => (rejectKind.value === 'evaluation' ? '驳回医德考评' : '驳回表彰'))
const rejectTip = computed(() => {
  if (rejectKind.value === 'evaluation') {
    return `驳回「${rejectRow.value.physicianName}」${rejectRow.value.assessYear} 年的医德考评（${rejectRow.value.totalScore} 分）：请填写驳回原因。`
  }
  return `驳回表彰「${rejectRow.value.title}」（${rejectRow.value.physicianName}）：请填写驳回原因。`
})

function handleEvalApprove(row) {
  approveKind.value = 'evaluation'
  approveRow.value = row
  Object.assign(approveForm, { approver: '', opinion: '' })
  approveVisible.value = true
}

function handleCommendApprove(row) {
  approveKind.value = 'commendation'
  approveRow.value = row
  Object.assign(approveForm, { approver: '', opinion: '' })
  approveVisible.value = true
}

async function handleApproveSubmit() {
  if (!approveForm.approver) {
    ElMessage.warning('请填写审批人')
    return
  }
  try {
    if (approveKind.value === 'evaluation') {
      await approveEthicsEvaluation(approveRow.value.id, { ...approveForm })
      ElMessage.success('考评已通过，档案最近考评结果已同步')
    } else {
      await approveEthicsCommendation(approveRow.value.id, { ...approveForm })
      ElMessage.success('表彰已通过')
    }
    approveVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

function handleEvalReject(row) {
  rejectKind.value = 'evaluation'
  rejectRow.value = row
  Object.assign(rejectForm, { approver: '', reason: '' })
  rejectVisible.value = true
}

function handleCommendReject(row) {
  rejectKind.value = 'commendation'
  rejectRow.value = row
  Object.assign(rejectForm, { approver: '', reason: '' })
  rejectVisible.value = true
}

async function handleRejectSubmit() {
  if (!rejectForm.approver) {
    ElMessage.warning('请填写审批人')
    return
  }
  if (!rejectForm.reason) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    if (rejectKind.value === 'evaluation') {
      await rejectEthicsEvaluation(rejectRow.value.id, { ...rejectForm })
      ElMessage.success('考评已驳回')
    } else {
      await rejectEthicsCommendation(rejectRow.value.id, { ...rejectForm })
      ElMessage.success('表彰已驳回')
    }
    rejectVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('驳回失败：' + e.message)
  }
}

// ---------- 医德医风投诉 ----------
const complaintQuery = reactive({ keyword: '', complaintType: '', status: null, overdue: false, page: 1, size: 10 })
const complaintData = ref([])
const complaintTotal = ref(0)
const complaintLoading = ref(false)
const complaintSelected = ref([])
const complaintDeptStats = ref([])
const overdueList = ref([])

const complaintDialogVisible = ref(false)
const complaintDialogTitle = ref('新增投诉')
const complaintFormRef = ref(null)
const complaintForm = reactive({
  id: null, complaintNo: '', complainantName: '', complainantPhone: '', physicianId: null,
  physicianName: '', deptName: '', complaintType: 'ATTITUDE', complaintDate: '', deadlineDate: '',
  content: '', remark: ''
})
const complaintRules = {
  complaintNo: [{ required: true, message: '请填写投诉编号', trigger: 'blur' }],
  complainantName: [{ required: true, message: '请填写投诉人姓名', trigger: 'blur' }],
  complaintType: [{ required: true, message: '请选择投诉类型', trigger: 'change' }]
}

const complaintDetailVisible = ref(false)
const complaintDetail = ref({})

const acceptVisible = ref(false)
const acceptRow = ref({})
const acceptForm = reactive({ handler: '' })

const handleVisible = ref(false)
const handleRow = ref({})
const handleForm = reactive({ handler: '', handleResult: '' })

const feedbackVisible = ref(false)
const feedbackRow = ref({})
const feedbackForm = reactive({ handler: '', feedbackContent: '' })

const complaintRejectVisible = ref(false)
const complaintRejectRow = ref({})
const complaintRejectForm = reactive({ approver: '', reason: '' })

async function fetchComplaints() {
  complaintLoading.value = true
  try {
    const data = await getEthicsComplaints({ ...complaintQuery })
    complaintData.value = data.records
    complaintTotal.value = data.total
    complaintDeptStats.value = await getEthicsComplaintStatsByDept()
    overdueList.value = await getEthicsComplaints({ overdue: true, page: 1, size: 100 }).then((d) => d.records || d)
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    complaintLoading.value = false
  }
}

function handleComplaintSearch() {
  complaintQuery.page = 1
  fetchComplaints()
}

function handleComplaintReset() {
  Object.assign(complaintQuery, { keyword: '', complaintType: '', status: null, overdue: false, page: 1, size: 10 })
  fetchComplaints()
}

function resetComplaintForm() {
  Object.assign(complaintForm, {
    id: null, complaintNo: '', complainantName: '', complainantPhone: '', physicianId: null,
    physicianName: '', deptName: '', complaintType: 'ATTITUDE', complaintDate: '', deadlineDate: '',
    content: '', remark: ''
  })
}

function onComplaintPhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    complaintForm.physicianName = p.name
    complaintForm.deptName = p.deptName
  } else {
    complaintForm.physicianName = ''
    complaintForm.deptName = ''
  }
}

function handleComplaintAdd() {
  resetComplaintForm()
  complaintDialogTitle.value = '新增投诉'
  complaintDialogVisible.value = true
}

function handleComplaintEdit(row) {
  resetComplaintForm()
  Object.assign(complaintForm, row)
  complaintDialogTitle.value = '编辑投诉'
  complaintDialogVisible.value = true
}

async function handleComplaintSubmit() {
  await complaintFormRef.value.validate()
  try {
    if (complaintForm.id) {
      await updateEthicsComplaint(complaintForm.id, { ...complaintForm })
      ElMessage.success('投诉修改成功')
    } else {
      await createEthicsComplaint({ ...complaintForm })
      ElMessage.success('投诉登记成功')
    }
    complaintDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleComplaintDetail(row) {
  try {
    complaintDetail.value = await getEthicsComplaintById(row.id)
    complaintDetailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

async function handleComplaintDelete(row) {
  await ElMessageBox.confirm(`确定删除投诉「${row.complaintNo}」吗？`, '提示', { type: 'warning' })
  try {
    await deleteEthicsComplaint(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleComplaintBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${complaintSelected.value.length} 条投诉吗？`, '提示', { type: 'warning' })
  try {
    await batchDeleteEthicsComplaints(complaintSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

function handleComplaintAccept(row) {
  acceptRow.value = row
  Object.assign(acceptForm, { handler: '' })
  acceptVisible.value = true
}

async function handleAcceptSubmit() {
  try {
    await acceptEthicsComplaint(acceptRow.value.id, { handler: acceptForm.handler })
    ElMessage.success('投诉已受理，进入调查')
    acceptVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('受理失败：' + e.message)
  }
}

function handleComplaintHandle(row) {
  handleRow.value = row
  Object.assign(handleForm, { handler: row.handler || '', handleResult: '' })
  handleVisible.value = true
}

async function handleHandleSubmit() {
  if (!handleForm.handleResult) {
    ElMessage.warning('请填写处理结果')
    return
  }
  try {
    await handleEthicsComplaint(handleRow.value.id, { ...handleForm })
    ElMessage.success('投诉已处理，待向投诉人反馈')
    handleVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('处理失败：' + e.message)
  }
}

function handleComplaintFeedback(row) {
  feedbackRow.value = row
  Object.assign(feedbackForm, { handler: row.handler || '', feedbackContent: '' })
  feedbackVisible.value = true
}

async function handleFeedbackSubmit() {
  if (!feedbackForm.feedbackContent) {
    ElMessage.warning('请填写反馈内容')
    return
  }
  try {
    await feedbackEthicsComplaint(feedbackRow.value.id, { ...feedbackForm })
    ElMessage.success('已向投诉人反馈')
    feedbackVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('反馈失败：' + e.message)
  }
}

function handleComplaintReject(row) {
  complaintRejectRow.value = row
  Object.assign(complaintRejectForm, { approver: '', reason: '' })
  complaintRejectVisible.value = true
}

async function handleComplaintRejectSubmit() {
  if (!complaintRejectForm.reason) {
    ElMessage.warning('请填写不予受理原因')
    return
  }
  try {
    await rejectEthicsComplaint(complaintRejectRow.value.id, { ...complaintRejectForm })
    ElMessage.success('投诉已标记为不予受理')
    complaintRejectVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}

async function handleComplaintExport() {
  try {
    const res = await exportEthicsComplaints({ ...complaintQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '医德医风投诉.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleComplaintTemplate() {
  const res = await downloadEthicsComplaintTemplate()
  downloadBlob(res.data, '医德医风投诉导入模板.xlsx')
}

// ---------- 医德医风表彰 ----------
const commendQuery = reactive({ keyword: '', deptId: null, commendType: '', commendLevel: '', status: null, page: 1, size: 10 })
const commendData = ref([])
const commendTotal = ref(0)
const commendLoading = ref(false)
const commendSelected = ref([])
const commendDeptStats = ref([])

const commendDialogVisible = ref(false)
const commendDialogTitle = ref('新增表彰')
const commendFormRef = ref(null)
const commendForm = reactive({
  id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
  commendType: 'BANNER', commendLevel: 'HOSPITAL', title: '', content: '',
  grantedBy: '', commendDate: '', remark: ''
})
const commendRules = {
  physicianId: [{ required: true, message: '请选择受表彰医师', trigger: 'change' }],
  commendType: [{ required: true, message: '请选择表彰类型', trigger: 'change' }],
  commendLevel: [{ required: true, message: '请选择表彰级别', trigger: 'change' }],
  title: [{ required: true, message: '请填写表彰标题', trigger: 'blur' }]
}

async function fetchCommendations() {
  commendLoading.value = true
  try {
    const data = await getEthicsCommendations({ ...commendQuery })
    commendData.value = data.records
    commendTotal.value = data.total
    commendDeptStats.value = await getEthicsCommendationStatsByDept()
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    commendLoading.value = false
  }
}

function handleCommendSearch() {
  commendQuery.page = 1
  fetchCommendations()
}

function handleCommendReset() {
  Object.assign(commendQuery, { keyword: '', deptId: null, commendType: '', commendLevel: '', status: null, page: 1, size: 10 })
  fetchCommendations()
}

function resetCommendForm() {
  Object.assign(commendForm, {
    id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
    commendType: 'BANNER', commendLevel: 'HOSPITAL', title: '', content: '',
    grantedBy: '', commendDate: '', remark: ''
  })
}

function onCommendPhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    commendForm.physicianName = p.name
    commendForm.deptId = p.deptId
    commendForm.deptName = p.deptName
  }
}

function handleCommendAdd() {
  resetCommendForm()
  commendDialogTitle.value = '新增表彰'
  commendDialogVisible.value = true
}

function handleCommendEdit(row) {
  resetCommendForm()
  Object.assign(commendForm, row)
  commendDialogTitle.value = '编辑表彰'
  commendDialogVisible.value = true
}

async function handleCommendSubmit() {
  await commendFormRef.value.validate()
  try {
    if (commendForm.id) {
      await updateEthicsCommendation(commendForm.id, { ...commendForm })
      ElMessage.success('表彰修改成功')
    } else {
      await createEthicsCommendation({ ...commendForm })
      ElMessage.success('表彰登记成功')
    }
    commendDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleCommendDelete(row) {
  await ElMessageBox.confirm(`确定删除表彰「${row.title}」（${row.physicianName}）吗？`, '提示', { type: 'warning' })
  try {
    await deleteEthicsCommendation(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleCommendBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${commendSelected.value.length} 条表彰吗？已通过的表彰不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteEthicsCommendations(commendSelected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

async function handleCommendExport() {
  try {
    const res = await exportEthicsCommendations({ ...commendQuery, page: 1, size: 10000 })
    downloadBlob(res.data, '医德医风表彰.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleCommendTemplate() {
  const res = await downloadEthicsCommendationTemplate()
  downloadBlob(res.data, '医德医风表彰导入模板.xlsx')
}

onMounted(async () => {
  try {
    depts.value = await getEnabledDepts()
    physicians.value = await getPhysicians({ page: 1, size: 100 }).then((d) => d.records)
    enabledTemplates.value = await getEnabledEthicsTemplates()
  } catch (e) {
    console.warn('下拉数据加载失败', e.message)
  }
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
  margin-top: 12px;
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
.overdue {
  color: #f56c6c;
  font-weight: bold;
}
</style>
