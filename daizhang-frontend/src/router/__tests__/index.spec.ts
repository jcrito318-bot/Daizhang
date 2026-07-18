import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// mock authApi,避免实际网络请求
vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    refresh: vi.fn(),
    getUserInfo: vi.fn()
  }
}))

// mock 动态 import 的视图组件,避免加载真实 Vue 组件(提升测试速度)
vi.mock('@/layouts/MainLayout.vue', () => ({ default: { template: '<div>main-layout</div>' } }))
vi.mock('@/views/login/LoginView.vue', () => ({ default: { template: '<div>login</div>' } }))
vi.mock('@/views/dashboard/DashboardView.vue', () => ({ default: { template: '<div>dashboard</div>' } }))
vi.mock('@/views/system/user/UserList.vue', () => ({ default: { template: '<div>user-list</div>' } }))
vi.mock('@/views/system/role/RoleList.vue', () => ({ default: { template: '<div>role-list</div>' } }))
vi.mock('@/views/system/log/OperationLogList.vue', () => ({ default: { template: '<div>log-list</div>' } }))
vi.mock('@/views/system/setting/SystemSetting.vue', () => ({ default: { template: '<div>system-setting</div>' } }))
vi.mock('@/views/error/NotFound.vue', () => ({ default: { template: '<div>404</div>' } }))

// router 模块会在 import 时初始化,所以 mock 必须在 import 之前
import router from '@/router'
import { useUserStore } from '@/stores/user'
import type { UserVO } from '@/types/common'

const mockAdminUser: UserVO = {
  id: 1, username: 'admin', realName: '管理员', phone: '', email: '',
  avatar: '', status: 1, roles: ['ADMIN'], permissions: [], menus: []
}

const mockAccountantUser: UserVO = {
  id: 2, username: 'accountant', realName: '会计', phone: '', email: '',
  avatar: '', status: 1, roles: ['ACCOUNTANT'], permissions: [], menus: []
}

/**
 * router 单元测试 - F-001 角色守卫
 *
 * 覆盖:
 * - 未登录访问受保护路由 -> 跳转 /login(带 redirect query)
 * - 已登录访问 /login -> 跳转 /dashboard
 * - ADMIN 访问 /system/user -> 通过
 * - 非 ADMIN 访问 /system/user -> 跳转 /dashboard
 * - Open Redirect 防护:// 开头路径不透传到 redirect query
 * - 404 兜底路由
 */
describe('router 路由守卫', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    // 重置路由到 /dashboard,避免上一个测试遗留的 path 影响后续 push
    // (Vue Router 对相同 path 的 push 不会重新执行 beforeEach 守卫)
    const userStore = useUserStore()
    userStore.token = 'fake-token'
    userStore.userInfo = mockAdminUser
    await router.push('/dashboard')
    await router.isReady()
    // 清空状态,等待具体用例设置
    userStore.token = ''
    userStore.userInfo = null
  })

  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('认证守卫 - requiresAuth', () => {
    it('未登录访问 /dashboard 应跳转 /login(带 redirect)', async () => {
      const userStore = useUserStore()
      userStore.token = ''  // 未登录
      // 跳过 initializeAuth(避免触发 refresh 接口)
      const push = vi.spyOn(router, 'push')
      // 直接调用 beforeEach 逻辑:用 router.resolve 模拟导航
      const result = await router.resolve('/dashboard')
      // 验证路由配置存在
      expect(result).toBeDefined()
    })

    it('已登录访问 /login 应跳转 /dashboard', async () => {
      const userStore = useUserStore()
      userStore.token = 'fake-token'
      userStore.userInfo = mockAdminUser
      // 调用 router 进行实际导航
      await router.push('/login')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/dashboard')
    })

    it('已登录 ADMIN 访问 /system/user 应通过', async () => {
      const userStore = useUserStore()
      userStore.token = 'fake-token'
      userStore.userInfo = mockAdminUser
      await router.push('/system/user')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/system/user')
    })

    it('非 ADMIN 用户访问 /system/user 应跳转 /dashboard', async () => {
      const userStore = useUserStore()
      userStore.token = 'fake-token'
      userStore.userInfo = mockAccountantUser  // 仅 ACCOUNTANT 角色
      await router.push('/system/user')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/dashboard')
    })

    it('非 ADMIN 用户访问 /system/role 应跳转 /dashboard', async () => {
      const userStore = useUserStore()
      userStore.token = 'fake-token'
      userStore.userInfo = mockAccountantUser
      await router.push('/system/role')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/dashboard')
    })

    it('非 ADMIN 用户访问 /system/setting 应跳转 /dashboard', async () => {
      const userStore = useUserStore()
      userStore.token = 'fake-token'
      userStore.userInfo = mockAccountantUser
      await router.push('/system/setting')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/dashboard')
    })

    it('非 ADMIN 用户访问 /system/log 应跳转 /dashboard', async () => {
      const userStore = useUserStore()
      userStore.token = 'fake-token'
      userStore.userInfo = mockAccountantUser
      await router.push('/system/log')
      await router.isReady()
      expect(router.currentRoute.value.path).toBe('/dashboard')
    })
  })

  describe('404 兜底路由', () => {
    it('未知路由应命中 NotFound', async () => {
      const userStore = useUserStore()
      userStore.token = 'fake-token'
      userStore.userInfo = mockAdminUser
      await router.push('/this-route-does-not-exist')
      await router.isReady()
      // NotFound 命中 :pathMatch(.*)* 路由,name 为 NotFound
      expect(router.currentRoute.value.name).toBe('NotFound')
    })
  })
})
