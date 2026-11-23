import request from '../utils/request'

/** 分页 + 多条件组合查询 */
export function getNewTechs(params) {
  return request.get('/new-techs', { params })
}

/** 新技术、新项目统计：总数 / 待审批数 / 已立项数 / 已验收数 / 已推广数 / 已驳回数 / 待伦理审查数 / 按项目类型分组 / 按科室汇总 / 按月趋势 */
export function getNewTechStats() {
  return request.get('/new-techs/stats')
}

/** 按科室汇总统计 */
export function getNewTechStatsByDept() {
  return request.get('/new-techs/stats/by-dept')
}

/** 按月趋势统计 */
export function getNewTechStatsByMonth() {
  return request.get('/new-techs/stats/by-month')
}

/** 详情（含全部进展记录） */
export function getNewTech(id) {
  return request.get(`/new-techs/${id}`)
}

/** 新增申报（默认待审批；需伦理审查的项目默认伦理待审查） */
export function createNewTech(data) {
  return request.post('/new-techs', data)
}

/** 修改（只有待审批的项目能修改申报内容） */
export function updateNewTech(id, data) {
  return request.put(`/new-techs/${id}`, data)
}

/** 删除 */
export function deleteNewTech(id) {
  return request.delete(`/new-techs/${id}`)
}

/** 批量删除 */
export function batchDeleteNewTechs(ids) {
  return request.delete('/new-techs/batch', { data: ids })
}

/** 一级审批通过（医务科）：需伦理审查的项目须伦理通过后才能立项 */
export function approveNewTech(id, data) {
  return request.post(`/new-techs/${id}/approve`, data)
}

/** 一级审批驳回（医务科） */
export function rejectNewTech(id, data) {
  return request.post(`/new-techs/${id}/reject`, data)
}

/** 伦理审查 */
export function ethicsReviewNewTech(id, data) {
  return request.post(`/new-techs/${id}/ethics`, data)
}

/** 查某项目的全部进展记录 */
export function getNewTechRecords(id) {
  return request.get(`/new-techs/${id}/records`)
}

/** 添加进展记录（验收记录使项目变为已验收，推广记录使项目变为已推广） */
export function addNewTechRecord(id, data) {
  return request.post(`/new-techs/${id}/records`, data)
}

/** 删除一条进展记录 */
export function deleteNewTechRecord(id, recordId) {
  return request.delete(`/new-techs/${id}/records/${recordId}`)
}

/** 导出 Excel（按当前查询条件） */
export function exportNewTechs(params) {
  return request.get('/new-techs/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadNewTechTemplate() {
  return request.get('/new-techs/template', { responseType: 'blob' })
}
