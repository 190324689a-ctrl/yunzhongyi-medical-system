import request from '../utils/request'

/** 登录：账号密码校验，成功返回 { token, expiresIn, user, roles, permissions } */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 登出：当前 token 加入黑名单 */
export function logout() {
  return request.post('/auth/logout')
}

/** 当前登录用户信息（角色与权限实时查库） */
export function getMe() {
  return request.get('/auth/me')
}

/** 当前登录用户可见的菜单树（按角色权限过滤） */
export function getMyMenus() {
  return request.get('/auth/menus')
}

/** 修改密码：成功后当前 token 失效，需重新登录 */
export function changePassword(data) {
  return request.post('/auth/change-password', data)
}
