import request from '../utils/request'

// ---------- 消息提醒总览 ----------
/** 消息提醒总览：站内消息 / 待办提醒 / 消息推送 / 短信提醒 / 28 号提醒设置的数量汇总 */
export function getNotificationOverview() {
  return request.get('/notifications/overview')
}

// ---------- 站内消息 ----------
/** 站内消息分页 + 多条件组合查询（关键字 / 分类 / 已读状态 / 接收范围） */
export function getSiteMessages(params) {
  return request.get('/site-messages', { params })
}

/** 站内消息统计：总数 / 未读 / 已读 / 按分类分组 / 按接收范围分组 */
export function getSiteMessageStats() {
  return request.get('/site-messages/stats')
}

/** 未读数：按用户名与角色编码统计该用户可见的未读消息数 */
export function getUnreadCount(params) {
  return request.get('/site-messages/unread-count', { params })
}

/** 站内消息详情 */
export function getSiteMessage(id) {
  return request.get(`/site-messages/${id}`)
}

/** 发送站内消息（默认通知分类、全部人员、未读） */
export function createSiteMessage(data) {
  return request.post('/site-messages', data)
}

/** 修改站内消息（已读消息不可修改） */
export function updateSiteMessage(id, data) {
  return request.put(`/site-messages/${id}`, data)
}

/** 删除站内消息 */
export function deleteSiteMessage(id) {
  return request.delete(`/site-messages/${id}`)
}

/** 标已读 */
export function markMessageRead(id) {
  return request.post(`/site-messages/${id}/read`)
}

/** 全部标已读：把某用户可见的未读消息全部标已读 */
export function markAllMessagesRead(params) {
  return request.post('/site-messages/read-all', null, { params })
}

/** 导出站内消息 Excel */
export function exportSiteMessages(params) {
  return request.get('/site-messages/export', { params, responseType: 'blob' })
}

/** 批量导入站内消息 Excel */
export function importSiteMessages(data) {
  return request.post('/site-messages/import', data)
}

/** 下载站内消息导入模板 */
export function downloadSiteMessageTemplate() {
  return request.get('/site-messages/template', { responseType: 'blob' })
}

// ---------- 待办提醒 ----------
/** 待办提醒分页 + 多条件组合查询（关键字 / 待办类型 / 状态 / 责任人 / 来源） */
export function getTodoReminders(params) {
  return request.get('/todo-reminders', { params })
}

/** 待办提醒统计：总数 / 待办 / 已完成 / 已逾期 / 今日到期 / 按类型分组 / 按来源分组 */
export function getTodoReminderStats() {
  return request.get('/todo-reminders/stats')
}

/** 待办提醒详情 */
export function getTodoReminder(id) {
  return request.get(`/todo-reminders/${id}`)
}

/** 新建待办（默认其他待办、手工创建、待办状态） */
export function createTodoReminder(data) {
  return request.post('/todo-reminders', data)
}

/** 修改待办（已完成的待办不可修改） */
export function updateTodoReminder(id, data) {
  return request.put(`/todo-reminders/${id}`, data)
}

/** 删除待办（已完成的待办不可直接删除） */
export function deleteTodoReminder(id) {
  return request.delete(`/todo-reminders/${id}`)
}

/** 完成待办 */
export function completeTodoReminder(id) {
  return request.post(`/todo-reminders/${id}/complete`)
}

/** 逾期扫描：待办且截止日期早于当天的改为已逾期 */
export function scanOverdueTodos() {
  return request.post('/todo-reminders/overdue-scan')
}

/** 按 28 号提醒设置生成待办：设置须存在且启用 */
export function generateTodoFromSetting(data) {
  return request.post('/todo-reminders/generate', data)
}

/** 导出待办提醒 Excel */
export function exportTodoReminders(params) {
  return request.get('/todo-reminders/export', { params, responseType: 'blob' })
}

/** 批量导入待办提醒 Excel */
export function importTodoReminders(data) {
  return request.post('/todo-reminders/import', data)
}

/** 下载待办提醒导入模板 */
export function downloadTodoReminderTemplate() {
  return request.get('/todo-reminders/template', { responseType: 'blob' })
}

// ---------- 消息推送 ----------
/** 消息推送记录分页 + 多条件组合查询（关键字 / 推送渠道 / 发送状态） */
export function getPushRecords(params) {
  return request.get('/push-records', { params })
}

/** 消息推送记录统计：总数 / 待发送 / 已发送 / 发送失败 / 按渠道分组 */
export function getPushRecordStats() {
  return request.get('/push-records/stats')
}

/** 消息推送记录详情 */
export function getPushRecord(id) {
  return request.get(`/push-records/${id}`)
}

/** 发送推送（预留占位：无真实通道，返回失败并如实记录） */
export function sendPushRecord(data) {
  return request.post('/push-records', data)
}

/** 重推（已发送成功不可重推） */
export function retryPushRecord(id) {
  return request.post(`/push-records/${id}/retry`)
}

/** 删除推送记录（已发送成功的记录不可直接删除） */
export function deletePushRecord(id) {
  return request.delete(`/push-records/${id}`)
}

/** 导出消息推送记录 Excel */
export function exportPushRecords(params) {
  return request.get('/push-records/export', { params, responseType: 'blob' })
}

// ---------- 短信提醒 ----------
/** 短信提醒记录分页 + 多条件组合查询（关键字 / 发送状态） */
export function getSmsRecords(params) {
  return request.get('/sms-records', { params })
}

/** 短信提醒记录统计：总数 / 待发送 / 已发送 / 发送失败 */
export function getSmsRecordStats() {
  return request.get('/sms-records/stats')
}

/** 短信提醒记录详情 */
export function getSmsRecord(id) {
  return request.get(`/sms-records/${id}`)
}

/** 发送短信（预留占位：无真实网关，返回失败并如实记录） */
export function sendSmsRecord(data) {
  return request.post('/sms-records', data)
}

/** 重发（已发送成功不可重发） */
export function retrySmsRecord(id) {
  return request.post(`/sms-records/${id}/retry`)
}

/** 删除短信记录（已发送成功的记录不可直接删除） */
export function deleteSmsRecord(id) {
  return request.delete(`/sms-records/${id}`)
}

/** 导出短信提醒记录 Excel */
export function exportSmsRecords(params) {
  return request.get('/sms-records/export', { params, responseType: 'blob' })
}
