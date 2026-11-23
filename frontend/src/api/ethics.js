import request from '../utils/request'

// ---------- 医德医风档案 ----------
/** 档案：分页 + 多条件组合查询 */
export function getEthicsProfiles(params) {
  return request.get('/ethics-profiles', { params })
}

/** 档案统计：总数 / 在档 / 封存 / 按科室汇总 / 已通过表彰数 / 投诉总数 / 考评平均分与优秀率 / 逾期投诉数 */
export function getEthicsProfileStats() {
  return request.get('/ethics-profiles/stats')
}

/** 档案按科室汇总统计 */
export function getEthicsProfileStatsByDept() {
  return request.get('/ethics-profiles/stats/by-dept')
}

/** 档案详情 */
export function getEthicsProfileById(id) {
  return request.get(`/ethics-profiles/${id}`)
}

/** 新增档案（默认在档；一名医师只能建一份档案） */
export function createEthicsProfile(data) {
  return request.post('/ethics-profiles', data)
}

/** 修改档案 */
export function updateEthicsProfile(id, data) {
  return request.put(`/ethics-profiles/${id}`, data)
}

/** 删除档案 */
export function deleteEthicsProfile(id) {
  return request.delete(`/ethics-profiles/${id}`)
}

/** 批量删除档案 */
export function batchDeleteEthicsProfiles(ids) {
  return request.delete('/ethics-profiles/batch', { data: ids })
}

/** 导出档案 Excel（按当前查询条件） */
export function exportEthicsProfiles(params) {
  return request.get('/ethics-profiles/export', { params, responseType: 'blob' })
}

/** 档案导入模板下载 */
export function downloadEthicsProfileTemplate() {
  return request.get('/ethics-profiles/template', { responseType: 'blob' })
}

// ---------- 医德考评模板 ----------
/** 考评模板：分页 + 多条件组合查询 */
export function getEthicsTemplates(params) {
  return request.get('/ethics-templates', { params })
}

/** 全部启用的考评模板（新建考评记录时下拉用） */
export function getEnabledEthicsTemplates() {
  return request.get('/ethics-templates/enabled')
}

/** 新增考评模板（评分项权重之和必须为 100） */
export function createEthicsTemplate(data) {
  return request.post('/ethics-templates', data)
}

/** 修改考评模板 */
export function updateEthicsTemplate(id, data) {
  return request.put(`/ethics-templates/${id}`, data)
}

/** 删除考评模板 */
export function deleteEthicsTemplate(id) {
  return request.delete(`/ethics-templates/${id}`)
}

// ---------- 医德考评 ----------
/** 考评：分页 + 多条件组合查询 */
export function getEthicsEvaluations(params) {
  return request.get('/ethics-evaluations', { params })
}

/** 考评统计：总数、平均分、优秀人数与优秀率、按等级分组、按科室汇总 */
export function getEthicsEvaluationStats() {
  return request.get('/ethics-evaluations/stats')
}

/** 考评按科室汇总统计 */
export function getEthicsEvaluationStatsByDept() {
  return request.get('/ethics-evaluations/stats/by-dept')
}

/** 新增考评记录：由各项得分求和自动算总分，再按总分自动算等级；默认待审批 */
export function createEthicsEvaluation(data) {
  return request.post('/ethics-evaluations', data)
}

/** 修改考评记录（只有待审批的考评能修改） */
export function updateEthicsEvaluation(id, data) {
  return request.put(`/ethics-evaluations/${id}`, data)
}

/** 删除考评记录（已通过的考评不能直接删除） */
export function deleteEthicsEvaluation(id) {
  return request.delete(`/ethics-evaluations/${id}`)
}

/** 批量删除考评记录 */
export function batchDeleteEthicsEvaluations(ids) {
  return request.delete('/ethics-evaluations/batch', { data: ids })
}

/** 一级审批通过：只有待审批的考评能通过；通过时同步医德档案的最近考评结果 */
export function approveEthicsEvaluation(id, data) {
  return request.post(`/ethics-evaluations/${id}/approve`, data || {})
}

/** 一级审批驳回：只有待审批的考评能驳回，驳回原因必填 */
export function rejectEthicsEvaluation(id, data) {
  return request.post(`/ethics-evaluations/${id}/reject`, data || {})
}

/** 导出考评 Excel（按当前查询条件） */
export function exportEthicsEvaluations(params) {
  return request.get('/ethics-evaluations/export', { params, responseType: 'blob' })
}

