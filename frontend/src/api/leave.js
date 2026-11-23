import request from '../utils/request'

/** 分页 + 多条件组合查询 */
export function getLeaves(params) {
  return request.get('/leaves', { params })
}

/** 统计 */
export function getLeaveStats() {
  return request.get('/leaves/stats')
}

/** 详情（打印预览用） */
export function getLeave(id) {
  return request.get(`/leaves/${id}`)
}

/** 在线申请 */
export function createLeave(data) {
  return request.post('/leaves', data)
}

/** 修改 */
export function updateLeave(id, data) {
  return request.put(`/leaves/${id}`, data)
}

/** 删除 */
export function deleteLeave(id) {
  return request.delete(`/leaves/${id}`)
}

/** 批量删除 */
export function batchDeleteLeaves(ids) {
  return request.delete('/leaves/batch', { data: ids })
}

/** 多级审批：通过当前一级 */
export function approveLeave(id, data) {
  return request.post(`/leaves/${id}/approve`, data || {})
}

/** 多级审批：驳回 */
export function rejectLeave(id, data) {
  return request.post(`/leaves/${id}/reject`, data || {})
}
