import request from '../utils/request'

// ---------- 系统设置总览 ----------
/** 系统设置总览：底座与新增五组配置的数量汇总 */
export function getSystemSettingsOverview() {
  return request.get('/system-settings/overview')
}

// ---------- 系统参数 ----------
/** 系统参数分页 + 多条件组合查询（关键字 / 参数分组 / 值类型 / 状态） */
export function getSysParams(params) {
  return request.get('/sys-params', { params })
}

/** 系统参数统计：总数 / 启用 / 停用 / 内置 / 自定义 / 按分组统计 */
export function getSysParamStats() {
  return request.get('/sys-params/stats')
}

/** 全部启用的参数（配置读取用） */
export function getEnabledSysParams() {
  return request.get('/sys-params/enabled')
}

/** 按参数键取启用参数（业务读取用） */
export function getSysParamByKey(paramKey) {
  return request.get(`/sys-params/by-key/${paramKey}`)
}

/** 系统参数详情 */
export function getSysParam(id) {
  return request.get(`/sys-params/${id}`)
}

/** 新增系统参数 */
export function createSysParam(data) {
  return request.post('/sys-params', data)
}

/** 修改系统参数（内置参数的键不可改） */
export function updateSysParam(id, data) {
  return request.put(`/sys-params/${id}`, data)
}

/** 删除系统参数（内置参数不可删除） */
export function deleteSysParam(id) {
  return request.delete(`/sys-params/${id}`)
}

/** 启用系统参数 */
export function enableSysParam(id) {
  return request.post(`/sys-params/${id}/enable`)
}

/** 停用系统参数 */
export function disableSysParam(id) {
  return request.post(`/sys-params/${id}/disable`)
}

/** 导出系统参数 Excel */
export function exportSysParams(params) {
  return request.get('/sys-params/export', { params, responseType: 'blob' })
}

/** 批量导入系统参数 Excel */
export function importSysParams(data) {
  return request.post('/sys-params/import', data)
}

/** 下载系统参数导入模板 */
export function downloadSysParamTemplate() {
  return request.get('/sys-params/template', { responseType: 'blob' })
}

// ---------- 数据备份 ----------
/** 数据备份分页 + 多条件组合查询（关键字 / 备份类型 / 状态 / 只看已恢复） */
export function getSysBackups(params) {
  return request.get('/sys-backups', { params })
}

/** 数据备份统计：总数 / 成功 / 失败 / 进行中 / 已恢复数 / 总记录数 / 按类型分组 */
export function getSysBackupStats() {
  return request.get('/sys-backups/stats')
}

/** 数据备份详情 */
export function getSysBackup(id) {
  return request.get(`/sys-backups/${id}`)
}

/** 执行备份（统计全库行数、生成备份文件名、登记为成功） */
export function createSysBackup(data) {
  return request.post('/sys-backups', data)
}

/** 恢复备份：只有成功的备份能恢复 */
export function restoreSysBackup(id) {
  return request.post(`/sys-backups/${id}/restore`)
}

/** 删除备份（已恢复过的备份不可直接删除） */
export function deleteSysBackup(id) {
  return request.delete(`/sys-backups/${id}`)
}

/** 导出数据备份 Excel */
export function exportSysBackups(params) {
  return request.get('/sys-backups/export', { params, responseType: 'blob' })
}

/** 批量导入数据备份 Excel（导入的是历史备份记录） */
export function importSysBackups(data) {
  return request.post('/sys-backups/import', data)
}

/** 下载数据备份导入模板 */
export function downloadSysBackupTemplate() {
  return request.get('/sys-backups/template', { responseType: 'blob' })
}

// ---------- 系统接口 ----------
/** 系统接口分页 + 多条件组合查询（关键字 / 接口类型 / 状态 / 测试结果） */
export function getSysInterfaces(params) {
  return request.get('/sys-interfaces', { params })
}

/** 系统接口统计：总数 / 启用 / 停用 / 测试成功 / 失败 / 未测试 / 按类型分组 */
export function getSysInterfaceStats() {
  return request.get('/sys-interfaces/stats')
}

/** 全部启用的接口 */
export function getEnabledSysInterfaces() {
  return request.get('/sys-interfaces/enabled')
}

/** 系统接口详情（鉴权凭证不回显） */
export function getSysInterface(id) {
  return request.get(`/sys-interfaces/${id}`)
}

/** 新增系统接口 */
export function createSysInterface(data) {
  return request.post('/sys-interfaces', data)
}

/** 修改系统接口（编码不可改；不传凭证时沿用原凭证） */
export function updateSysInterface(id, data) {
  return request.put(`/sys-interfaces/${id}`, data)
}

/** 删除系统接口 */
export function deleteSysInterface(id) {
  return request.delete(`/sys-interfaces/${id}`)
}

