import request from '../utils/request'

/** 文件制度：分页 + 多条件组合查询 */
export function getDocuments(params) {
  return request.get('/documents', { params })
}

/** 文件制度统计：总数 / 草稿 / 已发布 / 已作废 / 按分类分组 / 30 天内到期 / 已过期 */
export function getDocumentStats() {
  return request.get('/documents/stats')
}

/** 按文件分类汇总统计 */
export function getDocumentStatsByCategory() {
  return request.get('/documents/stats/by-category')
}

/** 按月发布趋势统计 */
export function getDocumentStatsByMonth() {
  return request.get('/documents/stats/by-month')
}

/** 文件详情（含正文，用于在线阅读） */
export function getDocumentById(id) {
  return request.get(`/documents/${id}`)
}

/** 新增文件（默认草稿、V1.0） */
export function createDocument(data) {
  return request.post('/documents', data)
}

/** 修改文件（只有草稿能修改；已发布的文件改版走新建版本） */
export function updateDocument(id, data) {
  return request.put(`/documents/${id}`, data)
}

/** 删除文件（已发布的文件不能直接删除，请先作废） */
export function deleteDocument(id) {
  return request.delete(`/documents/${id}`)
}

/** 批量删除文件 */
export function batchDeleteDocuments(ids) {
  return request.delete('/documents/batch', { data: ids })
}

/** 发布（一级审批）：只有草稿能发布，审批人必填 */
export function publishDocument(id, data) {
  return request.post(`/documents/${id}/publish`, data || {})
}

/** 作废：只有已发布的文件能作废，作废原因必填 */
export function abolishDocument(id, data) {
  return request.post(`/documents/${id}/abolish`, data || {})
}

/** 新建版本：只有已发布的文件能新建版本，生成草稿记录（版本号 +1） */
export function newVersionDocument(id) {
  return request.post(`/documents/${id}/new-version`)
}

/** 下载文件正文（.txt） */
export function downloadDocument(id) {
  return request.get(`/documents/${id}/download`, { responseType: 'blob' })
}

/** 导出文件制度 Excel（按当前查询条件） */
export function exportDocuments(params) {
  return request.get('/documents/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadDocumentTemplate() {
  return request.get('/documents/template', { responseType: 'blob' })
}
