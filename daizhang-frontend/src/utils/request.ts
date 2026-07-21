import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/common'
import { useUserStore } from '@/stores/user'

/**
 * 敏感操作二次确认请求头名称 (P3.4)。
 * 后端 SensitiveOperationAspect 检查该头是否为 "true",否则拒绝执行。
 */
export const SENSITIVE_CONFIRM_HEADER = 'X-Confirm'

/**
 * 扩展 axios 请求配置,支持 `sensitive` 标记 (P3.4)。
 *
 * 调用方在发起敏感操作请求时,通过 `withSensitive()` 包装 config,
 * 请求拦截器会自动注入 `X-Confirm: true` 头,后端切面据此放行。
 *
 * 这样设计的目的是:
 * 1. 不破坏既有 API 契约(默认不带 sensitive 标记,行为不变)
 * 2. 显式标记敏感操作,便于代码审查与审计
 * 3. 前端 UI 层在调用前应通过 ElMessageBox.confirm 弹窗确认,
 *    用户确认后再用 withSensitive() 发起请求
 */
declare module 'axios' {
  interface AxiosRequestConfig {
    /** 标记该请求为敏感操作,拦截器会自动添加 X-Confirm: true 头 */
    sensitive?: boolean
  }
}

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
    // P3.4:敏感操作二次确认
    // 调用方通过 withSensitive() 标记 config.sensitive=true,
    // 拦截器自动注入 X-Confirm: true 头,后端 SensitiveOperationAspect 据此放行。
    if (config.sensitive) {
      config.headers[SENSITIVE_CONFIRM_HEADER] = 'true'
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

/**
 * 标记一个 axios 请求配置为敏感操作 (P3.4)。
 *
 * 用法:
 * ```ts
 * // 删除用户(敏感操作)
 * request.delete(`/system/user/${id}`, withSensitive())
 *
 * // 带 params 的敏感操作
 * request.post('/period/close', null, withSensitive({ params: { accountSetId, year, month } }))
 *
 * // 带 body 的敏感操作
 * request.post(`/system/backup/${id}/restore`, withSensitive({ data: { backupId, confirm: true } }))
 * ```
 *
 * 拦截器会检测 config.sensitive 并自动注入 `X-Confirm: true` 头,
 * 后端 SensitiveOperationAspect 据此放行;未带该头的敏感操作请求会被后端拒绝(返回 400)。
 *
 * 调用方应在调用前通过 ElMessageBox.confirm 弹窗让用户二次确认,
 * 用户确认后再用 withSensitive() 发起请求。
 */
export function withSensitive<T extends AxiosRequestConfig>(config?: T): T & { sensitive: true } {
  return { ...(config as object), sensitive: true } as T & { sensitive: true }
}

export default service
