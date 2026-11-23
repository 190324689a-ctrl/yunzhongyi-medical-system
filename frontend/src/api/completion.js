import request from '../utils/request'

// ---------- 模块完成情况 ----------
/** 模块完成台账分页 + 多条件组合查询（关键字 / 分组 / 状态 / 模块编号） */
export function getModuleCompletions(params) {
  return request.get('/module-completions', { params })
}

/** 完成统计总览：模块总数、已完成/进行中/待开发数、完成率、接口总数、测试总数、平均进度 */
export function getModuleCompletionStats() {
  return request.get('/module-completions/stats')
}

/** 按分组汇总：各分组模块数、已完成/进行中/待开发数、完成率、接口数、测试数、平均进度 */
export function getModuleCompletionStatsByCategory() {
  return request.get('/module-completions/stats/by-category')
}

/** 按完成月份趋势：每月完成模块数 */
export function getModuleCompletionStatsByMonth() {
  return request.get('/module-completions/stats/by-month')
}

/** 模块完成记录详情 */
export function getModuleCompletion(id) {
  return request.get(`/module-completions/${id}`)
}

/** 新增模块完成记录（模块编号全院唯一） */
export function createModuleCompletion(data) {
  return request.post('/module-completions', data)
}

/** 修改模块完成记录 */
export function updateModuleCompletion(id, data) {
  return request.put(`/module-completions/${id}`, data)
}

/** 删除模块完成记录 */
export function deleteModuleCompletion(id) {
  return request.delete(`/module-completions/${id}`)
}

/** 批量删除模块完成记录 */
export function batchDeleteModuleCompletions(ids) {
  return request.delete('/module-completions/batch', { data: ids })
}

/** 标记完成：进度置 100、完成日期默认当天 */
export function completeModule(id) {
  return request.post(`/module-completions/${id}/complete`)
}

/** 重新开始：回到进行中、进度置 50、完成日期清空 */
export function reopenModule(id) {
  return request.post(`/module-completions/${id}/reopen`)
}

/** 导出模块完成情况 Excel */
export function exportModuleCompletions(params) {
  return request.get('/module-completions/export', { params, responseType: 'blob' })
}

/** 批量导入模块完成情况 Excel */
export function importModuleCompletions(data) {
  return request.post('/module-completions/import', data)
}

/** 下载模块完成情况导入模板 */
export function downloadModuleCompletionTemplate() {
  return request.get('/module-completions/template', { responseType: 'blob' })
}
