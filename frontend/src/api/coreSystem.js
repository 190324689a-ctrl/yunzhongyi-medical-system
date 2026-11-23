import request from '../utils/request'

/** 核心制度：分页 + 多条件组合查询 */
export function getCoreSystems(params) {
  return request.get('/core-systems', { params })
}

/** 核心制度统计：总数 / 草稿 / 已发布 / 已废止 / 按类别分组 / 按层级分组 / 30 天内到期 / 已过期 / 查阅学习统计 */
export function getCoreSystemStats() {
  return request.get('/core-systems/stats')
}

/** 按制度类别汇总统计 */
export function getCoreSystemStatsByCategory() {
  return request.get('/core-systems/stats/by-category')
}

/** 按适用层级汇总统计 */
export function getCoreSystemStatsByLevel() {
  return request.get('/core-systems/stats/by-level')
}

/** 按月发布趋势统计 */
export function getCoreSystemStatsByMonth() {
  return request.get('/core-systems/stats/by-month')
}

/** 制度详情（含正文，用于在线阅读） */
export function getCoreSystemById(id) {
  return request.get(`/core-systems/${id}`)
}

/** 新增制度（默认草稿、V1.0、适用科室默认“全院”） */
export function createCoreSystem(data) {
  return request.post('/core-systems', data)
}

/** 修改制度（只有草稿能修改；已发布的制度更新走新建版本） */
export function updateCoreSystem(id, data) {
  return request.put(`/core-systems/${id}`, data)
}

/** 删除制度（已发布的制度不能直接删除，请先废止） */
export function deleteCoreSystem(id) {
  return request.delete(`/core-systems/${id}`)
}

/** 批量删除制度 */
export function batchDeleteCoreSystems(ids) {
  return request.delete('/core-systems/batch', { data: ids })
}

/** 发布（一级审批）：只有草稿能发布，审批人必填 */
export function publishCoreSystem(id, data) {
  return request.post(`/core-systems/${id}/publish`, data || {})
}

/** 废止：只有已发布的制度能废止，废止原因必填 */
export function abolishCoreSystem(id, data) {
  return request.post(`/core-systems/${id}/abolish`, data || {})
}

/** 新建版本：只有已发布的制度能新建版本，生成草稿记录（版本号 +1） */
export function newVersionCoreSystem(id) {
  return request.post(`/core-systems/${id}/new-version`)
}

/** 下载制度正文（.txt） */
export function downloadCoreSystem(id) {
  return request.get(`/core-systems/${id}/download`, { responseType: 'blob' })
}

/** 导出核心制度 Excel（按当前查询条件） */
export function exportCoreSystems(params) {
  return request.get('/core-systems/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadCoreSystemTemplate() {
  return request.get('/core-systems/template', { responseType: 'blob' })
}

/** 查某项制度的全部查阅学习记录（按查阅日期降序） */
export function getCoreSystemReadingsByCoreSystem(coreSystemId) {
  return request.get(`/core-system-readings/by-core-system/${coreSystemId}`)
}

/** 登记查阅学习：只有已发布的制度才能登记 */
export function createCoreSystemReading(data) {
  return request.post('/core-system-readings', data)
}

/** 标记已学习：已查阅 → 已学习 */
export function markCoreSystemReadingLearned(id) {
  return request.post(`/core-system-readings/${id}/learned`)
}

/** 删除一条查阅学习记录 */
export function deleteCoreSystemReading(id) {
  return request.delete(`/core-system-readings/${id}`)
}
