<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">MDT 申请总数</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ stats.pending || 0 }}</div><div class="label">待审批</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num ok">{{ stats.approved || 0 }}</div><div class="label">已通过</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num bad">{{ stats.urgentCount || 0 }}</div><div class="label">紧急会诊</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="患者 / 住院号 / 申请医师" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="申请科室">
          <el-select v-model="query.deptId" placeholder="全部科室" clearable style="width: 140px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="会诊类型">
          <el-select v-model="query.mdtType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in mdtTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="query.urgency" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="u in urgencies" :key="u.value" :label="u.label" :value="u.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望会诊日期">
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
          <el-button type="primary" @click="handleAdd">新增 MDT 申请</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/mdts/import"
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
        <el-table-column prop="patientName" label="患者" width="90" />
        <el-table-column prop="inpatientNo" label="住院号" width="120" />
        <el-table-column prop="mdtType" label="会诊类型" width="100">
          <template #default="{ row }">{{ typeText(row.mdtType) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="申请科室" width="110" />
        <el-table-column prop="applyPhysicianName" label="申请医师" width="90" />
        <el-table-column prop="expectedTime" label="期望会诊时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.expectedTime) }}</template>
        </el-table-column>
        <el-table-column prop="urgency" label="紧急程度" width="90">
          <template #default="{ row }">
            <el-tag :type="row.urgency === 'URGENT' ? 'danger' : ''">{{ urgencyText(row.urgency) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="审批状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情/记录</el-button>
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

    <!-- 按会诊类型统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按会诊类型统计</span></template>
      <el-table :data="typeStats" border stripe>
        <el-table-column prop="label" label="会诊类型" />
        <el-table-column prop="count" label="申请数" />
      </el-table>
    </el-card>

    <!-- 按科室统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按科室统计</span></template>
      <el-table :data="deptStats" border stripe>
        <el-table-column prop="deptName" label="科室" />
        <el-table-column prop="count" label="申请数" />
        <el-table-column prop="pendingCount" label="待审批" />
        <el-table-column prop="approvedCount" label="已通过" />
        <el-table-column prop="urgentCount" label="紧急会诊" />
      </el-table>
    </el-card>

    <!-- 按月趋势 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按月趋势</span></template>
      <el-table :data="monthStats" border stripe>
        <el-table-column prop="month" label="申请月份" />
        <el-table-column prop="count" label="申请数" />
        <el-table-column prop="approvedCount" label="已通过" />
        <el-table-column prop="pendingCount" label="待审批" />
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者姓名" prop="patientName"><el-input v-model="form.patientName" placeholder="患者姓名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="住院号"><el-input v-model="form.inpatientNo" placeholder="住院号" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会诊类型" prop="mdtType">
              <el-select v-model="form.mdtType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in mdtTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急程度" prop="urgency">
              <el-select v-model="form.urgency" placeholder="请选择" style="width: 100%">
                <el-option v-for="u in urgencies" :key="u.value" :label="u.label" :value="u.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="申请医师" prop="applyPhysicianId">
              <el-select v-model="form.applyPhysicianId" placeholder="请选择医师" style="width: 100%" @change="onPhysicianChange">
                <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望会诊时间" prop="expectedTime">
              <el-date-picker v-model="form.expectedTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="拟邀请科室"><el-input v-model="form.invitedDepts" placeholder="多个科室用顿号分隔" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="拟邀请专家"><el-input v-model="form.invitedExperts" placeholder="多个专家用顿号分隔" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="病情摘要" prop="conditionSummary"><el-input v-model="form.conditionSummary" type="textarea" :rows="3" placeholder="简述患者病情与会诊目的" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert title="提交后进入待审批（一级审批：医务科/科主任）；通过后才能添加会诊记录。已结束（已通过/已驳回）的申请不能再修改、再审批。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 / 会诊记录弹窗 -->
    <el-dialog title="MDT 详情与会诊记录" v-model="detailVisible" width="860px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="患者">{{ detail.application?.patientName }}（{{ detail.application?.inpatientNo || '无住院号' }}）</el-descriptions-item>
        <el-descriptions-item label="会诊类型">{{ typeText(detail.application?.mdtType) }}</el-descriptions-item>
        <el-descriptions-item label="申请科室">{{ detail.application?.deptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="申请医师">{{ detail.application?.applyPhysicianName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="期望会诊时间">{{ fmtDateTime(detail.application?.expectedTime) }}</el-descriptions-item>
        <el-descriptions-item label="紧急程度">{{ urgencyText(detail.application?.urgency) }}</el-descriptions-item>
        <el-descriptions-item label="拟邀请科室">{{ detail.application?.invitedDepts || '—' }}</el-descriptions-item>
        <el-descriptions-item label="拟邀请专家">{{ detail.application?.invitedExperts || '—' }}</el-descriptions-item>
        <el-descriptions-item label="病情摘要" :span="2">{{ detail.application?.conditionSummary || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">
          <el-tag :type="statusTag(detail.application?.status)">{{ statusText(detail.application?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.application?.approver || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批记录" :span="2">{{ detail.application?.approvalHistory || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin: 16px 0 8px; display: flex; justify-content: space-between; align-items: center">
        <strong>会诊记录（{{ (detail.records || []).length }} 条，可多次补充）</strong>
        <el-button v-if="detail.application?.status === 1" type="primary" size="small" @click="recordFormVisible = true">添加会诊记录</el-button>
      </div>
      <el-table :data="detail.records || []" border stripe>
        <el-table-column prop="meetingTime" label="会诊时间" width="160">
          <template #default="{ row }">{{ fmtDateTime(row.meetingTime) }}</template>
        </el-table-column>
        <el-table-column prop="meetingPlace" label="地点" width="130" />
        <el-table-column prop="attendees" label="实际参会专家" width="180" />
        <el-table-column prop="opinion" label="会诊意见/结论" />
        <el-table-column prop="suggestion" label="后续治疗建议" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDeleteRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(detail.records || []).length" description="暂无会诊记录：审批通过后可添加" />

      <!-- 添加会诊记录表单 -->
      <el-form :model="recordForm" :rules="recordRules" ref="recordFormRef" label-width="110px" style="margin-top: 16px" v-if="recordFormVisible">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="会诊时间" prop="meetingTime">
              <el-date-picker v-model="recordForm.meetingTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会诊地点"><el-input v-model="recordForm.meetingPlace" placeholder="如：医务科会议室" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="实际参会专家" prop="attendees"><el-input v-model="recordForm.attendees" placeholder="多个专家用顿号分隔" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="会诊意见/结论" prop="opinion"><el-input v-model="recordForm.opinion" type="textarea" :rows="3" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="后续治疗建议"><el-input v-model="recordForm.suggestion" type="textarea" :rows="2" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="记录人"><el-input v-model="recordForm.recorder" placeholder="记录人姓名" /></el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" @click="handleRecordSubmit">保存记录</el-button>
        <el-button @click="recordFormVisible = false">取消</el-button>
      </el-form>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog title="MDT 审批（医务科/科主任）" v-model="approveVisible" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="患者">{{ approveRow.patientName }}（{{ approveRow.inpatientNo || '无住院号' }}）</el-descriptions-item>
        <el-descriptions-item label="会诊类型">{{ typeText(approveRow.mdtType) }} · {{ urgencyText(approveRow.urgency) }}</el-descriptions-item>
        <el-descriptions-item label="申请科室/医师">{{ approveRow.deptName }} / {{ approveRow.applyPhysicianName }}</el-descriptions-item>
        <el-descriptions-item label="期望会诊时间">{{ fmtDateTime(approveRow.expectedTime) }}</el-descriptions-item>
        <el-descriptions-item label="拟邀请专家">{{ approveRow.invitedExperts || '—' }}</el-descriptions-item>
        <el-descriptions-item label="病情摘要">{{ approveRow.conditionSummary || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="approveForm" label-width="90px" style="margin-top: 16px">
        <el-form-item label="审批人">
          <el-input v-model="approveForm.approver" placeholder="医务科/科主任姓名" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="2" placeholder="审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">驳回</el-button>
        <el-button type="primary" @click="handleApproveSubmit">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMdts, getMdtStats, getMdtStatsByDept, getMdtStatsByMonth, getMdt,
  createMdt, updateMdt, deleteMdt, batchDeleteMdts,
  approveMdt, rejectMdt, addMdtRecord, deleteMdtRecord,
  exportMdts, downloadMdtTemplate
} from '../../api/mdt'
import { getEnabledDepts } from '../../api/system'
import { getPhysicians } from '../../api/physician'

const mdtTypes = [
  { value: 'IN_HOSPITAL', label: '院内会诊' },
  { value: 'OUT_HOSPITAL', label: '院外会诊' },
  { value: 'REMOTE', label: '远程会诊' },
  { value: 'EMERGENCY', label: '急会诊' }
]
const urgencies = [
  { value: 'NORMAL', label: '一般' },
  { value: 'URGENT', label: '紧急' }
]
const statuses = [
  { value: 0, label: '待审批' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' }
]
const typeText = (v) => mdtTypes.find((t) => t.value === v)?.label || v || ''
const urgencyText = (v) => urgencies.find((u) => u.value === v)?.label || v || ''
const statusText = (v) => statuses.find((s) => s.value === v)?.label || ''
const statusTag = (v) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[v] || 'info')
const fmtDateTime = (v) => (v ? String(v).slice(0, 16) : '')

const query = reactive({ keyword: '', deptId: null, mdtType: '', status: null, urgency: '', startDate: null, endDate: null, page: 1, size: 10 })
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
  mdtTypes.map((t) => ({ label: t.label, count: stats.value.byType?.[t.value] || 0 }))
)

const dialogVisible = ref(false)
const dialogTitle = ref('新增 MDT 申请')
const formRef = ref(null)
const form = reactive({
  id: null, patientName: '', inpatientNo: '', mdtType: 'IN_HOSPITAL', deptId: null, deptName: '',
  applyPhysicianId: null, applyPhysicianName: '', conditionSummary: '',
  invitedDepts: '', invitedExperts: '', expectedTime: '', urgency: 'NORMAL', remark: ''
})
const rules = {
  patientName: [{ required: true, message: '请填写患者姓名', trigger: 'blur' }],
  mdtType: [{ required: true, message: '请选择会诊类型', trigger: 'change' }],
  applyPhysicianId: [{ required: true, message: '请选择申请医师', trigger: 'change' }],
  expectedTime: [{ required: true, message: '请选择期望会诊时间', trigger: 'change' }],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }]
}

const detailVisible = ref(false)
const detail = ref({ application: null, records: [] })
const recordFormVisible = ref(false)
const recordFormRef = ref(null)
const recordForm = reactive({ attendees: '', meetingTime: '', meetingPlace: '', opinion: '', suggestion: '', recorder: '' })
const recordRules = {
  attendees: [{ required: true, message: '请填写实际参会专家', trigger: 'blur' }],
  meetingTime: [{ required: true, message: '请选择会诊时间', trigger: 'change' }],
  opinion: [{ required: true, message: '请填写会诊意见/结论', trigger: 'blur' }]
}

const approveVisible = ref(false)
const approveRow = ref({})
const approveForm = reactive({ approver: '吴文斌', opinion: '' })

async function fetchList() {
  loading.value = true
  try {
    const data = await getMdts({ ...query })
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
    stats.value = await getMdtStats()
    deptStats.value = await getMdtStatsByDept()
    monthStats.value = await getMdtStatsByMonth()
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
  Object.assign(query, { keyword: '', deptId: null, mdtType: '', status: null, urgency: '', startDate: null, endDate: null, page: 1, size: 10 })
  dateRange.value = null
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, patientName: '', inpatientNo: '', mdtType: 'IN_HOSPITAL', deptId: null, deptName: '',
    applyPhysicianId: null, applyPhysicianName: '', conditionSummary: '',
    invitedDepts: '', invitedExperts: '', expectedTime: '', urgency: 'NORMAL', remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增 MDT 申请'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑 MDT 申请'
  dialogVisible.value = true
}

function onPhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    form.applyPhysicianName = p.name
    form.deptId = p.deptId
    form.deptName = p.deptName
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateMdt(form.id, { ...form })
      ElMessage.success('MDT 申请修改成功')
    } else {
      await createMdt({ ...form })
      ElMessage.success('MDT 申请新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除患者「${row.patientName}」的 MDT 申请吗？`, '提示', { type: 'warning' })
  await deleteMdt(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 条 MDT 申请吗？`, '提示', { type: 'warning' })
  await batchDeleteMdts(selected.value.map((r) => r.id))
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
    const res = await exportMdts({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, 'MDT会诊申请.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadMdtTemplate()
  downloadBlob(res.data, 'MDT导入模板.xlsx')
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

// ---- 详情 / 会诊记录 ----
async function handleDetail(row) {
  try {
    detail.value = await getMdt(row.id)
    recordFormVisible.value = false
    Object.assign(recordForm, { attendees: '', meetingTime: '', meetingPlace: '', opinion: '', suggestion: '', recorder: '' })
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

async function handleRecordSubmit() {
  await recordFormRef.value.validate()
  try {
    await addMdtRecord(detail.value.application.id, { ...recordForm })
    ElMessage.success('会诊记录添加成功')
    recordFormVisible.value = false
    detail.value = await getMdt(detail.value.application.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleDeleteRecord(row) {
  await ElMessageBox.confirm('确定删除这条会诊记录吗？', '提示', { type: 'warning' })
  await deleteMdtRecord(detail.value.application.id, row.id)
  ElMessage.success('删除成功')
  detail.value = await getMdt(detail.value.application.id)
}

// ---- 审批 ----
function handleApprove(row) {
  approveRow.value = row
  Object.assign(approveForm, { approver: '吴文斌', opinion: '' })
  approveVisible.value = true
}

async function handleApproveSubmit() {
  try {
    await approveMdt(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('审批通过')
    approveVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

async function handleReject() {
  try {
    await rejectMdt(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('已驳回')
    approveVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('驳回失败：' + e.message)
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
