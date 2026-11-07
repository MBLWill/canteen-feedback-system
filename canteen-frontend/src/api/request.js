import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'canteen_token'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

request.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const body = response.data
    if (body && body.code === 200) {
      return body
    }
    const message = (body && body.message) || '请求失败'
    ElMessage.error(message)
    if (body && body.code === 401) {
      clearToken()
      window.location.href = '/login'
    }
    return Promise.reject(new Error(message))
  },
  error => {
    const status = error.response && error.response.status
    const backendMessage = error.response && error.response.data && error.response.data.message
    if (status === 401) {
      ElMessage.error(backendMessage || '未登录或登录已过期')
      clearToken()
      window.location.href = '/login'
    } else if (status === 403) {
      ElMessage.error(backendMessage || '没有权限执行该操作')
    } else {
      ElMessage.error(backendMessage || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
