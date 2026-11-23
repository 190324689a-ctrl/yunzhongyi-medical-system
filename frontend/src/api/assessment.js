import request from '../utils/request'

/** 分页 + 多条件组合查询 */
export function getAssessments(params) {
  return request.get('/assessments', { params })
}

/** 评分统计：总数 / 平均分 / 各等级人数与优秀率 / 按等级分组 / 按科室汇总 */
export function getAssessmentStats() {
  return request.get('/assessments/stats')
}

/** 按科室汇总统计 */
export function getAssessmentStatsByDept() {
  return request.get('/assessments/stats/by-dept')
}

/** 详情 */
export function getAssessment(id) {
  return request.get(`/assessments/${id}`)
}

/** 新增考核记录（总分与等级由各项得分自动计算） */
export function createAssessment(data) {
  return request.post('/assessments', data)
}

/** 修改（重新录入得分后总分与等级自动重算） */
export function updateAssessment(id, data) {
  return request.put(`/assessments/${id}`, data)
}

/** 删除 */
export function deleteAssessment(id) {
  return request.delete(`/assessments/${id}`)
}

/** 批量删除 */
export function batchDeleteAssessments(ids) {
  return request.delete('/assessments/batch', { data: ids })
}

/** 导出 Excel（按当前查询条件） */
export function exportAssessments(params) {
  return request.get('/assessments/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadAssessmentTemplate() {
  return request.get('/assessments/template', { responseType: 'blob' })
}

/** 考核模板：分页查询 */
export function getAssessmentTemplates(params) {
  return request.get('/assessment-templates', { params })
}

/** 考核模板：全部启用的模板（新建考核记录时下拉用） */
export function getEnabledAssessmentTemplates() {
  return request.get('/assessment-templates/enabled')
}

/** 考核模板：新增（评分项权重之和必须为 100） */
export function createAssessmentTemplate(data) {
  return request.post('/assessment-templates', data)
}

/** 考核模板：修改 */
export function updateAssessmentTemplate(id, data) {
  return request.put(`/assessment-templates/${id}`, data)
}

/** 考核模板：删除 */
export function deleteAssessmentTemplate(id) {
  return request.delete(`/assessment-templates/${id}`)
}
