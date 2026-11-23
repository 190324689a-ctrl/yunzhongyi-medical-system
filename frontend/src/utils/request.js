import axios from 'axios'
import { clearAuth, getToken } from './auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求时携带登录 token
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (err) => Promise.reject(err)
)

// token 失效（401）时清登录态并回登录页（整页跳转，避免与 router 循环引用）
function handleUnauthorized() {
  clearAuth()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

// 统一处理后端 Result 结构：{ code, msg, data }
request.interceptors.response.use(
  (res) => {
    const body = res.data
    // 文件下载（blob）直接放行
    if (res.config.responseType === 'blob') {
      return res
    }
    if (body && typeof body.code === 'number') {
      if (body.code === 401) {
        handleUnauthorized()
        return Promise.reject(new Error(body.msg || '登录已过期，请重新登录'))
      }
      if (body.code !== 0) {
        return Promise.reject(new Error(body.msg || '请求失败'))
      }
      return body.data
    }
    return body
  },
  (err) => {
    if (err?.response?.status === 401) {
      handleUnauthorized()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // 403 是“已登录但无权限”：只提示，不清登录态、不回登录页
    if (err?.response?.status === 403) {
      return Promise.reject(new Error('无权限访问该接口'))
    }
    return Promise.reject(err)
  }
)

export default request