/** 启用系统接口 */
export function enableSysInterface(id) {
  return request.post(`/sys-interfaces/${id}/enable`)
}

/** 停用系统接口 */
export function disableSysInterface(id) {
  return request.post(`/sys-interfaces/${id}/disable`)
}

/** 连通性测试：只有启用的接口能测试；真实发起 HTTP 请求，不伪造成功 */
export function testSysInterface(id) {
  return request.post(`/sys-interfaces/${id}/test`)
}

/** 导出系统接口 Excel（鉴权凭证不导出） */
export function exportSysInterfaces(params) {
  return request.get('/sys-interfaces/export', { params, responseType: 'blob' })
}

/** 批量导入系统接口 Excel */
export function importSysInterfaces(data) {
  return request.post('/sys-interfaces/import', data)
}

/** 下载系统接口导入模板 */
export function downloadSysInterfaceTemplate() {
  return request.get('/sys-interfaces/template', { responseType: 'blob' })
}

// ---------- 消息提醒设置 ----------
/** 消息提醒设置分页 + 多条件组合查询（关键字 / 提醒渠道 / 是否启用） */
export function getSysNoticeSettings(params) {
  return request.get('/sys-notice-settings', { params })
}

/** 消息提醒设置统计：总数 / 启用 / 停用 / 按渠道分组 / 按接收范围分组 */
export function getSysNoticeSettingStats() {
  return request.get('/sys-notice-settings/stats')
}

/** 全部启用的提醒设置 */
export function getEnabledSysNoticeSettings() {
  return request.get('/sys-notice-settings/enabled')
}

/** 消息提醒设置详情 */
export function getSysNoticeSetting(id) {
  return request.get(`/sys-notice-settings/${id}`)
}

/** 新增消息提醒设置 */
export function createSysNoticeSetting(data) {
  return request.post('/sys-notice-settings', data)
}

/** 修改消息提醒设置 */
export function updateSysNoticeSetting(id, data) {
  return request.put(`/sys-notice-settings/${id}`, data)
}

/** 删除消息提醒设置 */
export function deleteSysNoticeSetting(id) {
  return request.delete(`/sys-notice-settings/${id}`)
}

/** 启用消息提醒设置 */
export function enableSysNoticeSetting(id) {
  return request.post(`/sys-notice-settings/${id}/enable`)
}

/** 停用消息提醒设置 */
export function disableSysNoticeSetting(id) {
  return request.post(`/sys-notice-settings/${id}/disable`)
}

/** 导出消息提醒设置 Excel */
export function exportSysNoticeSettings(params) {
  return request.get('/sys-notice-settings/export', { params, responseType: 'blob' })
}

/** 批量导入消息提醒设置 Excel */
export function importSysNoticeSettings(data) {
  return request.post('/sys-notice-settings/import', data)
}

/** 下载消息提醒设置导入模板 */
export function downloadSysNoticeSettingTemplate() {
  return request.get('/sys-notice-settings/template', { responseType: 'blob' })
}

// ---------- 知识库 ----------
/** 知识库分页 + 多条件组合查询（关键字全文检索 / 知识分类 / 状态） */
export function getKnowledgeItems(params) {
  return request.get('/sys-knowledge', { params })
}

/** 知识库统计：总数 / 启用 / 停用 / 总查阅量 / 按分类分组 */
export function getKnowledgeItemStats() {
  return request.get('/sys-knowledge/stats')
}

/** 全部启用的知识（检索用） */
export function getEnabledKnowledgeItems() {
  return request.get('/sys-knowledge/enabled')
}

/** 知识详情（打开一次查阅量 +1） */
export function getKnowledgeItem(id) {
  return request.get(`/sys-knowledge/${id}`)
}

/** 新增知识 */
export function createKnowledgeItem(data) {
  return request.post('/sys-knowledge', data)
}

/** 修改知识（查阅量不随编辑改写） */
export function updateKnowledgeItem(id, data) {
  return request.put(`/sys-knowledge/${id}`, data)
}

/** 删除知识 */
export function deleteKnowledgeItem(id) {
  return request.delete(`/sys-knowledge/${id}`)
}

/** 启用知识 */
export function enableKnowledgeItem(id) {
  return request.post(`/sys-knowledge/${id}/enable`)
}

/** 停用知识 */
export function disableKnowledgeItem(id) {
  return request.post(`/sys-knowledge/${id}/disable`)
}

/** 导出知识库 Excel */
export function exportKnowledgeItems(params) {
  return request.get('/sys-knowledge/export', { params, responseType: 'blob' })
}

/** 批量导入知识库 Excel（查阅量从 0 开始计） */
export function importKnowledgeItems(data) {
  return request.post('/sys-knowledge/import', data)
}

/** 下载知识库导入模板 */
export function downloadKnowledgeItemTemplate() {
  return request.get('/sys-knowledge/template', { responseType: 'blob' })
}
