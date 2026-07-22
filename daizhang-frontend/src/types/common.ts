export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  userInfo: UserVO
  /** 是否需要双因素认证(P4.2) */
  requiresTwoFactor?: boolean
  /** 2FA 临时令牌(requiresTwoFactor=true 时返回,用于 /auth/login/totp) */
  tempToken?: string
  /** 密码是否已过期(需强制改密,P4.3) */
  passwordExpired?: boolean
  /** 剩余登录尝试次数(P4.3) */
  remainingAttempts?: number
}

/** TOTP 设置响应(P4.2) */
export interface TotpSetupVO {
  secret: string
  otpauthUrl: string
  qrCodeBase64: string | null
}

/** TOTP 启用响应(P4.2) */
export interface TotpEnableResponse {
  enabled: boolean
  backupCodes: string[]
}

/** TOTP 状态响应(P4.2) */
export interface TotpStatusVO {
  enabled: boolean
  secretGenerated: boolean
}

/** 2FA 登录请求(P4.2) */
export interface TotpLoginRequest {
  tempToken: string
  code: string
}

/** 修改密码请求(P4.3) */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface UserVO {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  avatar: string
  status: number
  roles: string[]
  permissions: string[]
  menus: MenuVO[]
}

export interface MenuVO {
  id: number
  parentId: number
  name: string
  path: string
  component: string
  icon: string
  sortOrder: number
  menuType: number
  permission: string
  visible: number
  status: number
  children: MenuVO[]
}
