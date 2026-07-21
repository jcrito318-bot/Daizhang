import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/common'
import { useUserStore } from '@/stores/user'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  // BF-02 修复:启用 withCredentials,使浏览器跨域请求自动携带 HttpOnly refresh token Cookie。
  // 后端 /auth/refresh 通过 Cookie 读取 refresh token,不再依赖 Authorization header。
  // 要求后端 CORS 配置 allowCredentials(true) 且 allowedOriginPatterns 不能为 "*"。
  withCredentials: true
})

// 401 静默刷新:并发 401 共享同一次 refresh 尝试
let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

// 跳转登录页:优先使用 vue-router(SPA 内跳转,保留 Pinia 状态),
// 若 router 未初始化(早期模块加载阶段)则回退到 window.location。
let routerInstance: { push: (path: string) => void } | null = null
export function setRouter(r: { push: (path: string) => void }) {
  routerInstance = r
}
function redirectToLogin() {
  if (routerInstance) {
    routerInstance.push('/login')
  } else {
    window.location.href = '/login'
  }
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 通过 Pinia store 读取内存中的 access token,不再读 localStorage。
    // 若调用方已显式设置 Authorization(如 /auth/refresh 用 refresh token),则不覆盖。
    const userStore = useUserStore()
    if (userStore.token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        // 业务层 401:access token 已失效,刷新页面后由 initializeAuth 尝试静默恢复
        redirectToLogin()
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 返回 response.data(Result 包装器)而非完整 AxiosResponse,
    // 使 API 调用方通过 res.data 获取实际数据载荷,与 Promise<Result<T>> 类型一致
    return res as unknown as AxiosResponse
  },
  async (error) => {
    if (error.response) {
      const status = error.response.status
      const originalConfig = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
      // HTTP 401:尝试一次静默刷新 access token 后重放原请求
      const isRefreshRequest = originalConfig?.url?.includes('/auth/refresh')
      if (status === 401 && originalConfig && !originalConfig._retry && !isRefreshRequest) {
        // 已有刷新在进行中:挂起等待新 token
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            pendingRequests.push((newToken: string) => {
              originalConfig.headers.Authorization = `Bearer ${newToken}`
              service.request(originalConfig).then(resolve).catch(reject)
            })
          })
        }
        originalConfig._retry = true
        isRefreshing = true
        try {
          const userStore = useUserStore()
          const refreshed = await userStore.refreshToken()
          if (refreshed) {
            // 重放所有挂起的 401 请求
            const newToken = userStore.token
            pendingRequests.forEach((cb) => cb(newToken))
            pendingRequests = []
            // 重放当前请求
            originalConfig.headers.Authorization = `Bearer ${newToken}`
            return service.request(originalConfig)
          }
          // 刷新失败:清空挂起队列并跳转登录
          pendingRequests = []
          ElMessage.error('登录已过期，请重新登录')
          redirectToLogin()
          return Promise.reject(error)
        } finally {
          isRefreshing = false
        }
      } else if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
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
