/** 登录态存取（localStorage）：token、当前用户信息、角色、权限与菜单 */

const TOKEN_KEY = 'yz_token'
const USER_KEY = 'yz_user'
const ROLES_KEY = 'yz_roles'
const PERMISSIONS_KEY = 'yz_permissions'
const MENUS_KEY = 'yz_menus'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user || null))
}

export function getRoles() {
  try {
    return JSON.parse(localStorage.getItem(ROLES_KEY) || '[]')
  } catch {
    return []
  }
}

export function setRoles(roles) {
  localStorage.setItem(ROLES_KEY, JSON.stringify(roles || []))
}

/** 当前用户的权限标识，如 physician:list；登录后由后端返回 */
export function getPermissions() {
  try {
    return JSON.parse(localStorage.getItem(PERMISSIONS_KEY) || '[]')
  } catch {
    return []
  }
}

export function setPermissions(permissions) {
  localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(permissions || []))
}

/** 当前用户是否有某权限；未加载到权限（历史登录态）时放行，由菜单过滤兜底 */
export function hasPermission(code) {
  if (!code) return true
  const permissions = getPermissions()
  if (!permissions.length) return true
  return permissions.includes(code)
}

/** 当前用户可见的菜单树（/api/auth/menus 返回） */
export function getMenus() {
  try {
    return JSON.parse(localStorage.getItem(MENUS_KEY) || 'null')
  } catch {
    return null
  }
}

export function setMenus(menus) {
  localStorage.setItem(MENUS_KEY, JSON.stringify(menus || null))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(ROLES_KEY)
  localStorage.removeItem(PERMISSIONS_KEY)
  localStorage.removeItem(MENUS_KEY)
}
