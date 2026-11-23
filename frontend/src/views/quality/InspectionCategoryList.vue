<template>
  <div>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">分类总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.enabled || 0 }}</div><div class="label">已启用</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.disabled || 0 }}</div><div class="label">已停用</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.level1 || 0 }}</div><div class="label">一级分类</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.level2 || 0 }}</div><div class="label">二级分类</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.itemTotal || 0 }}</div><div class="label">归属检查项目</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.categoriesWithItems || 0 }}</div><div class="label">有项目归属的分类</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ stats.categoriesWithoutItems || 0 }}</div><div class="label">无项目归属的分类</div></div></el-card></el-col>
    </el-row>
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="分类名称 / 分类编码" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="层级">
          <el-select v-model="query.level" placeholder="全部层级" clearable style="width: 120px">
            <el-option :value="1" label="一级分类" />
            <el-option :value="2" label="二级分类" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用检查类型">
          <el-select v-model="query.applicableType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in applicableTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 110px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.withItems">只看有项目归属</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.withoutItems">只看无项目归属</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="handleAdd">新增检查分类</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/inspection-categories/import"
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

      <el-table :data="data" border stripe v-loading="loading" @selection-change="selected = $event" style="margin-top: 12px">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="categoryCode" label="分类编码" width="190" />
        <el-table-column prop="categoryName" label="分类名称" min-width="130" />
        <el-table-column prop="parentName" label="上级分类" width="120">
          <template #default="{ row }">{{ row.parentName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="90">
          <template #default="{ row }"><el-tag :type="row.level === 1 ? '' : 'warning'">{{ row.level === 1 ? '一级' : '二级' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="applicableType" label="适用检查类型" width="120">
          <template #default="{ row }">{{ applicableTypeText(row.applicableType) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="分类说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
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

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card>
          <template #header><span>按分类汇总统计（项目数 / 涉及模板数 / 下级数）</span></template>
          <el-table :data="byCategoryStats" border stripe>
            <el-table-column prop="categoryName" label="分类名称" />
            <el-table-column prop="categoryCode" label="分类编码" width="190" />
            <el-table-column label="层级" width="80">
              <template #default="{ row }">{{ row.level === 1 ? '一级' : '二级' }}</template>
            </el-table-column>
            <el-table-column label="适用检查类型" width="120">
              <template #default="{ row }">{{ applicableTypeText(row.applicableType) }}</template>
            </el-table-column>
            <el-table-column prop="itemCount" label="项目数" width="80" />
            <el-table-column prop="enabledItemCount" label="已启用项目" width="100" />
            <el-table-column prop="templateCount" label="涉及模板" width="90" />
            <el-table-column prop="childCount" label="下级数" width="80" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>分类树（一级分类带下级）</span></template>
          <el-tree :data="treeData" :props="{ children: 'children', label: 'categoryName' }" default-expand-all>
            <template #default="{ data }">
              <span>{{ data.categoryName }}（{{ data.categoryCode }}）</span>
            </template>
          </el-tree>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="如 MEDICAL_RECORD，提交后已有项目归属的分类不可再改" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如 病历质量" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" placeholder="不选为一级分类" clearable style="width: 100%">
            <el-option v-for="c in parentOptions" :key="c.id" :label="`${c.categoryName}（${c.categoryCode}）`" :value="c.id" />
          </el-select>
          <div class="form-tip">只有一级分类能做上级，不建三级分类；已有下级的分类不可改为二级。</div>
        </el-form-item>
        <el-form-item label="适用检查类型" prop="applicableType">
          <el-select v-model="form.applicableType" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in applicableTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="该分类覆盖的检查内容" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="1" style="width: 100%" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInspectionCategories, getInspectionCategoryStats, getInspectionCategoryStatsByCategory,
  getInspectionCategoryTree, createInspectionCategory, updateInspectionCategory,
  deleteInspectionCategory, batchDeleteInspectionCategories,
  enableInspectionCategory, disableInspectionCategory,
  exportInspectionCategories, downloadInspectionCategoryTemplate
} from '../../api/inspection'

// ---------- 字典 ----------
const applicableTypes = [
  { value: 'ALL', label: '通用' },
  { value: 'SELF', label: '科室自查' },
  { value: 'HOSPITAL', label: '院级检查' },
  { value: 'SPECIAL', label: '专项检查' }
]
const statuses = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' }
]

