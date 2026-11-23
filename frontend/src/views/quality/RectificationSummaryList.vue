<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.issueCount || 0 }}</div><div class="label">检查问题总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num bad">{{ stats.bySeverity?.MAJOR || 0 }}</div><div class="label">严重问题</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ stats.bySeverity?.MINOR || 0 }}</div><div class="label">一般问题</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.bySeverity?.MILD || 0 }}</div><div class="label">轻微问题</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.rectificationTotal || 0 }}</div><div class="label">整改任务总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.rectificationReviewed || 0 }}</div><div class="label">已复查通过</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.rectificationRate ?? 0 }}%</div><div class="label">整改完成率</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num bad">{{ stats.overdueCount || 0 }}</div><div class="label">逾期整改</div></div></el-card></el-col>
    </el-row>

    <!-- 逾期整改提醒 -->
    <el-card style="margin-bottom: 16px" v-if="overdueList.length">
      <template #header><span>逾期整改提醒</span></template>
      <el-table :data="overdueList" border stripe>
        <el-table-column prop="issueDescription" label="问题描述" min-width="240" />
        <el-table-column prop="severityText" label="严重程度" width="90" />
        <el-table-column prop="deptName" label="责任科室" width="120" />
        <el-table-column prop="measure" label="整改措施" min-width="200" />
        <el-table-column prop="respPerson" label="整改责任人" width="110" />
        <el-table-column prop="deadline" label="整改期限" width="110">
          <template #default="{ row }"><span class="overdue">{{ row.deadline }}</span></template>
        </el-table-column>
        <el-table-column prop="statusText" label="整改状态" width="110" />
      </el-table>
    </el-card>

    <!-- 按科室 / 按检查类型 / 按月汇总 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="8">
        <el-card>
          <template #header><span>按科室汇总</span></template>
          <el-table :data="deptStats" border stripe>
            <el-table-column prop="deptName" label="科室" />
            <el-table-column prop="issueCount" label="问题数" width="70" />
            <el-table-column label="严重/一般/轻微" width="130">
              <template #default="{ row }">{{ row.bySeverity?.MAJOR || 0 }}/{{ row.bySeverity?.MINOR || 0 }}/{{ row.bySeverity?.MILD || 0 }}</template>
            </el-table-column>
            <el-table-column prop="rectificationRate" label="整改完成率" width="100">
              <template #default="{ row }">{{ row.rectificationRate }}%</template>
            </el-table-column>
            <el-table-column prop="overdueCount" label="逾期" width="60" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>按检查类型汇总</span></template>
          <el-table :data="typeStats" border stripe>
            <el-table-column prop="typeText" label="检查类型" />
            <el-table-column prop="issueCount" label="问题数" width="70" />
            <el-table-column label="严重/一般/轻微" width="130">
              <template #default="{ row }">{{ row.bySeverity?.MAJOR || 0 }}/{{ row.bySeverity?.MINOR || 0 }}/{{ row.bySeverity?.MILD || 0 }}</template>
            </el-table-column>
            <el-table-column prop="rectificationRate" label="整改完成率" width="100">
              <template #default="{ row }">{{ row.rectificationRate }}%</template>
            </el-table-column>
            <el-table-column prop="overdueCount" label="逾期" width="60" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>按月汇总</span></template>
          <el-table :data="monthStats" border stripe>
            <el-table-column prop="month" label="月份" />
            <el-table-column prop="issueCount" label="问题数" width="70" />
            <el-table-column prop="rectificationTotal" label="整改任务" width="80" />
            <el-table-column prop="rectificationRate" label="整改完成率" width="100">
              <template #default="{ row }">{{ row.rectificationRate }}%</template>
            </el-table-column>
            <el-table-column prop="overdueCount" label="逾期" width="60" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 汇总报告：搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="统计周期">
          <el-input v-model="query.statPeriod" placeholder="如：2026-11" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="汇总范围">
          <el-select v-model="query.scopeType" placeholder="全部范围" clearable style="width: 130px">
            <el-option v-for="s in scopes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室">
          <el-input v-model="query.deptName" placeholder="如：中医内科" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="检查类型">
          <el-select v-model="query.inspectType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 汇总报告：操作区 + 列表 -->
    <el-card style="margin-top: 16px">
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="generateVisible = true">生成汇总报告</el-button>
          <el-button @click="handleAdd">手工新增</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/rectification-summaries/import"
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
        <el-table-column prop="statPeriod" label="统计周期" width="100" />
        <el-table-column prop="scopeType" label="汇总范围" width="110">
          <template #default="{ row }">{{ scopeText(row.scopeType) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="科室" width="120" />
        <el-table-column prop="inspectType" label="检查类型" width="110">
          <template #default="{ row }">{{ typeText(row.inspectType) }}</template>
        </el-table-column>
        <el-table-column prop="planCount" label="计划数" width="70" />
        <el-table-column prop="recordCount" label="记录数" width="70" />
        <el-table-column prop="avgScore" label="平均分" width="80" />
        <el-table-column prop="issueCount" label="问题数" width="70" />
        <el-table-column label="严重/一般/轻微" width="120">
          <template #default="{ row }">{{ row.majorCount }}/{{ row.minorCount }}/{{ row.mildCount }}</template>
        </el-table-column>
        <el-table-column prop="rectificationTotal" label="整改任务" width="80" />
        <el-table-column prop="rectificationRate" label="整改完成率" width="100">
          <template #default="{ row }">{{ row.rectificationRate }}%</template>
        </el-table-column>
        <el-table-column prop="overdueCount" label="逾期" width="60">
          <template #default="{ row }"><span :class="{ overdue: row.overdueCount > 0 }">{{ row.overdueCount }}</span></template>
        </el-table-column>
        <el-table-column prop="generatedBy" label="生成人" width="90" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleRegenerate(row)">重新生成</el-button>
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

    <!-- 检查问题汇总 -->
    <el-card style="margin-top: 16px">
      <template #header><span>检查问题汇总</span></template>
      <el-form :inline="true" :model="issueQuery" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="issueQuery.keyword" placeholder="问题描述 / 责任人" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="责任科室">
          <el-input v-model="issueQuery.deptName" placeholder="如：骨伤科" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="issueQuery.severity" placeholder="全部程度" clearable style="width: 110px">
            <el-option v-for="s in severities" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查类型">
          <el-select v-model="issueQuery.inspectType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="issueQuery.overdueOnly">只看逾期</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="issueQuery.unrectifiedOnly">只看未完成整改</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleIssueSearch">查询</el-button>
          <el-button @click="handleIssueReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="issueList" border stripe v-loading="issueLoading" style="margin-top: 12px">
        <el-table-column prop="description" label="问题描述" min-width="220" />
        <el-table-column prop="severityText" label="严重程度" width="90" />
        <el-table-column prop="respDeptName" label="责任科室" width="110" />
        <el-table-column prop="respPerson" label="责任人" width="90" />
        <el-table-column prop="inspectTypeText" label="检查类型" width="100" />
        <el-table-column prop="foundDate" label="发现日期" width="110" />
        <el-table-column prop="rectificationStatusText" label="整改状态" width="120" />
        <el-table-column prop="deadline" label="整改期限" width="110">
          <template #default="{ row }"><span :class="{ overdue: row.overdue }">{{ row.deadline }}</span></template>
        </el-table-column>
        <el-table-column prop="measure" label="整改措施" min-width="180" />
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="issueQuery.page"
        v-model:page-size="issueQuery.size"
        :total="issueTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchIssues"
        @current-change="fetchIssues"
      />
    </el-card>

    <!-- 生成汇总报告弹窗 -->
    <el-dialog v-model="generateVisible" title="生成汇总报告" width="520px">
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="统计周期">
          <el-input v-model="generateForm.statPeriod" placeholder="yyyy-MM，如：2026-11" style="width: 220px" />
        </el-form-item>
        <el-form-item label="汇总范围">
          <el-select v-model="generateForm.scopeType" placeholder="请选择" style="width: 220px">
            <el-option v-for="s in scopes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室" v-if="generateForm.scopeType === 'DEPT'">
          <el-input v-model="generateForm.deptName" placeholder="如：中医内科" style="width: 220px" />
        </el-form-item>
        <el-form-item label="检查类型" v-if="generateForm.scopeType === 'TYPE'">
          <el-select v-model="generateForm.inspectType" placeholder="请选择" style="width: 220px">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生成人">
          <el-input v-model="generateForm.generatedBy" placeholder="姓名" style="width: 220px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="generateForm.remark" style="width: 220px" />
        </el-form-item>
      </el-form>
      <div style="color: #909399; font-size: 13px">同一周期同一范围已有报告时，重新生成会用最新数据覆盖原报告。</div>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑汇总报告弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="统计周期" prop="statPeriod">
          <el-input v-model="form.statPeriod" placeholder="yyyy-MM，如：2026-11" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="汇总范围" prop="scopeType">
          <el-select v-model="form.scopeType" placeholder="请选择" :disabled="!!form.id" style="width: 100%">
            <el-option v-for="s in scopes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室" v-if="form.scopeType === 'DEPT'">
          <el-input v-model="form.deptName" placeholder="如：中医内科" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="检查类型" v-if="form.scopeType === 'TYPE'">
          <el-select v-model="form.inspectType" placeholder="请选择" :disabled="!!form.id" style="width: 100%">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查计划数"><el-input-number v-model="form.planCount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="检查记录数"><el-input-number v-model="form.recordCount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="检查平均分"><el-input-number v-model="form.avgScore" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="问题总数"><el-input-number v-model="form.issueCount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="严重问题数"><el-input-number v-model="form.majorCount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="一般问题数"><el-input-number v-model="form.minorCount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="轻微问题数"><el-input-number v-model="form.mildCount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="整改任务数"><el-input-number v-model="form.rectificationTotal" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="待整改数"><el-input-number v-model="form.rectificationPending" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="已整改数"><el-input-number v-model="form.rectificationDone" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="已复查通过数"><el-input-number v-model="form.rectificationReviewed" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="逾期整改数"><el-input-number v-model="form.overdueCount" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="生成人"><el-input v-model="form.generatedBy" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <div style="color: #909399; font-size: 13px">整改完成率由系统按“已复查通过数 / 整改任务数”自动计算，无需手工填写。</div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRectificationSummaries, getRectificationStats, getOverdueRectifications, getIssueSummaries,
  createRectificationSummary, updateRectificationSummary, deleteRectificationSummary,
  batchDeleteRectificationSummaries, generateRectificationSummary,
  exportRectificationSummaries, downloadRectificationTemplate
} from '../../api/rectification'

