<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">项目总数</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ stats.pending || 0 }}</div><div class="label">待审批</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num ok">{{ stats.approved || 0 }}</div><div class="label">已立项</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num bad">{{ stats.ethicsPending || 0 }}</div><div class="label">待伦理审查</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="项目名称 / 申报人 / 负责人" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="申报科室">
          <el-select v-model="query.deptId" placeholder="全部科室" clearable style="width: 140px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="query.projectType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in projectTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="伦理审查">
          <el-select v-model="query.ethicsStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="e in ethicsStatuses" :key="e.value" :label="e.label" :value="e.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="申报日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作区 + 列表 -->
    <el-card style="margin-top: 16px">
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="handleAdd">新增申报</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/new-techs/import"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-success="handleImportSuccess"
            :on-error="handleImportError"
            style="display: inline-block; margin: 0 8px"
          >
            <el-button type="success">批量导入</el-button>
          </el-upload>
          <el-button type="warning" @click="handleExport">导出 Excel</el-button>
          <el-button @click="handleDownloadTemplate">下载模板</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="selected = $event" style="margin-top: 12px">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="projectName" label="项目名称" min-width="200" />
        <el-table-column prop="projectType" label="项目类型" width="90">
          <template #default="{ row }">{{ typeText(row.projectType) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="申报科室" width="110" />
        <el-table-column prop="applicantName" label="申报人" width="90" />
        <el-table-column prop="projectLeader" label="项目负责人" width="100" />
        <el-table-column prop="budget" label="预算（万元）" width="110" />
        <el-table-column prop="startDate" label="计划起止" width="200">
          <template #default="{ row }">{{ row.startDate || '' }} 至 {{ row.endDate || '' }}</template>
        </el-table-column>
        <el-table-column prop="ethicsStatus" label="伦理审查" width="100">
          <template #default="{ row }">
            <el-tag :type="ethicsTag(row.ethicsStatus)">{{ ethicsText(row.ethicsStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="审批状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情/记录</el-button>
            <el-button v-if="row.ethicsRequired && row.ethicsStatus === 1 && row.status === 0" link type="success" @click="handleEthics(row)">伦理审查</el-button>
            <el-button v-if="row.status === 0" link type="warning" @click="handleApprove(row)">审批</el-button>
            <el-button v-if="row.status === 0" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <!-- 按项目类型统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按项目类型统计</span></template>
      <el-table :data="typeStats" border stripe>
        <el-table-column prop="label" label="项目类型" />
        <el-table-column prop="count" label="项目数" />
      </el-table>
    </el-card>

    <!-- 按科室统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按科室统计</span></template>
      <el-table :data="deptStats" border stripe>
        <el-table-column prop="deptName" label="科室" />
        <el-table-column prop="count" label="项目数" />
        <el-table-column prop="pendingCount" label="待审批" />
        <el-table-column prop="approvedCount" label="已立项" />
        <el-table-column prop="acceptedCount" label="已验收" />
        <el-table-column prop="promotedCount" label="已推广" />
      </el-table>
    </el-card>

    <!-- 按月趋势 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按月趋势</span></template>
      <el-table :data="monthStats" border stripe>
        <el-table-column prop="month" label="申报月份" />
        <el-table-column prop="count" label="申报数" />
        <el-table-column prop="approvedCount" label="已立项（含验收/推广）" />
        <el-table-column prop="pendingCount" label="待审批" />
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="820px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="项目名称" prop="projectName"><el-input v-model="form.projectName" placeholder="项目名称" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类型" prop="projectType">
              <el-select v-model="form.projectType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in projectTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申报人" prop="applicantId">
              <el-select v-model="form.applicantId" placeholder="请选择医师" style="width: 100%" @change="onPhysicianChange">
                <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目负责人"><el-input v-model="form.projectLeader" placeholder="项目负责人姓名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预算（万元）"><el-input-number v-model="form.budget" :min="0" :precision="2" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目组成员"><el-input v-model="form.teamMembers" placeholder="多个成员用顿号分隔" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划开始日期">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束日期">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需伦理审查">
              <el-switch v-model="form.ethicsRequired" active-text="是" inactive-text="否" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="立项依据"><el-input v-model="form.background" type="textarea" :rows="2" placeholder="项目背景与立项依据" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目目标"><el-input v-model="form.objective" type="textarea" :rows="2" placeholder="项目目标" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技术方案"><el-input v-model="form.techPlan" type="textarea" :rows="3" placeholder="技术方案 / 实施方案" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="预期效益"><el-input v-model="form.expectedBenefit" type="textarea" :rows="2" placeholder="预期效益" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert title="提交后进入待审批（一级审批：医务科）；需伦理审查的项目须伦理通过后才能立项。立项后可添加进展/验收/推广记录：验收记录使项目变为已验收，推广记录使项目变为已推广。已结束申报阶段的项目不能再修改、再审批。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 / 进展记录弹窗 -->
    <el-dialog title="新技术、新项目详情与进展记录" v-model="detailVisible" width="900px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="项目名称" :span="2">{{ detail.project?.projectName }}</el-descriptions-item>
        <el-descriptions-item label="项目类型">{{ typeText(detail.project?.projectType) }}</el-descriptions-item>
        <el-descriptions-item label="申报科室">{{ detail.project?.deptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="申报人">{{ detail.project?.applicantName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="项目负责人">{{ detail.project?.projectLeader || '—' }}</el-descriptions-item>
        <el-descriptions-item label="项目组成员" :span="2">{{ detail.project?.teamMembers || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预算">{{ detail.project?.budget ?? '—' }} 万元</el-descriptions-item>
        <el-descriptions-item label="计划起止">{{ detail.project?.startDate || '' }} 至 {{ detail.project?.endDate || '' }}</el-descriptions-item>
        <el-descriptions-item label="立项依据" :span="2">{{ detail.project?.background || '—' }}</el-descriptions-item>
        <el-descriptions-item label="项目目标" :span="2">{{ detail.project?.objective || '—' }}</el-descriptions-item>
        <el-descriptions-item label="技术方案" :span="2">{{ detail.project?.techPlan || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预期效益" :span="2">{{ detail.project?.expectedBenefit || '—' }}</el-descriptions-item>
        <el-descriptions-item label="伦理审查">
          <el-tag :type="ethicsTag(detail.project?.ethicsStatus)">{{ ethicsText(detail.project?.ethicsStatus) }}</el-tag>
          <span v-if="detail.project?.ethicsReviewer">（{{ detail.project.ethicsReviewer }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="伦理审查意见">{{ detail.project?.ethicsOpinion || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">
          <el-tag :type="statusTag(detail.project?.status)">{{ statusText(detail.project?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.project?.approver || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批记录" :span="2">{{ detail.project?.approvalHistory || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin: 16px 0 8px; display: flex; justify-content: space-between; align-items: center">
        <strong>进展记录（{{ (detail.records || []).length }} 条，可多次补充）</strong>
        <el-button v-if="detail.project?.status === 1 || detail.project?.status === 2" type="primary" size="small" @click="recordFormVisible = true">添加进展记录</el-button>
      </div>
      <el-table :data="detail.records || []" border stripe>
        <el-table-column prop="recordType" label="记录类型" width="110">
          <template #default="{ row }">{{ recordTypeText(row.recordType) }}</template>
        </el-table-column>
        <el-table-column prop="recordDate" label="记录日期" width="120" />
        <el-table-column prop="content" label="记录内容" />
        <el-table-column prop="conclusion" label="结论" />
        <el-table-column prop="recorder" label="记录人" width="90" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDeleteRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(detail.records || []).length" description="暂无进展记录：立项后可添加进展/验收/推广记录" />

      <!-- 添加进展记录表单 -->
      <el-form :model="recordForm" :rules="recordRules" ref="recordFormRef" label-width="110px" style="margin-top: 16px" v-if="recordFormVisible">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="记录类型" prop="recordType">
              <el-select v-model="recordForm.recordType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in recordTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录日期" prop="recordDate">
              <el-date-picker v-model="recordForm.recordDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="记录内容" prop="content"><el-input v-model="recordForm.content" type="textarea" :rows="3" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="结论"><el-input v-model="recordForm.conclusion" type="textarea" :rows="2" placeholder="验收结论 / 推广效果等" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录人"><el-input v-model="recordForm.recorder" placeholder="记录人姓名" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert title="验收记录只允许已立项的项目添加，添加后项目变为已验收；推广记录只允许已验收的项目添加，添加后项目变为已推广；进展记录只允许已立项/已验收的项目添加。" type="info" :closable="false" show-icon />
        <div style="margin-top: 12px">
          <el-button type="primary" @click="handleRecordSubmit">保存记录</el-button>
          <el-button @click="recordFormVisible = false">取消</el-button>
        </div>
      </el-form>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog title="新技术、新项目审批（医务科）" v-model="approveVisible" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="项目名称">{{ approveRow.projectName }}</el-descriptions-item>
        <el-descriptions-item label="项目类型">{{ typeText(approveRow.projectType) }} · {{ ethicsText(approveRow.ethicsStatus) }}</el-descriptions-item>
        <el-descriptions-item label="申报科室/申报人">{{ approveRow.deptName }} / {{ approveRow.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="预算">{{ approveRow.budget ?? '—' }} 万元</el-descriptions-item>
        <el-descriptions-item label="项目目标">{{ approveRow.objective || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert v-if="approveRow.ethicsRequired && approveRow.ethicsStatus !== 2" title="该项目需伦理审查通过后才能立项，当前伦理审查状态不是已通过。" type="warning" :closable="false" show-icon style="margin-top: 12px" />
      <el-form :model="approveForm" label-width="90px" style="margin-top: 16px">
        <el-form-item label="审批人">
          <el-input v-model="approveForm.approver" placeholder="医务科姓名" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="2" placeholder="审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">驳回</el-button>
        <el-button type="primary" @click="handleApproveSubmit">通过（立项）</el-button>
      </template>
    </el-dialog>

    <!-- 伦理审查弹窗 -->
    <el-dialog title="伦理审查" v-model="ethicsVisible" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="项目名称">{{ ethicsRow.projectName }}</el-descriptions-item>
        <el-descriptions-item label="申报科室/申报人">{{ ethicsRow.deptName }} / {{ ethicsRow.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="项目目标">{{ ethicsRow.objective || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="ethicsForm" label-width="90px" style="margin-top: 16px">
        <el-form-item label="审查人">
          <el-input v-model="ethicsForm.reviewer" placeholder="伦理委员会 / 审查人姓名" />
        </el-form-item>
        <el-form-item label="审查意见">
          <el-input v-model="ethicsForm.opinion" type="textarea" :rows="2" placeholder="伦理审查意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ethicsVisible = false">取消</el-button>
        <el-button type="danger" @click="handleEthicsSubmit(false)">未通过</el-button>
        <el-button type="primary" @click="handleEthicsSubmit(true)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getNewTechs, getNewTechStats, getNewTechStatsByDept, getNewTechStatsByMonth, getNewTech,
  createNewTech, updateNewTech, deleteNewTech, batchDeleteNewTechs,
  approveNewTech, rejectNewTech, ethicsReviewNewTech, addNewTechRecord, deleteNewTechRecord,
  exportNewTechs, downloadNewTechTemplate
} from '../../api/newtech'
import { getEnabledDepts } from '../../api/system'
import { getPhysicians } from '../../api/physician'

const projectTypes = [
  { value: 'NEW_TECH', label: '新技术' },
  { value: 'NEW_PROJECT', label: '新项目' }
]
const statuses = [
  { value: 0, label: '待审批' },
  { value: 1, label: '已立项' },
  { value: 2, label: '已验收' },
  { value: 3, label: '已驳回' },
  { value: 4, label: '已推广' }
]
const ethicsStatuses = [
  { value: 0, label: '不需审查' },
  { value: 1, label: '待审查' },
  { value: 2, label: '已通过' },
  { value: 3, label: '未通过' }
]
const recordTypes = [
  { value: 'PROGRESS', label: '进展记录' },
  { value: 'ACCEPTANCE', label: '验收记录' },
  { value: 'PROMOTION', label: '推广记录' }
]
const typeText = (v) => projectTypes.find((t) => t.value === v)?.label || v || ''
const statusText = (v) => statuses.find((s) => s.value === v)?.label || ''
const statusTag = (v) => ({ 0: 'warning', 1: '', 2: 'success', 3: 'danger', 4: 'success' }[v] || 'info')
const ethicsText = (v) => ethicsStatuses.find((e) => e.value === v)?.label || ''
const ethicsTag = (v) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[v] || 'info')
const recordTypeText = (v) => recordTypes.find((t) => t.value === v)?.label || v || ''

const query = reactive({ keyword: '', deptId: null, projectType: '', status: null, ethicsStatus: null, startDate: null, endDate: null, page: 1, size: 10 })
const dateRange = ref(null)
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const depts = ref([])
const physicians = ref([])
const stats = ref({})
const deptStats = ref([])
const monthStats = ref([])
const typeStats = computed(() =>
  projectTypes.map((t) => ({ label: t.label, count: stats.value.byType?.[t.value] || 0 }))
)

const dialogVisible = ref(false)
const dialogTitle = ref('新增申报')
const formRef = ref(null)
const form = reactive({
  id: null, projectName: '', projectType: 'NEW_TECH', deptId: null, deptName: '',
  applicantId: null, applicantName: '', projectLeader: '', teamMembers: '',
  background: '', objective: '', techPlan: '', expectedBenefit: '',
  budget: null, startDate: '', endDate: '', ethicsRequired: false, remark: ''
})
const rules = {
  projectName: [{ required: true, message: '请填写项目名称', trigger: 'blur' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  applicantId: [{ required: true, message: '请选择申报人', trigger: 'change' }]
}

const detailVisible = ref(false)
const detail = ref({ project: null, records: [] })
const recordFormVisible = ref(false)
const recordFormRef = ref(null)
const recordForm = reactive({ recordType: 'PROGRESS', recordDate: '', content: '', conclusion: '', recorder: '' })
const recordRules = {
  recordType: [{ required: true, message: '请选择记录类型', trigger: 'change' }],
  content: [{ required: true, message: '请填写记录内容', trigger: 'blur' }]
}

const approveVisible = ref(false)
const approveRow = ref({})
const approveForm = reactive({ approver: '吴文斌', opinion: '' })

const ethicsVisible = ref(false)
const ethicsRow = ref({})
const ethicsForm = reactive({ reviewer: '伦理委员会', opinion: '' })

async function fetchList() {
  loading.value = true
  try {
    const data = await getNewTechs({ ...query })
    tableData.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    stats.value = await getNewTechStats()
    deptStats.value = await getNewTechStatsByDept()
    monthStats.value = await getNewTechStatsByMonth()
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

function refresh() {
  fetchList()
  fetchStats()
}

function handleSearch() {
  query.startDate = dateRange.value ? dateRange.value[0] : null
  query.endDate = dateRange.value ? dateRange.value[1] : null
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { keyword: '', deptId: null, projectType: '', status: null, ethicsStatus: null, startDate: null, endDate: null, page: 1, size: 10 })
  dateRange.value = null
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, projectName: '', projectType: 'NEW_TECH', deptId: null, deptName: '',
    applicantId: null, applicantName: '', projectLeader: '', teamMembers: '',
    background: '', objective: '', techPlan: '', expectedBenefit: '',
    budget: null, startDate: '', endDate: '', ethicsRequired: false, remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增申报'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑申报'
  dialogVisible.value = true
}

function onPhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    form.applicantName = p.name
    form.projectLeader = form.projectLeader || p.name
    form.deptId = p.deptId
    form.deptName = p.deptName
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateNewTech(form.id, { ...form })
      ElMessage.success('申报修改成功')
    } else {
      await createNewTech({ ...form })
      ElMessage.success('申报新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除项目「${row.projectName}」吗？`, '提示', { type: 'warning' })
  await deleteNewTech(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 个项目吗？`, '提示', { type: 'warning' })
  await batchDeleteNewTechs(selected.value.map((r) => r.id))
  ElMessage.success('批量删除成功')
  refresh()
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function handleExport() {
  try {
    const res = await exportNewTechs({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '新技术新项目.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadNewTechTemplate()
  downloadBlob(res.data, '新技术新项目导入模板.xlsx')
}

function handleImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
    refresh()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---- 详情 / 进展记录 ----
async function handleDetail(row) {
  try {
    detail.value = await getNewTech(row.id)
    recordFormVisible.value = false
    Object.assign(recordForm, { recordType: 'PROGRESS', recordDate: '', content: '', conclusion: '', recorder: '' })
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

async function handleRecordSubmit() {
  await recordFormRef.value.validate()
  try {
    await addNewTechRecord(detail.value.project.id, { ...recordForm })
    ElMessage.success('进展记录添加成功')
    recordFormVisible.value = false
    detail.value = await getNewTech(detail.value.project.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleDeleteRecord(row) {
  await ElMessageBox.confirm('确定删除这条进展记录吗？', '提示', { type: 'warning' })
  await deleteNewTechRecord(detail.value.project.id, row.id)
  ElMessage.success('删除成功')
  detail.value = await getNewTech(detail.value.project.id)
}

// ---- 审批 ----
function handleApprove(row) {
  approveRow.value = row
  Object.assign(approveForm, { approver: '吴文斌', opinion: '' })
  approveVisible.value = true
}

async function handleApproveSubmit() {
  try {
    await approveNewTech(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('审批通过，已立项')
    approveVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

async function handleReject() {
  try {
    await rejectNewTech(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('已驳回')
    approveVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('驳回失败：' + e.message)
  }
}

// ---- 伦理审查 ----
function handleEthics(row) {
  ethicsRow.value = row
  Object.assign(ethicsForm, { reviewer: '伦理委员会', opinion: '' })
  ethicsVisible.value = true
}

async function handleEthicsSubmit(passed) {
  try {
    await ethicsReviewNewTech(ethicsRow.value.id, { passed, reviewer: ethicsForm.reviewer, opinion: ethicsForm.opinion })
    ElMessage.success(passed ? '伦理审查通过' : '伦理审查未通过')
    ethicsVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('伦理审查失败：' + e.message)
  }
}

onMounted(async () => {
  try {
    depts.value = await getEnabledDepts()
    const page = await getPhysicians({ page: 1, size: 100 })
    physicians.value = page.records || []
  } catch (e) {
    console.warn('基础数据加载失败（后端未启动？）', e.message)
  }
  refresh()
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
</style>
