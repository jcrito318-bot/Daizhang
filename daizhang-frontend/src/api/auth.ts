import request from '@/utils/request'
import type {
  Result,
  LoginRequest,
  LoginResponse,
  UserVO,
  TotpSetupVO,
  TotpEnableResponse,
  TotpStatusVO,
  TotpLoginRequest,
  ChangePasswordRequest
} from '@/types/common'

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

/**
 * P4.2: TOTP 双因素认证 API
 */
export const totpApi = {
  /** 生成 TOTP 密钥(返回二维码内容) */
  setup(): Promise<Result<TotpSetupVO>> {
    return request.post('/auth/totp/setup')
  },
  /** 启用 2FA(校验验证码,生成备用码) */
  enable(code: string): Promise<Result<TotpEnableResponse>> {
    return request.post('/auth/totp/enable', { code })
  },
  /** 禁用 2FA(校验验证码) */
  disable(code: string): Promise<Result<void>> {
    return request.post('/auth/totp/disable', { code })
  },
  /** 查询当前用户 2FA 状态 */
  status(): Promise<Result<TotpStatusVO>> {
    return request.get('/auth/totp/status')
  },
  /** P4.2: 2FA 双因素登录验证 */
  loginTotp(data: TotpLoginRequest): Promise<Result<LoginResponse>> {
    return request.post('/auth/login/totp', data)
  }
}

/**
 * P4.3: 密码策略 API
 */
export const passwordApi = {
  /** 用户自己修改密码(校验原密码 + 密码策略 + 密码历史) */
  changePassword(data: ChangePasswordRequest): Promise<Result<void>> {
    return request.post('/auth/change-password', data)
  }
}
