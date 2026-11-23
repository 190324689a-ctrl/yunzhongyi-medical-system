import request from '../utils/request'

/** 分页 + 多条件组合查询 */
export function getSchedules(params) {
  return request.get('/schedules', { params })
}

/** 排班统计：总数 / 待审批调班代班数 / 夜班人次 / 按类型分组 / 按班次分组 / 按科室汇总 / 按人员汇总 */
export function getScheduleStats() {
  return request.get('/schedules/stats')
}

/** 按科室汇总统计 */
export function getScheduleStatsByDept() {
  return request.get('/schedules/stats/by-dept')
}

/** 按人员汇总统计（排班天数、夜班天数） */
export function getScheduleStatsByPhysician() {
  return request.get('/schedules/stats/by-physician')
}

/** 详情 */
export function getSchedule(id) {
  return request.get(`/schedules/${id}`)
}

/** 新增排班（同一医师同一天不能重复排班；班次时间段缺失时按班次默认时间补全） */
export function createSchedule(data) {
  return request.post('/schedules', data)
}

/** 修改 */
export function updateSchedule(id, data) {
  return request.put(`/schedules/${id}`, data)
}

/** 删除 */
export function deleteSchedule(id) {
  return request.delete(`/schedules/${id}`)
}

/** 批量删除 */
export function batchDeleteSchedules(ids) {
  return request.delete('/schedules/batch', { data: ids })
}

/** 按模板批量生成排班（指定日期范围 + 科室/人员；冲突跳过并计数返回） */
export function generateSchedules(data) {
  return request.post('/schedules/generate', data)
}

/** 申请调班（换班次/换日期） */
export function applySwap(id, data) {
  return request.post(`/schedules/${id}/swap`, data)
}

/** 申请代班（换人） */
export function applySubstitute(id, data) {
  return request.post(`/schedules/${id}/substitute`, data)
}

/** 一级审批通过（科主任） */
export function approveChange(id, data) {
  return request.post(`/schedules/${id}/approve-change`, data)
}

/** 一级审批驳回（科主任） */
export function rejectChange(id, data) {
  return request.post(`/schedules/${id}/reject-change`, data)
}

/** 同步单条排班到 HIS（预留占位：目前未真实对接，调用会明确返回未启用） */
export function syncScheduleToHis(id) {
  return request.post(`/schedules/${id}/his-sync`)
}

/** 导出 Excel（按当前查询条件） */
export function exportSchedules(params) {
  return request.get('/schedules/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadScheduleTemplate() {
  return request.get('/schedules/template', { responseType: 'blob' })
}

/** 排班模板：分页查询 */
export function getScheduleTemplates(params) {
  return request.get('/schedule-templates', { params })
}

/** 排班模板：全部启用的模板（批量生成排班时下拉用） */
export function getEnabledScheduleTemplates() {
  return request.get('/schedule-templates/enabled')
}

/** 排班模板：新增（班次组合必须合法） */
export function createScheduleTemplate(data) {
  return request.post('/schedule-templates', data)
}

/** 排班模板：修改 */
export function updateScheduleTemplate(id, data) {
  return request.put(`/schedule-templates/${id}`, data)
}

/** 排班模板：删除 */
export function deleteScheduleTemplate(id) {
  return request.delete(`/schedule-templates/${id}`)
}
