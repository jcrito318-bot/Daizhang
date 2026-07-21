import request from '@/utils/request'
import type { Result, LoginRequest, LoginResponse, UserVO } from '@/types/common'

export const authApi = {
  login(data: LoginRequest): Promise<Result<LoginResponse>> {
    return request.post('/auth/login', data)
  },
  logout(): Promise<Result<void>> {
    return request.post('/auth/logout')
  },
  // BF-02 修复:refresh token 已通过 HttpOnly Cookie 自动携带(withCredentials: true),
  // 不再需要前端从 localStorage 读取并放入 Authorization header。
  // 后端从 Cookie 读取 refresh token,兼容旧前端过渡期也支持 Authorization header。
  refresh(): Promise<Result<LoginResponse>> {
    return request.post('/auth/refresh')
  },
  getUserInfo(): Promise<Result<UserVO>> {
    return request.get('/auth/info')
  }
}
