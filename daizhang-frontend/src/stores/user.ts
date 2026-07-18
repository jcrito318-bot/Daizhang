import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserVO, LoginRequest } from '@/types/common'

// 安全实践 (VUE-AUTH-001): access token 仅保存在内存中,不持久化到 localStorage,
// 防止 XSS 攻击窃取长生命周期的访问令牌。refresh token 保留在 localStorage,
// 仅用于 /auth/refresh 接口,作用域有限并由后端校验类型(type="refresh")。
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

  async function login(loginData: LoginRequest) {
    const res = await authApi.login(loginData)
    // access token 仅存内存
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    // refresh token 持久化到 localStorage,用于 access token 过期后刷新
    if (res.data.refreshToken) {
      localStorage.setItem('refreshToken', res.data.refreshToken)
    }
  }

  async function getUserInfo() {
    const res = await authApi.getUserInfo()
    userInfo.value = res.data
    return res.data
  }

  // 刷新 access token:用 refresh token 换取新的 access token。
  // 成功返回 true,失败返回 false 并清除 refresh token。
  async function refreshToken(): Promise<boolean> {
    const storedRefresh = localStorage.getItem('refreshToken')
    if (!storedRefresh) {
      return false
    }
    try {
      const res = await authApi.refresh(storedRefresh)
      token.value = res.data.token
      // 后端会返回(可能轮换后的) refresh token,持续更新以保持会话
      if (res.data.refreshToken) {
        localStorage.setItem('refreshToken', res.data.refreshToken)
      }
      return true
    } catch {
      // refresh token 失效,清除并退出登录态
      localStorage.removeItem('refreshToken')
      token.value = ''
      userInfo.value = null
      return false
    }
  }

  // 应用启动时调用:如果存在 refresh token,静默刷新获取新的 access token
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
      localStorage.removeItem('refreshToken')
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    roles,
    hasAnyRole,
    login,
    getUserInfo,
    refreshToken,
    initializeAuth,
    logout
  }
})
