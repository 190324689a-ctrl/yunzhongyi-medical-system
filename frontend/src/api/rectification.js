import request from '../utils/request'

/** 检查整改汇总报告：分页 + 多条件组合查询 */
export function getRectificationSummaries(params) {
  return request.get('/rectification-summaries', { params })
}

/** 实时汇总统计：问题总数 / 按严重程度分组 / 整改任务数 / 整改完成率 / 逾期数 / 按科室/按类型/按月汇总 */
export function getRectificationStats() {
  return request.get('/rectification-summaries/stats')
}

/** 按责任科室汇总统计 */
export function getRectificationStatsByDept() {
  return request.get('/rectification-summaries/stats/by-dept')
}

/** 按检查类型汇总统计 */
export function getRectificationStatsByType() {
  return request.get('/rectification-summaries/stats/by-type')
}

/** 按月汇总统计 */
export function getRectificationStatsByMonth() {
  return request.get('/rectification-summaries/stats/by-month')
}

/** 逾期整改任务分页查询 */
export function getOverdueRectifications(params) {
  return request.get('/rectification-summaries/overdue', { params })
}

/** 检查问题汇总分页查询 */
export function getIssueSummaries(params) {
  return request.get('/rectification-summaries/issues', { params })
}

/** 汇总报告详情 */
export function getRectificationSummaryById(id) {
  return request.get(`/rectification-summaries/${id}`)
}

/** 新增汇总报告 */
export function createRectificationSummary(data) {
  return request.post('/rectification-summaries', data)
}

/** 修改汇总报告（汇总口径不可改，改口径请重新生成） */
export function updateRectificationSummary(id, data) {
  return request.put(`/rectification-summaries/${id}`, data)
}

/** 删除汇总报告 */
export function deleteRectificationSummary(id) {
  return request.delete(`/rectification-summaries/${id}`)
}

/** 批量删除汇总报告 */
export function batchDeleteRectificationSummaries(ids) {
  return request.delete('/rectification-summaries/batch', { data: ids })
}

/** 生成汇总报告：按统计周期 + 汇总范围实时聚合生成；已存在时重新生成 */
export function generateRectificationSummary(data) {
  return request.post('/rectification-summaries/generate', data)
}

/** 导出汇总报告 Excel（按当前查询条件） */
export function exportRectificationSummaries(params) {
  return request.get('/rectification-summaries/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadRectificationTemplate() {
  return request.get('/rectification-summaries/template', { responseType: 'blob' })
}
