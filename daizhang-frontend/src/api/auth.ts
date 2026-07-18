import request from '@/utils/request'
import type { Result, LoginRequest, LoginResponse, UserVO } from '@/types/common'

export const authApi = {
  login(data: LoginRequest): Promise<Result<LoginResponse>> {
    return request.post('/auth/login', data)
  },
  logout(): Promise<Result<void>> {
    return request.post('/auth/logout')
  },
  // 刷新访问令牌:使用 refresh token 换取新的 access token
  refresh(refreshToken: string): Promise<Result<LoginResponse>> {
    return request.post('/auth/refresh', null, {
      headers: { Authorization: `Bearer ${refreshToken}` }
    })
  },
  getUserInfo(): Promise<Result<UserVO>> {
    return request.get('/auth/info')
  }
}
