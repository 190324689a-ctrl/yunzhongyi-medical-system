<template>
  <div>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ aStats.total || 0 }}</div><div class="label">评定总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ aStats.pending || 0 }}</div><div class="label">待审批</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ aStats.approved || 0 }}</div><div class="label">已通过</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ aStats.rejected || 0 }}</div><div class="label">已驳回</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ aStats.avgScore || 0 }}</div><div class="label">平均分</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ aStats.passRate || 0 }}%</div><div class="label">达标率（甲+乙+丙）</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ aStats.standardsUsed || 0 }}</div><div class="label">已用标准数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ aStats.deptsAssessed || 0 }}</div><div class="label">已评科室数</div></div></el-card></el-col>
    </el-row>
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="分级评定" name="assessment">
          <el-form :inline="true" :model="aQuery" class="search-form">
            <el-form-item label="关键字">
              <el-input v-model="aQuery.keyword" placeholder="科室名称 / 评定人" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item label="分级标准">
              <el-select v-model="aQuery.standardId" placeholder="全部标准" clearable style="width: 200px">
                <el-option v-for="s in enabledStandards" :key="s.id" :label="s.standardName" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="等级">
              <el-select v-model="aQuery.grade" placeholder="全部等级" clearable style="width: 110px">
                <el-option v-for="g in grades" :key="g.value" :label="g.label" :value="g.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="aQuery.status" placeholder="全部状态" clearable style="width: 110px">
                <el-option v-for="s in assessStatuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="评定周期">
              <el-input v-model="aQuery.period" placeholder="如 2026-11" clearable style="width: 120px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleASearch">查询</el-button>
              <el-button @click="handleAReset">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="toolbar">
            <div><el-button type="primary" @click="handleAAdd">新增分级评定</el-button></div>
            <div>
              <el-upload
                action="/api/grading-assessments/import"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-success="handleAImportSuccess"
                :on-error="handleImportError"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleAExport">导出 Excel</el-button>
              <el-button @click="handleADownloadTemplate">下载导入模板</el-button>
            </div>
          </div>
          <el-table :data="aData" border stripe v-loading="aLoading" style="margin-top: 12px">
            <el-table-column prop="deptName" label="评定科室" width="120" />
            <el-table-column prop="standardName" label="分级标准" min-width="170" />
            <el-table-column prop="period" label="评定周期" width="100" />
            <el-table-column prop="score" label="评分" width="80" />
            <el-table-column label="等级" width="80">
              <template #default="{ row }"><el-tag :type="gradeTagType(row.grade)">{{ gradeText(row.grade) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><el-tag :type="assessStatusTag(row.status)">{{ assessStatusText(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="assessor" label="评定人" width="90" />
            <el-table-column prop="assessDate" label="评定日期" width="110" />
            <el-table-column prop="approver" label="审批人" width="90" />
            <el-table-column prop="approveOpinion" label="审批意见" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status !== 0" @click="handleApprove(row)">通过</el-button>
                <el-button link type="warning" :disabled="row.status !== 0" @click="handleReject(row)">驳回</el-button>
                <el-button link type="primary" :disabled="row.status !== 0" @click="handleAEdit(row)">编辑</el-button>
                <el-button link type="danger" :disabled="row.status === 1" @click="handleADelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            style="margin-top: 16px; justify-content: flex-end"
            v-model:current-page="aQuery.page"
            v-model:page-size="aQuery.size"
            :total="aTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchAssessments"
            @current-change="fetchAssessments"
          />
          <el-table :data="byDept" border stripe style="margin-top: 16px" v-loading="aLoading">
            <el-table-column prop="deptName" label="科室" width="140" />
            <el-table-column label="当前分级" width="110">
              <template #default="{ row }"><el-tag :type="gradeTagType(row.currentGrade)">{{ row.currentGradeText }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="latestPeriod" label="最新已通过周期" width="150" />
            <el-table-column prop="avgScore" label="平均分" width="90" />
            <el-table-column prop="total" label="评定次数" width="90" />
            <el-table-column prop="approved" label="已通过" width="90" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="分级标准" name="standard">
          <el-form :inline="true" :model="sQuery" class="search-form">
            <el-form-item label="关键字">
              <el-input v-model="sQuery.keyword" placeholder="标准名称 / 编码" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item label="分级范围">
              <el-select v-model="sQuery.scope" placeholder="全部分级范围" clearable style="width: 150px">
                <el-option v-for="s in scopes" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="sQuery.status" placeholder="全部状态" clearable style="width: 110px">
                <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSSearch">查询</el-button>
              <el-button @click="handleSReset">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="toolbar">
            <div><el-button type="primary" @click="handleSAdd">新增分级标准</el-button></div>
            <div>
              <el-upload
                action="/api/grading-standards/import"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-success="handleSImportSuccess"
                :on-error="handleImportError"
                style="display: inline-block; margin: 0 8px"
              >
                <el-button type="success">批量导入</el-button>
              </el-upload>
              <el-button type="warning" @click="handleSExport">导出 Excel</el-button>
              <el-button @click="handleSDownloadTemplate">下载导入模板</el-button>
            </div>
          </div>
          <el-table :data="sData" border stripe v-loading="sLoading" style="margin-top: 12px">
            <el-table-column prop="standardCode" label="标准编码" width="120" />
            <el-table-column prop="standardName" label="标准名称" min-width="170" />
            <el-table-column label="分级范围" width="130">
              <template #default="{ row }">{{ scopeText(row.scope) }}</template>
            </el-table-column>
            <el-table-column label="分数线（甲/乙/丙）" width="170">
              <template #default="{ row }">{{ row.excellentMin }} / {{ row.goodMin }} / {{ row.passMin }}</template>
            </el-table-column>
            <el-table-column label="有效期" width="210">
              <template #default="{ row }">{{ row.validFrom || '—' }} 至 {{ row.validTo || '—' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleSEdit(row)">编辑</el-button>
                <el-button link type="warning" :disabled="row.status !== 1" @click="handleSDisable(row)">停用</el-button>
                <el-button link type="success" :disabled="row.status !== 0" @click="handleSEnable(row)">启用</el-button>
                <el-button link type="danger" @click="handleSDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            style="margin-top: 16px; justify-content: flex-end"
            v-model:current-page="sQuery.page"
            v-model:page-size="sQuery.size"
            :total="sTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchStandards"
            @current-change="fetchStandards"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增 / 编辑分级标准 -->
    <el-dialog v-model="sDialogVisible" :title="sDialogTitle" width="640px">
      <el-form ref="sFormRef" :model="sForm" :rules="sRules" label-width="110px">
        <el-form-item label="标准编码" prop="standardCode">
          <el-input v-model="sForm.standardCode" placeholder="如 STD-Q001" :disabled="!!sForm.id" />
        </el-form-item>
        <el-form-item label="标准名称" prop="standardName">
          <el-input v-model="sForm.standardName" placeholder="如 医疗质量分级标准" />
        </el-form-item>
        <el-form-item label="分级范围" prop="scope">
          <el-select v-model="sForm.scope" placeholder="请选择" style="width: 100%">
            <el-option v-for="s in scopes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="甲级线" prop="excellentMin">
          <el-input-number v-model="sForm.excellentMin" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="乙级线" prop="goodMin">
          <el-input-number v-model="sForm.goodMin" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="丙级线" prop="passMin">
          <el-input-number v-model="sForm.passMin" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期起">
          <el-date-picker v-model="sForm.validFrom" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期止">
          <el-date-picker v-model="sForm.validTo" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="sForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑分级评定 -->
    <el-dialog v-model="aDialogVisible" :title="aDialogTitle" width="640px">
      <el-form ref="aFormRef" :model="aForm" :rules="aRules" label-width="110px">
        <el-form-item label="分级标准" prop="standardId">
          <el-select v-model="aForm.standardId" placeholder="请选择启用的分级标准" style="width: 100%">
            <el-option v-for="s in enabledStandards" :key="s.id" :label="`${s.standardName}（甲 ${s.excellentMin} / 乙 ${s.goodMin} / 丙 ${s.passMin}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评定科室" prop="deptName">
          <el-input v-model="aForm.deptName" placeholder="如 中医内科" />
        </el-form-item>
        <el-form-item label="评定周期" prop="period">
          <el-input v-model="aForm.period" placeholder="如 2026-11" />
        </el-form-item>
        <el-form-item label="评分" prop="score">
          <el-input-number v-model="aForm.score" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="评定人">
          <el-input v-model="aForm.assessor" placeholder="评定人姓名" />
        </el-form-item>
        <el-form-item label="评定日期">
          <el-date-picker v-model="aForm.assessDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="aForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleASubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 审批 -->
    <el-dialog v-model="approveDialogVisible" :title="approveTitle" width="480px">
      <el-form :model="approveForm" label-width="90px">
        <el-form-item label="审批人">
          <el-input v-model="approveForm.approver" placeholder="审批人姓名" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="3" placeholder="审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApproveSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getGradingStandards, getGradingStandardStats, getEnabledGradingStandards,
  createGradingStandard, updateGradingStandard, deleteGradingStandard,
  enableGradingStandard, disableGradingStandard, exportGradingStandards,
  downloadGradingStandardTemplate,
  getGradingAssessments, getGradingAssessmentStats, getGradingStatsByDept,
  createGradingAssessment, updateGradingAssessment, deleteGradingAssessment,
  approveGradingAssessment, rejectGradingAssessment, exportGradingAssessments,
  downloadGradingAssessmentTemplate
} from '../../api/medGrading'

