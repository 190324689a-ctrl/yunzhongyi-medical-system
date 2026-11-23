import request from '../utils/request'

/** 政策法规：分页 + 多条件组合查询 */
export function getPolicies(params) {
  return request.get('/policies', { params })
}

/** 政策法规统计：总数 / 草稿 / 已发布 / 已废止 / 按分类分组 / 按层级分组 / 30 天内到期 / 已过期 / 查阅学习统计 */
export function getPolicyStats() {
  return request.get('/policies/stats')
}

/** 按法规分类汇总统计 */
export function getPolicyStatsByCategory() {
  return request.get('/policies/stats/by-category')
}

/** 按发布层级汇总统计 */
export function getPolicyStatsByLevel() {
  return request.get('/policies/stats/by-level')
}

/** 按月发布趋势统计 */
export function getPolicyStatsByMonth() {
  return request.get('/policies/stats/by-month')
}

/** 法规详情（含正文，用于在线阅读） */
export function getPolicyById(id) {
  return request.get(`/policies/${id}`)
}

/** 新增法规（默认草稿、V1.0、适用科室默认“全院”） */
export function createPolicy(data) {
  return request.post('/policies', data)
}

/** 修改法规（只有草稿能修改；已发布的法规更新走新建版本） */
export function updatePolicy(id, data) {
  return request.put(`/policies/${id}`, data)
}

/** 删除法规（已发布的法规不能直接删除，请先废止） */
export function deletePolicy(id) {
  return request.delete(`/policies/${id}`)
}

/** 批量删除法规 */
export function batchDeletePolicies(ids) {
  return request.delete('/policies/batch', { data: ids })
}

/** 发布（一级审批）：只有草稿能发布，审批人必填 */
export function publishPolicy(id, data) {
  return request.post(`/policies/${id}/publish`, data || {})
}

/** 废止：只有已发布的法规能废止，废止原因必填 */
export function abolishPolicy(id, data) {
  return request.post(`/policies/${id}/abolish`, data || {})
}

/** 新建版本：只有已发布的法规能新建版本，生成草稿记录（版本号 +1） */
export function newVersionPolicy(id) {
  return request.post(`/policies/${id}/new-version`)
}

/** 下载法规正文（.txt） */
export function downloadPolicy(id) {
  return request.get(`/policies/${id}/download`, { responseType: 'blob' })
}

/** 导出政策法规 Excel（按当前查询条件） */
export function exportPolicies(params) {
  return request.get('/policies/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadPolicyTemplate() {
  return request.get('/policies/template', { responseType: 'blob' })
}

/** 查某部法规的全部查阅学习记录（按查阅日期降序） */
export function getPolicyReadingsByPolicy(policyId) {
  return request.get(`/policy-readings/by-policy/${policyId}`)
}

/** 登记查阅学习：只有已发布的法规才能登记 */
export function createPolicyReading(data) {
  return request.post('/policy-readings', data)
}

/** 标记已学习：已查阅 → 已学习 */
export function markPolicyReadingLearned(id) {
  return request.post(`/policy-readings/${id}/learned`)
}

/** 删除一条查阅学习记录 */
export function deletePolicyReading(id) {
  return request.delete(`/policy-readings/${id}`)
}
