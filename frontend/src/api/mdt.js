import request from '../utils/request'

/** 分页 + 多条件组合查询 */
export function getMdts(params) {
  return request.get('/mdts', { params })
}

/** MDT 统计：总数 / 待审批数 / 已通过数 / 已驳回数 / 紧急会诊数 / 按会诊类型分组 / 按科室汇总 / 按月趋势 */
export function getMdtStats() {
  return request.get('/mdts/stats')
}

/** 按科室汇总统计 */
export function getMdtStatsByDept() {
  return request.get('/mdts/stats/by-dept')
}

/** 按月趋势统计 */
export function getMdtStatsByMonth() {
  return request.get('/mdts/stats/by-month')
}

/** 详情（含全部会诊记录） */
export function getMdt(id) {
  return request.get(`/mdts/${id}`)
}

/** 新增 MDT 申请（默认待审批） */
export function createMdt(data) {
  return request.post('/mdts', data)
}

/** 修改（只有待审批的申请能修改） */
export function updateMdt(id, data) {
  return request.put(`/mdts/${id}`, data)
}

/** 删除 */
export function deleteMdt(id) {
  return request.delete(`/mdts/${id}`)
}

/** 批量删除 */
export function batchDeleteMdts(ids) {
  return request.delete('/mdts/batch', { data: ids })
}

/** 一级审批通过（医务科/科主任） */
export function approveMdt(id, data) {
  return request.post(`/mdts/${id}/approve`, data)
}

/** 一级审批驳回（医务科/科主任） */
export function rejectMdt(id, data) {
  return request.post(`/mdts/${id}/reject`, data)
}

/** 查某申请的全部会诊记录 */
export function getMdtRecords(id) {
  return request.get(`/mdts/${id}/records`)
}

/** 添加会诊记录（只有审批通过的申请才能记录，可多次补充） */
export function addMdtRecord(id, data) {
  return request.post(`/mdts/${id}/records`, data)
}

/** 删除一条会诊记录 */
export function deleteMdtRecord(id, recordId) {
  return request.delete(`/mdts/${id}/records/${recordId}`)
}

/** 导出 Excel（按当前查询条件） */
export function exportMdts(params) {
  return request.get('/mdts/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadMdtTemplate() {
  return request.get('/mdts/template', { responseType: 'blob' })
}
