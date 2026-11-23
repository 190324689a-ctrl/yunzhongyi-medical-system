import request from '../utils/request'

// ================= 医疗技术档案 =================
/** 技术档案：分页 + 多条件组合查询 */
export function getMedicalTechs(params) {
  return request.get('/medical-techs', { params })
}

/** 技术档案统计：总数 / 待准入 / 已准入 / 已停用 / 按分级分组 / 按准入类别分组 / 按分类分组 / 90 天内到期 / 已过期 / 准入申请统计 / 医师授权统计 */
export function getMedicalTechStats() {
  return request.get('/medical-techs/stats')
}

/** 按技术分级汇总统计 */
export function getMedicalTechStatsByLevel() {
  return request.get('/medical-techs/stats/by-level')
}

/** 按技术分类汇总统计 */
export function getMedicalTechStatsByCategory() {
  return request.get('/medical-techs/stats/by-category')
}

/** 按开展科室汇总统计 */
export function getMedicalTechStatsByDept() {
  return request.get('/medical-techs/stats/by-dept')
}

/** 技术档案详情 */
export function getMedicalTechById(id) {
  return request.get(`/medical-techs/${id}`)
}

/** 新增技术档案（默认待准入，需走准入审批流程） */
export function createMedicalTech(data) {
  return request.post('/medical-techs', data)
}

/** 修改技术档案（已准入的技术不能直接修改，分级/科室/有效期变更请走准入申请） */
export function updateMedicalTech(id, data) {
  return request.put(`/medical-techs/${id}`, data)
}

/** 删除技术档案（已准入的技术不能直接删除，请先停用） */
export function deleteMedicalTech(id) {
  return request.delete(`/medical-techs/${id}`)
}

/** 批量删除技术档案 */
export function batchDeleteMedicalTechs(ids) {
  return request.delete('/medical-techs/batch', { data: ids })
}

/** 停用：只有已准入的技术能停用，停用原因必填 */
export function suspendMedicalTech(id, data) {
  return request.post(`/medical-techs/${id}/suspend`, data || {})
}

/** 重新启用：只有已停用的技术能重新启用 */
export function resumeMedicalTech(id) {
  return request.post(`/medical-techs/${id}/resume`)
}

/** 导出技术档案 Excel（按当前查询条件） */
export function exportMedicalTechs(params) {
  return request.get('/medical-techs/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadMedicalTechTemplate() {
  return request.get('/medical-techs/template', { responseType: 'blob' })
}

// ================= 技术准入申请 =================
/** 准入申请：分页 + 多条件组合查询 */
export function getTechAdmissions(params) {
  return request.get('/tech-admissions', { params })
}

/** 准入申请统计：总数 / 待审批 / 已通过 / 已驳回 / 按申请类型分组 */
export function getTechAdmissionStats() {
  return request.get('/tech-admissions/stats')
}

/** 查某项技术的全部准入申请（按申请日期降序） */
export function getTechAdmissionsByTech(techId) {
  return request.get(`/tech-admissions/by-tech/${techId}`)
}

/** 准入申请详情 */
export function getTechAdmissionById(id) {
  return request.get(`/tech-admissions/${id}`)
}

/** 新增准入申请（默认待审批，申请日期默认为当天） */
export function createTechAdmission(data) {
  return request.post('/tech-admissions', data)
}

/** 修改准入申请（只有待审批的申请能修改） */
export function updateTechAdmission(id, data) {
  return request.put(`/tech-admissions/${id}`, data)
}

/** 删除准入申请（已通过的准入申请不能直接删除） */
export function deleteTechAdmission(id) {
  return request.delete(`/tech-admissions/${id}`)
}

/** 批量删除准入申请 */
export function batchDeleteTechAdmissions(ids) {
  return request.delete('/tech-admissions/batch', { data: ids })
}

/** 审批通过（一级审批，医务科）：只有待审批的申请能通过，通过时同步技术档案 */
export function approveTechAdmission(id, data) {
  return request.post(`/tech-admissions/${id}/approve`, data || {})
}

/** 审批驳回（一级审批，医务科）：只有待审批的申请能驳回，驳回原因必填 */
export function rejectTechAdmission(id, data) {
  return request.post(`/tech-admissions/${id}/reject`, data || {})
}

/** 导出准入申请 Excel（按当前查询条件） */
export function exportTechAdmissions(params) {
  return request.get('/tech-admissions/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadTechAdmissionTemplate() {
  return request.get('/tech-admissions/template', { responseType: 'blob' })
}

// ================= 医师技术授权 =================
/** 技术授权：分页 + 多条件组合查询 */
export function getTechAuthorizations(params) {
  return request.get('/tech-authorizations', { params })
}

/** 技术授权统计：总数 / 待审批 / 已通过 / 已驳回 / 已注销 / 按授权级别分组 / 90 天内到期 / 已过期 */
export function getTechAuthorizationStats() {
  return request.get('/tech-authorizations/stats')
}

/** 按科室汇总统计 */
export function getTechAuthorizationStatsByDept() {
  return request.get('/tech-authorizations/stats/by-dept')
}

/** 查某项技术的全部授权（按授权开始日期降序） */
export function getTechAuthorizationsByTech(techId) {
  return request.get(`/tech-authorizations/by-tech/${techId}`)
}

/** 查某位医师的全部授权（按授权开始日期降序） */
export function getTechAuthorizationsByPhysician(physicianId) {
  return request.get(`/tech-authorizations/by-physician/${physicianId}`)
}

/** 技术授权详情 */
export function getTechAuthorizationById(id) {
  return request.get(`/tech-authorizations/${id}`)
}

/** 新增授权申请（默认待审批，授权开始日期默认为当天） */
export function createTechAuthorization(data) {
  return request.post('/tech-authorizations', data)
}

/** 修改授权申请（只有待审批的授权能修改） */
export function updateTechAuthorization(id, data) {
  return request.put(`/tech-authorizations/${id}`, data)
}

/** 删除授权申请（已通过的授权不能直接删除，请先注销） */
export function deleteTechAuthorization(id) {
  return request.delete(`/tech-authorizations/${id}`)
}

/** 批量删除授权申请 */
export function batchDeleteTechAuthorizations(ids) {
  return request.delete('/tech-authorizations/batch', { data: ids })
}

/** 审批通过（一级审批，医务科）：只有待审批的授权能通过 */
export function approveTechAuthorization(id, data) {
  return request.post(`/tech-authorizations/${id}/approve`, data || {})
}

/** 审批驳回（一级审批，医务科）：只有待审批的授权能驳回，驳回原因必填 */
export function rejectTechAuthorization(id, data) {
  return request.post(`/tech-authorizations/${id}/reject`, data || {})
}

/** 注销：只有已通过的授权能注销，注销原因必填 */
export function revokeTechAuthorization(id, data) {
  return request.post(`/tech-authorizations/${id}/revoke`, data || {})
}

/** 导出技术授权 Excel（按当前查询条件） */
export function exportTechAuthorizations(params) {
  return request.get('/tech-authorizations/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadTechAuthorizationTemplate() {
  return request.get('/tech-authorizations/template', { responseType: 'blob' })
}
