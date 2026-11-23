<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">申请总数</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ stats.pending || 0 }}</div><div class="label">待审批（科主任 {{ stats.pendingDept || 0 }} / 医务科 {{ stats.pendingMedical || 0 }}）</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num ok">{{ stats.approved || 0 }}</div><div class="label">已通过</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num bad">{{ stats.rejected || 0 }}</div><div class="label">已驳回</div></div></el-card></el-col>
    </el-row>

    <!-- 搜索区 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="申请人">
          <el-input v-model="query.keyword" placeholder="姓名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="query.deptId" placeholder="全部科室" clearable style="width: 150px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权类型">
          <el-select v-model="query.authType" placeholder="全部类型" clearable style="width: 170px">
            <el-option v-for="t in authTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option :value="0" label="待科主任审批" />
            <el-option :value="1" label="待医务科审批" />
            <el-option :value="2" label="已通过" />
            <el-option :value="3" label="已驳回" />
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
        <div><el-button type="primary" @click="handleAdd">在线申请</el-button></div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/authorizations/import"
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
        <el-table-column prop="physicianName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="科室" width="110" />
        <el-table-column prop="authType" label="授权类型" width="140">
          <template #default="{ row }">{{ typeText(row.authType) }}</template>
        </el-table-column>
        <el-table-column label="手术分级" width="90">
          <template #default="{ row }">{{ row.surgeryLevel ? row.surgeryLevel + '级' : '—' }}</template>
        </el-table-column>
        <el-table-column label="授权有效期" width="210">
          <template #default="{ row }">{{ row.validFrom || '—' }} 至 {{ row.validTo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="审批状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentApprover" label="当前审批" width="100" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handlePrint(row)">打印预览</el-button>
            <el-button v-if="row.status === 0 || row.status === 1" link type="success" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 0 || row.status === 1" link type="warning" @click="handleReject(row)">驳回</el-button>
            <el-button v-if="row.status === 0 || row.status === 1" link type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="申请人" prop="physicianId">
              <el-select v-model="form.physicianId" placeholder="请选择医师" style="width: 100%" @change="onPhysicianChange">
                <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="授权类型" prop="authType">
              <el-select v-model="form.authType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in authTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="授权有效期">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手术分级" v-if="form.authType === 'SURGERY'">
              <el-select v-model="form.surgeryLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option :value="1" label="一级" />
                <el-option :value="2" label="二级" />
                <el-option :value="3" label="三级" />
                <el-option :value="4" label="四级" />
              </el-select>
            </el-form-item>
            <el-form-item label="申请事由" v-else>
              <el-input v-model="form.reason" placeholder="申请事由" />
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.authType === 'SURGERY'">
            <el-form-item label="申请事由"><el-input v-model="form.reason" type="textarea" :rows="2" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 审批意见弹窗 -->
    <el-dialog :title="approvalTitle" v-model="approvalVisible" width="480px">
      <el-form label-width="90px">
        <el-form-item label="审批人"><el-input v-model="approvalForm.approver" placeholder="如：科主任 / 医务科" /></el-form-item>
        <el-form-item label="审批意见"><el-input v-model="approvalForm.opinion" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval">确定</el-button>
      </template>
    </el-dialog>

    <!-- 打印预览弹窗 -->
    <el-dialog title="打印预览" v-model="printVisible" width="640px">
      <div v-if="printRow" class="print-area">
        <h3 style="text-align: center">重点授权申请表</h3>
        <table class="print-table">
          <tr><td>申请人</td><td>{{ printRow.physicianName }}</td><td>科室</td><td>{{ printRow.deptName }}</td></tr>
          <tr><td>授权类型</td><td>{{ typeText(printRow.authType) }}</td><td>手术分级</td><td>{{ printRow.surgeryLevel ? printRow.surgeryLevel + '级' : '—' }}</td></tr>
          <tr><td>授权有效期</td><td>{{ printRow.validFrom }} 至 {{ printRow.validTo }}</td><td>申请日期</td><td>{{ printRow.applyDate }}</td></tr>
          <tr><td>申请事由</td><td colspan="3">{{ printRow.reason || '—' }}</td></tr>
          <tr><td>审批状态</td><td>{{ statusText(printRow.status) }}</td><td>当前审批</td><td>{{ printRow.currentApprover || '—' }}</td></tr>
          <tr><td>审批记录</td><td colspan="3">{{ printRow.approvalHistory || '—' }}</td></tr>
        </table>
      </div>
      <template #footer>
        <el-button @click="printVisible = false">关闭</el-button>
        <el-button type="primary" @click="doPrint">打印</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAuthorizations, getAuthorizationStats, createAuthorization, updateAuthorization,
  deleteAuthorization, batchDeleteAuthorizations, approveAuthorization, rejectAuthorization,
  exportAuthorizations, downloadAuthorizationTemplate
} from '../../api/authorization'
import { getEnabledDepts } from '../../api/system'
import { getPhysicians } from '../../api/physician'

