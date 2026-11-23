import request from '../utils/request'

// ================= 检查项目 =================
/** 检查项目：分页 + 多条件组合查询 */
export function getInspectionItems(params) {
  return request.get('/inspection-items', { params })
}

/** 检查项目统计：总数 / 已启用 / 已停用 / 按项目分类分组 / 按检查方式分组 / 按适用检查类型分组 / 已组装进模板数 / 未组装进模板数 */
export function getInspectionItemStats() {
  return request.get('/inspection-items/stats')
}

/** 按项目分类汇总统计 */
export function getInspectionItemStatsByCategory() {
  return request.get('/inspection-items/stats/by-category')
}

/** 检查项目详情 */
export function getInspectionItemById(id) {
  return request.get(`/inspection-items/${id}`)
}

/** 新增检查项目（默认启用） */
export function createInspectionItem(data) {
  return request.post('/inspection-items', data)
}

/** 修改检查项目 */
export function updateInspectionItem(id, data) {
  return request.put(`/inspection-items/${id}`, data)
}

/** 删除检查项目（已被模板引用的项目不能直接删除） */
export function deleteInspectionItem(id) {
  return request.delete(`/inspection-items/${id}`)
}

/** 批量删除检查项目 */
export function batchDeleteInspectionItems(ids) {
  return request.delete('/inspection-items/batch', { data: ids })
}

/** 启用：只有停用的项目能启用 */
export function enableInspectionItem(id) {
  return request.post(`/inspection-items/${id}/enable`)
}

/** 停用：只有启用的项目能停用；已被模板引用的项目不可停用 */
export function disableInspectionItem(id) {
  return request.post(`/inspection-items/${id}/disable`)
}