/** 考评导入模板下载 */
export function downloadEthicsEvaluationTemplate() {
  return request.get('/ethics-evaluations/template', { responseType: 'blob' })
}

// ---------- 医德医风投诉 ----------
/** 投诉：分页 + 多条件组合查询 */
export function getEthicsComplaints(params) {
  return request.get('/ethics-complaints', { params })
}

/** 投诉统计：总数、按流转状态分组、按投诉类型分组、按科室汇总、逾期数、反馈率 */
export function getEthicsComplaintStats() {
  return request.get('/ethics-complaints/stats')
}

/** 投诉按科室汇总统计 */
export function getEthicsComplaintStatsByDept() {
  return request.get('/ethics-complaints/stats/by-dept')
}

/** 投诉详情 */
export function getEthicsComplaintById(id) {
  return request.get(`/ethics-complaints/${id}`)
}

/** 新增投诉登记：默认待受理，处理期限默认为投诉日期 + 15 天 */
export function createEthicsComplaint(data) {
  return request.post('/ethics-complaints', data)
}

/** 修改投诉（只有待受理/调查中的投诉能修改） */
export function updateEthicsComplaint(id, data) {
  return request.put(`/ethics-complaints/${id}`, data)
}

/** 删除投诉 */
export function deleteEthicsComplaint(id) {
  return request.delete(`/ethics-complaints/${id}`)
}

/** 批量删除投诉 */
export function batchDeleteEthicsComplaints(ids) {
  return request.delete('/ethics-complaints/batch', { data: ids })
}

/** 受理：待受理 → 调查中 */
export function acceptEthicsComplaint(id, data) {
  return request.post(`/ethics-complaints/${id}/accept`, data || {})
}

/** 处理：调查中 → 已处理，处理结果必填 */
export function handleEthicsComplaint(id, data) {
  return request.post(`/ethics-complaints/${id}/handle`, data || {})
}

/** 反馈：已处理 → 已反馈，反馈内容必填 */
export function feedbackEthicsComplaint(id, data) {
  return request.post(`/ethics-complaints/${id}/feedback`, data || {})
}

/** 不予受理：待受理 → 不予受理，原因必填 */
export function rejectEthicsComplaint(id, data) {
  return request.post(`/ethics-complaints/${id}/reject`, data || {})
}

/** 导出投诉 Excel（按当前查询条件） */
export function exportEthicsComplaints(params) {
  return request.get('/ethics-complaints/export', { params, responseType: 'blob' })
}

/** 投诉导入模板下载 */
export function downloadEthicsComplaintTemplate() {
  return request.get('/ethics-complaints/template', { responseType: 'blob' })
}

// ---------- 医德医风表彰 ----------
/** 表彰：分页 + 多条件组合查询 */
export function getEthicsCommendations(params) {
  return request.get('/ethics-commendations', { params })
}

/** 表彰统计：总数、待审批、已通过、已驳回、按表彰类型分组、按表彰级别分组、按科室汇总 */
export function getEthicsCommendationStats() {
  return request.get('/ethics-commendations/stats')
}

/** 表彰按科室汇总统计 */
export function getEthicsCommendationStatsByDept() {
  return request.get('/ethics-commendations/stats/by-dept')
}

/** 新增表彰登记：默认待审批 */
export function createEthicsCommendation(data) {
  return request.post('/ethics-commendations', data)
}

/** 修改表彰（只有待审批的表彰能修改） */
export function updateEthicsCommendation(id, data) {
  return request.put(`/ethics-commendations/${id}`, data)
}

/** 删除表彰（已通过的表彰不能直接删除） */
export function deleteEthicsCommendation(id) {
  return request.delete(`/ethics-commendations/${id}`)
}

/** 批量删除表彰 */
export function batchDeleteEthicsCommendations(ids) {
  return request.delete('/ethics-commendations/batch', { data: ids })
}

/** 一级审批通过：只有待审批的表彰能通过 */
export function approveEthicsCommendation(id, data) {
  return request.post(`/ethics-commendations/${id}/approve`, data || {})
}

/** 一级审批驳回：只有待审批的表彰能驳回，驳回原因必填 */
export function rejectEthicsCommendation(id, data) {
  return request.post(`/ethics-commendations/${id}/reject`, data || {})
}

/** 导出表彰 Excel（按当前查询条件） */
export function exportEthicsCommendations(params) {
  return request.get('/ethics-commendations/export', { params, responseType: 'blob' })
}

/** 表彰导入模板下载 */
export function downloadEthicsCommendationTemplate() {
  return request.get('/ethics-commendations/template', { responseType: 'blob' })
}
