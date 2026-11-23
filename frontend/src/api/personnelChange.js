import request from '../utils/request'

/** 分页 + 多条件组合查询 */
export function getPersonnelChanges(params) {
  return request.get('/personnel-changes', { params })
}

/** 医疗人员动态统计：总数 / 待审批数 / 已通过数 / 已驳回数 / 按变动类型分组 / 按相关科室汇总 / 按月趋势 */
export function getPersonnelChangeStats() {
  return request.get('/personnel-changes/stats')
}

/** 按相关科室汇总统计 */
export function getPersonnelChangeStatsByDept() {
  return request.get('/personnel-changes/stats/by-dept')
}

/** 按月趋势统计 */
export function getPersonnelChangeStatsByMonth() {
  return request.get('/personnel-changes/stats/by-month')
}

/** 详情 */
export function getPersonnelChange(id) {
  return request.get(`/personnel-changes/${id}`)
}

/** 新增人员变动登记（默认待审批） */
export function createPersonnelChange(data) {
  return request.post('/personnel-changes', data)
}

/** 修改（只有待审批的变动能修改） */
export function updatePersonnelChange(id, data) {
  return request.put(`/personnel-changes/${id}`, data)
}

/** 删除 */
export function deletePersonnelChange(id) {
  return request.delete(`/personnel-changes/${id}`)
}

/** 批量删除 */
export function batchDeletePersonnelChanges(ids) {
  return request.delete('/personnel-changes/batch', { data: ids })
}

/** 一级审批通过（医务科）：通过时同步医师档案 */
export function approvePersonnelChange(id, data) {
  return request.post(`/personnel-changes/${id}/approve`, data)
}

/** 一级审批驳回（医务科） */
export function rejectPersonnelChange(id, data) {
  return request.post(`/personnel-changes/${id}/reject`, data)
}

/** 导出 Excel（按当前查询条件） */
export function exportPersonnelChanges(params) {
  return request.get('/personnel-changes/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadPersonnelChangeTemplate() {
  return request.get('/personnel-changes/template', { responseType: 'blob' })
}
