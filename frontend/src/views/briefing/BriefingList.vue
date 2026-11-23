<template>
  <div>
    <p class="page-note">口径：需求清单第 30 项“医疗工作简报”。工作简报生成、发布、统计；支持模板、按模板自动生成（从医师档案、工作量、质量检查、MDT、新技术、考核六个模块聚合本期真实数据写正文）、导出 Word（.docx）与真实 PDF（OpenPDF + 嵌入简体中文字体）。发布为一级审批（医务科）；已发布的简报不可修改、不可直接删除。</p>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">简报总数</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.draft || 0 }}</div><div class="label">草稿</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ stats.pending || 0 }}</div><div class="label">待审批</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num ok">{{ stats.published || 0 }}</div><div class="label">已发布</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.rejected || 0 }}</div><div class="label">已驳回</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.templateTotal || 0 }}</div><div class="label">简报模板</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ stats.templateEnabled || 0 }}</div><div class="label">启用模板</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ latestPeriod || '-' }}</div><div class="label">最新一期</div></div></el-card></el-col>
    </el-row>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 简报 -->
      <el-tab-pane label="简报" name="briefing">
        <el-card>
          <el-form :inline="true" :model="bQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="bQuery.keyword" placeholder="期号 / 标题 / 撰稿人" clearable style="width: 200px" /></el-form-item>
            <el-form-item label="分类">
              <el-select v-model="bQuery.category" placeholder="全部" clearable style="width: 130px">
                <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="发布状态">
              <el-select v-model="bQuery.status" placeholder="全部" clearable style="width: 110px">
                <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="统计周期"><el-input v-model="bQuery.period" placeholder="如 2026-11" clearable style="width: 130px" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleBSearch">查询</el-button><el-button @click="handleBReset">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div>
              <el-button type="primary" @click="handleBAdd">新增简报</el-button>
              <el-button type="success" @click="generateDialogVisible = true">按模板自动生成</el-button>
            </div>
            <div>
              <el-upload action="/api/briefings/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleBImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px"><el-button type="success">批量导入</el-button></el-upload>
              <el-button type="warning" @click="handleBExport">导出 Excel</el-button>
              <el-button @click="handleBDownloadTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="bData" border stripe v-loading="bLoading" style="margin-top: 12px" @selection-change="handleBSelection">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="issueNo" label="期号" width="120" />
            <el-table-column prop="title" label="标题" width="260" />
            <el-table-column label="分类" width="100"><template #default="{ row }">{{ categoryText(row.category) }}</template></el-table-column>
            <el-table-column prop="period" label="统计周期" width="100" />
            <el-table-column prop="templateName" label="模板" width="180" />
            <el-table-column label="发布状态" width="90"><template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
            <el-table-column prop="authorName" label="撰稿人" width="100" />
            <el-table-column prop="publishDate" label="发布日期" width="110" />
            <el-table-column label="操作" width="330" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleBDetail(row)">详情</el-button>
                <el-button link type="success" v-if="row.status === 0 || row.status === 3" @click="handleBSubmit(row)">提交审批</el-button>
                <el-button link type="success" v-if="row.status === 1" @click="handleBApprove(row)">通过</el-button>
                <el-button link type="warning" v-if="row.status === 1" @click="handleBReject(row)">驳回</el-button>
                <el-button link type="primary" v-if="row.status === 0 || row.status === 3" @click="handleBEdit(row)">编辑</el-button>
                <el-button link type="primary" @click="handleBExportWord(row)">Word</el-button>
                <el-button link type="primary" @click="handleBExportPdf(row)">PDF</el-button>
                <el-button link type="primary" @click="handleBDownloadText(row)">文本</el-button>
                <el-button link type="danger" v-if="row.status !== 2" @click="handleBDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 12px">
            <el-button type="danger" :disabled="bSelected.length === 0" @click="handleBBatchDelete">批量删除（{{ bSelected.length }}）</el-button>
          </div>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="bQuery.page" v-model:page-size="bQuery.size" :total="bTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchBriefings" @current-change="fetchBriefings" />
        </el-card>
        <el-card style="margin-top: 16px">
          <template #header><span>按分类 / 按统计周期汇总</span></template>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-table :data="byCategoryRows" border stripe size="small">
                <el-table-column prop="categoryText" label="分类" />
                <el-table-column prop="total" label="简报数" width="90" />
                <el-table-column prop="published" label="已发布" width="90" />
              </el-table>
            </el-col>
            <el-col :span="12">
              <el-table :data="byMonthRows" border stripe size="small">
                <el-table-column prop="period" label="统计周期" />
                <el-table-column prop="total" label="简报数" width="90" />
                <el-table-column prop="published" label="已发布" width="90" />
              </el-table>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <!-- 简报模板 -->
      <el-tab-pane label="简报模板" name="template">
        <el-card>
          <el-form :inline="true" :model="tQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="tQuery.keyword" placeholder="模板名称" clearable style="width: 200px" /></el-form-item>
            <el-form-item label="分类">
              <el-select v-model="tQuery.category" placeholder="全部" clearable style="width: 130px">
                <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="启用状态">
              <el-select v-model="tQuery.status" placeholder="全部" clearable style="width: 110px">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="handleTSearch">查询</el-button><el-button @click="handleTReset">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleTAdd">新增模板</el-button></div>
          </div>
          <el-table :data="tData" border stripe v-loading="tLoading" style="margin-top: 12px">
            <el-table-column prop="name" label="模板名称" width="240" />
            <el-table-column label="分类" width="110"><template #default="{ row }">{{ categoryText(row.category) }}</template></el-table-column>
            <el-table-column label="栏目" ><template #default="{ row }">{{ sectionsText(row.sections) }}</template></el-table-column>
            <el-table-column label="启用状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
            <el-table-column prop="remark" label="备注" width="180" />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleTEdit(row)">编辑</el-button>
                <el-button link type="success" v-if="row.status === 0" @click="handleTEnable(row)">启用</el-button>
                <el-button link type="warning" v-if="row.status === 1" @click="handleTDisable(row)">停用</el-button>
                <el-button link type="danger" @click="handleTDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 16px; justify-content: flex-end" v-model:current-page="tQuery.page" v-model:page-size="tQuery.size" :total="tTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" @size-change="fetchTemplates" @current-change="fetchTemplates" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 简报新增/编辑 -->
    <el-dialog v-model="bDialogVisible" :title="bForm.id ? '编辑简报' : '新增简报'" width="760px">
      <el-form :model="bForm" label-width="100px">
        <el-form-item label="期号"><el-input v-model="bForm.issueNo" placeholder="如 2026-11-01，全院唯一" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="bForm.title" placeholder="简报标题" /></el-form-item>
        <el-form-item label="模板">
          <el-select v-model="bForm.templateId" placeholder="请选择模板" clearable style="width: 100%" @change="handleBTemplateChange">
            <el-option v-for="t in enabledTemplates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="bForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计周期"><el-input v-model="bForm.period" placeholder="yyyy-MM，如 2026-11" /></el-form-item>
        <el-form-item label="撰稿人"><el-input v-model="bForm.authorName" placeholder="撰稿人" /></el-form-item>
        <el-form-item label="正文"><el-input v-model="bForm.content" type="textarea" :rows="10" placeholder="简报正文，可手工撰写，或用“按模板自动生成”" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="bForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 简报详情 -->
    <el-dialog v-model="detailDialogVisible" title="简报详情" width="760px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="期号">{{ detail.issueNo }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ categoryText(detail.category) }}</el-descriptions-item>
        <el-descriptions-item label="统计周期">{{ detail.period }}</el-descriptions-item>
        <el-descriptions-item label="模板">{{ detail.templateName }}</el-descriptions-item>
        <el-descriptions-item label="发布状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="撰稿人">{{ detail.authorName }}</el-descriptions-item>
        <el-descriptions-item label="发布日期">{{ detail.publishDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.approver || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ detail.approvalOpinion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="驳回原因" v-if="detail.rejectReason">{{ detail.rejectReason }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>正文</el-divider>
      <div style="white-space: pre-wrap; line-height: 1.8" v-if="detail">{{ detail.content }}</div>
      <div style="margin-top: 12px" v-if="detail && detail.approvalHistory">
        <el-divider>审批记录</el-divider>
        <div style="white-space: pre-wrap; color: #606266">{{ detail.approvalHistory }}</div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleBExportWord(detail)" v-if="detail">导出 Word</el-button>
        <el-button type="primary" @click="handleBExportPdf(detail)" v-if="detail">导出 PDF</el-button>
      </template>
    </el-dialog>

    <!-- 按模板自动生成 -->
    <el-dialog v-model="generateDialogVisible" title="按模板自动生成简报" width="560px">
      <el-form :model="genForm" label-width="100px">
        <el-form-item label="模板">
          <el-select v-model="genForm.templateId" placeholder="请选择已启用的模板" style="width: 100%">
            <el-option v-for="t in enabledTemplates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计周期"><el-input v-model="genForm.period" placeholder="yyyy-MM，如 2026-11" /></el-form-item>
        <el-form-item label="标题（可选）"><el-input v-model="genForm.title" placeholder="为空时按“模板名称（统计周期）”自动生成" /></el-form-item>
        <el-form-item label="期号（可选）"><el-input v-model="genForm.issueNo" placeholder="为空时按“统计周期-序号”自动生成" /></el-form-item>
        <el-form-item label="撰稿人"><el-input v-model="genForm.authorName" placeholder="撰稿人" /></el-form-item>
      </el-form>
      <p style="color: #909399; font-size: 13px">生成后为草稿：正文按模板栏目从医师档案、工作量、质量检查、MDT、新技术、考核聚合本期真实数据，认不出的栏目如实写“暂无自动汇总数据”；生成后请检查正文，提交审批后发布。</p>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 审批 -->
    <el-dialog v-model="approveDialogVisible" title="审批通过" width="480px">
      <el-form :model="approveForm" label-width="90px">
        <el-form-item label="审批人"><el-input v-model="approveForm.approver" placeholder="医务科审批人" /></el-form-item>
        <el-form-item label="审批意见"><el-input v-model="approveForm.opinion" placeholder="同意发布" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleApproveConfirm">通过并发布</el-button>
      </template>
    </el-dialog>

    <!-- 驳回 -->
    <el-dialog v-model="rejectDialogVisible" title="审批驳回" width="480px">
      <el-form :model="rejectForm" label-width="90px">
        <el-form-item label="审批人"><el-input v-model="rejectForm.approver" placeholder="医务科审批人" /></el-form-item>
        <el-form-item label="驳回原因"><el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="驳回原因（必填）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleRejectConfirm">驳回</el-button>
      </template>
    </el-dialog>

    <!-- 模板新增/编辑 -->
    <el-dialog v-model="tDialogVisible" :title="tForm.id ? '编辑模板' : '新增模板'" width="640px">
      <el-form :model="tForm" label-width="100px">
        <el-form-item label="模板名称"><el-input v-model="tForm.name" placeholder="模板名称" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="tForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="栏目">
          <el-input v-model="tSectionsText" type="textarea" :rows="3" placeholder="栏目用顿号分隔，如：工作概况、医疗质量、重点工作、下月计划" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="tForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBriefings, getBriefingStats, getBriefingStatsByCategory, getBriefingStatsByMonth,
  getBriefing, createBriefing, updateBriefing, deleteBriefing, deleteBriefings,
  submitBriefing, approveBriefing, rejectBriefing, generateBriefing,
  exportBriefingWord, exportBriefingPdf, downloadBriefingText,
  exportBriefings, importBriefings, downloadBriefingTemplate,
  getBriefingTemplates, getEnabledBriefingTemplates,
  createBriefingTemplate, updateBriefingTemplate, deleteBriefingTemplate,
  enableBriefingTemplate, disableBriefingTemplate
} from '../../api/briefing'

const categories = [
  { value: 'MONTHLY', label: '月报' },
  { value: 'QUARTERLY', label: '季报' },
  { value: 'ANNUAL', label: '年报' },
  { value: 'SPECIAL', label: '专项简报' },
  { value: 'DYNAMIC', label: '工作动态' }
]
const statuses = [
  { value: 0, label: '草稿' },
  { value: 1, label: '待审批' },
  { value: 2, label: '已发布' },
  { value: 3, label: '已驳回' }
]
const categoryText = (v) => (categories.find((c) => c.value === v) || {}).label || v || ''
const statusText = (v) => (statuses.find((s) => s.value === v) || {}).label || ''
const statusTag = (v) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[v] || 'info')

const activeTab = ref('briefing')
const stats = ref({})
const latestPeriod = ref('')
const byCategoryRows = ref([])
const byMonthRows = ref([])
const enabledTemplates = ref([])

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function showError(action, res) {
  ElMessage.error((action || '操作') + '失败：' + (res.msg || '未知错误'))
}

function sectionsText(sections) {
  try {
    const arr = JSON.parse(sections || '[]')
    return Array.isArray(arr) ? arr.join('、') : (sections || '')
  } catch (e) {
    return sections || ''
  }
}

// ---------- 简报 ----------
const bQuery = reactive({ keyword: '', category: '', status: '', period: '', page: 1, size: 10 })
const bData = ref([])
const bTotal = ref(0)
const bLoading = ref(false)
const bSelected = ref([])
const bDialogVisible = ref(false)
const bForm = reactive({ id: null, issueNo: '', title: '', templateId: '', templateName: '', category: 'MONTHLY', period: '', content: '', authorName: '', remark: '' })
const detailDialogVisible = ref(false)
const detail = ref(null)
const generateDialogVisible = ref(false)
const genForm = reactive({ templateId: '', period: '', title: '', issueNo: '', authorName: '' })
const approveDialogVisible = ref(false)
const approveForm = reactive({ id: null, approver: '', opinion: '同意发布' })
const rejectDialogVisible = ref(false)
const rejectForm = reactive({ id: null, approver: '', reason: '' })

async function fetchStats() {
  try {
    const res = await getBriefingStats()
    if (res.code === 0) stats.value = res.data
    const m = await getBriefingStatsByMonth()
    if (m.code === 0 && m.data.byMonth && m.data.byMonth.length > 0) {
      latestPeriod.value = m.data.byMonth[m.data.byMonth.length - 1].period
      byMonthRows.value = m.data.byMonth
    }
    const c = await getBriefingStatsByCategory()
    if (c.code === 0) byCategoryRows.value = c.data.byCategory
  } catch (e) {
    ElMessage.error('加载统计失败：' + e.message)
  }
}

async function fetchEnabledTemplates() {
  try {
    const res = await getEnabledBriefingTemplates()
    if (res.code === 0) enabledTemplates.value = res.data
  } catch (e) {
    ElMessage.error('加载模板下拉失败：' + e.message)
  }
}

async function fetchBriefings() {
  bLoading.value = true
  try {
    const res = await getBriefings({ ...bQuery })
    if (res.code === 0) {
      bData.value = res.data.records
      bTotal.value = res.data.total
    } else {
      showError('查询简报', res)
    }
  } catch (e) {
    ElMessage.error('查询简报失败：' + e.message)
  } finally {
    bLoading.value = false
  }
}

function handleBSearch() { bQuery.page = 1; fetchBriefings() }
function handleBReset() {
  Object.assign(bQuery, { keyword: '', category: '', status: '', period: '', page: 1, size: 10 })
  fetchBriefings()
}
function handleBSelection(rows) { bSelected.value = rows }
function resetBForm() {
  Object.assign(bForm, { id: null, issueNo: '', title: '', templateId: '', templateName: '', category: 'MONTHLY', period: '', content: '', authorName: '', remark: '' })
}
function handleBAdd() { resetBForm(); bDialogVisible.value = true }
function handleBEdit(row) {
  Object.assign(bForm, { ...row })
  bDialogVisible.value = true
}
function handleBTemplateChange(templateId) {
  const t = enabledTemplates.value.find((x) => x.id === templateId)
  if (t) {
    bForm.templateName = t.name
    bForm.category = t.category
  }
}
async function handleBSave() {
  try {
    const res = bForm.id
      ? await updateBriefing(bForm.id, { ...bForm })
      : await createBriefing({ ...bForm })
    if (res.code === 0) {
      ElMessage.success('保存成功')
      bDialogVisible.value = false
      fetchBriefings()
      fetchStats()
    } else {
      showError('保存简报', res)
    }
  } catch (e) {
    ElMessage.error('保存简报失败：' + e.message)
  }
}
async function handleBDetail(row) {
  try {
    const res = await getBriefing(row.id)
    if (res.code === 0) {
      detail.value = res.data
      detailDialogVisible.value = true
    } else {
      showError('查看详情', res)
    }
  } catch (e) {
    ElMessage.error('查看详情失败：' + e.message)
  }
}
async function handleBSubmit(row) {
  try {
    const res = await submitBriefing(row.id)
    if (res.code === 0) {
      ElMessage.success('已提交审批')
      fetchBriefings()
      fetchStats()
    } else {
      showError('提交审批', res)
    }
  } catch (e) {
    ElMessage.error('提交审批失败：' + e.message)
  }
}
function handleBApprove(row) {
  approveForm.id = row.id
  approveForm.approver = ''
  approveForm.opinion = '同意发布'
  approveDialogVisible.value = true
}
async function handleApproveConfirm() {
  try {
    const res = await approveBriefing(approveForm.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    if (res.code === 0) {
      ElMessage.success('审批通过，已发布')
      approveDialogVisible.value = false
      fetchBriefings()
      fetchStats()
    } else {
      showError('审批通过', res)
    }
  } catch (e) {
    ElMessage.error('审批通过失败：' + e.message)
  }
}
function handleBReject(row) {
  rejectForm.id = row.id
  rejectForm.approver = ''
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}
async function handleRejectConfirm() {
  try {
    const res = await rejectBriefing(rejectForm.id, { approver: rejectForm.approver, reason: rejectForm.reason })
    if (res.code === 0) {
      ElMessage.success('已驳回')
      rejectDialogVisible.value = false
      fetchBriefings()
      fetchStats()
    } else {
      showError('驳回', res)
    }
  } catch (e) {
    ElMessage.error('驳回失败：' + e.message)
  }
}
async function handleBDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除简报 ${row.issueNo} 吗？`, '提示', { type: 'warning' })
    const res = await deleteBriefing(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      fetchBriefings()
      fetchStats()
    } else {
      showError('删除简报', res)
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除简报失败：' + e.message)
  }
}
async function handleBBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${bSelected.value.length} 期简报吗？已发布的简报不能删除。`, '提示', { type: 'warning' })
    const res = await deleteBriefings(bSelected.value.map((r) => r.id))
    if (res.code === 0) {
      ElMessage.success('删除成功')
      fetchBriefings()
      fetchStats()
    } else {
      showError('批量删除', res)
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量删除失败：' + e.message)
  }
}
async function handleGenerate() {
  try {
    const res = await generateBriefing({ ...genForm })
    if (res.code === 0) {
      ElMessage.success(`已按模板生成草稿简报 ${res.data.issueNo}，请检查正文后提交审批`)
      generateDialogVisible.value = false
      Object.assign(genForm, { templateId: '', period: '', title: '', issueNo: '', authorName: '' })
      fetchBriefings()
      fetchStats()
    } else {
      showError('自动生成', res)
    }
  } catch (e) {
    ElMessage.error('自动生成失败：' + e.message)
  }
}
async function handleBExportWord(row) {
  try {
    const res = await exportBriefingWord(row.id)
    downloadBlob(res, `医疗工作简报-${row.issueNo}.docx`)
    ElMessage.success('Word 导出成功')
  } catch (e) {
    ElMessage.error('导出 Word 失败：' + e.message)
  }
}
async function handleBExportPdf(row) {
  try {
    const res = await exportBriefingPdf(row.id)
    downloadBlob(res, `医疗工作简报-${row.issueNo}.pdf`)
    ElMessage.success('PDF 导出成功')
  } catch (e) {
    ElMessage.error('导出 PDF 失败：' + e.message)
  }
}
async function handleBDownloadText(row) {
  try {
    const res = await downloadBriefingText(row.id)
    downloadBlob(res, `医疗工作简报-${row.issueNo}.txt`)
    ElMessage.success('正文下载成功')
  } catch (e) {
    ElMessage.error('下载正文失败：' + e.message)
  }
}
async function handleBExport() {
  try {
    const res = await exportBriefings({ ...bQuery })
    downloadBlob(res, '医疗工作简报.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出 Excel 失败：' + e.message)
  }
}
async function handleBDownloadTemplate() {
  try {
    const res = await downloadBriefingTemplate()
    downloadBlob(res, '医疗工作简报导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载模板失败：' + e.message)
  }
}
function handleBImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入完成：新增 ${res.data.created} 条，跳过 ${res.data.skipped} 条`)
    fetchBriefings()
    fetchStats()
  } else {
    showError('导入', res)
  }
}
function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

// ---------- 简报模板 ----------
const tQuery = reactive({ keyword: '', category: '', status: '', page: 1, size: 10 })
const tData = ref([])
const tTotal = ref(0)
const tLoading = ref(false)
const tDialogVisible = ref(false)
const tForm = reactive({ id: null, name: '', category: 'MONTHLY', sections: '', remark: '' })
const tSectionsText = ref('')

async function fetchTemplates() {
  tLoading.value = true
  try {
    const res = await getBriefingTemplates({ ...tQuery })
    if (res.code === 0) {
      tData.value = res.data.records
      tTotal.value = res.data.total
    } else {
      showError('查询模板', res)
    }
  } catch (e) {
    ElMessage.error('查询模板失败：' + e.message)
  } finally {
    tLoading.value = false
  }
}

function handleTSearch() { tQuery.page = 1; fetchTemplates() }
function handleTReset() {
  Object.assign(tQuery, { keyword: '', category: '', status: '', page: 1, size: 10 })
  fetchTemplates()
}
function resetTForm() {
  Object.assign(tForm, { id: null, name: '', category: 'MONTHLY', sections: '', remark: '' })
  tSectionsText.value = ''
}
function handleTAdd() { resetTForm(); tDialogVisible.value = true }
function handleTEdit(row) {
  Object.assign(tForm, { ...row })
  tSectionsText.value = sectionsText(row.sections)
  tDialogVisible.value = true
}
async function handleTSave() {
  try {
    const sections = tSectionsText.value.split('、').map((s) => s.trim()).filter((s) => s)
    const payload = { ...tForm, sections: JSON.stringify(sections) }
    const res = tForm.id
      ? await updateBriefingTemplate(tForm.id, payload)
      : await createBriefingTemplate(payload)
    if (res.code === 0) {
      ElMessage.success('保存成功')
      tDialogVisible.value = false
      fetchTemplates()
      fetchEnabledTemplates()
      fetchStats()
    } else {
      showError('保存模板', res)
    }
  } catch (e) {
    ElMessage.error('保存模板失败：' + e.message)
  }
}
async function handleTEnable(row) {
  try {
    const res = await enableBriefingTemplate(row.id)
    if (res.code === 0) {
      ElMessage.success('已启用')
      fetchTemplates()
      fetchEnabledTemplates()
      fetchStats()
    } else {
      showError('启用模板', res)
    }
  } catch (e) {
    ElMessage.error('启用模板失败：' + e.message)
  }
}
async function handleTDisable(row) {
  try {
    const res = await disableBriefingTemplate(row.id)
    if (res.code === 0) {
      ElMessage.success('已停用')
      fetchTemplates()
      fetchEnabledTemplates()
      fetchStats()
    } else {
      showError('停用模板', res)
    }
  } catch (e) {
    ElMessage.error('停用模板失败：' + e.message)
  }
}
async function handleTDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除模板 ${row.name} 吗？已被简报引用的模板不能删除。`, '提示', { type: 'warning' })
    const res = await deleteBriefingTemplate(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      fetchTemplates()
      fetchEnabledTemplates()
      fetchStats()
    } else {
      showError('删除模板', res)
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除模板失败：' + e.message)
  }
}

function handleTabChange(tab) {
  if (tab === 'template') fetchTemplates()
  if (tab === 'briefing') fetchBriefings()
}

onMounted(() => {
  fetchStats()
  fetchEnabledTemplates()
  fetchBriefings()
})
</script>

<style scoped>
.page-note { color: #909399; font-size: 13px; margin: 0 0 12px; }
.stat { text-align: center; }
.stat .num { font-size: 24px; font-weight: bold; }
.stat .num.warn { color: #e6a23c; }
.stat .num.ok { color: #67c23a; }
.stat .label { color: #909399; font-size: 13px; margin-top: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 0; }
</style>
