import request from '../utils/request'

/** 启用科室列表（下拉框） */
export function getEnabledDepts() {
  return request.get('/depts/enabled')
}

/** 按字典编码取数据项，如 title / education / staff_type */
export function getDictItems(dictCode) {
  return request.get(`/dicts/${dictCode}`)
}

// ---------- 用户管理 ----------
/** 用户分页 + 多条件组合查询（关键字 / 科室 / 状态） */
export function getUsers(params) {
  return request.get('/users', { params })
}

/** 用户统计：总数 / 启用 / 停用 / 按科室分组 */
export function getUserStats() {
  return request.get('/users/stats')
}

/** 用户详情（密码不回显） */
export function getUser(id) {
  return request.get(`/users/${id}`)
}

/** 新增用户（不填密码时默认 123456） */
export function createUser(data) {
  return request.post('/users', data)
}

/** 修改用户（改密码走单独接口，这里不覆盖密码） */
export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

/** 删除用户（同时删除其角色分配） */
export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}

/** 启用用户 */
export function enableUser(id) {
  return request.post(`/users/${id}/enable`)
}

/** 停用用户 */
export function disableUser(id) {
  return request.post(`/users/${id}/disable`)
}

/** 重置密码（默认 123456） */
export function resetUserPassword(id) {
  return request.post(`/users/${id}/reset-password`)
}

/** 查某用户的全部角色 */
export function getUserRoles(id) {
  return request.get(`/users/${id}/roles`)
}

/** 给用户分配角色（全量替换） */
export function assignUserRoles(id, roleIds) {
  return request.post(`/users/${id}/roles`, { roleIds })
}

/** 导出用户 Excel（密码不导出） */
export function exportUsers(params) {
  return request.get('/users/export', { params, responseType: 'blob' })
}

/** 批量导入用户 Excel（密码统一为默认密码 123456） */
export function importUsers(data) {
  return request.post('/users/import', data)
}

/** 下载用户导入模板 */
export function downloadUserTemplate() {
  return request.get('/users/template', { responseType: 'blob' })
}

// ---------- 角色管理 ----------
/** 角色分页 + 多条件组合查询（关键字 / 状态） */
export function getRoles(params) {
  return request.get('/roles', { params })
}

/** 角色统计：总数 / 启用 / 停用 / 各角色已分配用户数 */
export function getRoleStats() {
  return request.get('/roles/stats')
}

/** 全部启用的角色（给用户分配角色时下拉用） */
export function getEnabledRoles() {
  return request.get('/roles/enabled')
}

/** 新增角色 */
export function createRole(data) {
  return request.post('/roles', data)
}

/** 修改角色 */
export function updateRole(id, data) {
  return request.put(`/roles/${id}`, data)
}

/** 删除角色（同时删除其权限分配与用户分配） */
export function deleteRole(id) {
  return request.delete(`/roles/${id}`)
}

/** 启用角色 */
export function enableRole(id) {
  return request.post(`/roles/${id}/enable`)
}

/** 停用角色 */
export function disableRole(id) {
  return request.post(`/roles/${id}/disable`)
}

/** 查某角色的全部权限 */
export function getRolePermissions(id) {
  return request.get(`/roles/${id}/permissions`)
}

/** 给角色分配权限（全量替换） */
export function assignRolePermissions(id, permissionIds) {
  return request.post(`/roles/${id}/permissions`, { permissionIds })
}

/** 查某角色绑定的全部科室 */
export function getRoleDepts(id) {
  return request.get(`/roles/${id}/depts`)
}

/** 给角色绑定科室（全量替换） */
export function assignRoleDepts(id, deptIds) {
  return request.post(`/roles/${id}/depts`, { deptIds })
}

// ---------- 权限管理 ----------
/** 全部权限（按排序返回，前端自行组树） */
export function getPermissions() {
  return request.get('/permissions')
}

/** 权限统计：总数 / 启用 / 停用 / 按类型分组（目录/菜单/按钮） */
export function getPermissionStats() {
  return request.get('/permissions/stats')
}

