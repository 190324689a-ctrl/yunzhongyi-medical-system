import request from '../utils/request'

/** 分页 + 多条件组合查询 */
export function getAuthorizations(params) {
  return request.get('/authorizations', { params })
}

/** 统计 */
export function getAuthorizationStats() {
  return request.get('/authorizations/stats')
}

/** 详情（打印预览用） */
export function getAuthorization(id) {
  return request.get(`/authorizations/${id}`)
}

/** 在线申请 */
export function createAuthorization(data) {
  return request.post('/authorizations', data)
}

/** 修改 */
export function updateAuthorization(id, data) {
  return request.put(`/authorizations/${id}`, data)
}

/** 删除 */
export function deleteAuthorization(id) {
  return request.delete(`/authorizations/${id}`)
}

/** 批量删除 */
export function batchDeleteAuthorizations(ids) {
  return request.delete('/authorizations/batch', { data: ids })
}

/** 多级审批：通过当前一级 */
export function approveAuthorization(id, data) {
  return request.post(`/authorizations/${id}/approve`, data || {})
}

/** 多级审批：驳回 */
export function rejectAuthorization(id, data) {
  return request.post(`/authorizations/${id}/reject`, data || {})
}

/** 导出 Excel（按当前查询条件） */
export function exportAuthorizations(params) {
  return request.get('/authorizations/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadAuthorizationTemplate() {
  return request.get('/authorizations/template', { responseType: 'blob' })
}
