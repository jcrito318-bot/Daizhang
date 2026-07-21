import { defineStore } from 'pinia'
import { ref } from 'vue'
import { accountSetApi, preferenceApi } from '@/api/accountset'
import type { AccountSetVO, RecentAccountSet } from '@/types/accountset'

const CURRENT_ACCOUNT_SET_KEY = 'currentAccountSetId'
// 最近访问列表最大保留条数
const RECENT_MAX = 5

export const useAppStore = defineStore('app', () => {
  // 账套列表缓存:多个页面共享,避免每次进入页面都调 /accountset/list
  const accountSetList = ref<AccountSetVO[]>([])
  const accountSetListLoaded = ref(false)
  // 当前账套:从 localStorage 读取,刷新不丢失
  const currentAccountSetId = ref<number | null>(
    (() => {
      const stored = localStorage.getItem(CURRENT_ACCOUNT_SET_KEY)
      const num = stored ? Number(stored) : NaN
      return Number.isFinite(num) ? num : null
    })()
  )
  const sidebarCollapsed = ref(false)

  // ===== 账套切换器优化:收藏置顶 + 最近访问 =====
  /** 收藏的账套ID列表 */
  const favorites = ref<number[]>([])
  /** 最近访问的账套(按 lastAccessedAt DESC,最多 RECENT_MAX 条) */
  const recentAccountSets = ref<RecentAccountSet[]>([])
  /** 偏好是否已加载(首次使用时为 false,切换器回退到全部账套列表) */
  const preferencesLoaded = ref(false)

  /**
   * 加载账套列表(带缓存)。首次调用请求接口,后续返回缓存。
   * 账套发生增删改后可传 force=true 强制刷新。
   */
  async function loadAccountSetList(force = false): Promise<AccountSetVO[]> {
    if (accountSetListLoaded.value && !force) {
      return accountSetList.value
    }
    try {
      const res = await accountSetApi.getList()
      accountSetList.value = res.data || []
      accountSetListLoaded.value = true
      // 若当前账套为空或已不存在,则回退到第一个
      const exists = accountSetList.value.some(item => item.id === currentAccountSetId.value)
      if (!currentAccountSetId.value || !exists) {
        const firstId = accountSetList.value[0]?.id ?? null
        setCurrentAccountSet(firstId)
      }
    } catch {
      // 拦截器已提示,这里不重复处理
    }
    return accountSetList.value
  }

  function setCurrentAccountSet(id: number | null) {
    // NaN 视为 null:避免上游传入 NaN 时,内存中残留 NaN 破坏类型一致性
    // (类型为 number | null,NaN 既非 null 也非有效数字,会导致后续比较失败)
    if (id !== null && Number.isNaN(id)) {
      id = null
    }
    currentAccountSetId.value = id
    if (id === null) {
      localStorage.removeItem(CURRENT_ACCOUNT_SET_KEY)
    } else {
      localStorage.setItem(CURRENT_ACCOUNT_SET_KEY, String(id))
      // 切换账套时异步记录访问(更新后端最近访问),失败不影响切换
      void recordAccess(id)
    }
  }

  /**
   * 启动时从后端加载偏好(收藏 + 最近访问)。
   * 失败不阻塞应用:捕获异常后保持空列表,切换器回退到全部账套列表。
   */
  async function loadPreferences(): Promise<void> {
    try {
      const res = await preferenceApi.getPreferences()
      const list = res.data || []
      favorites.value = list
        .filter(p => p.isFavorite === 1)
        .map(p => p.accountSetId)
      // 最近访问:仅保留有 lastAccessedAt 的项,按时间倒序取前 RECENT_MAX 条
      recentAccountSets.value = list
        .filter(p => p.lastAccessedAt)
        .sort((a, b) => {
          const ta = a.lastAccessedAt ? new Date(a.lastAccessedAt).getTime() : 0
          const tb = b.lastAccessedAt ? new Date(b.lastAccessedAt).getTime() : 0
          return tb - ta
        })
        .slice(0, RECENT_MAX)
        .map(p => ({
          id: p.accountSetId,
          name: p.accountSetName,
          lastAccessedAt: p.lastAccessedAt
        }))
      preferencesLoaded.value = true
    } catch {
      // 启动加载失败不阻塞应用,保持空偏好,切换器回退到全部账套列表
    }
  }

  /**
   * 记录账套访问:乐观更新本地"最近访问"列表,并异步通知后端。
   * 失败不影响账套切换(吞掉异常)。
   */
  async function recordAccess(accountSetId: number): Promise<void> {
    // 乐观更新:将当前账套置顶到最近访问列表,立即反馈
    const acct = accountSetList.value.find(a => a.id === accountSetId)
    if (acct) {
      const now = new Date().toISOString()
      recentAccountSets.value = [
        { id: accountSetId, name: acct.name, lastAccessedAt: now },
        ...recentAccountSets.value.filter(r => r.id !== accountSetId)
      ].slice(0, RECENT_MAX)
    }
    // 异步通知后端,失败静默处理(拦截器已提示)
    try {
      await preferenceApi.recordAccess(accountSetId)
    } catch {
      // 记录访问失败不影响切换
    }
  }

  /**
   * 切换账套收藏状态。返回切换后的收藏状态(true=已收藏)。
   */
  async function toggleFavorite(accountSetId: number): Promise<boolean> {
    const wasFavorite = favorites.value.includes(accountSetId)
    try {
      const res = await preferenceApi.toggleFavorite(accountSetId)
      const nowFavorite = res.data === true
      if (nowFavorite) {
        if (!favorites.value.includes(accountSetId)) {
          favorites.value = [...favorites.value, accountSetId]
        }
      } else {
        favorites.value = favorites.value.filter(id => id !== accountSetId)
      }
      return nowFavorite
    } catch {
      // 失败时保持原状态
      return wasFavorite
    }
  }

  /** 判断账套是否已收藏(供模板使用) */
  function isFavorite(accountSetId: number): boolean {
    return favorites.value.includes(accountSetId)
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    accountSetList,
    accountSetListLoaded,
    currentAccountSetId,
    sidebarCollapsed,
    // 账套切换器偏好
    favorites,
    recentAccountSets,
    preferencesLoaded,
    loadAccountSetList,
    setCurrentAccountSet,
    loadPreferences,
    recordAccess,
    toggleFavorite,
    isFavorite,
    toggleSidebar
  }
})
