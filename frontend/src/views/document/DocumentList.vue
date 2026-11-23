<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">文件总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.draft || 0 }}</div><div class="label">草稿</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ stats.published || 0 }}</div><div class="label">已发布</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.abolished || 0 }}</div><div class="label">已作废</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ stats.expiringSoon || 0 }}</div><div class="label">30天内到期</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ stats.expired || 0 }}</div><div class="label">已过期</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="文件标题 / 文件编号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 130px">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.expiringSoon">只看30天内到期</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.expired">只看已过期</el-checkbox>
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
          <el-button type="primary" @click="handleAdd">新增文件</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/documents/import"
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
        <el-table-column prop="docNo" label="文件编号" width="160" />
        <el-table-column prop="title" label="文件标题" min-width="200" />
        <el-table-column prop="category" label="分类" width="80">
          <template #default="{ row }">{{ categoryText(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="department" label="归口科室" width="100" />
        <el-table-column prop="drafter" label="起草人" width="90" />
        <el-table-column prop="publishDate" label="发布日期" width="110" />
        <el-table-column prop="effectiveDate" label="生效日期" width="110" />
        <el-table-column prop="expiryDate" label="失效日期" width="110">
          <template #default="{ row }">
            <span :class="{ expired: isExpired(row) }">{{ row.expiryDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">阅读</el-button>
            <el-button link type="primary" :disabled="row.status !== 0" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" :disabled="row.status !== 0" @click="handlePublish(row)">发布</el-button>
            <el-button link type="primary" :disabled="row.status !== 1" @click="handleNewVersion(row)">新建版本</el-button>
            <el-button link type="warning" :disabled="row.status !== 1" @click="handleAbolish(row)">作废</el-button>
            <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
            <el-button link type="danger" :disabled="row.status === 1" @click="handleDelete(row)">删除</el-button>
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

    <!-- 已过期提醒 -->
    <el-card style="margin-top: 16px" v-if="expiredList.length">
      <template #header><span>已过期文件提醒</span></template>
      <el-table :data="expiredList" border stripe>
        <el-table-column prop="docNo" label="文件编号" width="160" />
        <el-table-column prop="title" label="文件标题" />
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="expiryDate" label="失效日期" width="120">
          <template #default="{ row }"><span class="expired">{{ row.expiryDate }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 按分类统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按文件分类统计</span></template>
      <el-table :data="categoryStats" border stripe>
        <el-table-column prop="categoryText" label="文件分类" />
        <el-table-column prop="count" label="文件数" />
        <el-table-column prop="publishedCount" label="已发布" />
      </el-table>
    </el-card>

    <!-- 按月发布统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按月发布统计</span></template>
      <el-table :data="monthStats" border stripe>
        <el-table-column prop="month" label="发布月份" />
        <el-table-column prop="count" label="发布数" />
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="文件标题" prop="title">
          <el-input v-model="form.title" placeholder="如：三级医师查房制度" />
        </el-form-item>
        <el-form-item label="文件编号" prop="docNo">
          <el-input v-model="form.docNo" placeholder="如：YW-ZD-2024-001" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.version" placeholder="如：V1.0" />
        </el-form-item>
        <el-form-item label="归口科室">
          <el-input v-model="form.department" placeholder="如：医务科" />
        </el-form-item>
        <el-form-item label="起草人">
          <el-input v-model="form.drafter" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="生效日期" style="width: 48%" />
          <span style="margin: 0 8px">失效日期</span>
          <el-date-picker v-model="form.expiryDate" type="date" value-format="YYYY-MM-DD" placeholder="失效日期" style="width: 48%" />
        </el-form-item>
        <el-form-item label="正文内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="文件正文，用于在线阅读与下载" />
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

    <!-- 发布弹窗 -->
    <el-dialog v-model="publishVisible" title="发布文件" width="500px">
      <p>发布「{{ publishRow.title }}」（{{ publishRow.docNo }} {{ publishRow.version }}）：发布后生效，同一编号的旧版本将自动作废。</p>
      <el-form :model="publishForm" label-width="100px">
        <el-form-item label="审批人">
          <el-input v-model="publishForm.approver" placeholder="如：吴文斌" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="publishForm.opinion" placeholder="默认：同意" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePublishSubmit">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- 作废弹窗 -->
    <el-dialog v-model="abolishVisible" title="作废文件" width="500px">
      <p>作废「{{ abolishRow.title }}」（{{ abolishRow.docNo }} {{ abolishRow.version }}）：作废后文件不再有效。</p>
      <el-form :model="abolishForm" label-width="100px">
        <el-form-item label="作废原因">
          <el-input v-model="abolishForm.reason" type="textarea" :rows="3" placeholder="请填写作废原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="abolishVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAbolishSubmit">确认作废</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗：基本信息 + 在线阅读 -->
    <el-dialog v-model="detailVisible" :title="`文件详情：${detail.title || ''}`" width="800px">
      <el-tabs v-model="detailTab">
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="文件标题">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="文件编号">{{ detail.docNo }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ categoryText(detail.category) }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
            <el-descriptions-item label="归口科室">{{ detail.department }}</el-descriptions-item>
            <el-descriptions-item label="起草人">{{ detail.drafter }}</el-descriptions-item>
            <el-descriptions-item label="审批人">{{ detail.approver }}</el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag :type="statusTag(detail.status)">{{ statusText(detail.status) }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="发布日期">{{ detail.publishDate }}</el-descriptions-item>
            <el-descriptions-item label="生效日期">{{ detail.effectiveDate }}</el-descriptions-item>
            <el-descriptions-item label="失效日期">{{ detail.expiryDate }}</el-descriptions-item>
            <el-descriptions-item label="作废原因">{{ detail.abolishReason }}</el-descriptions-item>
            <el-descriptions-item label="审批记录" :span="2">{{ detail.approvalHistory }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="在线阅读" name="read">
          <pre class="doc-content">{{ detail.content || '（暂无正文）' }}</pre>
          <div style="margin-top: 12px; text-align: right">
            <el-button type="primary" @click="handleDownload(detail)">下载正文（.txt）</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDocuments, getDocumentStats, getDocumentStatsByCategory, getDocumentStatsByMonth,
  getDocumentById, createDocument, updateDocument, deleteDocument,
  batchDeleteDocuments, publishDocument, abolishDocument, newVersionDocument,
  downloadDocument, exportDocuments, downloadDocumentTemplate
} from '../../api/document'

const categories = [
  { value: 'SYSTEM', label: '制度' },
  { value: 'STANDARD', label: '规范' },
  { value: 'PROCESS', label: '流程' },
  { value: 'FILE', label: '文件' },
  { value: 'OTHER', label: '其他' }
]
const statuses = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已作废' }
]
const categoryText = (v) => categories.find((c) => c.value === v)?.label || v || ''
const statusText = (v) => statuses.find((s) => s.value === v)?.label || ''
const statusTag = (v) => ({ 0: 'info', 1: 'success', 2: 'warning' }[v] || 'info')
const isExpired = (row) => {
  if (!row.expiryDate || row.status !== 1) return false
  return row.expiryDate < new Date().toISOString().slice(0, 10)
}

const query = reactive({ keyword: '', category: '', status: null, expiringSoon: false, expired: false, page: 1, size: 10 })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const stats = ref({})
const categoryStats = ref([])
const monthStats = ref([])
const expiredList = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增文件')
const formRef = ref(null)
const form = reactive({
  id: null, title: '', docNo: '', category: 'SYSTEM', version: 'V1.0',
  content: '', department: '', drafter: '', effectiveDate: '', expiryDate: '', remark: ''
})
const rules = {
  title: [{ required: true, message: '请填写文件标题', trigger: 'blur' }],
  docNo: [{ required: true, message: '请填写文件编号', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const publishVisible = ref(false)
const publishRow = ref({})
const publishForm = reactive({ approver: '', opinion: '' })

const abolishVisible = ref(false)
const abolishRow = ref({})
const abolishForm = reactive({ reason: '' })

const detailVisible = ref(false)
const detailTab = ref('info')
const detail = ref({})

async function fetchList() {
  loading.value = true
  try {
    const data = await getDocuments({ ...query })
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
    stats.value = await getDocumentStats()
    categoryStats.value = await getDocumentStatsByCategory()
    monthStats.value = await getDocumentStatsByMonth()
    expiredList.value = await getDocuments({ expired: true, page: 1, size: 100 })
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
  Object.assign(query, { keyword: '', category: '', status: null, expiringSoon: false, expired: false, page: 1, size: 10 })
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, title: '', docNo: '', category: 'SYSTEM', version: 'V1.0',
    content: '', department: '', drafter: '', effectiveDate: '', expiryDate: '', remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增文件'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑文件'
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateDocument(form.id, { ...form })
      ElMessage.success('文件修改成功')
    } else {
      await createDocument({ ...form })
      ElMessage.success('文件新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除文件「${row.title}」（${row.docNo} ${row.version}）吗？`, '提示', { type: 'warning' })
  try {
    await deleteDocument(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 个文件吗？已发布的文件不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteDocuments(selected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refresh()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
  }
}

function handlePublish(row) {
  publishRow.value = row
  Object.assign(publishForm, { approver: '', opinion: '' })
  publishVisible.value = true
}

async function handlePublishSubmit() {
  if (!publishForm.approver) {
    ElMessage.warning('请填写审批人')
    return
  }
  try {
    await publishDocument(publishRow.value.id, { ...publishForm })
    ElMessage.success('文件已发布')
    publishVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('发布失败：' + e.message)
  }
}

function handleAbolish(row) {
  abolishRow.value = row
  Object.assign(abolishForm, { reason: '' })
  abolishVisible.value = true
}

async function handleAbolishSubmit() {
  if (!abolishForm.reason) {
    ElMessage.warning('请填写作废原因')
    return
  }
  try {
    await abolishDocument(abolishRow.value.id, { ...abolishForm })
    ElMessage.success('文件已作废')
    abolishVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('作废失败：' + e.message)
  }
}

async function handleNewVersion(row) {
  await ElMessageBox.confirm(`为「${row.title}」新建版本吗？将生成 ${nextVersionText(row.version)} 草稿记录，正文与归口科室沿用当前版本。`, '提示', { type: 'info' })
  try {
    const next = await newVersionDocument(row.id)
    ElMessage.success(`已生成 ${next.version} 版本草稿，请完善后发布`)
    refresh()
  } catch (e) {
    ElMessage.error('新建版本失败：' + e.message)
  }
}

function nextVersionText(version) {
  const m = /^V(\d+)\.(\d+)$/.exec(version || '')
  if (!m) return 'V1.0'
  return `V${Number(m[1]) + 1}.0`
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function handleDownload(row) {
  try {
    const res = await downloadDocument(row.id)
    downloadBlob(res.data, `${row.docNo}_${row.version || ''}.txt`)
    ElMessage.success('下载成功')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}

async function handleExport() {
  try {
    const res = await exportDocuments({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '文件制度.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadDocumentTemplate()
  downloadBlob(res.data, '文件制度导入模板.xlsx')
}

function handleImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条（均为草稿，需走发布审批）`)
    refresh()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---- 详情：基本信息 + 在线阅读 ----
async function handleDetail(row) {
  try {
    detail.value = await getDocumentById(row.id)
    detailTab.value = 'info'
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

onMounted(() => {
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
.expired {
  color: #f56c6c;
  font-weight: bold;
}
.doc-content {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 14px;
  line-height: 1.8;
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 480px;
  overflow-y: auto;
  margin: 0;
}
</style>
