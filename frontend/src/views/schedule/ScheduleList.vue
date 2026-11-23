<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.total || 0 }}</div><div class="label">排班总数</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num warn">{{ stats.pendingChange || 0 }}</div><div class="label">待审批调班/代班</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.nightCount || 0 }}</div><div class="label">夜班人次</div></div></el-card></el-col>
      <el-col :span="6"><el-card><div class="stat"><div class="num">{{ stats.byType?.EMERGENCY || 0 }}</div><div class="label">急诊排班</div></div></el-card></el-col>
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
        <el-form-item label="排班类型">
          <el-select v-model="query.scheduleType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="t in scheduleTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="班次">
          <el-select v-model="query.shiftCode" placeholder="全部班次" clearable style="width: 120px">
            <el-option v-for="s in shifts" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="排班日期">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item label="调班/代班">
          <el-select v-model="query.changeStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option v-for="c in changeStatuses" :key="c.value" :label="c.label" :value="c.value" />
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
          <el-button type="primary" @click="handleAdd">新增排班</el-button>
          <el-button @click="generateVisible = true">按模板批量排班</el-button>
          <el-button @click="templateVisible = true">模板管理</el-button>
        </div>
        <div>
          <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
          <el-upload
            action="/api/schedules/import"
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
        <el-table-column prop="physicianName" label="医师" width="90" />
        <el-table-column prop="deptName" label="科室" width="110" />
        <el-table-column prop="scheduleDate" label="排班日期" width="110" />
        <el-table-column prop="scheduleType" label="排班类型" width="100">
          <template #default="{ row }">{{ typeText(row.scheduleType) }}</template>
        </el-table-column>
        <el-table-column prop="shiftCode" label="班次" width="90">
          <template #default="{ row }">
            <el-tag :type="shiftTag(row.shiftCode)">{{ shiftText(row.shiftCode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="班次时间段" width="130">
          <template #default="{ row }">{{ timeRange(row) }}</template>
        </el-table-column>
        <el-table-column prop="templateName" label="来源模板" width="150" />
        <el-table-column prop="changeStatus" label="调班/代班" width="150">
          <template #default="{ row }">
            <el-tag :type="changeTag(row.changeStatus)">{{ changeStatusText(row.changeStatus) }}</el-tag>
            <span v-if="row.changeType" style="margin-left: 4px">{{ changeTypeText(row.changeType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.changeStatus === 1" link type="warning" @click="handleApprove(row)">审批</el-button>
            <el-button v-if="row.changeStatus !== 1" link type="primary" @click="handleChange(row)">调班/代班</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleHisSync(row)">HIS 同步</el-button>
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

    <!-- 按科室排班统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按科室排班统计</span></template>
      <el-table :data="deptStats" border stripe>
        <el-table-column prop="deptName" label="科室" />
        <el-table-column prop="count" label="排班人次" />
        <el-table-column prop="nightCount" label="夜班人次" />
        <el-table-column prop="emergencyCount" label="急诊排班" />
      </el-table>
    </el-card>

    <!-- 按人员排班统计 -->
    <el-card style="margin-top: 16px">
      <template #header><span>按人员排班统计</span></template>
      <el-table :data="physicianStats" border stripe>
        <el-table-column prop="physicianName" label="医师" />
        <el-table-column prop="deptName" label="科室" />
        <el-table-column prop="count" label="排班天数" />
        <el-table-column prop="nightCount" label="夜班天数" />
      </el-table>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排班医师" prop="physicianId">
              <el-select v-model="form.physicianId" placeholder="请选择医师" style="width: 100%" @change="onPhysicianChange">
                <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排班日期" prop="scheduleDate">
              <el-date-picker v-model="form.scheduleDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排班类型" prop="scheduleType">
              <el-select v-model="form.scheduleType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in scheduleTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班次" prop="shiftCode">
              <el-select v-model="form.shiftCode" placeholder="请选择" style="width: 100%" @change="onShiftChange">
                <el-option v-for="s in shifts" :key="s.value" :label="s.label" :value="s.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm" placeholder="默认按班次补全" style="width: 100%" :disabled="form.shiftCode === 'REST'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm" placeholder="默认按班次补全" style="width: 100%" :disabled="form.shiftCode === 'REST'" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert title="同一医师同一天只能有一条排班记录；班次时间段不填时按班次默认时间自动补全（白班 08:00–17:30，上午班 08:00–12:00，下午班 14:00–17:30，夜班 17:30–次日 08:00，休息无时间段）。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 按模板批量排班弹窗 -->
    <el-dialog title="按模板批量排班" v-model="generateVisible" width="640px">
      <el-form :model="generateForm" ref="generateFormRef" label-width="110px">
        <el-form-item label="排班模板">
          <el-select v-model="generateForm.templateId" placeholder="请选择启用的模板" style="width: 100%">
            <el-option v-for="t in enabledTemplates" :key="t.id" :label="`${t.templateName}（${typeText(t.scheduleType)}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="generateDateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排班科室">
          <el-select v-model="generateForm.deptId" placeholder="选择科室（取该科室全部医师）" clearable style="width: 100%" @change="generateForm.physicianIds = []">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指定人员">
          <el-select v-model="generateForm.physicianIds" multiple placeholder="指定人员（优先于科室，可多选）" clearable style="width: 100%">
            <el-option v-for="p in physicians" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-alert title="按模板的班次组合逐日生成；同一医师同一天已有排班时跳过该天（不覆盖），休息日不建记录。结束日期不能早于开始日期。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGenerate">开始生成</el-button>
      </template>
    </el-dialog>

    <!-- 调班 / 代班申请弹窗 -->
    <el-dialog title="调班 / 代班申请" v-model="changeVisible" width="640px">
      <el-form :model="changeForm" ref="changeFormRef" label-width="110px">
        <el-form-item label="当前排班">
          <span>{{ changeRow.physicianName }} {{ changeRow.scheduleDate }} {{ typeText(changeRow.scheduleType) }} {{ shiftText(changeRow.shiftCode) }}</span>
        </el-form-item>
        <el-form-item label="申请类型">
          <el-radio-group v-model="changeForm.changeType">
            <el-radio label="SWAP">调班（换班次/换日期）</el-radio>
            <el-radio label="SUBSTITUTE">代班（换人）</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="changeForm.changeType === 'SWAP'">
          <el-form-item label="目标日期">
            <el-date-picker v-model="changeForm.targetDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="目标班次">
            <el-select v-model="changeForm.targetShiftCode" placeholder="请选择" style="width: 100%">
              <el-option v-for="s in shifts" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="代班医师">
            <el-select v-model="changeForm.targetPhysicianId" placeholder="请选择代班医师" style="width: 100%">
              <el-option v-for="p in physicians.filter((x) => x.id !== changeRow.physicianId)" :key="p.id" :label="`${p.name}（${p.deptName || ''}）`" :value="p.id" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="申请事由">
          <el-input v-model="changeForm.changeReason" type="textarea" :rows="2" placeholder="请填写调班/代班事由" />
        </el-form-item>
        <el-alert title="提交后进入待科主任审批（一级审批）；通过后调班改日期/班次、代班改人员，审批不通过则原排班不变。审批时会再次校验调班/代班后不冲突。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="changeVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangeSubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog title="调班 / 代班审批（科主任）" v-model="approveVisible" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="当前排班">{{ approveRow.physicianName }} {{ approveRow.scheduleDate }} {{ typeText(approveRow.scheduleType) }} {{ shiftText(approveRow.shiftCode) }}</el-descriptions-item>
        <el-descriptions-item label="申请类型">{{ changeTypeText(approveRow.changeType) }}</el-descriptions-item>
        <el-descriptions-item v-if="approveRow.changeType === 'SWAP'" label="调至">{{ approveRow.targetDate }} {{ shiftText(approveRow.targetShiftCode) }} {{ fmtTime(approveRow.targetStartTime) }}–{{ fmtTime(approveRow.targetEndTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="approveRow.changeType === 'SUBSTITUTE'" label="代班医师">{{ approveRow.targetPhysicianName }}</el-descriptions-item>
        <el-descriptions-item label="申请事由">{{ approveRow.changeReason || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="approveRow.approvalHistory" label="审批记录">{{ approveRow.approvalHistory }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="approveForm" label-width="90px" style="margin-top: 16px">
        <el-form-item label="审批人">
          <el-input v-model="approveForm.approver" placeholder="科主任姓名" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="2" placeholder="审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="danger" @click="handleRejectChange">驳回</el-button>
        <el-button type="primary" @click="handleApproveChange">通过</el-button>
      </template>
    </el-dialog>

    <!-- 模板管理弹窗 -->
    <el-dialog title="排班模板管理" v-model="templateVisible" width="860px">
      <div class="toolbar" style="margin-bottom: 12px">
        <el-button type="primary" @click="handleTemplateAdd">新增模板</el-button>
      </div>
      <el-table :data="templates" border stripe>
        <el-table-column prop="templateName" label="模板名称" width="180" />
        <el-table-column prop="deptName" label="适用科室" width="110" />
        <el-table-column prop="scheduleType" label="排班类型" width="100">
          <template #default="{ row }">{{ typeText(row.scheduleType) }}</template>
        </el-table-column>
        <el-table-column label="班次组合（按星期）">
          <template #default="{ row }">{{ shiftPatternText(row.shiftPattern) }}</template>
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
            <el-form-item label="适用科室">
              <el-select v-model="templateForm.deptId" placeholder="全院通用" clearable style="width: 100%" @change="onTemplateDeptChange">
                <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排班类型" prop="scheduleType">
              <el-select v-model="templateForm.scheduleType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in scheduleTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="班次组合">
          <div style="width: 100%">
            <el-row :gutter="8" v-for="(item, idx) in templatePattern" :key="idx" style="margin-bottom: 8px">
              <el-col :span="6"><span style="line-height: 32px">{{ weekNames[idx] }}</span></el-col>
              <el-col :span="12">
                <el-select v-model="item.shiftCode" placeholder="不排班" clearable style="width: 100%">
                  <el-option v-for="s in shifts" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </el-col>
              <el-col :span="6"><span style="line-height: 32px; color: #909399">{{ item.shiftCode ? defaultTimeText(item.shiftCode) : '' }}</span></el-col>
            </el-row>
            <span style="color: #909399">未选班次的星期不排班；选“休息”时批量生成跳过当天。</span>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="templateForm.remark" type="textarea" :rows="2" /></el-form-item>
        <el-button type="primary" @click="handleTemplateSubmit">保存模板</el-button>
        <el-button @click="templateFormVisible = false">取消</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSchedules, getScheduleStats, getScheduleStatsByDept, getScheduleStatsByPhysician,
  createSchedule, updateSchedule, deleteSchedule, batchDeleteSchedules, generateSchedules,
  applySwap, applySubstitute, approveChange, rejectChange, syncScheduleToHis,
  exportSchedules, downloadScheduleTemplate,
  getScheduleTemplates, getEnabledScheduleTemplates, createScheduleTemplate,
  updateScheduleTemplate, deleteScheduleTemplate
} from '../../api/schedule'
import { getEnabledDepts } from '../../api/system'
import { getPhysicians } from '../../api/physician'

const scheduleTypes = [
  { value: 'DEPT', label: '科室排班' },
  { value: 'OUTPATIENT', label: '门诊排班' },
  { value: 'INPATIENT', label: '住院排班' },
  { value: 'EMERGENCY', label: '急诊排班' }
]
const shifts = [
  { value: 'DAY', label: '白班' },
  { value: 'MORNING', label: '上午班' },
  { value: 'AFTERNOON', label: '下午班' },
  { value: 'NIGHT', label: '夜班' },
  { value: 'REST', label: '休息' }
]
const changeStatuses = [
  { value: 0, label: '无申请' },
  { value: 1, label: '待科主任审批' },
  { value: 2, label: '已通过' },
  { value: 3, label: '已驳回' }
]
const weekNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const typeText = (v) => scheduleTypes.find((t) => t.value === v)?.label || v || ''
const shiftText = (v) => shifts.find((s) => s.value === v)?.label || v || ''
const shiftTag = (v) => ({ DAY: '', MORNING: '', AFTERNOON: '', NIGHT: 'warning', REST: 'info' }[v] || 'info')
const changeTypeText = (v) => ({ SWAP: '调班', SUBSTITUTE: '代班' }[v] || '')
const changeStatusText = (v) => changeStatuses.find((c) => c.value === v)?.label || ''
const changeTag = (v) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[v] || 'info')
const fmtTime = (v) => (v ? String(v).slice(0, 5) : '')
const timeRange = (row) => {
  if (row.shiftCode === 'REST' || !row.startTime) return '—'
  return `${fmtTime(row.startTime)}–${fmtTime(row.endTime)}`
}
const defaultTimeText = (shiftCode) => ({
  DAY: '08:00–17:30', MORNING: '08:00–12:00', AFTERNOON: '14:00–17:30', NIGHT: '17:30–次日 08:00', REST: '无时间段'
}[shiftCode] || '')
const shiftPatternText = (json) => {
  try {
    return JSON.parse(json || '[]')
      .map((i) => `${weekNames[i.dayOfWeek - 1]} ${shiftText(i.shiftCode)}`)
      .join('；')
  } catch (e) {
    return json || ''
  }
}

const query = reactive({ keyword: '', deptId: null, scheduleType: '', shiftCode: '', startDate: null, endDate: null, changeStatus: null, page: 1, size: 10 })
const dateRange = ref(null)
const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const selected = ref([])
const depts = ref([])
const physicians = ref([])
const stats = ref({})
const deptStats = ref([])
const physicianStats = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增排班')
const formRef = ref(null)
const form = reactive({
  id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
  scheduleDate: '', scheduleType: 'DEPT', shiftCode: 'DAY', startTime: '', endTime: '', remark: ''
})
const rules = {
  physicianId: [{ required: true, message: '请选择排班医师', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择排班日期', trigger: 'change' }],
  scheduleType: [{ required: true, message: '请选择排班类型', trigger: 'change' }],
  shiftCode: [{ required: true, message: '请选择班次', trigger: 'change' }]
}
const defaultTimes = { DAY: ['08:00', '17:30'], MORNING: ['08:00', '12:00'], AFTERNOON: ['14:00', '17:30'], NIGHT: ['17:30', '08:00'], REST: ['', ''] }

const generateVisible = ref(false)
const generateFormRef = ref(null)
const generateForm = reactive({ templateId: null, deptId: null, physicianIds: [] })
const generateDateRange = ref(null)

const changeVisible = ref(false)
const changeFormRef = ref(null)
const changeRow = ref({})
const changeForm = reactive({ changeType: 'SWAP', targetDate: '', targetShiftCode: 'DAY', targetPhysicianId: null, changeReason: '' })

const approveVisible = ref(false)
const approveRow = ref({})
const approveForm = reactive({ approver: '科主任', opinion: '' })

const templateVisible = ref(false)
const templates = ref([])
const enabledTemplates = ref([])
const templateFormVisible = ref(false)
const templateFormRef = ref(null)
const templateForm = reactive({ id: null, templateName: '', deptId: null, deptName: '', scheduleType: 'DEPT', status: 1, remark: '' })
const templatePattern = ref([])
const templateRules = {
  templateName: [{ required: true, message: '请填写模板名称', trigger: 'blur' }],
  scheduleType: [{ required: true, message: '请选择排班类型', trigger: 'change' }]
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getSchedules({ ...query })
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
    stats.value = await getScheduleStats()
    deptStats.value = await getScheduleStatsByDept()
    physicianStats.value = await getScheduleStatsByPhysician()
  } catch (e) {
    console.warn('统计加载失败', e.message)
  }
}

async function fetchTemplates() {
  try {
    const page = await getScheduleTemplates({ page: 1, size: 100 })
    templates.value = page.records || []
    enabledTemplates.value = await getEnabledScheduleTemplates()
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
  query.startDate = dateRange.value ? dateRange.value[0] : null
  query.endDate = dateRange.value ? dateRange.value[1] : null
  query.page = 1
  fetchList()
}

function handleReset() {
  Object.assign(query, { keyword: '', deptId: null, scheduleType: '', shiftCode: '', startDate: null, endDate: null, changeStatus: null, page: 1, size: 10 })
  dateRange.value = null
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null, physicianId: null, physicianName: '', deptId: null, deptName: '',
    scheduleDate: '', scheduleType: 'DEPT', shiftCode: 'DAY', startTime: '', endTime: '', remark: ''
  })
}

function handleAdd() {
  resetForm()
  dialogTitle.value = '新增排班'
  dialogVisible.value = true
}

function handleEdit(row) {
  resetForm()
  Object.assign(form, row, { startTime: fmtTime(row.startTime), endTime: fmtTime(row.endTime) })
  dialogTitle.value = '编辑排班'
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

function onShiftChange(shiftCode) {
  const [start, end] = defaultTimes[shiftCode] || ['', '']
  form.startTime = start
  form.endTime = end
}

async function handleSubmit() {
  await formRef.value.validate()
  const payload = { ...form }
  if (payload.shiftCode === 'REST') {
    payload.startTime = null
    payload.endTime = null
  }
  try {
    if (form.id) {
      await updateSchedule(form.id, payload)
      ElMessage.success('排班修改成功')
    } else {
      await createSchedule(payload)
      ElMessage.success('排班新增成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除「${row.physicianName}」${row.scheduleDate} 的排班记录吗？`, '提示', { type: 'warning' })
  await deleteSchedule(row.id)
  ElMessage.success('删除成功')
  refresh()
}

async function handleBatchDelete() {
  await ElMessageBox.confirm(`确定批量删除选中的 ${selected.value.length} 条排班记录吗？`, '提示', { type: 'warning' })
  await batchDeleteSchedules(selected.value.map((r) => r.id))
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
    const res = await exportSchedules({ ...query, page: 1, size: 10000 })
    downloadBlob(res.data, '排班记录.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

async function handleDownloadTemplate() {
  const res = await downloadScheduleTemplate()
  downloadBlob(res.data, '排班导入模板.xlsx')
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

// ---- 按模板批量排班 ----
async function handleGenerate() {
  if (!generateForm.templateId) {
    ElMessage.warning('请选择排班模板')
    return
  }
  if (!generateDateRange.value || generateDateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  if (!generateForm.physicianIds.length && !generateForm.deptId) {
    ElMessage.warning('请指定排班人员或科室')
    return
  }
  try {
    const res = await generateSchedules({
      templateId: generateForm.templateId,
      startDate: generateDateRange.value[0],
      endDate: generateDateRange.value[1],
      physicianIds: generateForm.physicianIds.length ? generateForm.physicianIds : null,
      deptId: generateForm.deptId
    })
    ElMessage.success(`批量生成完成：新建 ${res.created} 条，冲突跳过 ${res.skipped} 条，休息 ${res.restDays} 天`)
    generateVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('批量生成失败：' + e.message)
  }
}

// ---- 调班 / 代班 ----
function handleChange(row) {
  changeRow.value = row
  Object.assign(changeForm, { changeType: 'SWAP', targetDate: '', targetShiftCode: 'DAY', targetPhysicianId: null, changeReason: '' })
  changeVisible.value = true
}

async function handleChangeSubmit() {
  const payload = { changeReason: changeForm.changeReason }
  try {
    if (changeForm.changeType === 'SWAP') {
      if (!changeForm.targetDate || !changeForm.targetShiftCode) {
        ElMessage.warning('请填写调班目标日期和班次')
        return
      }
      payload.targetDate = changeForm.targetDate
      payload.targetShiftCode = changeForm.targetShiftCode
      await applySwap(changeRow.value.id, payload)
      ElMessage.success('调班申请已提交，待科主任审批')
    } else {
      if (!changeForm.targetPhysicianId) {
        ElMessage.warning('请选择代班医师')
        return
      }
      payload.targetPhysicianId = changeForm.targetPhysicianId
      await applySubstitute(changeRow.value.id, payload)
      ElMessage.success('代班申请已提交，待科主任审批')
    }
    changeVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('提交失败：' + e.message)
  }
}

// ---- 审批 ----
function handleApprove(row) {
  approveRow.value = row
  Object.assign(approveForm, { approver: '科主任', opinion: '' })
  approveVisible.value = true
}

async function handleApproveChange() {
  try {
    await approveChange(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('审批通过，排班已更新')
    approveVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

async function handleRejectChange() {
  try {
    await rejectChange(approveRow.value.id, { approver: approveForm.approver, opinion: approveForm.opinion })
    ElMessage.success('已驳回，原排班不变')
    approveVisible.value = false
    refresh()
  } catch (e) {
    ElMessage.error('驳回失败：' + e.message)
  }
}

// ---- HIS 同步（预留占位） ----
async function handleHisSync(row) {
  try {
    await syncScheduleToHis(row.id)
    ElMessage.success('已同步到 HIS')
    refresh()
  } catch (e) {
    ElMessage.warning('HIS 同步未执行：' + e.message)
  }
}

// ---- 模板管理 ----
function resetTemplateForm() {
  Object.assign(templateForm, { id: null, templateName: '', deptId: null, deptName: '', scheduleType: 'DEPT', status: 1, remark: '' })
  templatePattern.value = weekNames.map((_, i) => ({ dayOfWeek: i + 1, shiftCode: i < 5 ? 'DAY' : i === 5 ? 'MORNING' : 'REST' }))
}

function handleTemplateAdd() {
  resetTemplateForm()
  templateFormVisible.value = true
}

function handleTemplateEdit(row) {
  Object.assign(templateForm, row)
  const parsed = {}
  try {
    JSON.parse(row.shiftPattern || '[]').forEach((i) => { parsed[i.dayOfWeek] = i.shiftCode })
  } catch (e) {
    // 解析失败时按空处理
  }
  templatePattern.value = weekNames.map((_, i) => ({ dayOfWeek: i + 1, shiftCode: parsed[i + 1] || '' }))
  templateFormVisible.value = true
}

function onTemplateDeptChange(id) {
  const d = depts.value.find((x) => x.id === id)
  templateForm.deptName = d ? d.name : ''
}

async function handleTemplateToggle(row) {
  await updateScheduleTemplate(row.id, { ...row, status: row.status === 1 ? 0 : 1 })
  ElMessage.success(row.status === 1 ? '已停用' : '已启用')
  fetchTemplates()
}

async function handleTemplateDelete(row) {
  await ElMessageBox.confirm(`确定删除模板「${row.templateName}」吗？`, '提示', { type: 'warning' })
  await deleteScheduleTemplate(row.id)
  ElMessage.success('删除成功')
  fetchTemplates()
}

async function handleTemplateSubmit() {
  await templateFormRef.value.validate()
  const pattern = templatePattern.value
    .filter((i) => i.shiftCode)
    .map((i) => ({ dayOfWeek: i.dayOfWeek, shiftCode: i.shiftCode }))
  if (!pattern.length) {
    ElMessage.warning('班次组合至少包含一个星期几的班次')
    return
  }
  const payload = { ...templateForm, shiftPattern: JSON.stringify(pattern) }
  try {
    if (templateForm.id) {
      await updateScheduleTemplate(templateForm.id, payload)
      ElMessage.success('模板修改成功')
    } else {
      await createScheduleTemplate(payload)
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
