import { defineStore } from 'pinia'
import { ref } from 'vue'
import { accountSetApi } from '@/api/accountset'
import type { AccountSetVO } from '@/types/accountset'

const CURRENT_ACCOUNT_SET_KEY = 'currentAccountSetId'

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
    currentAccountSetId.value = id
    if (id === null || Number.isNaN(id)) {
      localStorage.removeItem(CURRENT_ACCOUNT_SET_KEY)
    } else {
      localStorage.setItem(CURRENT_ACCOUNT_SET_KEY, String(id))
    }
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    accountSetList,
    accountSetListLoaded,
    currentAccountSetId,
    sidebarCollapsed,
    loadAccountSetList,
    setCurrentAccountSet,
    toggleSidebar
  }
})
