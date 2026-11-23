import request from '../utils/request'

/** 分页 + 多条件组合查询 */
export function getPhysicians(params) {
  return request.get('/physicians', { params })
}

/** 详情 */
export function getPhysician(id) {
  return request.get(`/physicians/${id}`)
}

/** 新增 */
export function createPhysician(data) {
  return request.post('/physicians', data)
}

/** 修改 */
export function updatePhysician(id, data) {
  return request.put(`/physicians/${id}`, data)
}

/** 删除 */
export function deletePhysician(id) {
  return request.delete(`/physicians/${id}`)
}

/** 批量删除 */
export function batchDeletePhysicians(ids) {
  return request.delete('/physicians/batch', { data: ids })
}

/** 导出 Excel（返回 blob） */
export function exportPhysicians(params) {
  return request.get('/physicians/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadTemplate() {
  return request.get('/physicians/template', { responseType: 'blob' })
}
