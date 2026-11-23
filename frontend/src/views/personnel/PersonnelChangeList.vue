<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">变动总数</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ stats.pending || 0 }}</div><div class="label">待审批</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num ok">{{ stats.approved || 0 }}</div><div class="label">已通过</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num bad">{{ stats.rejected || 0 }}</div><div class="label">已驳回</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="人员姓名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="相关科室">
          <el-select v-model="query.deptId" placeholder="全部科室" clearable style="width: 140px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="变动类型">
          <el-select v-model="query.changeType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in changeTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效日期">
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
          <el-button type="primary" @click="handleAdd">新增登记</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/personnel-changes/import"
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
        <el-table-column prop="physicianName" label="人员姓名" width="100" />
        <el-table-column prop="changeType" label="变动类型" width="90">
          <template #default="{ row }">{{ typeText(row.changeType) }}</template>
        </el-table-column>
        <el-table-column prop="fromDeptName" label="原科室" width="120">
          <template #default="{ row }">{{ row.fromDeptName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="toDeptName" label="调入/新科室" width="120">
          <template #default="{ row }">{{ row.toDeptName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="changeDate" label="生效日期" width="120" />
        <el-table-column prop="applyDate" label="申请日期" width="120" />
        <el-table-column prop="reason" label="变动原因" min-width="180" />
        <el-table-column prop="status" label="审批状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
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

    <!-- 按变动类型统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按变动类型统计</span></template>
      <el-table :data="typeStats" border stripe>
        <el-table-column prop="label" label="变动类型" />
        <el-table-column prop="count" label="变动数" />
      </el-table>
    </el-card>

    <!-- 按相关科室统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按相关科室统计</span></template>
      <el-table :data="deptStats" border stripe>
        <el-table-column prop="deptName" label="科室" />
        <el-table-column prop="count" label="变动数" />
        <el-table-column prop="pendingCount" label="待审批" />
        <el-table-column prop="approvedCount" label="已通过" />
        <el-table-column prop="rejectedCount" label="已驳回" />
      </el-table>
    </el-card>

    <!-- 按月趋势 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按月趋势</span></template>
      <el-table :data="monthStats" border stripe>
        <el-table-column prop="month" label="生效月份" />
        <el-table-column prop="count" label="变动数" />
        <el-table-column prop="approvedCount" label="已通过" />
        <el-table-column prop="pendingCount" label="待审批" />
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="820px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="选择医师">
              <el-select v-model="form.physicianId" placeholder="医师档案中选择（新入职可不选）" clearable style="width: 100%" @change="onPhysicianChange">
                <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人员姓名" prop="physicianName"><el-input v-model="form.physicianName" placeholder="人员姓名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="变动类型" prop="changeType">
              <el-select v-model="form.changeType" placeholder="请选择" style="width: 100%" @change="onTypeChange">
                <el-option v-for="t in changeTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效日期" prop="changeDate">
              <el-date-picker v-model="form.changeDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原科室">
              <el-select v-model="form.fromDeptId" placeholder="请选择" clearable style="width: 100%" :disabled="form.changeType === 'JOIN'" @change="onFromDeptChange">
                <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调入/新科室">
              <el-select v-model="form.toDeptId" placeholder="请选择" clearable style="width: 100%" :disabled="form.changeType === 'RESIGN' || form.changeType === 'RETIRE'" @change="onToDeptChange">
                <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="变动原因"><el-input v-model="form.reason" type="textarea" :rows="2" placeholder="变动原因" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert title="调动须填调入科室且与原科室不同；离职/退休须填原科室；入职/返聘须填调入（新）科室。提交后进入待审批（一级审批：医务科），通过时同步医师档案：调动改科室，离职改在职状态为离职，退休改为退休，入职/返聘改回在职；新入职人员尚未建档时需在医师档案里补建档案。已结束审批的变动不能再修改、再审批。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog title="医疗人员动态详情" v-model="detailVisible" width="760px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="人员姓名">{{ detail.physicianName }}</el-descriptions-item>
        <el-descriptions-item label="变动类型">{{ typeText(detail.changeType) }}</el-descriptions-item>
        <el-descriptions-item label="原科室">{{ detail.fromDeptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="调入/新科室">{{ detail.toDeptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="生效日期">{{ detail.changeDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="申请日期">{{ detail.applyDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="变动原因" :span="2">{{ detail.reason || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">
          <el-tag :type="statusTag(detail.status)">{{ statusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.approver || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ detail.approvalOpinion || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批记录" :span="2">{{ detail.approvalHistory || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog title="医疗人员动态审批（医务科）" v-model="approveVisible" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="人员姓名">{{ approveRow.physicianName }}</el-descriptions-item>
        <el-descriptions-item label="变动类型">{{ typeText(approveRow.changeType) }}</el-descriptions-item>
        <el-descriptions-item label="原科室 / 调入（新）科室">{{ approveRow.fromDeptName || '—' }} / {{ approveRow.toDeptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="生效日期">{{ approveRow.changeDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="变动原因">{{ approveRow.reason || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert v-if="approveRow.changeType === 'JOIN' && !approveRow.physicianId" title="该人员尚未在医师档案建档，审批通过后需在医师档案里补建档案。" type="warning" :closable="false" show-icon style="margin-top: 12px" />
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
        <el-button type="primary" @click="handleApproveSubmit">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPersonnelChanges, getPersonnelChangeStats, getPersonnelChangeStatsByDept, getPersonnelChangeStatsByMonth,
  getPersonnelChange, createPersonnelChange, updatePersonnelChange, deletePersonnelChange,
  batchDeletePersonnelChanges, approvePersonnelChange, rejectPersonnelChange,
  exportPersonnelChanges, downloadPersonnelChangeTemplate
} from '../../api/personnelChange'
import { getEnabledDepts } from '../../api/system'
import { getPhysicians } from '../../api/physician'

const changeTypes = [
  { value: 'TRANSFER', label: '调动' },
  { value: 'RESIGN', label: '离职' },
  { value: 'RETIRE', label: '退休' },
  { value: 'JOIN', label: '入职' },
  { value: 'REHIRE', label: '返聘' }
]
const statuses = [
  { value: 0, label: '待审批' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' }
]
const typeText = (v) => changeTypes.find((t) => t.value === v)?.label || v || ''
const statusText = (v) => statuses.find((s) => s.value === v)?.label || ''
const statusTag = (v) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[v] || 'info')

const query = reactive({ keyword: '', deptId: null, changeType: '', status: null, startDate: null, endDate: null, page: 1, size: 10 })
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
  changeTypes.map((t) => ({ label: t.label, count: stats.value.byType?.[t.value] || 0 }))
)

const dialogVisible = ref(false)
const dialogTitle = ref('新增登记')
const formRef = ref(null)
const form = reactive({
  id: null, physicianId: null, physicianName: '', changeType: 'TRANSFER',
  fromDeptId: null, fromDeptName: '', toDeptId: null, toDeptName: '',
  changeDate: '', reason: '', remark: ''
})
const rules = {
  physicianName: [{ required: true, message: '请填写人员姓名', trigger: 'blur' }],
  changeType: [{ required: true, message: '请选择变动类型', trigger: 'change' }],
  changeDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }]
}

const detailVisible = ref(false)
const detail = ref({})

const approveVisible = ref(false)
const approveRow = ref({})
const approveForm = reactive({ approver: '吴文斌', opinion: '' })

async function fetchList() {
  loading.value = true
  try {
    const data = await getPersonnelChanges({ ...query })
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
    stats.value = await getPersonnelChangeStats()
    deptStats.value = await getPersonnelChangeStatsByDept()
    monthStats.value = await getPersonnelChangeStatsByMonth()
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
  Object.assign(query, { keyword: '', deptId: null, changeType: '', status: null, startDate: null, endDate: null, page: 1, size: 10 })
  dateRange.value = null
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, physicianId: null, physicianName: '', changeType: 'TRANSFER',
    fromDeptId: null, fromDeptName: '', toDeptId: null, toDeptName: '',
    changeDate: '', reason: '', remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增登记'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑登记'
  dialogVisible.value = true
}

function onPhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    form.physicianName = p.name
    form.fromDeptId = p.deptId
    form.fromDeptName = p.deptName
  }
}

function onTypeChange() {
  // 入职无原科室，离职/退休无调入科室：切换类型时清空不适用的科室
  if (form.changeType === 'JOIN') {
    form.fromDeptId = null
    form.fromDeptName = ''
  }
  if (form.changeType === 'RESIGN' || form.changeType === 'RETIRE') {
    form.toDeptId = null
    form.toDeptName = ''
  }
}

function onFromDeptChange(id) {
  const d = depts.value.find((x) => x.id === id)
  form.fromDeptName = d ? d.name : ''
}

function onToDeptChange(id) {
  const d = depts.value.find((x) => x.id === id)
  form.toDeptName = d ? d.name : ''
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updatePersonnelChange(form.id, { ...form })
      ElMessage.success('变动修改成功')
    } else {
      await createPersonnelChange({ ...form })
      ElMessage.success('变动登记成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除人员「${row.physicianName}」的${typeText(row.changeType)}记录吗？`, '提示', { type: 'warning' })
  await deletePersonnelChange(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 条变动记录吗？`, '提示', { type: 'warning' })
  await batchDeletePersonnelChanges(selected.value.map((r) => r.id))
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
    const res = await exportPersonnelChanges({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '医疗人员动态.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadPersonnelChangeTemplate()
  downloadBlob(res.data, '医疗人员动态导入模板.xlsx')
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

// ---- 详情 ----
async function handleDetail(row) {
  try {
    detail.value = await getPersonnelChange(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

// ---- 审批 ----
function handleApprove(row) {
  approveRow.value = row
  Object.assign(approveForm, { approver: '吴文斌', opinion: '' })
  approveVisible.value = true
}

async function handleApproveSubmit() {
  try {
    await approvePersonnelChange(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('审批通过，医师档案已同步')
    approveVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

async function handleReject() {
  try {
    await rejectPersonnelChange(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
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
