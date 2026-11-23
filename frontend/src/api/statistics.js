import request from '../utils/request'

/** 工作量登记分页 + 多条件组合查询 */
export function getWorkloads(params) {
  return request.get('/workloads', { params })
}

/** 工作统计：总量 / 记录数 / 按科室 / 按人员 / 按月汇总 */
export function getWorkloadStats() {
  return request.get('/workloads/stats')
}

/** 工作量按科室汇总统计 */
export function getWorkloadStatsByDept() {
  return request.get('/workloads/stats/by-dept')
}

/** 工作量按人员汇总统计 */
export function getWorkloadStatsByPhysician() {
  return request.get('/workloads/stats/by-physician')
}

/** 工作量按月趋势统计 */
export function getWorkloadStatsByMonth() {
  return request.get('/workloads/stats/by-month')
}

/** 新增工作量登记（同一医师同一统计月份只允许一条） */
export function createWorkload(data) {
  return request.post('/workloads', data)
}

/** 修改工作量登记 */
export function updateWorkload(id, data) {
  return request.put(`/workloads/${id}`, data)
}

/** 删除工作量登记 */
export function deleteWorkload(id) {
  return request.delete(`/workloads/${id}`)
}

/** 批量删除工作量登记 */
export function batchDeleteWorkloads(ids) {
  return request.delete('/workloads/batch', { data: ids })
}

/** 导出工作量登记 Excel（按当前查询条件） */
export function exportWorkloads(params) {
  return request.get('/workloads/export', { params, responseType: 'blob' })
}

/** 工作量登记导入模板下载 */
export function downloadWorkloadTemplate() {
  return request.get('/workloads/template', { responseType: 'blob' })
}

/** 三级医院医疗质量指标分页 + 多条件组合查询 */
export function getQualityIndicators(params) {
  return request.get('/quality-indicators', { params })
}

/** 指标统计：总数 / 已填报数 / 达标数 / 达标率 / 按标准级别 / 按分类汇总 */
export function getQualityIndicatorStats() {
  return request.get('/quality-indicators/stats')
}

/** 指标按标准级别汇总统计 */
export function getQualityIndicatorStatsByLevel() {
  return request.get('/quality-indicators/stats/by-level')
}

/** 指标按分类汇总统计 */
export function getQualityIndicatorStatsByCategory() {
  return request.get('/quality-indicators/stats/by-category')
}

/** 新增医疗质量指标（指标编码全院唯一） */
export function createQualityIndicator(data) {
  return request.post('/quality-indicators', data)
}

/** 修改医疗质量指标 */
export function updateQualityIndicator(id, data) {
  return request.put(`/quality-indicators/${id}`, data)
}

/** 删除医疗质量指标 */
export function deleteQualityIndicator(id) {
  return request.delete(`/quality-indicators/${id}`)
}

/** 批量删除医疗质量指标 */
export function batchDeleteQualityIndicators(ids) {
  return request.delete('/quality-indicators/batch', { data: ids })
}

/** 导出三级医院医疗质量指标 Excel（按当前查询条件） */
export function exportQualityIndicators(params) {
  return request.get('/quality-indicators/export', { params, responseType: 'blob' })
}

/** 三级医院医疗质量指标导入模板下载 */
export function downloadQualityIndicatorTemplate() {
  return request.get('/quality-indicators/template', { responseType: 'blob' })
}

/** 三组统计一次取全：工作统计 + 三级医院指标统计 + 医疗质量统计 */
export function getStatisticsOverview() {
  return request.get('/statistics/overview')
}

/** 工作统计（24）：工作量登记汇总 + 排班人次（另一口径，单独列示） */
export function getStatisticsWork() {
  return request.get('/statistics/work')
}

/** 三级医院医疗质量指标统计（25）：指标表汇总 */
export function getStatisticsTertiary() {
  return request.get('/statistics/tertiary')
}

/** 医疗质量统计（26）：医疗质量 / 医疗安全 / 患者安全三组实时聚合指标 */
export function getStatisticsQuality() {
  return request.get('/statistics/quality')
}

/** 导出统计报表 Excel：工作统计（按科室）、三级医院指标、医疗质量统计三个 sheet */
export function exportStatisticsReport() {
  return request.get('/statistics/export', { responseType: 'blob' })
}
