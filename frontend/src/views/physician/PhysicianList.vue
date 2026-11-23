<template>
  <div>
    <!-- 搜索区：多条件组合查询 -->
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="姓名 / 执业证号 / 手机号" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="query.deptId" placeholder="全部科室" clearable style="width: 150px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称">
          <el-select v-model="query.title" placeholder="全部职称" clearable style="width: 130px">
            <el-option v-for="t in titles" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="最高学历">
          <el-select v-model="query.education" placeholder="全部学历" clearable style="width: 110px">
            <el-option v-for="e in educations" :key="e.value" :label="e.label" :value="e.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="人员类别">
          <el-select v-model="query.staffType" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="s in staffTypes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="手术分级">
          <el-select v-model="query.surgeryLevel" placeholder="全部" clearable style="width: 100px">
            <el-option :value="1" label="一级" />
            <el-option :value="2" label="二级" />
            <el-option :value="3" label="三级" />
            <el-option :value="4" label="四级" />
          </el-select>
        </el-form-item>
        <el-form-item label="处方权">
          <el-select v-model="query.antitumorRx" placeholder="抗肿瘤" clearable style="width: 110px">
            <el-option :value="1" label="有抗肿瘤处方权" />
            <el-option :value="0" label="无抗肿瘤处方权" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 110px">
            <el-option :value="1" label="在职" />
            <el-option :value="2" label="休假" />
            <el-option :value="3" label="进修" />
            <el-option :value="4" label="离职" />
            <el-option :value="5" label="退休" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作区 -->
    <el-card style="margin-top: 16px">
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="handleAdd">新增医师</el-button>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
        </div>
        <div>
          <el-button @click="handleDownloadTemplate">下载导入模板</el-button>
          <el-upload
            action="/api/physicians/import"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-success="handleImportSuccess"
            :on-error="handleImportError"
            style="display: inline-block; margin: 0 8px"
          >
            <el-button type="success">批量导入</el-button>
          </el-upload>
          <el-button type="warning" @click="handleExport">导出 Excel</el-button>
        </div>
      </div>

      <!-- 列表 -->
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="selected = $event" style="margin-top: 12px">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="60">
          <template #default="{ row }">{{ row.gender === 'M' ? '男' : row.gender === 'F' ? '女' : '' }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="科室" width="110" />
        <el-table-column prop="title" label="职称" width="100" />
        <el-table-column prop="education" label="最高学历" width="90" />
        <el-table-column prop="practiceNo" label="执业证号" width="150" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="staffType" label="人员类别" width="90" />
        <el-table-column prop="surgeryLevel" label="手术分级" width="90">
          <template #default="{ row }">{{ row.surgeryLevel ? row.surgeryLevel + '级' : '未授权' }}</template>
        </el-table-column>
        <el-table-column label="处方权" width="190">
          <template #default="{ row }">
            <el-tag v-if="row.antitumorRx" size="small" type="success">抗肿瘤</el-tag>
            <el-tag v-if="row.anesthesiaRx" size="small" type="warning" style="margin-left: 4px">麻醉</el-tag>
            <el-tag v-if="row.psychotropicRx" size="small" type="danger" style="margin-left: 4px">精神</el-tag>
            <span v-if="!row.antitumorRx && !row.anesthesiaRx && !row.psychotropicRx" style="color: #909399">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio value="M">男</el-radio>
                <el-radio value="F">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard"><el-input v-model="form.idCard" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科室">
              <el-select v-model="form.deptId" placeholder="请选择" style="width: 100%" @change="onDeptChange">
                <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称">
              <el-select v-model="form.title" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in titles" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最高学历">
              <el-select v-model="form.education" placeholder="请选择" style="width: 100%">
                <el-option v-for="e in educations" :key="e.value" :label="e.label" :value="e.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="毕业院校"><el-input v-model="form.graduateSchool" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业"><el-input v-model="form.major" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执业证号"><el-input v-model="form.practiceNo" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执业范围"><el-input v-model="form.practiceScope" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执业类别">
              <el-select v-model="form.practiceCategory" placeholder="请选择" style="width: 100%">
                <el-option v-for="c in practiceCategories" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人员类别">
              <el-select v-model="form.staffType" placeholder="请选择" style="width: 100%">
                <el-option v-for="s in staffTypes" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="聘期">
              <el-date-picker
                v-model="hireRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="证书"><el-input v-model="form.certificates" placeholder="多个证书以分号分隔" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="进修经历"><el-input v-model="form.training" type="textarea" :rows="2" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手术分级">
              <el-select v-model="form.surgeryLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option :value="0" label="未授权" />
                <el-option :value="1" label="一级" />
                <el-option :value="2" label="二级" />
                <el-option :value="3" label="三级" />
                <el-option :value="4" label="四级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="在职状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option :value="1" label="在职" />
                <el-option :value="2" label="休假" />
                <el-option :value="3" label="进修" />
                <el-option :value="4" label="离职" />
                <el-option :value="5" label="退休" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="授权与处方权">
              <el-checkbox v-model="form.surgeryAuth" :true-value="1" :false-value="0">手术分级授权</el-checkbox>
              <el-checkbox v-model="form.antitumorRx" :true-value="1" :false-value="0">抗肿瘤处方权</el-checkbox>
              <el-checkbox v-model="form.anesthesiaRx" :true-value="1" :false-value="0">麻醉药品处方权</el-checkbox>
              <el-checkbox v-model="form.psychotropicRx" :true-value="1" :false-value="0">精神药品处方权</el-checkbox>
            </el-form-item>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPhysicians,
  createPhysician,
  updatePhysician,
  deletePhysician,
  batchDeletePhysicians,
  exportPhysicians,
  downloadTemplate
} from '../../api/physician'
import { getEnabledDepts, getDictItems } from '../../api/system'