const scopes = [
  { value: 'ALL', label: '全院' },
  { value: 'DEPT', label: '按科室' },
  { value: 'TYPE', label: '按检查类型' }
]
const types = [
  { value: 'SELF', label: '科室自查' },
  { value: 'HOSPITAL', label: '院级检查' },
  { value: 'SPECIAL', label: '专项检查' }
]
const severities = [
  { value: 'MAJOR', label: '严重' },
  { value: 'MINOR', label: '一般' },
  { value: 'MILD', label: '轻微' }
]
const scopeText = (v) => scopes.find((s) => s.value === v)?.label || v || ''
const typeText = (v) => types.find((t) => t.value === v)?.label || v || ''

const query = reactive({ statPeriod: '', scopeType: '', deptName: '', inspectType: '', page: 1, size: 10 })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const stats = ref({})
const deptStats = ref([])
const typeStats = ref([])
const monthStats = ref([])
const overdueList = ref([])

const issueQuery = reactive({ keyword: '', deptName: '', severity: '', inspectType: '', overdueOnly: false, unrectifiedOnly: false, page: 1, size: 10 })
const issueList = ref([])
const issueTotal = ref(0)
const issueLoading = ref(false)

const generateVisible = ref(false)
const generateForm = reactive({ statPeriod: '', scopeType: 'ALL', deptName: '', inspectType: '', generatedBy: '', remark: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('新增汇总报告')
const formRef = ref(null)
const form = reactive({
  id: null, statPeriod: '', scopeType: 'ALL', deptName: '', inspectType: '',
  planCount: 0, recordCount: 0, avgScore: 0, issueCount: 0, majorCount: 0, minorCount: 0, mildCount: 0,
  rectificationTotal: 0, rectificationPending: 0, rectificationDone: 0, rectificationReviewed: 0,
  overdueCount: 0, generatedBy: '', remark: ''
})
const rules = {
  statPeriod: [
    { required: true, message: '请填写统计周期', trigger: 'blur' },
    { pattern: /^\d{4}-(0[1-9]|1[0-2])$/, message: '统计周期格式应为 yyyy-MM，如 2026-11', trigger: 'blur' }
  ],
  scopeType: [{ required: true, message: '请选择汇总范围', trigger: 'change' }]
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getRectificationSummaries({ ...query })
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
    stats.value = await getRectificationStats()
    deptStats.value = stats.value.byDept || []
    typeStats.value = stats.value.byType || []
    monthStats.value = stats.value.byMonth || []
    const overdue = await getOverdueRectifications({ page: 1, size: 100 })
    overdueList.value = overdue.records || []
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

async function fetchIssues() {
  issueLoading.value = true
  try {
    const data = await getIssueSummaries({ ...issueQuery })
    issueList.value = data.records
    issueTotal.value = data.total
  } catch (e) {
    ElMessage.error('问题汇总加载失败：' + e.message)
  } finally {
    issueLoading.value = false
  }
}

function refresh() {
  fetchList()
  fetchStats()
  fetchIssues()
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { statPeriod: '', scopeType: '', deptName: '', inspectType: '', page: 1, size: 10 })
  fetchList()
}

function handleIssueSearch() {
  issueQuery.page = 1
  fetchIssues()
}

function handleIssueReset() {
  Object.assign(issueQuery, { keyword: '', deptName: '', severity: '', inspectType: '', overdueOnly: false, unrectifiedOnly: false, page: 1, size: 10 })
  fetchIssues()
}

function resetForm() {
  Object.assign(form, {
    id: null, statPeriod: '', scopeType: 'ALL', deptName: '', inspectType: '',
    planCount: 0, recordCount: 0, avgScore: 0, issueCount: 0, majorCount: 0, minorCount: 0, mildCount: 0,
    rectificationTotal: 0, rectificationPending: 0, rectificationDone: 0, rectificationReviewed: 0,
    overdueCount: 0, generatedBy: '', remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增汇总报告'
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, { ...row })
  dialogTitle.value = '编辑汇总报告'
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    const data = { ...form }
    if (data.id) {
      await updateRectificationSummary(data.id, data)
      ElMessage.success('修改成功')
    } else {
      await createRectificationSummary(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleGenerate() {
  if (!generateForm.statPeriod || !/^\d{4}-(0[1-9]|1[0-2])$/.test(generateForm.statPeriod)) {
    ElMessage.warning('请填写正确的统计周期（yyyy-MM，如 2026-11）')
    return
  }
  if (generateForm.scopeType === 'DEPT' && !generateForm.deptName) {
    ElMessage.warning('按科室汇总时请填写科室名称')
    return
  }
  if (generateForm.scopeType === 'TYPE' && !generateForm.inspectType) {
    ElMessage.warning('按检查类型汇总时请选择检查类型')
    return
  }
  try {
    const result = await generateRectificationSummary({ ...generateForm })
    ElMessage.success(`汇总报告已生成：${result.statPeriod} ${scopeText(result.scopeType)}${result.deptName || ''}${typeText(result.inspectType)}，问题 ${result.issueCount} 个，整改完成率 ${result.rectificationRate}%`)
    generateVisible.value = false
    Object.assign(generateForm, { statPeriod: '', scopeType: 'ALL', deptName: '', inspectType: '', generatedBy: '', remark: '' })
    refresh()
  } catch (e) {
    ElMessage.error('生成失败：' + e.message)
  }
}

async function handleRegenerate(row) {
  try {
    await generateRectificationSummary({
      statPeriod: row.statPeriod,
      scopeType: row.scopeType,
      deptName: row.deptName,
      inspectType: row.inspectType,
      generatedBy: row.generatedBy,
      remark: row.remark
    })
    ElMessage.success('已用最新数据重新生成')
    refresh()
  } catch (e) {
    ElMessage.error('重新生成失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除 ${row.statPeriod} ${scopeText(row.scopeType)}${row.deptName || ''} 的汇总报告吗？`, '提示', { type: 'warning' })
  try {
    await deleteRectificationSummary(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (e) {
    ElMessage.error('删除失败：' + e.message)
  }
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 条汇总报告吗？`, '提示', { type: 'warning' })
  try {
    await batchDeleteRectificationSummaries(selected.value.map((r) => r.id))
    ElMessage.success('批量删除成功')
    refresh()
  } catch (e) {
    ElMessage.error('批量删除失败：' + e.message)
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
    const res = await exportRectificationSummaries({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '检查整改汇总.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadRectificationTemplate()
  downloadBlob(res.data, '检查整改汇总导入模板.xlsx')
}

function handleImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data?.imported ?? 0} 条`)
    refresh()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
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
.overdue {
  color: #f56c6c;
  font-weight: bold;
}
</style>
