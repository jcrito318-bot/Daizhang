import request from '@/utils/request'
import type { Result, LoginRequest, LoginResponse, UserVO } from '@/types/common'

export const authApi = {
  login(data: LoginRequest): Promise<Result<LoginResponse>> {
    return request.post('/auth/login', data)
  },
  logout(): Promise<Result<void>> {
    return request.post('/auth/logout')
  },
  getUserInfo(): Promise<Result<UserVO>> {
    return request.get('/auth/info')
  }
}