/** 新增权限 */
export function createPermission(data) {
  return request.post('/permissions', data)
}

/** 修改权限 */
export function updatePermission(id, data) {
  return request.put(`/permissions/${id}`, data)
}

/** 删除权限（有子权限的不可直接删除） */
export function deletePermission(id) {
  return request.delete(`/permissions/${id}`)
}

/** 启用权限 */
export function enablePermission(id) {
  return request.post(`/permissions/${id}/enable`)
}

/** 停用权限 */
export function disablePermission(id) {
  return request.post(`/permissions/${id}/disable`)
}

// ---------- 科室管理 ----------
/** 科室分页 + 多条件组合查询（关键字 / 类型 / 状态） */
export function getDepts(params) {
  return request.get('/depts', { params })
}

/** 科室树（层级展示与角色绑定科室时勾选用） */
export function getDeptTree() {
  return request.get('/depts/tree')
}

/** 科室统计：总数 / 启用 / 停用 / 按类型分组 */
export function getDeptStats() {
  return request.get('/depts/stats')
}

/** 科室详情 */
export function getDept(id) {
  return request.get(`/depts/${id}`)
}

/** 新增科室 */
export function createDept(data) {
  return request.post('/depts', data)
}

/** 修改科室（已有下级科室时编码不可改） */
export function updateDept(id, data) {
  return request.put(`/depts/${id}`, data)
}

/** 删除科室（有下级科室的不可直接删除） */
export function deleteDept(id) {
  return request.delete(`/depts/${id}`)
}

/** 启用科室 */
export function enableDept(id) {
  return request.post(`/depts/${id}/enable`)
}

/** 停用科室（有下级科室的不可停用） */
export function disableDept(id) {
  return request.post(`/depts/${id}/disable`)
}

/** 导出科室 Excel */
export function exportDepts(params) {
  return request.get('/depts/export', { params, responseType: 'blob' })
}

/** 批量导入科室 Excel */
export function importDepts(data) {
  return request.post('/depts/import', data)
}

/** 下载科室导入模板 */
export function downloadDeptTemplate() {
  return request.get('/depts/template', { responseType: 'blob' })
}

// ---------- 字典管理 ----------
/** 字典数据项分页 + 多条件组合查询（字典编码 / 关键字） */
export function getDictData(params) {
  return request.get('/dicts', { params })
}

/** 字典统计：字典类型数 / 数据项总数 / 启用数 / 各字典数据项数 */
export function getDictStats() {
  return request.get('/dicts/stats')
}

/** 全部字典类型（字典管理页下拉用） */
export function getDictTypes() {
  return request.get('/dicts/types')
}

/** 新增字典数据项 */
export function createDictData(data) {
  return request.post('/dicts', data)
}

/** 修改字典数据项 */
export function updateDictData(id, data) {
  return request.put(`/dicts/${id}`, data)
}

/** 删除字典数据项 */
export function deleteDictData(id) {
  return request.delete(`/dicts/${id}`)
}

/** 导出字典数据 Excel */
export function exportDictData(params) {
  return request.get('/dicts/export', { params, responseType: 'blob' })
}

/** 批量导入字典数据 Excel */
export function importDictData(data) {
  return request.post('/dicts/import', data)
}

/** 下载字典数据导入模板 */
export function downloadDictDataTemplate() {
  return request.get('/dicts/template', { responseType: 'blob' })
}

// ---------- 操作日志 ----------
/** 操作日志分页 + 多条件组合查询（模块 / 操作人 / 结果） */
export function getOperLogs(params) {
  return request.get('/oper-logs', { params })
}

/** 操作日志统计：总数 / 成功 / 失败 / 按模块分组 */
export function getOperLogStats() {
  return request.get('/oper-logs/stats')
}

/** 导出操作日志 Excel */
export function exportOperLogs(params) {
  return request.get('/oper-logs/export', { params, responseType: 'blob' })
}
