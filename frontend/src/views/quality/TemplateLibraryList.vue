<template>
  <div>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.templateTotal || 0 }}</div><div class="label">模板总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.enabled || 0 }}</div><div class="label">已启用</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.disabled || 0 }}</div><div class="label">已停用</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.versionTotal || 0 }}</div><div class="label">版本总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.avgItems || 0 }}</div><div class="label">平均检查项数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ stats.neverUsed || 0 }}</div><div class="label">从未使用</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.templatesWithoutVersions || 0 }}</div><div class="label">无版本记录</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.mostUsedPlanCount || 0 }}</div><div class="label">最常用：{{ stats.mostUsedTemplate || '—' }}</div></div></el-card></el-col>
    </el-row>
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="模板名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="检查类型">
          <el-select v-model="query.inspectType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in inspectTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 110px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="handleAdd">新增检查模板</el-button>
        </div>
        <div>
          <el-upload
            action="/api/template-library/templates/import"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-success="handleImportSuccess"
            :on-error="handleImportError"
            style="display: inline-block; margin: 0 8px"
          >
            <el-button type="success">批量导入</el-button>
          </el-upload>
          <el-button type="warning" @click="handleExport">导出 Excel</el-button>
          <el-button @click="handleDownloadTemplate">下载导入模板</el-button>
        </div>
      </div>

      <el-table :data="data" border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="templateName" label="模板名称" min-width="170" />
        <el-table-column prop="inspectType" label="检查类型" width="110">
          <template #default="{ row }">{{ typeText(row.inspectType) }}</template>
        </el-table-column>
        <el-table-column label="检查项数" width="90">
          <template #default="{ row }">{{ itemCount(row.checkItems) }}</template>
        </el-table-column>
        <el-table-column label="权重之和" width="90">
          <template #default="{ row }">{{ weightSum(row.checkItems) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleVersions(row)">版本历史</el-button>
            <el-button link type="primary" @click="handleNewVersion(row)">新建版本</el-button>
            <el-button link type="primary" @click="handleCopy(row)">复制</el-button>
            <el-button link type="primary" @click="handleUsage(row)">使用情况</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" :disabled="row.status !== 1" @click="handleDisable(row)">停用</el-button>
            <el-button link type="success" :disabled="row.status !== 0" @click="handleEnable(row)">启用</el-button>
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

    <!-- 新增 / 编辑模板 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="如 科室自查模板" />
        </el-form-item>
        <el-form-item label="检查类型" prop="inspectType">
          <el-select v-model="form.inspectType" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in inspectTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查项" prop="checkItems">
          <div style="width: 100%">
            <div v-for="(item, idx) in editItems" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-input v-model="item.name" placeholder="检查项名称" style="flex: 2" />
              <el-input-number v-model="item.weight" :min="1" :max="100" placeholder="权重" style="flex: 1" />
              <el-input v-model="item.standard" placeholder="检查标准" style="flex: 2" />
              <el-button link type="danger" @click="editItems.splice(idx, 1)">删除</el-button>
            </div>
            <el-button @click="editItems.push({ name: '', weight: 10, standard: '' })">添加检查项</el-button>
            <span style="margin-left: 12px">权重之和：<b :style="{ color: editWeightSum === 100 ? '#67c23a' : '#f56c6c' }">{{ editWeightSum }}</b>（须为 100）</span>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 版本历史 -->
    <el-dialog v-model="versionDialogVisible" :title="`版本历史：${currentTemplate.templateName || ''}`" width="720px">
      <el-alert v-if="versionTip" :title="versionTip" type="warning" :closable="false" style="margin-bottom: 12px" />
      <el-table :data="versions" border stripe>
        <el-table-column prop="versionNo" label="版本号" width="90" />
        <el-table-column prop="changeRemark" label="版本说明" min-width="180" />
        <el-table-column label="检查项" width="90">
          <template #default="{ row }">{{ itemCount(row.checkItems) }} 项</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '当前版本' : '历史版本' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 1" @click="handleRestore(row)">恢复此版</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 新建版本 -->
    <el-dialog v-model="newVersionDialogVisible" :title="`新建版本：${currentTemplate.templateName || ''}`" width="640px">
      <el-form label-width="110px">
        <el-form-item label="检查项">
          <div style="width: 100%">
            <div v-for="(item, idx) in versionItems" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px">
              <el-input v-model="item.name" placeholder="检查项名称" style="flex: 2" />
              <el-input-number v-model="item.weight" :min="1" :max="100" placeholder="权重" style="flex: 1" />
              <el-input v-model="item.standard" placeholder="检查标准" style="flex: 2" />
              <el-button link type="danger" @click="versionItems.splice(idx, 1)">删除</el-button>
            </div>
            <el-button @click="versionItems.push({ name: '', weight: 10, standard: '' })">添加检查项</el-button>
            <span style="margin-left: 12px">权重之和：<b :style="{ color: versionWeightSum === 100 ? '#67c23a' : '#f56c6c' }">{{ versionWeightSum }}</b>（须为 100）</span>
          </div>
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="versionRemark" type="textarea" :rows="2" placeholder="如：病历完整性权重由 40 调整为 35" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newVersionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitVersion">保存为新版本</el-button>
      </template>
    </el-dialog>

    <!-- 复制模板 -->
    <el-dialog v-model="copyDialogVisible" :title="`复制模板：${currentTemplate.templateName || ''}`" width="480px">
      <el-form label-width="110px">
        <el-form-item label="新模板名称">
          <el-input v-model="copyName" placeholder="如 科室自查模板（2026 版）" />
        </el-form-item>
        <div class="form-tip">复制后新模板默认停用，检查项与原模板一致，版本号从 V1.0 重新开始。</div>
      </el-form>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitCopy">复制</el-button>
      </template>
    </el-dialog>

    <!-- 使用情况 -->
    <el-dialog v-model="usageDialogVisible" :title="`使用情况：${currentTemplate.templateName || ''}`" width="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="检查计划总数">{{ usage.planTotal || 0 }}</el-descriptions-item>
        <el-descriptions-item label="检查记录总数">{{ usage.recordTotal || 0 }}</el-descriptions-item>
        <el-descriptions-item label="待执行">{{ usage.planPending || 0 }}</el-descriptions-item>
        <el-descriptions-item label="进行中">{{ usage.planInProgress || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已完成">{{ usage.planCompleted || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已取消">{{ usage.planCancelled || 0 }}</el-descriptions-item>
        <el-descriptions-item label="最近一次计划">{{ usage.lastPlanDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="有进行中计划">{{ usage.hasActivePlan ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>
      <div class="form-tip" style="margin-top: 12px">有待执行 / 进行中计划的模板不能改版与停用；已被检查计划引用的模板不能直接删除。</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getQualityTemplates, createQualityTemplate, updateQualityTemplate,
  deleteQualityTemplate, enableQualityTemplate, disableQualityTemplate
} from '../../api/quality'
import {
  getTemplateLibraryStats, getTemplateUsage, copyQualityTemplate,
  getTemplateVersions, createTemplateVersion, restoreTemplateVersion,
  exportTemplates, downloadTemplateImportTemplate
} from '../../api/templateLibrary'

// ---------- 字典 ----------
const inspectTypes = [
  { value: 'SELF', label: '科室自查' },
  { value: 'HOSPITAL', label: '院级检查' },
  { value: 'SPECIAL', label: '专项检查' }
]
const statuses = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' }
]

function typeText(v) {
  return (inspectTypes.find((c) => c.value === v) || {}).label || v || ''
}
function statusText(v) {
  return (statuses.find((c) => c.value === v) || {}).label || ''
}

function parseItems(json) {
  try {
    const arr = JSON.parse(json || '[]')
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}
function itemCount(json) {
  return parseItems(json).length
}
function weightSum(json) {
  return parseItems(json).reduce((s, it) => s + (Number(it.weight) || 0), 0)
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

// ---------- 统计 ----------
const stats = ref({})

async function fetchStats() {
  try {
    stats.value = await getTemplateLibraryStats()
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

// ---------- 列表 ----------
const query = reactive({ keyword: '', inspectType: '', status: null, page: 1, size: 10 })
const data = ref([])
const total = ref(0)
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const res = await getQualityTemplates({ ...query })
    data.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { keyword: '', inspectType: '', status: null, page: 1, size: 10 })
  fetchList()
}

function refreshAll() {
  fetchStats()
  fetchList()
}

// ---------- 新增 / 编辑模板 ----------
const dialogVisible = ref(false)
const dialogTitle = ref('新增检查模板')
const formRef = ref(null)
const form = reactive({ id: null, templateName: '', inspectType: 'SELF', remark: '' })
const editItems = ref([])
const editWeightSum = computed(() => editItems.value.reduce((s, it) => s + (Number(it.weight) || 0), 0))
const rules = {
  templateName: [{ required: true, message: '请填写模板名称', trigger: 'blur' }],
  inspectType: [{ required: true, message: '请选择检查类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, { id: null, templateName: '', inspectType: 'SELF', remark: '' })
  editItems.value = [{ name: '', weight: 100, standard: '' }]
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增检查模板'
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, { id: row.id, templateName: row.templateName, inspectType: row.inspectType, remark: row.remark || '' })
  const items = parseItems(row.checkItems)
  editItems.value = items.length ? items.map((it) => ({ name: it.name || '', weight: it.weight || 0, standard: it.standard || '' })) : [{ name: '', weight: 100, standard: '' }]
  dialogTitle.value = '编辑检查模板'
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  if (editWeightSum.value !== 100) {
    ElMessage.error('检查项权重之和必须为 100，当前为 ' + editWeightSum.value)
    return
  }
  const payload = {
    templateName: form.templateName,
    inspectType: form.inspectType,
    checkItems: JSON.stringify(editItems.value),
    remark: form.remark
  }
  try {
    if (form.id) {
      await updateQualityTemplate(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createQualityTemplate(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error((form.id ? '修改失败：' : '新增失败：') + e.message)
  }
}

async function handleEnable(row) {
  try {
    await enableQualityTemplate(row.id)
    ElMessage.success('已启用')
    refreshAll()
  } catch (e) {
    ElMessage.error('启用失败：' + e.message)
  }
}

async function handleDisable(row) {
  try {
    await ElMessageBox.confirm('停用后该模板不能用于新建检查计划，确定停用吗？', '提示', { type: 'warning' })
    await disableQualityTemplate(row.id)
    ElMessage.success('已停用')
    refreshAll()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('停用失败：' + e.message)
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${row.templateName}」吗？已被检查计划引用的模板不能删除。`, '提示', { type: 'warning' })
    await deleteQualityTemplate(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}

// ---------- 版本历史 / 新建版本 / 恢复 ----------
const versionDialogVisible = ref(false)
const newVersionDialogVisible = ref(false)
const currentTemplate = ref({})
const versions = ref([])
const versionTip = ref('')
const versionItems = ref([])
const versionRemark = ref('')
const versionWeightSum = computed(() => versionItems.value.reduce((s, it) => s + (Number(it.weight) || 0), 0))

async function handleVersions(row) {
  currentTemplate.value = row
  try {
    versions.value = await getTemplateVersions(row.id)
    versionTip.value = ''
    versionDialogVisible.value = true
  } catch (e) {
    ElMessage.error('版本历史加载失败：' + e.message)
  }
}

function handleNewVersion(row) {
  currentTemplate.value = row
  const items = parseItems(row.checkItems)
  versionItems.value = items.length ? items.map((it) => ({ name: it.name || '', weight: it.weight || 0, standard: it.standard || '' })) : [{ name: '', weight: 100, standard: '' }]
  versionRemark.value = ''
  newVersionDialogVisible.value = true
}

async function handleSubmitVersion() {
  if (versionWeightSum.value !== 100) {
    ElMessage.error('检查项权重之和必须为 100，当前为 ' + versionWeightSum.value)
    return
  }
  try {
    const res = await createTemplateVersion(currentTemplate.value.id, {
      checkItems: JSON.stringify(versionItems.value),
      changeRemark: versionRemark.value
    })
    ElMessage.success(`新版本 ${res.versionNo} 已创建`)
    newVersionDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('新建版本失败：' + e.message)
  }
}

async function handleRestore(row) {
  try {
    await ElMessageBox.confirm(`确定从 ${row.versionNo} 恢复吗？将以该版本内容新建一版，当前版本改为历史版本。`, '提示', { type: 'warning' })
    const res = await restoreTemplateVersion(row.id, {})
    ElMessage.success(`已恢复，新版本 ${res.versionNo}`)
    versions.value = await getTemplateVersions(currentTemplate.value.id)
    refreshAll()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('恢复失败：' + e.message)
  }
}

// ---------- 复制 ----------
const copyDialogVisible = ref(false)
const copyName = ref('')

function handleCopy(row) {
  currentTemplate.value = row
  copyName.value = `${row.templateName}（副本）`
  copyDialogVisible.value = true
}

async function handleSubmitCopy() {
  if (!copyName.value || !copyName.value.trim()) {
    ElMessage.error('请填写新模板名称')
    return
  }
  try {
    await copyQualityTemplate(currentTemplate.value.id, { templateName: copyName.value.trim() })
    ElMessage.success('复制成功，新模板默认停用')
    copyDialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('复制失败：' + e.message)
  }
}

// ---------- 使用情况 ----------
const usageDialogVisible = ref(false)
const usage = ref({})

async function handleUsage(row) {
  currentTemplate.value = row
  try {
    usage.value = await getTemplateUsage(row.id)
    usageDialogVisible.value = true
  } catch (e) {
    ElMessage.error('使用情况加载失败：' + e.message)
  }
}

// ---------- 导入导出 ----------
async function handleImportSuccess(res) {
  if (res && res.code === 0) {
    ElMessage.success(`导入完成，新增 ${res.data.created} 条，跳过 ${res.data.skipped} 条`)
    refreshAll()
  } else {
    ElMessage.error('导入失败：' + (res && res.msg ? res.msg : '未知错误'))
  }
}

async function handleExport() {
  try {
    const res = await exportTemplates()
    downloadBlob(res.data, '检查模板.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadTemplateImportTemplate()
  downloadBlob(res.data, '检查模板导入模板.xlsx')
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.stat { text-align: center; }
.stat .num { font-size: 22px; font-weight: bold; }
.stat .num.ok { color: #67c23a; }
.stat .num.warn { color: #e6a23c; }
.stat .label { color: #909399; font-size: 12px; margin-top: 4px; }
.search-form { margin-bottom: 12px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 4px; }
.form-tip { color: #909399; font-size: 12px; line-height: 1.6; }
</style>