// ---------- 字典 ----------
const scopes = [
  { value: 'QUALITY', label: '医疗质量分级' },
  { value: 'SERVICE', label: '医疗服务分级' },
  { value: 'COMPREHENSIVE', label: '综合分级' }
]
const statuses = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' }
]
const grades = [
  { value: 'A', label: '甲级' },
  { value: 'B', label: '乙级' },
  { value: 'C', label: '丙级' },
  { value: 'D', label: '丁级' }
]
const assessStatuses = [
  { value: 0, label: '待审批' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' }
]

function scopeText(v) {
  return (scopes.find((c) => c.value === v) || {}).label || v || ''
}
function statusText(v) {
  return (statuses.find((c) => c.value === v) || {}).label || ''
}
function gradeText(v) {
  return (grades.find((c) => c.value === v) || {}).label || v || ''
}
function gradeTagType(v) {
  return { A: 'success', B: '', C: 'warning', D: 'danger' }[v] || 'info'
}
function assessStatusText(v) {
  return (assessStatuses.find((c) => c.value === v) || {}).label || ''
}
function assessStatusTag(v) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[v] || 'info'
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---------- 公共 ----------
const activeTab = ref('assessment')
const enabledStandards = ref([])

async function fetchEnabledStandards() {
  try {
    const res = await getEnabledGradingStandards()
    enabledStandards.value = res.data || []
  } catch (e) {
    ElMessage.error('加载分级标准失败：' + e.message)
  }
}

function handleTabChange() {
  if (activeTab.value === 'standard') fetchStandards()
  else fetchAssessments()
}

// ---------- 分级评定 ----------
const aStats = ref({})
const byDept = ref([])
const aData = ref([])
const aTotal = ref(0)
const aLoading = ref(false)
const aQuery = reactive({ keyword: '', standardId: '', grade: '', status: '', period: '', page: 1, size: 10 })

async function fetchAssessments() {
  aLoading.value = true
  try {
    const res = await getGradingAssessments({ ...aQuery })
    aData.value = res.data.records || []
    aTotal.value = res.data.total || 0
    const stats = await getGradingAssessmentStats()
    aStats.value = stats.data || {}
    const dept = await getGradingStatsByDept()
    byDept.value = dept.data || []
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    aLoading.value = false
  }
}
function handleASearch() {
  aQuery.page = 1
  fetchAssessments()
}
function handleAReset() {
  Object.assign(aQuery, { keyword: '', standardId: '', grade: '', status: '', period: '', page: 1, size: 10 })
  fetchAssessments()
}

const aDialogVisible = ref(false)
const aDialogTitle = ref('新增分级评定')
const aFormRef = ref(null)
const aForm = reactive({ id: null, standardId: '', deptName: '', period: '', score: null, assessor: '', assessDate: '', remark: '' })
const aRules = {
  standardId: [{ required: true, message: '请选择分级标准', trigger: 'change' }],
  deptName: [{ required: true, message: '请输入评定科室', trigger: 'blur' }],
  period: [{ required: true, message: '请输入评定周期，如 2026-11', trigger: 'blur' }],
  score: [{ required: true, message: '请输入评分', trigger: 'blur' }]
}

function handleAAdd() {
  aDialogTitle.value = '新增分级评定'
  Object.assign(aForm, { id: null, standardId: '', deptName: '', period: '', score: null, assessor: '', assessDate: '', remark: '' })
  aDialogVisible.value = true
}
function handleAEdit(row) {
  aDialogTitle.value = '编辑分级评定'
  Object.assign(aForm, { ...row })
  aDialogVisible.value = true
}
async function handleASubmit() {
  try {
    await aFormRef.value.validate()
    if (aForm.id) {
      await updateGradingAssessment(aForm.id, aForm)
      ElMessage.success('修改成功')
    } else {
      await createGradingAssessment(aForm)
      ElMessage.success('新增成功')
    }
    aDialogVisible.value = false
    fetchAssessments()
  } catch (e) {
    if (e && e.message) ElMessage.error((aForm.id ? '修改失败：' : '新增失败：') + e.message)
  }
}
async function handleADelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.deptName} ${row.period}」的分级评定吗？已通过的评定不能删除。`, '提示', { type: 'warning' })
    await deleteGradingAssessment(row.id)
    ElMessage.success('删除成功')
    fetchAssessments()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}

const approveDialogVisible = ref(false)
const approveTitle = ref('审批通过')
const approveTarget = ref(null)
const approveAction = ref('approve')
const approveForm = reactive({ approver: '', opinion: '' })

function handleApprove(row) {
  approveTitle.value = '审批通过'
  approveAction.value = 'approve'
  approveTarget.value = row
  Object.assign(approveForm, { approver: '', opinion: '' })
  approveDialogVisible.value = true
}
function handleReject(row) {
  approveTitle.value = '审批驳回'
  approveAction.value = 'reject'
  approveTarget.value = row
  Object.assign(approveForm, { approver: '', opinion: '' })
  approveDialogVisible.value = true
}
async function handleApproveSubmit() {
  if (!approveForm.approver || !approveForm.approver.trim()) {
    ElMessage.error('审批人不能为空')
    return
  }
  try {
    if (approveAction.value === 'approve') {
      await approveGradingAssessment(approveTarget.value.id, approveForm)
      ElMessage.success('已通过')
    } else {
      await rejectGradingAssessment(approveTarget.value.id, approveForm)
      ElMessage.success('已驳回')
    }
    approveDialogVisible.value = false
    fetchAssessments()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

async function handleAExport() {
  try {
    const res = await exportGradingAssessments({ ...aQuery })
    downloadBlob(res, '分级评定.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleADownloadTemplate() {
  try {
    const res = await downloadGradingAssessmentTemplate()
    downloadBlob(res, '分级评定导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}
function handleAImportSuccess(res) {
  if (res && res.code === 200) {
    ElMessage.success(`导入完成：新增 ${res.data.created} 条，跳过 ${res.data.skipped} 条`)
    fetchAssessments()
  } else {
    ElMessage.error('导入失败：' + (res ? res.message : '未知错误'))
  }
}

// ---------- 分级标准 ----------
const sData = ref([])
const sTotal = ref(0)
const sLoading = ref(false)
const sQuery = reactive({ keyword: '', scope: '', status: '', page: 1, size: 10 })

async function fetchStandards() {
  sLoading.value = true
  try {
    const res = await getGradingStandards({ ...sQuery })
    sData.value = res.data.records || []
    sTotal.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    sLoading.value = false
  }
}
function handleSSearch() {
  sQuery.page = 1
  fetchStandards()
}
function handleSReset() {
  Object.assign(sQuery, { keyword: '', scope: '', status: '', page: 1, size: 10 })
  fetchStandards()
}

const sDialogVisible = ref(false)
const sDialogTitle = ref('新增分级标准')
const sFormRef = ref(null)
const sForm = reactive({ id: null, standardCode: '', standardName: '', scope: '', excellentMin: 90, goodMin: 80, passMin: 60, validFrom: '', validTo: '', remark: '' })
const sRules = {
  standardCode: [{ required: true, message: '请输入标准编码', trigger: 'blur' }],
  standardName: [{ required: true, message: '请输入标准名称', trigger: 'blur' }],
  scope: [{ required: true, message: '请选择分级范围', trigger: 'change' }],
  excellentMin: [{ required: true, message: '请输入甲级线', trigger: 'blur' }],
  goodMin: [{ required: true, message: '请输入乙级线', trigger: 'blur' }],
  passMin: [{ required: true, message: '请输入丙级线', trigger: 'blur' }]
}

function handleSAdd() {
  sDialogTitle.value = '新增分级标准'
  Object.assign(sForm, { id: null, standardCode: '', standardName: '', scope: '', excellentMin: 90, goodMin: 80, passMin: 60, validFrom: '', validTo: '', remark: '' })
  sDialogVisible.value = true
}
function handleSEdit(row) {
  sDialogTitle.value = '编辑分级标准'
  Object.assign(sForm, { ...row })
  sDialogVisible.value = true
}
async function handleSSubmit() {
  try {
    await sFormRef.value.validate()
    if (!(sForm.passMin < sForm.goodMin && sForm.goodMin < sForm.excellentMin)) {
      ElMessage.error('分数线须满足 丙级线 < 乙级线 < 甲级线')
      return
    }
    if (sForm.id) {
      await updateGradingStandard(sForm.id, sForm)
      ElMessage.success('修改成功')
    } else {
      await createGradingStandard(sForm)
      ElMessage.success('新增成功')
    }
    sDialogVisible.value = false
    fetchStandards()
    fetchEnabledStandards()
  } catch (e) {
    if (e && e.message) ElMessage.error((sForm.id ? '修改失败：' : '新增失败：') + e.message)
  }
}
async function handleSEnable(row) {
  try {
    await enableGradingStandard(row.id)
    ElMessage.success('已启用')
    fetchStandards()
    fetchEnabledStandards()
  } catch (e) {
    ElMessage.error('启用失败：' + e.message)
  }
}
async function handleSDisable(row) {
  try {
    await ElMessageBox.confirm('停用后该标准不能用于新建分级评定，确定停用吗？', '提示', { type: 'warning' })
    await disableGradingStandard(row.id)
    ElMessage.success('已停用')
    fetchStandards()
    fetchEnabledStandards()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('停用失败：' + e.message)
  }
}
async function handleSDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除标准「${row.standardName}」吗？已有分级评定的标准不能删除。`, '提示', { type: 'warning' })
    await deleteGradingStandard(row.id)
    ElMessage.success('删除成功')
    fetchStandards()
    fetchEnabledStandards()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}
async function handleSExport() {
  try {
    const res = await exportGradingStandards({ ...sQuery })
    downloadBlob(res, '分级标准.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleSDownloadTemplate() {
  try {
    const res = await downloadGradingStandardTemplate()
    downloadBlob(res, '分级标准导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}
function handleSImportSuccess(res) {
  if (res && res.code === 200) {
    ElMessage.success(`导入完成：新增 ${res.data.created} 条，跳过 ${res.data.skipped} 条`)
    fetchStandards()
    fetchEnabledStandards()
  } else {
    ElMessage.error('导入失败：' + (res ? res.message : '未知错误'))
  }
}

onMounted(() => {
  fetchAssessments()
  fetchEnabledStandards()
})
</script>

<style scoped>
.stat { text-align: center; }
.stat .num { font-size: 22px; font-weight: bold; }
.stat .num.ok { color: #67c23a; }
.stat .num.warn { color: #e6a23c; }
.stat .label { font-size: 12px; color: #909399; margin-top: 4px; }
.search-form { margin-bottom: 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
</style>