function applicableTypeText(v) {
  return (applicableTypes.find((c) => c.value === v) || {}).label || v || ''
}
function statusText(v) {
  return (statuses.find((c) => c.value === v) || {}).label || ''
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
const byCategoryStats = ref([])
const treeData = ref([])

async function fetchStats() {
  try {
    stats.value = await getInspectionCategoryStats()
    byCategoryStats.value = await getInspectionCategoryStatsByCategory()
    treeData.value = await getInspectionCategoryTree()
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

// ---------- 列表 ----------
const query = reactive({ keyword: '', level: null, applicableType: '', status: null, withItems: false, withoutItems: false, page: 1, size: 10 })
const data = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const parentOptions = ref([])

async function fetchList() {
  loading.value = true
  try {
    const res = await getInspectionCategories({ ...query })
    data.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error('加载失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function fetchParentOptions() {
  try {
    const res = await getInspectionCategories({ level: 1, page: 1, size: 10000 })
    parentOptions.value = res.records || []
  } catch (e) {
    console.warn('上级分类下拉加载失败', e.message)
  }
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { keyword: '', level: null, applicableType: '', status: null, withItems: false, withoutItems: false, page: 1, size: 10 })
  fetchList()
}

function refreshAll() {
  fetchStats()
  fetchList()
  fetchParentOptions()
}

// ---------- 新增 / 编辑 ----------
const dialogVisible = ref(false)
const dialogTitle = ref('新增检查分类')
const formRef = ref(null)
const form = reactive({
  id: null, categoryCode: '', categoryName: '', parentId: null, applicableType: 'ALL',
  description: '', sortOrder: 1, remark: ''
})
const rules = {
  categoryCode: [{ required: true, message: '请填写分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请填写分类名称', trigger: 'blur' }],
  applicableType: [{ required: true, message: '请选择适用检查类型', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    id: null, categoryCode: '', categoryName: '', parentId: null, applicableType: 'ALL',
    description: '', sortOrder: 1, remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增检查分类'
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, {
    id: row.id, categoryCode: row.categoryCode, categoryName: row.categoryName,
    parentId: row.parentId, applicableType: row.applicableType || 'ALL',
    description: row.description || '', sortOrder: row.sortOrder || 1, remark: row.remark || ''
  })
  dialogTitle.value = '编辑检查分类'
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    const payload = { ...form }
    if (payload.id) {
      await updateInspectionCategory(payload.id, payload)
    } else {
      await createInspectionCategory(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    refreshAll()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

// ---------- 启用 / 停用 / 删除 ----------
async function handleEnable(row) {
  try {
    await enableInspectionCategory(row.id)
    ElMessage.success('启用成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('启用失败：' + e.message)
  }
}

async function handleDisable(row) {
  await ElMessageBox.confirm(`确定停用检查分类「${row.categoryName}」吗？已有检查项目归属、或有启用下级的分类不可停用。`, '提示', { type: 'warning' })
  try {
    await disableInspectionCategory(row.id)
    ElMessage.success('停用成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('停用失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除检查分类「${row.categoryName}」（${row.categoryCode}）吗？已有下级、或已有检查项目归属的分类不能删除。`, '提示', { type: 'warning' })
  try {
    await deleteInspectionCategory(row.id)
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 个检查分类吗？已有下级、或已有检查项目归属的分类不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteInspectionCategories(selected.value.map((r) => r.id))
    ElMessage.success('删除成功')
    refreshAll()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

// ---------- 导入导出 ----------
async function handleImportSuccess(res) {
  if (res && res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
    refreshAll()
  } else {
    ElMessage.error('导入失败：' + (res && res.msg ? res.msg : '未知错误'))
  }
}

async function handleExport() {
  try {
    const res = await exportInspectionCategories({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '检查分类.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadInspectionCategoryTemplate()
  downloadBlob(res.data, '检查分类导入模板.xlsx')
}

onMounted(() => {
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
  align-items: center;
  margin: 12px 0;
}
.stat {
  text-align: center;
}
.stat .num {
  font-size: 24px;
  font-weight: bold;
}
.stat .num.ok {
  color: #67c23a;
}
.stat .num.warn {
  color: #e6a23c;
}
.stat .label {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
</style>
