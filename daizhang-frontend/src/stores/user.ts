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
    login,
    getUserInfo,
    refreshToken,
    initializeAuth,
    logout
  }
})
