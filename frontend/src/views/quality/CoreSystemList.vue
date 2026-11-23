<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">制度总数</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.draft || 0 }}</div><div class="label">草稿</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num ok">{{ stats.published || 0 }}</div><div class="label">已发布</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num">{{ stats.abolished || 0 }}</div><div class="label">已废止</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num warn">{{ stats.expiringSoon || 0 }}</div><div class="label">30天内到期</div></div></el-card></el-col>
      <el-col :span="4"><el-card><div class="stat"><div class="num bad">{{ stats.expired || 0 }}</div><div class="label">已过期</div></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="8"><el-card><div class="stat"><div class="num">{{ stats.totalReadings || 0 }}</div><div class="label">查阅总人次</div></div></el-card></el-col>
      <el-col :span="8"><el-card><div class="stat"><div class="num ok">{{ stats.learnedCount || 0 }}</div><div class="label">已学习人次</div></div></el-card></el-col>
      <el-col :span="8"><el-card><div class="stat"><div class="num">{{ stats.readCoreSystemCount || 0 }}</div><div class="label">有查阅记录的制度（部）</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="制度名称 / 制度编号 / 制定部门" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 140px">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用层级">
          <el-select v-model="query.level" placeholder="全部层级" clearable style="width: 110px">
            <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 110px">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用科室">
          <el-input v-model="query.applicableDept" placeholder="如：中医内科" clearable style="width: 140px" />
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
          <el-button type="primary" @click="handleAdd">新增制度</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/core-systems/import"
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
        <el-table-column prop="systemNo" label="制度编号" width="170" />
        <el-table-column prop="title" label="制度名称" min-width="200" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">{{ categoryText(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="level" label="适用层级" width="80">
          <template #default="{ row }">{{ levelText(row.level) }}</template>
        </el-table-column>
        <el-table-column prop="issuingAuthority" label="制定部门" width="140" />
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="applicableDept" label="适用科室" width="130" />
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
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row)">阅读</el-button>
            <el-button link type="primary" :disabled="row.status !== 0" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" :disabled="row.status !== 0" @click="handlePublish(row)">发布</el-button>
            <el-button link type="primary" :disabled="row.status !== 1" @click="handleNewVersion(row)">新建版本</el-button>
            <el-button link type="warning" :disabled="row.status !== 1" @click="handleAbolish(row)">废止</el-button>
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
      <template #header><span>已过期制度提醒</span></template>
      <el-table :data="expiredList" border stripe>
        <el-table-column prop="systemNo" label="制度编号" width="170" />
        <el-table-column prop="title" label="制度名称" />
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
      <template #header><span>按制度分类统计</span></template>
      <el-table :data="categoryStats" border stripe>
        <el-table-column prop="categoryText" label="制度分类" />
        <el-table-column prop="count" label="制度数" />
        <el-table-column prop="publishedCount" label="已发布" />
      </el-table>
    </el-card>

    <!-- 按适用层级统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按适用层级统计</span></template>
      <el-table :data="levelStats" border stripe>
        <el-table-column prop="levelText" label="适用层级" />
        <el-table-column prop="count" label="制度数" />
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
        <el-form-item label="制度名称" prop="title">
          <el-input v-model="form.title" placeholder="如：首诊负责制度" />
        </el-form-item>
        <el-form-item label="制度编号" prop="systemNo">
          <el-input v-model="form.systemNo" placeholder="如：HX-ZD-2024-001" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用层级" prop="level">
          <el-select v-model="form.level" placeholder="请选择适用层级" style="width: 100%">
            <el-option v-for="l in levels" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="制定部门">
          <el-input v-model="form.issuingAuthority" placeholder="如：医务科" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.version" placeholder="如：V1.0" />
        </el-form-item>
        <el-form-item label="适用科室">
          <el-input v-model="form.applicableDept" placeholder="如：全院，或中医内科、骨伤科" />
        </el-form-item>
        <el-form-item label="适用人员">
          <el-input v-model="form.applicablePersonnel" placeholder="如：全体医师" />
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
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="制度正文，用于在线阅读与下载" />
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
    <el-dialog v-model="publishVisible" title="发布制度" width="500px">
      <p>发布「{{ publishRow.title }}」（{{ publishRow.systemNo }} {{ publishRow.version }}）：发布后生效，同一制度编号的旧版本将自动废止。</p>
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

    <!-- 废止弹窗 -->
    <el-dialog v-model="abolishVisible" title="废止制度" width="500px">
      <p>废止「{{ abolishRow.title }}」（{{ abolishRow.systemNo }} {{ abolishRow.version }}）：废止后制度不再有效。</p>
      <el-form :model="abolishForm" label-width="100px">
        <el-form-item label="废止原因">
          <el-input v-model="abolishForm.reason" type="textarea" :rows="3" placeholder="请填写废止原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="abolishVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAbolishSubmit">确认废止</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗：基本信息 + 在线阅读 + 查阅学习记录 -->
    <el-dialog v-model="detailVisible" :title="`制度详情：${detail.title || ''}`" width="850px">
      <el-tabs v-model="detailTab">
        <el-tab-pane label="基本信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="制度名称">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="制度编号">{{ detail.systemNo }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ categoryText(detail.category) }}</el-descriptions-item>
            <el-descriptions-item label="适用层级">{{ levelText(detail.level) }}</el-descriptions-item>
            <el-descriptions-item label="制定部门">{{ detail.issuingAuthority }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
            <el-descriptions-item label="适用科室">{{ detail.applicableDept }}</el-descriptions-item>
            <el-descriptions-item label="适用人员">{{ detail.applicablePersonnel }}</el-descriptions-item>
            <el-descriptions-item label="归口科室">{{ detail.department }}</el-descriptions-item>
            <el-descriptions-item label="起草人">{{ detail.drafter }}</el-descriptions-item>
            <el-descriptions-item label="审批人">{{ detail.approver }}</el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag :type="statusTag(detail.status)">{{ statusText(detail.status) }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="发布日期">{{ detail.publishDate }}</el-descriptions-item>
            <el-descriptions-item label="生效日期">{{ detail.effectiveDate }}</el-descriptions-item>
            <el-descriptions-item label="失效日期">{{ detail.expiryDate }}</el-descriptions-item>
            <el-descriptions-item label="废止原因">{{ detail.abolishReason }}</el-descriptions-item>
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
        <el-tab-pane label="查阅学习记录" name="readings">
          <div v-if="detail.status !== 1" style="margin-bottom: 12px; color: #909399">只有已发布的制度才能登记查阅学习。</div>
          <el-form v-if="detail.status === 1" :inline="true" :model="readingForm" style="margin-bottom: 12px">
            <el-form-item label="查阅人">
              <el-input v-model="readingForm.readerName" placeholder="姓名" style="width: 120px" />
            </el-form-item>
            <el-form-item label="科室">
              <el-input v-model="readingForm.deptName" placeholder="科室" style="width: 120px" />
            </el-form-item>
            <el-form-item label="查阅日期">
              <el-date-picker v-model="readingForm.readDate" type="date" value-format="YYYY-MM-DD" style="width: 150px" />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="readingForm.learnedBool">已完成学习</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAddReading">登记查阅学习</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="readings" border stripe>
            <el-table-column prop="readerName" label="查阅人" width="100" />
            <el-table-column prop="deptName" label="科室" width="120" />
            <el-table-column prop="readDate" label="查阅日期" width="120" />
            <el-table-column prop="learned" label="学习情况" width="100">
              <template #default="{ row }">
                <el-tag :type="row.learned === 1 ? 'success' : 'info'">{{ row.learned === 1 ? '已学习' : '已查阅' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="success" :disabled="row.learned === 1" @click="handleMarkLearned(row)">标记已学习</el-button>
                <el-button link type="danger" @click="handleDeleteReading(row)">删除</el-button>
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
  getCoreSystems, getCoreSystemStats, getCoreSystemStatsByCategory, getCoreSystemStatsByLevel, getCoreSystemStatsByMonth,
  getCoreSystemById, createCoreSystem, updateCoreSystem, deleteCoreSystem,
  batchDeleteCoreSystems, publishCoreSystem, abolishCoreSystem, newVersionCoreSystem,
  downloadCoreSystem, exportCoreSystems, downloadCoreSystemTemplate,
  getCoreSystemReadingsByCoreSystem, createCoreSystemReading, markCoreSystemReadingLearned, deleteCoreSystemReading
} from '../../api/coreSystem'

const categories = [
  { value: 'MEDICAL', label: '医疗' },
  { value: 'NURSING', label: '护理' },
  { value: 'PHARMACY', label: '药事' },
  { value: 'MANAGEMENT', label: '管理' },
  { value: 'SAFETY', label: '安全' },
  { value: 'OTHER', label: '其他' }
]
const levels = [
  { value: 'HOSPITAL', label: '院级' },
  { value: 'DEPT', label: '科室级' }
]
const statuses = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已废止' }
]
const categoryText = (v) => categories.find((c) => c.value === v)?.label || v || ''
const levelText = (v) => levels.find((l) => l.value === v)?.label || v || ''
const statusText = (v) => statuses.find((s) => s.value === v)?.label || ''
const statusTag = (v) => ({ 0: 'info', 1: 'success', 2: 'warning' }[v] || 'info')
const isExpired = (row) => {
  if (!row.expiryDate || row.status !== 1) return false
  return row.expiryDate < new Date().toISOString().slice(0, 10)
}

const query = reactive({ keyword: '', category: '', level: '', status: null, applicableDept: '', expiringSoon: false, expired: false, page: 1, size: 10 })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const stats = ref({})
const categoryStats = ref([])
const levelStats = ref([])
const monthStats = ref([])
const expiredList = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增制度')
const formRef = ref(null)
const form = reactive({
  id: null, title: '', systemNo: '', category: 'MEDICAL', level: 'HOSPITAL', issuingAuthority: '',
  version: 'V1.0', content: '', applicableDept: '全院', applicablePersonnel: '',
  department: '', drafter: '', effectiveDate: '', expiryDate: '', remark: ''
})
const rules = {
  title: [{ required: true, message: '请填写制度名称', trigger: 'blur' }],
  systemNo: [{ required: true, message: '请填写制度编号', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  level: [{ required: true, message: '请选择适用层级', trigger: 'change' }]
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
const readings = ref([])
const readingForm = reactive({ readerName: '', deptName: '', readDate: '', learnedBool: false })

async function fetchList() {
  loading.value = true
  try {
    const data = await getCoreSystems({ ...query })
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
    stats.value = await getCoreSystemStats()
    categoryStats.value = await getCoreSystemStatsByCategory()
    levelStats.value = await getCoreSystemStatsByLevel()
    monthStats.value = await getCoreSystemStatsByMonth()
    expiredList.value = await getCoreSystems({ expired: true, page: 1, size: 100 })
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
  Object.assign(query, { keyword: '', category: '', level: '', status: null, applicableDept: '', expiringSoon: false, expired: false, page: 1, size: 10 })
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, title: '', systemNo: '', category: 'MEDICAL', level: 'HOSPITAL', issuingAuthority: '',
    version: 'V1.0', content: '', applicableDept: '全院', applicablePersonnel: '',
    department: '', drafter: '', effectiveDate: '', expiryDate: '', remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增制度'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑制度'
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateCoreSystem(form.id, { ...form })
      ElMessage.success('制度修改成功')
    } else {
      await createCoreSystem({ ...form })
      ElMessage.success('制度新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除制度「${row.title}」（${row.systemNo} ${row.version}）吗？`, '提示', { type: 'warning' })
  try {
    await deleteCoreSystem(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 部制度吗？已发布的制度不能删除。`, '提示', { type: 'warning' })
  try {
    await batchDeleteCoreSystems(selected.value.map((r) => r.id))
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
    await publishCoreSystem(publishRow.value.id, { ...publishForm })
    ElMessage.success('制度已发布')
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
    ElMessage.warning('请填写废止原因')
    return
  }
  try {
    await abolishCoreSystem(abolishRow.value.id, { ...abolishForm })
    ElMessage.success('制度已废止')
    abolishVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('废止失败：' + e.message)
  }
}

async function handleNewVersion(row) {
  await ElMessageBox.confirm(`为「${row.title}」新建版本吗？将生成 ${nextVersionText(row.version)} 草稿记录，正文与适用范围、归口科室沿用当前版本。`, '提示', { type: 'info' })
  try {
    const next = await newVersionCoreSystem(row.id)
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
    const res = await downloadCoreSystem(row.id)
    downloadBlob(res.data, `${row.systemNo}_${row.version || ''}.txt`)
    ElMessage.success('下载成功')
  } catch (e) {
    ElMessage.error('下载失败：' + e.message)
  }
}

async function handleExport() {
  try {
    const res = await exportCoreSystems({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '政策制度.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadCoreSystemTemplate()
  downloadBlob(res.data, '政策制度导入模板.xlsx')
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

// ---- 详情：基本信息 + 在线阅读 + 查阅学习记录 ----
async function handleDetail(row) {
  try {
    detail.value = await getCoreSystemById(row.id)
    detailTab.value = 'info'
    readings.value = await getCoreSystemReadingsByCoreSystem(row.id)
    Object.assign(readingForm, { readerName: '', deptName: '', readDate: '', learnedBool: false })
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败：' + e.message)
  }
}

async function fetchReadings() {
  readings.value = await getCoreSystemReadingsByCoreSystem(detail.value.id)
}

async function handleAddReading() {
  if (!readingForm.readerName) {
    ElMessage.warning('请填写查阅人姓名')
    return
  }
  try {
    await createCoreSystemReading({
      coreSystemId: detail.value.id,
      readerName: readingForm.readerName,
      deptName: readingForm.deptName,
      readDate: readingForm.readDate || undefined,
      learned: readingForm.learnedBool ? 1 : 0
    })
    ElMessage.success('查阅学习登记成功')
    Object.assign(readingForm, { readerName: '', deptName: '', readDate: '', learnedBool: false })
    fetchReadings()
    fetchStats()
  } catch (e) {
    ElMessage.error('登记失败：' + e.message)
  }
}

async function handleMarkLearned(row) {
  try {
    await markCoreSystemReadingLearned(row.id)
    ElMessage.success('已标记为已学习')
    fetchReadings()
    fetchStats()
  } catch (e) {
    ElMessage.error('标记失败：' + e.message)
  }
}

async function handleDeleteReading(row) {
  await ElMessageBox.confirm(`确定删除「${row.readerName}」的查阅学习记录吗？`, '提示', { type: 'warning' })
  try {
    await deleteCoreSystemReading(row.id)
    ElMessage.success('删除成功')
    fetchReadings()
    fetchStats()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
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
