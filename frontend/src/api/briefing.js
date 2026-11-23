import request from '../utils/request'

// ---------- 医疗工作简报 ----------
/** 简报分页 + 多条件组合查询（关键字 / 分类 / 发布状态 / 统计周期 / 模板） */
export function getBriefings(params) {
  return request.get('/briefings', { params })
}

/** 简报统计：总数 / 草稿 / 待审批 / 已发布 / 已驳回 / 按分类分组 / 模板数 */
export function getBriefingStats() {
  return request.get('/briefings/stats')
}

/** 按分类汇总统计 */
export function getBriefingStatsByCategory() {
  return request.get('/briefings/stats/by-category')
}

/** 按统计周期汇总统计（按周期升序） */
export function getBriefingStatsByMonth() {
  return request.get('/briefings/stats/by-month')
}

/** 简报详情 */
export function getBriefing(id) {
  return request.get(`/briefings/${id}`)
}

/** 新增简报（默认草稿） */
export function createBriefing(data) {
  return request.post('/briefings', data)
}

/** 修改简报（只有草稿与已驳回的简报能修改） */
export function updateBriefing(id, data) {
  return request.put(`/briefings/${id}`, data)
}

/** 删除简报（已发布的简报不能直接删除） */
export function deleteBriefing(id) {
  return request.delete(`/briefings/${id}`)
}

/** 批量删除简报（已发布的简报不能直接删除） */
export function deleteBriefings(ids) {
  return request.delete('/briefings/batch', { data: ids })
}

/** 提交审批：草稿 / 已驳回 → 待审批 */
export function submitBriefing(id) {
  return request.post(`/briefings/${id}/submit`)
}

/** 审批通过：待审批 → 已发布，发布日期默认为当天，审批记录留痕 */
export function approveBriefing(id, params) {
  return request.post(`/briefings/${id}/approve`, null, { params })
}

/** 审批驳回：待审批 → 已驳回，驳回原因必填，审批记录留痕 */
export function rejectBriefing(id, params) {
  return request.post(`/briefings/${id}/reject`, null, { params })
}

/** 按模板自动生成简报：从各业务模块聚合本期数据写正文，生成结果为草稿 */
export function generateBriefing(data) {
  return request.post('/briefings/generate', data)
}

/** 导出 Word（.docx，Apache POI 真实文件） */
export function exportBriefingWord(id) {
  return request.get(`/briefings/${id}/export-word`, { responseType: 'blob' })
}

/** 导出 PDF（OpenPDF + 嵌入简体中文字体，真实 PDF 文件） */
export function exportBriefingPdf(id) {
  return request.get(`/briefings/${id}/export-pdf`, { responseType: 'blob' })
}

/** 下载正文纯文本（.txt） */
export function downloadBriefingText(id) {
  return request.get(`/briefings/${id}/download`, { responseType: 'blob' })
}

/** 导出简报 Excel */
export function exportBriefings(params) {
  return request.get('/briefings/export', { params, responseType: 'blob' })
}

/** 批量导入简报 Excel（导入的均为草稿，需走提交审批流程） */
export function importBriefings(data) {
  return request.post('/briefings/import', data)
}

/** 下载简报导入模板 */
export function downloadBriefingTemplate() {
  return request.get('/briefings/template', { responseType: 'blob' })
}

// ---------- 简报模板 ----------
/** 简报模板分页 + 多条件组合查询（关键字 / 分类 / 启用状态） */
export function getBriefingTemplates(params) {
  return request.get('/briefing-templates', { params })
}

/** 全部启用的模板（新建/自动生成简报时下拉用） */
export function getEnabledBriefingTemplates() {
  return request.get('/briefing-templates/enabled')
}

/** 简报模板详情 */
export function getBriefingTemplate(id) {
  return request.get(`/briefing-templates/${id}`)
}

/** 新增简报模板（默认启用、默认月报分类） */
export function createBriefingTemplate(data) {
  return request.post('/briefing-templates', data)
}

/** 修改简报模板（启用状态不在此改，走启用/停用接口） */
export function updateBriefingTemplate(id, data) {
  return request.put(`/briefing-templates/${id}`, data)
}

/** 删除简报模板（已被简报引用的模板不允许删除） */
export function deleteBriefingTemplate(id) {
  return request.delete(`/briefing-templates/${id}`)
}

/** 启用简报模板 */
export function enableBriefingTemplate(id) {
  return request.post(`/briefing-templates/${id}/enable`)
}

/** 停用简报模板（停用的模板不能用来新建/自动生成简报） */
export function disableBriefingTemplate(id) {
  return request.post(`/briefing-templates/${id}/disable`)
}
