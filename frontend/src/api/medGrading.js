import request from '../utils/request'

// ---------- 分级标准 ----------
/** 分级标准分页 + 多条件组合查询（关键字 / 分级范围 / 状态） */
export function getGradingStandards(params) {
  return request.get('/grading-standards', { params })
}

/** 分级标准统计：总数 / 启用 / 停用 / 按分级范围分组 / 已用 / 未用 */
export function getGradingStandardStats() {
  return request.get('/grading-standards/stats')
}

/** 全部启用的标准（新建评定时下拉用） */
export function getEnabledGradingStandards() {
  return request.get('/grading-standards/enabled')
}

/** 分级标准详情 */
export function getGradingStandard(id) {
  return request.get(`/grading-standards/${id}`)
}

/** 新增分级标准 */
export function createGradingStandard(data) {
  return request.post('/grading-standards', data)
}

/** 修改分级标准 */
export function updateGradingStandard(id, data) {
  return request.put(`/grading-standards/${id}`, data)
}

/** 删除分级标准（已有评定的标准不能直接删除） */
export function deleteGradingStandard(id) {
  return request.delete(`/grading-standards/${id}`)
}

/** 启用分级标准 */
export function enableGradingStandard(id) {
  return request.post(`/grading-standards/${id}/enable`)
}

/** 停用分级标准（有待审批评定的标准不可停用） */
export function disableGradingStandard(id) {
  return request.post(`/grading-standards/${id}/disable`)
}

/** 导出分级标准 Excel */
export function exportGradingStandards(params) {
  return request.get('/grading-standards/export', { params, responseType: 'blob' })
}

/** 批量导入分级标准 Excel */
export function importGradingStandards(data) {
  return request.post('/grading-standards/import', data)
}

/** 下载分级标准导入模板 */
export function downloadGradingStandardTemplate() {
  return request.get('/grading-standards/template', { responseType: 'blob' })
}

// ---------- 分级评定 ----------
/** 分级评定分页 + 多条件组合查询 */
export function getGradingAssessments(params) {
  return request.get('/grading-assessments', { params })
}

/** 分级评定统计：总数 / 待审批 / 已通过 / 已驳回 / 按等级分组 / 平均分 / 达标率 */
export function getGradingAssessmentStats() {
  return request.get('/grading-assessments/stats')
}

/** 按科室汇总：各科室最新已通过等级（当前分级）、平均分、评定次数 */
export function getGradingStatsByDept() {
  return request.get('/grading-assessments/stats/by-dept')
}

/** 按月趋势：每月评定数、已通过数、平均分 */
export function getGradingStatsByMonth() {
  return request.get('/grading-assessments/stats/by-month')
}

/** 分级评定详情 */
export function getGradingAssessment(id) {
  return request.get(`/grading-assessments/${id}`)
}

/** 新增分级评定（默认待审批；等级按标准分数线自动算） */
export function createGradingAssessment(data) {
  return request.post('/grading-assessments', data)
}

/** 修改分级评定（只有待审批的能修改） */
export function updateGradingAssessment(id, data) {
  return request.put(`/grading-assessments/${id}`, data)
}

/** 删除分级评定（已通过的评定不可直接删除） */
export function deleteGradingAssessment(id) {
  return request.delete(`/grading-assessments/${id}`)
}

/** 一级审批通过 */
export function approveGradingAssessment(id, data) {
  return request.post(`/grading-assessments/${id}/approve`, data)
}

/** 一级审批驳回 */
export function rejectGradingAssessment(id, data) {
  return request.post(`/grading-assessments/${id}/reject`, data)
}

/** 导出分级评定 Excel */
export function exportGradingAssessments(params) {
  return request.get('/grading-assessments/export', { params, responseType: 'blob' })
}

/** 批量导入分级评定 Excel */
export function importGradingAssessments(data) {
  return request.post('/grading-assessments/import', data)
}

/** 下载分级评定导入模板 */
export function downloadGradingAssessmentTemplate() {
  return request.get('/grading-assessments/template', { responseType: 'blob' })
}
