import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// mock authApi:避免实际发起 HTTP 请求,直接返回测试数据
vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
    getUserInfo: vi.fn()
  }
}))

import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import type { UserVO, LoginResponse } from '@/types/common'

const mockUser: UserVO = {
  id: 1,
  username: 'admin',
  realName: '管理员',
  phone: '13800000000',
  email: 'admin@test.com',
  avatar: '',
  status: 1,
  roles: ['ADMIN'],
  permissions: ['*'],
  menus: []
}

const mockLoginResponse: LoginResponse = {
  token: 'fake-access-token',
  refreshToken: 'fake-refresh-token',
  userInfo: mockUser
}

/**
 * user store 单元测试
 *
 * 覆盖:
 * - 状态计算属性(isLoggedIn, roles)
 * - hasAnyRole: 角色判断(含 ADMIN 隐式放行)
 * - login: 写入 token/userInfo(BF-02 后 refresh token 由 HttpOnly Cookie 管理,前端不处理)
 * - logout: 清空状态(不操作 localStorage)
 * - refreshToken: 成功/失败两条路径(无参,依赖浏览器自动携带 Cookie)
 * - initializeAuth: 启动时调用 refreshToken
 *
 * BF-02 修复后:refresh token 由后端通过 HttpOnly + Secure + SameSite=Strict Cookie
 * 下发,JS 无法通过 document.cookie 读取。前端不再存储/读取 refresh token,
 * 刷新时由浏览器自动携带 Cookie (axios withCredentials: true)。
 */
describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
  })

  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('初始状态', () => {
    it('默认 token 为空字符串', () => {
      const store = useUserStore()
      expect(store.token).toBe('')
    })

    it('默认 userInfo 为 null', () => {
      const store = useUserStore()
      expect(store.userInfo).toBeNull()
    })

    it('默认未登录(isLoggedIn=false)', () => {
      const store = useUserStore()
      expect(store.isLoggedIn).toBe(false)
    })

    it('默认 roles 为空数组', () => {
      const store = useUserStore()
      expect(store.roles).toEqual([])
    })
  })

  describe('hasAnyRole - 角色判断', () => {
    it('未指定角色时返回 true(无限制)', () => {
      const store = useUserStore()
      expect(store.hasAnyRole(undefined)).toBe(true)
      expect(store.hasAnyRole([])).toBe(true)
    })

    it('用户拥有指定角色之一时返回 true', () => {
      const store = useUserStore()
      store.userInfo = { ...mockUser, roles: ['ACCOUNTANT', 'VIEWER'] }
      expect(store.hasAnyRole(['ACCOUNTANT'])).toBe(true)
      expect(store.hasAnyRole(['VIEWER', 'AUDITOR'])).toBe(true)
    })

    it('用户不拥有任何指定角色时返回 false', () => {
      const store = useUserStore()
      store.userInfo = { ...mockUser, roles: ['VIEWER'] }
      expect(store.hasAnyRole(['ACCOUNTANT', 'ADMIN'])).toBe(false)
    })

    it('ADMIN 角色应隐式拥有所有权限', () => {
      const store = useUserStore()
      store.userInfo = { ...mockUser, roles: ['ADMIN'] }
      expect(store.hasAnyRole(['ACCOUNTANT'])).toBe(true)
      expect(store.hasAnyRole(['ACCOUNTANT', 'AUDITOR', 'VIEWER'])).toBe(true)
    })

    it('userInfo 为 null 时返回 false(有角色要求时)', () => {
      const store = useUserStore()
      store.userInfo = null
      expect(store.hasAnyRole(['ACCOUNTANT'])).toBe(false)
    })
  })

  describe('login - 登录', () => {
    it('成功登录应写入 token 和 userInfo', async () => {
      ;(authApi.login as any).mockResolvedValue({
        code: 200,
        message: 'ok',
        data: mockLoginResponse,
        timestamp: Date.now()
      })
      const store = useUserStore()
      await store.login({ username: 'admin', password: '123456' })
      expect(store.token).toBe('fake-access-token')
      expect(store.userInfo).toEqual(mockUser)
      expect(store.isLoggedIn).toBe(true)
      expect(store.roles).toEqual(['ADMIN'])
    })

    it('BF-02 修复后登录不应将 refreshToken 写入 localStorage(改由 HttpOnly Cookie 管理)', async () => {
      ;(authApi.login as any).mockResolvedValue({
        code: 200,
        message: 'ok',
        data: mockLoginResponse,
        timestamp: Date.now()
      })
      const store = useUserStore()
      await store.login({ username: 'admin', password: '123456' })
      // refresh token 不应出现在 localStorage 中(由后端 Set-Cookie HttpOnly 下发)
      expect(localStorage.getItem('refreshToken')).toBeNull()
    })

    it('登录失败(authApi 抛错)应向上抛出', async () => {
      ;(authApi.login as any).mockRejectedValue(new Error('用户名或密码错误'))
      const store = useUserStore()
      await expect(store.login({ username: 'admin', password: 'wrong' })).rejects.toThrow('用户名或密码错误')
      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
    })
  })

  describe('logout - 登出', () => {
    it('登出应清空 token 和 userInfo', async () => {
      ;(authApi.logout as any).mockResolvedValue({ code: 200, message: 'ok', data: null, timestamp: 0 })
      const store = useUserStore()
      store.token = 'token'
      store.userInfo = mockUser
      await store.logout()
      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
      expect(store.isLoggedIn).toBe(false)
    })

    it('后端 logout 接口失败时也应清空本地状态(finally 块)', async () => {
      ;(authApi.logout as any).mockRejectedValue(new Error('网络错误'))
      const store = useUserStore()
      store.token = 'token'
      store.userInfo = mockUser
      // logout 内部 catch -> finally,不抛错
      await store.logout()
      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
    })
  })

  describe('refreshToken - 刷新 access token', () => {
    it('刷新成功应更新 token 并返回 true(BF-02: 无参,依赖浏览器自动携带 Cookie)', async () => {
      ;(authApi.refresh as any).mockResolvedValue({
        code: 200,
        message: 'ok',
        data: {
          token: 'new-access-token',
          refreshToken: 'new-refresh-token',
          userInfo: mockUser
        },
        timestamp: Date.now()
      })
      const store = useUserStore()
      const result = await store.refreshToken()
      expect(result).toBe(true)
      expect(store.token).toBe('new-access-token')
      // refresh token 由后端 Set-Cookie 轮换,前端不应在 localStorage 中处理
      expect(localStorage.getItem('refreshToken')).toBeNull()
      // refresh API 应以无参形式调用(浏览器自动携带 Cookie)
      expect(authApi.refresh).toHaveBeenCalledWith()
    })

    it('刷新失败应清除登录态并返回 false', async () => {
      ;(authApi.refresh as any).mockRejectedValue(new Error('refresh token 失效'))
      const store = useUserStore()
      store.token = 'old-token'
      store.userInfo = mockUser
      const result = await store.refreshToken()
      expect(result).toBe(false)
      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
    })
  })

  describe('initializeAuth - 应用启动时静默刷新', () => {
    it('应调用 refreshToken(BF-02: 无条件调用,由浏览器决定是否携带 Cookie)', async () => {
      ;(authApi.refresh as any).mockResolvedValue({
        code: 200,
        message: 'ok',
        data: { token: 'token', refreshToken: 'rft', userInfo: mockUser },
        timestamp: Date.now()
      })
      const store = useUserStore()
      await store.initializeAuth()
      // 无参调用 refresh,浏览器自动携带 HttpOnly Cookie
      expect(authApi.refresh).toHaveBeenCalledWith()
      expect(store.token).toBe('token')
    })
  })
})