const query = reactive({
  keyword: '',
  deptId: null,
  title: '',
  education: '',
  staffType: '',
  surgeryLevel: null,
  antitumorRx: null,
  status: null,
  page: 1,
  size: 10
})
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const depts = ref([])
const titles = ref([])
const educations = ref([])
const staffTypes = ref([])
const practiceCategories = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增医师')
const formRef = ref(null)
const form = reactive({
  id: null, name: '', gender: 'M', idCard: '', phone: '',
  deptId: null, deptName: '', title: '', education: '', graduateSchool: '',
  major: '', practiceNo: '', practiceScope: '', practiceCategory: '',
  staffType: '', hireStartDate: '', hireEndDate: '', certificates: '',
  training: '', surgeryLevel: 0, surgeryAuth: 0, antitumorRx: 0,
  anesthesiaRx: 0, psychotropicRx: 0, status: 1, remark: ''
})
const hireRange = ref([])
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [{ pattern: /(^$)|(^[0-9]{17}[0-9Xx]$)/, message: '身份证号格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /(^$)|(^1[3-9]\d{9}$)/, message: '手机号格式不正确', trigger: 'blur' }]
}

const statusText = (s) => ({ 1: '在职', 2: '休假', 3: '进修', 4: '离职', 5: '退休' }[s] || '')
const statusTag = (s) => ({ 1: 'success', 2: 'warning', 3: '', 4: 'info', 5: 'info' }[s] || 'info')

async function fetchList() {
  loading.value = true
  try {
    const data = await getPhysicians({ ...query })
    tableData.value = data.records
    total.value = data.total
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
  Object.assign(query, {
    keyword: '', deptId: null, title: '', education: '', staffType: '',
    surgeryLevel: null, antitumorRx: null, status: null, page: 1, size: 10
  })
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, name: '', gender: 'M', idCard: '', phone: '',
    deptId: null, deptName: '', title: '', education: '', graduateSchool: '',
    major: '', practiceNo: '', practiceScope: '', practiceCategory: '',
    staffType: '', hireStartDate: '', hireEndDate: '', certificates: '',
    training: '', surgeryLevel: 0, surgeryAuth: 0, antitumorRx: 0,
    anesthesiaRx: 0, psychotropicRx: 0, status: 1, remark: ''
  })
  hireRange.value = []
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增医师'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row)
  hireRange.value = row.hireStartDate && row.hireEndDate ? [row.hireStartDate, row.hireEndDate] : []
  dialogTitle.value = '编辑医师'
  dialogVisible.value = true
}

function onDeptChange(id) {
  const d = depts.value.find((x) => x.id === id)
  form.deptName = d ? d.name : ''
}

async function handleSubmit() {
  await formRef.value.validate()
  form.hireStartDate = hireRange.value?.[0] || ''
  form.hireEndDate = hireRange.value?.[1] || ''
  try {
    if (form.id) {
      await updatePhysician(form.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createPhysician({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除医师「${row.name}」吗？`, '提示', { type: 'warning' })
  await deletePhysician(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 名医师吗？`, '提示', { type: 'warning' })
  await batchDeletePhysicians(selected.value.map((r) => r.id))
  ElMessage.success('批量删除成功')
  fetchList()
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
    const res = await exportPhysicians({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '医师档案.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadTemplate()
  downloadBlob(res.data, '医师档案导入模板.xlsx')
}

function handleImportSuccess(res) {
  if (res.code === 0) {
    ElMessage.success(`导入成功，共导入 ${res.data.imported} 条`)
    fetchList()
  } else {
    ElMessage.error('导入失败：' + (res.msg || '未知错误'))
  }
}

function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}

onMounted(async () => {
  try {
    depts.value = await getEnabledDepts()
    titles.value = await getDictItems('title')
    educations.value = await getDictItems('education')
    staffTypes.value = await getDictItems('staff_type')
    practiceCategories.value = await getDictItems('practice_category')
  } catch (e) {
    console.warn('字典加载失败（后端未启动？）', e.message)
  }
  fetchList()
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
</style>
