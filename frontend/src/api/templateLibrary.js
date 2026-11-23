import request from '../utils/request'

/** 模板库统计：模板总数 / 启用停用 / 按检查类型分组 / 版本数 / 最常用模板 / 从未使用数 */
export function getTemplateLibraryStats() {
  return request.get('/template-library/stats')
}

/** 某模板的使用情况：计划数（按状态）、记录数、最近一次计划日期 */
export function getTemplateUsage(id) {
  return request.get(`/template-library/templates/${id}/usage`)
}

/** 复制模板：同类型同检查项的新模板，默认停用 */
export function copyQualityTemplate(id, data) {
  return request.post(`/template-library/templates/${id}/copy`, data)
}

/** 某模板的版本历史（按版本号倒序） */
export function getTemplateVersions(id) {
  return request.get(`/template-library/templates/${id}/versions`)
}

/** 某模板的当前版本 */
export function getTemplateCurrentVersion(id) {
  return request.get(`/template-library/templates/${id}/versions/current`)
}

/** 新建版本：检查项权重之和须为 100；有进行中计划时拒绝 */
export function createTemplateVersion(id, data) {
  return request.post(`/template-library/templates/${id}/versions`, data)
}

/** 版本详情 */
export function getTemplateVersion(id) {
  return request.get(`/template-library/versions/${id}`)
}

/** 从历史版本恢复：以该版本快照内容新建一版 */
export function restoreTemplateVersion(id, data) {
  return request.post(`/template-library/versions/${id}/restore`, data || {})
}

/** 导出全部模板 Excel */
export function exportTemplates() {
  return request.get('/template-library/templates/export', { responseType: 'blob' })
}

/** 导入模板下载（空表头，与导入格式一致） */
export function downloadTemplateImportTemplate() {
  return request.get('/template-library/templates/import-template', { responseType: 'blob' })
}
