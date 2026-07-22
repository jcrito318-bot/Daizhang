import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, totpApi } from '@/api/auth'
import type { UserVO, LoginRequest, LoginResponse, TotpLoginRequest } from '@/types/common'

// 安全实践 (BF-02 已修复):
// - access token 仅保存在内存中,不持久化到 localStorage,防止 XSS 窃取
// - refresh token 通过 HttpOnly + Secure + SameSite=Strict Cookie 由后端下发,
//   JS 无法通过 document.cookie 读取,从根本上消除 XSS 窃取 refresh token 的风险
// - 前端不再存储/读取 refresh token,刷新时由浏览器自动携带 Cookie (withCredentials: true)
export const useUserStore = defineStore('user', () => {
  // access token: 仅内存,不写入 localStorage
  const token = ref<string>('')
  const userInfo = ref<UserVO | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  // 角色编码数组(派生自 userInfo),用于路由守卫和菜单过滤
  const roles = computed<string[]>(() => userInfo.value?.roles ?? [])

  // 判断当前用户是否拥有任一指定角色(用于路由 meta.roles 和菜单可见性)
  function hasAnyRole(requiredRoles: string[] | undefined): boolean {
    if (!requiredRoles || requiredRoles.length === 0) return true
    if (roles.value.includes('ADMIN')) return true  // ADMIN 隐式拥有所有权限
    return requiredRoles.some(r => roles.value.includes(r))
  }

  /**
   * 登录:返回完整 LoginResponse,由调用方根据 requiresTwoFactor 决定后续流程。
   * - requiresTwoFactor=true:调用方引导用户输入验证码,再调 loginTotp
   * - 正常:store 自动写入 token 与 userInfo
   */
  async function login(loginData: LoginRequest): Promise<LoginResponse> {
    const res = await authApi.login(loginData)
    const data = res.data
    // P4.2: 若需 2FA,不写入 token(此时无正式 token),由调用方处理 2FA 流程
    if (data.requiresTwoFactor) {
      return data
    }
    // access token 仅存内存
    token.value = data.token
    userInfo.value = data.userInfo
    // BF-02 修复:refresh token 由后端通过 HttpOnly Cookie 下发,前端无需也无法读取,
    // 不再写入 localStorage。浏览器会自动在后续 /auth/refresh 请求中携带该 Cookie。
    return data
  }

  /**
   * P4.2: 2FA 双因素登录验证。
   * 用 tempToken + code 换取正式 token,成功后写入 store。
   */
  async function loginTotp(loginData: TotpLoginRequest): Promise<LoginResponse> {
    const res = await totpApi.loginTotp(loginData)
    const data = res.data
    token.value = data.token
    userInfo.value = data.userInfo
    return data
  }

  async function getUserInfo() {
    const res = await authApi.getUserInfo()
    userInfo.value = res.data
    return res.data
  }

  // 刷新 access token:用 refresh token 换取新的 access token。
  // BF-02 修复:refresh token 通过 HttpOnly Cookie 自动携带,前端不再从 localStorage 读取。
  // 成功返回 true,失败返回 false 并清除本地状态。
  async function refreshToken(): Promise<boolean> {
    try {
      const res = await authApi.refresh()
      token.value = res.data.token
      // 后端会通过 Set-Cookie 轮换 refresh token,浏览器自动更新 Cookie,前端无需处理
      return true
    } catch {
      // refresh token 失效(Cookie 过期/被吊销/无效),清除本地登录态
      token.value = ''
      userInfo.value = null
      return false
    }
  }

  // 应用启动时调用:浏览器会自动携带 refresh token Cookie,静默刷新获取新的 access token
  async function initializeAuth() {
    await refreshToken()
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 后端 logout 失败(网络/服务不可达/401 等)不阻塞本地登出:
      // 用户已明确表达登出意图,无论如何都要清空本地状态,避免 token 残留导致安全问题。
      // 拦截器已对错误做了用户可见的提示,这里静默处理。
    } finally {
      token.value = ''
      userInfo.value = null
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    roles,
    hasAnyRole,
    login,
    loginTotp,
    getUserInfo,
    refreshToken,
    initializeAuth,
    logout
  }
})