const authTypes = [
  { value: 'SURGERY', label: '手术分级授权' },
  { value: 'ANESTHESIA', label: '麻醉药品处方权' },
  { value: 'ANTITUMOR', label: '抗肿瘤处方权' },
  { value: 'PSYCHOTROPIC', label: '精神药品处方权' }
]
const typeText = (v) => authTypes.find((t) => t.value === v)?.label || v || ''
const statusText = (s) => ({ 0: '待科主任审批', 1: '待医务科审批', 2: '已通过', 3: '已驳回' }[s] || '')
const statusTag = (s) => ({ 0: 'warning', 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')

const query = reactive({ keyword: '', deptId: null, authType: '', status: null, page: 1, size: 10 })
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const depts = ref([])
const physicians = ref([])
const stats = ref({})

const dialogVisible = ref(false)
const dialogTitle = ref('在线申请')
const formRef = ref(null)
const form = reactive({
  id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
  authType: 'SURGERY', surgeryLevel: null, validFrom: '', validTo: '',
  reason: '', remark: ''
})
const dateRange = ref([])
const rules = {
  physicianId: [{ required: true, message: '请选择申请人', trigger: 'change' }],
  authType: [{ required: true, message: '请选择授权类型', trigger: 'change' }]
}

const approvalVisible = ref(false)
const approvalTitle = ref('审批通过')
const approvalAction = ref('approve')
const approvalRow = ref(null)
const approvalForm = reactive({ approver: '', opinion: '' })

const printVisible = ref(false)
const printRow = ref(null)

async function fetchList() {
  loading.value = true
  try {
    const data = await getAuthorizations({ ...query })
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
    stats.value = await getAuthorizationStats()
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
  Object.assign(query, { keyword: '', deptId: null, authType: '', status: null, page: 1, size: 10 })
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
    authType: 'SURGERY', surgeryLevel: null, validFrom: '', validTo: '',
    reason: '', remark: ''
  })
  dateRange.value = []
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '在线申请'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  dateRange.value = row.validFrom && row.validTo ? [row.validFrom, row.validTo] : []
  dialogTitle.value = '编辑申请'
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

async function handleSubmit() {
  await formRef.value.validate()
  form.validFrom = dateRange.value?.[0] || ''
  form.validTo = dateRange.value?.[1] || ''
  try {
    if (form.id) {
      await updateAuthorization(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createAuthorization({ ...form })
      ElMessage.success('申请已提交，待科主任审批')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.physicianName}」的${typeText(row.authType)}申请吗？`, '提示', { type: 'warning' })
  await deleteAuthorization(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 条申请吗？`, '提示', { type: 'warning' })
  await batchDeleteAuthorizations(selected.value.map((r) => r.id))
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
    const res = await exportAuthorizations({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '重点授权.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadAuthorizationTemplate()
  downloadBlob(res.data, '重点授权导入模板.xlsx')
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

function openApproval(row, action) {
  approvalRow.value = row
  approvalAction.value = action
  approvalTitle.value = action === 'approve' ? '审批通过' : '审批驳回'
  approvalForm.approver = row.status === 0 ? '科主任' : '医务科'
  approvalForm.opinion = action === 'approve' ? '同意' : ''
  approvalVisible.value = true
}

function handleApprove(row) {
  openApproval(row, 'approve')
}

function handleReject(row) {
  openApproval(row, 'reject')
}

async function submitApproval() {
  const row = approvalRow.value
  try {
    if (approvalAction.value === 'approve') {
      await approveAuthorization(row.id, { ...approvalForm })
      ElMessage.success('已通过本级审批')
    } else {
      if (!approvalForm.opinion) {
        ElMessage.warning('驳回请填写审批意见')
        return
      }
      await rejectAuthorization(row.id, { ...approvalForm })
      ElMessage.success('已驳回')
    }
    approvalVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

function handlePrint(row) {
  printRow.value = row
  printVisible.value = true
}

function doPrint() {
  window.print()
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
.print-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
}
.print-table td {
  border: 1px solid #dcdfe6;
  padding: 8px 10px;
  font-size: 14px;
}
.print-table td:nth-child(odd) {
  background: #f5f7fa;
  width: 110px;
}
@media print {
  body * {
    visibility: hidden;
  }
  .print-area, .print-area * {
    visibility: visible;
  }
  .print-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
}
</style>
