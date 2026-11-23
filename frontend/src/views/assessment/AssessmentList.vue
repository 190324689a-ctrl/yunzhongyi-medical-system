<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">考核总数</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.avgScore ?? '—' }}</div><div class="label">平均分</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num ok">{{ stats.excellentCount || 0 }}（{{ stats.excellentRate ?? 0 }}%）</div><div class="label">优秀人数（优秀率）</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num bad">{{ stats.byGrade?.UNQUALIFIED || 0 }}</div><div class="label">不合格人数</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="医师">
          <el-input v-model="query.keyword" placeholder="姓名" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="query.deptId" placeholder="全部科室" clearable style="width: 140px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考核类型">
          <el-select v-model="query.assessType" placeholder="全部类型" clearable style="width: 160px">
            <el-option v-for="t in assessTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="考核年度">
          <el-select v-model="query.assessYear" placeholder="全部年度" clearable style="width: 120px">
            <el-option v-for="y in years" :key="y" :label="y + '年'" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="query.grade" placeholder="全部等级" clearable style="width: 130px">
            <el-option v-for="g in grades" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
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
          <el-button type="primary" @click="handleAdd">新增考核</el-button>
          <el-button @click="templateVisible = true">模板管理</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/assessments/import"
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
        <el-table-column prop="physicianName" label="医师" width="100" />
        <el-table-column prop="deptName" label="科室" width="110" />
        <el-table-column prop="templateName" label="考核模板" width="150" />
        <el-table-column prop="assessType" label="考核类型" width="120">
          <template #default="{ row }">{{ typeText(row.assessType) }}</template>
        </el-table-column>
        <el-table-column prop="assessYear" label="年度" width="80" />
        <el-table-column prop="cycle" label="周期" width="130" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="grade" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="gradeTag(row.grade)">{{ gradeText(row.grade) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="考核结果" width="130" />
        <el-table-column prop="assessor" label="考核人" width="90" />
        <el-table-column prop="assessDate" label="考核日期" width="110" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleScore(row)">评分录入</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <!-- 按科室评分统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按科室评分统计</span></template>
      <el-table :data="deptStats" border stripe>
        <el-table-column prop="deptName" label="科室" />
        <el-table-column prop="count" label="考核人数" />
        <el-table-column prop="avgScore" label="平均分" />
        <el-table-column prop="excellentCount" label="优秀人数" />
        <el-table-column label="优秀率">
          <template #default="{ row }">{{ row.excellentRate }}%</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 编辑 / 评分录入弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="被考核医师" prop="physicianId">
              <el-select v-model="form.physicianId" placeholder="请选择医师" style="width: 100%" @change="onPhysicianChange">
                <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核模板" prop="templateId">
              <el-select v-model="form.templateId" placeholder="请选择模板" style="width: 100%" @change="onTemplateChange">
                <el-option v-for="t in enabledTemplates" :key="t.id" :label="t.templateName" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核年度" prop="assessYear">
              <el-input-number v-model="form.assessYear" :min="2000" :max="2100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核周期">
              <el-input v-model="form.cycle" placeholder="如：年度 / 2026年第三季度" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核人">
              <el-input v-model="form.assessor" placeholder="考核人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核日期">
              <el-date-picker v-model="form.assessDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 评分项：按模板动态生成，每项得分不得超过权重 -->
        <el-divider content-position="left">评分录入（总分 100 分，等级由总分自动计算）</el-divider>
        <el-row :gutter="16">
          <el-col :span="12" v-for="item in scoreItems" :key="item.name">
            <el-form-item :label="`${item.name}（满分${item.weight}）`">
              <el-input-number v-model="scores[item.name]" :min="0" :max="item.weight" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert v-if="scoreItems.length" :title="`当前总分 ${computedTotal} 分，等级：${computedGradeText}`" type="info" :closable="false" show-icon />
        <el-form-item label="备注" style="margin-top: 16px"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 模板管理弹窗 -->
    <el-dialog title="考核模板管理" v-model="templateVisible" width="860px">
      <div class="toolbar" style="margin-bottom: 12px">
        <el-button type="primary" @click="handleTemplateAdd">新增模板</el-button>
      </div>
      <el-table :data="templates" border stripe>
        <el-table-column prop="templateName" label="模板名称" width="180" />
        <el-table-column prop="assessType" label="考核类型" width="130">
          <template #default="{ row }">{{ typeText(row.assessType) }}</template>
        </el-table-column>
        <el-table-column prop="cycle" label="考核周期" width="90" />
        <el-table-column label="评分项（维度：权重）">
          <template #default="{ row }">{{ scoreItemsText(row.scoreItems) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTemplateEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleTemplateToggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" @click="handleTemplateDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 模板新增 / 编辑表单 -->
      <el-form :model="templateForm" :rules="templateRules" ref="templateFormRef" label-width="110px" style="margin-top: 16px" v-if="templateFormVisible">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName"><el-input v-model="templateForm.templateName" placeholder="模板名称" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核类型" prop="assessType">
              <el-select v-model="templateForm.assessType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in assessTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核周期">
              <el-select v-model="templateForm.cycle" placeholder="请选择" style="width: 100%">
                <el-option label="年度" value="年度" />
                <el-option label="半年度" value="半年度" />
                <el-option label="季度" value="季度" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评分项" prop="scoreItems">
              <div style="width: 100%">
                <el-row :gutter="8" v-for="(item, idx) in templateItems" :key="idx" style="margin-bottom: 8px">
                  <el-col :span="12"><el-input v-model="item.name" placeholder="评分维度，如：医德医风" /></el-col>
                  <el-col :span="8"><el-input-number v-model="item.weight" :min="1" :max="100" placeholder="权重" style="width: 100%" /></el-col>
                  <el-col :span="4"><el-button type="danger" link @click="templateItems.splice(idx, 1)">删除</el-button></el-col>
                </el-row>
                <el-button @click="templateItems.push({ name: '', weight: null })">添加评分项</el-button>
                <span style="margin-left: 12px; color: #909399">权重之和必须为 100，当前：{{ templateWeightSum }}</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="templateForm.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" @click="handleTemplateSubmit">保存模板</el-button>
        <el-button @click="templateFormVisible = false">取消</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAssessments, getAssessmentStats, getAssessmentStatsByDept, createAssessment, updateAssessment,
  deleteAssessment, batchDeleteAssessments, exportAssessments, downloadAssessmentTemplate,
  getAssessmentTemplates, getEnabledAssessmentTemplates, createAssessmentTemplate,
  updateAssessmentTemplate, deleteAssessmentTemplate
} from '../../api/assessment'
import { getEnabledDepts } from '../../api/system'
import { getPhysicians } from '../../api/physician'

const assessTypes = [
  { value: 'PERIODIC', label: '定期考核' },
  { value: 'ETHICS', label: '医德考评' },
  { value: 'QUALITY', label: '医疗质量考核' }
]
const grades = [
  { value: 'EXCELLENT', label: '优秀' },
  { value: 'QUALIFIED', label: '合格' },
  { value: 'BASIC', label: '基本合格' },
  { value: 'UNQUALIFIED', label: '不合格' }
]
const typeText = (v) => assessTypes.find((t) => t.value === v)?.label || v || ''
const gradeText = (v) => grades.find((g) => g.value === v)?.label || v || ''
const gradeTag = (v) => ({ EXCELLENT: 'success', QUALIFIED: '', BASIC: 'warning', UNQUALIFIED: 'danger' }[v] || 'info')
const years = [2024, 2025, 2026, 2027]

const query = reactive({ keyword: '', deptId: null, assessType: '', assessYear: null, grade: '', page: 1, size: 10 })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const depts = ref([])
const physicians = ref([])
const stats = ref({})
const deptStats = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增考核')
const formRef = ref(null)
const form = reactive({
  id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
  templateId: null, templateName: '', assessType: 'PERIODIC', assessYear: new Date().getFullYear(),
  cycle: '年度', assessor: '', assessDate: '', remark: ''
})
const scores = reactive({})
const scoreItems = ref([])
const rules = {
  physicianId: [{ required: true, message: '请选择被考核医师', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择考核模板', trigger: 'change' }],
  assessYear: [{ required: true, message: '请填写考核年度', trigger: 'blur' }]
}
const computedTotal = computed(() => Object.values(scores).reduce((s, v) => s + (Number(v) || 0), 0))
const computedGradeText = computed(() => {
  const t = computedTotal.value
  return t >= 90 ? '优秀' : t >= 80 ? '合格' : t >= 60 ? '基本合格' : '不合格'
})

const templateVisible = ref(false)
const templates = ref([])
const enabledTemplates = ref([])
const templateFormVisible = ref(false)
const templateFormRef = ref(null)
const templateForm = reactive({ id: null, templateName: '', assessType: 'PERIODIC', cycle: '年度', status: 1, remark: '' })
const templateItems = ref([])
const templateRules = {
  templateName: [{ required: true, message: '请填写模板名称', trigger: 'blur' }],
  assessType: [{ required: true, message: '请选择考核类型', trigger: 'change' }]
}
const templateWeightSum = computed(() => templateItems.value.reduce((s, i) => s + (Number(i.weight) || 0), 0))
const scoreItemsText = (json) => {
  try {
    return JSON.parse(json || '[]').map((i) => `${i.name}：${i.weight}`).join('；')
  } catch (e) {
    return json || ''
  }
}
const parseScoreItems = (json) => {
  try {
    return JSON.parse(json || '[]')
  } catch (e) {
    return []
  }
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getAssessments({ ...query })
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
    stats.value = await getAssessmentStats()
    deptStats.value = await getAssessmentStatsByDept()
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

async function fetchTemplates() {
  try {
    const page = await getAssessmentTemplates({ page: 1, size: 100 })
    templates.value = page.records || []
    enabledTemplates.value = await getEnabledAssessmentTemplates()
  } catch (e) {
    console.warn('模板加载失败', e.message)
  }
}

function refresh() {
  fetchList()
  fetchStats()
  fetchTemplates()
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { keyword: '', deptId: null, assessType: '', assessYear: null, grade: '', page: 1, size: 10 })
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
    templateId: null, templateName: '', assessType: 'PERIODIC', assessYear: new Date().getFullYear(),
    cycle: '年度', assessor: '', assessDate: '', remark: ''
  })
  scoreItems.value = []
  Object.keys(scores).forEach((k) => delete scores[k])
}

function fillScoresFromTemplate(templateId, itemScoresJson) {
  const t = enabledTemplates.value.find((x) => x.id === templateId)
    || templates.value.find((x) => x.id === templateId)
  if (!t) return
  form.templateName = t.templateName
  form.assessType = t.assessType
  form.cycle = t.cycle
  scoreItems.value = parseScoreItems(t.scoreItems)
  const saved = {}
  try {
    Object.assign(saved, JSON.parse(itemScoresJson || '{}'))
  } catch (e) {
    // 无历史得分时全部置 0
  }
  Object.keys(scores).forEach((k) => delete scores[k])
  scoreItems.value.forEach((item) => {
    scores[item.name] = saved[item.name] ?? 0
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增考核'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  fillScoresFromTemplate(row.templateId, row.itemScores)
  dialogTitle.value = '编辑考核'
  dialogVisible.value = true
}

function handleScore(row) {
  resetForm()
  Object.assign(form, row)
  fillScoresFromTemplate(row.templateId, row.itemScores)
  dialogTitle.value = '评分录入'
  dialogVisible.value = true
}

function onPhysicianChange(id) {
  const p = physicians.value.find((x) => x.id === id)
  if (p) {
    form.physicianName = p.name
    form.deptId = p.deptId
    form.deptName = p.deptName
  }
}

function onTemplateChange(id) {
  fillScoresFromTemplate(id, null)
}

async function handleSubmit() {
  await formRef.value.validate()
  if (!scoreItems.value.length) {
    ElMessage.warning('请先选择考核模板')
    return
  }
  const payload = { ...form, itemScores: JSON.stringify(scores) }
  try {
    if (form.id) {
      await updateAssessment(form.id, payload)
      ElMessage.success(`保存成功，总分 ${computedTotal.value} 分，等级 ${computedGradeText.value}`)
    } else {
      await createAssessment(payload)
      ElMessage.success(`考核已保存，总分 ${computedTotal.value} 分，等级 ${computedGradeText.value}`)
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.physicianName}」${row.assessYear} 年${typeText(row.assessType)}记录吗？`, '提示', { type: 'warning' })
  await deleteAssessment(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 条考核记录吗？`, '提示', { type: 'warning' })
  await batchDeleteAssessments(selected.value.map((r) => r.id))
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
    const res = await exportAssessments({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '医师考核.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadAssessmentTemplate()
  downloadBlob(res.data, '医师考核导入模板.xlsx')
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

// ---- 模板管理 ----
function resetTemplateForm() {
  Object.assign(templateForm, { id: null, templateName: '', assessType: 'PERIODIC', cycle: '年度', status: 1, remark: '' })
  templateItems.value = [
    { name: '医德医风', weight: 30 },
    { name: '业务能力', weight: 30 },
    { name: '工作实绩', weight: 25 },
    { name: '服务质量', weight: 15 }
  ]
}

function handleTemplateAdd() {
  resetTemplateForm()
  templateFormVisible.value = true
}

function handleTemplateEdit(row) {
  Object.assign(templateForm, row)
  templateItems.value = parseScoreItems(row.scoreItems)
  templateFormVisible.value = true
}

async function handleTemplateToggle(row) {
  await updateAssessmentTemplate(row.id, { ...row, status: row.status === 1 ? 0 : 1 })
  ElMessage.success(row.status === 1 ? '已停用' : '已启用')
  fetchTemplates()
}

async function handleTemplateDelete(row) {
  await ElMessageBox.confirm(`确定删除模板「${row.templateName}」吗？`, '提示', { type: 'warning' })
  await deleteAssessmentTemplate(row.id)
  ElMessage.success('删除成功')
  fetchTemplates()
}

async function handleTemplateSubmit() {
  await templateFormRef.value.validate()
  if (templateWeightSum.value !== 100) {
    ElMessage.warning('评分项权重之和必须为 100，当前为 ' + templateWeightSum.value)
    return
  }
  if (templateItems.value.some((i) => !i.name || !i.weight)) {
    ElMessage.warning('评分项名称和权重都不能为空')
    return
  }
  const payload = { ...templateForm, scoreItems: JSON.stringify(templateItems.value) }
  try {
    if (templateForm.id) {
      await updateAssessmentTemplate(templateForm.id, payload)
      ElMessage.success('模板修改成功')
    } else {
      await createAssessmentTemplate(payload)
      ElMessage.success('模板新增成功')
    }
    templateFormVisible.value = false
    fetchTemplates()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
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
