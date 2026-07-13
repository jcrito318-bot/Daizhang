import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/common'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

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
        localStorage.removeItem('token')
        window.location.href = '/login'
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
