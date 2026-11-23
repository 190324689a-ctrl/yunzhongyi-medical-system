import request from '../utils/request'

/** 档案：分页 + 多条件组合查询 */
export function getRadiationPersonnel(params) {
  return request.get('/radiation-personnel', { params })
}

/** 档案统计：档案总数 / 在岗 / 停岗 / 离岗 / 按放射类别分组 / 按科室汇总 / 资质 / 培训 / 健康聚合统计 */
export function getRadiationPersonnelStats() {
  return request.get('/radiation-personnel/stats')
}

/** 按科室汇总统计 */
export function getRadiationPersonnelStatsByDept() {
  return request.get('/radiation-personnel/stats/by-dept')
}

/** 按放射类别汇总统计 */
export function getRadiationPersonnelStatsByWorkType() {
  return request.get('/radiation-personnel/stats/by-work-type')
}

/** 档案详情 */
export function getRadiationPersonnelById(id) {
  return request.get(`/radiation-personnel/${id}`)
}

/** 新增放射人员档案（默认在岗） */
export function createRadiationPersonnel(data) {
  return request.post('/radiation-personnel', data)
}

/** 修改档案 */
export function updateRadiationPersonnel(id, data) {
  return request.put(`/radiation-personnel/${id}`, data)
}

/** 删除档案 */
export function deleteRadiationPersonnel(id) {
  return request.delete(`/radiation-personnel/${id}`)
}

/** 批量删除档案 */
export function batchDeleteRadiationPersonnel(ids) {
  return request.delete('/radiation-personnel/batch', { data: ids })
}

/** 导出档案 Excel（按当前查询条件） */
export function exportRadiationPersonnel(params) {
  return request.get('/radiation-personnel/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadRadiationPersonnelTemplate() {
  return request.get('/radiation-personnel/template', { responseType: 'blob' })
}

/** 资质：分页 + 多条件组合查询（含只看 90 天内到期） */
export function getRadiationQualifications(params) {
  return request.get('/radiation-qualifications', { params })
}

/** 查某放射人员的全部资质（按有效期升序） */
export function getQualificationsByPersonnel(personnelId) {
  return request.get(`/radiation-qualifications/by-personnel/${personnelId}`)
}

/** 新增资质登记（默认待审核） */
export function createRadiationQualification(data) {
  return request.post('/radiation-qualifications', data)
}

/** 修改资质（只有待审核的资质能修改） */
export function updateRadiationQualification(id, data) {
  return request.put(`/radiation-qualifications/${id}`, data)
}

/** 删除资质 */
export function deleteRadiationQualification(id) {
  return request.delete(`/radiation-qualifications/${id}`)
}

/** 资质一级审核通过（医务科） */
export function approveRadiationQualification(id, data) {
  return request.post(`/radiation-qualifications/${id}/approve`, data)
}

/** 资质一级审核驳回（医务科） */
export function rejectRadiationQualification(id, data) {
  return request.post(`/radiation-qualifications/${id}/reject`, data)
}

/** 培训：分页 + 多条件组合查询 */
export function getRadiationTrainings(params) {
  return request.get('/radiation-trainings', { params })
}

/** 查某放射人员的全部培训记录（按培训日期降序） */
export function getTrainingsByPersonnel(personnelId) {
  return request.get(`/radiation-trainings/by-personnel/${personnelId}`)
}

/** 新增培训记录（结果默认合格） */
export function createRadiationTraining(data) {
  return request.post('/radiation-trainings', data)
}

/** 修改培训记录 */
export function updateRadiationTraining(id, data) {
  return request.put(`/radiation-trainings/${id}`, data)
}

/** 删除培训记录 */
export function deleteRadiationTraining(id) {
  return request.delete(`/radiation-trainings/${id}`)
}

/** 健康：分页 + 多条件组合查询（含只看剂量超标） */
export function getRadiationHealth(params) {
  return request.get('/radiation-health', { params })
}

/** 查某放射人员的全部体检记录（按体检日期降序） */
export function getHealthByPersonnel(personnelId) {
  return request.get(`/radiation-health/by-personnel/${personnelId}`)
}

/** 新增体检记录（结论默认合格） */
export function createRadiationHealth(data) {
  return request.post('/radiation-health', data)
}

/** 修改体检记录 */
export function updateRadiationHealth(id, data) {
  return request.put(`/radiation-health/${id}`, data)
}

/** 删除体检记录 */
export function deleteRadiationHealth(id) {
  return request.delete(`/radiation-health/${id}`)
}
