<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">委员会总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ stats.active || 0 }}</div><div class="label">在任</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ stats.expiringSoon || 0 }}</div><div class="label">任期90天内到期</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.meetingTotal || 0 }}</div><div class="label">会议总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.resolutionTotal || 0 }}</div><div class="label">决议总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ stats.overdueResolutionCount || 0 }}</div><div class="label">逾期决议</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="委员会名称 / 主任委员" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="委员会类别">
          <el-select v-model="query.type" placeholder="全部类别" clearable style="width: 140px">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.expiringSoon">只看任期90天内到期</el-checkbox>
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
          <el-button type="primary" @click="handleAdd">新增委员会</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/committees/import"
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
        <el-table-column prop="name" label="委员会名称" width="220" />
        <el-table-column prop="type" label="类别" width="90">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="chairperson" label="主任委员" width="100" />
        <el-table-column prop="secretary" label="秘书" width="100" />
        <el-table-column prop="memberCount" label="委员人数" width="90" />
        <el-table-column prop="termNo" label="届次" width="70">
          <template #default="{ row }">第 {{ row.termNo }} 届</template>
        </el-table-column>
        <el-table-column prop="termStartDate" label="任期开始" width="110" />
        <el-table-column prop="termEndDate" label="任期结束" width="110">
          <template #default="{ row }">
            <span :class="{ expired: isExpiringSoon(row) }">{{ row.termEndDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" :disabled="row.status !== 1" @click="handleRenew(row)">换届</el-button>
            <el-button link type="warning" :disabled="row.status !== 1" @click="handleDissolve(row)">解散</el-button>
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

    <!-- 逾期决议提醒 -->
    <el-card style="margin-top: 16px" v-if="overdueResolutions.length">
      <template #header><span>逾期决议提醒</span></template>
      <el-table :data="overdueResolutions" border stripe>
        <el-table-column prop="title" label="决议事项" />
        <el-table-column prop="responsiblePerson" label="责任人" width="100" />
        <el-table-column prop="deadline" label="决议期限" width="120">
          <template #default="{ row }"><span class="expired">{{ row.deadline }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="resolutionStatusTag(row.status)">{{ resolutionStatusText(row.status) }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 按类别统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按委员会类别统计</span></template>
      <el-table :data="typeStats" border stripe>
        <el-table-column prop="typeText" label="委员会类别" />
        <el-table-column prop="count" label="委员会数" />
        <el-table-column prop="activeCount" label="在任" />
      </el-table>
    </el-card>

    <!-- 按月会议统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按月会议统计</span></template>
      <el-table :data="monthStats" border stripe>
        <el-table-column prop="month" label="会议月份" />
        <el-table-column prop="count" label="会议次数" />
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="委员会名称" prop="name">
          <el-input v-model="form.name" placeholder="如：医疗质量管理委员会" />
        </el-form-item>
        <el-form-item label="委员会类别" prop="type">
          <el-select v-model="form.type" placeholder="请选择类别" style="width: 100%">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="主任委员">
          <el-input v-model="form.chairperson" />
        </el-form-item>
        <el-form-item label="副主任委员">
          <el-input v-model="form.viceChairperson" />
        </el-form-item>
        <el-form-item label="秘书">
          <el-input v-model="form.secretary" />
        </el-form-item>
        <el-form-item label="委员人数">
          <el-input-number v-model="form.memberCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="届次">
          <el-input-number v-model="form.termNo" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="任期">
          <el-date-picker v-model="form.termStartDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 48%" />
          <span style="margin: 0 8px">至</span>
          <el-date-picker v-model="form.termEndDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width: 48%" />
        </el-form-item>
        <el-form-item label="状态" v-if="form.id">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 换届弹窗 -->
    <el-dialog v-model="renewVisible" title="委员会换届" width="600px">
      <p>为「{{ renewRow.name }}」换届：生成第 {{ (renewRow.termNo || 1) + 1 }} 届新记录，原记录改为已换届。空字段沿用上一届。</p>
      <el-form :model="renewForm" label-width="130px">
        <el-form-item label="新一届主任委员">
          <el-input v-model="renewForm.chairperson" :placeholder="renewRow.chairperson || ''" />
        </el-form-item>
        <el-form-item label="新一届副主任委员">
          <el-input v-model="renewForm.viceChairperson" :placeholder="renewRow.viceChairperson || ''" />
        </el-form-item>
        <el-form-item label="新一届秘书">
          <el-input v-model="renewForm.secretary" :placeholder="renewRow.secretary || ''" />
        </el-form-item>
        <el-form-item label="新一届委员人数">
          <el-input-number v-model="renewForm.memberCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="新一届任期">
          <el-date-picker v-model="renewForm.termStartDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 48%" />
          <span style="margin: 0 8px">至</span>
          <el-date-picker v-model="renewForm.termEndDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width: 48%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenewSubmit">确认换届</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗：基本信息 + 会议记录 + 决议 -->
    <el-dialog v-model="detailVisible" :title="`委员会详情：${detail.name || ''}`" width="900px">
      <el-tabs v-model="detailTab">
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="委员会名称">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="类别">{{ typeText(detail.type) }}</el-descriptions-item>
            <el-descriptions-item label="主任委员">{{ detail.chairperson }}</el-descriptions-item>
            <el-descriptions-item label="副主任委员">{{ detail.viceChairperson }}</el-descriptions-item>
            <el-descriptions-item label="秘书">{{ detail.secretary }}</el-descriptions-item>
            <el-descriptions-item label="委员人数">{{ detail.memberCount }}</el-descriptions-item>
            <el-descriptions-item label="届次">第 {{ detail.termNo }} 届</el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag :type="statusTag(detail.status)">{{ statusText(detail.status) }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="任期">{{ detail.termStartDate }} 至 {{ detail.termEndDate }}</el-descriptions-item>
            <el-descriptions-item label="决议完成率">{{ stats.completionRate }}%</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="会议记录" name="meetings">
          <el-form :inline="true" :model="meetingForm" class="sub-form">
            <el-form-item><el-input v-model="meetingForm.title" placeholder="会议主题" style="width: 220px" /></el-form-item>
            <el-form-item><el-date-picker v-model="meetingForm.meetingDate" type="date" value-format="YYYY-MM-DD" placeholder="会议日期" style="width: 150px" /></el-form-item>
            <el-form-item><el-input v-model="meetingForm.location" placeholder="地点" style="width: 130px" /></el-form-item>
            <el-form-item><el-input v-model="meetingForm.host" placeholder="主持人" style="width: 100px" /></el-form-item>
            <el-form-item><el-input v-model="meetingForm.recorder" placeholder="记录人" style="width: 100px" /></el-form-item>
            <el-form-item>
              <el-button type="primary" :disabled="detail.status !== 1" @click="handleMeetingAdd">添加会议</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="meetingList" border stripe style="margin-top: 12px">
            <el-table-column prop="title" label="会议主题" />
            <el-table-column prop="meetingDate" label="会议日期" width="110" />
            <el-table-column prop="location" label="地点" width="150" />
            <el-table-column prop="host" label="主持人" width="90" />
            <el-table-column prop="attendeeCount" label="参会人数" width="90" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleMeetingDetail(row)">查看决议</el-button>
                <el-button link type="danger" @click="handleMeetingDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="决议管理" name="resolutions">
          <el-form :inline="true" :model="resolutionForm" class="sub-form">
            <el-form-item>
              <el-select v-model="resolutionForm.meetingId" placeholder="选择会议" style="width: 260px">
                <el-option v-for="m in meetingList" :key="m.id" :label="`${m.meetingDate} ${m.title}`" :value="m.id" />
              </el-select>
            </el-form-item>
            <el-form-item><el-input v-model="resolutionForm.title" placeholder="决议事项" style="width: 180px" /></el-form-item>
            <el-form-item><el-input v-model="resolutionForm.responsiblePerson" placeholder="责任人" style="width: 100px" /></el-form-item>
            <el-form-item><el-date-picker v-model="resolutionForm.deadline" type="date" value-format="YYYY-MM-DD" placeholder="决议期限" style="width: 150px" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleResolutionAdd">添加决议</el-button></el-form-item>
          </el-form>
          <el-table :data="resolutionList" border stripe style="margin-top: 12px">
            <el-table-column prop="title" label="决议事项" />
            <el-table-column prop="responsiblePerson" label="责任人" width="90" />
            <el-table-column prop="deadline" label="决议期限" width="110">
              <template #default="{ row }">
                <span :class="{ expired: isResolutionOverdue(row) }">{{ row.deadline }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="resolutionStatusTag(row.status)">{{ resolutionStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="completeDate" label="完成日期" width="110" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button link type="primary" v-if="row.status === 0" @click="handleResolutionStart(row)">开始执行</el-button>
                <el-button link type="primary" v-if="row.status === 1" @click="handleResolutionComplete(row)">完成</el-button>
                <el-button link type="warning" v-if="row.status === 0 || row.status === 1" @click="handleResolutionCancel(row)">取消</el-button>
                <el-button link type="danger" @click="handleResolutionDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCommittees, getCommitteeStats, getCommitteeStatsByType, getCommitteeStatsByMonth,
  getCommitteeById, createCommittee, updateCommittee, deleteCommittee,
  batchDeleteCommittees, renewCommittee, dissolveCommittee,
  exportCommittees, downloadCommitteeTemplate,
  getMeetingsByCommittee, createCommitteeMeeting, deleteCommitteeMeeting,
  getCommitteeResolutions, createCommitteeResolution, deleteCommitteeResolution,
  startCommitteeResolution, completeCommitteeResolution, cancelCommitteeResolution
} from '../../api/committee'

const types = [
  { value: 'MEDICAL_QUALITY', label: '医疗质量' },
  { value: 'PHARMACY', label: '药事' },
  { value: 'INFECTION', label: '院感' },
  { value: 'TRANSFUSION', label: '输血' },
  { value: 'MEDICAL_RECORD', label: '病案' },
  { value: 'OTHER', label: '其他' }
]
const statuses = [
  { value: 1, label: '在任' },
  { value: 2, label: '已换届' },
  { value: 3, label: '已解散' }
]
const typeText = (v) => types.find((t) => t.value === v)?.label || v || ''
const statusText = (v) => statuses.find((s) => s.value === v)?.label || ''
const statusTag = (v) => ({ 1: 'success', 2: 'warning', 3: 'info' }[v] || 'info')
const resolutionStatusText = (v) => ({ 0: '待执行', 1: '执行中', 2: '已完成', 3: '已取消' }[v] || '')
const resolutionStatusTag = (v) => ({ 0: 'warning', 1: '', 2: 'success', 3: 'info' }[v] || 'info')
const isExpiringSoon = (row) => {
  if (!row.termEndDate || row.status !== 1) return false
  const today = new Date()
  const warn = new Date(today.getTime() + 90 * 24 * 3600 * 1000)
  return new Date(row.termEndDate) <= warn
}
const isResolutionOverdue = (row) => {
  if (!row.deadline) return false
  return (row.status === 0 || row.status === 1) && row.deadline < new Date().toISOString().slice(0, 10)
}

const query = reactive({ keyword: '', type: '', status: null, expiringSoon: false, page: 1, size: 10 })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const stats = ref({})
const typeStats = ref([])
const monthStats = ref([])
const overdueResolutions = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增委员会')
const formRef = ref(null)
const form = reactive({
  id: null, name: '', type: 'MEDICAL_QUALITY', chairperson: '', viceChairperson: '',
  secretary: '', memberCount: null, termNo: 1, termStartDate: '', termEndDate: '', status: 1, remark: ''
})
const rules = {
  name: [{ required: true, message: '请填写委员会名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择委员会类别', trigger: 'change' }]
}

const renewVisible = ref(false)
const renewRow = ref({})
const renewForm = reactive({
  chairperson: '', viceChairperson: '', secretary: '', memberCount: null, termStartDate: '', termEndDate: ''
})

const detailVisible = ref(false)
const detailTab = ref('info')
const detail = ref({})
const meetingList = ref([])
const resolutionList = ref([])
const meetingForm = reactive({ title: '', meetingDate: '', location: '', host: '', recorder: '' })
const resolutionForm = reactive({ meetingId: null, title: '', responsiblePerson: '', deadline: '' })

async function fetchList() {
  loading.value = true
  try {
    const data = await getCommittees({ ...query })
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
    stats.value = await getCommitteeStats()
    typeStats.value = await getCommitteeStatsByType()
    monthStats.value = await getCommitteeStatsByMonth()
    overdueResolutions.value = await getCommitteeResolutions({ overdue: true, page: 1, size: 100 })
      .then((d) => d.records || d)
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

function refresh() {
  fetchList()
  fetchStats()
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { keyword: '', type: '', status: null, expiringSoon: false, page: 1, size: 10 })
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, name: '', type: 'MEDICAL_QUALITY', chairperson: '', viceChairperson: '',
    secretary: '', memberCount: null, termNo: 1, termStartDate: '', termEndDate: '', status: 1, remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增委员会'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑委员会'
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateCommittee(form.id, { ...form })
      ElMessage.success('委员会修改成功')
    } else {
      await createCommittee({ ...form })
      ElMessage.success('委员会新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除委员会「${row.name}」第 ${row.termNo} 届吗？其会议与决议将一并删除。`, '提示', { type: 'warning' })
  await deleteCommittee(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 个委员会吗？其会议与决议将一并删除。`, '提示', { type: 'warning' })
  await batchDeleteCommittees(selected.value.map((r) => r.id))
  ElMessage.success('批量删除成功')
  refresh()
}

function handleRenew(row) {
  renewRow.value = row
  Object.assign(renewForm, {
    chairperson: '', viceChairperson: '', secretary: '',
    memberCount: row.memberCount, termStartDate: '', termEndDate: ''
  })
  renewVisible.value = true
}

async function handleRenewSubmit() {
  try {
    const next = await renewCommittee(renewRow.value.id, { ...renewForm })
    ElMessage.success(`换届成功，已生成第 ${next.termNo} 届记录`)
    renewVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('换届失败：' + e.message)
  }
}

async function handleDissolve(row) {
  await ElMessageBox.confirm(`确定解散委员会「${row.name}」第 ${row.termNo} 届吗？解散后不能再新增会议。`, '提示', { type: 'warning' })
  try {
    await dissolveCommittee(row.id)
    ElMessage.success('已解散')
    refresh()
  } catch (e) {
    ElMessage.error('解散失败：' + e.message)
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

async function handleExport() {
  try {
    const res = await exportCommittees({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '委员会.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadCommitteeTemplate()
  downloadBlob(res.data, '委员会导入模板.xlsx')
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

// ---- 详情：基本信息 + 会议记录 + 决议管理 ----
async function handleDetail(row) {
  try {
    detail.value = await getCommitteeById(row.id)
    detailTab.value = 'info'
    await fetchSubRecords(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

async function fetchSubRecords(committeeId) {
  try {
    meetingList.value = await getMeetingsByCommittee(committeeId)
    const all = []
    for (const m of meetingList.value) {
      const rs = await getCommitteeResolutions({ meetingId: m.id, page: 1, size: 100 })
        .then((d) => d.records || d)
      all.push(...rs)
    }
    resolutionList.value = all
  } catch (e) {
    ElMessage.error('加载会议/决议失败：' + e.message)
  }
}

function resetSubForms() {
  Object.assign(meetingForm, { title: '', meetingDate: '', location: '', host: '', recorder: '' })
  Object.assign(resolutionForm, { meetingId: null, title: '', responsiblePerson: '', deadline: '' })
}

async function handleMeetingAdd() {
  if (!meetingForm.title || !meetingForm.meetingDate) {
    ElMessage.warning('请填写会议主题和会议日期')
    return
  }
  try {
    await createCommitteeMeeting({
      ...meetingForm, committeeId: detail.value.id, committeeName: detail.value.name
    })
    ElMessage.success('会议记录添加成功')
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleMeetingDelete(row) {
  await ElMessageBox.confirm(`确定删除会议「${row.title}」吗？其决议将一并删除。`, '提示', { type: 'warning' })
  await deleteCommitteeMeeting(row.id)
  ElMessage.success('删除成功')
  await fetchSubRecords(detail.value.id)
  fetchStats()
}

function handleMeetingDetail(row) {
  resolutionForm.meetingId = row.id
  detailTab.value = 'resolutions'
}

async function handleResolutionAdd() {
  if (!resolutionForm.meetingId || !resolutionForm.title) {
    ElMessage.warning('请选择会议并填写决议事项')
    return
  }
  try {
    await createCommitteeResolution({
      ...resolutionForm, committeeId: detail.value.id
    })
    ElMessage.success('决议添加成功')
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('添加失败：' + e.message)
  }
}

async function handleResolutionDelete(row) {
  await ElMessageBox.confirm(`确定删除决议「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteCommitteeResolution(row.id)
  ElMessage.success('删除成功')
  await fetchSubRecords(detail.value.id)
  fetchStats()
}

async function handleResolutionStart(row) {
  try {
    await startCommitteeResolution(row.id)
    ElMessage.success('已开始执行')
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}

async function handleResolutionComplete(row) {
  try {
    await completeCommitteeResolution(row.id)
    ElMessage.success('决议已完成')
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}

async function handleResolutionCancel(row) {
  await ElMessageBox.confirm(`确定取消决议「${row.title}」吗？`, '提示', { type: 'warning' })
  try {
    await cancelCommitteeResolution(row.id)
    ElMessage.success('决议已取消')
    await fetchSubRecords(detail.value.id)
    fetchStats()
  } catch (e) {
    ElMessage.error('操作失败：' + e.message)
  }
}

onMounted(() => {
  resetSubForms()
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
.sub-form {
  margin-top: 16px;
}
.expired {
  color: #f56c6c;
  font-weight: bold;
}
</style>
