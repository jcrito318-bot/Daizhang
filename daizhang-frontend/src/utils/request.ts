import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/common'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// refreshToken 刷新竞态控制:
// 多个并发请求同时收到 401 时,只发起一次刷新,其余请求排队等待新 token 后用新 token 重试
let isRefreshing = false
let requestsQueue: Array<{
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
  originalConfig: InternalAxiosRequestConfig & { _retry?: boolean }
}> = []

function clearAuthAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  window.location.href = '/login'
}

// 使用独立的 axios 请求调用刷新接口,避免复用 service 实例触发拦截器造成循环
function doRefreshToken(): Promise<string> {
  const refreshToken = localStorage.getItem('refreshToken')
  return axios
    .post<Result<{ token: string; refreshToken?: string }>>('/api/auth/refresh', { refreshToken })
    .then((res) => {
      const data = res.data?.data
      if (!data?.token) {
        return Promise.reject(new Error('刷新 token 失败'))
      }
      localStorage.setItem('token', data.token)
      if (data.refreshToken) {
        localStorage.setItem('refreshToken', data.refreshToken)
      }
      return data.token
    })
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    // Blob/文件下载响应：response.data 是 Blob,没有 code 字段,
    // 必须跳过业务 code 校验并直接返回 Blob,否则所有报表导出都会被误判为业务错误而失败
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response.data as unknown as AxiosResponse
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 返回 response.data(Result 包装器)而非完整 AxiosResponse,
    // 使 API 调用方通过 res.data 获取实际数据载荷,与 Promise<Result<T>> 类型一致
    return res as unknown as AxiosResponse
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const originalConfig = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
        const refreshToken = localStorage.getItem('refreshToken')
        // 没有 refreshToken 或已重试过,直接跳转登录页,避免无限循环
        if (!refreshToken || originalConfig?._retry) {
          clearAuthAndRedirect()
          ElMessage.error('登录已过期，请重新登录')
          return Promise.reject(error)
        }
        originalConfig._retry = true

        // 已有刷新请求进行中,排队等待新 token 后重试
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            requestsQueue.push({ resolve, reject, originalConfig })
          })
        }

        isRefreshing = true
        return doRefreshToken()
          .then((newToken: string) => {
            // 通知所有排队请求用新 token 重试
            requestsQueue.forEach((item) => {
              item.originalConfig.headers.Authorization = `Bearer ${newToken}`
              item.resolve(service(item.originalConfig))
            })
            requestsQueue = []
            originalConfig.headers.Authorization = `Bearer ${newToken}`
            return service(originalConfig)
          })
          .catch((err) => {
            // 刷新失败,拒绝所有排队请求并跳转登录页
            requestsQueue.forEach((item) => item.reject(err))
            requestsQueue = []
            clearAuthAndRedirect()
            ElMessage.error('登录已过期，请重新登录')
            return Promise.reject(err)
          })
          .finally(() => {
            isRefreshing = false
          })
      } else if (status === 403) {
        ElMessage.error('没有权限访问该资源')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接异常，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default service
