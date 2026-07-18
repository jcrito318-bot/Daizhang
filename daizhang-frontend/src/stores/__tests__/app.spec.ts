import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

vi.mock('@/api/accountset', () => ({
  accountSetApi: {
    getList: vi.fn(),
    getPage: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    init: vi.fn()
  }
}))

import { useAppStore } from '@/stores/app'
import { accountSetApi } from '@/api/accountset'
import type { AccountSetVO } from '@/types/accountset'

const mockAccountSets: AccountSetVO[] = [
  {
    id: 1, code: '001', name: '账套A', companyName: 'A公司',
    industryType: '', accountingStandard: '', startYear: 2024, startMonth: 1,
    currencyCode: 'CNY', taxpayerType: '', contactPerson: '', contactPhone: '',
    address: '', status: 1, createTime: '2024-01-01'
  },
  {
    id: 2, code: '002', name: '账套B', companyName: 'B公司',
    industryType: '', accountingStandard: '', startYear: 2024, startMonth: 1,
    currencyCode: 'CNY', taxpayerType: '', contactPerson: '', contactPhone: '',
    address: '', status: 1, createTime: '2024-01-01'
  }
]

/**
 * app store 单元测试
 *
 * 覆盖:
 * - currentAccountSetId: localStorage 读取/同步
 * - setCurrentAccountSet: 写入内存 + localStorage,null/NaN 处理
 * - loadAccountSetList: 缓存命中/强制刷新/列表为空时回退
 * - sidebarCollapsed / toggleSidebar
 */
describe('useAppStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
  })

  afterEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('currentAccountSetId - 初始化从 localStorage 读取', () => {
    it('localStorage 有合法数字时应读取该值', () => {
      localStorage.setItem('currentAccountSetId', '42')
      const store = useAppStore()
      expect(store.currentAccountSetId).toBe(42)
    })

    it('localStorage 为空时应为 null', () => {
      const store = useAppStore()
      expect(store.currentAccountSetId).toBeNull()
    })

    it('localStorage 为非数字字符串时应为 null(NaN 处理)', () => {
      localStorage.setItem('currentAccountSetId', 'abc')
      const store = useAppStore()
      expect(store.currentAccountSetId).toBeNull()
    })

    it('localStorage 为空字符串时应为 null', () => {
      localStorage.setItem('currentAccountSetId', '')
      const store = useAppStore()
      expect(store.currentAccountSetId).toBeNull()
    })
  })

  describe('setCurrentAccountSet - 写入', () => {
    it('设置合法 id 应同步写入 localStorage', () => {
      const store = useAppStore()
      store.setCurrentAccountSet(99)
      expect(store.currentAccountSetId).toBe(99)
      expect(localStorage.getItem('currentAccountSetId')).toBe('99')
    })

    it('设置 null 应清除 localStorage', () => {
      localStorage.setItem('currentAccountSetId', '5')
      const store = useAppStore()
      store.setCurrentAccountSet(null)
      expect(store.currentAccountSetId).toBeNull()
      expect(localStorage.getItem('currentAccountSetId')).toBeNull()
    })

    it('设置 NaN 应清除 localStorage(等价于 null)', () => {
      localStorage.setItem('currentAccountSetId', '5')
      const store = useAppStore()
      store.setCurrentAccountSet(NaN)
      expect(store.currentAccountSetId).toBeNull()
      expect(localStorage.getItem('currentAccountSetId')).toBeNull()
    })
  })

  describe('sidebarCollapsed / toggleSidebar', () => {
    it('默认未折叠', () => {
      const store = useAppStore()
      expect(store.sidebarCollapsed).toBe(false)
    })

    it('toggleSidebar 应切换状态', () => {
      const store = useAppStore()
      store.toggleSidebar()
      expect(store.sidebarCollapsed).toBe(true)
      store.toggleSidebar()
      expect(store.sidebarCollapsed).toBe(false)
    })
  })

  describe('loadAccountSetList - 缓存与刷新', () => {
    it('首次调用应请求接口并缓存', async () => {
      ;(accountSetApi.getList as any).mockResolvedValue({
        code: 200, message: 'ok', data: mockAccountSets, timestamp: 0
      })
      const store = useAppStore()
      const result = await store.loadAccountSetList()
      expect(accountSetApi.getList).toHaveBeenCalledTimes(1)
      expect(result).toEqual(mockAccountSets)
      expect(store.accountSetList).toEqual(mockAccountSets)
      expect(store.accountSetListLoaded).toBe(true)
    })

    it('已加载后再调用应命中缓存,不重新请求', async () => {
      ;(accountSetApi.getList as any).mockResolvedValue({
        code: 200, message: 'ok', data: mockAccountSets, timestamp: 0
      })
      const store = useAppStore()
      await store.loadAccountSetList()
      await store.loadAccountSetList()
      expect(accountSetApi.getList).toHaveBeenCalledTimes(1)
    })

    it('force=true 应强制刷新缓存', async () => {
      ;(accountSetApi.getList as any).mockResolvedValue({
        code: 200, message: 'ok', data: mockAccountSets, timestamp: 0
      })
      const store = useAppStore()
      await store.loadAccountSetList()
      await store.loadAccountSetList(true)
      expect(accountSetApi.getList).toHaveBeenCalledTimes(2)
    })

    it('当前账套不存在于列表时应回退到第一个', async () => {
      localStorage.setItem('currentAccountSetId', '999')
      ;(accountSetApi.getList as any).mockResolvedValue({
        code: 200, message: 'ok', data: mockAccountSets, timestamp: 0
      })
      const store = useAppStore()
      await store.loadAccountSetList()
      expect(store.currentAccountSetId).toBe(1)  // 回退到第一个
      expect(localStorage.getItem('currentAccountSetId')).toBe('1')
    })

    it('当前账套为空时应自动选择第一个', async () => {
      ;(accountSetApi.getList as any).mockResolvedValue({
        code: 200, message: 'ok', data: mockAccountSets, timestamp: 0
      })
      const store = useAppStore()
      await store.loadAccountSetList()
      expect(store.currentAccountSetId).toBe(1)
    })

    it('列表为空时应保持 currentAccountSetId 为 null(不抛错)', async () => {
      ;(accountSetApi.getList as any).mockResolvedValue({
        code: 200, message: 'ok', data: [], timestamp: 0
      })
      const store = useAppStore()
      const result = await store.loadAccountSetList()
      expect(result).toEqual([])
      // currentAccountSetId 不应被设置为 undefined,在 null 时仍为 null
      expect(store.currentAccountSetId).toBeNull()
    })

    it('接口失败时应静默处理(不抛错,拦截器已提示)', async () => {
      ;(accountSetApi.getList as any).mockRejectedValue(new Error('网络错误'))
      const store = useAppStore()
      // 不应抛出
      const result = await store.loadAccountSetList()
      expect(result).toEqual([])
      expect(store.accountSetListLoaded).toBe(false)
    })
  })
})