/** 导出检查项目 Excel（按当前查询条件） */
export function exportInspectionItems(params) {
  return request.get('/inspection-items/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadInspectionItemTemplate() {
  return request.get('/inspection-items/template', { responseType: 'blob' })
}

// ================= 检查标准 =================
/** 检查标准：分页 + 多条件组合查询 */
export function getInspectionStandards(params) {
  return request.get('/inspection-standards', { params })
}

/** 检查标准统计：总数 / 现行 / 已废止 / 按标准级别分组 / 30 天内到期 / 已过期 / 被检查项目引用数 */
export function getInspectionStandardStats() {
  return request.get('/inspection-standards/stats')
}

/** 按标准级别汇总统计 */
export function getInspectionStandardStatsByLevel() {
  return request.get('/inspection-standards/stats/by-level')
}

/** 检查标准详情 */
export function getInspectionStandardById(id) {
  return request.get(`/inspection-standards/${id}`)
}

/** 新增检查标准（默认现行） */
export function createInspectionStandard(data) {
  return request.post('/inspection-standards', data)
}

/** 修改检查标准 */
export function updateInspectionStandard(id, data) {
  return request.put(`/inspection-standards/${id}`, data)
}

/** 删除检查标准（已被检查项目引用的标准不能直接删除） */
export function deleteInspectionStandard(id) {
  return request.delete(`/inspection-standards/${id}`)
}

/** 批量删除检查标准 */
export function batchDeleteInspectionStandards(ids) {
  return request.delete('/inspection-standards/batch', { data: ids })
}

/** 废止：只有现行的标准能废止，废止原因必填 */
export function abolishInspectionStandard(id, data) {
  return request.post(`/inspection-standards/${id}/abolish`, data || {})
}

/** 恢复现行：只有已废止的标准能恢复 */
export function restoreInspectionStandard(id) {
  return request.post(`/inspection-standards/${id}/restore`)
}

/** 导出检查标准 Excel（按当前查询条件） */
export function exportInspectionStandards(params) {
  return request.get('/inspection-standards/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadInspectionStandardTemplate() {
  return request.get('/inspection-standards/template', { responseType: 'blob' })
}

// ================= 模板检查项配置 =================
/** 模板检查项配置：分页 + 多条件组合查询 */
export function getInspectionTemplateItems(params) {
  return request.get('/inspection-template-items', { params })
}

/** 模板配置统计：已配置模板数 / 配置记录总数 / 平均每模板检查项数 / 未配置检查项的模板数 / 未组装进任何模板的项目数 */
export function getInspectionTemplateItemStats() {
  return request.get('/inspection-template-items/stats')
}

/** 查某模板的全部检查项（按排序号升序） */
export function getInspectionTemplateItemsByTemplate(templateId) {
  return request.get(`/inspection-template-items/by-template/${templateId}`)
}

/** 模板检查项配置详情 */
export function getInspectionTemplateItemById(id) {
  return request.get(`/inspection-template-items/${id}`)
}

/** 配置模板检查项（整模板替换，权重之和须为 100，保存后同步回写模板 JSON） */
export function configureInspectionTemplateItems(data) {
  return request.post('/inspection-template-items/configure', data)
}

/** 删除单项（一个模板至少保留一项，删除后同步回写模板 JSON） */
export function deleteInspectionTemplateItem(id) {
  return request.delete(`/inspection-template-items/${id}`)
}

/** 批量删除模板检查项 */
export function batchDeleteInspectionTemplateItems(ids) {
  return request.delete('/inspection-template-items/batch', { data: ids })
}

/** 导出指定模板的检查项 Excel */
export function exportInspectionTemplateItems(params) {
  return request.get('/inspection-template-items/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadInspectionTemplateItemTemplate(templateId) {
  return request.get('/inspection-template-items/template', { params: { templateId }, responseType: 'blob' })
}

// ================= 检查分类 =================
/** 检查分类：分页 + 多条件组合查询 */
export function getInspectionCategories(params) {
  return request.get('/inspection-categories', { params })
}

/** 检查分类统计：总数 / 已启用 / 已停用 / 一级 / 二级 / 按适用检查类型分组 / 归属项目数 / 有项目归属的分类数 / 已组装进模板的项目数 / 涉及模板数 */
export function getInspectionCategoryStats() {
  return request.get('/inspection-categories/stats')
}

/** 按分类汇总统计（每个分类的项目数、已启用项目数、涉及模板数、下级数） */
export function getInspectionCategoryStatsByCategory() {
  return request.get('/inspection-categories/stats/by-category')
}

/** 分类树（一级分类带下级） */
export function getInspectionCategoryTree() {
  return request.get('/inspection-categories/tree')
}

/** 全部启用的分类（新建检查项目时下拉用） */
export function getEnabledInspectionCategories() {
  return request.get('/inspection-categories/enabled')
}

/** 检查分类详情 */
export function getInspectionCategoryById(id) {
  return request.get(`/inspection-categories/${id}`)
}

/** 新增检查分类（默认启用、适用类型默认通用） */
export function createInspectionCategory(data) {
  return request.post('/inspection-categories', data)
}

/** 修改检查分类（已有项目归属时编码不可改） */
export function updateInspectionCategory(id, data) {
  return request.put(`/inspection-categories/${id}`, data)
}

/** 删除检查分类（已有下级、或已有检查项目归属的分类不能直接删除） */
export function deleteInspectionCategory(id) {
  return request.delete(`/inspection-categories/${id}`)
}

/** 批量删除检查分类 */
export function batchDeleteInspectionCategories(ids) {
  return request.delete('/inspection-categories/batch', { data: ids })
}

/** 启用：只有停用的分类能启用；父分类停用时子分类不可启用 */
export function enableInspectionCategory(id) {
  return request.post(`/inspection-categories/${id}/enable`)
}

/** 停用：只有启用的分类能停用；已有检查项目归属、或有启用下级的分类不可停用 */
export function disableInspectionCategory(id) {
  return request.post(`/inspection-categories/${id}/disable`)
}

/** 导出检查分类 Excel（按当前查询条件） */
export function exportInspectionCategories(params) {
  return request.get('/inspection-categories/export', { params, responseType: 'blob' })
}

/** 导入模板下载 */
export function downloadInspectionCategoryTemplate() {
  return request.get('/inspection-categories/template', { responseType: 'blob' })
}
