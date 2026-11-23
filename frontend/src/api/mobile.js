import request from '../utils/request'

// ---------- 移动端总览 ----------
// 移动端首页总览：医师 / 待审批 / 考核 / 设备 / 消息 / 推送的数量汇总
export function getMobileOverview(params) {
  return request.get('/mobile/overview', { params })
}

// 移动端查看医师档案（只读分页，与 PC 端同一数据源）
export function getMobilePhysicians(params) {
  return request.get('/mobile/physicians', { params })
}

// 移动端查看今日排班（只读，按当天日期过滤）
export function getMobileTodaySchedules() {
  return request.get('/mobile/schedules/today')
}

// 移动端消息推送：固定走 APP 渠道，复用 29 号推送记录；通道未对接时如实返回失败
export function sendMobilePush(data) {
  return request.post('/mobile/push', data)
}

// 移动端推送统计：总数 / 待发送 / 已发送 / 发送失败 / 按渠道分组
export function getMobilePushStats() {
  return request.get('/mobile/push/stats')
}

// ---------- 待审批事项 ----------
// 待审批事项聚合列表（可按业务模块 / 关键字过滤）
export function getMobilePending(params) {
  return request.get('/mobile-approvals/pending', { params })
}

// 待审批计数：总数与按业务模块分组
export function getMobilePendingCount() {
  return request.get('/mobile-approvals/pending/count')
}

// 移动端审批通过：委托业务模块审批
export function approveMobileItem(moduleType, id, data) {
  return request.post(`/mobile-approvals/${moduleType}/${id}/approve`, data)
}

// 移动端审批驳回：驳回原因必填，委托业务模块审批
export function rejectMobileItem(moduleType, id, data) {
  return request.post(`/mobile-approvals/${moduleType}/${id}/reject`, data)
}

// 审批操作记录分页 + 多条件组合查询
export function getMobileApprovalLogs(params) {
  return request.get('/mobile-approvals/logs', { params })
}

// 审批操作记录统计：总数 / 通过 / 驳回 / 成功 / 失败 / 按业务模块分组
export function getMobileApprovalLogStats() {
  return request.get('/mobile-approvals/logs/stats')
}

// ---------- 移动端设备 ----------
// 设备分页 + 多条件组合查询（关键字 / 平台 / 绑定状态 / 使用人）
export function getMobileDevices(params) {
  return request.get('/mobile-devices', { params })
}

// 设备统计：总数 / 已绑定 / 已解绑 / 按平台分组
export function getMobileDeviceStats() {
  return request.get('/mobile-devices/stats')
}

// 设备详情
export function getMobileDevice(id) {
  return request.get(`/mobile-devices/${id}`)
}

// 登记设备（默认已绑定）
export function createMobileDevice(data) {
  return request.post('/mobile-devices', data)
}

// 修改设备（绑定状态与最近活跃时间不在此改，走绑定 / 解绑接口）
export function updateMobileDevice(id, data) {
  return request.put(`/mobile-devices/${id}`, data)
}

// 删除设备（已绑定的设备不可直接删除，请先解绑）
export function deleteMobileDevice(id) {
  return request.delete(`/mobile-devices/${id}`)
}

// 批量删除设备：已绑定的逐条跳过
export function deleteMobileDevices(ids) {
  return request.delete('/mobile-devices/batch', { data: ids })
}

// 绑定设备
export function bindMobileDevice(id) {
  return request.post(`/mobile-devices/${id}/bind`)
}

// 解绑设备
export function unbindMobileDevice(id) {
  return request.post(`/mobile-devices/${id}/unbind`)
}

// ---------- 导入导出 ----------
// 导出设备 Excel（按当前查询条件）
export function exportMobileDevices(params) {
  return request.get('/mobile-devices/export', { params, responseType: 'blob' })
}

// 下载设备导入模板
export function downloadMobileDeviceTemplate() {
  return request.get('/mobile-devices/template', { responseType: 'blob' })
}

// 导出审批操作记录 Excel（按当前查询条件）
export function exportMobileApprovalLogs(params) {
  return request.get('/mobile-approvals/logs/export', { params, responseType: 'blob' })
}
