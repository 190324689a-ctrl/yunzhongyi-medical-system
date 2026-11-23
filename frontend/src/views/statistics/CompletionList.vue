<template>
  <div>
    <p class="page-note">口径：需求清单 31 个模块的完成台账现算。接口数 / 测试数为代码实测（各包 @Mapping 注解数与各测试类 @Test 数）；合入其他模块的（第 14/15 项、25/26 项测试）记为空，统计时按 0 计、不虚增。</p>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">模块总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.completed || 0 }}</div><div class="label">已完成</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ stats.inProgress || 0 }}</div><div class="label">进行中</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.pending || 0 }}</div><div class="label">待开发</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.completionRate || 0 }}%</div><div class="label">完成率</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.totalInterfaces || 0 }}</div><div class="label">接口总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.totalTests || 0 }}</div><div class="label">测试总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.avgProgress || 0 }}%</div><div class="label">平均进度</div></div></el-card></el-col>
    </el-row>

    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="模块名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="query.category" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <div class="toolbar">
        <div><el-button type="primary" @click="handleAdd">新增模块</el-button></div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload action="/api/module-completions/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px">
            <el-button type="success">批量导入</el-button>
          </el-upload>
          <el-button type="warning" @click="handleExport">导出 Excel</el-button>
          <el-button @click="handleDownloadTemplate">下载模板</el-button>
        </div>
      </div>
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="selected = $event" style="margin-top: 12px">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="moduleNo" label="编号" width="70" />
        <el-table-column prop="moduleName" label="模块名称" width="190" />
        <el-table-column prop="category" label="分组" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="完成进度" width="170">
          <template #default="{ row }"><el-progress :percentage="row.progress || 0" :status="row.progress === 100 ? 'success' : ''" /></template>
        </el-table-column>
        <el-table-column label="接口数" width="90">
          <template #default="{ row }">{{ row.interfaceCount == null ? '计入他项' : row.interfaceCount }}</template>
        </el-table-column>
        <el-table-column label="测试数" width="90">
          <template #default="{ row }">{{ row.testCount == null ? '计入他项' : row.testCount }}</template>
        </el-table-column>
        <el-table-column prop="completedDate" label="完成日期" width="120" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" v-if="row.status !== 2" @click="handleComplete(row)">标记完成</el-button>
            <el-button link type="warning" v-if="row.status === 2" @click="handleReopen(row)">重新开始</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchList" @current-change="fetchList" />
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card>
          <template #header><span>按分组统计</span></template>
          <el-table :data="byCategory" border stripe>
            <el-table-column prop="category" label="分组" />
            <el-table-column prop="total" label="模块数" />
            <el-table-column prop="completed" label="已完成" />
            <el-table-column prop="inProgress" label="进行中" />
            <el-table-column prop="pending" label="待开发" />
            <el-table-column label="完成率">
              <template #default="{ row }">{{ row.completionRate }}%</template>
            </el-table-column>
            <el-table-column prop="totalInterfaces" label="接口数" />
            <el-table-column prop="totalTests" label="测试数" />
            <el-table-column label="平均进度">
              <template #default="{ row }">{{ row.avgProgress }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>按完成月份统计</span></template>
          <el-table :data="byMonth" border stripe>
            <el-table-column prop="month" label="完成月份" />
            <el-table-column prop="completedCount" label="完成模块数" />
          </el-table>
          <p class="page-note" style="margin-top: 12px">待开发：医务管理移动端。</p>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模块编号"><el-input-number v-model="form.moduleNo" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="模块名称"><el-input v-model="form.moduleName" placeholder="如 医务系统完成统计" /></el-form-item>
        <el-form-item label="分组">
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="完成进度"><el-input-number v-model="form.progress" :min="0" :max="100" style="width: 100%" /></el-form-item>
        <el-form-item label="接口数"><el-input-number v-model="form.interfaceCount" :min="0" placeholder="计入他项请留空" style="width: 100%" /></el-form-item>
        <el-form-item label="测试数"><el-input-number v-model="form.testCount" :min="0" placeholder="计入他项请留空" style="width: 100%" /></el-form-item>
        <el-form-item label="完成日期"><el-date-picker v-model="form.completedDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getModuleCompletions, getModuleCompletionStats, getModuleCompletionStatsByCategory,
  getModuleCompletionStatsByMonth, createModuleCompletion, updateModuleCompletion,
  deleteModuleCompletion, batchDeleteModuleCompletions, completeModule, reopenModule,
  exportModuleCompletions, downloadModuleCompletionTemplate
} from '../../api/completion'

