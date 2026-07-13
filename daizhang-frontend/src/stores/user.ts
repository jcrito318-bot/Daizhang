import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { useAppStore } from '@/stores/app'
import type { UserVO, LoginRequest } from '@/types/common'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserVO | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  async function login(loginData: LoginRequest) {
    const res = await authApi.login(loginData)
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    localStorage.setItem('token', res.data.token)
    // 存储 refresh token,用于 accessToken 过期后刷新
    if (res.data.refreshToken) {
      localStorage.setItem('refreshToken', res.data.refreshToken)
    }
  }

  async function getUserInfo() {
    const res = await authApi.getUserInfo()
    userInfo.value = res.data
    return res.data
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      // 重置账套相关缓存:避免 A 用户退出后 B 用户在同一浏览器登录仍看到 A 的账套列表。
      // appStore 在此处延迟获取,避免 store 初始化阶段的循环依赖。
      useAppStore().resetAccountSetState()
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    getUserInfo,
    logout
  }
})
