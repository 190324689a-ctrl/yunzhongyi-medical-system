import request from '../utils/request'

/** 分页 + 多条件组合查询检查计划 */
export function getQualityPlans(params) {
  return request.get('/quality-inspections', { params })
}

/** 质量检查统计：计划总数 / 按状态分组 / 按检查类型分组 / 按科室汇总 / 按月趋势 / 记录数 / 平均分 / 问题数 / 整改完成率 / 逾期整改数 */
export function getQualityStats() {
  return request.get('/quality-inspections/stats')
}

/** 按科室汇总统计 */
export function getQualityStatsByDept() {
  return request.get('/quality-inspections/stats/by-dept')
}

/** 按月趋势统计 */
export function getQualityStatsByMonth() {
  return request.get('/quality-inspections/stats/by-month')
}

/** 详情（含全部检查记录、问题、整改任务） */
export function getQualityPlan(id) {
  return request.get(`/quality-inspections/${id}`)
}

/** 新增检查计划（默认待执行） */
export function createQualityPlan(data) {
  return request.post('/quality-inspections', data)
}

/** 修改（只有待执行的计划能修改） */
export function updateQualityPlan(id, data) {
  return request.put(`/quality-inspections/${id}`, data)
}

/** 删除 */
export function deleteQualityPlan(id) {
  return request.delete(`/quality-inspections/${id}`)
}

/** 批量删除 */
export function batchDeleteQualityPlans(ids) {
  return request.delete('/quality-inspections/batch', { data: ids })
}

/** 开始检查：待执行 → 进行中 */
export function startQualityPlan(id) {
  return request.post(`/quality-inspections/${id}/start`)
}

/** 完成检查：进行中 → 已完成（需要至少一条检查记录） */
export function completeQualityPlan(id) {
  return request.post(`/quality-inspections/${id}/complete`)
}

/** 取消计划：待执行/进行中 → 已取消 */
export function cancelQualityPlan(id) {
  return request.post(`/quality-inspections/${id}/cancel`)
}

/** 检查报告：按计划生成报告摘要 */
export function getQualityReport(id) {
  return request.get(`/quality-inspections/${id}/report`)
}

/** 查某计划的全部检查记录 */
export function getQualityRecords(id) {
  return request.get(`/quality-inspections/${id}/records`)
}

/** 添加检查记录（只有进行中的计划才能记录；逐项打分，总分与等级自动算） */
export function addQualityRecord(id, data) {
  return request.post(`/quality-inspections/${id}/records`, data)
}

/** 删除一条检查记录 */
export function deleteQualityRecord(id, recordId) {
  return request.delete(`/quality-inspections/${id}/records/${recordId}`)
}

/** 查某记录的全部问题 */
export function getQualityIssues(recordId) {
  return request.get(`/quality-inspections/records/${recordId}/issues`)
}

/** 添加问题 */
export function addQualityIssue(recordId, data) {
  return request.post(`/quality-inspections/records/${recordId}/issues`, data)
}

/** 删除一条问题 */
export function deleteQualityIssue(recordId, issueId) {
  return request.delete(`/quality-inspections/records/${recordId}/issues/${issueId}`)
}

/** 查某计划的全部整改任务 */
export function getQualityRectifications(id) {
  return request.get(`/quality-inspections/${id}/rectifications`)
}

/** 整改任务分页查询（责任人/状态/只看逾期/计划） */
export function getRectificationPage(params) {
  return request.get('/quality-inspections/rectifications', { params })
}

/** 针对一个问题发整改任务：一个问题只发一个 */
export function addQualityRectification(issueId, data) {
  return request.post(`/quality-inspections/issues/${issueId}/rectifications`, data)
}

/** 提交整改：待整改 → 已整改（待复查） */
export function submitRectification(rectificationId) {
  return request.post(`/quality-inspections/rectifications/${rectificationId}/submit`)
}

/** 复查通过：已整改 → 已复查通过 */
export function reviewPassRectification(rectificationId, data) {
  return request.post(`/quality-inspections/rectifications/${rectificationId}/review-pass`, data)
}

/** 复查打回：已整改 → 待整改，可重新提交 */
export function reviewRejectRectification(rectificationId, data) {
  return request.post(`/quality-inspections/rectifications/${rectificationId}/review-reject`, data)
}

/** 导出 Excel：检查记录（按当前查询条件对应的计划） */
export function exportQualityRecords(params) {
  return request.get('/quality-inspections/export', { params, responseType: 'blob' })
}

/** 导入模板下载（检查计划导入模板） */
export function downloadQualityTemplate() {
  return request.get('/quality-inspections/template', { responseType: 'blob' })
}

// ---- 检查模板 ----

/** 检查模板分页查询 */
export function getQualityTemplates(params) {
  return request.get('/quality-templates', { params })
}

/** 全部启用的模板（新建检查计划时下拉用） */
export function getEnabledQualityTemplates() {
  return request.get('/quality-templates/enabled')
}

/** 检查模板详情 */
export function getQualityTemplate(id) {
  return request.get(`/quality-templates/${id}`)
}

/** 新增模板（检查项权重之和必须为 100） */
export function createQualityTemplate(data) {
  return request.post('/quality-templates', data)
}

/** 修改模板 */
export function updateQualityTemplate(id, data) {
  return request.put(`/quality-templates/${id}`, data)
}

/** 删除模板 */
export function deleteQualityTemplate(id) {
  return request.delete(`/quality-templates/${id}`)
}

/** 启用模板 */
export function enableQualityTemplate(id) {
  return request.post(`/quality-templates/${id}/enable`)
}

/** 停用模板 */
export function disableQualityTemplate(id) {
  return request.post(`/quality-templates/${id}/disable`)
}
