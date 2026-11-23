<template>
  <div>
    <p class="page-note">口径：需求清单第 31 项“医务管理移动端”。移动端支持查看、审批、统计，支持 iOS/Android，支持消息推送。待审批事项聚合自 8 个业务模块（休假执业 / 重点授权 / 排班调班 / MDT / 新技术新项目 / 医疗人员动态 / 医疗分级 / 医疗工作简报），审批复用各业务模块已有的审批逻辑；医师档案与今日排班为只读查看；消息推送固定走 APP 渠道，复用 29 号推送记录，推送通道本机没有真实服务，发送明确返回失败、如实记录，不伪造发送成功。</p>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.physicians || 0 }}</div><div class="label">在册医师</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ overview.pendingApprovals || 0 }}</div><div class="label">待审批事项</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.todaySchedules || 0 }}</div><div class="label">今日排班</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.assessments || 0 }}</div><div class="label">考核人次</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.assessmentAvgScore || 0 }}</div><div class="label">考核平均分</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num">{{ overview.devices || 0 }}</div><div class="label">移动端设备</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ overview.unreadMessages || 0 }}</div><div class="label">未读消息</div></div></el-card></el-col>
      <el-col :span="3"><el-card><div class="stat"><div class="num warn">{{ overview.failedPush || 0 }}</div><div class="label">推送失败</div></div></el-card></el-col>
    </el-row>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 待审批 -->
      <el-tab-pane label="待审批" name="pending">
        <el-card>
          <el-form :inline="true" :model="pQuery" class="search-form">
            <el-form-item label="业务模块">
              <el-select v-model="pQuery.moduleType" placeholder="全部" clearable style="width: 170px">
                <el-option v-for="m in moduleTypes" :key="m.value" :label="m.label" :value="m.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键字"><el-input v-model="pQuery.keyword" placeholder="标题 / 申请人 / 科室" clearable style="width: 200px" /></el-form-item>
            <el-form-item><el-button type="primary" @click="loadPending">查询</el-button><el-button @click="handlePReset">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <el-table :data="pendingData" border stripe v-loading="pLoading" style="margin-top: 12px">
            <el-table-column prop="moduleName" label="业务模块" width="130" />
            <el-table-column prop="title" label="事项" min-width="220" />
            <el-table-column prop="applicant" label="申请人" width="100" />
            <el-table-column prop="deptName" label="科室" width="120" />
            <el-table-column prop="statusText" label="审批状态" width="130" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" @click="openApproval(row, 'approve')">通过</el-button>
                <el-button link type="danger" @click="openApproval(row, 'reject')">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!pLoading && pendingData.length === 0" description="暂无待审批事项" />
        </el-card>
      </el-tab-pane>

      <!-- 移动端设备 -->
      <el-tab-pane label="移动端设备" name="device">
        <el-card>
          <el-form :inline="true" :model="dQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="dQuery.keyword" placeholder="设备编码 / 使用人 / 型号" clearable style="width: 200px" /></el-form-item>
            <el-form-item label="平台">
              <el-select v-model="dQuery.platform" placeholder="全部" clearable style="width: 120px">
                <el-option label="iOS" value="IOS" /><el-option label="Android" value="ANDROID" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="dQuery.status" placeholder="全部" clearable style="width: 110px">
                <el-option label="已绑定" :value="1" /><el-option label="已解绑" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="handleDSearch">查询</el-button><el-button @click="handleDReset">重置</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <div class="toolbar">
            <div><el-button type="primary" @click="handleDAdd">登记设备</el-button></div>
            <div>
              <el-upload action="/api/mobile-devices/import" :show-file-list="false" accept=".xlsx,.xls" :on-success="handleDImportSuccess" :on-error="handleImportError" style="display: inline-block; margin: 0 8px"><el-button type="success">批量导入</el-button></el-upload>
              <el-button type="warning" @click="handleDExport">导出 Excel</el-button>
              <el-button @click="handleDDownloadTemplate">下载模板</el-button>
            </div>
          </div>
          <el-table :data="dData" border stripe v-loading="dLoading" @selection-change="dSelected = $event" style="margin-top: 12px">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="deviceCode" label="设备编码" width="110" />
            <el-table-column prop="username" label="使用人" width="100" />
            <el-table-column prop="displayName" label="姓名" width="100" />
            <el-table-column label="平台" width="100"><template #default="{ row }">{{ row.platform === 'IOS' ? 'iOS' : row.platform === 'ANDROID' ? 'Android' : row.platform }}</template></el-table-column>
            <el-table-column prop="deviceModel" label="设备型号" width="150" />
            <el-table-column prop="appVersion" label="App 版本" width="100" />
            <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已绑定' : '已解绑' }}</el-tag></template></el-table-column>
            <el-table-column prop="lastActiveTime" label="最近活跃" width="170" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" link type="success" @click="handleDBind(row)">绑定</el-button>
                <el-button v-if="row.status === 1" link type="warning" @click="handleDUnbind(row)">解绑</el-button>
                <el-button link type="primary" @click="handleDEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="dQuery.page" v-model:page-size="dQuery.size" :total="dTotal" layout="total, prev, pager, next" style="margin-top: 12px; justify-content: flex-end" @current-change="loadDevices" @size-change="loadDevices" />
        </el-card>
      </el-tab-pane>

      <!-- 审批记录 -->
      <el-tab-pane label="审批记录" name="logs">
        <el-card>
          <el-form :inline="true" :model="lQuery" class="search-form">
            <el-form-item label="业务模块">
              <el-select v-model="lQuery.moduleType" placeholder="全部" clearable style="width: 170px">
                <el-option v-for="m in moduleTypes" :key="m.value" :label="m.label" :value="m.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="操作">
              <el-select v-model="lQuery.action" placeholder="全部" clearable style="width: 110px">
                <el-option label="通过" value="APPROVE" /><el-option label="驳回" value="REJECT" />
              </el-select>
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="lQuery.result" placeholder="全部" clearable style="width: 110px">
                <el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAIL" />
              </el-select>
            </el-form-item>
            <el-form-item><el-button type="primary" @click="handleLSearch">查询</el-button><el-button @click="handleLReset">重置</el-button></el-form-item>
            <el-form-item><el-button type="warning" @click="handleLExport">导出 Excel</el-button></el-form-item>
          </el-form>
        </el-card>
        <el-card style="margin-top: 16px">
          <el-table :data="lData" border stripe v-loading="lLoading" style="margin-top: 12px">
            <el-table-column prop="moduleType" label="业务模块" width="130"><template #default="{ row }">{{ moduleText(row.moduleType) }}</template></el-table-column>
            <el-table-column prop="relatedTitle" label="业务标题" min-width="200" />
            <el-table-column label="操作" width="80"><template #default="{ row }">{{ row.action === 'APPROVE' ? '通过' : '驳回' }}</template></el-table-column>
            <el-table-column prop="operator" label="审批人" width="100" />
            <el-table-column prop="deviceCode" label="设备编码" width="110" />
            <el-table-column prop="opinion" label="审批意见" min-width="160" />
            <el-table-column label="结果" width="80"><template #default="{ row }"><el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'">{{ row.result === 'SUCCESS' ? '成功' : '失败' }}</el-tag></template></el-table-column>
            <el-table-column prop="failReason" label="失败原因" min-width="160" />
            <el-table-column prop="actionTime" label="操作时间" width="170" />
          </el-table>
          <el-pagination v-model:current-page="lQuery.page" v-model:page-size="lQuery.size" :total="lTotal" layout="total, prev, pager, next" style="margin-top: 12px; justify-content: flex-end" @current-change="loadLogs" @size-change="loadLogs" />
        </el-card>
      </el-tab-pane>

      <!-- 移动端推送 -->
      <el-tab-pane label="移动端推送" name="push">
        <el-card>
          <p style="color: #909399; margin: 0 0 12px">推送固定走 APP 渠道，复用 29 号消息提醒的推送记录；推送通道本机没有真实服务，发送会明确返回失败并如实记一条发送失败的推送记录，不伪造发送成功。</p>
          <el-form :model="pushForm" label-width="90px" style="max-width: 560px">
            <el-form-item label="推送标题"><el-input v-model="pushForm.title" placeholder="如：休假审批提醒" /></el-form-item>
            <el-form-item label="推送内容"><el-input v-model="pushForm.content" type="textarea" :rows="3" placeholder="推送内容" /></el-form-item>
            <el-form-item label="接收范围">
              <el-select v-model="pushForm.receiverScope" style="width: 200px">
                <el-option label="全部人员" value="ALL" /><el-option label="按角色" value="ROLE" /><el-option label="按人员" value="USER" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="pushForm.receiverScope !== 'ALL'" label="接收对象"><el-input v-model="pushForm.receiverValue" placeholder="角色编码 / 用户名" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handlePushSend">发送推送</el-button><el-button @click="loadPushStats">刷新统计</el-button></el-form-item>
          </el-form>
          <el-descriptions :column="4" border style="margin-top: 8px">
            <el-descriptions-item label="推送总数">{{ pushStats.total || 0 }}</el-descriptions-item>
            <el-descriptions-item label="待发送">{{ pushStats.pending || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已发送">{{ pushStats.sent || 0 }}</el-descriptions-item>
            <el-descriptions-item label="发送失败">{{ pushStats.failed || 0 }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>

      <!-- 移动端查看 -->
      <el-tab-pane label="移动端查看" name="view">
        <el-card>
          <p style="color: #909399; margin: 0 0 12px">移动端只读查看：医师档案与今日排班，与 PC 端同一数据源，不可在此修改。</p>
          <el-form :inline="true" :model="vQuery" class="search-form">
            <el-form-item label="关键字"><el-input v-model="vQuery.keyword" placeholder="医师姓名 / 科室" clearable style="width: 200px" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleVSearch">查询</el-button><el-button @click="handleVReset">重置</el-button></el-form-item>
          </el-form>
          <el-table :data="vData" border stripe v-loading="vLoading" style="margin-top: 12px">
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="deptName" label="科室" width="120" />
            <el-table-column prop="title" label="职称" width="120" />
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="practiceNo" label="执业证号" width="160" />
          </el-table>
          <el-pagination v-model:current-page="vQuery.page" v-model:page-size="vQuery.size" :total="vTotal" layout="total, prev, pager, next" style="margin-top: 12px; justify-content: flex-end" @current-change="loadPhysicians" @size-change="loadPhysicians" />
        </el-card>
        <el-card style="margin-top: 16px">
          <template #header><span>今日排班（{{ todaySchedules.length }} 条）</span></template>
          <el-table :data="todaySchedules" border stripe>
            <el-table-column prop="physicianName" label="医师" width="100" />
            <el-table-column prop="deptName" label="科室" width="120" />
            <el-table-column prop="scheduleType" label="排班类型" width="120" />
            <el-table-column prop="shiftCode" label="班次" width="120" />
            <el-table-column prop="scheduleDate" label="日期" width="130" />
          </el-table>
          <el-empty v-if="todaySchedules.length === 0" description="今日暂无排班" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approvalVisible" :title="approvalTitle" width="480px">
      <el-form :model="approvalForm" label-width="80px">
        <el-form-item label="事项"><span>{{ approvalRow ? approvalRow.title : '' }}</span></el-form-item>
        <el-form-item label="审批人"><el-input v-model="approvalForm.operator" placeholder="如：吴文斌" /></el-form-item>
        <el-form-item label="设备编码"><el-input v-model="approvalForm.deviceCode" placeholder="如：DEV-001" /></el-form-item>
        <el-form-item label="审批意见"><el-input v-model="approvalForm.opinion" type="textarea" :rows="3" :placeholder="approvalAction === 'reject' ? '驳回原因（必填）' : '同意'" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval">确定</el-button>
      </template>
    </el-dialog>

    <!-- 设备弹窗 -->
    <el-dialog v-model="deviceVisible" :title="deviceEditing ? '编辑设备' : '登记设备'" width="520px">
      <el-form :model="deviceForm" label-width="90px">
        <el-form-item label="设备编码"><el-input v-model="deviceForm.deviceCode" :disabled="deviceEditing" placeholder="如：DEV-007" /></el-form-item>
        <el-form-item label="使用人"><el-input v-model="deviceForm.username" placeholder="用户名，如：admin" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="deviceForm.displayName" placeholder="使用人姓名" /></el-form-item>
        <el-form-item label="平台"><el-select v-model="deviceForm.platform" style="width: 100%"><el-option label="iOS" value="IOS" /><el-option label="Android" value="ANDROID" /></el-select></el-form-item>
        <el-form-item label="设备型号"><el-input v-model="deviceForm.deviceModel" placeholder="如：iPhone 15 Pro" /></el-form-item>
        <el-form-item label="App 版本"><el-input v-model="deviceForm.appVersion" placeholder="如：1.0.0" /></el-form-item>
        <el-form-item label="推送 token"><el-input v-model="deviceForm.pushToken" placeholder="推送 token（仅登记）" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="deviceForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deviceVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDevice">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMobileOverview, getMobilePhysicians, getMobileTodaySchedules,
  sendMobilePush, getMobilePushStats,
  getMobilePending, approveMobileItem, rejectMobileItem,
  getMobileApprovalLogs, exportMobileApprovalLogs,
  getMobileDevices, createMobileDevice, updateMobileDevice, deleteMobileDevice,
  bindMobileDevice, unbindMobileDevice, exportMobileDevices, downloadMobileDeviceTemplate
} from '../../api/mobile'

// ---------- 字典 ----------
const moduleTypes = [
  { label: '休假执业', value: 'LEAVE' },
  { label: '重点授权', value: 'AUTH' },
  { label: '排班调班', value: 'SCHEDULE' },
  { label: '多学科会诊', value: 'MDT' },
  { label: '新技术新项目', value: 'NEWTECH' },
  { label: '医疗人员动态', value: 'PERSONNEL' },
  { label: '医疗分级', value: 'MEDGRADING' },
  { label: '医疗工作简报', value: 'BRIEFING' }
]
function moduleText(v) {
  const m = moduleTypes.find((x) => x.value === v)
  return m ? m.label : v
}

// ---------- 总览 ----------
const overview = ref({})
const activeTab = ref('pending')
async function loadOverview() {
  try {
    overview.value = await getMobileOverview()
  } catch (e) {
    ElMessage.error('加载总览失败：' + e.message)
  }
}

// ---------- 待审批 ----------
const pQuery = reactive({ moduleType: '', keyword: '' })
const pendingData = ref([])
const pLoading = ref(false)
async function loadPending() {
  pLoading.value = true
  try {
    pendingData.value = await getMobilePending({ ...pQuery })
  } catch (e) {
    ElMessage.error('加载待审批失败：' + e.message)
  } finally {
    pLoading.value = false
  }
}
function handlePReset() {
  pQuery.moduleType = ''
  pQuery.keyword = ''
  loadPending()
}

// ---------- 审批弹窗 ----------
const approvalVisible = ref(false)
const approvalTitle = ref('审批通过')
const approvalAction = ref('approve')
const approvalRow = ref(null)
const approvalForm = reactive({ operator: '', deviceCode: '', opinion: '' })
function openApproval(row, action) {
  approvalRow.value = row
  approvalAction.value = action
  approvalTitle.value = action === 'approve' ? '审批通过' : '审批驳回'
  approvalForm.operator = '吴文斌'
  approvalForm.deviceCode = 'DEV-001'
  approvalForm.opinion = action === 'approve' ? '同意' : ''
  approvalVisible.value = true
}
async function submitApproval() {
  if (!approvalForm.operator) {
    ElMessage.error('审批人不能为空')
    return
  }
  if (approvalAction.value === 'reject' && !approvalForm.opinion) {
    ElMessage.error('驳回原因不能为空')
    return
  }
  try {
    const row = approvalRow.value
    const fn = approvalAction.value === 'approve' ? approveMobileItem : rejectMobileItem
    const log = await fn(row.moduleType, row.relatedId, { ...approvalForm })
    approvalVisible.value = false
    ElMessage.success(log.result === 'SUCCESS' ? '审批成功，已记操作日志' : '审批失败：' + (log.failReason || ''))
    loadPending()
    loadOverview()
  } catch (e) {
    ElMessage.error('审批失败：' + e.message)
  }
}

// ---------- 移动端设备 ----------
const dQuery = reactive({ keyword: '', platform: '', status: '', page: 1, size: 10 })
const dData = ref([])
const dTotal = ref(0)
const dLoading = ref(false)
const dSelected = ref([])
async function loadDevices() {
  dLoading.value = true
  try {
    const page = await getMobileDevices({ ...dQuery })
    dData.value = page.records
    dTotal.value = page.total
  } catch (e) {
    ElMessage.error('加载设备失败：' + e.message)
  } finally {
    dLoading.value = false
  }
}
function handleDSearch() {
  dQuery.page = 1
  loadDevices()
}
function handleDReset() {
  dQuery.keyword = ''
  dQuery.platform = ''
  dQuery.status = ''
  dQuery.page = 1
  loadDevices()
}
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
async function handleDExport() {
  try {
    const res = await exportMobileDevices({ ...dQuery })
    downloadBlob(res, '移动端设备.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}
async function handleDDownloadTemplate() {
  try {
    const res = await downloadMobileDeviceTemplate()
    downloadBlob(res, '移动端设备导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载模板失败：' + e.message)
  }
}
function handleDImportSuccess(res) {
  ElMessage.success(`导入完成：新增 ${res.created} 条，跳过 ${res.skipped} 条`)
  loadDevices()
  loadOverview()
}
function handleImportError() {
  ElMessage.error('导入失败，请检查文件格式')
}
async function handleDBind(row) {
  try {
    await bindMobileDevice(row.id)
    ElMessage.success('绑定成功')
    loadDevices()
    loadOverview()
  } catch (e) {
    ElMessage.error('绑定失败：' + e.message)
  }
}
async function handleDUnbind(row) {
  try {
    await ElMessageBox.confirm(`确定解绑设备「${row.deviceCode}」吗？`, '提示', { type: 'warning' })
    await unbindMobileDevice(row.id)
    ElMessage.success('解绑成功')
    loadDevices()
    loadOverview()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('解绑失败：' + e.message)
  }
}
async function handleDDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除设备「${row.deviceCode}」吗？已绑定的设备须先解绑。`, '提示', { type: 'warning' })
    await deleteMobileDevice(row.id)
    ElMessage.success('删除成功')
    loadDevices()
    loadOverview()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + e.message)
  }
}

// ---------- 设备弹窗 ----------
const deviceVisible = ref(false)
const deviceEditing = ref(false)
const deviceForm = reactive({ id: null, deviceCode: '', username: '', displayName: '', platform: 'IOS', deviceModel: '', appVersion: '', pushToken: '', remark: '' })
function handleDAdd() {
  deviceEditing.value = false
  Object.assign(deviceForm, { id: null, deviceCode: '', username: '', displayName: '', platform: 'IOS', deviceModel: '', appVersion: '', pushToken: '', remark: '' })
  deviceVisible.value = true
}
function handleDEdit(row) {
  deviceEditing.value = true
  Object.assign(deviceForm, { ...row })
  deviceVisible.value = true
}
async function submitDevice() {
  try {
    if (deviceEditing.value) {
      await updateMobileDevice(deviceForm.id, { ...deviceForm })
      ElMessage.success('修改成功')
    } else {
      await createMobileDevice({ ...deviceForm })
      ElMessage.success('登记成功，默认已绑定')
    }
    deviceVisible.value = false
    loadDevices()
    loadOverview()
  } catch (e) {
    ElMessage.error('保存失败：' + e.message)
  }
}

// ---------- 审批记录 ----------
const lQuery = reactive({ moduleType: '', action: '', result: '', keyword: '', page: 1, size: 10 })
const lData = ref([])
const lTotal = ref(0)
const lLoading = ref(false)
async function loadLogs() {
  lLoading.value = true
  try {
    const page = await getMobileApprovalLogs({ ...lQuery })
    lData.value = page.records
    lTotal.value = page.total
  } catch (e) {
    ElMessage.error('加载审批记录失败：' + e.message)
  } finally {
    lLoading.value = false
  }
}
function handleLSearch() {
  lQuery.page = 1
  loadLogs()
}
function handleLReset() {
  lQuery.moduleType = ''
  lQuery.action = ''
  lQuery.result = ''
  lQuery.keyword = ''
  lQuery.page = 1
  loadLogs()
}
async function handleLExport() {
  try {
    const res = await exportMobileApprovalLogs({ ...lQuery })
    downloadBlob(res, '移动端审批记录.xlsx')
  } catch (e) {
    ElMessage.error('导出失败：' + e.message)
  }
}

// ---------- 移动端推送 ----------
const pushForm = reactive({ title: '', content: '', receiverScope: 'ALL', receiverValue: '' })
const pushStats = ref({})
async function loadPushStats() {
  try {
    pushStats.value = await getMobilePushStats()
  } catch (e) {
    ElMessage.error('加载推送统计失败：' + e.message)
  }
}
async function handlePushSend() {
  if (!pushForm.title) {
    ElMessage.error('推送标题不能为空')
    return
  }
  try {
    const result = await sendMobilePush({ ...pushForm })
    if (result.success) {
      ElMessage.success('推送成功')
    } else {
      ElMessage.warning('推送未发出（通道未对接）：' + (result.message || ''))
    }
    loadPushStats()
    loadOverview()
  } catch (e) {
    ElMessage.error('推送失败：' + e.message)
  }
}

// ---------- 移动端查看 ----------
const vQuery = reactive({ keyword: '', page: 1, size: 10 })
const vData = ref([])
const vTotal = ref(0)
const vLoading = ref(false)
const todaySchedules = ref([])
async function loadPhysicians() {
  vLoading.value = true
  try {
    const page = await getMobilePhysicians({ ...vQuery })
    vData.value = page.records
    vTotal.value = page.total
  } catch (e) {
    ElMessage.error('加载医师档案失败：' + e.message)
  } finally {
    vLoading.value = false
  }
}
async function loadTodaySchedules() {
  try {
    todaySchedules.value = await getMobileTodaySchedules()
  } catch (e) {
    ElMessage.error('加载今日排班失败：' + e.message)
  }
}
function handleVSearch() {
  vQuery.page = 1
  loadPhysicians()
}
function handleVReset() {
  vQuery.keyword = ''
  vQuery.page = 1
  loadPhysicians()
}

// ---------- 页签切换 ----------
function handleTabChange(tab) {
  if (tab === 'device') loadDevices()
  if (tab === 'logs') loadLogs()
  if (tab === 'push') loadPushStats()
  if (tab === 'view') {
    loadPhysicians()
    loadTodaySchedules()
  }
}

onMounted(() => {
  loadOverview()
  loadPending()
})
</script>

<style scoped>
.stat { text-align: center; }
.stat .num { font-size: 24px; font-weight: bold; }
.stat .num.warn { color: #e6a23c; }
.stat .label { color: #909399; font-size: 13px; margin-top: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.search-form { margin-bottom: 0; }
</style>