const categories = [
  { value: '医师管理', label: '医师管理' },
  { value: '组织与制度', label: '组织与制度' },
  { value: '质量管理', label: '质量管理' },
  { value: '科研与统计', label: '科研与统计' },
  { value: '系统与移动', label: '系统与移动' }
]
const statuses = [
  { value: 0, label: '待开发' },
  { value: 1, label: '进行中' },
  { value: 2, label: '已完成' }
]

function statusText(v) {
  return (statuses.find((c) => c.value === v) || {}).label || ''
}
function statusTag(v) {
  return { 0: 'info', 1: 'warning', 2: 'success' }[v] || 'info'
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
const byCategory = ref([])
const byMonth = ref([])

async function fetchStats() {
  try {
    const res = await getModuleCompletionStats()
    stats.value = res.data || {}
  } catch (e) {
    ElMessage.error('加载统计失败：' + e.message)
  }
  try {
    const res = await getModuleCompletionStatsByCategory()
    byCategory.value = res.data || []
  } catch (e) {
    ElMessage.error('加载分组统计失败：' + e.message)
  }
  try {
    const res = await getModuleCompletionStatsByMonth()
    byMonth.value = res.data || []
  } catch (e) {
    ElMessage.error('加载月度统计失败：' + e.message)
  }
}

// ---------- 列表 ----------
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const query = reactive({ keyword: '', category: '', status: '', page: 1, size: 10 })

async function fetchList() {
  loading.value = true
  try {
    const res = await getModuleCompletions({ ...query })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
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
  query.keyword = ''
  query.category = ''
  query.status = ''
  query.page = 1
  fetchList()
}

// ---------- 新增 / 编辑 ----------
const dialogVisible = ref(false)
const dialogTitle = ref('新增模块')
const form = reactive({
  id: null, moduleNo: null, moduleName: '', category: '', status: 0,
  progress: 0, interfaceCount: null, testCount: null, completedDate: '', remark: ''
})

function resetForm() {
  form.id = null
  form.moduleNo = null
  form.moduleName = ''
  form.category = ''
  form.status = 0
  form.progress = 0
  form.interfaceCount = null
  form.testCount = null
  form.completedDate = ''
  form.remark = ''
}
function handleAdd() {
  resetForm()
  dialogTitle.value = '新增模块'
  dialogVisible.value = true
}
function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑模块'
  dialogVisible.value = true
}
async function handleSubmit() {
  try {
    if (form.id) {
      await updateModuleCompletion(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createModuleCompletion({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

// ---------- 标记完成 / 重新开始 ----------
async function handleComplete(row) {
  try {
    await completeModule(row.id)
    ElMessage.success('已标记完成')
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error('标记完成失败：' + e.message)
  }
}
async function handleReopen(row) {
  try {
    await ElMessageBox.confirm('该模块将回到进行中，进度置 50%，完成日期清空，确定吗？', '重新开始', { type: 'warning' })
    await reopenModule(row.id)
    ElMessage.success('已重新开始')
    fetchList()
    fetchStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败：' + e.message)
  }
}

// ---------- 删除 / 导入导出 ----------
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.moduleName}」吗？`, '删除', { type: 'warning' })
    await deleteModuleCompletion(row.id)
    ElMessage.success('删除成功')
    fetchList()
    fetchStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}
async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条记录吗？`, '批量删除', { type: 'warning' })
    await batchDeleteModuleCompletions(selected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    selected.value = []
    fetchList()
    fetchStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量删除失败：' + e.message)
  }
}
async function handleExport() {
  try {
    const res = await exportModuleCompletions({ ...query })
    downloadBlob(res, '模块完成情况.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleDownloadTemplate() {
  try {
    const res = await downloadModuleCompletionTemplate()
    downloadBlob(res, '模块完成情况导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}
function handleImportSuccess(res) {
  if (res && res.code === 0) {
    ElMessage.success(`导入完成：新增 ${res.data.created} 条，跳过 ${res.data.skipped} 条`)
    fetchList()
    fetchStats()
  } else {
    ElMessage.error('导入失败：' + (res ? res.message : '未知错误'))
  }
}

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.stat { text-align: center; }
.stat .num { font-size: 26px; font-weight: bold; }
.stat .num.ok { color: #67c23a; }
.stat .num.warn { color: #e6a23c; }
.stat .label { color: #909399; margin-top: 4px; }
.page-note { color: #909399; font-size: 13px; margin: 0 0 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 0; }
</style>
